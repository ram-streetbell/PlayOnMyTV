# PlayOnMyTV

PlayOnMyTV is an offline-first digital signage platform for Android TV with a PHP-based admin portal and REST API.

## Repository Structure

1. `app/` PHP application source
2. `bootstrap/` application bootstrap files
3. `config/` environment-aware configuration
4. `database/migrations/` SQL migrations
5. `docs/` project architecture and technical docs
6. `public/` web root for the admin portal and API
7. `routes/` web and API route registration
8. `android-tv/` Android Studio scaffold

## Backend Stack

1. PHP 8.2
2. PDO
3. Composer
4. Bootstrap 5
5. Vanilla JavaScript

## Android Stack

1. Kotlin
2. Room
3. WorkManager
4. ExoPlayer

## Local Setup

1. Copy `.env.example` to `.env`
2. Set a strong `APP_KEY` value for secure token handoff
3. Run `composer dump-autoload`
4. Point your web server document root to `public/`
5. Create the MySQL database configured in `.env`
6. Import SQL files from `database/migrations/` in order

## Current Status

This repository currently contains a production-oriented scaffold only.

Included:

1. MVC-style PHP foundation
2. Shared admin portal layout
3. Empty web controllers and API controllers
4. SQL migrations
5. Android TV project skeleton

Not implemented yet:

1. Business logic
2. CRUD
3. Authentication flow
4. API behavior
5. Playback logic
6. Synchronization logic

## Important Entry Points

1. Web root: `public/index.php`
2. Web routes: `routes/web.php`
3. API routes: `routes/api.php`
4. App bootstrap: `bootstrap/app.php`
5. Android app package root: `android-tv/app/src/main/java/com/playonmytv`
