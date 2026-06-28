<?php

declare(strict_types=1);

namespace App\Controllers\Web;

use App\Core\Controller;
use App\Core\Request;

class DashboardController extends Controller
{
    public function index(Request $request)
    {
        return $this->view('dashboard/index', ['pageTitle' => 'Dashboard']);
    }
}

