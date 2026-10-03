const productUrlInput = document.getElementById('productUrl');
const fetchBtn = document.getElementById('fetchBtn');
const pasteBtn = document.getElementById('pasteBtn');
const loader = document.getElementById('loader');
const statusText = document.getElementById('statusText');
const mediaGrid = document.getElementById('mediaGrid');
const toolbar = document.getElementById('toolbar');
const selectAll = document.getElementById('selectAll');
const selectionCount = document.getElementById('selectionCount');
const downloadBar = document.getElementById('downloadBar');
const downloadSelectedBtn = document.getElementById('downloadSelectedBtn');
const downloadAllBtn = document.getElementById('downloadAllBtn');

let mediaItems = [];

function setStatus(message, isError = false) {
  statusText.textContent = message;
  statusText.style.color = isError ? '#ff8a8a' : '#9ea9bb';
}

function setLoading(isLoading) {
  loader.classList.toggle('hidden', !isLoading);
  fetchBtn.disabled = isLoading;
  fetchBtn.textContent = isLoading ? 'Fetching...' : 'Fetch Media';
}

async function pasteFromClipboard() {
  try {
    const text = await navigator.clipboard.readText();
    if (text) {
      productUrlInput.value = text.trim();
      setStatus('Clipboard pasted successfully.');
    } else {
      setStatus('Clipboard is empty.', true);
    }
  } catch (error) {
    setStatus('Clipboard access is unavailable. Paste manually.', true);
  }
}

function renderMediaList(items) {
  mediaGrid.innerHTML = '';

  if (!items.length) {
    mediaGrid.innerHTML = '<div class="card">No media found for this page.</div>';
    return;
  }

  items.forEach((item) => {
    const card = document.createElement('article');
    card.className = `media-card ${item.selected ? 'selected' : ''}`;

    const checkboxWrap = document.createElement('div');
    checkboxWrap.className = 'check-wrap';

    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.checked = !!item.selected;
    checkbox.addEventListener('change', () => {
      item.selected = checkbox.checked;
      renderMediaList(mediaItems);
      syncSelectionState();
    });

    checkboxWrap.appendChild(checkbox);
    card.appendChild(checkboxWrap);

    if (item.type === 'video') {
      const videoPlaceholder = document.createElement('div');
      videoPlaceholder.className = 'video-placeholder';
      videoPlaceholder.textContent = 'VIDEO';
      card.appendChild(videoPlaceholder);
    } else {
      const image = document.createElement('img');
      image.src = item.url;
      image.alt = item.title;
      image.loading = 'lazy';
      card.appendChild(image);
    }

    const info = document.createElement('div');
    info.className = 'info';

    const title = document.createElement('h3');
    title.textContent = item.title;

    const quality = document.createElement('p');
    quality.textContent = item.quality;

    info.appendChild(title);
    info.appendChild(quality);
    card.appendChild(info);

    card.addEventListener('click', (event) => {
      if (event.target === checkbox) return;
      item.selected = !item.selected;
      renderMediaList(mediaItems);
      syncSelectionState();
    });

    mediaGrid.appendChild(card);
  });
}

function syncSelectionState() {
  const selected = mediaItems.filter((item) => item.selected);
  selectionCount.textContent = `${selected.length} selected`;
  selectAll.checked = mediaItems.length > 0 && selected.length === mediaItems.length;
  toolbar.classList.toggle('hidden', mediaItems.length === 0);
  downloadBar.classList.toggle('hidden', mediaItems.length === 0);
}

async function fetchMedia() {
  const url = productUrlInput.value.trim();
  if (!url) {
    setStatus('Please enter a product URL first.', true);
    return;
  }

  setLoading(true);
  setStatus('Fetching media list...');

  try {
    const response = await fetch('/api/fetch-media', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url })
    });

    const data = await response.json();
    if (!response.ok || data.error) {
      throw new Error(data.error || 'Failed to fetch media.');
    }

    mediaItems = (data.items || []).map((item) => ({ ...item, selected: false }));
    renderMediaList(mediaItems);
    syncSelectionState();

    if (!mediaItems.length) {
      setStatus(data.message || 'No media was found on this page.', true);
      return;
    }

    setStatus(data.message || 'Media extracted successfully.');
  } catch (error) {
    setStatus(error.message || 'Unable to fetch media.', true);
  } finally {
    setLoading(false);
  }
}

function selectAllMedia() {
  const shouldSelect = !mediaItems.every((item) => item.selected);
  mediaItems = mediaItems.map((item) => ({ ...item, selected: shouldSelect }));
  renderMediaList(mediaItems);
  syncSelectionState();
}

async function downloadFile(url, filename) {
  const response = await fetch(url);
  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);

  const anchor = document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = filename;
  anchor.style.display = 'none';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();

  URL.revokeObjectURL(objectUrl);
}

async function downloadSelected() {
  const selected = mediaItems.filter((item) => item.selected);
  if (!selected.length) {
    setStatus('Select at least one media item first.', true);
    return;
  }

  for (const item of selected) {
    const extension = item.type === 'video' ? '.mp4' : '.jpg';
    const fileName = `${item.title || 'media'}${extension}`.replace(/\s+/g, '_');
    try {
      await downloadFile(item.url, fileName);
    } catch (error) {
      console.error('Download failed:', error);
    }
  }

  setStatus(`Started download for ${selected.length} item(s).`);
}

async function downloadAll() {
  if (!mediaItems.length) {
    setStatus('There are no media items to download.', true);
    return;
  }

  for (const item of mediaItems) {
    const extension = item.type === 'video' ? '.mp4' : '.jpg';
    const fileName = `${item.title || 'media'}${extension}`.replace(/\s+/g, '_');
    try {
      await downloadFile(item.url, fileName);
    } catch (error) {
      console.error('Download failed:', error);
    }
  }

  setStatus(`Started download for all ${mediaItems.length} item(s).`);
}

fetchBtn.addEventListener('click', fetchMedia);
pasteBtn.addEventListener('click', pasteFromClipboard);
selectAll.addEventListener('change', selectAllMedia);
downloadSelectedBtn.addEventListener('click', downloadSelected);
downloadAllBtn.addEventListener('click', downloadAll);

setStatus('Paste an AliExpress product URL to begin.');
