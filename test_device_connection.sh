#!/bin/bash

# Test API connection from Android device via ADB
# Usage: ./test_device_connection.sh [IP_ADDRESS]

IP_ADDRESS=${1:-"192.168.1.100"}
PORT=${2:-"3000"}

echo "=== Testing API Connection from Android Device ==="
echo "Target: http://$IP_ADDRESS:$PORT/api/"
echo ""

# Check if device is connected
echo "📱 Checking device connection..."
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected via USB"
    echo "   Make sure device is connected and USB debugging is enabled"
    exit 1
fi

echo "✅ Device connected"
echo ""

# Test basic connectivity
echo "🌐 Testing basic connectivity..."
adb shell ping -c 3 $IP_ADDRESS > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Device can reach $IP_ADDRESS"
else
    echo "❌ Device cannot reach $IP_ADDRESS"
    echo "   Check network configuration and IP address"
fi
echo ""

# Test API health endpoint
echo "🏥 Testing API health endpoint..."
HEALTH_RESPONSE=$(adb shell curl -s "http://$IP_ADDRESS:$PORT/health" 2>/dev/null)
if [ $? -eq 0 ] && echo "$HEALTH_RESPONSE" | grep -q "ok"; then
    echo "✅ API health check passed"
    echo "   Response: $HEALTH_RESPONSE"
else
    echo "❌ API health check failed"
    echo "   Response: $HEALTH_RESPONSE"
    echo "   Make sure API server is running on $IP_ADDRESS:$PORT"
fi
echo ""

# Test API endpoints
echo "🔗 Testing API endpoints..."

# Test login endpoint (should return 400 for missing data)
LOGIN_TEST=$(adb shell curl -s -X POST "http://$IP_ADDRESS:$PORT/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{}' 2>/dev/null)

if echo "$LOGIN_TEST" | grep -q "400"; then
    echo "✅ API login endpoint accessible (returned expected 400)"
else
    echo "❌ API login endpoint not accessible"
    echo "   Response: $LOGIN_TEST"
fi
echo ""

echo "📋 Summary:"
echo "- Device IP reachability: $(adb shell ping -c 1 $IP_ADDRESS > /dev/null 2>&1 && echo '✅' || echo '❌')"
echo "- API health: $(echo "$HEALTH_RESPONSE" | grep -q "ok" && echo '✅' || echo '❌')"
echo "- API endpoints: $(echo "$LOGIN_TEST" | grep -q "400" && echo '✅' || echo '❌')"
echo ""

if adb shell ping -c 1 $IP_ADDRESS > /dev/null 2>&1 && echo "$HEALTH_RESPONSE" | grep -q "ok"; then
    echo "🎉 Setup looks good! Try running the app on your device."
else
    echo "⚠️  There are connection issues. Check the troubleshooting steps."
fi