package com.prism.screenharmony.flex.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.prism.screenharmony.flex.data.BlockRepository
import com.prism.screenharmony.flex.data.PauseType
import com.prism.screenharmony.flex.data.WallConfig
import com.prism.screenharmony.flex.ui.blocker.BlockedActivity
import kotlinx.coroutines.*

class WebsiteAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastBlockedPackage: String? = null
    private var lastBlockedUrl: String? = null

    companion object {
        private const val TAG = "ScreenHarmony_Accessibility"
        private var instance: WebsiteAccessibilityService? = null

        fun lockDevice(): Boolean {
            val service = instance
            if (service == null) {
                Log.w(TAG, "lockDevice: WebsiteAccessibilityService instance is null (not enabled or not connected)")
                return false
            }
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val success = service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Log.i(TAG, "lockDevice: GLOBAL_ACTION_LOCK_SCREEN performed = $success")
                success
            } else {
                service.performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }

        fun isAccessibilityActive(): Boolean = instance != null

        fun launchBlockWall(
            context: android.content.Context,
            target: String,
            isWebsite: Boolean = false,
            quote: String? = null,
            delaySeconds: Int = 0
        ) {
            val intent = Intent(context, BlockedActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra("TARGET", target)
                putExtra("IS_WEBSITE", isWebsite)
                putExtra("QUOTE", quote)
                putExtra("DELAY_SECONDS", delaySeconds)
            }
            context.startActivity(intent)
        }
    }

    // Common browser URL bar IDs
    private val browserUrlBarIds = mapOf(
        "com.android.chrome" to "com.android.chrome:id/url_bar",
        "org.mozilla.firefox" to "org.mozilla.firefox:id/url_bar_title",
        "com.opera.browser" to "com.opera.browser:id/url_field",
        "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
        "com.brave.browser" to "com.brave.browser:id/url_bar"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "WebsiteAccessibilityService connected & active")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        // 1. Check if this is an app that needs to be blocked
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val matchingAppRule = BlockRepository.getActiveRuleForApp(packageName)
            if (matchingAppRule != null) {
                if (lastBlockedPackage != packageName) {
                    lastBlockedPackage = packageName
                    Log.i(TAG, "🚨 ACCESSIBILITY: Intercepted blocked app '$packageName' | Rule: '${matchingAppRule.name}'")

                    val customQuote = if (matchingAppRule.wallConfig is WallConfig.StandardQuote) {
                        (matchingAppRule.wallConfig as WallConfig.StandardQuote).quote
                    } else null

                    val delaySec = matchingAppRule.blockDurationSeconds

                    // Launch Lock Wall directly over the blocked app
                    launchBlockWall(target = packageName, isWebsite = false, quote = customQuote, delaySeconds = delaySec)

                    serviceScope.launch {
                        kotlinx.coroutines.delay(1500)
                        lastBlockedPackage = null
                    }
                }
                return
            } else {
                lastBlockedPackage = null
            }
        }

        // 2. Check if browser URL needs to be blocked (skip keystroke text-change events)
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        ) {
            if (browserUrlBarIds.containsKey(packageName) || browserPackages.contains(packageName)) {
                checkAndBlockWebsite(packageName)
            }
        }

        // 3. Strict Mode Anti-Bypass Guard
        if (BlockRepository.hasActiveStrictBlock()) {
            if (packageName == "com.android.settings" ||
                packageName == "com.google.android.packageinstaller" ||
                packageName == "com.android.packageinstaller"
            ) {
                val rootNode = rootInActiveWindow
                if (rootNode != null && isTargetingScreenHarmony(rootNode)) {
                    Log.w(TAG, "🚨 STRICT MODE GUARD: Intercepted attempt to modify ScreenHarmony in Settings!")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(
                            this,
                            "Strict Mode Active: App modifications and uninstallation are locked during focus session.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    rootNode.recycle()
                    return
                }
                rootNode?.recycle()
            }
        }
    }

    private fun isTargetingScreenHarmony(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (text.contains("screenharmony") || desc.contains("screenharmony") ||
            text.contains("screen harmony") || desc.contains("screen harmony") ||
            text.contains("com.prism.screenharmony")
        ) {
            return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (isTargetingScreenHarmony(child)) {
                child?.recycle()
                return true
            }
            child?.recycle()
        }
        return false
    }

    private val browserPackages = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.google.android.apps.chrome"
    )

    private fun checkAndBlockWebsite(packageName: String) {
        val rootNode = rootInActiveWindow ?: return
        val urlNode = findUrlNode(rootNode, browserUrlBarIds[packageName])

        if (urlNode != null) {
            // CRITICAL FIX: If the user is currently typing/focused in the address bar, DO NOT BLOCK!
            // This prevents interfering with typing, autocomplete suggestions, and search queries.
            if (urlNode.isFocused) {
                urlNode.recycle()
                rootNode.recycle()
                return
            }

            val currentUrl = urlNode.text?.toString()?.trim() ?: ""
            if (currentUrl.isNotBlank()) {
                val match = BlockRepository.getActiveRuleForWebsite(currentUrl)
                if (match != null) {
                    val (rule, domain) = match
                    if (lastBlockedUrl != domain) {
                        lastBlockedUrl = domain
                        Log.i(TAG, "🚨 ACCESSIBILITY: Blocked website '$domain' in '$packageName'")

                        val customQuote = if (rule.wallConfig is WallConfig.StandardQuote) {
                            (rule.wallConfig as WallConfig.StandardQuote).quote
                        } else null

                        val delaySec = rule.blockDurationSeconds
                        val redirectUrl = com.prism.screenharmony.flex.utils.AppConstants.BROWSER_REDIRECT_URL

                        // 1. Force the browser tab to actually load the fallback redirect URL (e.g. google.com)
                        try {
                            val redirectIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(redirectUrl)).apply {
                                setPackage(packageName)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(redirectIntent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to launch redirect intent to browser", e)
                        }

                        // 2. Set the address bar text for instant synchronization
                        try {
                            val arguments = Bundle().apply {
                                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, redirectUrl)
                            }
                            urlNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update URL bar text", e)
                        }

                        // 3. Launch the Lock Wall directly over the browser
                        launchBlockWall(target = domain, isWebsite = true, quote = customQuote, delaySeconds = delaySec)

                        serviceScope.launch {
                            kotlinx.coroutines.delay(1500)
                            lastBlockedUrl = null
                        }
                    }
                } else {
                    lastBlockedUrl = null
                }
            }
            urlNode.recycle()
        }
        rootNode.recycle()
    }

    private fun findUrlNode(root: AccessibilityNodeInfo, targetId: String?): AccessibilityNodeInfo? {
        if (targetId != null) {
            val nodes = root.findAccessibilityNodeInfosByViewId(targetId)
            if (!nodes.isNullOrEmpty()) return nodes[0]
        }
        return recursiveFindUrlNode(root)
    }

    private fun recursiveFindUrlNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val description = node.contentDescription?.toString()?.lowercase() ?: ""
        val className = node.className?.toString() ?: ""

        val isUrlBar = (className.contains("EditText") || className.contains("AutoCompleteTextView")) && (
            viewId.contains("url") ||
            viewId.contains("location") ||
            viewId.contains("omnibox") ||
            viewId.contains("search_box") ||
            description.contains("address and search bar") ||
            description.contains("search or type url") ||
            description.contains("address")
        )

        if (isUrlBar) return AccessibilityNodeInfo.obtain(node)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = recursiveFindUrlNode(child)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    private fun launchBlockWall(
        target: String,
        isWebsite: Boolean,
        quote: String?,
        delaySeconds: Int
    ) {
        val intent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("TARGET", target)
            putExtra("IS_WEBSITE", isWebsite)
            putExtra("QUOTE", quote)
            putExtra("DELAY_SECONDS", delaySeconds)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.w(TAG, "WebsiteAccessibilityService interrupted")
    }

    override fun onDestroy() {
        if (instance == this) instance = null
        super.onDestroy()
        Log.w(TAG, "WebsiteAccessibilityService destroyed")
        serviceScope.cancel()
    }
}
