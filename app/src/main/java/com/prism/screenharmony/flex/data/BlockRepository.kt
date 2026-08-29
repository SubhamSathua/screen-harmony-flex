package com.prism.screenharmony.flex.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalTime

object BlockRepository {
    private const val TAG = "ScreenHarmony_Repository"
    private const val PREFS_NAME = "screen_harmony_block_rules"
    private const val KEY_RULES_JSON = "saved_rules_json"

    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var prefs: SharedPreferences? = null

    private val _rules = MutableStateFlow<List<BlockRule>>(emptyList())
    val rules: StateFlow<List<BlockRule>> = _rules.asStateFlow()

    private var dbHelper: com.prism.screenharmony.flex.data.db.AppDatabaseHelper? = null

    fun initialize(context: Context) {
        if (prefs != null && dbHelper != null) return
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        dbHelper = com.prism.screenharmony.flex.data.db.AppDatabaseHelper.getInstance(appContext)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        // 1. Try loading from SQLite database
        val sqliteJsons = dbHelper?.loadAllRulesJson() ?: emptyList()
        if (sqliteJsons.isNotEmpty()) {
            try {
                val loadedRules = mutableListOf<BlockRule>()
                for (json in sqliteJsons) {
                    val parsedList = deserializeRules(json)
                    if (parsedList.isNotEmpty()) {
                        loadedRules.addAll(parsedList)
                    }
                }
                if (loadedRules.isNotEmpty()) {
                    _rules.value = loadedRules
                    Log.i(TAG, "Loaded ${loadedRules.size} persistent rules from SQLite database.")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse SQLite rules", e)
            }
        }

        // 2. Fallback to SharedPreferences if legacy data exists
        val jsonStr = prefs?.getString(KEY_RULES_JSON, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val loadedRules = deserializeRules(jsonStr)
                if (loadedRules.isNotEmpty()) {
                    _rules.value = loadedRules
                    Log.i(TAG, "Loaded ${loadedRules.size} persistent rules from SharedPreferences.")
                    saveToDisk(loadedRules) // migrate to SQLite
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse saved rules JSON", e)
            }
        }

        // Fresh start -> No default template, show empty state
        _rules.value = emptyList()
    }

    private fun saveToDisk(rulesList: List<BlockRule>) {
        repositoryScope.launch {
            try {
                val jsonStr = serializeRules(rulesList)
                prefs?.edit()?.putString(KEY_RULES_JSON, jsonStr)?.apply()

                // Save to SQLite
                val triples = rulesList.map { Triple(it.id, it.name, it.isEnabled) }
                val singleJsons = rulesList.map { serializeRules(listOf(it)) }
                dbHelper?.syncAllRules(triples, singleJsons)

                Log.d(TAG, "Successfully synced ${rulesList.size} rules to SQLite database.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save rules to disk/SQLite", e)
            }
        }
    }

    fun getActiveRuleForApp(packageName: String): BlockRule? {
        val now = LocalTime.now()
        val day = DayOfWeek.from(java.time.LocalDate.now())
        return _rules.value.firstOrNull { rule ->
            rule.selectedApps.contains(packageName) && rule.isCurrentlyBlocked(now, day)
        }
    }

    fun hasActiveStrictBlock(): Boolean {
        val now = LocalTime.now()
        val day = DayOfWeek.from(java.time.LocalDate.now())
        return _rules.value.any { rule ->
            rule.blockType == BlockType.STRICT && rule.isCurrentlyBlocked(now, day)
        }
    }

    fun getActiveRuleForWebsite(url: String): Pair<BlockRule, String>? {
        val now = LocalTime.now()
        val day = DayOfWeek.from(java.time.LocalDate.now())
        val hostFromUrl = extractHost(url) ?: return null

        for (rule in _rules.value) {
            if (rule.isCurrentlyBlocked(now, day)) {
                for (domain in rule.selectedWebsites) {
                    val cleanDomain = extractHost(domain) ?: domain.lowercase().trim()
                    if (cleanDomain.isNotEmpty() && isDomainMatching(hostFromUrl, cleanDomain)) {
                        return Pair(rule, cleanDomain)
                    }
                }
            }
        }
        return null
    }

    private fun extractHost(raw: String): String? {
        val clean = raw.lowercase().trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .substringBefore("/")
            .substringBefore("?")
            .substringBefore(":")
        return if (clean.isNotEmpty()) clean else null
    }

    private fun isDomainMatching(host: String, blockedDomain: String): Boolean {
        return host == blockedDomain || host.endsWith(".$blockedDomain")
    }

    fun cleanExpiredPauses() {
        val current = _rules.value
        var changed = false
        val updated = current.map { rule ->
            if (rule.lastPausedAt != null && !rule.isPaused()) {
                changed = true
                rule.copy(lastPausedAt = null, pauseDurationMinutes = null)
            } else rule
        }
        if (changed) {
            _rules.value = updated
            saveToDisk(updated)
            com.prism.screenharmony.flex.service.AppBlockerService.resetInterceptState()
            Log.i(TAG, "cleanExpiredPauses: Expired paused blocks reactivated and intercept reset")
        }
    }

    fun addOrUpdateRule(rule: BlockRule) {
        Log.i(TAG, "addOrUpdateRule: '${rule.name}' with ${rule.selectedApps.size} apps, ${rule.selectedWebsites.size} websites")
        val current = _rules.value
        val exists = current.any { it.id == rule.id }
        val updated = if (exists) {
            current.map { if (it.id == rule.id) rule else it }
        } else {
            current + rule
        }
        _rules.value = updated
        saveToDisk(updated)
    }

    fun toggleRule(ruleId: String, isEnabled: Boolean) {
        Log.i(TAG, "toggleRule: id=$ruleId -> isEnabled=$isEnabled")
        val updated = _rules.value.map {
            if (it.id == ruleId) it.copy(isEnabled = isEnabled) else it
        }
        _rules.value = updated
        saveToDisk(updated)
    }

    fun deleteRule(ruleId: String) {
        Log.i(TAG, "deleteRule: id=$ruleId")
        val updated = _rules.value.filterNot { it.id == ruleId }
        _rules.value = updated
        saveToDisk(updated)
    }

    fun pauseRule(ruleId: String, durationMinutes: Int) {
        Log.i(TAG, "pauseRule: id=$ruleId for ${durationMinutes}m")
        val updated = _rules.value.map {
            if (it.id == ruleId) {
                it.copy(
                    lastPausedAt = System.currentTimeMillis(),
                    pauseDurationMinutes = durationMinutes
                )
            } else it
        }
        _rules.value = updated
        saveToDisk(updated)
    }

    fun unpauseRule(ruleId: String) {
        Log.i(TAG, "unpauseRule: id=$ruleId")
        val updated = _rules.value.map {
            if (it.id == ruleId) {
                it.copy(lastPausedAt = null, pauseDurationMinutes = null)
            } else it
        }
        _rules.value = updated
        saveToDisk(updated)
    }

    // ==========================================
    // JSON SERIALIZATION / DESERIALIZATION
    // ==========================================

    private fun serializeRules(rulesList: List<BlockRule>): String {
        val rootArray = JSONArray()
        for (rule in rulesList) {
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("isEnabled", rule.isEnabled)
                put("blockAppLaunch", rule.blockAppLaunch)
                put("blockNotifications", rule.blockNotifications)
                put("blockDurationSeconds", rule.blockDurationSeconds)
                put("lastPausedAt", rule.lastPausedAt ?: JSONObject.NULL)
                put("pauseDurationMinutes", rule.pauseDurationMinutes ?: JSONObject.NULL)

                // Apps
                val appsArray = JSONArray()
                rule.selectedApps.forEach { appsArray.put(it) }
                put("selectedApps", appsArray)

                // Websites
                val sitesArray = JSONArray()
                rule.selectedWebsites.forEach { sitesArray.put(it) }
                put("selectedWebsites", sitesArray)

                // PauseConfig
                val pauseObj = JSONObject().apply {
                    put("type", rule.pauseConfig.type.name)
                    put("extraValue", rule.pauseConfig.extraValue ?: JSONObject.NULL)
                    put("typeTextLength", rule.pauseConfig.typeTextLength)
                    put("typeTextCount", rule.pauseConfig.typeTextCount)
                }
                put("pauseConfig", pauseObj)

                // Conditions (WeeklySchedule slots)
                val condArray = JSONArray()
                rule.conditions.forEach { cond ->
                    if (cond is BlockCondition.WeeklySchedule) {
                        val condObj = JSONObject().apply {
                            put("type", "WeeklySchedule")
                            put("id", cond.id)
                            val slotsArray = JSONArray()
                            cond.slots.forEach { slot ->
                                val slotObj = JSONObject().apply {
                                    put("id", slot.id)
                                    put("dayBitmask", slot.dayBitmask)
                                    put("startMinute", slot.startMinute)
                                    put("endMinute", slot.endMinute)
                                }
                                slotsArray.put(slotObj)
                            }
                            put("slots", slotsArray)
                        }
                        condArray.put(condObj)
                    }
                }
                put("conditions", condArray)

                // WallConfig
                val wallObj = JSONObject().apply {
                    when (val wall = rule.wallConfig) {
                        is WallConfig.StandardQuote -> {
                            put("type", "StandardQuote")
                            put("quote", wall.quote ?: JSONObject.NULL)
                        }
                        WallConfig.Emoji -> put("type", "Emoji")
                        WallConfig.Task -> put("type", "Task")
                    }
                }
                put("wallConfig", wallObj)
            }
            rootArray.put(obj)
        }
        return rootArray.toString()
    }

