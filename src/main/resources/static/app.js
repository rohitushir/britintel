'use strict';

// --- CSRF helper: Spring writes the token to the XSRF-TOKEN cookie; echo it back as a header. ---
function csrfToken() {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : '';
}

async function api(method, url, body) {
  const opts = { method, headers: {} };
  if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  if (method !== 'GET') {
    opts.headers['X-XSRF-TOKEN'] = csrfToken();
  }
  const res = await fetch(url, opts);
  return res;
}

async function apiJson(method, url, body) {
  const res = await api(method, url, body);
  if (!res.ok) {
    let message = res.statusText;
    try { message = (await res.json()).message || message; } catch (_) { /* ignore */ }
    throw new Error(message);
  }
  return res.status === 204 ? null : res.json();
}

const $ = (id) => document.getElementById(id);
const fmt = (iso) => iso ? new Date(iso).toLocaleString() : '';

// --- Views ---
function showLoggedOut() {
  $('auth').classList.remove('hidden');
  $('app').classList.add('hidden');
}

async function showLoggedIn(me) {
  $('auth').classList.add('hidden');
  $('app').classList.remove('hidden');
  $('me-email').textContent = me.email;
  $('me-usage').textContent = `${me.watchedCount} / ${me.companyCap} watched`;
  await Promise.all([loadWatchlist(), loadEvents()]);
}

async function loadWatchlist() {
  const rows = await apiJson('GET', '/api/watchlist');
  const tbody = $('watchlist');
  tbody.innerHTML = '';
  for (const w of rows) {
    const tr = document.createElement('tr');
    tr.innerHTML = `<td>${w.companyNumber}</td><td>${w.companyName ?? ''}</td>` +
      `<td>${fmt(w.addedAt)}</td><td><button data-num="${w.companyNumber}">Remove</button></td>`;
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
    tr.innerHTML = `<td>${fmt(e.createdAt)}</td><td>${e.companyNumber}</td>` +
      `<td>${e.eventType}</td><td>${e.summary}</td>`;
    tbody.appendChild(tr);
  }
}

async function addCompany() {
  $('add-msg').textContent = '';
  const number = $('add-number').value.trim();
  try {
    await apiJson('POST', '/api/watchlist', { companyNumber: number });
    $('add-number').value = '';
    await Promise.all([loadWatchlist(), refreshUsage()]);
  } catch (e) {
    $('add-msg').textContent = e.message;
  }
}

async function removeCompany(number) {
  await apiJson('DELETE', `/api/watchlist/${number}`);
  await Promise.all([loadWatchlist(), refreshUsage()]);
}

async function refreshUsage() {
  const me = await apiJson('GET', '/api/me');
  $('me-usage').textContent = `${me.watchedCount} / ${me.companyCap} watched`;
}

async function register() {
  $('reg-msg').textContent = '';
  try {
    await apiJson('POST', '/api/register', {
      email: $('reg-email').value.trim(),
      password: $('reg-password').value,
    });
    $('reg-msg').style.color = '#15803d';
    $('reg-msg').textContent = 'Account created — now log in.';
  } catch (e) {
    $('reg-msg').style.color = '';
    $('reg-msg').textContent = e.message;
  }
}

async function logout() {
  await api('POST', '/logout');
  location.reload();
}

async function init() {
  $('reg-btn').onclick = register;
  $('add-btn').onclick = addCompany;
  $('logout-btn').onclick = logout;

  const res = await api('GET', '/api/me');
  if (res.ok) {
    showLoggedIn(await res.json());
  } else {
    showLoggedOut();
  }
}

init();
