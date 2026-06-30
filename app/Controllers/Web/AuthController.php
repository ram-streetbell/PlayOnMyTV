<?php

declare(strict_types=1);

namespace App\Controllers\Web;

use App\Core\Controller;
use App\Core\Request;
use App\Core\Response;
use App\Repositories\UserRepository;

class AuthController extends Controller
{
    public function login(Request $request)
    {
        // Development-only auto login
        $user = (new UserRepository())->findActiveById(1);

        if ($user !== null) {
            $_SESSION['user'] = $user;
            $_SESSION['user_id'] = $user['id'];

            return Response::redirect('/dashboard');
        }

        return $this->view('auth/login', [
            'pageTitle' => 'Login',
        ], 'layouts/guest');
    }
}