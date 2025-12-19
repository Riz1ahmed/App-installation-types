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
            .setSingleChoiceItems(options, -1) { _, which ->
                selectedOption = which
            }
            .setPositiveButton("Install") { dialog, _ ->
                when (selectedOption) {
                    0 -> isConfirmed(InstallationType.SILENT)
                    1 -> isConfirmed(InstallationType.ACCESSIBILITY_SERVICE)
                    2 -> isConfirmed(InstallationType.REGULAR)
                    else -> isConfirmed(InstallationType.CANCEL)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                isConfirmed(InstallationType.CANCEL)
                dialog.dismiss()
            }
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
            .setNegativeButton("Cancel", ) { dialog, _ ->
                isConfirmed(false)
                dialog.dismiss()
            }
            .show()
    }

    fun showDeviceAdminDialog(context: Context, isConfirmed: (Boolean) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Enable Device Admin")
            .setMessage(DeviceAdminUtils.getDeviceOwnerSetupInstructions())
            .setPositiveButton("Enable Device Admin") { _, _ -> isConfirmed(true) }
            .setNegativeButton("Cancel") { dialog, _ ->
                isConfirmed(false)
                dialog.dismiss()
            }
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
            .setPositiveButton("Remove") { _, _ ->
                onAction(DeviceOwnerAction.REMOVE)
            }
            .setNegativeButton("Cancel") { _, _ ->
                onAction(DeviceOwnerAction.CANCEL)
            }
            .setNeutralButton("Instructions") { _, _ ->
                onAction(DeviceOwnerAction.SHOW_INSTRUCTIONS)
            }
            .show()
    }

    fun showDeviceOwnerRemovalSuccess(context: Context, onDismiss: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Success")
            .setMessage("Device Owner removed successfully!\n\nYou can now uninstall the app normally.")
            .setPositiveButton("OK") { _, _ ->
                onDismiss()
            }
            .show()
    }

    fun showDeviceOwnerRemovalFailed(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Failed")
            .setMessage("Could not remove Device Owner from app.\n\nPlease use ADB command:\n\nadb shell dpm remove-active-admin com.example.silent_installapp/.SilentInstallAdminReceiver")
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy Command") { _, _ ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText(
                    "ADB Command",
                    "adb shell dpm remove-active-admin com.example.silent_installapp/.SilentInstallAdminReceiver"
                )
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, "Command copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    fun showDeviceOwnerRemovalInstructions(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Remove Device Owner")
            .setMessage(DeviceAdminUtils.getDeviceOwnerRemovalInstructions())
            .setPositiveButton("OK", null)
            .show()
    }

    fun showDeviceAdminLimitationDialog(context: Context, onAction: (DeviceAdminLimitationAction) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Device Admin Limitation")
            .setMessage(
                "Device Admin alone cannot perform true silent installation.\n\n" +
                "Options:\n" +
                "1. Set up Device Owner for TRUE silent install (no confirmation)\n" +
                "2. Use 'By Auto Click' method (auto-clicks buttons)\n\n" +
                "Device Owner requires ADB setup on factory reset device."
            )
            .setPositiveButton("Setup Device Owner") { _, _ ->
                onAction(DeviceAdminLimitationAction.SETUP_DEVICE_OWNER)
            }
            .setNegativeButton("Use Auto Click") { _, _ ->
                onAction(DeviceAdminLimitationAction.USE_AUTO_CLICK)
            }
            .setNeutralButton("Cancel") { _, _ ->
                onAction(DeviceAdminLimitationAction.CANCEL)
            }
            .show()
    }

    fun showSilentInstallNotAvailableDialog(context: Context, onAction: (SilentInstallAction) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Silent Installation Not Available")
            .setMessage(
                "True silent installation requires Device Owner privileges.\n\n" +
                "Choose an option:\n" +
                "1. Setup Device Owner (TRUE silent - like Play Store)\n" +
                "2. Use Auto Click method (requires Accessibility Service)"
            )
            .setPositiveButton("Setup Device Owner") { _, _ ->
                onAction(SilentInstallAction.SETUP_DEVICE_OWNER)
            }
            .setNegativeButton("Use Auto Click") { _, _ ->
                onAction(SilentInstallAction.USE_AUTO_CLICK)
            }
            .setNeutralButton("Cancel") { _, _ ->
                onAction(SilentInstallAction.CANCEL)
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
    USE_AUTO_CLICK,
    CANCEL
}

enum class SilentInstallAction {
    SETUP_DEVICE_OWNER,
    USE_AUTO_CLICK,
    CANCEL
}
