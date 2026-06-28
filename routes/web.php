<?php

declare(strict_types=1);

use App\Controllers\Web\AuthController;
use App\Controllers\Web\DashboardController;
use App\Controllers\Web\DeviceController;
use App\Controllers\Web\MediaController;
use App\Controllers\Web\PlaylistController;
use App\Controllers\Web\ScheduleController;
use App\Controllers\Web\SettingsController;

$router->get('/', [AuthController::class, 'login']);
$router->get('/login', [AuthController::class, 'login']);
$router->get('/dashboard', [DashboardController::class, 'index']);
$router->get('/devices', [DeviceController::class, 'index']);
$router->get('/media', [MediaController::class, 'index']);
$router->get('/playlists', [PlaylistController::class, 'index']);
$router->get('/schedules', [ScheduleController::class, 'index']);
$router->get('/settings', [SettingsController::class, 'index']);
