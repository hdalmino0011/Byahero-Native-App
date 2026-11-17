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
    private const val KEY_ORDERS = "orders_json_v2"
    private const val KEY_PAYMENTS = "payment_methods_json_v1"
    private const val KEY_FAVORITES = "favorite_destinations_json_v1"
    private const val KEY_PROMO = "applied_promo_code_v1"

    fun currentTimeLabel(): String {
        return SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date())
    }

    fun defaultPromoCodes(): List<PromoCodeItem> {
        return listOf(
            PromoCodeItem(
                code = "BYAHERO10",
                title = "10% Off Any Ride",
                description = "Save 10% (up to P50) on any BYAHERO Moto or Cab trip.",
                discountPercent = 10,
                maxDiscountPhp = 50.0
            ),
            PromoCodeItem(
                code = "SATPROMO",
                title = "Saturday 10% Off + 5% Rider Bonus",
                description = "Weekend special: 10% passenger discount while riders earn +5% extra.",
                discountPercent = 10,
                maxDiscountPhp = 80.0
            ),
            PromoCodeItem(
                code = "FIRST50",
                title = "15% Welcome Ride Saver",
                description = "Enjoy 15% off (up to P50) on your booked destination.",
                discountPercent = 15,
                maxDiscountPhp = 50.0
            )
        )
    }

    fun defaultPaymentMethods(): List<SavedPaymentMethod> {
        return listOf(
            SavedPaymentMethod(
                id = "pm_cash",
                type = PaymentMethodType.CASH,
                label = "Cash",
                subtitle = "Pay rider directly upon arrival",
                isDefault = true
            )
        )
    }

    fun defaultFavorites(): List<FavoriteDestination> {
        return listOf(
            FavoriteDestination(
                id = "fav_1",
                label = "SM City Cebu",
                address = "North Reclamation Area, Cebu City",
                lat = 10.3119,
                lng = 123.9182,
                ordersCount = 8
            ),
            FavoriteDestination(
                id = "fav_2",
                label = "IT Park, Lahug",
                address = "Cebu IT Park, Apas, Cebu City",
                lat = 10.3304,
                lng = 123.9056,
                ordersCount = 6
            ),
            FavoriteDestination(
                id = "fav_3",
                label = "Ayala Center Cebu",
                address = "Cebu Business Park, Cebu City",
                lat = 10.3181,
                lng = 123.9049,
                ordersCount = 4
            )
        )
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
                roleContext = "passenger",
                paymentMethodLabel = "Cash"
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
                roleContext = "passenger",
                paymentMethodLabel = "GCash"
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
                        roleContext = obj.optString("roleContext", "passenger"),
                        paymentMethodLabel = obj.optString("paymentMethodLabel", "Cash").ifBlank { "Cash" },
                        promoCode = obj.optString("promoCode").ifBlank { null }
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
                    put("paymentMethodLabel", o.paymentMethodLabel)
                    put("promoCode", o.promoCode.orEmpty())
                }
                arr.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ORDERS, arr.toString())
                .apply()
        } catch (_: Exception) {
        }
    }

    fun loadPaymentMethods(context: Context): List<SavedPaymentMethod> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_PAYMENTS, null) ?: return defaultPaymentMethods()
            val arr = JSONArray(raw)
            val list = mutableListOf<SavedPaymentMethod>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val type = when (obj.optString("type")) {
                    PaymentMethodType.GCASH.name -> PaymentMethodType.GCASH
                    PaymentMethodType.CARD.name -> PaymentMethodType.CARD
                    else -> PaymentMethodType.CASH
                }
                list.add(
                    SavedPaymentMethod(
                        id = obj.optString("id"),
                        type = type,
                        label = obj.optString("label"),
                        subtitle = obj.optString("subtitle"),
                        accountNumberOrLast4 = obj.optString("accountNumberOrLast4"),
                        holderName = obj.optString("holderName"),
                        expiry = obj.optString("expiry"),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }
            if (list.isEmpty()) defaultPaymentMethods() else list
        } catch (_: Exception) {
            defaultPaymentMethods()
        }
    }

    fun savePaymentMethods(context: Context, methods: List<SavedPaymentMethod>) {
        try {
            val arr = JSONArray()
            for (m in methods) {
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("type", m.type.name)
                    put("label", m.label)
                    put("subtitle", m.subtitle)
                    put("accountNumberOrLast4", m.accountNumberOrLast4)
                    put("holderName", m.holderName)
                    put("expiry", m.expiry)
                    put("isDefault", m.isDefault)
                }
                arr.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PAYMENTS, arr.toString())
                .apply()
        } catch (_: Exception) {
        }
    }

    fun loadFavorites(context: Context): List<FavoriteDestination> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_FAVORITES, null) ?: return defaultFavorites()
            val arr = JSONArray(raw)
            val list = mutableListOf<FavoriteDestination>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FavoriteDestination(
                        id = obj.optString("id"),
                        label = obj.optString("label"),
                        address = obj.optString("address"),
                        lat = obj.optDouble("lat"),
                        lng = obj.optDouble("lng"),
                        ordersCount = obj.optInt("ordersCount", 1)
                    )
                )
            }
            list
        } catch (_: Exception) {
            defaultFavorites()
        }
    }

    fun saveFavorites(context: Context, favorites: List<FavoriteDestination>) {
        try {
            val arr = JSONArray()
            for (f in favorites) {
                val obj = JSONObject().apply {
                    put("id", f.id)
                    put("label", f.label)
                    put("address", f.address)
                    put("lat", f.lat)
                    put("lng", f.lng)
                    put("ordersCount", f.ordersCount)
                }
                arr.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FAVORITES, arr.toString())
                .apply()
        } catch (_: Exception) {
        }
    }

    fun loadAppliedPromo(context: Context): String? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_PROMO, null)?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    fun saveAppliedPromo(context: Context, code: String?) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PROMO, code.orEmpty())
                .apply()
        } catch (_: Exception) {
        }
    }
}
