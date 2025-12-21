package com.example.silent_installapp

import android.content.Context
import androidx.appcompat.app.AlertDialog


/* created by @Riz1 on 19/12/2025 */

object Dialogs {
    fun showConfirmDialog(context: Context, isConfirmed: (InstallationType) -> Unit) {
        val installerService = ApkInstallerService(context)
        val bestMethod = installerService.getBestInstallationMethod()

        val title = when (bestMethod) {
            InstallationMethod.DEVICE_OWNER -> "Choose Installation Method\n✅ Device Owner Available"
            InstallationMethod.DEVICE_ADMIN -> "Choose Installation Method\n✅ Device Admin Available"
            InstallationMethod.ACCESSIBILITY_SERVICE -> "Choose Installation Method\n✅ Auto-Click Available"
            InstallationMethod.MANUAL -> "Choose Installation Method"
        }

        val options = arrayOf(
            "Silent Install (Device Admin)",
            "By Auto Click (Accessibility)",
            "Regular Install"
        )

        var selectedOption = -1

        AlertDialog.Builder(context)
            .setTitle(title)
            .setSingleChoiceItems(options, -1) { _, which -> selectedOption = which }
            .setPositiveButton("Install") { dialog, _ ->
                when (selectedOption) {
                    0 -> isConfirmed(InstallationType.SILENT)
                    1 -> isConfirmed(InstallationType.ACCESSIBILITY_SERVICE)
                    2 -> isConfirmed(InstallationType.REGULAR)
                    else -> isConfirmed(InstallationType.CANCEL)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> isConfirmed(InstallationType.CANCEL) }
            .show()
    }

    fun showErrorDialog(context: Context, message: String) {
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun showAccessibilityServiceDialog(context: Context, isConfirmed: (Boolean) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Enable Accessibility Service")
            .setMessage(
                "To enable silent installation, you need to enable the Auto Install accessibility service.\n\n" +
                    "Steps:\n" +
                    "1. Go to Accessibility Settings\n" +
                    "2. Find 'Silent-install app'\n" +
                    "3. Enable the service\n" +
                    "4. Come back and try again"
            )
            .setPositiveButton("Open Settings") { _, _ -> isConfirmed(true) }
            .setNegativeButton("Cancel") { dialog, _ -> isConfirmed(false) }
            .show()
    }

    fun showDeviceAdminDialog(context: Context, isConfirmed: (Boolean) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Enable Device Admin")
            .setMessage(DeviceAdminUtils.enableDeviceAdminInstruction)
            .setPositiveButton("Enable Device Admin") { _, _ -> isConfirmed(true) }
            .setNegativeButton("Cancel") { dialog, _ -> isConfirmed(false) }
            .show()
    }

    fun showRemoveDeviceOwnerDialog(context: Context, onAction: (DeviceOwnerAction) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Remove Device Owner?")
            .setMessage(
                "This will:\n" +
                    "• Restore uninstall button\n" +
                    "• Disable silent installation\n" +
                    "• Remove all Device Owner privileges\n\n" +
                    "You can set it up again later via ADB."
            )
            .setPositiveButton("Remove") { _, _ -> onAction(DeviceOwnerAction.REMOVE) }
            .setNegativeButton("Cancel") { _, _ -> onAction(DeviceOwnerAction.CANCEL) }
            .setNeutralButton("Instructions") { _, _ -> onAction(DeviceOwnerAction.SHOW_INSTRUCTIONS) }
            .show()
    }

    fun showDeviceOwnerRemovalSuccess(context: Context, onDismiss: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Success")
            .setMessage("Device Owner removed successfully!\n\nYou can now uninstall the app normally.")
            .setPositiveButton("OK") { _, _ -> onDismiss() }
            .show()
    }

    fun showDeviceOwnerRemovalFailed(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Failed")
            .setMessage("Could not remove Device Owner from app.\n\nPlease use ADB command:\n\nadb shell dpm remove-active-admin com.example.silent_installapp/.SilentInstallAdminReceiver")
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy Command") { _, _ ->
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText(
                    "ADB Command",
                    "adb shell dpm remove-active-admin com.example.silent_installapp/.SilentInstallAdminReceiver"
                )
                clipboard.setPrimaryClip(clip)
                context.showToast("Command copied to clipboard")
            }
            .show()
    }

    fun showDeviceOwnerRemovalInstructions(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Remove Device Owner")
            .setMessage(DeviceAdminUtils.deviceOwnerRemovalInstructions)
            .setPositiveButton("OK", null)
            .show()
    }

    fun showDeviceAdminLimitationDialog(
        context: Context,
        isConfirmed: (Boolean) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Device Admin Limitation")
            .setMessage(
                "Device Admin alone cannot perform silent installation.\n\n" +
                    "Options:\n" +
                    "1. Set up Device Owner for silent install (no confirmation)\n" +
                    "2. Use 'By Auto Click' method (auto-clicks buttons)\n\n" +
                    "Device Owner requires ADB setup on factory reset device."
            )
            .setNegativeButton("Cancel") { _, _ -> isConfirmed(false) }
            .setPositiveButton("Setup Device Owner") { _, _ -> isConfirmed(true) }
            .show()
    }

    fun showSilentInstallNotAvailableDialog(
        context: Context,
        isConfirmed: (Boolean) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Silent Installation Not Available")
            .setMessage(
                "Silent installation requires Device Owner privileges. Please enable Device Owner for silent installs without user interaction."
            )
            .setNegativeButton("Cancel") { _, _ -> isConfirmed(false) }
            .setPositiveButton("Setup Device Owner") { _, _ -> isConfirmed(true) }
            .show()
    }

    /**
     * Show dialog with option to set Device Owner via Wireless ADB
     */
    fun showWirelessAdbSetupDialog(context: Context, onAction: (WirelessAdbAction) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Set Device Owner via Wireless ADB")
            .setMessage(
                "📱 QUICK SETUP (No PC needed!)\n\n" +
                    "Prerequisites:\n" +
                    "1. Install Termux from F-Droid\n" +
                    "2. In Termux, run:\n" +
                    "   pkg install android-tools\n" +
                    "   adb connect localhost:5037\n\n" +
                    "Then:\n" +
                    "• Make sure Wireless Debugging is ON\n" +
                    "• Tap 'Set Device Owner' below\n" +
                    "• This app will run the ADB command automatically!\n\n" +
                    "✅ You already did this in Termux, so just tap the button!"
            )
            .setPositiveButton("Set Device Owner") { _, _ ->
                onAction(WirelessAdbAction.SET_DEVICE_OWNER)
            }
            .setNegativeButton("Cancel") { _, _ ->
                onAction(WirelessAdbAction.CANCEL)
            }
            .setNeutralButton("Full Instructions") { _, _ ->
                onAction(WirelessAdbAction.SHOW_INSTRUCTIONS)
            }
            .show()
    }

    /**
     * Show progress dialog for wireless ADB setup
     */
    fun showWirelessAdbProgress(context: Context): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle("Setting Device Owner...")
            .setMessage("Running ADB command...\n\nPlease wait...")
            .setCancelable(false)
            .create()
            .apply { show() }
    }

    /**
     * Show result of wireless ADB setup
     */
    fun showWirelessAdbResult(context: Context, success: Boolean, message: String, onDismiss: () -> Unit = {}) {
        AlertDialog.Builder(context)
            .setTitle(if (success) "✓ Success!" else "✗ Failed")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> onDismiss() }
            .apply {
                if (!success) {
                    setNeutralButton("Instructions") { _, _ ->
                        showDeviceAdminDialog(context) {}
                    }
                }
            }
            .show()
    }
}

enum class InstallationType {
    CANCEL,
    ACCESSIBILITY_SERVICE,
    SILENT,
    REGULAR
}

enum class DeviceOwnerAction {
    REMOVE,
    CANCEL,
    SHOW_INSTRUCTIONS
}

enum class DeviceAdminLimitationAction {
    SETUP_DEVICE_OWNER,
    CANCEL
}

enum class SilentInstallAction {
    SETUP_DEVICE_OWNER,
    USE_AUTO_CLICK,
    CANCEL
}

enum class WirelessAdbAction {
    SET_DEVICE_OWNER,
    SHOW_INSTRUCTIONS,
    CANCEL
}

