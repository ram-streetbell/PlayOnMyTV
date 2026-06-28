<?php

declare(strict_types=1);

namespace App\Core;

class SessionManager
{
    public function start(): void
    {
        if (session_status() === PHP_SESSION_ACTIVE) {
            return;
        }

        session_name(config('session.name', 'playonmytv_session'));
        session_set_cookie_params([
            'lifetime' => config('session.lifetime', 120) * 60,
            'path' => '/',
            'httponly' => true,
            'samesite' => 'Lax',
        ]);

        session_start();
    }

    public function regenerate(): void
    {
        session_regenerate_id(true);
    }

    public function destroy(): void
    {
        $_SESSION = [];

        if (session_status() === PHP_SESSION_ACTIVE) {
            session_destroy();
        }
    }

    public function terminateRequest(): void
    {
        unset($_SESSION['_old']);
    }
}

