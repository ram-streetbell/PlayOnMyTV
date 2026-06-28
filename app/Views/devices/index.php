<section>
    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h1 class="h2 mb-1">Devices</h1>
            <p class="text-muted mb-0">Register Android TV screens using one-time pairing codes.</p>
        </div>
        <button
            type="button"
            class="btn btn-primary"
            data-bs-toggle="modal"
            data-bs-target="#pairDeviceModal"
            <?= $currentUser === null ? 'disabled' : '' ?>
        >
            Add Device
        </button>
    </div>

    <?php if ($currentUser === null): ?>
        <div class="alert alert-warning border-0 shadow-sm">
            Log in with an admin account before pairing devices.
        </div>
    <?php endif; ?>

    <div class="card border-0 shadow-sm">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th scope="col">Device</th>
                        <th scope="col">Status</th>
                        <th scope="col">App Version</th>
                        <th scope="col">Pairing Code</th>
                        <th scope="col">Last Seen</th>
                    </tr>
                    </thead>
                    <tbody>
                    <?php if (empty($devices)): ?>
                        <tr>
                            <td colspan="5" class="text-center text-muted py-4">No devices have been paired yet.</td>
                        </tr>
                    <?php else: ?>
                        <?php foreach ($devices as $device): ?>
                            <tr>
                                <td>
                                    <div class="fw-semibold"><?= htmlspecialchars((string) $device['device_name'], ENT_QUOTES, 'UTF-8') ?></div>
                                    <div class="small text-muted"><?= htmlspecialchars((string) $device['device_uuid'], ENT_QUOTES, 'UTF-8') ?></div>
                                </td>
                                <td>
                                    <span class="badge text-bg-<?= $device['status'] === 'active' ? 'success' : 'secondary' ?>">
                                        <?= htmlspecialchars((string) ucwords(str_replace('_', ' ', (string) $device['status'])), ENT_QUOTES, 'UTF-8') ?>
                                    </span>
                                </td>
                                <td><?= htmlspecialchars((string) $device['app_version'], ENT_QUOTES, 'UTF-8') ?></td>
                                <td>
                                    <?php if (!empty($device['pairing_code'])): ?>
                                        <div class="fw-semibold"><?= htmlspecialchars((string) $device['pairing_code'], ENT_QUOTES, 'UTF-8') ?></div>
                                        <div class="small text-muted">Expires <?= htmlspecialchars((string) $device['pairing_code_expires_at'], ENT_QUOTES, 'UTF-8') ?></div>
                                    <?php else: ?>
                                        <span class="text-muted">Paired</span>
                                    <?php endif; ?>
                                </td>
                                <td><?= htmlspecialchars((string) ($device['last_seen_at'] ?? 'Never'), ENT_QUOTES, 'UTF-8') ?></td>
                            </tr>
                        <?php endforeach; ?>
                    <?php endif; ?>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <div class="modal fade" id="pairDeviceModal" tabindex="-1" aria-labelledby="pairDeviceModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content border-0 shadow">
                <div class="modal-header">
                    <h2 class="modal-title fs-5" id="pairDeviceModalLabel">Pair Device</h2>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div id="pairingAlert" class="alert d-none" role="alert"></div>
                    <div class="mb-3">
                        <label for="pairingCodeInput" class="form-label">Pairing Code</label>
                        <input
                            id="pairingCodeInput"
                            type="text"
                            class="form-control form-control-lg text-uppercase"
                            maxlength="6"
                            placeholder="AB7X92"
                            <?= $currentUser === null ? 'disabled' : '' ?>
                        >
                        <div class="form-text">Enter the code currently shown on the Android TV screen.</div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-primary" id="pairDeviceButton" <?= $currentUser === null ? 'disabled' : '' ?>>Pair Device</button>
                </div>
            </div>
        </div>
    </div>
</section>

<script>
document.addEventListener('DOMContentLoaded', () => {
    const button = document.getElementById('pairDeviceButton');
    const input = document.getElementById('pairingCodeInput');
    const alertBox = document.getElementById('pairingAlert');

    if (!button || !input || !alertBox || button.disabled) {
        return;
    }

    button.addEventListener('click', async () => {
        const pairingCode = input.value.trim().toUpperCase();
        const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content') ?? '';

        alertBox.className = 'alert d-none';
        alertBox.textContent = '';

        try {
            const response = await fetch('/api/v1/pairing/submit-code', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: JSON.stringify({ pairing_code: pairingCode })
            });

            const payload = await response.json();

            if (!response.ok || !payload.success) {
                const message = payload.message || 'Unable to pair device.';
                throw new Error(message);
            }

            alertBox.className = 'alert alert-success';
            alertBox.textContent = payload.data.message;
            window.setTimeout(() => window.location.reload(), 900);
        } catch (error) {
            alertBox.className = 'alert alert-danger';
            alertBox.textContent = error.message || 'Unable to pair device.';
        }
    });
});
</script>
