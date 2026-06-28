<?php
declare(strict_types=1);

$totalMedia = count($mediaItems ?? []);
?>
<section
    class="media-library"
    data-media-library
    data-upload-url="/api/v1/media/upload"
    data-total-media="<?= htmlspecialchars((string) $totalMedia, ENT_QUOTES, 'UTF-8') ?>"
>
    <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-center gap-3 mb-4">
        <div>
            <h1 class="h2 mb-1">Media Library</h1>
            <p class="text-muted mb-0">Upload and manage the creative assets that will later sync to your screens.</p>
        </div>
        <div class="d-flex flex-wrap gap-2">
            <button
                type="button"
                class="btn btn-primary"
                data-bs-toggle="modal"
                data-bs-target="#uploadMediaModal"
                <?= $currentUser === null ? 'disabled' : '' ?>
            >
                Upload Media
            </button>
        </div>
    </div>

    <?php if ($currentUser === null): ?>
        <div class="alert alert-warning border-0 shadow-sm mb-4">
            Log in with an admin account before uploading media.
        </div>
    <?php endif; ?>

    <div id="mediaLibraryAlert" class="alert d-none shadow-sm border-0" role="alert"></div>

    <div class="card border-0 shadow-sm mb-4">
        <div class="card-body">
            <div class="row g-3 align-items-center">
                <div class="col-12 col-xl-4">
                    <label for="mediaSearchInput" class="form-label small text-uppercase text-muted mb-2">Search</label>
                    <input
                        id="mediaSearchInput"
                        type="search"
                        class="form-control"
                        placeholder="Search by title or filename"
                        data-media-search
                    >
                </div>
                <div class="col-12 col-sm-6 col-xl-3">
                    <label for="mediaFilterType" class="form-label small text-uppercase text-muted mb-2">Filter</label>
                    <select id="mediaFilterType" class="form-select" data-media-filter>
                        <option value="all">All Media</option>
                        <option value="image">Images</option>
                        <option value="video">Videos</option>
                    </select>
                </div>
                <div class="col-12 col-sm-6 col-xl-2">
                    <label class="form-label small text-uppercase text-muted mb-2">Visible</label>
                    <div class="media-counter h5 mb-0" data-media-counter><?= htmlspecialchars((string) $totalMedia, ENT_QUOTES, 'UTF-8') ?></div>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-4" data-media-grid>
        <?php if ($totalMedia === 0): ?>
            <div class="col-12" data-media-empty-state>
                <div class="card border-0 shadow-sm">
                    <div class="card-body py-5 text-center">
                        <h2 class="h5 mb-2">No media uploaded yet</h2>
                        <p class="text-muted mb-0">Use the upload button to add your first image or video asset.</p>
                    </div>
                </div>
            </div>
        <?php endif; ?>

        <?php foreach ($mediaItems ?? [] as $media): ?>
            <?php
            $title = (string) ($media['title'] ?? 'Untitled');
            $filename = (string) ($media['original_filename'] ?? '');
            $type = (string) ($media['media_type'] ?? 'image');
            $url = (string) ($media['storage_url'] ?? '');
            $thumbnail = (string) ($media['thumbnail_url'] ?? $url);
            $sizeBytes = (int) ($media['file_size_bytes'] ?? 0);
            $durationSeconds = isset($media['duration_seconds']) ? (int) $media['duration_seconds'] : null;
            $createdAt = (string) ($media['created_at'] ?? '');
            ?>
            <div
                class="col-12 col-sm-6 col-xl-4 col-xxl-3"
                data-media-card
                data-media-id="<?= htmlspecialchars((string) $media['id'], ENT_QUOTES, 'UTF-8') ?>"
                data-media-type="<?= htmlspecialchars($type, ENT_QUOTES, 'UTF-8') ?>"
                data-media-title="<?= htmlspecialchars(strtolower($title), ENT_QUOTES, 'UTF-8') ?>"
                data-media-filename="<?= htmlspecialchars(strtolower($filename), ENT_QUOTES, 'UTF-8') ?>"
                data-media-url="<?= htmlspecialchars($url, ENT_QUOTES, 'UTF-8') ?>"
                data-media-thumbnail="<?= htmlspecialchars($thumbnail, ENT_QUOTES, 'UTF-8') ?>"
            >
                <article class="card media-card border-0 shadow-sm h-100">
                    <div class="media-card__thumbnail">
                        <?php if ($type === 'video'): ?>
                            <img src="<?= htmlspecialchars($thumbnail, ENT_QUOTES, 'UTF-8') ?>" alt="<?= htmlspecialchars($title, ENT_QUOTES, 'UTF-8') ?>" class="media-card__image">
                            <span class="media-card__badge badge text-bg-dark">Video</span>
                        <?php else: ?>
                            <img src="<?= htmlspecialchars($thumbnail, ENT_QUOTES, 'UTF-8') ?>" alt="<?= htmlspecialchars($title, ENT_QUOTES, 'UTF-8') ?>" class="media-card__image">
                            <span class="media-card__badge badge text-bg-light">Image</span>
                        <?php endif; ?>
                    </div>
                    <div class="card-body d-flex flex-column">
                        <h2 class="h6 mb-1 text-truncate" title="<?= htmlspecialchars($title, ENT_QUOTES, 'UTF-8') ?>"><?= htmlspecialchars($title, ENT_QUOTES, 'UTF-8') ?></h2>
                        <p class="small text-muted text-truncate mb-3" title="<?= htmlspecialchars($filename, ENT_QUOTES, 'UTF-8') ?>"><?= htmlspecialchars($filename, ENT_QUOTES, 'UTF-8') ?></p>
                        <dl class="row small mb-3">
                            <dt class="col-5 text-muted">Type</dt>
                            <dd class="col-7 mb-1 text-capitalize"><?= htmlspecialchars($type, ENT_QUOTES, 'UTF-8') ?></dd>
                            <dt class="col-5 text-muted">Size</dt>
                            <dd class="col-7 mb-1"><?= htmlspecialchars(number_format($sizeBytes / 1048576, 2), ENT_QUOTES, 'UTF-8') ?> MB</dd>
                            <dt class="col-5 text-muted">Duration</dt>
                            <dd class="col-7 mb-1"><?= htmlspecialchars($durationSeconds !== null ? gmdate('i:s', $durationSeconds) : 'N/A', ENT_QUOTES, 'UTF-8') ?></dd>
                            <dt class="col-5 text-muted">Created</dt>
                            <dd class="col-7 mb-0"><?= htmlspecialchars($createdAt !== '' ? date('d M Y', strtotime($createdAt)) : 'Unknown', ENT_QUOTES, 'UTF-8') ?></dd>
                        </dl>
                        <div class="d-flex gap-2 mt-auto">
                            <button type="button" class="btn btn-outline-primary btn-sm flex-fill" data-preview-trigger>Preview</button>
                            <button type="button" class="btn btn-outline-secondary btn-sm flex-fill" disabled>Replace</button>
                            <button type="button" class="btn btn-outline-danger btn-sm flex-fill" disabled>Delete</button>
                        </div>
                    </div>
                </article>
            </div>
        <?php endforeach; ?>
    </div>

    <div class="modal fade" id="uploadMediaModal" tabindex="-1" aria-labelledby="uploadMediaModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow">
                <div class="modal-header">
                    <h2 class="modal-title fs-5" id="uploadMediaModalLabel">Upload Media</h2>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form data-upload-form novalidate>
                        <div class="upload-dropzone mb-4" data-upload-dropzone tabindex="0">
                            <input type="file" class="d-none" data-upload-input accept=".jpg,.jpeg,.png,.webp,.mp4,.mov,.webm" <?= $currentUser === null ? 'disabled' : '' ?>>
                            <div class="upload-dropzone__inner text-center">
                                <div class="upload-dropzone__icon mb-3">+</div>
                                <h3 class="h5 mb-2">Drag and drop media here</h3>
                                <p class="text-muted mb-3">or choose an image or video from your device</p>
                                <button type="button" class="btn btn-outline-primary" data-upload-browse <?= $currentUser === null ? 'disabled' : '' ?>>Choose File</button>
                            </div>
                        </div>

                        <div class="row g-4 align-items-start">
                            <div class="col-md-5">
                                <div class="upload-preview card border-0 bg-body-tertiary">
                                    <div class="upload-preview__media" data-upload-preview>
                                        <div class="upload-preview__placeholder text-muted text-center px-4">
                                            Select a file to preview it here
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-7">
                                <div class="mb-3">
                                    <label for="mediaTitleInput" class="form-label">Title</label>
                                    <input id="mediaTitleInput" type="text" class="form-control" data-upload-title maxlength="180" placeholder="Enter a title">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Selected File</label>
                                    <div class="form-control bg-body-tertiary" data-upload-filename>None selected</div>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Upload Progress</label>
                                    <div class="progress" role="progressbar" aria-label="Upload progress" aria-valuemin="0" aria-valuemax="100">
                                        <div class="progress-bar progress-bar-striped progress-bar-animated" data-upload-progress style="width: 0%">0%</div>
                                    </div>
                                </div>
                                <div class="small text-muted">
                                    Supported formats: JPG, JPEG, PNG, WEBP, MP4, MOV, WEBM.
                                </div>
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal" data-upload-cancel>Cancel</button>
                    <button type="button" class="btn btn-primary" data-upload-submit <?= $currentUser === null ? 'disabled' : '' ?>>Upload</button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="mediaPreviewModal" tabindex="-1" aria-labelledby="mediaPreviewModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-xl">
            <div class="modal-content border-0 shadow">
                <div class="modal-header">
                    <h2 class="modal-title fs-5" id="mediaPreviewModalLabel" data-preview-title>Preview</h2>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div class="preview-modal__content" data-preview-content></div>
                </div>
            </div>
        </div>
    </div>
</section>
