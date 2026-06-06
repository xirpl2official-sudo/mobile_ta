# API Configuration Guide

## Base URLs

### Development (Local)
- **Android Emulator**: `http://10.0.2.2:3000/api/`
- **Physical Device (USB)**: `http://127.0.0.1:3000/api/` (Requires running `adb reverse tcp:3000 tcp:3000`)
- **Physical Device (WiFi)**: `http://[YOUR_LOCAL_IP]:3000/api/`
- **Localhost**: `http://localhost:3000/api/` (if accessible)

### Production
- **Vercel**: `https://absensholat-api.vercel.app/api/`

## How to Change URL

Edit `mobile_ta/app/src/main/java/network/RetrofitClient.kt`:

```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/api/"  // For Android Emulator
// OR
private const val BASE_URL = "http://192.168.1.100:3000/api/"  // For Physical Device
// OR
private const val BASE_URL = "https://absensholat-api.vercel.app/api/"  // For Production
```

## Starting Local API Server

```bash
cd server/api-raiylake/absensholat-api
PORT=3000 go run main.go
```

## Network Security Config

Already configured in `res/xml/network_security_config.xml` to allow:
- Cleartext traffic to 10.0.2.2, 192.168.1.100, localhost
- HTTPS for production domains

## Testing Connection

```bash
# Test API health
curl http://localhost:3000/health

# Test from Android
# In Android Studio Logcat, look for Retrofit logs
```

## Build Variants (Optional)

For different environments, you can create build variants:

1. Create `app/src/debug/res/values/strings.xml`
2. Create `app/src/release/res/values/strings.xml`
3. Use different BASE_URL values
4. Access via `getString(R.string.api_base_url)`