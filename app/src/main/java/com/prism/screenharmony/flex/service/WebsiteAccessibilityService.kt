package com.prism.screenharmony.flex.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.prism.screenharmony.flex.data.BlockRepository
import com.prism.screenharmony.flex.data.PauseType
import com.prism.screenharmony.flex.data.WallConfig
import com.prism.screenharmony.flex.ui.blocker.BlockedActivity
import kotlinx.coroutines.*

class WebsiteAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastBlockedUrl: String? = null

    // Common browser URL bar IDs
    private val browserUrlBarIds = mapOf(
        "com.android.chrome" to "com.android.chrome:id/url_bar",
        "org.mozilla.firefox" to "org.mozilla.firefox:id/url_bar_title",
        "com.opera.browser" to "com.opera.browser:id/url_field",
        "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
        "com.brave.browser" to "com.brave.browser:id/url_bar"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        if (browserUrlBarIds.containsKey(packageName)) {
            checkAndBlockWebsite(packageName)
        }
    }

    private fun checkAndBlockWebsite(packageName: String) {
        val rootNode = rootInActiveWindow ?: return
        val urlNode = findUrlNode(rootNode, browserUrlBarIds[packageName])

        if (urlNode != null) {
            val currentUrl = urlNode.text?.toString() ?: ""
            if (currentUrl.isNotBlank()) {
                val match = BlockRepository.getActiveRuleForWebsite(currentUrl)
                if (match != null) {
                    val (rule, domain) = match
                    if (lastBlockedUrl != domain) {
                        lastBlockedUrl = domain
                        val customQuote = if (rule.wallConfig is WallConfig.StandardQuote) {
                            (rule.wallConfig as WallConfig.StandardQuote).quote
                        } else null

                        val delaySec = if (rule.pauseConfig.type == PauseType.DELAY) {
                            rule.pauseConfig.extraValue ?: 5
                        } else if (rule.pauseConfig.type == PauseType.STRICT) {
                            0
                        } else {
                            5
                        }

                        redirectAndBlock(urlNode, domain, customQuote, delaySec)
                    }
                } else {
                    lastBlockedUrl = null
                    urlNode.recycle()
                }
            } else {
                urlNode.recycle()
            }
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
        val description = node.contentDescription?.toString()?.lowercase() ?: ""
        val className = node.className?.toString() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        val isUrlBar = (className.contains("EditText") || className.contains("View")) && (
            description.contains("address") ||
            description.contains("url") ||
            description.contains("search") ||
            text.matches(Regex(".*\\.[a-z]{2,6}(/.*)?"))
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

    private fun redirectAndBlock(
        urlNode: AccessibilityNodeInfo,
        blockedDomain: String,
        quote: String?,
        delaySeconds: Int
    ) {
        // Clear the URL in browser to prevent auto-reloading
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "about:blank")
        urlNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        urlNode.recycle()

        // Launch direct block overlay
        val intent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            putExtra("TARGET", blockedDomain)
            putExtra("IS_WEBSITE", true)
            putExtra("QUOTE", quote)
            putExtra("DELAY_SECONDS", delaySeconds)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
