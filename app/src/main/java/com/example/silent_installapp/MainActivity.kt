package com.example.silent_installapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.silent_installapp.Dialogs.showErrorDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("ObsoleteSdkInt")
class MainActivity : AppCompatActivity() {

    private lateinit var selectButton: Button
    private lateinit var removeDeviceOwnerButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var setupDeviceOwnerButton: Button
    private lateinit var installerService: ApkInstallerService

    // Modern Activity Result API
    private val pickApkLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { apkUri ->
            if (apkUri == null) return@registerForActivityResult
            getRealPathFromUri(this, apkUri)?.let { apkPath ->
                Dialogs.showConfirmDialog(this) { installationType ->
                    when (installationType) {
                        InstallationType.CANCEL -> return@showConfirmDialog
                        InstallationType.SILENT -> installSilently(apkPath)
                        InstallationType.REGULAR -> installApkRegular(apkPath)
                        InstallationType.ACCESSIBILITY_SERVICE -> installByAutoClick(apkPath)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize installer service
        installerService = ApkInstallerService(this)

        selectButton = findViewById(R.id.selectButton)
        removeDeviceOwnerButton = findViewById(R.id.removeDeviceOwnerButton)
        setupDeviceOwnerButton = findViewById(R.id.setupDeviceOwnerButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        selectButton.setOnClickListener { pickApkLauncher.launch(APK_MIME_TYPE) }

        // Setup remove device owner button
        removeDeviceOwnerButton.setOnClickListener { showRemoveDeviceOwnerDialog() }

        // Setup wireless ADB device owner button
        setupDeviceOwnerButton.setOnClickListener { showWirelessAdbSetupDialog() }

        // Check and show device owner status
        updateDeviceOwnerStatus()
    }

    private fun updateDeviceOwnerStatus() {
        if (DeviceAdminUtils.isDeviceOwner(this)) {
            removeDeviceOwnerButton.visibility = Button.VISIBLE
            //setupDeviceOwnerButton.visibility = Button.GONE
            statusText.text = "Device Owner Active\n⚠️ Uninstall blocked"
        } else {
            removeDeviceOwnerButton.visibility = Button.GONE
            setupDeviceOwnerButton.visibility = Button.VISIBLE
            statusText.text = "Ready"
        }
    }

    private fun showRemoveDeviceOwnerDialog() {
        Dialogs.showRemoveDeviceOwnerDialog(this) { action ->
            when (action) {
                DeviceOwnerAction.REMOVE -> removeDeviceOwnerPrivileges()
                DeviceOwnerAction.SHOW_INSTRUCTIONS -> Dialogs.showDeviceOwnerRemovalInstructions(this)
                DeviceOwnerAction.CANCEL -> { /* Do nothing */
                }
            }
        }
    }

    private fun removeDeviceOwnerPrivileges() {
        val success = DeviceAdminUtils.removeDeviceOwner(this)
        if (success) {
            Dialogs.showDeviceOwnerRemovalSuccess(this) {
                updateDeviceOwnerStatus()
            }
        } else {
            Dialogs.showDeviceOwnerRemovalFailed(this)
        }
    }

    private fun showWirelessAdbSetupDialog() {
        Dialogs.showWirelessAdbSetupDialog(this) { action ->
            when (action) {
                WirelessAdbAction.SET_DEVICE_OWNER -> {
                    // Check if Device Admin is enabled first
                    if (!DeviceAdminUtils.isDeviceAdmin(this)) {
                        Dialogs.showErrorDialog(this, "⚠️ Please enable Device Admin first")
                        //DeviceAdminUtils.requestDeviceAdmin(this)
                        return@showWirelessAdbSetupDialog
                    }
                    setupDeviceOwnerViaWirelessAdb()
                }

                WirelessAdbAction.SHOW_INSTRUCTIONS -> {
                    Dialogs.showDeviceAdminDialog(this) { enableDeviceAdmin ->
                        if (enableDeviceAdmin) DeviceAdminUtils.requestDeviceAdmin(this)


                    }
                }

                WirelessAdbAction.CANCEL -> {
                    // Do nothing
                }
            }
        }
    }

    private fun setupDeviceOwnerViaWirelessAdb() {
        val progressDialog = Dialogs.showWirelessAdbProgress(this)

        CoroutineScope(Dispatchers.Main).launch {
            delay(1000) // Small delay to ensure dialog is shown

            try {
                val result = withContext(Dispatchers.IO) {
                    DeviceAdminUtils.enableDeviceOwnerViaAdb(this@MainActivity)
                }

                progressDialog.dismiss()

                result.fold(
                    onSuccess = { message ->
                        Dialogs.showWirelessAdbResult(this@MainActivity, true, message) {
                            updateDeviceOwnerStatus()
                        }
                    },
                    onFailure = { exception ->
                        val errorMessage = exception.message ?: "Unknown error"
                        Dialogs.showWirelessAdbResult(this@MainActivity, false, errorMessage)
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                progressDialog.dismiss()
                Dialogs.showWirelessAdbResult(
                    this@MainActivity,
                    false,
                    "Error: ${e.message}\n\nMake sure:\n1. Wireless debugging is ON\n2. You ran 'adb connect' in Termux\n3. android-tools is installed in Termux"
                )
            }
        }
    }

    private fun installSilently(apkPath: String) {
        Log.d(TAG, "Starting silent installation for APK: $apkPath")

        // Check installation method priority: Device Owner (TRUE silent) or fallback to auto-click
        if (DeviceAdminUtils.isDeviceOwner(this)) {
            // TRUE silent installation with Device Owner
            progressBar.visibility = ProgressBar.VISIBLE
            statusText.text = "Installing silently (no user interaction)..."

            installerService.installSilentlyAsDeviceAdmin(apkPath) { success, message ->
                progressBar.visibility = ProgressBar.GONE
                statusText.text = message
                if (!success) {
                    showErrorDialog(this, message)
                }
            }
        } else if (DeviceAdminUtils.isDeviceAdmin(this)) {
            // Device Admin alone cannot do silent install, show explanation
            Dialogs.showDeviceAdminLimitationDialog(this) { isConfirmed ->
                Log.d(TAG, "Device Admin limitation dialog result: $isConfirmed")
                if (isConfirmed) Dialogs.showDeviceAdminDialog(this) { isOk ->
                    if (isOk) DeviceAdminUtils.requestDeviceAdmin(this)
                }

            }
        } else {
            // No silent installation method available, show options
            Dialogs.showSilentInstallNotAvailableDialog(this) { isConfirmed ->
                if (isConfirmed) Dialogs.showDeviceAdminDialog(this) { enableDeviceAdmin ->
                    if (enableDeviceAdmin) {
                        DeviceAdminUtils.requestDeviceAdmin(this)
                    }
                }
            }
        }

    }

    private fun installByAutoClick(apkPath: String) {
        Log.d(TAG, "Starting silent installation for APK: $apkPath")

        // Check if accessibility service is enabled
        if (!AccessibilityUtils.isAccessibilityServiceEnabled(this, AutoInstallService::class.java)) {
            Dialogs.showAccessibilityServiceDialog(this) { isConfirmed ->
                if (isConfirmed) AccessibilityUtils.openAccessibilitySettings(this)
            }
            return
        }

        // Accessibility service is enabled, proceed with installation
        progressBar.visibility = ProgressBar.VISIBLE
        statusText.text = "Installing by auto click..."

        installerService.installSilentlyByAutoClick(apkPath) { success, message ->
            progressBar.visibility = ProgressBar.GONE
            statusText.text = message
            if (!success) showErrorDialog(this, message)
        }
    }

    private fun installApkRegular(apkPath: String) {
        Log.d(TAG, "Starting installation for APK: $apkPath")
        progressBar.visibility = ProgressBar.VISIBLE
        statusText.text = "Installing..."

        installerService.installApk(apkPath) { success, message ->
            progressBar.visibility = ProgressBar.GONE
            statusText.text = message
            if (!success) {
                showErrorDialog(this, message)
            }
        }
    }


    companion object {
        const val TAG = "MainActivity1"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
