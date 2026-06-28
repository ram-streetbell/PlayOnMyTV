# PlayOnMyTV Architecture Blueprint

## 1. Executive Summary

PlayOnMyTV is a production-grade, offline-first digital signage SaaS platform composed of:

1. A PHP 8.2 web-based Admin Portal hosted on cPanel
2. A REST API returning JSON
3. An Android TV player application written in Kotlin

Primary business goal:
Allow businesses to manage media, playlists, schedules, and Android TV devices from a web portal while ensuring the TV always plays downloaded local content, even when connectivity is unavailable.

Core architectural principles:

1. Layered architecture with strict separation of concerns
2. Offline-first playback on Android TV
3. Storage abstraction for media providers
4. Incremental synchronization using versioned change tracking
5. Tenant isolation by business/account
6. Security-first design using prepared statements, hashed passwords, scoped tokens, and auditable actions

## 2. Functional Scope

### Admin Portal

Modules:

1. Authentication
2. Dashboard
3. Devices
4. Media Library
5. Playlists
6. Schedules
7. Settings
8. Profile

### Device Player

Capabilities:

1. First-run pairing by temporary code
2. Permanent device token after pairing
3. Local storage of media, playlists, and schedules
4. Local playback only
5. Periodic sync for delta changes
6. Status reporting and heartbeat
7. Automatic scheduled playlist switching

### API

Responsibilities:

1. Authentication and session/token issuance
2. Device pairing and token lifecycle
3. CRUD for media, playlists, schedules, devices, and assignments
4. Delta sync generation
5. Status ingestion from devices
6. Signed or provider-backed media download URLs

## 3. Non-Functional Requirements

1. Production-ready and maintainable
2. Compatible with shared hosting or cPanel-based deployment
3. Secure by default
4. Horizontally evolvable, even if initial deployment is single-server
5. Resilient to temporary network failures
6. Auditable for operational debugging
7. Extensible storage layer supporting Cloudinary initially and S3 later
8. API versioning from the start

## 4. High-Level Architecture

### Logical Components

1. Presentation Layer
   - Admin HTML views
   - Bootstrap 5 UI
   - JavaScript/AJAX interactions

2. Web Application Layer
   - Controllers for portal requests
   - Form handling, validation, session-aware routing

3. API Layer
   - JSON controllers
   - Request/response serialization
   - Device and admin endpoints

4. Application/Service Layer
   - Media service
   - Playlist service
   - Schedule service
   - Device service
   - Pairing service
   - Sync service
   - Auth service

5. Domain/Model Layer
   - Entities and business rules
   - Validation rules
   - Synchronization contracts

6. Data Access Layer
   - Repositories
   - PDO-based prepared statements only
   - Transaction boundaries

7. Infrastructure Layer
   - Database connection manager
   - Storage provider adapters
   - Logging
   - Configuration
   - Token generation
   - File upload helpers

### Recommended Deployment View

1. cPanel-hosted PHP app
   - Admin portal and REST API in one codebase
   - Public entry points separated by route groups

2. MySQL 8 database
   - Shared by admin and API layers

3. Cloudinary media storage
   - Original media stored remotely
   - Device downloads use signed or controlled URLs

4. Android TV client
   - Local Room database
   - Local media file cache
   - Background sync with WorkManager

## 5. Suggested Backend Directory Structure

