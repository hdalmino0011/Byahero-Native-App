export type UserRole = 'passenger' | 'rider' | 'both';
export type ActiveMode = 'passenger' | 'rider';
export type VehicleType = 'motorcycle' | 'taxi';
export type AppTab = 'ride' | 'explore' | 'history' | 'rider' | 'account';
export type DiscoveryCategory = 'fast_food' | 'resort_beach' | 'all';
export type MapSelectionTarget = 'pickup' | 'destination' | null;

export interface LatLngPoint {
  lat: number;
  lng: number;
}

export interface LocationStop {
  label: string;
  address: string;
  lat: number;
  lng: number;
  placeId?: string;
}

export interface FareBreakdown {
  baseFare: number;
  distanceFare: number;
  subtotal: number;
  isSaturdayPromo: boolean;
  passengerDiscount: number;
  finalFare: number;
  riderIncentive: number;
  riderTotalPayout: number;
}

export type RideStatus =
  | 'draft'
  | 'searching'
  | 'accepted'
  | 'arriving'
  | 'in_progress'
  | 'completed'
  | 'cancelled';

export interface RideBooking {
  id: string;
  passengerName: string;
  riderName?: string;
  riderPlate?: string;
  riderRating?: number;
  vehicleType: VehicleType;
  pickup: LocationStop;
  destination: LocationStop;
  distanceKm: number;
  durationMins: number;
  fare: FareBreakdown;
  status: RideStatus;
  roleContext?: 'passenger' | 'rider';
  createdAt: string;
}

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  avatarUrl?: string;
  authProvider: 'google' | 'guest';
  role: UserRole;
  activeMode: ActiveMode;
  vehicleType: VehicleType;
  plateNumber: string;
  isOnline: boolean;
  totalRides: number;
  riderEarningsPhp: number;
  riderIncentivesPhp: number;
}

export interface DiscoveredPlace {
  id: string;
  name: string;
  address: string;
  lat: number;
  lng: number;
  rating?: number;
  userRatingCount?: number;
  businessStatus?: string;
  category: 'fast_food' | 'resort_beach' | 'search';
  brandTag?: string;
  googleMapsUri?: string;
  distanceKm?: number;
}
