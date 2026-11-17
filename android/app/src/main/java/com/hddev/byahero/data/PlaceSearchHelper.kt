package com.hddev.byahero.data

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.hddev.byahero.BuildConfig
import com.hddev.byahero.nativeengine.ByaheroNativeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume

object PlaceSearchHelper {

    val hasValidMapsKey: Boolean
        get() = BuildConfig.MAPS_API_KEY.isNotBlank() &&
            !BuildConfig.MAPS_API_KEY.contains("Placeholder", ignoreCase = true)

    suspend fun searchPlaces(
        context: Context,
        query: String,
        category: String,
        center: LatLng
    ): List<NearbySpot> {
        if (hasValidMapsKey && Places.isInitialized()) {
            val googleResults = searchViaPlacesSdk(context, query, category, center)
            if (googleResults.isNotEmpty()) return googleResults
        }
        return searchViaNominatim(query, category, center)
    }

    private suspend fun searchViaPlacesSdk(
        context: Context,
        query: String,
        category: String,
        center: LatLng
    ): List<NearbySpot> = suspendCancellableCoroutine { cont ->
        val placesClient = Places.createClient(context)
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.RATING
        )
        val request = SearchByTextRequest.builder(query, fields)
            .setLocationBias(CircularBounds.newInstance(center, 20000.0))
            .setMaxResultCount(10)
            .build()

        placesClient.searchByText(request)
            .addOnSuccessListener { response ->
                val list = response.places.mapNotNull { place ->
                    val loc = place.location ?: return@mapNotNull null
                    val dist = ByaheroNativeEngine.calculateHaversineKm(
                        center.latitude,
                        center.longitude,
                        loc.latitude,
                        loc.longitude
                    )
                    NearbySpot(
                        id = place.id ?: place.displayName.orEmpty(),
                        name = place.displayName ?: "Location",
                        address = place.formattedAddress ?: "",
                        lat = loc.latitude,
                        lng = loc.longitude,
                        rating = place.rating,
                        category = category,
                        distanceKm = dist
                    )
                }.sortedBy { it.distanceKm }
                if (cont.isActive) cont.resume(list)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(emptyList())
            }
    }

    private suspend fun searchViaNominatim(
        query: String,
        category: String,
        center: LatLng
    ): List<NearbySpot> = withContext(Dispatchers.IO) {
        val searchQueries = when (category) {
            "fast_food" -> listOf("Jollibee", "McDonald's", "KFC")
            "resort_beach" -> listOf("resort", "beach")
            else -> listOf(query)
        }

        val aggregated = mutableListOf<NearbySpot>()
        val delta = if (category == "resort_beach") 0.45 else 0.15
        val viewbox = "${center.longitude - delta},${center.latitude + delta},${center.longitude + delta},${center.latitude - delta}"

        for (q in searchQueries) {
            try {
                val encoded = URLEncoder.encode(q, "UTF-8")
                val endpoint =
                    "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=5&viewbox=$viewbox&bounded=0"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "BYAHERO-Android/1.0")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                        val lng = obj.optString("lon").toDoubleOrNull() ?: continue
                        val display = obj.optString("display_name", q)
                        val shortName = obj.optString("name").ifBlank {
                            display.substringBefore(",").ifBlank { q }
                        }
                        val dist = ByaheroNativeEngine.calculateHaversineKm(
                            center.latitude,
                            center.longitude,
                            lat,
                            lng
                        )
                        aggregated.add(
                            NearbySpot(
                                id = obj.optString("place_id", "${lat}_${lng}_$i"),
                                name = shortName,
                                address = display,
                                lat = lat,
                                lng = lng,
                                rating = null,
                                category = category,
                                distanceKm = dist
                            )
                        )
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
                // ignore single query timeout and try next
            }
        }

        aggregated.distinctBy { it.name + "_" + String.format("%.3f", it.lat) }
            .sortedBy { it.distanceKm }
            .take(10)
    }
}
