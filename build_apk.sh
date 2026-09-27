#!/usr/bin/env bash
# Script to build Flowpay Debug APK locally
set -e

echo "=========================================="
echo " Building Flowpay Debug APK"
echo "=========================================="

# Ensure .env exists
if [ ! -f .env ]; then
  touch .env
fi

# Ensure debug.keystore exists
if [ ! -f debug.keystore ]; then
  echo "Generating debug.keystore..."
  keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

# Make gradlew executable
chmod +x ./gradlew

# Build the debug APK
./gradlew assembleDebug --stacktrace

# Copy output to convenient location
mkdir -p apk
cp app/build/outputs/apk/debug/app-debug.apk apk/flowpay-debug.apk

echo "=========================================="
echo " APK Built Successfully!"
echo " Location: apk/flowpay-debug.apk"
echo "=========================================="
