package com.hddev.byahero.data

import kotlinx.serialization.Serializable

enum class UserRole {
    PASSENGER,
    RIDER,
    BOTH
}

enum class ActiveMode {
    PASSENGER,
    RIDER
}

enum class VehicleType {
    MOTORCYCLE,
    TAXI
}

enum class RideStatus {
    SEARCHING,
    ACCEPTED,
    ARRIVING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class PaymentMethodType {
    CASH,
    GCASH,
    CARD
}

data class SavedPaymentMethod(
    val id: String,
    val type: PaymentMethodType,
    val label: String,
    val subtitle: String,
    val accountNumberOrLast4: String = "",
    val holderName: String = "",
    val expiry: String = "",
    val isDefault: Boolean = false
)

data class FavoriteDestination(
    val id: String,
    val label: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val ordersCount: Int = 1
)

data class PromoCodeItem(
    val code: String,
    val title: String,
    val description: String,
    val discountPercent: Int,
    val maxDiscountPhp: Double
)

data class SupportTicketItem(
    val id: String,
    val category: String,
    val message: String,
    val status: String,
    val createdAt: String
)

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val authProvider: String = "member",
    val role: UserRole = UserRole.BOTH,
    val activeMode: ActiveMode = ActiveMode.PASSENGER,
    val vehicleType: VehicleType = VehicleType.MOTORCYCLE,
    val plateNumber: String = "BYH-2026",
    val isOnline: Boolean = true,
    val totalRides: Int = 14,
    val riderEarningsPhp: Double = 3840.0,
    val riderIncentivesPhp: Double = 192.0
)

data class FareQuote(
    val baseFare: Double,
    val distanceFare: Double,
    val subtotal: Double,
    val passengerDiscount: Double,
    val finalFare: Double,
    val riderIncentive: Double,
    val isSaturdayPromo: Boolean
)

data class NearbySpot(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val rating: Double?,
    val category: String,
    val distanceKm: Double
)

data class ActiveRide(
    val id: String,
    val passengerName: String,
    val vehicleType: VehicleType,
    val pickupLabel: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val destinationLabel: String,
    val destLat: Double,
    val destLng: Double,
    val distanceKm: Double,
    val durationMins: Int,
    val fare: FareQuote,
    val status: RideStatus,
    val riderName: String? = null,
    val riderPlate: String? = null,
    val createdAtLabel: String = "Just now",
    val roleContext: String = "passenger",
    val paymentMethodLabel: String = "Cash",
    val promoCode: String? = null
)

@Serializable
data class RideRecord(
    val id: String,
    val passenger_name: String,
    val vehicle_type: String,
    val pickup_address: String,
    val pickup_lat: Double,
    val pickup_lng: Double,
    val destination_address: String,
    val destination_lat: Double,
    val destination_lng: Double,
    val distance_km: Double,
    val final_fare: Double,
    val rider_incentive: Double,
    val is_saturday_promo: Boolean,
    val status: String
)
