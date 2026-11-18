package com.hddev.byahero.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

object PlaceSearchHelper {

    val hasValidMapsKey: Boolean
        get() = BuildConfig.MAPS_API_KEY.isNotBlank() &&
            !BuildConfig.MAPS_API_KEY.contains("Placeholder", ignoreCase = true)

    @SuppressLint("MissingPermission")
    fun getBestLastKnownDeviceLocation(context: Context): LatLng? {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
                val currentBest = bestLocation
                if (currentBest == null || loc.time > currentBest.time || loc.accuracy < currentBest.accuracy) {
                    bestLocation = loc
                }
            }
            bestLocation?.let { LatLng(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun detectApproximateNetworkLocation(): LatLng? = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://ipwho.is/",
            "https://ipapi.co/json/"
        )
        for (urlStr in endpoints) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "BYAHERO-Android/2026.1")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val obj = JSONObject(body)
                    val lat = obj.optDouble("latitude", Double.NaN)
                    val lng = obj.optDouble("longitude", Double.NaN)
                    if (!lat.isNaN() && !lng.isNaN() && lat != 0.0 && lng != 0.0) {
                        return@withContext LatLng(lat, lng)
                    }
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
        }
        null
    }

    suspend fun reverseGeocode(
        context: Context,
        point: LatLng
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        // 1. Photon Komoot 2026 high-precision POI & street reverse geocoding
        try {
            val photonUrl =
                "https://photon.komoot.io/reverse?lat=${point.latitude}&lon=${point.longitude}&lang=en"
            val conn = (URL(photonUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "BYAHERO-Android/2026.1")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val features = JSONObject(body).optJSONArray("features")
                if (features != null && features.length() > 0) {
                    val props = features.getJSONObject(0).optJSONObject("properties")
                    if (props != null) {
                        val name = props.optString("name").trim()
                        val street = props.optString("street").trim()
                        val district = props.optString("district").ifBlank { props.optString("locality") }.trim()
                        val city = props.optString("city").ifBlank { props.optString("county") }.trim()
                        val primary = name.ifBlank { street }.ifBlank { district }
                        val secondary = district.ifBlank { city }
                        val label = when {
                            primary.isNotBlank() && secondary.isNotBlank() && !primary.equals(secondary, ignoreCase = true) ->
                                "$primary, $secondary"
                            primary.isNotBlank() -> primary
                            else -> city
                        }
                        if (label.isNotBlank()) {
                            val fullParts = listOf(name, street, district, city).filter { it.isNotBlank() }.distinct()
                            conn.disconnect()
                            return@withContext label to fullParts.joinToString(", ").ifBlank { label }
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {
        }

        // 2. OpenStreetMap Nominatim 2026 reverse geocoding
        try {
            val endpoint =
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${point.latitude}&lon=${point.longitude}&zoom=18&addressdetails=1"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                setRequestProperty("User-Agent", "BYAHERO-Android/2026.1")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val obj = JSONObject(body)
                val addr = obj.optJSONObject("address")
                val venueName = obj.optString("name").ifBlank {
                    addr?.optString("amenity")
                        ?.ifBlank { addr.optString("shop") }
                        ?.ifBlank { addr.optString("tourism") }
                        ?.ifBlank { addr.optString("building") }
                        .orEmpty()
                }
                val road = addr?.optString("road").orEmpty()
                val suburb = addr?.optString("suburb")
                    ?.ifBlank { addr.optString("neighbourhood") }
                    ?.ifBlank { addr.optString("village") }
                    ?.ifBlank { addr.optString("town") }
                    ?.ifBlank { addr.optString("city") }
                    .orEmpty()

                val primary = venueName.ifBlank { road }.ifBlank { suburb }
                val label = when {
                    primary.isNotBlank() && suburb.isNotBlank() && !primary.equals(suburb, ignoreCase = true) ->
                        "$primary, $suburb"
                    primary.isNotBlank() -> primary
                    else -> obj.optString("display_name").substringBeforeLast(",").trim()
                }
                if (label.isNotBlank()) {
                    conn.disconnect()
                    return@withContext label to obj.optString("display_name", label)
                }
            }
            conn.disconnect()
        } catch (_: Exception) {
        }

        // 3. Android system Geocoder
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine<List<android.location.Address>> { cont ->
                        geocoder.getFromLocation(
                            point.latitude,
                            point.longitude,
                            1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<android.location.Address>) {
                                    if (cont.isActive) cont.resume(addresses)
                                }

                                override fun onError(errorMessage: String?) {
                                    if (cont.isActive) cont.resume(emptyList())
                                }
                            }
                        )
                    }
                } else {
                    geocoder.getFromLocation(point.latitude, point.longitude, 1)
                }
                val first = addresses?.firstOrNull()
                if (first != null) {
                    val feature = first.featureName?.takeIf { it.isNotBlank() && it.length > 2 && !it.all(Char::isDigit) }
                    val road = first.thoroughfare?.takeIf { it.isNotBlank() }
                    val area = first.subLocality?.takeIf { it.isNotBlank() } ?: first.locality?.takeIf { it.isNotBlank() }
                    val main = feature ?: road ?: area ?: "Current Location"
                    val label = if (area != null && !main.equals(area, ignoreCase = true)) "$main, $area" else main
                    val full = first.getAddressLine(0) ?: label
                    return@withContext label to full
                }
            }
        } catch (_: Exception) {
        }

        "Current Location" to String.format("%.5f, %.5f", point.latitude, point.longitude)
    }

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
        return searchViaMultiProvider2026(query, category, center)
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
            .setLocationBias(CircularBounds.newInstance(center, 25000.0))
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

    private suspend fun searchViaMultiProvider2026(
        query: String,
        category: String,
        center: LatLng
    ): List<NearbySpot> = withContext(Dispatchers.IO) {
        val aggregated = mutableListOf<NearbySpot>()

        // 1. Overpass 2026 POI discovery for Fast Food & Resorts/Beaches
        if (category == "fast_food" || category == "resort_beach") {
            try {
                val radiusMeters = if (category == "resort_beach") 35000 else 10000
                val overpassFilter = if (category == "fast_food") {
                    """[out:json][timeout:10];(node["amenity"="fast_food"](around:$radiusMeters,${center.latitude},${center.longitude});way["amenity"="fast_food"](around:$radiusMeters,${center.latitude},${center.longitude});node["name"~"Jollibee|McDonald|KFC|Chowking|Burger King|Mang Inasal|Greenwich",i](around:$radiusMeters,${center.latitude},${center.longitude}););out center 18;"""
                } else {
                    """[out:json][timeout:10];(node["tourism"~"resort|hotel"](around:$radiusMeters,${center.latitude},${center.longitude});way["tourism"="resort"](around:$radiusMeters,${center.latitude},${center.longitude});node["natural"="beach"](around:$radiusMeters,${center.latitude},${center.longitude});way["natural"="beach"](around:$radiusMeters,${center.latitude},${center.longitude}););out center 18;"""
                }
                val conn = (URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "BYAHERO-Android/2026.1")
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                }
                conn.outputStream.use {
                    it.write(("data=" + URLEncoder.encode(overpassFilter, "UTF-8")).toByteArray())
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val elements = JSONObject(body).optJSONArray("elements") ?: JSONArray()
                    for (i in 0 until elements.length()) {
                        val el = elements.getJSONObject(i)
                        val tags = el.optJSONObject("tags") ?: continue
                        val name = tags.optString("name")
                        if (name.isBlank()) continue
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
                                address = street.ifBlank { "Nearby location" },
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
            }
        }

        // 2. Photon Komoot 2026 location-biased search (fast, typo-tolerant POI & address search)
        val photonQueries = when (category) {
            "fast_food" -> if (aggregated.isEmpty()) listOf("Jollibee", "McDonald's") else emptyList()
            "resort_beach" -> if (aggregated.isEmpty()) listOf("beach resort") else emptyList()
            else -> listOf(query)
        }
        for (q in photonQueries) {
            if (q.isBlank()) continue
            try {
                val encoded = URLEncoder.encode(q, "UTF-8")
                val photonEndpoint =
                    "https://photon.komoot.io/api/?q=$encoded&lat=${center.latitude}&lon=${center.longitude}&limit=10&lang=en"
                val conn = (URL(photonEndpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4500
                    readTimeout = 4500
                    setRequestProperty("User-Agent", "BYAHERO-Android/2026.1")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val features = JSONObject(body).optJSONArray("features") ?: JSONArray()
                    for (i in 0 until features.length()) {
                        val feat = features.getJSONObject(i)
                        val geom = feat.optJSONObject("geometry") ?: continue
                        val coords = geom.optJSONArray("coordinates") ?: continue
                        if (coords.length() < 2) continue
                        val lng = coords.optDouble(0, Double.NaN)
                        val lat = coords.optDouble(1, Double.NaN)
                        if (lat.isNaN() || lng.isNaN()) continue
                        val props = feat.optJSONObject("properties") ?: continue
                        val name = props.optString("name").ifBlank {
                            props.optString("street").ifBlank { props.optString("district", q) }
                        }
                        val addrParts = listOf(
                            props.optString("street"),
                            props.optString("district"),
                            props.optString("city"),
                            props.optString("state")
                        ).filter { it.isNotBlank() }.distinct()
                        val dist = ByaheroNativeEngine.calculateHaversineKm(center.latitude, center.longitude, lat, lng)
                        aggregated.add(
                            NearbySpot(
                                id = "ph_${props.optLong("osm_id", i.toLong())}_$i",
                                name = name,
                                address = addrParts.joinToString(", ").ifBlank { name },
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
            }
        }

        if (aggregated.isNotEmpty()) {
            return@withContext aggregated
                .distinctBy { it.name.lowercase() + "_" + String.format("%.3f", it.lat) }
                .sortedBy { it.distanceKm }
                .take(12)
        }

        // 3. Nominatim place search fallback
        val searchQueries = when (category) {
            "fast_food" -> listOf("Jollibee", "McDonald's", "KFC")
            "resort_beach" -> listOf("resort", "beach")
            else -> listOf(query)
        }

        val delta = if (category == "resort_beach") 0.45 else 0.25
        val viewbox = "${center.longitude - delta},${center.latitude + delta},${center.longitude + delta},${center.latitude - delta}"

        for (q in searchQueries) {
            if (q.isBlank()) continue
            try {
                val encoded = URLEncoder.encode(q, "UTF-8")
                val endpoint =
                    "https://nominatim.openstreetmap.org/search?format=jsonv2&q=$encoded&limit=8&viewbox=$viewbox&bounded=0"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "BYAHERO-Android/2026.1")
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
            }
        }

        aggregated.distinctBy { it.name.lowercase() + "_" + String.format("%.3f", it.lat) }
            .sortedBy { it.distanceKm }
            .take(12)
    }
}
