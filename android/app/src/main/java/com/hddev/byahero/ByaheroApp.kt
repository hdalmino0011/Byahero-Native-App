package com.hddev.byahero

import android.app.Application
import com.google.android.gms.maps.MapsApiSettings
import com.google.android.gms.maps.MapsInitializer
import com.google.android.libraries.places.api.Places

class ByaheroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, null)
        MapsApiSettings.addInternalUsageAttributionId(
            applicationContext,
            "gmp_mcp_codeassist_v1_aistudio"
        )
        if (!Places.isInitialized() && BuildConfig.MAPS_API_KEY.isNotBlank()) {
            Places.initializeWithNewPlacesApiEnabled(
                applicationContext,
                BuildConfig.MAPS_API_KEY
            )
        }
    }
}
