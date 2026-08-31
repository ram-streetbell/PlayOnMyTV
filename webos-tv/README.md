# PlayOnMyTV for LG webOS TV

LG webOS TV client for the PlayOnMyTV digital signage platform.

## Current features

- Device-token authentication against the existing `/api/v1/device/manifest` endpoint
- Automatic manifest refresh every 5 minutes
- Uses the server-selected playlist, including scheduled playlists
- Falls back to all business media when no playlist is assigned
- Image and video playback in fullscreen
- Visible media sync progress bar
- IndexedDB media cache for offline playback after successful sync
- Network playback fallback when a media item could not be cached
- Designed for LG webOS TV remote control and 1920x1080 signage

## Important webOS limitation

LG documents that a JavaScript service can download files, but files downloaded by a service are not accessible from a web app. This implementation therefore uses browser storage (IndexedDB) for the offline media cache instead of relying on the webOS service filesystem.

## Build

Install the current webOS CLI from LG, then package the `webos-tv` directory with `ares-package`. LG's current documentation recommends the `@webos-tools/cli` package and uses `ares-package`, `ares-install`, and `ares-launch` for packaging and device testing.

The GitHub Actions workflow also creates the required PNG icons from `icon.svg` and produces an `.ipk` artifact.

## Test on an LG TV

1. Enable the LG Developer Mode app on the TV.
2. Add the TV to webOS CLI with `ares-setup-device`.
3. Package the app with `ares-package webos-tv`.
4. Install with `ares-install --device <tv-name> <ipk-file>`.
5. Launch with `ares-launch --device <tv-name> com.streetbell.playonmytv`.
6. Enter the PlayOnMyTV device token on first launch.

The Android TV and LG webOS clients use the same PlayOnMyTV backend manifest, so the CMS remains the single place to manage media, playlists, schedules, and devices.
