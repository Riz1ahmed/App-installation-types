package com.example.silent_installapp

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Device Admin Receiver for managing device admin privileges
 * Required for silent installation with admin access
 */
class SilentInstallAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin disabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.d(TAG, "Device Admin disable requested")
        return "Warning: Disabling device admin will prevent silent app installation"
    }

    companion object {
        private const val TAG = "SilentInstallAdminReceiver"
    }
}

