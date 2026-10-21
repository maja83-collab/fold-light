package com.thelightphone.fold

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object TripPreferences {
    val TRIPS_JSON = stringPreferencesKey("trips_json")
}

internal class TripRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): TripStore {
        val raw = dataStore.data.first()[TripPreferences.TRIPS_JSON] ?: return TripStore()
        return runCatching { json.decodeFromString<TripStore>(raw) }.getOrDefault(TripStore())
    }

    suspend fun save(store: TripStore) {
        dataStore.edit { prefs ->
            prefs[TripPreferences.TRIPS_JSON] = json.encodeToString(store)
        }
    }
}
