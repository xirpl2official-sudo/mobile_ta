#!/bin/bash

# USB Device Setup Helper Script
# Helps configure API connection for physical Android device

echo "=== USB Device API Setup Helper ==="
echo ""

# Find IP addresses
echo "📡 Finding your IP addresses..."
echo "Local IP addresses:"
ip addr show | grep "inet " | grep -v 127.0.0.1 | awk '{print "  " $2}' | cut -d'/' -f1
echo ""

# Test API server
echo "🔍 Testing API server..."
if curl -s http://localhost:3000/health > /dev/null 2>&1; then
    echo "✅ API server is running on localhost:3000"
else
    echo "❌ API server is not running on localhost:3000"
    echo "   Start it with: cd server/api-raiylake/absensholat-api && go run main.go"
fi
echo ""

# Check ADB devices
echo "📱 Checking ADB connection..."
if command -v adb &> /dev/null; then
    echo "ADB devices:"
    adb devices | grep -v "List"
    echo ""
else
    echo "❌ ADB not found. Install Android SDK platform-tools."
fi

# Show current BASE_URL
echo "🔗 Current BASE_URL in RetrofitClient.kt:"
grep "BASE_URL" mobile_ta/app/src/main/java/network/RetrofitClient.kt
echo ""

echo "📝 Next steps:"
echo "1. Update BASE_URL in RetrofitClient.kt with your IP"
echo "2. Start API server: PORT=3000 go run main.go"
echo "3. Connect Android device via USB"
echo "4. Build and run app on device"
echo ""

echo "📚 See USB_DEVICE_SETUP.md for detailed instructions"