```text
playonmytv/
├── app/
│   ├── Config/
│   │   ├── app.php
│   │   ├── database.php
│   │   ├── storage.php
│   │   └── routes.php
│   ├── Core/
│   │   ├── Application.php
│   │   ├── Router.php
│   │   ├── Request.php
│   │   ├── Response.php
│   │   ├── View.php
│   │   ├── Controller.php
│   │   ├── ApiController.php
│   │   ├── Database.php
│   │   ├── Auth.php
│   │   ├── Validator.php
│   │   ├── Logger.php
│   │   └── Exceptions/
│   ├── Domain/
│   │   ├── Auth/
│   │   ├── Device/
│   │   ├── Media/
│   │   ├── Playlist/
│   │   ├── Schedule/
│   │   ├── Sync/
│   │   └── Shared/
│   ├── DTO/
│   ├── Services/
│   │   ├── AuthService.php
│   │   ├── DeviceService.php
│   │   ├── PairingService.php
│   │   ├── MediaService.php
│   │   ├── PlaylistService.php
│   │   ├── ScheduleService.php
│   │   ├── SyncService.php
│   │   └── DashboardService.php
│   ├── Repositories/
│   │   ├── UserRepository.php
│   │   ├── BusinessRepository.php
│   │   ├── DeviceRepository.php
│   │   ├── MediaRepository.php
│   │   ├── PlaylistRepository.php
│   │   ├── ScheduleRepository.php
│   │   ├── AssignmentRepository.php
│   │   └── SyncRepository.php
│   ├── Storage/
│   │   ├── Contracts/
│   │   │   └── MediaStorageInterface.php
│   │   ├── Cloudinary/
│   │   │   └── CloudinaryStorage.php
│   │   ├── S3/
│   │   │   └── S3Storage.php
│   │   └── StorageManager.php
│   ├── Http/
│   │   ├── Middleware/
│   │   │   ├── AuthMiddleware.php
│   │   │   ├── ApiAuthMiddleware.php
│   │   │   ├── DeviceAuthMiddleware.php
│   │   │   └── CsrfMiddleware.php
│   │   ├── Controllers/
│   │   │   ├── Web/
│   │   │   │   ├── AuthController.php
│   │   │   │   ├── DashboardController.php
│   │   │   │   ├── DeviceController.php
│   │   │   │   ├── MediaController.php
│   │   │   │   ├── PlaylistController.php
│   │   │   │   ├── ScheduleController.php
│   │   │   │   ├── SettingsController.php
│   │   │   │   └── ProfileController.php
│   │   │   └── Api/
│   │   │       ├── AuthApiController.php
│   │   │       ├── DeviceApiController.php
│   │   │       ├── PairingApiController.php
│   │   │       ├── MediaApiController.php
│   │   │       ├── PlaylistApiController.php
│   │   │       ├── ScheduleApiController.php
│   │   │       ├── SyncApiController.php
│   │   │       └── StatusApiController.php
│   └── Views/
│       ├── layouts/
│       ├── partials/
│       ├── auth/
│       ├── dashboard/
│       ├── devices/
│       ├── media/
│       ├── playlists/
│       ├── schedules/
│       ├── settings/
│       └── profile/
├── public/
│   ├── index.php
│   ├── assets/
│   │   ├── css/
│   │   ├── js/
│   │   ├── img/
│   │   └── vendors/
│   └── uploads/
├── routes/
│   ├── web.php
│   └── api.php
├── storage/
│   ├── logs/
│   ├── cache/
│   └── temp/
├── database/
│   ├── migrations/
│   ├── seeds/
│   └── schema/
├── tests/
│   ├── Unit/
│   ├── Integration/
│   └── Api/
├── docs/
│   ├── architecture-blueprint.md
│   ├── api-spec.md
│   ├── database-schema.md
│   └── deployment.md
├── scripts/
├── composer.json
└── README.md
```

Notes:

1. `public/` is the cPanel document root.
2. Admin portal and API live in the same application but stay separated by controllers, middleware, and routes.
3. SQL stays in repositories only.

## 6. Android TV Project Structure

```text
android-tv/
├── app/
│   ├── src/main/java/com/playonmytv/
│   │   ├── app/
│   │   │   ├── PlayOnMyTvApplication.kt
│   │   │   ├── di/
│   │   │   └── config/
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── db/
│   │   │   │   ├── dao/
│   │   │   │   ├── entities/
│   │   │   │   └── preferences/
│   │   │   ├── remote/
│   │   │   ├── repository/
│   │   │   └── sync/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── usecase/
│   │   ├── player/
│   │   │   ├── playback/
│   │   │   ├── scheduler/
│   │   │   ├── download/
│   │   │   └── cache/
│   │   ├── pairing/
│   │   ├── status/
│   │   ├── ui/
│   │   │   ├── pairing/
│   │   │   ├── player/
│   │   │   ├── settings/
│   │   │   └── diagnostics/
│   │   ├── worker/
│   │   │   ├── SyncWorker.kt
│   │   │   ├── DownloadWorker.kt
│   │   │   └── HeartbeatWorker.kt
│   │   └── util/
│   ├── src/main/res/
│   └── src/test/
└── build.gradle
```

