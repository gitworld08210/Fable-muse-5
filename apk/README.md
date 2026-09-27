# Flowpay APK Output Directory

This directory stores the generated Android Application Package (APK) binaries for Flowpay.

## How to Build the APK

### On Mac / Linux:
Run:
```bash
./build_apk.sh
```
Or with Gradle directly:
```bash
./gradlew assembleDebug
```

### On Windows:
Double-click `build_apk.bat` or run in Command Prompt:
```cmd
build_apk.bat
```

## Generated File Paths:
- **`apk/flowpay-debug.apk`** (Copied here after running the build script)
- **`app/build/outputs/apk/debug/app-debug.apk`** (Default Gradle output path)

## On GitHub Actions CI:
Whenever you push to GitHub, the workflow automatically builds the APK and makes it available for download in the **Actions** tab under **Artifacts** as `flowpay-apks`.
