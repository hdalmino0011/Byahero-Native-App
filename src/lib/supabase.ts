import {createClient, SupabaseClient} from '@supabase/supabase-js';
import {ActiveMode, RideBooking, UserProfile, UserRole, VehicleType} from '../types/byahero';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL as string | undefined;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined;

export const isSupabaseConfigured = Boolean(supabaseUrl && supabaseAnonKey);

export const supabase: SupabaseClient | null = isSupabaseConfigured
  ? createClient(supabaseUrl!, supabaseAnonKey!, {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
      },
    })
  : null;

export async function signInWithGoogleSupabase(): Promise<{error: string | null}> {
  if (!supabase) {
    return {error: 'Supabase environment variables not configured yet.'};
  }
  const {error} = await supabase.auth.signInWithOAuth({
    provider: 'google',
    options: {
      redirectTo: window.location.origin,
      queryParams: {
        access_type: 'offline',
        prompt: 'consent',
      },
    },
  });
  return {error: error ? error.message : null};
}

export async function syncProfileToSupabase(profile: UserProfile): Promise<void> {
  if (!supabase || profile.authProvider === 'guest') return;
  await supabase.from('profiles').upsert({
    id: profile.id,
    email: profile.email,
    full_name: profile.fullName,
    avatar_url: profile.avatarUrl ?? null,
    role: profile.role,
    active_mode: profile.activeMode,
    vehicle_type: profile.vehicleType,
    plate_number: profile.plateNumber,
    is_online: profile.isOnline,
    total_rides: profile.totalRides,
    rider_earnings_php: profile.riderEarningsPhp,
    rider_incentives_php: profile.riderIncentivesPhp,
    updated_at: new Date().toISOString(),
  });
}

export async function updateRoleInSupabase(
  userId: string,
  role: UserRole,
  activeMode: ActiveMode,
  vehicleType?: VehicleType
): Promise<void> {
  if (!supabase) return;
  await supabase
    .from('profiles')
    .update({
      role,
      active_mode: activeMode,
      ...(vehicleType ? {vehicle_type: vehicleType} : {}),
      updated_at: new Date().toISOString(),
    })
    .eq('id', userId);
}

export async function publishRideToSupabase(ride: RideBooking, passengerId: string): Promise<void> {
  if (!supabase) return;
  await supabase.from('rides').upsert({
    id: ride.id,
    passenger_id: passengerId,
    passenger_name: ride.passengerName,
    rider_name: ride.riderName ?? null,
    rider_plate: ride.riderPlate ?? null,
    vehicle_type: ride.vehicleType,
    pickup_address: ride.pickup.address,
    pickup_lat: ride.pickup.lat,
    pickup_lng: ride.pickup.lng,
    destination_address: ride.destination.address,
    destination_lat: ride.destination.lat,
    destination_lng: ride.destination.lng,
    distance_km: ride.distanceKm,
    duration_mins: ride.durationMins,
    base_fare: ride.fare.baseFare,
    discount_amount: ride.fare.passengerDiscount,
    final_fare: ride.fare.finalFare,
    rider_incentive: ride.fare.riderIncentive,
    is_saturday_promo: ride.fare.isSaturdayPromo,
    status: ride.status === 'draft' ? 'searching' : ride.status,
  });
}
