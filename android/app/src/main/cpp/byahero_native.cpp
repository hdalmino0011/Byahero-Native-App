#include <jni.h>
#include <cmath>
#include <vector>
#include <string>
#include <algorithm>

namespace {
constexpr double kEarthRadiusKm = 6371.0;
constexpr double kPi = 3.14159265358979323846;

inline double toRadians(double deg) {
    return deg * kPi / 180.0;
}
} // namespace

extern "C" {

JNIEXPORT jdouble JNICALL
Java_com_hddev_byahero_nativeengine_ByaheroNativeEngine_calculateHaversineKm(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jdouble lat1,
    jdouble lng1,
    jdouble lat2,
    jdouble lng2
) {
    const double dLat = toRadians(lat2 - lat1);
    const double dLng = toRadians(lng2 - lng1);
    const double sinLat = std::sin(dLat * 0.5);
    const double sinLng = std::sin(dLng * 0.5);
    const double a = sinLat * sinLat +
        std::cos(toRadians(lat1)) * std::cos(toRadians(lat2)) * sinLng * sinLng;
    const double clamped = std::min(1.0, std::sqrt(a));
    return 2.0 * kEarthRadiusKm * std::asin(clamped);
}

/**
 * Returns double[6]:
 * [0] baseFare
 * [1] distanceFare
 * [2] subtotal
 * [3] passengerDiscount (10% on Saturday)
 * [4] finalFare
 * [5] riderIncentive (+5% extra incentive on every completed ride)
 */
JNIEXPORT jdoubleArray JNICALL
Java_com_hddev_byahero_nativeengine_ByaheroNativeEngine_calculateFareNative(
    JNIEnv* env,
    jobject /* thiz */,
    jdouble distanceKm,
    jint vehicleTypeCode, // 0 = Motorcycle, 1 = Taxi
    jboolean isSaturday
) {
    const double safeDistance = std::max(0.5, distanceKm);
    const double baseFare = (vehicleTypeCode == 0) ? 40.0 : 60.0;
    const double perKmRate = (vehicleTypeCode == 0) ? 10.0 : 15.0;
    const double extraKm = std::max(0.0, safeDistance - 2.0);
    const double distanceFare = std::round(extraKm * perKmRate);
    const double subtotal = baseFare + distanceFare;
    const double passengerDiscount = isSaturday ? std::round(subtotal * 0.10) : 0.0;
    const double finalFare = std::max(baseFare * (isSaturday ? 0.90 : 1.0), subtotal - passengerDiscount);
    const double riderIncentive = std::round(subtotal * 0.05 * 100.0) / 100.0;

    jdoubleArray out = env->NewDoubleArray(6);
    if (out != nullptr) {
        const jdouble values[6] = {
            baseFare,
            distanceFare,
            subtotal,
            passengerDiscount,
            std::round(finalFare),
            riderIncentive
        };
        env->SetDoubleArrayRegion(out, 0, 6, values);
    }
    return out;
}

/**
 * High-speed C++ Google Encoded Polyline Decoder returning flat [lat0, lng0, lat1, lng1, ...]
 */
JNIEXPORT jdoubleArray JNICALL
Java_com_hddev_byahero_nativeengine_ByaheroNativeEngine_decodePolylineNative(
    JNIEnv* env,
    jobject /* thiz */,
    jstring encoded
) {
    if (encoded == nullptr) {
        return env->NewDoubleArray(0);
    }
    const char* raw = env->GetStringUTFChars(encoded, nullptr);
    const std::string poly(raw ? raw : "");
    if (raw != nullptr) {
        env->ReleaseStringUTFChars(encoded, raw);
    }

    std::vector<double> coords;
    coords.reserve(poly.size());

    size_t index = 0;
    long lat = 0;
    long lng = 0;

    while (index < poly.size()) {
        int b = 0;
        int shift = 0;
        long result = 0;
        do {
            if (index >= poly.size()) break;
            b = static_cast<int>(poly[index++]) - 63;
            result |= static_cast<long>(b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        const long dlat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
        lat += dlat;

        shift = 0;
        result = 0;
        do {
            if (index >= poly.size()) break;
            b = static_cast<int>(poly[index++]) - 63;
            result |= static_cast<long>(b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        const long dlng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
        lng += dlng;

        coords.push_back(static_cast<double>(lat) / 1e5);
        coords.push_back(static_cast<double>(lng) / 1e5);
    }

    jdoubleArray out = env->NewDoubleArray(static_cast<jsize>(coords.size()));
    if (out != nullptr && !coords.empty()) {
        env->SetDoubleArrayRegion(out, 0, static_cast<jsize>(coords.size()), coords.data());
    }
    return out;
}

} // extern "C"
