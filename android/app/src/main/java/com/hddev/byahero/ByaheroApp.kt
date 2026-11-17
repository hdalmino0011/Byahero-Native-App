package com.hddev.byahero

import android.app.Application
import com.google.android.gms.maps.MapsApiSettings
import com.google.android.gms.maps.MapsInitializer
import com.google.android.libraries.places.api.Places

class ByaheroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val apiKey = BuildConfig.MAPS_API_KEY
        val hasValidKey = apiKey.isNotBlank() && !apiKey.contains("Placeholder", ignoreCase = true)

        if (hasValidKey) {
            MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, null)
            MapsApiSettings.addInternalUsageAttributionId(
                applicationContext,
                "gmp_mcp_codeassist_v1_aistudio"
            )
            if (!Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
            }
        }
    }
}
