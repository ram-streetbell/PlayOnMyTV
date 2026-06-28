<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="csrf-token" content="<?= htmlspecialchars(csrf_token(), ENT_QUOTES, 'UTF-8') ?>">
    <title><?= htmlspecialchars(($pageTitle ?? 'PlayOnMyTV') . ' | PlayOnMyTV', ENT_QUOTES, 'UTF-8') ?></title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="<?= asset('assets/css/app.css') ?>">
    <?php foreach (($pageStyles ?? []) as $pageStyle): ?>
        <link rel="stylesheet" href="<?= htmlspecialchars((string) $pageStyle, ENT_QUOTES, 'UTF-8') ?>">
    <?php endforeach; ?>
</head>
<body class="bg-body-tertiary">
    <?php require base_path('app/Views/partials/header.php'); ?>
    <div class="container-fluid">
        <div class="row min-vh-100">
            <?php require base_path('app/Views/partials/sidebar.php'); ?>
            <main class="col-lg-10 ms-sm-auto px-md-4 py-4">
                <?= $content ?? '' ?>
            </main>
        </div>
    </div>
    <?php require base_path('app/Views/partials/footer.php'); ?>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="<?= asset('assets/js/app.js') ?>"></script>
    <?php foreach (($pageScripts ?? []) as $pageScript): ?>
        <script src="<?= htmlspecialchars((string) $pageScript, ENT_QUOTES, 'UTF-8') ?>"></script>
    <?php endforeach; ?>
</body>
</html>
