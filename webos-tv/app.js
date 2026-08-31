const DEFAULT_API = 'https://playonmytv-web.onrender.com/api/v1';
const APP_VERSION = '1.1.0';
const DB_NAME = 'playonmytv-webos';
const DB_VERSION = 1;
const STORE = 'media';
const SYNC_INTERVAL = 5 * 60 * 1000;
const PAIRING_POLL_INTERVAL = 3000;
const PAIRING_EXPIRES_FALLBACK = 10 * 60 * 1000;

let apiBase = DEFAULT_API;
let token = localStorage.getItem('playonmytv.token') || '';
let deviceUuid = localStorage.getItem('playonmytv.deviceUuid') || createUuid();
let pairingExpiresAt = 0;
let manifest = null;
let playlist = [];
let currentIndex = 0;
let currentObjectUrl = null;
let rotationTimer = null;
let syncTimer = null;
let pairingTimer = null;
let pairingRequestInFlight = false;
let syncInFlight = false;

localStorage.setItem('playonmytv.deviceUuid', deviceUuid);

const $ = (id) => document.getElementById(id);

openDb().then(() => bootstrap()).catch((error) => showPairingError(error));

async function bootstrap() {
  if (token) {
    try {
      await sync();
      return;
    } catch (_) {
      token = '';
      localStorage.removeItem('playonmytv.token');
    }
  }
  showPairing();
  await requestPairingCode();
  startPairingPolling();
}

