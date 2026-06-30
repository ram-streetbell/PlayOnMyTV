<?php

try {
    $pdo = new PDO(
        "mysql:host=127.0.0.1;dbname=playonmytv",
        "root",
        ""
    );

    echo "Connected";
} catch (Throwable $e) {
    echo $e->getMessage();
}