Android architecture style:

1. Clean-ish layered approach
2. Repository pattern
3. Room for local truth
4. WorkManager for background sync
5. ExoPlayer for local playback
6. A single playback coordinator that decides what should play now from local schedule and playlist data

## 7. Database Schema Design

### Multi-Tenant Strategy

Use `businesses` as the tenant root. Nearly all business-owned records include `business_id`.

### Core Tables

#### 7.1 businesses

Purpose:
Represents a customer account or business tenant.

Columns:

1. `id` BIGINT PK
2. `name` VARCHAR(150)
3. `slug` VARCHAR(180) UNIQUE
4. `status` ENUM('active','suspended')
5. `timezone` VARCHAR(64)
6. `contact_email` VARCHAR(190)
7. `created_at` DATETIME
8. `updated_at` DATETIME

#### 7.2 users

Purpose:
Admin users who log into the portal.

Columns:

1. `id` BIGINT PK
2. `business_id` BIGINT FK
3. `name` VARCHAR(150)
4. `email` VARCHAR(190) UNIQUE
5. `password_hash` VARCHAR(255)
6. `role` ENUM('owner','admin','editor','viewer')
7. `status` ENUM('active','inactive')
8. `last_login_at` DATETIME NULL
9. `created_at` DATETIME
10. `updated_at` DATETIME

#### 7.3 devices

Purpose:
Registered Android TV devices.

Columns:

1. `id` BIGINT PK
2. `business_id` BIGINT FK NULL until paired
3. `device_uuid` CHAR(36) UNIQUE
4. `device_name` VARCHAR(150)
5. `platform` VARCHAR(50)
6. `app_version` VARCHAR(50)
7. `firmware_version` VARCHAR(100) NULL
8. `pairing_code` VARCHAR(12) NULL
9. `pairing_code_expires_at` DATETIME NULL
10. `device_token_hash` VARCHAR(255) NULL
11. `status` ENUM('pending_pairing','active','inactive','revoked')
12. `last_seen_at` DATETIME NULL
13. `last_sync_at` DATETIME NULL
14. `last_sync_version` BIGINT DEFAULT 0
15. `storage_free_bytes` BIGINT NULL
16. `screen_resolution` VARCHAR(50) NULL
17. `timezone` VARCHAR(64) NULL
18. `created_at` DATETIME
19. `updated_at` DATETIME

#### 7.4 device_heartbeats

Purpose:
Historical device health snapshots.

Columns:

1. `id` BIGINT PK
2. `device_id` BIGINT FK
3. `online_status` ENUM('online','offline','degraded')
4. `current_playlist_id` BIGINT NULL
5. `current_media_id` BIGINT NULL
6. `free_storage_bytes` BIGINT NULL
7. `network_type` VARCHAR(50) NULL
8. `battery_level` INT NULL
9. `app_version` VARCHAR(50) NULL
10. `payload_json` JSON NULL
11. `created_at` DATETIME

#### 7.5 media

Purpose:
Uploaded images and videos.

Columns:

1. `id` BIGINT PK
2. `business_id` BIGINT FK
3. `title` VARCHAR(180)
4. `description` TEXT NULL
5. `media_type` ENUM('image','video')
6. `mime_type` VARCHAR(120)
7. `file_size_bytes` BIGINT
8. `duration_seconds` INT NULL for videos
9. `width` INT NULL
10. `height` INT NULL
11. `checksum_sha256` CHAR(64)
12. `storage_provider` VARCHAR(50)
13. `storage_key` VARCHAR(255)
14. `storage_url` TEXT
15. `thumbnail_url` TEXT NULL
16. `status` ENUM('processing','ready','failed','deleted')
17. `created_by` BIGINT FK users.id
18. `created_at` DATETIME
19. `updated_at` DATETIME
20. `deleted_at` DATETIME NULL

