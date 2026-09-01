<?php

declare(strict_types=1);

namespace App\Controllers\Web;

use App\Core\Controller;
use App\Core\Request;

class DownloadController extends Controller
{
    public function index(Request $request)
    {
        return $this->view('downloads/index', [
            'pageTitle' => 'Downloads',
            'version' => '1.1.0',
            'androidUrl' => 'https://github.com/ram-streetbell/PlayOnMyTV/releases/download/PlayOnMyTV/PlayOnMyTV-android.apk',
            'webosUrl' => 'https://github.com/ram-streetbell/PlayOnMyTV/releases/download/PlayOnMyTV/PlayOnMyTV-WebOs.ipk',
            'releaseUrl' => 'https://github.com/ram-streetbell/PlayOnMyTV/releases/tag/PlayOnMyTV',
        ], 'layouts/guest');
    }
}
