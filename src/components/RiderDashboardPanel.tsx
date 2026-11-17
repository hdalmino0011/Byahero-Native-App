import React from 'react';
import {Bike, Car, CheckCircle2, MapPin, Navigation, Power, TrendingUp} from 'lucide-react';
import {RideBooking, UserProfile} from '../types/byahero';
import {formatPhp} from '../utils/pricing';

interface RiderDashboardPanelProps {
  profile: UserProfile;
  incomingRequests: RideBooking[];
  activeRiderJob: RideBooking | null;
  onEnableRiderRole: () => void;
  onToggleOnline: () => void;
  onAcceptRideRequest: (ride: RideBooking) => void;
  onCompleteRiderJob: () => void;
}

export const RiderDashboardPanel: React.FC<RiderDashboardPanelProps> = ({
  profile,
  incomingRequests,
  activeRiderJob,
  onEnableRiderRole,
  onToggleOnline,
  onAcceptRideRequest,
  onCompleteRiderJob,
}) => {
  const canRide = profile.role === 'rider' || profile.role === 'both';

  if (!canRide) {
    return (
      <div className="p-5 rounded-2xl glass-surface space-y-4">
        <div className="w-11 h-11 rounded-2xl bg-[#0F522B] flex items-center justify-center shadow-sm">
          <Bike className="w-5 h-5 text-white" />
        </div>
        <div>
          <h3 className="text-base font-bold text-[#101713]">
            Switch to Rider & Earn +5% Extra Incentive
          </h3>
          <p className="text-xs text-[#55625A] mt-1 leading-relaxed">
            BYAHERO allows seamless switching between Passenger and Rider roles. Activate Dual Role (Both) to accept motorcycle or taxi trips and earn an extra 5% incentive on every completed ride.
          </p>
        </div>
        <button
          type="button"
          onClick={onEnableRiderRole}
          className="w-full h-12 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-sm font-bold shadow-md transition-colors"
        >
          Activate Rider Mode (Dual Role)
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Rider Online Header */}
      <div className="p-4 rounded-2xl glass-surface flex items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2 text-xs text-[#55625A]">
            {profile.vehicleType === 'motorcycle' ? (
              <Bike className="w-4 h-4 text-[#0F522B]" />
            ) : (
              <Car className="w-4 h-4 text-[#0F522B]" />
            )}
            <span>
              {profile.vehicleType === 'motorcycle' ? 'Motorcycle' : 'Taxi'} ·{' '}
              {profile.plateNumber || 'BYH-2026'}
            </span>
          </div>
          <div className="text-sm font-bold text-[#101713] mt-0.5">
            {profile.isOnline ? 'Online & Receiving Requests' : 'Offline'}
          </div>
        </div>

        <button
          type="button"
          onClick={onToggleOnline}
          className={`min-h-[44px] px-4 rounded-xl text-xs font-bold inline-flex items-center gap-2 transition-colors ${
            profile.isOnline
              ? 'bg-[#0F522B] text-white hover:bg-[#146C3A] shadow-sm'
              : 'bg-[#EBF1ED] text-[#101713] hover:bg-[#DCE7E0]'
          }`}
        >
          <Power className="w-4 h-4" />
          <span>{profile.isOnline ? 'Go Offline' : 'Go Online'}</span>
        </button>
      </div>

      {/* Generous Earnings & +5% Incentive Summary */}
      <div className="grid grid-cols-2 gap-2.5 tabular-nums">
        <div className="p-3.5 rounded-2xl glass-surface">
          <div className="text-[11px] text-[#55625A] font-medium">Total Payouts</div>
          <div className="text-xl font-extrabold text-[#101713] mt-1">
            {formatPhp(profile.riderEarningsPhp)}
          </div>
          <div className="text-[11px] text-[#55625A] mt-0.5">
            {profile.totalRides} completed rides
          </div>
        </div>
        <div className="p-3.5 rounded-2xl glass-green text-white">
          <div className="flex items-center gap-1 text-[11px] text-emerald-200 font-semibold">
            <TrendingUp className="w-3.5 h-3.5" />
            <span>+5% Rider Incentives</span>
          </div>
          <div className="text-xl font-extrabold text-white mt-1">
            {formatPhp(profile.riderIncentivesPhp)}
          </div>
          <div className="text-[11px] text-white/80 mt-0.5">
            Paid on every ride
          </div>
        </div>
      </div>

      {/* Active Job or Incoming Queue */}
      {activeRiderJob ? (
        <div className="p-4 rounded-2xl glass-green text-white space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-emerald-200">Active Rider Dispatch</span>
            <span className="text-sm font-extrabold text-white tabular-nums">
              Payout {formatPhp(activeRiderJob.fare.riderTotalPayout)}
            </span>
          </div>
          <div className="space-y-1.5 text-xs">
            <div className="text-white font-semibold">
              Passenger: {activeRiderJob.passengerName}
            </div>
            <div className="text-white/85 flex items-center gap-1.5">
              <MapPin className="w-3.5 h-3.5 text-white shrink-0" />
              <span className="truncate">{activeRiderJob.pickup.label}</span>
            </div>
            <div className="text-emerald-200 flex items-center gap-1.5">
              <Navigation className="w-3.5 h-3.5 text-emerald-200 shrink-0" />
              <span className="truncate">{activeRiderJob.destination.label}</span>
            </div>
          </div>
          <button
            type="button"
            onClick={onCompleteRiderJob}
            className="w-full h-11 rounded-xl bg-white text-[#101713] font-bold text-xs flex items-center justify-center gap-2 hover:bg-[#F4F7F5] shadow-sm transition-colors"
          >
            <CheckCircle2 className="w-4 h-4 text-[#0F522B]" />
            <span>
              Complete & Claim +{formatPhp(activeRiderJob.fare.riderIncentive)} Bonus
            </span>
          </button>
        </div>
      ) : (
        <div className="space-y-2.5">
          <div className="text-xs font-bold text-[#101713]">Nearby Ride Requests</div>
          {!profile.isOnline ? (
            <div className="p-4 rounded-2xl glass-surface text-xs text-[#55625A]">
              Go online to view and accept real-time passenger requests nearby.
            </div>
          ) : incomingRequests.length === 0 ? (
            <div className="p-4 rounded-2xl glass-surface text-xs text-[#55625A]">
              Looking for nearby passenger ride requests...
            </div>
          ) : (
            incomingRequests.map((req) => (
              <div
                key={req.id}
                className="p-3.5 rounded-2xl glass-surface space-y-2.5"
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <div className="text-xs font-bold text-[#101713]">
                      {req.passengerName} ·{' '}
                      {req.vehicleType === 'motorcycle' ? 'Moto' : 'Taxi'}
                    </div>
                    <div className="text-[11px] text-[#55625A] tabular-nums mt-0.5">
                      {req.distanceKm.toFixed(1)} km · {req.durationMins} mins
                    </div>
                  </div>
                  <div className="text-right tabular-nums">
                    <div className="text-sm font-extrabold text-[#101713]">
                      {formatPhp(req.fare.riderTotalPayout)}
                    </div>
                    <div className="text-[11px] text-[#0F522B] font-bold">
                      +{formatPhp(req.fare.riderIncentive)} (5% bonus)
                    </div>
                  </div>
                </div>

                <div className="text-xs text-[#55625A] space-y-1">
                  <div className="truncate">From: {req.pickup.label}</div>
                  <div className="truncate font-medium text-[#101713]">
                    To: {req.destination.label}
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => onAcceptRideRequest(req)}
                  className="w-full h-10 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold shadow-sm transition-colors"
                >
                  Accept Ride
                </button>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};
