<?php

declare(strict_types=1);

use App\Controllers\Api\AuthController as ApiAuthController;
use App\Controllers\Api\DeviceController as ApiDeviceController;
use App\Controllers\Api\HeartbeatController as ApiHeartbeatController;
use App\Controllers\Api\ManifestController as ApiManifestController;
use App\Controllers\Api\MediaController as ApiMediaController;
use App\Controllers\Api\PairingController as ApiPairingController;
use App\Controllers\Api\PlaylistController as ApiPlaylistController;
use App\Controllers\Api\ScheduleController as ApiScheduleController;
use App\Controllers\Api\SyncController as ApiSyncController;
use App\Middleware\AuthMiddleware;
use App\Middleware\ApiMiddleware;
use App\Middleware\CsrfMiddleware;
use App\Middleware\DeviceAuthMiddleware;

$router->get('/api/v1/auth', ApiAuthController::class, [ApiMiddleware::class]);
$router->post('/api/v1/device/pairing/start', [ApiDeviceController::class, 'startPairing'], [ApiMiddleware::class]);
$router->post('/api/v1/device/pairing/status', [ApiDeviceController::class, 'pairingStatus'], [ApiMiddleware::class]);
$router->get('/api/v1/device/manifest', [ApiManifestController::class, 'show'], [ApiMiddleware::class, DeviceAuthMiddleware::class]);
$router->get('/api/v1/media', ApiMediaController::class, [ApiMiddleware::class]);
$router->post('/api/v1/media/upload', [ApiMediaController::class, 'upload'], [ApiMiddleware::class, AuthMiddleware::class, CsrfMiddleware::class]);
$router->get('/api/v1/playlists', ApiPlaylistController::class, [ApiMiddleware::class]);
$router->get('/api/v1/schedules', ApiScheduleController::class, [ApiMiddleware::class]);
$router->get('/api/v1/sync', ApiSyncController::class, [ApiMiddleware::class]);
$router->post('/api/v1/pairing/submit-code', [ApiPairingController::class, 'submitCode'], [ApiMiddleware::class, AuthMiddleware::class, CsrfMiddleware::class]);
$router->get('/api/v1/heartbeat', ApiHeartbeatController::class, [ApiMiddleware::class]);
