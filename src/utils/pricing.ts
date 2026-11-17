import {FareBreakdown, LatLngPoint, VehicleType} from '../types/byahero';

const EARTH_RADIUS_KM = 6371.0;

/**
 * Mirrors C++ ByaheroNativeEngine::calculateHaversineKm for consistent web/Android spatial math.
 */
export function calculateHaversineKm(a: LatLngPoint, b: LatLngPoint): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const sinLat = Math.sin(dLat / 2);
  const sinLng = Math.sin(dLng / 2);
  const h =
    sinLat * sinLat +
    Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * sinLng * sinLng;
  return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(h)));
}

export function isTodaySaturday(date = new Date()): boolean {
  return date.getDay() === 6;
}

/**
 * Mirrors C++ ByaheroNativeEngine::calculateFare:
 * - Motorcycle: Base P40 (first 2 km) + P10/km thereafter
 * - Taxi: Base P60 (first 2 km) + P15/km thereafter
 * - Saturday Generosity: 10% passenger discount on Saturdays
 * - Rider Generosity: +5% extra incentive paid to riders on every completed ride
 */
export function calculateByaheroFare(
  distanceKm: number,
  vehicleType: VehicleType,
  saturdayPromoActive: boolean
): FareBreakdown {
  const safeDistance = Math.max(0.5, distanceKm);
  const baseFare = vehicleType === 'motorcycle' ? 40 : 60;
  const perKmRate = vehicleType === 'motorcycle' ? 10 : 15;
  const billableExtraKm = Math.max(0, safeDistance - 2);
  const distanceFare = Math.round(billableExtraKm * perKmRate);
  const subtotal = baseFare + distanceFare;

  const passengerDiscount = saturdayPromoActive ? Math.round(subtotal * 0.1) : 0;
  const finalFare = Math.max(baseFare * (saturdayPromoActive ? 0.9 : 1), subtotal - passengerDiscount);

  // BYAHERO gives riders a 5% extra incentive on every successful ride
  const riderIncentive = Math.round(subtotal * 0.05 * 100) / 100;
  const riderTotalPayout = Math.round((subtotal + riderIncentive) * 100) / 100;

  return {
    baseFare,
    distanceFare,
    subtotal,
    isSaturdayPromo: saturdayPromoActive,
    passengerDiscount,
    finalFare: Math.round(finalFare),
    riderIncentive,
    riderTotalPayout,
  };
}

export function formatPhp(amount: number): string {
  return `PHP ${amount.toLocaleString('en-PH', {
    minimumFractionDigits: amount % 1 === 0 ? 0 : 2,
    maximumFractionDigits: 2,
  })}`;
}
