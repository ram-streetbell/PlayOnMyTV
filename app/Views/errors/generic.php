<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-md-6">
            <div class="card border-0 shadow-sm">
                <div class="card-body p-4 text-center">
                    <p class="text-muted small mb-2">Error <?= htmlspecialchars((string) ($status ?? 500), ENT_QUOTES, 'UTF-8') ?></p>
                    <h1 class="h3 mb-3">Something needs attention</h1>
                    <p class="mb-0 text-muted"><?= htmlspecialchars($message ?? 'Unexpected error.', ENT_QUOTES, 'UTF-8') ?></p>
                </div>
            </div>
        </div>
    </div>
</div>

