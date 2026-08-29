package com.prism.screenharmony.flex.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.prism.screenharmony.flex.ui.theme.AppColorPalette
import com.prism.screenharmony.flex.ui.theme.AppThemeMode
import com.prism.screenharmony.flex.ui.theme.ThemeState

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "ScreenHarmony_SQLite"
        private const val DATABASE_NAME = "screenharmony_flex.db"
        private const val DATABASE_VERSION = 1

        // Settings Table
        private const val TABLE_SETTINGS = "app_settings"
        private const val COL_KEY = "setting_key"
        private const val COL_VALUE = "setting_value"

        // Setting Keys
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_IS_AMOLED = "is_amoled"
        const val KEY_COLOR_PALETTE = "color_palette"

        @Volatile
        private var instance: AppDatabaseHelper? = null

        fun getInstance(context: Context): AppDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AppDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.i(TAG, "Creating SQLite database tables...")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SETTINGS (
                $COL_KEY TEXT PRIMARY KEY,
                $COL_VALUE TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Seed default settings
        insertOrUpdateSetting(db, KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        insertOrUpdateSetting(db, KEY_IS_AMOLED, "false")
        insertOrUpdateSetting(db, KEY_COLOR_PALETTE, AppColorPalette.TEAL.name)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "Upgrading SQLite database from $oldVersion to $newVersion")
    }

    private fun insertOrUpdateSetting(db: SQLiteDatabase, key: String, value: String) {
        val values = ContentValues().apply {
            put(COL_KEY, key)
            put(COL_VALUE, value)
        }
        db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun saveSetting(key: String, value: String) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_KEY, key)
                put(COL_VALUE, value)
            }
            db.insertWithOnConflict(TABLE_SETTINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Log.d(TAG, "Saved setting in SQLite: $key -> $value")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving setting $key to SQLite", e)
        }
    }

    fun getSetting(key: String, defaultValue: String): String {
        return try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_SETTINGS,
                arrayOf(COL_VALUE),
                "$COL_KEY = ?",
                arrayOf(key),
                null, null, null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    it.getString(0) ?: defaultValue
                } else {
                    defaultValue
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading setting $key from SQLite", e)
            defaultValue
        }
    }

    // High level settings helpers
    fun loadThemeState(): ThemeState {
        val themeModeStr = getSetting(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        val isAmoledStr = getSetting(KEY_IS_AMOLED, "false")
        val paletteStr = getSetting(KEY_COLOR_PALETTE, AppColorPalette.TEAL.name)

        val themeMode = try {
            AppThemeMode.valueOf(themeModeStr)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }

        val isAmoled = isAmoledStr.toBoolean()

        val palette = try {
            AppColorPalette.valueOf(paletteStr)
        } catch (e: Exception) {
            AppColorPalette.TEAL
        }

        return ThemeState(
            themeMode = themeMode,
            isAmoled = isAmoled,
            palette = palette
        )
    }

    fun persistThemeMode(mode: AppThemeMode) {
        saveSetting(KEY_THEME_MODE, mode.name)
    }

    fun persistIsAmoled(isAmoled: Boolean) {
        saveSetting(KEY_IS_AMOLED, isAmoled.toString())
    }

    fun persistColorPalette(palette: AppColorPalette) {
        saveSetting(KEY_COLOR_PALETTE, palette.name)
    }
}
