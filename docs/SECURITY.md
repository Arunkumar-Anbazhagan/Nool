# Nool Security & Privacy Architecture

## 1. Core Principles
1. **Zero Telemetry**: No analytical pings, error reporting beacons, or user tracking.
2. **Local-First & Offline**: All features run locally on-device. No network access occurs without explicit user request (e.g. Git remote sync).
3. **Never Log User Content**: Vault files, note bodies, titles, and search queries must never appear in production system logs or error stacks.
4. **Secure Storage**: Sensitive credentials (Git personal access tokens, SSH private keys) are protected with Android `EncryptedSharedPreferences` / OS Keyring.

## 2. Default-Disabled Features
The following features are disabled by default and require deliberate user opt-in:
- Git auto-sync / network sync.
- Remote AI / LLM integration (brings-your-own-key, outbound prompt review).
- Desktop plugin runtime / execution sandbox.
- Localhost IPC / REST server (strictly loopback `127.0.0.1` with bearer token auth).
- E2EE relay sync.

## 3. Sandboxing & Threat Model
- **Offline Diagram Rendering**: `mermaid.min.js` runs in an isolated WebView without internet permissions or external CDN resources.
- **Biometric App Lock**: Cryptographic AndroidX Biometric authentication gating access to the application lifecycle when enabled.
