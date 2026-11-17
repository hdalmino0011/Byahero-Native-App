package com.hddev.byahero.data

import android.content.Context
import com.hddev.byahero.nativeengine.ByaheroNativeEngine
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OrderHistoryStore {
    private const val PREFS_NAME = "byahero_order_history"
    private const val KEY_ORDERS = "orders_json_v1"

    fun currentTimeLabel(): String {
        return SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date())
    }

    fun defaultSeeds(passengerName: String): List<ActiveRide> {
        return listOf(
            ActiveRide(
                id = "hist_001",
                passengerName = passengerName,
                vehicleType = VehicleType.MOTORCYCLE,
                pickupLabel = "IT Park, Lahug",
                pickupLat = 10.3304,
                pickupLng = 123.9056,
                destinationLabel = "SM City Cebu",
                destLat = 10.3119,
                destLng = 123.9182,
                distanceKm = 3.8,
                durationMins = 12,
                fare = ByaheroNativeEngine.computeFareQuote(3.8, VehicleType.MOTORCYCLE, false),
                status = RideStatus.COMPLETED,
                riderName = "Kuya Carlo D.",
                riderPlate = "MTO-8421",
                createdAtLabel = "Yesterday, 4:20 PM",
                roleContext = "passenger"
            ),
            ActiveRide(
                id = "hist_002",
                passengerName = passengerName,
                vehicleType = VehicleType.TAXI,
                pickupLabel = "Ayala Center Cebu",
                pickupLat = 10.3181,
                pickupLng = 123.9049,
                destinationLabel = "Mactan Shrine Resort Strip",
                destLat = 10.3106,
                destLng = 124.0151,
                distanceKm = 13.4,
                durationMins = 32,
                fare = ByaheroNativeEngine.computeFareQuote(13.4, VehicleType.TAXI, true),
                status = RideStatus.COMPLETED,
                riderName = "Kuya Ramon V.",
                riderPlate = "TXI-3902",
                createdAtLabel = "Sat, 11:05 AM",
                roleContext = "passenger"
            )
        )
    }

    fun load(context: Context, passengerName: String): List<ActiveRide> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_ORDERS, null) ?: return defaultSeeds(passengerName)
            val arr = JSONArray(raw)
            val list = mutableListOf<ActiveRide>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val vType = if (obj.optString("vehicleType") == VehicleType.TAXI.name) {
                    VehicleType.TAXI
                } else {
                    VehicleType.MOTORCYCLE
                }
                val rStatus = when (obj.optString("status")) {
                    RideStatus.CANCELLED.name -> RideStatus.CANCELLED
                    else -> RideStatus.COMPLETED
                }
                val fareObj = obj.optJSONObject("fare") ?: JSONObject()
                val fare = FareQuote(
                    baseFare = fareObj.optDouble("baseFare", 40.0),
                    distanceFare = fareObj.optDouble("distanceFare", 20.0),
                    subtotal = fareObj.optDouble("subtotal", 60.0),
                    passengerDiscount = fareObj.optDouble("passengerDiscount", 0.0),
                    finalFare = fareObj.optDouble("finalFare", 60.0),
                    riderIncentive = fareObj.optDouble("riderIncentive", 3.0),
                    isSaturdayPromo = fareObj.optBoolean("isSaturdayPromo", false)
                )
                list.add(
                    ActiveRide(
                        id = obj.optString("id"),
                        passengerName = obj.optString("passengerName", passengerName),
                        vehicleType = vType,
                        pickupLabel = obj.optString("pickupLabel"),
                        pickupLat = obj.optDouble("pickupLat"),
                        pickupLng = obj.optDouble("pickupLng"),
                        destinationLabel = obj.optString("destinationLabel"),
                        destLat = obj.optDouble("destLat"),
                        destLng = obj.optDouble("destLng"),
                        distanceKm = obj.optDouble("distanceKm", 1.0),
                        durationMins = obj.optInt("durationMins", 5),
                        fare = fare,
                        status = rStatus,
                        riderName = obj.optString("riderName").ifBlank { null },
                        riderPlate = obj.optString("riderPlate").ifBlank { null },
                        createdAtLabel = obj.optString("createdAtLabel", "Recent trip"),
                        roleContext = obj.optString("roleContext", "passenger")
                    )
                )
            }
            if (list.isEmpty()) defaultSeeds(passengerName) else list
        } catch (_: Exception) {
            defaultSeeds(passengerName)
        }
    }

    fun save(context: Context, orders: List<ActiveRide>) {
        try {
            val arr = JSONArray()
            for (o in orders) {
                val fareObj = JSONObject().apply {
                    put("baseFare", o.fare.baseFare)
                    put("distanceFare", o.fare.distanceFare)
                    put("subtotal", o.fare.subtotal)
                    put("passengerDiscount", o.fare.passengerDiscount)
                    put("finalFare", o.fare.finalFare)
                    put("riderIncentive", o.fare.riderIncentive)
                    put("isSaturdayPromo", o.fare.isSaturdayPromo)
                }
                val obj = JSONObject().apply {
                    put("id", o.id)
                    put("passengerName", o.passengerName)
                    put("vehicleType", o.vehicleType.name)
                    put("pickupLabel", o.pickupLabel)
                    put("pickupLat", o.pickupLat)
                    put("pickupLng", o.pickupLng)
                    put("destinationLabel", o.destinationLabel)
                    put("destLat", o.destLat)
                    put("destLng", o.destLng)
                    put("distanceKm", o.distanceKm)
                    put("durationMins", o.durationMins)
                    put("fare", fareObj)
                    put("status", o.status.name)
                    put("riderName", o.riderName.orEmpty())
                    put("riderPlate", o.riderPlate.orEmpty())
                    put("createdAtLabel", o.createdAtLabel)
                    put("roleContext", o.roleContext)
                }
                arr.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ORDERS, arr.toString())
                .apply()
        } catch (_: Exception) {
            // Ignore preference serialization errors
        }
    }
}
