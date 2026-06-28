<?php

declare(strict_types=1);

namespace App\Controllers\Web;

use App\Core\Controller;
use App\Core\Request;
use App\Services\AuthService;
use App\Services\MediaService;

class MediaController extends Controller
{
    public function __construct(
        private readonly MediaService $mediaService = new MediaService(),
        private readonly AuthService $authService = new AuthService()
    ) {
    }

    public function index(Request $request)
    {
        $currentUser = $this->authService->currentUserOrNull();
        $mediaItems = $currentUser !== null
            ? $this->mediaService->listForBusiness((int) $currentUser['business_id'])
            : [];

        return $this->view('media/index', [
            'pageTitle' => 'Media Library',
            'currentUser' => $currentUser,
            'mediaItems' => $mediaItems,
            'pageStyles' => [
                asset('assets/css/media-library.css'),
            ],
            'pageScripts' => [
                asset('assets/js/media-library.js'),
            ],
        ]);
    }
}
