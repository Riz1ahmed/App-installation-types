package com.example.silent_installapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Accessibility Service to automatically click Install/Done buttons during APK installation
 * This enables "silent" installation by automating user interaction
 */
class AutoInstallService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AutoInstallService connected")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags =
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            packageNames = arrayOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.miui.packageinstaller", // Xiaomi
                "com.oppo.packageinstaller", // OPPO
                "com.vivo.packageinstaller", // Vivo
                "com.samsung.android.packageinstaller" // Samsung
            )
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        Log.d(TAG, "Event: ${AccessibilityEvent.eventTypeToString(eventType)} from ${event.packageName}")

        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleInstallationWindow()
            }

            else -> {
                // Ignore other events
            }
        }
    }

    private fun handleInstallationWindow() {
        val rootNode = rootInActiveWindow ?: return

        try {
            // Try to find and click various install-related buttons
            val clicked =
                clickButtonByText(rootNode, "Setup") ||
                clickButtonByText(rootNode, "Install") ||
                clickButtonByText(rootNode, "INSTALL") ||
                clickButtonByText(rootNode, "Continue") ||
                clickButtonByText(rootNode, "CONTINUE") ||
                clickButtonByText(rootNode, "Update") ||
                clickButtonByText(rootNode, "UPDATE") ||
                //clickButtonByText(rootNode, "Next") ||
                //clickButtonByText(rootNode, "NEXT") ||
                //clickButtonByText(rootNode, "Done") ||
                //clickButtonByText(rootNode, "DONE") ||
                //clickButtonByText(rootNode, "Open") ||
                //clickButtonByText(rootNode, "OPEN") ||
                clickButtonById(rootNode, "install") ||
                clickButtonById(rootNode, "continue_button") ||
                clickButtonById(rootNode, "done_button")

            if (clicked) {
                Log.d(TAG, "Successfully clicked installation button")
            } else {
                Log.d(TAG, "No installation button found to click")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling installation window", e)
        } finally {
            rootNode.recycle()
        }
    }

    private fun clickButtonByText(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text?.toString()?.equals(text, ignoreCase = true) == true && node.isClickable) {
            Log.d(TAG, "Found clickable button with text: $text")
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Search in child nodes
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (clickButtonByText(child, text)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }

    private fun clickButtonById(node: AccessibilityNodeInfo, id: String): Boolean {
        val viewId = node.viewIdResourceName
        if (viewId != null && viewId.contains(id, ignoreCase = true) && node.isClickable) {
            Log.d(TAG, "Found clickable button with id: $viewId")
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Search in child nodes
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (clickButtonById(child, id)) {
                child.recycle()
                return true
            }
            child.recycle()
        }

        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "AutoInstallService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AutoInstallService destroyed")
    }

    companion object {
        private const val TAG = "AutoInstallService"
    }
}

