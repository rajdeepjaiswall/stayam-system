package `in`.getdownfoundation.sahusales.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Config.PREFS_NAME)

object SessionKeys {
    val TOKEN = stringPreferencesKey("token")
    val USER_JSON = stringPreferencesKey("user_json")
    val REMINDERS_JSON = stringPreferencesKey("reminders_json")
    val LAST_SYNC = stringPreferencesKey("last_sync")
}

class SessionStore(private val context: Context) {
    val token: Flow<String?> = context.dataStore.data.map { it[SessionKeys.TOKEN] }
    val userJson: Flow<String?> = context.dataStore.data.map { it[SessionKeys.USER_JSON] }
    val remindersJson: Flow<String?> = context.dataStore.data.map { it[SessionKeys.REMINDERS_JSON] }
    val lastSync: Flow<String?> = context.dataStore.data.map { it[SessionKeys.LAST_SYNC] }

    suspend fun saveSession(token: String, user: User) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.TOKEN] = token
            prefs[SessionKeys.USER_JSON] = Json.encodeToString(user)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(SessionKeys.TOKEN)
            prefs.remove(SessionKeys.USER_JSON)
        }
    }

    suspend fun getToken(): String? = token.first()

    suspend fun getUser(): User? {
        val json = userJson.first() ?: return null
        return try { Json.decodeFromString<User>(json) } catch (e: Exception) { null }
    }

    suspend fun saveReminders(reminders: List<ReminderFeedItem>) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.REMINDERS_JSON] = Json.encodeToString(reminders)
            prefs[SessionKeys.LAST_SYNC] = System.currentTimeMillis().toString()
        }
    }

    suspend fun getCachedReminders(): List<ReminderFeedItem> {
        val json = remindersJson.first() ?: return emptyList()
        return try { Json.decodeFromString<List<ReminderFeedItem>>(json) } catch (e: Exception) { emptyList() }
    }

    suspend fun getLastSync(): Long? = lastSync.first()?.toLongOrNull()
}
