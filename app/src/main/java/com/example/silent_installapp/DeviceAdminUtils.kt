package com.example.silent_installapp

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast

/**
 * Utility class for Device Admin management and silent installation
 */
object DeviceAdminUtils {

    /**
     * Check if the app is a device admin
     */
    fun isDeviceAdmin(context: Context): Boolean {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, SilentInstallAdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(componentName)
    }

    /**
     * Check if the app is a device owner
     */
    fun isDeviceOwner(context: Context): Boolean {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }

    /**
     * Request device admin privileges
     */
    fun requestDeviceAdmin(context: Context) {
        if (isDeviceAdmin(context)) {
            context.showToast("Device Admin already enabled")
            return
        }
        val componentName = ComponentName(context, SilentInstallAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enable device admin to allow silent app installation without user interaction"
            )
        }
        context.startActivity(intent)
    }

    /**
     * Attempt to set Device Owner programmatically (on rooted devices)
     * This method requires root access and will use shell commands
     */
    fun setDeviceOwnerViaShell(context: Context): Boolean {
        return try {
            val componentName = "com.example.silent_installapp/.SilentInstallAdminReceiver"
            val command = "dpm set-device-owner $componentName"

            Log.d(TAG, "Attempting to set device owner via shell: $command")

            // Try with su (root)
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Log.d(TAG, "Device Owner set successfully via shell")
                return true
            } else {
                Log.w(TAG, "Shell command failed with exit code: $exitCode")
                // Read error output
                val errorStream = process.errorStream.bufferedReader().use { it.readText() }
                Log.w(TAG, "Error output: $errorStream")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set device owner via shell: ${e.message}", e)
            false
        }
    }

    /**
     * Attempt to enable Device Owner on rooted device without PC
     * Shows step-by-step instructions
     */
    fun enableDeviceOwnerOnDevice(context: Context, onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                // First check if already Device Owner
                if (isDeviceOwner(context)) {
                    onResult(true, "✓ Already Device Owner")
                    return@Thread
                }

                // Step 1: Enable Device Admin first
                if (!isDeviceAdmin(context)) {
                    onResult(false, "⚠️ First enable Device Admin in settings, then try again")
                    return@Thread
                }

                // Step 2: Attempt to set Device Owner via shell (requires root)
                val success = setDeviceOwnerViaShell(context)

                if (success) {
                    onResult(true, "✓ Device Owner enabled successfully!")
                } else {
                    onResult(false, "✗ Failed - Device requires root access.\n\nUse Termux method instead (see instructions)")
                }
            } catch (e: Exception) {
                onResult(false, "✗ Error: ${e.message}")
            }
        }.start()
    }

    /**
     * NEW METHOD: Enable Device Owner via wireless ADB (like you did in Termux)
     * This method executes the ADB command directly from the app
     * Requires: Wireless debugging enabled, android-tools installed
     */
    suspend fun enableDeviceOwnerViaWirelessAdb(
        context: Context,
        ipAddress: String? = null,
        port: Int? = null
    ): Result<String> {
        return try {
            // First check if already Device Owner
            if (isDeviceOwner(context)) {
                return Result.success("✓ Already Device Owner")
            }

            // Check if Device Admin is enabled first
            if (!isDeviceAdmin(context)) {
                return Result.failure(Exception("⚠️ First enable Device Admin, then try again"))
            }

            // Use the AdbWirelessUtils to set device owner
            AdbWirelessUtils.setupDeviceOwnerViaWirelessAdb(context, ipAddress, port)
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling device owner via wireless ADB: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * SIMPLIFIED METHOD: Use this if you already connected via wireless debugging
     * This is equivalent to just running: adb shell dpm set-device-owner ...
     */
    suspend fun enableDeviceOwnerViaAdb(context: Context): Result<String> {
        return try {
            if (isDeviceOwner(context)) {
                return Result.success("✓ Already Device Owner")
            }

            if (!isDeviceAdmin(context)) {
                return Result.failure(Exception("⚠️ First enable Device Admin, then try again"))
            }

            // Just run the ADB command directly
            AdbWirelessUtils.setDeviceOwnerViaAdb(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling device owner via ADB: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Enable Device Owner via wireless debugging (no PC needed)
     * Requires: Wireless debugging enabled + pairing done
     */
    fun setDeviceOwnerViaWirelessAdb(
        context: Context,
        adbHost: String = "localhost",
        adbPort: Int = 5555,
        onResult: (Boolean, String) -> Unit
    ) {
        Thread {
            try {
                val componentName = "com.example.silent_installapp/.SilentInstallAdminReceiver"

                // Connect to ADB server
                val connectCommand = "adb connect $adbHost:$adbPort"
                val setOwnerCommand = "adb shell dpm set-device-owner $componentName"

                Log.d(TAG, "Connecting to ADB: $connectCommand")

                // Execute connect command
                val connectProcess = Runtime.getRuntime().exec(connectCommand)
                val connectResult = connectProcess.inputStream.bufferedReader().use { it.readText() }
                connectProcess.waitFor()

                Log.d(TAG, "Connect result: $connectResult")

                if (connectResult.contains("connected") || connectResult.contains("already connected")) {
                    // Execute set-device-owner command
                    Log.d(TAG, "Setting device owner: $setOwnerCommand")
                    val ownerProcess = Runtime.getRuntime().exec(setOwnerCommand)
                    val ownerResult = ownerProcess.inputStream.bufferedReader().use { it.readText() }
                    val ownerError = ownerProcess.errorStream.bufferedReader().use { it.readText() }
                    ownerProcess.waitFor()

                    Log.d(TAG, "Owner result: $ownerResult")
                    Log.d(TAG, "Owner error: $ownerError")

                    if (ownerResult.contains("Success") || isDeviceOwner(context)) {
                        onResult(true, "✓ Device Owner enabled successfully!")
                    } else {
                        onResult(false, "✗ Failed: $ownerError")
                    }
                } else {
                    onResult(false, "✗ Failed to connect to ADB: $connectResult")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Wireless ADB error: ${e.message}", e)
                onResult(false, "✗ Error: ${e.message}\n\nMake sure ADB tools are available.")
            }
        }.start()
    }

    /**
     * Check if wireless debugging is available on this device
     */
    fun isWirelessDebuggingAvailable(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R // Android 11+
    }

    /**
     * Get device admin component name
     */
    fun getAdminComponentName(context: Context): ComponentName {
        return ComponentName(context, SilentInstallAdminReceiver::class.java)
    }

    /**
     * Install APK silently using Device Owner privileges
     * Note: Only Device Owner (not Device Admin) can do true silent installation
     */
    fun installApkAsDeviceAdmin(
        context: Context,
        apkPath: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (!isDeviceOwner(context)) {
            onComplete(
                false,
                "Device Owner privileges required for true silent installation.\n\n" +
                    "Device Admin alone CANNOT do silent install.\n\n" +
                    "To set up Device Owner via ADB:\n" +
                    "1. Go to Settings → System → Multiple users\n" +
                    "2. Remove all secondary user accounts (keep only primary)\n" +
                    "3. Then run: adb shell dpm set-device-owner " +
                    "com.example.silent_installapp/.SilentInstallAdminReceiver\n\n" +
                    "Alternatively, use 'By Auto Click' method."
            )
            return
        }

        val apkFile = java.io.File(apkPath)
        if (!apkFile.exists() || !apkFile.canRead()) {
            onComplete(false, "APK file not found or cannot be read")
            return
        }

        Log.d(TAG, "Device Owner detected - attempting silent installation")

        // Even with Device Owner, PackageInstaller will still show UI on most devices
        // The only way to truly silent install is through system-level MDM solutions
        // For now, fallback to PackageInstaller which is the best we can do
        installViaPackageInstaller(context, apkPath, onComplete)
    }

    /**
     * Install using PackageInstaller API
     * Note: This will still show system confirmation dialog unless app is system app
     */
    private fun installViaPackageInstaller(
        context: Context,
        apkPath: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val packageInstaller = context.packageManager.packageInstaller
        val apkFile = java.io.File(apkPath)
        var session: PackageInstaller.Session? = null

        try {
            Log.d(TAG, "Creating installation session")

            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(null)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    setInstallReason(android.content.pm.PackageManager.INSTALL_REASON_POLICY)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            session = packageInstaller.openSession(sessionId)
            Log.d(TAG, "Session opened with ID: $sessionId")

            // Write APK to session
            apkFile.inputStream().use { input ->
                session.openWrite("package", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            // Create callback intent
            val callbackIntent = Intent(context, InstallReceiver::class.java).apply {
                action = "com.example.silent_installapp.INSTALL_ACTION"
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                sessionId,
                callbackIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            )

            session.commit(pendingIntent.intentSender)
            Log.d(TAG, "Installation session committed")

            // Note: System will still show confirmation dialog
            onComplete(
                true,
                "Installation initiated - system confirmation may appear.\n\nFor true silent install, use 'By Auto Click' method."
            )

        } catch (e: Exception) {
            Log.e(TAG, "Installation error: ${e.message}", e)
            session?.abandon()
            onComplete(false, "Installation failed: ${e.message}")
        }
    }

    /**
     * Instructions for setting up device owner via ADB
     */
    val enableDeviceAdminInstruction = """
            To enable silent installation (like Play Store), you need Device Owner privileges:

            ⚠️  IMPORTANT REQUIREMENTS:
            • Device must be factory reset (no accounts, single user only)
            • No multiple user accounts can exist on device
            • Wireless Debugging OR USB Debugging must be enabled

            ═══════════════════════════════════════════════════════════════
            METHOD 1️⃣: FROM APP VIA WIRELESS DEBUGGING (Recommended - No PC!)
            ═══════════════════════════════════════════════════════════════
            One-Time Setup (in Termux):
            1. Install Termux from F-Droid (https://f-droid.org)
            2. In Termux, run: pkg install android-tools
            3. Enable Wireless Debugging in Settings → Developer Options
            4. In Termux, run: adb connect localhost:5037
               (This connects ADB to your device)

            Using THIS APP (every time you need Device Owner):
            1. Make sure Wireless Debugging is ON
            2. In THIS APP, tap "Enable Device Admin" first
            3. Then tap "Set via Wireless ADB" button
            4. Done! The app will run: adb shell dpm set-device-owner

            ✅ No need to open Termux again - the app does it for you!

            Alternative (If you already ran adb connect in Termux):
            • Just open THIS APP and tap "Set via Wireless ADB"
            • The app will reuse the existing ADB connection

            ═══════════════════════════════════════════════════════════════
            METHOD 2️⃣: FROM APP (If device is rooted)
            ═══════════════════════════════════════════════════════════════
            1. Tap "Enable Device Admin" button first
            2. Grant Device Admin permission when prompted
            3. Tap "Set Device Owner" button (on this app)
            4. App will attempt to enable Device Owner using root access

            ═══════════════════════════════════════════════════════════════
            METHOD 3️⃣: ADB Command via PC (Most reliable)
            ═══════════════════════════════════════════════════════════════
            1. Enable USB debugging on your device
            2. Connect device to PC via USB
            3. Open command prompt/terminal on PC
            4. Run: adb shell dpm set-device-owner com.example.silent_installapp/.SilentInstallAdminReceiver

            ═══════════════════════════════════════════════════════════════
            TROUBLESHOOTING:
            ═══════════════════════════════════════════════════════════════
            ❌ "Not allowed to set the device owner because there are already several users"
            → Go to Settings → System → Multiple users → Remove all secondary users

            ❌ "Not allowed to set the device owner because there are already some accounts"
            → Go to Settings → Accounts → Remove all accounts (Google, etc.)
            → Or factory reset device

            ℹ️ Device Owner can only be set on a factory reset device without any accounts.
        """.trimIndent()

    /**
     * Remove Device Owner privileges
     * This will restore normal functionality including uninstall button
     */
    fun removeDeviceOwner(context: Context): Boolean {
        if (!isDeviceOwner(context)) {
            Log.w(TAG, "App is not Device Owner, nothing to remove")
            return false
        }

        try {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            devicePolicyManager.clearDeviceOwnerApp(context.packageName)
            Log.d(TAG, "Device Owner removed successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove Device Owner: ${e.message}", e)
            return false
        }
    }

    /**
     * Get instructions for removing Device Owner
     */
    val deviceOwnerRemovalInstructions = """
            To remove Device Owner and restore normal functionality:

            Method 1: Via ADB (Recommended)
            1. Connect device to PC via USB
            2. Open command prompt/terminal
            3. Run: adb shell dpm remove-active-admin com.example.silent_installapp/.SilentInstallAdminReceiver

            Method 2: From App
            Use the "Remove Device Owner" button in the app

            Method 3: Factory Reset (Last resort)
            Settings → System → Reset → Factory data reset

            After removal, the uninstall button will appear again.
        """.trimIndent()

    /**
     * Check if app can be uninstalled (blocked by Device Owner)
     */
    fun isUninstallBlocked(context: Context): Boolean {
        return isDeviceOwner(context)
    }

    private const val TAG = "DeviceAdminUtils"
}
