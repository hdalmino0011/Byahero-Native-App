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
        val aggregated = mutableListOf<NearbySpot>()

        // 1. Try Overpass API first for accurate nearby POIs around real phone GPS
        try {
            val radiusMeters = if (category == "resort_beach") 35000 else 8000
            val overpassFilter = when (category) {
                "fast_food" ->
                    """[out:json][timeout:10];(node["amenity"="fast_food"](around:$radiusMeters,${center.latitude},${center.longitude});way["amenity"="fast_food"](around:$radiusMeters,${center.latitude},${center.longitude});node["name"~"Jollibee|McDonald|KFC|Chowking|Burger King",i](around:$radiusMeters,${center.latitude},${center.longitude}););out center 15;"""
                "resort_beach" ->
                    """[out:json][timeout:10];(node["tourism"~"resort|hotel"](around:$radiusMeters,${center.latitude},${center.longitude});way["tourism"="resort"](around:$radiusMeters,${center.latitude},${center.longitude});node["natural"="beach"](around:$radiusMeters,${center.latitude},${center.longitude});way["natural"="beach"](around:$radiusMeters,${center.latitude},${center.longitude}););out center 15;"""
                else -> ""
            }
            if (overpassFilter.isNotEmpty()) {
                val conn = (URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "BYAHERO-Android/1.0")
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                }
                conn.outputStream.use {
                    it.write(("data=" + URLEncoder.encode(overpassFilter, "UTF-8")).toByteArray())
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val elements = org.json.JSONObject(body).optJSONArray("elements") ?: JSONArray()
                    for (i in 0 until elements.length()) {
                        val el = elements.getJSONObject(i)
                        val tags = el.optJSONObject("tags") ?: continue
                        val name = tags.optString("name").ifBlank { continue }
                        val cObj = el.optJSONObject("center")
                        val lat = el.optDouble("lat", Double.NaN).let { if (it.isNaN()) cObj?.optDouble("lat", Double.NaN) ?: Double.NaN else it }
                        val lng = el.optDouble("lon", Double.NaN).let { if (it.isNaN()) cObj?.optDouble("lon", Double.NaN) ?: Double.NaN else it }
                        if (lat.isNaN() || lng.isNaN()) continue
                        val street = tags.optString("addr:street").ifBlank { tags.optString("addr:city", "") }
                        val dist = ByaheroNativeEngine.calculateHaversineKm(center.latitude, center.longitude, lat, lng)
                        aggregated.add(
                            NearbySpot(
                                id = "ov_${el.optLong("id", i.toLong())}",
                                name = name,
                                address = street.ifBlank { "Nearby ${category.replace('_', ' ')}" },
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
            }
        } catch (_: Exception) {
            // Fall through to Nominatim
        }

        if (aggregated.isNotEmpty()) {
            return@withContext aggregated
                .distinctBy { it.name + "_" + String.format("%.3f", it.lat) }
                .sortedBy { it.distanceKm }
                .take(12)
        }

        // 2. Fallback to Nominatim bounded search
        val searchQueries = when (category) {
            "fast_food" -> listOf("Jollibee", "McDonald's", "KFC")
            "resort_beach" -> listOf("resort", "beach")
            else -> listOf(query)
        }

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