#### 7.6 playlists

Purpose:
Business playlists.

Columns:

1. `id` BIGINT PK
2. `business_id` BIGINT FK
3. `name` VARCHAR(180)
4. `description` TEXT NULL
5. `status` ENUM('draft','active','archived')
6. `is_looping` TINYINT(1) DEFAULT 1
7. `created_by` BIGINT FK users.id
8. `created_at` DATETIME
9. `updated_at` DATETIME

#### 7.7 playlist_items

Purpose:
Ordered media items inside a playlist.

Columns:

1. `id` BIGINT PK
2. `playlist_id` BIGINT FK
3. `media_id` BIGINT FK
4. `sort_order` INT
5. `image_duration_seconds` INT NULL
6. `created_at` DATETIME
7. `updated_at` DATETIME

Rules:

1. Videos ignore `image_duration_seconds`
2. Images must have a positive display duration

#### 7.8 schedules

Purpose:
Named schedule collections assignable to devices.

Columns:

1. `id` BIGINT PK
2. `business_id` BIGINT FK
3. `name` VARCHAR(180)
4. `description` TEXT NULL
5. `status` ENUM('draft','active','archived')
6. `timezone` VARCHAR(64)
7. `created_by` BIGINT FK users.id
8. `created_at` DATETIME
9. `updated_at` DATETIME

#### 7.9 schedule_slots

Purpose:
Time ranges mapped to playlists.

Columns:

1. `id` BIGINT PK
2. `schedule_id` BIGINT FK
3. `playlist_id` BIGINT FK
4. `day_of_week` TINYINT
5. `start_time` TIME
6. `end_time` TIME
7. `priority` INT DEFAULT 0
8. `created_at` DATETIME
9. `updated_at` DATETIME

Rules:

1. One schedule can contain multiple daily slots
2. Validate overlapping slots according to business rules
3. Cross-midnight support may require either split rows or explicit overnight handling

#### 7.10 device_schedule_assignments

Purpose:
Assigns one active schedule to a device, with future extensibility.

Columns:

1. `id` BIGINT PK
2. `device_id` BIGINT FK
3. `schedule_id` BIGINT FK
4. `is_active` TINYINT(1) DEFAULT 1
5. `assigned_by` BIGINT FK users.id
6. `assigned_at` DATETIME
7. `created_at` DATETIME
8. `updated_at` DATETIME

#### 7.11 pairing_sessions

Purpose:
Tracks temporary pairing lifecycle.

Columns:

1. `id` BIGINT PK
2. `device_id` BIGINT FK
3. `pairing_code` VARCHAR(12)
4. `status` ENUM('pending','paired','expired','cancelled')
5. `expires_at` DATETIME
6. `paired_by_user_id` BIGINT FK users.id NULL
7. `paired_at` DATETIME NULL
8. `created_at` DATETIME

#### 7.12 api_tokens

Purpose:
Optional admin API tokens and internal service tokens if needed later.

Columns:

1. `id` BIGINT PK
2. `business_id` BIGINT FK
3. `user_id` BIGINT FK NULL
4. `token_name` VARCHAR(100)
5. `token_hash` VARCHAR(255)
6. `scope` VARCHAR(255)
7. `last_used_at` DATETIME NULL
8. `expires_at` DATETIME NULL
9. `created_at` DATETIME

#### 7.13 sync_changes

Purpose:
Central change log used for device delta synchronization.

Columns:

1. `id` BIGINT PK AUTO_INCREMENT
2. `business_id` BIGINT FK
3. `entity_type` ENUM('media','playlist','playlist_item','schedule','schedule_slot','device_assignment')
4. `entity_id` BIGINT
5. `change_type` ENUM('created','updated','deleted')
6. `version_no` BIGINT
7. `payload_json` JSON NULL
8. `created_at` DATETIME

