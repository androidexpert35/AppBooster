# Wear OS Optimization - Alternative Approaches

## The Challenge

Apps that sideload APKs to Wear OS watches (like **Wear Installer**, **Easy Fire Tools**, **Bugjaeger**) need to communicate with the watch via ADB. Here's how they actually work:

---

## How Sideloading Apps Work

### Key Insight: ALL Apps Require Initial Setup

**No consumer Android app can bypass the Android 11+ pairing requirement without root or one-time PC setup.**

The apps that seem to "just work" actually:
1. Guide users through initial pairing
2. Store RSA keys for future connections
3. Use workarounds like legacy TCP mode

---

### Method 1: **Legacy TCP/IP Mode (Pre-Android 11)**

Some older Wear OS watches or devices with older firmware:

```
1. Connect watch to PC via USB/WiFi debugging
2. Run: adb tcpip 5555  (enables ADB over network on fixed port)
3. Watch listens on <watch-ip>:5555
4. Any app can: adb connect <watch-ip>:5555
5. Only RSA key exchange needed (no PAKE pairing code)
```

**This is how Wear Installer worked initially** - but modern watches disable this or require Wireless Debugging.

---

### Method 2: **Android 11+ Wireless Debugging (Modern Watches)**

All modern Wear OS watches (Wear OS 3+, Galaxy Watch 4+) require:

```
1. Enable Developer Options on watch
2. Enable Wireless Debugging (creates random port each time)
3. "Pair device with pairing code" → shows 6-digit code + pairing port
4. Client MUST implement SPAKE2+ (PAKE) protocol to exchange crypto keys
5. After successful pairing, RSA key is trusted
6. Future connections just need IP:port + trusted RSA key
```

---

### Method 3: **How Wear Installer Actually Works**

Looking at Malcolm Bryant's Wear Installer app flow:

```kotlin
// Simplified flow
1. User opens Wear Installer on phone
2. App shows: "Enable Wireless Debugging on your watch"
3. App shows: "Go to 'Pair device with pairing code'"
4. User enters the IP, port, and 6-digit code shown on watch
5. App implements PAKE pairing internally (or guides to PC)
6. After pairing, app stores keys and can install APKs
```

**The secret**: Some versions use native code (JNI) to implement PAKE, or they guide users to pair via PC's `adb pair` command first.

---

### Method 4: **Bugjaeger's Approach**

Bugjaeger (Pro) is one of the few apps that implements full ADB protocol including pairing:

- Uses native libraries for SPAKE2+ cryptography
- Can directly pair with Android 11+ devices
- Likely uses BouncyCastle or custom PAKE implementation

---

## Available Libraries & Their Capabilities

| Library | Connect | Pair (PAKE) | Android Platform |
|---------|---------|-------------|------------------|
| `dadb` 1.2.10 | ✅ | ❌ | ✅ Works on Android |
| `adam` 0.5.x | ✅ | ⚠️ Partial | ✅ Works on Android |
| `adblib` (Google) | ✅ | ✅ | ❌ JVM only (Android Studio) |
| `ddmlib` | ✅ | ❌ | ⚠️ Heavy, desktop-focused |

### The Missing Piece: PAKE on Android

None of the Android-compatible libraries fully implement Android 11+ pairing. Options:

1. **Implement SPAKE2+ yourself** - Complex, requires crypto expertise
2. **Use native code (JNI)** - Port Google's C++ implementation
3. **Accept one-time PC setup** - Practical solution most apps use

---

## Practical Solutions for AppBooster

Given the constraints:
- **Shizuku requires sideloading** → Not viable (chicken-and-egg)
- **PAKE implementation** → Very complex, 2-4 weeks work
- **PC is required at least once** → Unavoidable for non-rooted watches

### Realistic Options:

---

### ⭐ Option 1: Watch Self-ADB with One-Time PC Pairing

**How it works:**
1. AppBooster Wear is installed from Play Store (no sideloading!)
2. User enables Wireless Debugging on watch
3. **One-time PC step**: User runs `adb pair` from PC to trust watch's own RSA key
4. Watch app connects to `localhost:<port>` and can run shell commands

**Key Insight**: The watch can connect to its OWN ADB daemon! After the RSA key is trusted, no more PC needed.

