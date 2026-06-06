# API Configuration for Physical Android Device (USB)

## Current Setup
- **BASE_URL**: `http://192.168.1.100:3000/api/`
- **Network Security**: Allows cleartext traffic to local IPs

## Step-by-Step Setup for USB Device Testing

### 1. Find Your Computer's IP Address
```bash
# On Linux/Mac:
ip addr show | grep "inet " | grep -v 127.0.0.1

# Or use:
hostname -I

# Example output: 192.168.1.100 192.168.1.101
```

### 2. Update BASE_URL in RetrofitClient.kt
Replace `192.168.1.100` with your actual IP:
```kotlin
private const val BASE_URL = "http://YOUR_IP_HERE:3000/api/"
```

### 3. Start API Server
```bash
cd server/api-raiylake/absensholat-api
PORT=3000 go run main.go
```

### 4. Test Server Accessibility
```bash
# From your computer:
curl http://localhost:3000/health

# From your Android device (browser):
http://YOUR_IP:3000/health
```

### 5. Connect Android Device via USB
```bash
# Enable USB debugging on device
# Connect device to computer
adb devices  # Should show your device

# Optional: Port forwarding (if needed)
adb reverse tcp:3000 tcp:3000
```

### 6. Run App on Device
- Build and install APK on device
- App will connect to `http://YOUR_IP:3000/api/`

## Troubleshooting

### Device Cannot Connect
1. **Check Firewall**: Allow port 3000
   ```bash
   sudo ufw allow 3000
   ```

2. **Check IP Address**: Make sure device and computer are on same network

3. **Try Port Forwarding**:
   ```bash
   adb reverse tcp:3000 tcp:3000
   ```

4. **Check Network Security Config**: Already allows `192.168.1.100`

### ADB Issues
```bash
# Restart ADB
adb kill-server
adb start-server

# Check device connection
adb devices

# If device not found, try:
adb usb
```

## Alternative URLs

### If IP Changes Frequently:
```kotlin
// Use this and change IP as needed
private const val BASE_URL = "http://192.168.1.XXX:3000/api/"
```

### For Multiple Networks:
```kotlin
// WiFi Network 1
private const val BASE_URL = "http://192.168.1.100:3000/api/"

// WiFi Network 2
private const val BASE_URL = "http://192.168.0.100:3000/api/"
```

## Testing Commands

```bash
# Test from computer
curl -X GET "http://localhost:3000/health"

# Test from device (via ADB)
adb shell curl -X GET "http://192.168.1.100:3000/health"

# Check app logs
adb logcat | grep -i retrofit
```