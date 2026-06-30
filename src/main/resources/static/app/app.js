'use strict';

// --- Clerk auth ---------------------------------------------------------------
let clerk = null;
let lastSignedIn = null; // de-dupe re-renders from Clerk's listener

async function authToken() {
  try {
    return clerk && clerk.session ? await clerk.session.getToken() : null;
  } catch (_) {
    return null;
  }
}

// Load Clerk's browser SDK using the publishable key + frontend URL from /api/config.
function loadClerk(cfg) {
  return new Promise((resolve, reject) => {
    if (!cfg.publishableKey || !cfg.frontendApiUrl) {
      reject(new Error('Clerk is not configured on the server.'));
      return;
    }
    const base = cfg.frontendApiUrl.replace(/\/+$/, '');
    const s = document.createElement('script');
    s.async = true;
    s.crossOrigin = 'anonymous';
    s.setAttribute('data-clerk-publishable-key', cfg.publishableKey);
    s.src = base + '/npm/@clerk/clerk-js@5/dist/clerk.browser.js';
    s.addEventListener('load', async () => {
      try {
        clerk = window.Clerk;
        if (!clerk.loaded) await clerk.load();
        resolve();
      } catch (e) {
        reject(e);
      }
    });
    s.addEventListener('error', () => reject(new Error('Could not reach Clerk.')));
    document.head.appendChild(s);
  });
}

// --- API ---------------------------------------------------------------------
async function api(method, url, body) {
  const opts = { method, headers: {} };
  const token = await authToken();
  if (token) opts.headers['Authorization'] = 'Bearer ' + token;
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
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
function showSignedOut() {
  $('signed-out').classList.remove('hidden');
  $('app').classList.add('hidden');
  $('clerk-userbutton').classList.add('hidden');
  const el = $('clerk-signin');
  el.innerHTML = '';
  clerk.mountSignIn(el);
}

async function showSignedIn() {
  $('signed-out').classList.add('hidden');
  $('app').classList.remove('hidden');
  const ub = $('clerk-userbutton');
  ub.classList.remove('hidden');
  ub.innerHTML = '';
  clerk.mountUserButton(ub, { afterSignOutUrl: '/' });
  try {
    renderUsage(await apiJson('GET', '/api/me'));
    await Promise.all([loadWatchlist(), loadEvents()]);
  } catch (e) {
    $('add-msg').textContent = e.message;
  }
}

function render() {
  const signedIn = !!(clerk && clerk.user);
  if (signedIn === lastSignedIn) return; // ignore no-op listener fires
  lastSignedIn = signedIn;
  if (signedIn) showSignedIn(); else showSignedOut();
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

// --- Boot --------------------------------------------------------------------
async function init() {
  $('add-form').onsubmit = addCompany;
  $('test-alert-btn').onclick = sendTestAlert;

  let cfg;
  try {
    cfg = await (await fetch('/api/config')).json();
    await loadClerk(cfg);
  } catch (e) {
    $('signed-out').classList.remove('hidden');
    $('clerk-error').textContent = e.message || 'Sign-in is unavailable right now.';
    return;
  }

  render();
  // Re-render whenever Clerk's auth state changes (sign in / sign out).
  clerk.addListener(() => render());
}

init();
