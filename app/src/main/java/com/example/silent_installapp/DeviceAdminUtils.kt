package com.example.silent_installapp

import android.app.admin.DeviceAdminReceiver
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
            Toast.makeText(context, "Device Admin already enabled", Toast.LENGTH_SHORT).show()
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
            onComplete(false, "Device Owner privileges required for true silent installation.\n\nDevice Admin alone CANNOT do silent install.\n\nPlease either:\n1. Set up Device Owner via ADB\n2. Use 'By Auto Click' method instead")
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
            onComplete(true, "Installation initiated - system confirmation may appear.\n\nFor true silent install, use 'By Auto Click' method.")

        } catch (e: Exception) {
            Log.e(TAG, "Installation error: ${e.message}", e)
            session?.abandon()
            onComplete(false, "Installation failed: ${e.message}")
        }
    }

    /**
     * Instructions for setting up device owner via ADB
     */
    fun getDeviceOwnerSetupInstructions(): String {
        return """
            To enable TRUE silent installation (like Play Store), you need Device Owner privileges:
            
            Method 1: ADB Command (Recommended for testing)
            1. Enable USB debugging on your device
            2. Connect device to PC via USB
            3. Open command prompt/terminal
            4. Run: adb shell dpm set-device-owner com.example.silent_installapp/.SilentInstallAdminReceiver
            
            Method 2: Device Admin (Limited - requires user confirmation)
            1. Go to Settings → Security → Device Admin
            2. Enable "Silent-install app"
            
            Note: Device Owner can only be set on a factory reset device without any accounts.
            For production, consider using MDM (Mobile Device Management) solutions.
        """.trimIndent()
    }

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
            val adminComponent = getAdminComponentName(context)

            // Clear device owner
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
    fun getDeviceOwnerRemovalInstructions(): String {
        return """
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
    }

    /**
     * Check if app can be uninstalled (blocked by Device Owner)
     */
    fun isUninstallBlocked(context: Context): Boolean {
        return isDeviceOwner(context)
    }

    private const val TAG = "DeviceAdminUtils"
}
