package com.hddev.byahero.nativeengine

import com.hddev.byahero.data.FareQuote
import com.hddev.byahero.data.VehicleType

object ByaheroNativeEngine {
    init {
        System.loadLibrary("byahero_native")
    }

    external fun calculateHaversineKm(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double

    external fun calculateFareNative(
        distanceKm: Double,
        vehicleTypeCode: Int,
        isSaturday: Boolean
    ): DoubleArray

    external fun decodePolylineNative(encoded: String): DoubleArray

    fun computeFareQuote(
        distanceKm: Double,
        vehicleType: VehicleType,
        isSaturday: Boolean
    ): FareQuote {
        val code = if (vehicleType == VehicleType.MOTORCYCLE) 0 else 1
        val result = calculateFareNative(distanceKm, code, isSaturday)
        return FareQuote(
            baseFare = result[0],
            distanceFare = result[1],
            subtotal = result[2],
            passengerDiscount = result[3],
            finalFare = result[4],
            riderIncentive = result[5],
            isSaturdayPromo = isSaturday
        )
    }
}