**User Flow:**
```
1. Install AppBooster Wear from Play Store ✓
2. Enable Developer Options → Wireless Debugging on watch
3. On PC: adb pair <watch-ip>:<port> <code>
4. Watch generates RSA key, PC adds to trusted keys
5. Watch app connects to localhost and optimizes!
```

**Pros:**
- AppBooster Wear installs from Play Store (no sideloading)
- One-time PC setup only
- Watch is fully autonomous after setup

**Implementation**: Already exists in `WearAdbClient.kt` - just needs better UX

---

### Option 2: Root Mode (Rooted Watches Only)

For users with rooted watches - no setup needed at all:

```kotlin
Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd package compile -m speed -a"))
```

**Pros:** Zero setup, instant
**Cons:** Only works on rooted devices (small audience)

---

### Option 3: Implement PAKE Pairing (Future Enhancement)

Implement SPAKE2+ protocol to pair directly from phone without PC:

**Pros:** Best UX - no PC ever needed
**Cons:** 2-4 weeks of complex crypto implementation

---

### ~~Option 4: Shizuku on Watch~~ (NOT VIABLE)

❌ **Problem**: Installing Shizuku on the watch requires sideloading - creates chicken-and-egg problem.
```

---

## Implementation Plan

### ✅ Phase 1: Shell Provider Architecture (DONE)

Created unified shell access layer:

```
wear/domain/client/
└── WearShellClient.kt          # Interface for shell execution

wear/data/client/
├── RootShellClient.kt          # Root (su) implementation
├── WearAdbClient.kt            # Self-ADB implementation (existing)
└── WearShellProvider.kt        # Auto-detection & provider
```

**Priority Chain:**
```
┌─────────────────────────────────────────┐
│ 1. Root (su)                            │
│    ↓ Best: No setup, instant access     │
├─────────────────────────────────────────┤
│ 2. Self-ADB (localhost)                 │
│    ↓ Good: Requires one-time PC pair    │
├─────────────────────────────────────────┤
│ 3. Unavailable                          │
│    Show setup wizard to user            │
└─────────────────────────────────────────┘
```

### Phase 2: Setup Wizard UX (TODO)

For users without root, guide through one-time ADB setup:

1. **Detect shell status** on app launch
2. **If unavailable**, show setup screen:
   - Step-by-step instructions with screenshots
   - "Enable Wireless Debugging" guide
   - Show IP/port for `adb pair` command
   - "Test Connection" button
3. **Store success** in DataStore
4. **Auto-reconnect** on future launches

### Phase 3: PAKE Pairing (Future)

Implement SPAKE2+ protocol for zero-PC setup:

1. Research BouncyCastle SPAKE2 implementation
2. Create `AdbPairingClient` 
3. Enable direct pairing from phone → watch

---

## Appendix: SPAKE2+ Protocol Overview

For future reference, if implementing pairing directly:

```
SPAKE2+ is a Password-Authenticated Key Exchange protocol:

1. Both parties share a password (the 6-digit pairing code)
2. Client generates: X = x*P + pw*M (blinded point)
3. Server generates: Y = y*P + pw*N (blinded point)
4. Exchange X and Y
5. Client computes: K = x*(Y - pw*N)
6. Server computes: K = y*(X - pw*M)
7. Both derive same shared secret K
8. Use K to encrypt subsequent RSA key exchange

Libraries: BouncyCastle (Java), libsodium (C), or Google's C++ implementation
```

Android's implementation is in `system/security/adb/` in AOSP.

---

## Summary

| Approach | PC Required | Play Store Install | Difficulty | Status |
|----------|-------------|-------------------|------------|--------|
| **SPAKE2+ Pairing** | No | ✅ Yes | Implemented | ⭐ **NEW!** |
| **Root Mode** | No | ✅ Yes | Easy | ✅ Done |
| ~~Shizuku~~ | Once | ❌ Needs sideload | - | ❌ Not viable |

**What's New**: 
- Implemented SPAKE2+ (PAKE) pairing protocol
- Watch app can now pair directly without PC!
- Phone app can pair with watch directly without PC!

### How It Works Now

1. **User enables Wireless Debugging** on watch
2. **User taps "Pair device with pairing code"** - sees 6-digit code + port
3. **User enters code in AppBooster** (on watch or phone)
4. **App performs SPAKE2+ handshake** using the code
5. **RSA keys are exchanged** over encrypted channel
6. **Done!** Future connections work automatically

No PC required! 🎉
