<?php

declare(strict_types=1);

namespace App\Repositories;

class UserRepository extends BaseRepository
{
    public function findActiveById(int $userId): ?array
    {
        $statement = $this->connection->prepare(
            'SELECT id, business_id, name, email, role, status
             FROM users
             WHERE id = :id AND status = :status
             LIMIT 1'
        );
        $statement->execute([
            'id' => $userId,
            'status' => 'active',
        ]);

        $user = $statement->fetch();

        return is_array($user) ? $user : null;
    }
}
