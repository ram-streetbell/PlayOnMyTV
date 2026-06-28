<?php

declare(strict_types=1);

namespace App\Controllers\Api;

use App\Core\ApiController;
use App\Core\Request;
use App\Services\AuthService;
use App\Services\MediaService;

class MediaController extends ApiController
{
    public function __construct(
        private readonly MediaService $mediaService = new MediaService(),
        private readonly AuthService $authService = new AuthService()
    ) {
    }

    public function __invoke(Request $request)
    {
        return $this->success([
            'module' => 'media',
            'status' => 'scaffold',
        ]);
    }

    public function upload(Request $request)
    {
        $user = $this->authService->currentUser();
        $file = $request->file('file') ?? $request->file('media');

        if (!is_array($file)) {
            return $this->json([
                'success' => false,
                'message' => 'No upload file was provided.',
            ], 422);
        }

        $result = $this->mediaService->upload($file, $request->all(), $user);

        return $this->json($result, 201);
    }
}