Important note:
`version_no` should be monotonically increasing. Simplest approach: use `id` as sync version.

#### 7.14 audit_logs

Purpose:
Operational and security audit trail.

Columns:

1. `id` BIGINT PK
2. `business_id` BIGINT FK NULL
3. `user_id` BIGINT FK NULL
4. `action` VARCHAR(100)
5. `entity_type` VARCHAR(50)
6. `entity_id` BIGINT NULL
7. `ip_address` VARCHAR(45) NULL
8. `user_agent` VARCHAR(255) NULL
9. `metadata_json` JSON NULL
10. `created_at` DATETIME

## 8. Database Relationship Summary

1. One `business` has many `users`
2. One `business` has many `devices`
3. One `business` has many `media`
4. One `business` has many `playlists`
5. One `playlist` has many `playlist_items`
6. One `business` has many `schedules`
7. One `schedule` has many `schedule_slots`
8. One `device` has one active schedule assignment
9. Sync changes are business-scoped and consumed by devices belonging to that business

## 9. REST API Design

Base path:

```text
/api/v1
```

### 9.1 Admin Authentication

1. `POST /auth/login`
2. `POST /auth/logout`
3. `GET /auth/me`
4. `POST /auth/forgot-password`
5. `POST /auth/reset-password`

### 9.2 Admin Dashboard

1. `GET /dashboard/summary`

Returns:

1. Device counts
2. Online/offline summary
3. Recent sync failures
4. Recent uploads

### 9.3 Devices

1. `GET /devices`
2. `POST /devices`
3. `GET /devices/{id}`
4. `PUT /devices/{id}`
5. `DELETE /devices/{id}`
6. `POST /devices/{id}/rename`
7. `POST /devices/{id}/revoke-token`
8. `POST /devices/{id}/unpair`
9. `GET /devices/{id}/status`
10. `GET /devices/{id}/heartbeats`
11. `POST /devices/{id}/assign-schedule`
12. `DELETE /devices/{id}/assign-schedule`

### 9.4 Pairing

1. `POST /pairing/submit-code`
   - Admin submits device pairing code
2. `POST /device/pairing/start`
   - Device requests a new pairing code
3. `GET /device/pairing/status/{code}`
   - Device checks whether pairing completed
4. `POST /device/pairing/confirm`
   - Device exchanges pairing confirmation for permanent device token

Recommended simplification:
Use polling by the device until paired.

### 9.5 Media

1. `GET /media`
2. `POST /media`
3. `GET /media/{id}`
4. `PUT /media/{id}`
5. `DELETE /media/{id}`
6. `POST /media/{id}/replace`
7. `GET /media/{id}/download`
8. `GET /media/{id}/usage`

### 9.6 Playlists

1. `GET /playlists`
2. `POST /playlists`
3. `GET /playlists/{id}`
4. `PUT /playlists/{id}`
5. `DELETE /playlists/{id}`
6. `GET /playlists/{id}/items`
7. `POST /playlists/{id}/items`
8. `PUT /playlists/{id}/items/{itemId}`
9. `DELETE /playlists/{id}/items/{itemId}`
10. `POST /playlists/{id}/reorder`

### 9.7 Schedules

1. `GET /schedules`
2. `POST /schedules`
3. `GET /schedules/{id}`
4. `PUT /schedules/{id}`
5. `DELETE /schedules/{id}`
6. `GET /schedules/{id}/slots`
7. `POST /schedules/{id}/slots`
8. `PUT /schedules/{id}/slots/{slotId}`
9. `DELETE /schedules/{id}/slots/{slotId}`
10. `POST /schedules/{id}/validate`

### 9.8 Device Sync and Status

1. `POST /device/auth/token`
   - Device re-auth or token refresh if implemented
2. `POST /device/heartbeat`
3. `POST /device/sync`
4. `POST /device/downloads/report`
5. `POST /device/playback/report`
6. `GET /device/config`

### 9.9 Settings/Profile

1. `GET /settings`
2. `PUT /settings`
3. `GET /profile`
4. `PUT /profile`
5. `PUT /profile/password`