function createUuid() {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') return window.crypto.randomUUID();
  const bytes = new Uint8Array(16);
  if (window.crypto && typeof window.crypto.getRandomValues === 'function') window.crypto.getRandomValues(bytes);
  else for (let i = 0; i < bytes.length; i++) bytes[i] = Math.floor(Math.random() * 256);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`;
}

function showPairing() {
  $('pairing').hidden = false;
  $('player').hidden = true;
  $('pairingCode').textContent = '------';
  $('pairingState').textContent = 'Connecting…';
  $('pairingError').textContent = '';
}

async function requestPairingCode() {
  if (pairingRequestInFlight) return;
  pairingRequestInFlight = true;
  showPairing();
  $('pairingState').textContent = 'Getting device code…';
  try {
    const response = await fetch(`${apiBase}/device/pairing/start`, {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify({
        device_uuid: deviceUuid,
        device_name: 'LG webOS TV',
        app_version: APP_VERSION
      }),
      cache: 'no-store'
    });
    const body = await response.json().catch(() => null);
    if (!response.ok || !body || !body.success || !body.data) {
      throw new Error((body && (body.message || body.error)) || `Pairing request failed (${response.status})`);
    }
    const data = body.data;
    $('pairingCode').textContent = formatCode(data.pairing_code);
    $('pairingState').textContent = 'Waiting for pairing…';
    pairingExpiresAt = Date.parse(data.expires_at || '') || (Date.now() + PAIRING_EXPIRES_FALLBACK);
    $('pairingError').textContent = '';
  } catch (error) {
    $('pairingState').textContent = 'Unable to connect';
    showPairingError(error);
  } finally {
    pairingRequestInFlight = false;
  }
}

function formatCode(code) {
  const value = String(code || '').replace(/\D/g, '').slice(0, 6);
  return value.length === 6 ? `${value.slice(0,3)} ${value.slice(3)}` : '------';
}

function startPairingPolling() {
  clearInterval(pairingTimer);
  pairingTimer = setInterval(checkPairingStatus, PAIRING_POLL_INTERVAL);
  checkPairingStatus();
}

async function checkPairingStatus() {
  if (pairingRequestInFlight || pairingTimer === null) return;
  if (Date.now() >= pairingExpiresAt) {
    await requestPairingCode();
    return;
  }
  try {
    const response = await fetch(`${apiBase}/device/pairing/status`, {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify({ device_uuid: deviceUuid }),
      cache: 'no-store'
    });
    const body = await response.json().catch(() => null);
    if (!response.ok || !body || !body.success) throw new Error((body && (body.message || body.error)) || `Pairing status failed (${response.status})`);
    const data = body.data || {};
    if (!data.waiting && data.device_token) {
      token = String(data.device_token);
      localStorage.setItem('playonmytv.token', token);
      clearInterval(pairingTimer);
      pairingTimer = null;
      $('pairingCode').textContent = '✓';
      $('pairingState').textContent = 'TV paired. Preparing content…';
      await sync();
    }
  } catch (error) {
    showPairingError(error);
  }
}

function showPairingError(error) {
  console.error(error);
  $('pairingError').textContent = error && error.message ? error.message : 'Unable to connect. Retrying…';
}

async function sync() {
  if (!token || syncInFlight) return;
  syncInFlight = true;
  setStatus('Checking for updates…');
  try {
    const response = await fetch(`${apiBase}/device/manifest`, {
      headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
      cache: 'no-store'
    });
    const body = await response.json().catch(() => null);
    if (!response.ok || !body || !body.success) {
      throw new Error((body && (body.message || body.error)) || `Manifest request failed (${response.status})`);
    }

    manifest = body.data;
    playlist = buildPlaylist(manifest);
    await syncMedia(manifest.media || []);
    localStorage.setItem('playonmytv.manifestVersion', String(manifest.manifest_version || ''));

    showPlayer();
    if (!playlist.length) {
      setStatus('No media available');
      showSync(false);
      return;
    }
    startPlayback(true);
    setStatus(`Ready · ${playlist.length} media item${playlist.length === 1 ? '' : 's'}`);
  } catch (error) {
    console.error(error);
    if (manifest && playlist.length) {
      showPlayer();
      startPlayback(false);
      setStatus('Offline mode');
    } else if (String(error.message || '').toLowerCase().includes('token')) {
      token = '';
      localStorage.removeItem('playonmytv.token');
      showPairing();
      await requestPairingCode();
      startPairingPolling();
    } else {
      throw error;
    }
  } finally {
    syncInFlight = false;
  }
}

function buildPlaylist(data) {
  const mediaById = new Map((data.media || []).map(item => [String(item.id), item]));
  const assigned = (data.assigned_playlists || [])[0];
  if (assigned && Array.isArray(assigned.items) && assigned.items.length) {
    return assigned.items
      .slice()
      .sort((a, b) => (a.sort_order || 0) - (b.sort_order || 0))
      .map(item => ({ media: mediaById.get(String(item.media_id)), duration: item.image_duration_seconds }))
      .filter(item => item.media);
  }
  return (data.media || []).map(media => ({ media, duration: null }));
}

async function syncMedia(items) {
  if (!items.length) return;
  const db = await openDb();
  let total = items.reduce((sum, item) => sum + (Number(item.size) || 0), 0);
  let completed = 0;
  let downloadedBytes = 0;
  let downloadedItems = 0;
  const work = [];

  showSync(true, 0, `Preparing ${items.length} item${items.length === 1 ? '' : 's'}…`);
  for (const item of items) {
    const existing = await idbGet(db, cacheKey(item));
    if (existing && existing.blob) {
      completed += Number(item.size) || existing.blob.size || 0;
      downloadedBytes += Number(item.size) || existing.blob.size || 0;
      downloadedItems++;
    } else {
      work.push(item);
    }
    updateSync(total, completed, downloadedBytes, items.length, downloadedItems);
  }

  for (const item of work) {
    showSync(true, total ? Math.round((completed / total) * 100) : 0, `Downloading ${downloadedItems + 1} of ${items.length}…`);
    try {
      const blob = await downloadBlob(item, (bytes) => {
        updateSync(total, completed + bytes, downloadedBytes, items.length, downloadedItems);
      });
      await idbPut(db, { key: cacheKey(item), mediaId: String(item.id), checksum: item.checksum, blob, updatedAt: item.updated_at });
      completed += Number(item.size) || blob.size || 0;
      downloadedBytes += Number(item.size) || blob.size || 0;
      downloadedItems++;
    } catch (error) {
      console.warn('Media cache failed', item.id, error);
      completed += Number(item.size) || 0;
      downloadedItems++;
    }
    updateSync(total, completed, downloadedBytes, items.length, downloadedItems);
  }
  showSync(true, 100, 'Sync complete');
  await new Promise(resolve => setTimeout(resolve, 350));
  showSync(false);
}

async function downloadBlob(item, onProgress) {
  const response = await fetch(item.storage_url, { cache: 'no-store' });
  if (!response.ok || !response.body) throw new Error(`Media ${item.id} returned ${response.status}`);
  const reader = response.body.getReader();
  const chunks = [];
  let received = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    chunks.push(value);
    received += value.byteLength;
    onProgress(received);
  }
  const type = item.type === 'image' ? 'image/jpeg' : (response.headers.get('content-type') || 'video/mp4');
  return new Blob(chunks, { type });
}

function cacheKey(item) {
  return `${item.id}:${item.checksum}`;
}

async function getCachedMedia(item) {
  try {
    const db = await openDb();
    const cached = await idbGet(db, cacheKey(item));
    return cached && cached.blob ? URL.createObjectURL(cached.blob) : null;
  } catch (_) {
    return null;
  }
}

function startPlayback(reset) {
  if (reset) currentIndex = 0;
  clearTimeout(rotationTimer);
  if (!playlist.length) return;
  playCurrent();
}

async function playCurrent() {
  if (!playlist.length) return;
  const entry = playlist[currentIndex % playlist.length];
  const item = entry.media;
  if (!item) return nextMedia();

  clearTimeout(rotationTimer);
  releaseObjectUrl();
  $('empty').hidden = true;
  $('image').hidden = true;
  $('video').hidden = true;

  const cachedUrl = await getCachedMedia(item);
  const src = cachedUrl || item.storage_url;
  setStatus(cachedUrl ? 'Playing cached media' : 'Playing network media');

  if (item.type === 'image') {
    $('image').src = src;
    $('image').hidden = false;
    currentObjectUrl = cachedUrl;
    const seconds = Number(entry.duration || item.duration || 10);
    rotationTimer = setTimeout(nextMedia, Math.max(1, seconds) * 1000);
    return;
  }

  const video = $('video');
  video.src = src;
  video.hidden = false;
  currentObjectUrl = cachedUrl;
  video.onended = nextMedia;
  video.onerror = () => {
    setStatus('Media playback failed');
    rotationTimer = setTimeout(nextMedia, 1500);
  };
  try {
    await video.play();
  } catch (_) {
    setStatus('Video ready');
    video.focus();
  }
}

function nextMedia() {
  currentIndex = (currentIndex + 1) % playlist.length;
  playCurrent();
}

function releaseObjectUrl() {
  if (currentObjectUrl) {
    URL.revokeObjectURL(currentObjectUrl);
    currentObjectUrl = null;
  }
}

function showPlayer() {
  $('pairing').hidden = true;
  $('player').hidden = false;
  if (!syncTimer) syncTimer = setInterval(() => sync().catch(error => console.error(error)), SYNC_INTERVAL);
}

function setStatus(text) {
  $('status').textContent = text;
}

function showSync(show, percent = 0, title = 'Preparing content…') {
  $('sync').hidden = !show;
  if (show) {
    $('syncBar').style.width = `${Math.max(0, Math.min(100, percent))}%`;
    $('syncText').textContent = `${Math.max(0, Math.min(100, percent))}%`;
    $('syncTitle').textContent = title;
  }
}

function updateSync(total, completed, downloadedBytes, count, downloadedItems) {
  const percent = total > 0
    ? Math.min(100, Math.round((completed / total) * 100))
    : Math.min(100, Math.round((downloadedItems / Math.max(1, count)) * 100));
  showSync(true, percent, `Preparing content… ${downloadedItems} of ${count}`);
}

function openDb() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE)) db.createObjectStore(STORE, { keyPath: 'key' });
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function idbGet(db, key) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly');
    const req = tx.objectStore(STORE).get(key);
    req.onsuccess = () => resolve(req.result || null);
    req.onerror = () => reject(req.error);
  });
}

function idbPut(db, value) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite');
    tx.objectStore(STORE).put(value);
    tx.oncomplete = resolve;
    tx.onerror = () => reject(tx.error);
  });
}

window.addEventListener('beforeunload', releaseObjectUrl);
