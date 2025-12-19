# Fixing "Multiple Users" Device Owner Setup Error

## Problem

When trying to set Device Owner via ADB, you get this error:

```
Exception occurred while executing 'set-device-owner':
java.lang.IllegalStateException: Not allowed to set the device owner 
because there are already several users on the device.
```

This is a **safety feature** in Android that prevents Device Owner from being set on devices with multiple user profiles.

---

## Solution: Remove Secondary User Accounts

### Step 1: Access Settings
1. On your Android device, open **Settings**
2. Navigate to **System** → **Multiple users** (or **Users & accounts** depending on device)

### Step 2: Identify and Remove Secondary Users
1. Look for the list of user accounts on the device
2. You should see:
   - **Primary user** (usually labeled "Owner" or with your name)
   - **Secondary users** (guest accounts, work profiles, etc.)
3. Select each **secondary user** and tap **Delete** or **Remove**
4. Confirm the deletion when prompted

### Step 3: Verify Single User
After removal, verify only one user exists:
- Settings → System → Multiple users
- Only your primary account should be listed

### Step 4: Set Device Owner via ADB

Once you have only one user account:

```bash
adb shell dpm set-device-owner com.example.silent_installapp/.SilentInstallAdminReceiver
```

**Expected success message:**
```
Success: Device owner set to package com.example.silent_installapp
```

---

## Common Scenarios

### Scenario 1: Work Profile
If you have an **Android Work Profile** set up:
1. Go to Settings → Apps
2. Look for "Work Profile" or "Managed device"
3. Remove or disable the work profile
4. Then proceed with Device Owner setup

### Scenario 2: Guest Account
If you have a **Guest account**:
1. Settings → System → Multiple users
2. Remove the Guest account
3. Try Device Owner setup again

### Scenario 3: Still Not Working

If you still get the "multiple users" error after removing all secondary users:

**Option 1: Factory Reset (Nuclear option)**
```bash
adb shell wipe data
adb reboot
```

**Option 2: Via Settings**
1. Settings → System → Reset
2. Select **Factory data reset**
3. Wait for device to restart
4. Set Device Owner immediately after boot (before adding any accounts)

---

## Prevention: Setting Device Owner on Fresh Device

To **avoid this issue entirely**:

1. **Before first setup**, enable USB Debugging (tap Build Number 7 times in About Phone)
2. **During device setup**, choose **"Set up later"** or **"Skip"** for Google account
3. **Immediately** connect via USB and run:
   ```bash
   adb shell dpm set-device-owner com.example.silent_installapp/.SilentInstallAdminReceiver
   ```
4. **Only after** Device Owner is set, add your Google account if needed

---

## Checking Current User Status

To verify your device status before attempting Device Owner setup:

```bash
# Check current Device Owner
adb shell dpm get-device-owner

# List all users on device
adb shell pm list users

# Check if user 0 (primary) exists
adb shell getprop ro.build.user
```

---

## After Successfully Setting Device Owner

Once Device Owner is set:

✅ Navigate to Settings → Security → Device admin apps  
✅ You'll see "Silent-install app" listed  
✅ It **cannot be disabled** from settings (by design)  
✅ You have full silent installation capabilities  

To **remove** Device Owner later:
```bash
adb shell dpm remove-active-admin com.example.silent_installapp/.SilentInstallAdminReceiver
adb shell dpm clear-device-owner-app com.example.silent_installapp
```

---

## Troubleshooting Commands

```bash
# Check if Device Owner is set
adb shell dpm get-device-owner

# Get Device Owner name
adb shell dpm get-device-owner-name

# List all device admins
adb shell dumpsys device_policy

# Force remove (if stuck)
adb shell dpm force-remove-device-owner com.example.silent_installapp
```

---

## Quick Checklist ✓

- [ ] Factory reset device OR removed all secondary users
- [ ] Only one user account exists on device
- [ ] USB Debugging is enabled
- [ ] Device is connected via USB to PC
- [ ] ADB can access device (`adb devices` shows device)
- [ ] App is already installed: `adb install app-debug.apk`
- [ ] Ready to run: `adb shell dpm set-device-owner com.example.silent_installapp/.SilentInstallAdminReceiver`

---

**Status: Ready for Device Owner setup!** 🚀

