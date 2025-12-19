# Silent Install App

A comprehensive Android application demonstrating **3 different APK installation methods** with varying levels of automation and user interaction.

## 🎯 Features

This app showcases **three distinct installation approaches**, each with different permission requirements and user experience:

### 1. **Device Owner (Silent Installation - No Interaction)**
- ✅ **True silent installation** - zero user interaction
- 📦 Apps install completely in background
- ⚡ Same behavior as Google Play Store and Samsung Store
- 🔒 Requires: Device Owner privileges (system-level access)
- 🎯 Best for: Enterprise deployments, MDM solutions, testing

### 2. **Device Admin (Semi-Silent Installation - Minimal Interaction)**
- 📋 Minimal user interaction required
- ✅ Admin-level permissions for enhanced control
- 🔒 Requires: Device Admin enabled
- ⚙️ Best for: Controlled enterprise environments

### 3. **Accessibility Service (Auto-Click Installation)**
- 🤖 Automatically clicks Install/Done buttons
- 📱 Installation UI still visible but fully automated
- ♿ Uses Android Accessibility Service framework
- 🔒 Requires: Accessibility Service enabled
- ⚙️ Best for: Fallback when higher permissions unavailable

## 📋 Project Structure

```
app/src/main/java/com/example/silent_installapp/
├── MainActivity.kt                 # Main UI and installation flow
├── ApkInstallerService.kt         # Service handling installations
├── AutoInstallService.kt          # Accessibility service implementation
├── DeviceAdminUtils.kt            # Device Admin/Owner utilities
├── AccessibilityUtils.kt          # Accessibility service utilities
├── Dialogs.kt                     # UI dialogs and confirmations
├── SilentInstallAdminReceiver.kt  # Device Admin receiver
├── InstallReceiver.kt             # Installation broadcast receiver
└── Util.kt                        # Utility functions
```

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (latest version recommended)
- **Android SDK** (minSdk: 24, targetSdk: 36)
- **Java 11** or higher
- **Android device** (API 24+) for testing
- **ADB** (Android Debug Bridge) for Device Owner setup

### Build & Install

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd App-installation-types
   ```

2. **Build the project:**
   ```bash
   ./gradlew build
   ```

3. **Install on device:**
   ```bash
   ./gradlew installDebug
   ```

## 🔧 Configuration

### Device Requirements

- **Device Owner Setup**: Requires factory reset or device without Google/Samsung accounts
- **Device Admin Setup**: Available on any Android 5.0+ device
- **Accessibility Service**: Available on any Android device

### Permissions (AndroidManifest.xml)

The app requires the following permissions:

```xml
<!-- Installation-related permissions -->
<uses-permission android:name="android.permission.INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.DELETE_PACKAGES" />

<!-- Accessibility Service -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />

<!-- Device Admin -->
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
```

## 📖 Usage Guide

### For Silent Installation (Device Owner - Recommended)

#### Step 1: Set Device Owner via ADB

```bash
# Prerequisites:
# 1. Factory reset device (or device without accounts)
# 2. Enable USB Debugging
# 3. Install the app first

# Command:
adb shell dpm set-device-owner com.example.silent_installapp/.SilentInstallAdminReceiver
```

**Success Response:**
```
Success: Device owner set to package com.example.silent_installapp
```

#### Step 2: Test Silent Installation

1. Open the Silent Install App
2. Tap **"Select APK"** button
3. Choose an APK file from device storage
4. Select **"Silent"** installation method
5. 🎉 App installs **completely silently** without any UI!

### For Device Admin Installation

1. Open the app
2. Select an APK file
3. Choose **"Silent"** installation
4. Follow prompts to enable Device Admin
5. Grant admin permissions
6. Installation proceeds with minimal UI

### For Accessibility Service Installation

1. Open the app
2. Select an APK file
3. Choose **"Silent"** installation
4. Tap **"Open Settings"**
5. Enable "Silent-install app" in Accessibility settings
6. Return to app and proceed
7. Service auto-clicks installation buttons

## ⚙️ Core Components

### 1. MainActivity.kt
- **Purpose**: Main user interface and installation flow orchestration
- **Features**:
  - APK file selection via Android's file picker
  - Installation method selection (Silent/Regular/Auto-click)
  - Device Owner status display
  - Device Owner removal capability
  - Real-time progress updates

### 2. ApkInstallerService.kt
- **Purpose**: Handles all APK installation operations
- **Methods**:
  - `installSilentlyAsDeviceAdmin()` - True silent installation (Device Owner only)
  - `installSilentlyByAutoClick()` - Auto-click installation
  - `installApk()` - Regular installation via PackageManager

### 3. AutoInstallService.kt
- **Purpose**: Accessibility Service that automates button clicks
- **Features**:
  - Detects "Install" button via accessibility tree
  - Automatically performs clicks on install dialogs
  - Monitors installation progress

### 4. DeviceAdminUtils.kt
- **Purpose**: Device Admin and Device Owner management
- **Features**:
  - Check Device Owner/Admin status
  - Request Device Admin activation
  - Remove Device Owner privileges
  - Package manager operations

### 5. AccessibilityUtils.kt
- **Purpose**: Accessibility Service utilities
- **Features**:
  - Check if Accessibility Service is enabled
  - Open Accessibility settings
  - Service status verification

### 6. Dialogs.kt
- **Purpose**: User interface dialogs and confirmations
- **Dialogs**:
  - Installation method selection
  - Device Admin setup confirmation
  - Accessibility Service setup
  - Device Owner removal confirmation
  - Error and success messages

## 🔐 Permissions & Security

### Installation Permissions
- `INSTALL_PACKAGES` - Required for silent installation
- `DELETE_PACKAGES` - Required for app removal
- `QUERY_ALL_PACKAGES` - Required to check installed apps

### Device Admin Permissions
- Allows device administration capabilities
- Controlled via Device Admin settings

### Accessibility Service
- Enabled by user through system settings
- Uses accessibility APIs for UI automation
- Subject to system security policies

## ⚠️ Important Limitations

### Device Owner Setup

✅ **Works on:**
- Factory reset devices
- Devices without Google/Samsung accounts
- Enterprise-provided devices
- Test environments

❌ **Doesn't work on:**
- Devices with active Google Account
- Devices with Samsung Account
- Personal consumer devices

### Removal of Device Owner

Device Owner is **persistent** and difficult to remove:

```bash
# Remove via ADB (factory reset recommended):
adb shell dpm remove-active-admin com.example.silent_installapp/.SilentInstallAdminReceiver

