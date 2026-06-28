<?php

declare(strict_types=1);

namespace App\Controllers\Web;

use App\Core\Controller;
use App\Core\Request;

class AuthController extends Controller
{
    public function login(Request $request)
    {
        return $this->view('auth/login', [
            'pageTitle' => 'Login',
        ], 'layouts/guest');
    }
}

