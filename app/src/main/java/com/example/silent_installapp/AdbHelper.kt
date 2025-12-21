package com.example.silent_installapp

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Helper class for ADB operations via wireless debugging
 * This allows the app to execute ADB commands after wireless debugging is paired
 */
object AdbHelper {
    private const val TAG = "AdbHelper"

    /**
     * Execute ADB command
     * Note: This requires 'adb' command to be available (via Termux or embedded binaries)
     */
    fun executeAdbCommand(command: String): Pair<Boolean, String> {
        return try {
            Log.d(TAG, "Executing: $command")

            val process = Runtime.getRuntime().exec(command)
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            Log.d(TAG, "Exit code: $exitCode")
            Log.d(TAG, "Output: $output")
            Log.d(TAG, "Error: $error")

            if (exitCode == 0) {
                Pair(true, output)
            } else {
                Pair(false, error.ifEmpty { output })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Command failed: ${e.message}", e)
            Pair(false, "Error: ${e.message}")
        }
    }

    /**
     * Connect to ADB server via wireless debugging
     */
    fun connectWirelessAdb(host: String, port: Int): Pair<Boolean, String> {
        return executeAdbCommand("adb connect $host:$port")
    }

    /**
     * Set device owner using connected ADB
     */
    fun setDeviceOwner(packageName: String, receiverClass: String): Pair<Boolean, String> {
        return executeAdbCommand("adb shell dpm set-device-owner $packageName/$receiverClass")
    }

    /**
     * Check if ADB is available (installed via Termux or embedded)
     */
    fun isAdbAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("adb version")
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get wireless debugging instructions
     */
    val wirelessDebugInstructions = """
        📱 SETUP WIRELESS DEBUGGING:

        1. Enable Developer Options:
           • Go to Settings → About phone
           • Tap "Build number" 7 times

        2. Enable Wireless Debugging:
           • Settings → Developer Options
           • Turn ON "Wireless debugging"
           • Tap on "Wireless debugging"

        3. Pair Device:
           • Tap "Pair device with pairing code"
           • You'll see IP:Port and 6-digit code
           • Note these down

        4. Install ADB in Termux:
           • Install Termux from F-Droid
           • Run: pkg install android-tools

        5. Pair via Termux:
           • Run: adb pair <IP>:<Port>
           • Enter the 6-digit pairing code

        6. Return to this app and tap "Enable via Wireless ADB"

        ℹ️ After pairing once, you won't need to pair again unless you disable wireless debugging.
    """.trimIndent()
}

