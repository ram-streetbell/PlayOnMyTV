<?php

declare(strict_types=1);

namespace App\Core;

use RuntimeException;

class Crypt
{
    public static function encrypt(string $plainText): string
    {
        $key = self::key();
        $iv = random_bytes(16);
        $cipherText = openssl_encrypt($plainText, 'AES-256-CBC', $key, OPENSSL_RAW_DATA, $iv);

        if ($cipherText === false) {
            throw new RuntimeException('Unable to encrypt payload.');
        }

        return base64_encode($iv . $cipherText);
    }

    public static function decrypt(string $payload): string
    {
        $decoded = base64_decode($payload, true);

        if ($decoded === false || strlen($decoded) <= 16) {
            throw new RuntimeException('Invalid encrypted payload.');
        }

        $iv = substr($decoded, 0, 16);
        $cipherText = substr($decoded, 16);
        $plainText = openssl_decrypt($cipherText, 'AES-256-CBC', self::key(), OPENSSL_RAW_DATA, $iv);

        if ($plainText === false) {
            throw new RuntimeException('Unable to decrypt payload.');
        }

        return $plainText;
    }

    private static function key(): string
    {
        $appKey = (string) config('security.app_key', '');

        if ($appKey === '') {
            throw new RuntimeException('APP_KEY must be configured.');
        }

        return hash('sha256', $appKey, true);
    }
}

