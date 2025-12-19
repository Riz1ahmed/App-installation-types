package com.example.silent_installapp

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import kotlin.math.log10

/* created by @Riz1 on 19/12/2025 */

/**
 * Get the real path from a URI by copying the content to cache directory
 */
fun getRealPathFromUri(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "temp_app.apk"
        val tempFile = File(context.cacheDir, fileName)
        copyStreamToFile(inputStream, tempFile)
        tempFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Copy an input stream to a file
 */
fun copyStreamToFile(input: InputStream?, output: File) {
    input?.use { inputStream ->
        output.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}

/**
 * Get file size in human-readable format
 */
fun getReadableFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

/**
 * Check if a file is a valid APK file
 */
fun isValidApkFile(file: File): Boolean {
    return file.exists() && file.isFile && file.extension.equals("apk", ignoreCase = true) && file.length() > 0
}

/**
 * Clean up temporary APK files from cache directory
 */
fun cleanupTempApkFiles(context: Context) {
    try {
        val cacheDir = context.cacheDir
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk", ignoreCase = true)) {
                file.delete()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Context.showToast(msg: String) {
    android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
}