## 10. Example Sync Contract

Request:

```json
{
  "device_uuid": "3ce6bfa4-2664-4f4d-bc43-dafe5a4c00aa",
  "last_sync_version": 1250,
  "installed_media": [
    {
      "media_id": 10,
      "checksum_sha256": "abc123"
    }
  ],
  "app_version": "1.0.0",
  "device_time": "2026-06-28T14:00:00+05:30"
}
```

Response:

```json
{
  "server_time": "2026-06-28T14:00:02+05:30",
  "sync_version": 1288,
  "full_resync_required": false,
  "changes": {
    "media_upserts": [],
    "media_deletes": [],
    "playlist_upserts": [],
    "playlist_deletes": [],
    "schedule_upserts": [],
    "schedule_deletes": [],
    "assignments": []
  }
}
```

## 11. Synchronization Strategy

### Principles

1. Playback must never depend on live network streaming
2. Device local state is authoritative for playback while offline
3. Server is authoritative for intended configuration
4. Sync should be incremental and idempotent

### Recommended Approach

1. Every change to media, playlists, schedule slots, or assignments writes an entry to `sync_changes`
2. Each device stores `last_sync_version`
3. Device sends `last_sync_version` during sync
4. Server returns all changes with `version_no > last_sync_version`
5. Device applies them in order within a local transaction where possible

### Media Sync Flow

1. Server sends metadata for new or changed media
2. Device compares checksum and local file presence
3. Device downloads missing or changed files
4. Device verifies checksum
5. Device updates local Room records only after successful download
6. If a media file is deleted on server, device removes local reference and file only when no current playback depends on it

### Playlist/ Schedule Sync Flow

1. Device receives playlist and slot upserts
2. Device updates Room tables
3. Playback coordinator recalculates active playlist
4. Switching occurs safely at media boundary except when a hard cut policy is required

### Full Resync Conditions

Use `full_resync_required = true` when:

1. Device `last_sync_version` is too old
2. Local state is inconsistent
3. Major schema/version migration occurred
4. Server-side repair or manual reset was performed

### Conflict Model

1. Server-owned entities are effectively read-only on the device
2. Device only writes operational state such as heartbeat, installed-media report, playback report
3. Therefore, business data conflicts are minimal

## 12. Android TV Architecture

### Key Modules

1. Pairing Module
   - Generates or requests pairing code
   - Polls pairing status
   - Persists device token securely

2. Sync Module
   - Runs periodically using WorkManager
   - Requests delta changes
   - Coordinates metadata updates and downloads

3. Download Manager
   - Downloads files in background
   - Verifies checksums
   - Stores to app-private local storage

4. Local Data Layer
   - Room entities for media, playlists, playlist items, schedules, assignments, sync state

5. Playback Engine
   - Uses ExoPlayer
   - Plays local images and local videos
   - Supports image duration timers
   - Handles loop progression

6. Schedule Evaluator
   - Computes which playlist should be active based on current local time and timezone

7. Heartbeat/Diagnostics Module
   - Sends health, storage, playback, and last-seen data

### Android Local Tables

Recommended Room tables:

1. `device_config`
2. `local_media`
3. `local_playlists`
4. `local_playlist_items`
5. `local_schedules`
6. `local_schedule_slots`
7. `local_assignments`
8. `sync_state`
9. `download_queue`
10. `playback_state`

### Playback Decision Logic

1. Determine active schedule assigned to device
2. Evaluate current day/time against schedule slots
3. Resolve active playlist
4. Read ordered playlist items from local DB
5. For each item:
   - Video: play local file until completion
   - Image: display local file for configured duration
6. Repeat loop until schedule changes

### Offline-First Guarantees

1. If network fails, playback continues from existing local files
2. If sync fails, device retries later without interrupting player
3. If a scheduled asset is missing, fallback rules should exist

Recommended fallback priority:

1. Continue current cached playlist
2. Use last known valid assigned playlist
3. Use emergency fallback playlist or placeholder screen

## 13. Admin Portal Architecture

### Web Layer Pattern