    private fun deserializeRules(jsonStr: String): List<BlockRule> {
        val rootArray = JSONArray(jsonStr)
        val result = mutableListOf<BlockRule>()

        for (i in 0 until rootArray.length()) {
            val obj = rootArray.getJSONObject(i)
            val id = obj.getString("id")
            val name = obj.getString("name")
            val isEnabled = obj.optBoolean("isEnabled", true)
            val blockAppLaunch = obj.optBoolean("blockAppLaunch", true)
            val blockNotifications = obj.optBoolean("blockNotifications", false)
            val blockDurationSeconds = obj.optInt("blockDurationSeconds", 5)
            val lastPausedAt = if (obj.isNull("lastPausedAt")) null else obj.getLong("lastPausedAt")
            val pauseDurationMinutes = if (obj.isNull("pauseDurationMinutes")) null else obj.getInt("pauseDurationMinutes")

            // Apps
            val selectedApps = mutableSetOf<String>()
            val appsArray = obj.optJSONArray("selectedApps")
            if (appsArray != null) {
                for (j in 0 until appsArray.length()) {
                    selectedApps.add(appsArray.getString(j))
                }
            }

            // Websites
            val selectedWebsites = mutableSetOf<String>()
            val sitesArray = obj.optJSONArray("selectedWebsites")
            if (sitesArray != null) {
                for (j in 0 until sitesArray.length()) {
                    selectedWebsites.add(sitesArray.getString(j))
                }
            }

            // PauseConfig
            val pauseConfig = if (obj.has("pauseConfig")) {
                val pObj = obj.getJSONObject("pauseConfig")
                val pType = try {
                    PauseType.valueOf(pObj.getString("type"))
                } catch (e: Exception) {
                    PauseType.DELAY
                }
                val extraVal = if (pObj.isNull("extraValue")) null else pObj.getInt("extraValue")
                PauseConfig(
                    type = pType,
                    extraValue = extraVal,
                    typeTextLength = pObj.optInt("typeTextLength", 5),
                    typeTextCount = pObj.optInt("typeTextCount", 3)
                )
            } else {
                PauseConfig()
            }

            // Conditions
            val conditions = mutableListOf<BlockCondition>()
            val condArray = obj.optJSONArray("conditions")
            if (condArray != null) {
                for (j in 0 until condArray.length()) {
                    val condObj = condArray.getJSONObject(j)
                    if (condObj.optString("type") == "WeeklySchedule") {
                        val condId = condObj.optString("id")
                        val slots = mutableListOf<TimeSlot>()
                        val slotsArray = condObj.optJSONArray("slots")
                        if (slotsArray != null) {
                            for (k in 0 until slotsArray.length()) {
                                val sObj = slotsArray.getJSONObject(k)
                                slots.add(
                                    TimeSlot(
                                        id = sObj.optString("id"),
                                        dayBitmask = sObj.optInt("dayBitmask", DayBitmask.ALL),
                                        startMinute = sObj.optInt("startMinute", 0),
                                        endMinute = sObj.optInt("endMinute", 1439)
                                    )
                                )
                            }
                        }
                        conditions.add(BlockCondition.WeeklySchedule(id = condId, slots = slots))
                    }
                }
            }

            // WallConfig
            val wallConfig = if (obj.has("wallConfig")) {
                val wObj = obj.getJSONObject("wallConfig")
                when (wObj.optString("type")) {
                    "StandardQuote" -> WallConfig.StandardQuote(
                        quote = if (wObj.isNull("quote")) null else wObj.getString("quote")
                    )
                    "Emoji" -> WallConfig.Emoji
                    "Task" -> WallConfig.Task
                    else -> WallConfig.StandardQuote()
                }
            } else {
                WallConfig.StandardQuote()
            }

            result.add(
                BlockRule(
                    id = id,
                    name = name,
                    isEnabled = isEnabled,
                    selectedApps = selectedApps,
                    selectedWebsites = selectedWebsites,
                    blockAppLaunch = blockAppLaunch,
                    blockNotifications = blockNotifications,
                    pauseConfig = pauseConfig,
                    conditions = conditions,
                    blockDurationSeconds = blockDurationSeconds,
                    wallConfig = wallConfig,
                    lastPausedAt = lastPausedAt,
                    pauseDurationMinutes = pauseDurationMinutes
                )
            )
        }
        return result
    }
}
