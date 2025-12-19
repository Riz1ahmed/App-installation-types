package com.example.silent_installapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File

/**
 * Service class to handle APK installation operations
 * Separates installation logic from UI layer
 */
class ApkInstallerService(private val context: Context) {

    /**
     * Install APK via Intent (for Android 7+)
     * This shows the system installation UI
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun installViaIntent(apkPath: String, onComplete: (Boolean, String) -> Unit) {
        try {
            val apkFile = File(apkPath)
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, APK_MIME_TYPE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
            Log.d(TAG, "Installation intent started for: $apkPath")
            onComplete(true, "Installation started. Complete it manually.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting installation intent", e)
            onComplete(false, e.message ?: "Failed to start installation")
        }
    }

    /**
     * Install APK via PackageInstaller (for Android 12+)
     * Uses PackageInstaller API for better control and tracking
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun installViaPackageInstaller(apkPath: String, onComplete: (Boolean, String) -> Unit) {
        val packageInstaller = context.packageManager.packageInstaller
        val apkFile = File(apkPath)

        if (!apkFile.exists() || !apkFile.canRead()) {
            onComplete(false, "APK file not found or cannot be read")
            return
        }

        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(null) // Let system determine package name
        }

        var sessionId = -1
        var session: PackageInstaller.Session? = null

        try {
            Log.d(TAG, "Creating installation session for: $apkPath")
            sessionId = packageInstaller.createSession(params)
            session = packageInstaller.openSession(sessionId)
            Log.d(TAG, "Session opened with ID: $sessionId")

            // Write APK to session
            Log.d(TAG, "Writing APK file to session, size: ${apkFile.length()} bytes")
            apkFile.inputStream().use { input ->
                session.openWrite("package", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            Log.d(TAG, "APK file written successfully")

            // Create callback intent for installation result
            val callbackIntent = Intent(context, InstallReceiver::class.java).apply {
                action = INSTALL_ACTION
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                sessionId,
                callbackIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                android.app.PendingIntent.FLAG_MUTABLE
            )

            // Commit the session
            Log.d(TAG, "Committing session")
            session.commit(pendingIntent.intentSender)
            Log.d(TAG, "Session committed successfully")

            onComplete(true, "Installation in progress...")

        } catch (e: Exception) {
            Log.e(TAG, "Installation error: ${e.message}", e)
            session?.abandon()
            onComplete(false, e.message ?: "Installation failed")
        }
    }

    /**
     * Install APK based on Android version
     * Automatically selects the appropriate installation method
     */
    fun installApk(apkPath: String, onComplete: (Boolean, String) -> Unit) {
        Log.d(TAG, "Starting installation for APK: $apkPath")

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 12+ - use PackageInstaller
                    installViaPackageInstaller(apkPath, onComplete)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                    // Android 7+ - use FileProvider
                    installViaIntent(apkPath, onComplete)
                }
                else -> {
                    installViaIntent(apkPath, onComplete)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during installation", e)
            onComplete(false, e.message ?: "Unknown error")
        }
    }

    /**
     * Install APK silently using Accessibility Service
     * Requires AutoInstallService to be enabled
     */
    fun installSilentlyByAutoClick(apkPath: String, onComplete: (Boolean, String) -> Unit) {
        Log.d(TAG, "Starting silent installation for APK: $apkPath")

        // Check if accessibility service is enabled
        if (!AccessibilityUtils.isAccessibilityServiceEnabled(context, AutoInstallService::class.java)) {
            onComplete(false, "Accessibility service not enabled")
            return
        }

        // Proceed with regular installation - accessibility service will auto-click
        installApk(apkPath, onComplete)
    }

    /**
     * Install APK with TRUE silent installation using Device Admin/Owner privileges
     * This is like Play Store - NO user interaction required!
     */
    fun installSilentlyAsDeviceAdmin(apkPath: String, onComplete: (Boolean, String) -> Unit) {
        Log.d(TAG, "Starting TRUE silent installation as Device Admin for APK: $apkPath")

        // Check if we have Device Admin or Device Owner privileges
        if (!DeviceAdminUtils.isDeviceAdmin(context) && !DeviceAdminUtils.isDeviceOwner(context)) {
            onComplete(false, "Device Admin privileges required for true silent installation")
            return
        }

        // Use Device Admin installation method
        DeviceAdminUtils.installApkAsDeviceAdmin(context, apkPath, onComplete)
    }

    /**
     * Get the best installation method based on available privileges
     */
    fun getBestInstallationMethod(): InstallationMethod {
        return when {
            DeviceAdminUtils.isDeviceOwner(context) -> InstallationMethod.DEVICE_OWNER
            DeviceAdminUtils.isDeviceAdmin(context) -> InstallationMethod.DEVICE_ADMIN
            AccessibilityUtils.isAccessibilityServiceEnabled(context, AutoInstallService::class.java) ->
                InstallationMethod.ACCESSIBILITY_SERVICE
            else -> InstallationMethod.MANUAL
        }
    }

    /**
     * Check if the APK file is valid before installation
     */
    fun validateApkFile(apkPath: String): Boolean {
        val file = File(apkPath)
        return file.exists() && file.isFile && file.canRead() && file.length() > 0
    }

    companion object {
        private const val TAG = "ApkInstallerService"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val INSTALL_ACTION = "com.example.silent_installapp.INSTALL_ACTION"
    }
}

/**
 * Enum representing different installation methods
 */
enum class InstallationMethod {
    DEVICE_OWNER,           // TRUE silent - no user interaction (like Play Store)
    DEVICE_ADMIN,           // Silent with admin privileges
    ACCESSIBILITY_SERVICE,  // Auto-click installation buttons
    MANUAL                  // Regular installation requiring user interaction
}
