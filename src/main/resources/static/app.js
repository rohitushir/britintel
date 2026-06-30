'use strict';

// --- CSRF: Spring writes the token to the XSRF-TOKEN cookie; echo it back. ---
function csrfToken() {
  const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return m ? decodeURIComponent(m[1]) : '';
}

async function api(method, url, body) {
  const opts = { method, headers: {} };
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  if (method !== 'GET') opts.headers['X-XSRF-TOKEN'] = csrfToken();
  return fetch(url, opts);
}

async function apiJson(method, url, body) {
  const res = await api(method, url, body);
  if (!res.ok) {
    let message = res.statusText;
    try { message = (await res.json()).message || message; } catch (_) {}
    throw new Error(message);
  }
  return res.status === 204 ? null : res.json();
}

const $ = (id) => document.getElementById(id);
const fmt = (iso) => iso ? new Date(iso).toLocaleString(undefined,
  { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '';
const esc = (s) => (s ?? '').toString().replace(/[&<>"]/g, c =>
  ({ '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;' }[c]));

// --- Status & event styling ---
function statusBadge(status) {
  if (!status) return '<span class="badge neutral">—</span>';
  const s = status.toLowerCase();
  let cls = 'neutral';
  if (s === 'active') cls = 'good';
  else if (s.includes('liquidation') || s.includes('insolven') || s.includes('administration')) cls = 'bad';
  else if (s === 'dissolved' || s.includes('closed')) cls = 'bad';
  else cls = 'warn';
  return `<span class="badge ${cls}">${esc(status)}</span>`;
}
function eventBadge(type) {
  const high = ['STATUS_CHANGE', 'CHARGE_CREATED'];
  const mid = ['CHARGE_SATISFIED', 'OFFICER_APPOINTED', 'OFFICER_RESIGNED', 'ADDRESS_CHANGE'];
  const cls = high.includes(type) ? 'bad' : mid.includes(type) ? 'warn' : 'neutral';
  return `<span class="badge ${cls}">${esc((type || '').replace(/_/g, ' '))}</span>`;
}

// --- Views ---
function showLoggedOut() {
  $('auth').classList.remove('hidden');
  $('app').classList.add('hidden');
  $('who').hidden = true;
  $('login-csrf').value = csrfToken();
  const p = new URLSearchParams(location.search);
  if (p.has('error')) showLoginMsg('Incorrect email or password.');
  if (p.has('loggedout')) showRegMsgOk('');
}

async function showLoggedIn(me) {
  $('auth').classList.add('hidden');
  $('app').classList.remove('hidden');
  $('who').hidden = false;
  $('who-email').textContent = me.email;
  renderUsage(me);
  await Promise.all([loadWatchlist(), loadEvents()]);
}

function renderUsage(me) {
  $('stat-count').textContent = me.watchedCount;
  $('stat-cap').textContent = `of ${me.companyCap} watched`;
}

async function loadWatchlist() {
  const rows = await apiJson('GET', '/api/watchlist');
  const tbody = $('watchlist');
  tbody.innerHTML = '';
  $('watchlist-empty').classList.toggle('hidden', rows.length > 0);
  for (const w of rows) {
    const tr = document.createElement('tr');
    tr.innerHTML =
      `<td><strong>${esc(w.companyName) || '<span class="empty">(name pending)</span>'}</strong></td>` +
      `<td class="mono">${esc(w.companyNumber)}</td>` +
      `<td>${statusBadge(w.status)}</td>` +
      `<td class="tagline">${fmt(w.addedAt)}</td>` +
      `<td style="text-align:right;"><button class="danger">Remove</button></td>`;
    tr.querySelector('button').onclick = () => removeCompany(w.companyNumber);
    tbody.appendChild(tr);
  }
}

async function loadEvents() {
  const rows = await apiJson('GET', '/api/events?limit=50');
  const tbody = $('events');
  tbody.innerHTML = '';
  $('events-empty').classList.toggle('hidden', rows.length > 0);
  for (const e of rows) {
    const tr = document.createElement('tr');
    tr.innerHTML =
      `<td class="tagline">${fmt(e.createdAt)}</td>` +
      `<td class="mono">${esc(e.companyNumber)}</td>` +
      `<td>${eventBadge(e.eventType)}</td>` +
      `<td>${esc(e.summary)}</td>`;
    tbody.appendChild(tr);
  }
}

async function refreshUsage() {
  try { renderUsage(await apiJson('GET', '/api/me')); } catch (_) {}
}

async function sendTestAlert() {
  const msg = $('test-alert-msg'); msg.className = 'msg'; msg.textContent = '';
  const btn = $('test-alert-btn'); btn.disabled = true;
  try {
    const res = await apiJson('POST', '/api/test-alert');
    msg.className = 'msg ok';
    msg.textContent = `Test alert sent to ${res.sentTo} — check your inbox in a moment.`;
  } catch (e) {
    msg.className = 'msg err';
    msg.textContent = e.message;
  } finally {
    btn.disabled = false;
  }
}

async function addCompany(ev) {
  ev.preventDefault();
  $('add-msg').textContent = '';
  const btn = $('add-btn'); btn.disabled = true;
  try {
    await apiJson('POST', '/api/watchlist', { companyNumber: $('add-number').value.trim() });
    $('add-number').value = '';
    await Promise.all([loadWatchlist(), refreshUsage()]);
  } catch (e) {
    $('add-msg').textContent = e.message;
  } finally {
    btn.disabled = false;
  }
}

async function removeCompany(number) {
  await apiJson('DELETE', `/api/watchlist/${number}`);
  await Promise.all([loadWatchlist(), refreshUsage()]);
}

async function register() {
  const msg = $('reg-msg'); msg.className = 'msg'; msg.textContent = '';
  try {
    await apiJson('POST', '/api/register', {
      email: $('reg-email').value.trim(),
      password: $('reg-password').value,
    });
    msg.className = 'msg ok';
    msg.textContent = 'Account created — switch to Sign in to continue.';
    // prefill the login form for convenience
    $('login-username').value = $('reg-email').value.trim();
    setTimeout(() => selectTab('login'), 900);
  } catch (e) {
    msg.className = 'msg err';
    msg.textContent = e.message;
  }
}

function showLoginMsg(t) { $('login-msg').textContent = t; }
function showRegMsgOk(t) { const m = $('reg-msg'); m.className = 'msg ok'; m.textContent = t; }

function selectTab(which) {
  const login = which === 'login';
  $('tab-login').classList.toggle('active', login);
  $('tab-register').classList.toggle('active', !login);
  $('login-form').classList.toggle('hidden', !login);
  $('register-pane').classList.toggle('hidden', login);
}

async function logout() {
  // native form post so Spring clears the session
  const f = document.createElement('form');
  f.method = 'post'; f.action = '/logout';
  const t = document.createElement('input');
  t.type = 'hidden'; t.name = '_csrf'; t.value = csrfToken();
  f.appendChild(t); document.body.appendChild(f); f.submit();
}

async function init() {
  $('tab-login').onclick = () => selectTab('login');
  $('tab-register').onclick = () => selectTab('register');
  $('reg-btn').onclick = register;
  $('add-form').onsubmit = addCompany;
  $('test-alert-btn').onclick = sendTestAlert;
  $('logout-btn').onclick = logout;
  // ensure the login form carries a fresh CSRF token right before submit
  $('login-form').addEventListener('submit', () => { $('login-csrf').value = csrfToken(); });

  const res = await api('GET', '/api/me');
  if (res.ok) showLoggedIn(await res.json());
  else showLoggedOut();
}

init();
