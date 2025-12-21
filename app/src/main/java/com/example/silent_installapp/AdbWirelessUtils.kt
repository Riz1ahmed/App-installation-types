package com.example.silent_installapp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

/**
 * Utility class for executing ADB commands over wireless debugging
 * This allows the app to connect to ADB server and execute commands without Termux
 */
object AdbWirelessUtils {
    private const val TAG = "AdbWirelessUtils"
    private const val ADB_DEFAULT_PORT = 5037

    /**
     * Check if ADB server is running on localhost
     */
    fun isAdbServerRunning(port: Int = ADB_DEFAULT_PORT): Boolean {
        return try {
            Socket("localhost", port).use {
                it.isConnected
            }
        } catch (e: Exception) {
            Log.d(TAG, "ADB server not accessible: ${e.message}")
            false
        }
    }

    /**
     * Execute shell command to start ADB server
     * Note: This requires the android-tools package to be installed (via Termux or root)
     */
    suspend fun startAdbServer(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("adb start-server")
            val exitCode = process.waitFor()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }

            if (exitCode == 0) {
                Result.success("ADB server started: $output")
            } else {
                Result.failure(Exception("Failed to start ADB server: $error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ADB server: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Connect to wireless debugging
     * @param ipAddress Device IP address (e.g., "192.168.1.100")
     * @param port Wireless debugging port (from Developer Options)
     */
    suspend fun connectWirelessDebugging(ipAddress: String, port: Int): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val command = "adb connect $ipAddress:$port"
                val process = Runtime.getRuntime().exec(command)
                val exitCode = process.waitFor()

                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }

                if (exitCode == 0 || output.contains("connected")) {
                    Result.success(output)
                } else {
                    Result.failure(Exception("Connection failed: $error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to wireless debugging: ${e.message}", e)
                Result.failure(e)
            }
        }

    /**
     * Connect to local ADB (when ADB is running on the same device via Termux)
     */
    suspend fun connectLocalAdb(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val command = "adb connect localhost:5037"
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()

            val output = process.inputStream.bufferedReader().use { it.readText() }

            if (exitCode == 0 || output.contains("connected")) {
                Result.success(output)
            } else {
                Result.failure(Exception("Connection failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to local ADB: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Set device owner using ADB command
     * This is the main function to achieve what you did in Termux
     */
    suspend fun setDeviceOwnerViaAdb(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val componentName = "com.example.silent_installapp/.SilentInstallAdminReceiver"
            val command = "adb shell dpm set-device-owner $componentName"

            Log.d(TAG, "Executing: $command")

            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }

            Log.d(TAG, "Output: $output")
            Log.d(TAG, "Error: $error")

            when {
                output.contains("Success") -> {
                    Result.success("✓ Device Owner set successfully!")
                }
                error.contains("multiple users") -> {
                    Result.failure(Exception("Multiple users exist. Remove all secondary users first."))
                }
                error.contains("already") -> {
                    Result.failure(Exception("Device owner already set."))
                }
                error.contains("not allowed") -> {
                    Result.failure(Exception("Not allowed. Device may need factory reset."))
                }
                exitCode != 0 -> {
                    Result.failure(Exception("Command failed: $error"))
                }
                else -> {
                    Result.success(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting device owner via ADB: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Complete workflow: Connect to ADB and set device owner
     * This combines all steps you did in Termux
     */
    suspend fun setupDeviceOwnerViaWirelessAdb(
        context: Context,
        ipAddress: String? = null,
        port: Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Check if ADB tools are available
            if (!isAdbCommandAvailable()) {
                return@withContext Result.failure(
                    Exception("ADB tools not found. Install android-tools first:\n" +
                            "1. Install Termux\n" +
                            "2. Run: pkg install android-tools")
                )
            }

            // Step 2: Start ADB server if not running
            if (!isAdbServerRunning()) {
                val startResult = startAdbServer()
                if (startResult.isFailure) {
                    return@withContext Result.failure(
                        Exception("Failed to start ADB server: ${startResult.exceptionOrNull()?.message}")
                    )
                }
            }

            // Step 3: Connect to device
            val connectResult = if (ipAddress != null && port != null) {
                connectWirelessDebugging(ipAddress, port)
            } else {
                connectLocalAdb()
            }

            if (connectResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Failed to connect to ADB: ${connectResult.exceptionOrNull()?.message}")
                )
            }

            // Step 4: Set device owner
            setDeviceOwnerViaAdb(context)

        } catch (e: Exception) {
            Log.e(TAG, "Error in complete workflow: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if ADB command is available (android-tools installed)
     */
    private fun isAdbCommandAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("adb version")
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get list of connected ADB devices
     */
    suspend fun getConnectedDevices(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("adb devices")
            process.waitFor()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val devices = output.lines()
                .filter { it.contains("\tdevice") }
                .map { it.split("\t")[0] }

            Result.success(devices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Instructions for setting up wireless debugging
     */
    const val WIRELESS_DEBUG_INSTRUCTIONS = """
        📱 WIRELESS DEBUGGING SETUP

        Before using this feature:

        1️⃣ Enable Wireless Debugging:
           Settings → Developer Options → Wireless debugging → ON

        2️⃣ Get IP address and Port:
           Tap on "Wireless debugging"
           Note the IP address and Port (e.g., 192.168.1.100:12345)

        3️⃣ Install ADB Tools (one-time setup):
           • Install Termux from F-Droid
           • Run in Termux: pkg install android-tools
           • This installs 'adb' command on your device

        4️⃣ Use this app:
           • Enter the IP and Port from step 2
           • Tap "Connect & Set Device Owner"
           • Your app will become Device Owner automatically!

        ⚠️ Requirements:
        • No secondary user accounts on device
        • Device Admin must be enabled first
        • Wireless debugging must stay ON during setup
    """

    /**
     * Simpler method using localhost (when ADB is running locally via Termux)
     */
    suspend fun setupDeviceOwnerViaLocalAdb(context: Context): Result<String> {
        return try {
            // Just run the command directly - assumes ADB is already connected
            setDeviceOwnerViaAdb(context)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

