package com.mathcore.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mathcore_prefs")

class PreferencesManager(private val context: Context) {
    companion object {
        val XP_KEY = intPreferencesKey("xp")
        val STREAK_KEY = intPreferencesKey("streak")
        val LAST_ACTIVE_KEY = stringPreferencesKey("last_active")
        val DAILY_DATE_KEY = stringPreferencesKey("daily_date")
        val DAILY_SCORE_KEY = intPreferencesKey("daily_score")
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        val SOUND_KEY = booleanPreferencesKey("sound_enabled")
    }

    val xp: Flow<Int> = context.dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[XP_KEY] ?: 0 }

    val streak: Flow<Int> = context.dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[STREAK_KEY] ?: 0 }

    val darkTheme: Flow<Boolean> = context.dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[DARK_THEME_KEY] ?: false }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[SOUND_KEY] ?: true }

    val dailyCompleted: Flow<Boolean> = context.dataStore.data.catch { emit(emptyPreferences()) }
        .map { prefs ->
            val savedDate = prefs[DAILY_DATE_KEY] ?: ""
            savedDate == LocalDate.now().toString()
        }

    val dailyScore: Flow<Int> = context.dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[DAILY_SCORE_KEY] ?: 0 }

    suspend fun addXP(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[XP_KEY] ?: 0
            prefs[XP_KEY] = current + amount
            // Update streak
            val lastActive = prefs[LAST_ACTIVE_KEY] ?: ""
            val today = LocalDate.now().toString()
            val yesterday = LocalDate.now().minusDays(1).toString()
            when {
                lastActive == today -> { /* already active today */ }
                lastActive == yesterday -> {
                    prefs[STREAK_KEY] = (prefs[STREAK_KEY] ?: 0) + 1
                    prefs[LAST_ACTIVE_KEY] = today
                }
                else -> {
                    prefs[STREAK_KEY] = 1
                    prefs[LAST_ACTIVE_KEY] = today
                }
            }
        }
    }

    /** Устанавливает XP напрямую (для синхронизации с историей из БД).
     *  Обновляет только если dbTotal > локального, чтобы не затирать свежезаработанное. */
    suspend fun syncXp(dbTotal: Int) {
        context.dataStore.edit { prefs ->
            val local = prefs[XP_KEY] ?: 0
            if (dbTotal > local) prefs[XP_KEY] = dbTotal
        }
    }

    suspend fun setDailyCompleted(score: Int) {
        context.dataStore.edit { prefs ->
            prefs[DAILY_DATE_KEY] = LocalDate.now().toString()
            prefs[DAILY_SCORE_KEY] = score
        }
    }

    /** Сбрасывает статус ежедневного вызова.
     *  Вызывается при входе нового пользователя, у которого нет daily-результата за сегодня,
     *  чтобы не показывался чужой/устаревший статус «Выполнено». */
    suspend fun resetDailyCompleted() {
        context.dataStore.edit { prefs ->
            prefs.remove(DAILY_DATE_KEY)
            prefs[DAILY_SCORE_KEY] = 0
        }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[DARK_THEME_KEY] = dark }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SOUND_KEY] = enabled }
    }
}
