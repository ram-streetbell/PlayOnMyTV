<?php

declare(strict_types=1);

use App\Core\Application;
use App\Core\DotEnv;
use App\Core\ExceptionHandler;
use App\Core\Router;
use App\Core\SessionManager;

require_once dirname(__DIR__) . '/vendor/autoload.php';

$basePath = dirname(__DIR__);

$dotenv = new DotEnv($basePath . '/.env');
$dotenv->load();

date_default_timezone_set(env('APP_TIMEZONE', 'UTC'));

$sessionManager = new SessionManager();
$sessionManager->start();

$router = new Router();

require $basePath . '/routes/web.php';
require $basePath . '/routes/api.php';

ExceptionHandler::register();

return new Application($basePath, $router, $sessionManager);

