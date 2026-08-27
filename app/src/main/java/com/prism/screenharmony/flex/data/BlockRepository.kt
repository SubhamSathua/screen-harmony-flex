package com.prism.screenharmony.flex.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

object BlockRepository {
    private val _rules = MutableStateFlow<List<BlockRule>>(
        listOf(
            BlockRule(
                id = "default_social_block",
                name = "Social & Gaming",
                isEnabled = true,
                selectedApps = setOf(
                    "com.google.android.youtube",
                    "com.instagram.android",
                    "com.zhiliaoapp.musically",
                    "com.roblox.client"
                ),
                selectedWebsites = setOf("instagram.com", "tiktok.com", "youtube.com"),
                showQuotes = true,
                pauseDelaySeconds = 0 // Strict
            )
        )
    )
    val rules: StateFlow<List<BlockRule>> = _rules.asStateFlow()

    fun getActiveRuleForApp(packageName: String): BlockRule? {
        val now = LocalTime.now()
        val day = java.time.DayOfWeek.from(java.time.LocalDate.now())
        return _rules.value.firstOrNull { rule ->
            rule.selectedApps.contains(packageName) && rule.isCurrentlyActive(now, day)
        }
    }

    fun getActiveRuleForWebsite(url: String): Pair<BlockRule, String>? {
        val now = LocalTime.now()
        val day = java.time.DayOfWeek.from(java.time.LocalDate.now())
        val cleanUrl = url.lowercase().trim()
        
        for (rule in _rules.value) {
            if (rule.isCurrentlyActive(now, day)) {
                for (domain in rule.selectedWebsites) {
                    val cleanDomain = domain.lowercase().trim()
                    if (cleanDomain.isNotEmpty() && cleanUrl.contains(cleanDomain)) {
                        return Pair(rule, cleanDomain)
                    }
                }
            }
        }
        return null
    }

    fun addRule(rule: BlockRule) {
        _rules.value = _rules.value + rule
    }

    fun updateRule(updatedRule: BlockRule) {
        _rules.value = _rules.value.map { if (it.id == updatedRule.id) updatedRule else it }
    }

    fun toggleRule(ruleId: String, isEnabled: Boolean) {
        _rules.value = _rules.value.map {
            if (it.id == ruleId) it.copy(isEnabled = isEnabled) else it
        }
    }

    fun deleteRule(ruleId: String) {
        _rules.value = _rules.value.filterNot { it.id == ruleId }
    }

    fun pauseRule(ruleId: String, durationMinutes: Int) {
        val until = System.currentTimeMillis() + (durationMinutes * 60 * 1000)
        _rules.value = _rules.value.map {
            if (it.id == ruleId) it.copy(lastPausedUntil = until) else it
        }
    }
}
