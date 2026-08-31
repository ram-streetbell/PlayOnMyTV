const DEFAULT_API = 'https://playonmytv-web.onrender.com/api/v1';
const DB_NAME = 'playonmytv-webos';
const DB_VERSION = 1;
const STORE = 'media';
const SYNC_INTERVAL = 5 * 60 * 1000;

let apiBase = localStorage.getItem('playonmytv.api') || DEFAULT_API;
let token = localStorage.getItem('playonmytv.token') || '';
let manifest = null;
let playlist = [];
let currentIndex = 0;
let currentObjectUrl = null;
let rotationTimer = null;
let syncTimer = null;

const $ = (id) => document.getElementById(id);

$('apiUrl').value = apiBase;
$('token').value = token;
$('connect').addEventListener('click', connectAndSync);
$('token').addEventListener('keydown', (e) => { if (e.key === 'Enter') connectAndSync(); });
$('apiUrl').addEventListener('change', () => { apiBase = $('apiUrl').value.trim().replace(/\/$/, ''); });

openDb().then(() => {
  if (token) connectAndSync();
});

async function connectAndSync() {
  apiBase = $('apiUrl').value.trim().replace(/\/$/, '') || DEFAULT_API;
  token = $('token').value.trim();
  if (!token) {
    $('setupError').textContent = 'Enter the device token first.';
    return;
  }
  localStorage.setItem('playonmytv.api', apiBase);
  localStorage.setItem('playonmytv.token', token);
  $('setupError').textContent = '';
  await sync();
}

async function sync() {
  setStatus('Checking for updates…');
  try {
    const response = await fetch(`${apiBase}/device/manifest`, {
      headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
      cache: 'no-store'
    });
    const body = await response.json().catch(() => null);
    if (!response.ok || !body || !body.success) {
      throw new Error((body && body.message) || `Manifest request failed (${response.status})`);
    }

    manifest = body.data;
    playlist = buildPlaylist(manifest);
    await syncMedia(manifest.media || []);
    localStorage.setItem('playonmytv.manifestVersion', String(manifest.manifest_version || ''));

    if (!playlist.length) {
      showPlayer();
      setStatus('No media assigned');
      return;
    }

    showPlayer();
    startPlayback(true);
    setStatus(`Synced ${playlist.length} media item${playlist.length === 1 ? '' : 's'}`);
  } catch (error) {
    console.error(error);
    if (manifest && playlist.length) {
      showPlayer();
      startPlayback(false);
      setStatus('Offline mode');
    } else {
      $('setup').hidden = false;
      $('player').hidden = true;
      $('setupError').textContent = error.message || 'Unable to connect.';
    }
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
  const work = [];
  let total = items.reduce((sum, item) => sum + (Number(item.size) || 0), 0);
  let completed = 0;
  let downloaded = 0;

  showSync(true, 0);
  for (const item of items) {
    const key = cacheKey(item);
    const existing = await idbGet(db, key);
    if (existing && existing.blob) {
      completed += Number(item.size) || 0;
      downloaded += Number(item.size) || 0;
      updateSync(total, completed, downloaded, items.length);
      continue;
    }
    work.push(item);
  }

  for (const item of work) {
    try {
      const blob = await downloadBlob(item, (bytes) => {
        const current = completed + bytes;
        updateSync(total, current, downloaded, items.length);
      });
      await idbPut(db, { key: cacheKey(item), mediaId: String(item.id), checksum: item.checksum, blob, updatedAt: item.updated_at });
      completed += Number(item.size) || blob.size || 0;
      downloaded += Number(item.size) || blob.size || 0;
    } catch (error) {
      console.warn('Media cache failed', item.id, error);
      completed += Number(item.size) || 0;
    }
    updateSync(total, completed, downloaded, items.length);
  }
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
  setStatus(cachedUrl ? 'Playing offline media' : 'Playing network media');

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
  } catch (error) {
    setStatus('Press OK to start video');
    video.onkeydown = () => video.play().catch(() => {});
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
  $('setup').hidden = true;
  $('player').hidden = false;
  if (!syncTimer) syncTimer = setInterval(sync, SYNC_INTERVAL);
}

function setStatus(text) {
  $('status').textContent = text;
}

function showSync(show, percent = 0) {
  $('sync').hidden = !show;
  if (show) {
    $('syncBar').style.width = `${percent}%`;
    $('syncText').textContent = `${percent}%`;
  }
}

function updateSync(total, completed, downloaded, count) {
  const percent = total > 0 ? Math.min(100, Math.round((completed / total) * 100)) : Math.min(100, Math.round((downloaded / Math.max(1, count)) * 100));
  showSync(true, percent);
  $('syncText').textContent = `${percent}%`;
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
