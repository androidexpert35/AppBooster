# Emulator Wireless Debugging Limitations

## The Problem

When running on **Android Emulator**, the pairing port shown in Wireless Debugging settings has limitations:

### Issue 1: Pairing Dialog Closes
When you switch from the Wireless Debugging settings to the AppBooster app, the pairing dialog **closes automatically**. This means:
- The pairing port stops listening
- The 6-digit code becomes invalid
- "Connection refused" error occurs

### Issue 2: Emulator Network Isolation
The emulator uses a virtual network:
- `localhost` inside the emulator = the emulator itself
- The host PC is at `10.0.2.2`
- The pairing port may only listen on `127.0.0.1` (localhost)

### Issue 3: Port Forwarding Required
For emulator-to-emulator communication (phone emulator → watch emulator), you need `adb forward` from the host.

---

## Solutions

### For Watch Self-Pairing (Watch → itself):

**The pairing dialog MUST stay open** while the app tries to pair. This is nearly impossible on a watch because:
1. Screen is small
2. Switching apps closes the dialog
3. The code expires quickly

**Workaround**: Use split-screen or a helper app (not practical on Wear OS).

### For Real Devices:
The implementation should work on **real physical devices** because:
1. The pairing dialog can stay open while you switch apps (on Android 12+)
2. The port is accessible over the network
3. Both devices are on the same WiFi

### For Emulator Testing:
You can test the pairing protocol by forwarding the port from host:

```bash
# On host PC, while pairing dialog is open on watch emulator:
adb -s emulator-5556 forward tcp:37845 tcp:37845

# Then from phone emulator, connect to 10.0.2.2:37845
```

But this still requires the pairing dialog to stay open.

---

## Recommended Testing Approach

1. **Test on real devices** - Borrow a Wear OS watch and Android phone
2. **Use the Root fallback** - For rooted emulators, the `RootShellClient` works
3. **Pre-pair via PC** - Use `adb pair` from host, then test connection only

---

## Why "Connection Refused"

The error "Connection refused" means:
1. Nothing is listening on that port, OR
2. The pairing dialog closed (most likely), OR
3. Network/firewall blocking the connection

**Most likely cause**: The pairing dialog closed when you switched to AppBooster.

---

## Code Path Verification

To verify the SPAKE2+ code works correctly, we could:
1. Create a mock pairing server for testing
2. Test against a real device with pairing dialog held open
3. Use a second person to hold the dialog open while you enter the code

The implementation follows the AOSP protocol, but real-device testing is needed.