Use server-rendered PHP views with AJAX for interactive CRUD.

Recommended boundaries:

1. Web controllers render pages
2. AJAX endpoints call services and return JSON fragments or status payloads
3. Views remain presentation-only
4. Business logic stays in services
5. Repositories handle all persistence

### Page-Level Module Intent

1. Dashboard
   - KPIs, recent activity, device health summary

2. Devices
   - List devices
   - Pair device by code
   - View status and last sync
   - Assign schedule
   - Revoke or unpair

3. Media Library
   - Upload media
   - View previews and metadata
   - Replace or delete media
   - Search and filter

4. Playlists
   - Create playlist
   - Add media items
   - Reorder items
   - Set image durations

5. Schedules
   - Create day/time slots
   - Validate overlaps
   - Assign playlist per slot

6. Settings
   - Business profile
   - Timezone
   - Branding basics

7. Profile
   - User details
   - Password changes

### Frontend Approach

1. Bootstrap 5 for layout and components
2. Vanilla JavaScript modules for each page
3. AJAX for CRUD actions without excessive reloads
4. Shared form validation patterns
5. Shared alert/toast component for success/failure messages

## 14. Storage Abstraction Design

Define a storage interface that hides provider implementation details.

### Interface Responsibilities

1. Upload file
2. Delete file
3. Replace file
4. Generate delivery URL
5. Generate secure download URL if needed
6. Fetch metadata if required

### Example Contract

```php
interface MediaStorageInterface
{
    public function upload(string $localPath, array $options = []): StorageUploadResult;
    public function delete(string $storageKey): bool;
    public function replace(string $storageKey, string $localPath, array $options = []): StorageUploadResult;
    public function getDeliveryUrl(string $storageKey, array $options = []): string;
    public function getSignedDownloadUrl(string $storageKey, int $ttlSeconds = 900): string;
}
```

Design rules:

1. Database stores `storage_provider` and `storage_key`
2. Business logic never depends directly on Cloudinary SDK details
3. Switching to S3 later should only affect the adapter and configuration

## 15. Security Architecture

1. Passwords hashed using `password_hash()`
2. Prepared statements only through PDO
3. CSRF protection for web forms
4. Session hardening for portal login
5. Device tokens stored hashed server-side
6. Pairing codes short-lived and rate-limited
7. Role-based authorization for admin features
8. Input validation and output escaping everywhere
9. File upload validation for MIME type, extension, size, and scanning if possible
10. Audit logging for sensitive actions

## 16. Missing Requirements and Recommended Clarifications

The following items should be finalized before implementation starts:

1. Subscription model
   - Single business per account or multi-location hierarchy?

2. Role permissions
   - Exact capabilities for owner, admin, editor, viewer

3. Device naming and grouping
   - Need device groups, tags, or locations?

4. Schedule semantics
   - How should overlapping slots be resolved?
   - Are date-range campaigns needed in addition to weekly recurring slots?

5. Timezone policy
   - Use business timezone, device timezone, or schedule timezone as source of truth?

6. Content fallback policy
   - What should display if current scheduled playlist has missing local files?

7. Media processing
   - Should videos/images be transcoded or resized before delivery to TVs?

8. Reporting depth
   - Is proof-of-play reporting required for ad compliance?

9. Real-time status definition
   - What heartbeat interval counts as online vs offline?

10. Device token lifecycle
   - Expiring tokens or permanent until revoked?

11. Soft delete policy
   - Which entities are restorable and for how long?

12. API rate limiting and abuse protection
   - Especially pairing and device endpoints

13. Maximum media library/storage quotas
   - Needed for commercial SaaS controls

14. Monitoring/alerting
   - Email alerts for offline screens or failed syncs?

15. Disaster recovery
   - Backup policy for MySQL and media metadata

## 17. Recommended Enhancements

1. Add `device_groups` for assigning schedules to many TVs at once
2. Add `campaigns` with start/end dates
3. Add proof-of-play analytics
4. Add remote screenshot capture if platform allows
5. Add emergency override playlist
6. Add media pre-validation and recommended aspect-ratio warnings
7. Add app-update management and minimum supported device version checks

