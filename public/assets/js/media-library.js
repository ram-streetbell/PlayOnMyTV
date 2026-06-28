document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('[data-media-library]');

    if (!root) {
        return;
    }

    const alertBox = document.getElementById('mediaLibraryAlert');
    const searchInput = root.querySelector('[data-media-search]');
    const filterInput = root.querySelector('[data-media-filter]');
    const counter = root.querySelector('[data-media-counter]');
    const grid = root.querySelector('[data-media-grid]');
    const uploadUrl = root.dataset.uploadUrl || '/api/v1/media/upload';
    const previewModalElement = document.getElementById('mediaPreviewModal');
    const previewTitle = previewModalElement?.querySelector('[data-preview-title]');
    const previewContent = previewModalElement?.querySelector('[data-preview-content]');
    const uploadModalElement = document.getElementById('uploadMediaModal');
    const uploadForm = uploadModalElement?.querySelector('[data-upload-form]');
    const uploadDropzone = uploadModalElement?.querySelector('[data-upload-dropzone]');
    const uploadInput = uploadModalElement?.querySelector('[data-upload-input]');
    const uploadBrowse = uploadModalElement?.querySelector('[data-upload-browse]');
    const uploadTitle = uploadModalElement?.querySelector('[data-upload-title]');
    const uploadFilename = uploadModalElement?.querySelector('[data-upload-filename]');
    const uploadPreview = uploadModalElement?.querySelector('[data-upload-preview]');
    const uploadProgress = uploadModalElement?.querySelector('[data-upload-progress]');
    const uploadSubmit = uploadModalElement?.querySelector('[data-upload-submit]');
    const uploadCancel = uploadModalElement?.querySelector('[data-upload-cancel]');
    const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content') ?? '';
    const previewModal = previewModalElement ? new bootstrap.Modal(previewModalElement) : null;

    let selectedFile = null;
    let activeRequest = null;

    const updateCounter = () => {
        const visibleCards = Array.from(root.querySelectorAll('[data-media-card]')).filter((card) => card.style.display !== 'none');
        if (counter) {
            counter.textContent = String(visibleCards.length);
        }
    };

    const applyFilters = () => {
        const searchValue = (searchInput?.value || '').trim().toLowerCase();
        const filterValue = filterInput?.value || 'all';

        root.querySelectorAll('[data-media-card]').forEach((card) => {
            const title = card.dataset.mediaTitle || '';
            const filename = card.dataset.mediaFilename || '';
            const type = card.dataset.mediaType || '';
            const matchesSearch = searchValue === '' || title.includes(searchValue) || filename.includes(searchValue);
            const matchesFilter = filterValue === 'all' || type === filterValue;

            card.style.display = matchesSearch && matchesFilter ? '' : 'none';
        });

        updateCounter();
    };

    const showAlert = (type, message) => {
        if (!alertBox) {
            return;
        }

        alertBox.className = `alert alert-${type} shadow-sm border-0`;
        alertBox.textContent = message;
        alertBox.classList.remove('d-none');
    };

    const clearAlert = () => {
        if (!alertBox) {
            return;
        }

        alertBox.className = 'alert d-none shadow-sm border-0';
        alertBox.textContent = '';
    };

    const setUploadProgress = (value) => {
        if (!uploadProgress) {
            return;
        }

        const safeValue = Math.max(0, Math.min(100, Math.round(value)));
        uploadProgress.style.width = `${safeValue}%`;
        uploadProgress.textContent = `${safeValue}%`;
        uploadProgress.setAttribute('aria-valuenow', String(safeValue));
    };

    const setUploadingState = (isUploading) => {
        if (uploadSubmit) {
            uploadSubmit.disabled = isUploading;
            uploadSubmit.textContent = isUploading ? 'Uploading...' : 'Upload';
        }

        if (uploadCancel) {
            uploadCancel.disabled = isUploading;
        }

        if (uploadBrowse) {
            uploadBrowse.disabled = isUploading;
        }

        if (uploadInput) {
            uploadInput.disabled = isUploading;
        }

        if (uploadTitle) {
            uploadTitle.disabled = isUploading;
        }
    };

    const resetUploadForm = () => {
        selectedFile = null;
        activeRequest = null;
        if (uploadForm instanceof HTMLFormElement) {
            uploadForm.reset();
        }
        if (uploadFilename) {
            uploadFilename.textContent = 'None selected';
        }
        if (uploadPreview) {
            uploadPreview.innerHTML = '<div class="upload-preview__placeholder text-muted text-center px-4">Select a file to preview it here</div>';
        }
        setUploadProgress(0);
        setUploadingState(false);
    };

    const renderSelectedFile = (file) => {
        selectedFile = file;

        if (uploadFilename) {
            uploadFilename.textContent = `${file.name} (${formatFileSize(file.size)})`;
        }

        if (uploadTitle && uploadTitle.value.trim() === '') {
            const derivedTitle = file.name.replace(/\.[^.]+$/, '');
            uploadTitle.value = derivedTitle;
        }

        if (!uploadPreview) {
            return;
        }

        const objectUrl = URL.createObjectURL(file);

        if (file.type.startsWith('video/')) {
            uploadPreview.innerHTML = `<video src="${objectUrl}" controls muted></video>`;
        } else if (file.type.startsWith('image/')) {
            uploadPreview.innerHTML = `<img src="${objectUrl}" alt="Selected upload preview">`;
        } else {
            uploadPreview.innerHTML = '<div class="upload-preview__placeholder text-muted text-center px-4">Preview unavailable for this file.</div>';
        }
    };

    const createMediaCard = (payload) => {
        const col = document.createElement('div');
        col.className = 'col-12 col-sm-6 col-xl-4 col-xxl-3';
        col.dataset.mediaCard = '';
        col.dataset.mediaId = String(payload.media_id);
        col.dataset.mediaType = payload.detectedType;
        col.dataset.mediaTitle = (payload.title || '').toLowerCase();
        col.dataset.mediaFilename = (payload.filename || '').toLowerCase();
        col.dataset.mediaUrl = payload.url;
        col.dataset.mediaThumbnail = payload.thumbnail;

        col.innerHTML = `
            <article class="card media-card border-0 shadow-sm h-100">
                <div class="media-card__thumbnail">
                    <img src="${escapeHtml(payload.thumbnail)}" alt="${escapeHtml(payload.title)}" class="media-card__image">
                    <span class="media-card__badge badge ${payload.detectedType === 'video' ? 'text-bg-dark' : 'text-bg-light'}">${payload.detectedType === 'video' ? 'Video' : 'Image'}</span>
                </div>
                <div class="card-body d-flex flex-column">
                    <h2 class="h6 mb-1 text-truncate" title="${escapeHtml(payload.title)}">${escapeHtml(payload.title)}</h2>
                    <p class="small text-muted text-truncate mb-3" title="${escapeHtml(payload.filename)}">${escapeHtml(payload.filename)}</p>
                    <dl class="row small mb-3">
                        <dt class="col-5 text-muted">Type</dt>
                        <dd class="col-7 mb-1 text-capitalize">${escapeHtml(payload.detectedType)}</dd>
                        <dt class="col-5 text-muted">Size</dt>
                        <dd class="col-7 mb-1">${escapeHtml(formatFileSize(payload.size))}</dd>
                        <dt class="col-5 text-muted">Duration</dt>
                        <dd class="col-7 mb-1">${payload.detectedType === 'video' ? 'Processing' : 'N/A'}</dd>
                        <dt class="col-5 text-muted">Created</dt>
                        <dd class="col-7 mb-0">${escapeHtml(formatDate(new Date()))}</dd>
                    </dl>
                    <div class="d-flex gap-2 mt-auto">
                        <button type="button" class="btn btn-outline-primary btn-sm flex-fill" data-preview-trigger>Preview</button>
                        <button type="button" class="btn btn-outline-secondary btn-sm flex-fill" disabled>Replace</button>
                        <button type="button" class="btn btn-outline-danger btn-sm flex-fill" disabled>Delete</button>
                    </div>
                </div>
            </article>
        `;

        return col;
    };

    const handleUpload = () => {
        clearAlert();

        if (!selectedFile) {
            showAlert('warning', 'Choose a file before uploading.');
            return;
        }

        const formData = new FormData();
        formData.append('file', selectedFile);
        formData.append('title', uploadTitle?.value?.trim() || '');

        setUploadingState(true);
        setUploadProgress(0);

        const xhr = new XMLHttpRequest();
        activeRequest = xhr;
        xhr.open('POST', uploadUrl, true);
        xhr.responseType = 'json';
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.setRequestHeader('X-CSRF-TOKEN', csrfToken);

        xhr.upload.addEventListener('progress', (event) => {
            if (!event.lengthComputable) {
                return;
            }

            const percentage = (event.loaded / event.total) * 100;
            setUploadProgress(percentage);
        });

        xhr.addEventListener('load', () => {
            setUploadingState(false);

            const response = xhr.response || safeJsonParse(xhr.responseText);
            if (xhr.status < 200 || xhr.status >= 300 || !response?.success) {
                showAlert('danger', response?.message || 'Upload failed. Please try again.');
                return;
            }

            const emptyState = root.querySelector('[data-media-empty-state]');
            if (emptyState) {
                emptyState.remove();
            }

            const card = createMediaCard({
                media_id: response.media_id,
                url: response.url,
                thumbnail: response.thumbnail,
                title: uploadTitle?.value?.trim() || selectedFile.name.replace(/\.[^.]+$/, ''),
                filename: selectedFile.name,
                size: selectedFile.size,
                detectedType: selectedFile.type.startsWith('video/') ? 'video' : 'image'
            });
            grid?.prepend(card);
            applyFilters();

            showAlert('success', response.message || 'Upload successful');
            const modal = bootstrap.Modal.getInstance(uploadModalElement);
            modal?.hide();
            resetUploadForm();
        });

        xhr.addEventListener('error', () => {
            setUploadingState(false);
            showAlert('danger', 'Upload failed. Please check your connection and try again.');
        });

        xhr.addEventListener('abort', () => {
            setUploadingState(false);
            showAlert('warning', 'Upload cancelled.');
        });

        xhr.send(formData);
    };

    const openPreview = (card) => {
        if (!previewModal || !previewTitle || !previewContent) {
            return;
        }

        const type = card.dataset.mediaType || 'image';
        const title = card.querySelector('h2')?.textContent?.trim() || 'Preview';
        const url = card.dataset.mediaUrl || '';

        previewTitle.textContent = title;

        if (type === 'video') {
            previewContent.innerHTML = `<video src="${escapeHtml(url)}" controls autoplay playsinline></video>`;
        } else {
            previewContent.innerHTML = `<img src="${escapeHtml(url)}" alt="${escapeHtml(title)}">`;
        }

        previewModal.show();
    };

    const safeJsonParse = (value) => {
        try {
            return JSON.parse(value);
        } catch {
            return null;
        }
    };

    const formatFileSize = (bytes) => {
        const size = Number(bytes || 0);
        if (size <= 0) {
            return '0 MB';
        }

        return `${(size / 1048576).toFixed(2)} MB`;
    };

    const formatDate = (date) => {
        return new Intl.DateTimeFormat('en-GB', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        }).format(date);
    };

    const escapeHtml = (value) => {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    };

    searchInput?.addEventListener('input', applyFilters);
    filterInput?.addEventListener('change', applyFilters);

    grid?.addEventListener('click', (event) => {
        const trigger = event.target.closest('[data-preview-trigger]');
        if (!trigger) {
            return;
        }

        const card = trigger.closest('[data-media-card]');
        if (card) {
            openPreview(card);
        }
    });

    uploadBrowse?.addEventListener('click', () => uploadInput?.click());
    uploadInput?.addEventListener('change', (event) => {
        const file = event.target.files?.[0];
        if (file) {
            renderSelectedFile(file);
        }
    });

    uploadDropzone?.addEventListener('click', () => uploadInput?.click());
    uploadDropzone?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            uploadInput?.click();
        }
    });

    ['dragenter', 'dragover'].forEach((eventName) => {
        uploadDropzone?.addEventListener(eventName, (event) => {
            event.preventDefault();
            uploadDropzone.classList.add('is-dragover');
        });
    });

    ['dragleave', 'drop'].forEach((eventName) => {
        uploadDropzone?.addEventListener(eventName, (event) => {
            event.preventDefault();
            uploadDropzone.classList.remove('is-dragover');
        });
    });

    uploadDropzone?.addEventListener('drop', (event) => {
        const file = event.dataTransfer?.files?.[0];
        if (file) {
            renderSelectedFile(file);
        }
    });

    uploadSubmit?.addEventListener('click', handleUpload);
    uploadModalElement?.addEventListener('hidden.bs.modal', resetUploadForm);

    previewModalElement?.addEventListener('hidden.bs.modal', () => {
        if (previewContent) {
            previewContent.innerHTML = '';
        }
    });

    uploadCancel?.addEventListener('click', () => {
        if (activeRequest) {
            activeRequest.abort();
        }
    });

    applyFilters();
});
