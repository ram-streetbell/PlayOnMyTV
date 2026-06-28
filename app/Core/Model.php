<?php

declare(strict_types=1);

namespace App\Core;

use PDO;

abstract class Model
{
    protected PDO $connection;
    protected string $table = '';

    public function __construct()
    {
        $this->connection = Database::connection();
    }

    public function table(): string
    {
        return $this->table;
    }
}

