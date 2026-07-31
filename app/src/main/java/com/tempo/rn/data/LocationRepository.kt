package com.tempo.rn.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tempo.rn.core.model.AppLocation
import com.tempo.rn.core.model.Locations
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "tempo_settings")

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyLocation = stringPreferencesKey("selected_location")

    val selectedLocation: Flow<AppLocation> =
        context.dataStore.data.map { prefs -> Locations.byId(prefs[keyLocation]) }

    suspend fun select(location: AppLocation) {
        context.dataStore.edit { it[keyLocation] = location.id }
    }
}
