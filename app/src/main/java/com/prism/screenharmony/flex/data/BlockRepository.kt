package com.prism.screenharmony.flex.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
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
                pauseConfig = PauseConfig(type = PauseType.DELAY, extraValue = 5),
                wallConfig = WallConfig.StandardQuote()
            )
        )
    )
    val rules: StateFlow<List<BlockRule>> = _rules.asStateFlow()

    fun getActiveRuleForApp(packageName: String): BlockRule? {
        val now = LocalTime.now()
        val day = DayOfWeek.from(java.time.LocalDate.now())
        return _rules.value.firstOrNull { rule ->
            rule.selectedApps.contains(packageName) && rule.isCurrentlyBlocked(now, day)
        }
    }

    fun getActiveRuleForWebsite(url: String): Pair<BlockRule, String>? {
        val now = LocalTime.now()
        val day = DayOfWeek.from(java.time.LocalDate.now())
        val cleanUrl = url.lowercase().trim()

        for (rule in _rules.value) {
            if (rule.isCurrentlyBlocked(now, day)) {
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

    fun addOrUpdateRule(rule: BlockRule) {
        val current = _rules.value
        val exists = current.any { it.id == rule.id }
        _rules.value = if (exists) {
            current.map { if (it.id == rule.id) rule else it }
        } else {
            current + rule
        }
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
        _rules.value = _rules.value.map {
            if (it.id == ruleId) {
                it.copy(
                    lastPausedAt = System.currentTimeMillis(),
                    pauseDurationMinutes = durationMinutes
                )
            } else it
        }
    }

    fun unpauseRule(ruleId: String) {
        _rules.value = _rules.value.map {
            if (it.id == ruleId) {
                it.copy(lastPausedAt = null, pauseDurationMinutes = null)
            } else it
        }
    }
}