# Full reset (nuclear option):
adb shell wipe data
```

### Production Deployment

For production apps requiring silent installation:
- Use **MDM (Mobile Device Management)** solutions
- Examples: Samsung Knox, Google EMM, MobileIron
- MDM handles provisioning and device ownership
- Provides centralized management and security

## 🔄 Installation Flow Diagram

```
┌─────────────────────────────┐
│   Select APK File           │
└──────────────┬──────────────┘
               │
       ┌───────▼────────┐
       │ Installation   │
       │ Method Choice  │
       └───┬────┬───┬───┘
           │    │   │
      ┌────▼─┐ ┌┴───▼───────┐ ┌──────────┐
      │Silent│ │Regular     │ │Accessibility
      └─┬────┘ │Installation│ │Service   
        │      └┬───────────┘ └──────────┘
        │       │
    ┌───┴───────▼──────────────────┐
    │ Check Permission Hierarchy   │
    └───┬───────────┬──────────┬───┘
        │           │          │
   ┌────▼──┐   ┌────▼──┐   ┌───▼──┐
   │Device │   │Device │   │Access│
   │Owner  │   │Admin  │   │bility│
   └────┬──┘   └────┬──┘   └───┬──┘
        │           │          │
   ┌────▼───────────▼──────────▼───────┐
   │  Proceed with Installation        │
   └───────────────────────────────────┘
```

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### UI Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist

- [ ] Device Owner silent installation
- [ ] Device Admin semi-silent installation
- [ ] Accessibility Service auto-click installation
- [ ] Regular installation fallback
- [ ] Error handling and recovery
- [ ] Device Owner removal
- [ ] Permission state transitions

## 📝 Build Configuration

### Gradle Files

**app/build.gradle.kts:**
- minSdk: 24
- targetSdk: 36
- Kotlin JVM target: 11
- Dependencies: AndroidX, Material Design, AppCompat

### ProGuard Configuration
- ProGuard rules configured in `proguard-rules.pro`
- Minification disabled in debug builds
- Production releases can enable minification

## 🐛 Troubleshooting

### Device Owner Setup Issues

**Problem**: `Error: Not allowed to set package as device owner`

**Solution**:
1. Device must be factory reset
2. No accounts should be set up
3. USB Debugging must be enabled
4. App must be installed before setting owner

**Problem**: `adb: command not found`

**Solution**: Add ADB to system PATH or use full path to adb executable

### Silent Installation Not Working

**Problem**: Installation shows UI despite choosing "Silent"

**Possible causes**:
- Device Owner not properly set - verify with: `adb shell dpm get-device-owner`
- Device Admin not enabled - check in Settings > Security
- Accessibility Service not enabled - check Settings > Accessibility

**Solution**: Follow permission hierarchy in proper order

### Accessibility Service Not Detecting Buttons

**Problem**: Auto-click fails to find install button

**Possible causes**:
- Service not enabled in system accessibility settings
- Different button text/location in specific Android version
- Service crashed silently

**Solution**:
1. Verify service enabled in Settings > Accessibility
2. Check logcat: `adb logcat | grep AutoInstallService`
3. Try regular installation as fallback

## 📚 Additional Resources

- [Android Device Admin Documentation](https://developer.android.com/guide/topics/admin/device-admin)
- [Android Accessibility Service Guide](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Device Policy Manager Documentation](https://developer.android.com/reference/android/app/admin/DevicePolicyManager)
- [Enterprise Device Management](https://developer.android.com/work)

## 📄 License

This project is provided as-is for educational and demonstration purposes.

## 👨‍💻 Contributing

Contributions are welcome! Please ensure:
- Code follows Kotlin best practices
- All changes are properly tested
- Commits have clear, descriptive messages
- New features include documentation

## 📞 Support

For issues, bugs, or feature requests, please refer to the detailed setup guide in `DEVICE_ADMIN_SETUP.md` or check Android documentation links above.

---

**Last Updated**: December 2025
**Target API**: Android 36 (15)
**Minimum API**: Android 24 (7.0)