## 18. Phased Implementation Plan

### Phase 0: Foundation

1. Initialize repositories
2. Establish coding standards
3. Set up PHP project skeleton
4. Define environment/config loading
5. Set up MySQL schema migration approach
6. Define API response format and error contract
7. Create Android app skeleton and dependency baseline

Deliverables:

1. Folder structure
2. Base router, controller, DB, auth helpers
3. Initial Room database shell
4. Shared documentation

### Phase 1: Authentication and Tenant Core

1. Implement businesses and users schema
2. Build portal login/logout
3. Role model and middleware
4. Basic dashboard shell

Deliverables:

1. Secure admin authentication
2. Business-scoped data access foundations

### Phase 2: Device Pairing and Registration

1. Implement device registration tables
2. Android pairing screen
3. Pairing code issuance
4. Admin pairing workflow
5. Permanent device token exchange

Deliverables:

1. TVs can register and pair successfully
2. Device inventory visible in portal

### Phase 3: Media Library and Storage Abstraction

1. Implement storage interface
2. Build Cloudinary adapter
3. Build media upload UI and API
4. Persist metadata and checksums
5. Add media replace/delete flows

Deliverables:

1. Production-ready media ingestion baseline
2. Provider abstraction ready for future S3 adapter

### Phase 4: Playlists

1. Playlist CRUD
2. Playlist item ordering
3. Image duration configuration
4. Playlist validation

Deliverables:

1. Business users can assemble playback content

### Phase 5: Schedules and Assignments

1. Schedule CRUD
2. Time-slot validation
3. Device-to-schedule assignment
4. Admin UI for schedule mapping

Deliverables:

1. Configured timed playback intent per device

### Phase 6: Android Offline Sync and Local Storage

1. Room schema implementation
2. Sync API contract
3. Sync change log on backend
4. WorkManager sync jobs
5. Download verification and local storage lifecycle

Deliverables:

1. Device receives and stores all required content locally
2. Delta sync functioning end-to-end

### Phase 7: Local Playback Engine

1. ExoPlayer integration
2. Image playback timer logic
3. Playlist loop engine
4. Schedule evaluator
5. Fallback behavior

Deliverables:

1. Offline playback works reliably from local files only

### Phase 8: Heartbeats, Monitoring, and Stability

1. Device heartbeat API
2. Portal device status UI
3. Sync diagnostics
4. Playback error reporting
5. Audit logs and operational logging

Deliverables:

1. Operators can monitor deployed screens

### Phase 9: Hardening and Release Readiness

1. Permission review
2. Input/output security review
3. Performance optimization
4. Failure scenario testing
5. Backup strategy
6. Deployment runbook

Deliverables:

1. MVP ready for controlled production rollout

## 19. Testing Strategy

### Backend

1. Unit tests for services
2. Repository integration tests against MySQL
3. API contract tests
4. Auth and authorization tests

### Android

1. Unit tests for schedule evaluator
2. Sync parser tests
3. Repository tests
4. Instrumented playback flow tests where practical

### End-to-End

1. Pairing flow
2. Media upload to local playback
3. Playlist change sync
4. Schedule switch at boundary times
5. Offline continuity after network disconnect

## 20. Recommended Immediate Next Artifacts

After approval of this blueprint, the next documents to produce should be:

1. Detailed API specification with request/response bodies and error codes
2. SQL migration plan
3. Android local database schema mapping
4. Web portal screen flow and wireframe map
5. Deployment/configuration checklist for cPanel and Cloudinary

## 21. Final Recommendation

The safest implementation path is:

1. Build one monolithic PHP application with clean internal layering
2. Introduce API versioning immediately
3. Use a centralized sync change log for delta updates
4. Treat the Android app as a local-first player with server-coordinated configuration
5. Keep storage provider concerns behind a strict adapter contract

This approach is well aligned with cPanel hosting constraints, your locked technology stack, and the need to evolve the platform into a commercial SaaS product without a rewrite.
