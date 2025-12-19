package com.example.silent_installapp


// InstallReceiver.kt - BroadcastReceiver for installation status
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) {
            Log.e(TAG, "Received null intent")
            return
        }

        Log.d(TAG, "Received intent action: ${intent.action}")
        Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString()}")

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        Log.d(TAG, "Installation status received: $status")

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Installation confirmation needed
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                Log.d(TAG, "Confirm intent: $confirmIntent")
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                    Log.d(TAG, "Started confirmation activity")
                } else {
                    Log.w(TAG, "No confirmation intent available")
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d(TAG, "Installation successful")
                Toast.makeText(context, "Installation successful!", Toast.LENGTH_SHORT).show()
            }
            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                val statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.e(TAG, "Installation failed: $statusMessage")
                Toast.makeText(context, "Installation failed: $statusMessage", Toast.LENGTH_LONG).show()
            }
            -1 -> {
                Log.w(TAG, "Received status -1, checking for other intent extras")
                val allExtras = intent.extras
                if (allExtras != null) {
                    for (key in allExtras.keySet()) {
                        Log.d(TAG, "Extra: $key = ${allExtras.get(key)}")
                    }
                }
                Toast.makeText(context, "Installation status unknown", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.w(TAG, "Unknown status: $status")
                Toast.makeText(context, "Unknown installation status: $status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "InstallReceiver"
    }
}