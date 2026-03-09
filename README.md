# Pocket Relay

Pocket Relay is a lightweight Nostr NIP-01 WebSocket relay that runs on Android as a foreground service. It provides a local WebSocket endpoint (default ws://<device_ip>:4444) for Nostr clients to publish and subscribe to events for development, testing, and small LAN usage.

## Features

- Nostr NIP-01 relay (EVENT / REQ / CLOSE)
- Embedded Ktor Netty WebSocket server on port 4444
- Jetpack Compose UI to monitor status, connected clients, and recent events
- Schnorr signature validation (event validation framework)
- Persistent disk-backed event storage (`DiskEventRepository` -> `events.json` in app files)
- In-memory recent-event tracker (`EventTracker`) for UI display
- Thread-safe connection tracking (`ConnectionTracker`)
- Python test clients included for posting and subscribing to events

## Architecture

- `RelayServer` — Ktor WebSocket server
- `NostrHandler` — protocol parsing, validation, persistence, broadcasting
- `EventRepository` — storage abstraction (in-memory and disk implementations)
- `EventTracker` — recent events for UI
- `MainActivity` + `MainViewModel` — Compose UI and state
- `RelayService` — Android foreground service wrapping the relay

## Quick start (debug build)

1. Build and install (from project root):

```bash
./gradle/wrapper/gradle-8.5/bin/gradle installDebug
```

2. Open the app on the device and tap `START RELAY`.
3. From another device on the same LAN, connect to the relay endpoint shown in the UI:

```
ws://<device_ip>:4444
```

4. Use the included Python test client to publish and subscribe:

```bash
python3 /tmp/nostr_test_client.py
```

## Developer notes

- Events are persisted to `events.json` in the app files directory.
- `DiskEventRepository` implements a simple JSON-backed store for quick testing.
- Invalid signature events may be stored for debugging (configurable).
- Consider adding TLS, rate limiting, and access controls before public exposure.

## Contributing

Fork, create a branch, implement and test changes, then open a pull request.

## License

Add your preferred license here.
