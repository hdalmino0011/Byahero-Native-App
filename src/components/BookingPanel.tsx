import React, {useState} from 'react';
import {
  ArrowUpDown,
  CheckCircle2,
  Clock,
  Crosshair,
  MapPin,
  Navigation,
  Percent,
  Route as RouteIcon,
} from 'lucide-react';
import {
  FareBreakdown,
  LatLngPoint,
  LocationStop,
  MapSelectionTarget,
  RideBooking,
  VehicleType,
} from '../types/byahero';
import {formatPhp} from '../utils/pricing';
import {
  PlaceSuggestionItem,
  useAutocompleteSuggestions,
} from '../hooks/useAutocompleteSuggestions';
import {ByaheroMotorcycleVector, ByaheroTaxiVector} from './VectorGraphics';

interface BookingPanelProps {
  userLocation: LatLngPoint;
  pickup: LocationStop | null;
  destination: LocationStop | null;
  vehicleType: VehicleType;
  distanceKm: number;
  durationMins: number;
  fare: FareBreakdown | null;
  saturdayPromoActive: boolean;
  isActualSaturday: boolean;
  activeRide: RideBooking | null;
  onSetPickup: (stop: LocationStop | null) => void;
  onSetDestination: (stop: LocationStop | null) => void;
  onSwapStops: () => void;
  onSelectVehicle: (type: VehicleType) => void;
  onToggleSaturdayPreview: () => void;
  onStartMapPick: (target: MapSelectionTarget) => void;
  onUseCurrentGpsAsPickup: () => void;
  onBookRide: () => void;
  onAdvanceRideStatus: () => void;
  onCancelRide: () => void;
}

export const BookingPanel: React.FC<BookingPanelProps> = ({
  userLocation,
  pickup,
  destination,
  vehicleType,
  distanceKm,
  durationMins,
  fare,
  saturdayPromoActive,
  isActualSaturday,
  activeRide,
  onSetPickup,
  onSetDestination,
  onSwapStops,
  onSelectVehicle,
  onToggleSaturdayPreview,
  onStartMapPick,
  onUseCurrentGpsAsPickup,
  onBookRide,
  onAdvanceRideStatus,
  onCancelRide,
}) => {
  const [pickupQuery, setPickupQuery] = useState('');
  const [destQuery, setDestQuery] = useState('');
  const [focusedField, setFocusedField] = useState<'pickup' | 'destination' | null>(
    null
  );

  const activeInput = focusedField === 'pickup' ? pickupQuery : destQuery;
  const {suggestions, isLoading, resolvePlace, resetSession} =
    useAutocompleteSuggestions(activeInput, userLocation);

  const handleSelectSuggestion = async (item: PlaceSuggestionItem) => {
    const resolved = await resolvePlace(item);
    if (!resolved) return;
    if (focusedField === 'pickup') {
      onSetPickup(resolved);
      setPickupQuery('');
    } else {
      onSetDestination(resolved);
      setDestQuery('');
    }
    setFocusedField(null);
    resetSession();
  };

  if (activeRide) {
    const statusSteps = [
      {key: 'searching', label: 'Finding nearest BYAHERO rider'},
      {key: 'accepted', label: 'Rider assigned and confirmed'},
      {key: 'arriving', label: 'Rider arriving at pickup point'},
      {key: 'in_progress', label: 'Trip in progress to destination'},
      {key: 'completed', label: 'Ride completed safely'},
    ] as const;

    const currentIdx = statusSteps.findIndex((s) => s.key === activeRide.status);

    return (
      <div className="space-y-4">
        <div className="p-4 rounded-2xl glass-green text-white space-y-3">
          <div className="flex items-center justify-between gap-2">
            <div>
              <div className="text-xs font-semibold text-emerald-200">
                {activeRide.vehicleType === 'motorcycle'
                  ? 'BYAHERO Moto'
                  : 'BYAHERO Cab'}
              </div>
              <h3 className="text-base font-bold text-white mt-0.5">
                {statusSteps[Math.max(0, currentIdx)].label}
              </h3>
            </div>
            <div className="text-right tabular-nums">
              <div className="text-lg font-extrabold text-white">
                {formatPhp(activeRide.fare.finalFare)}
              </div>
              {activeRide.fare.isSaturdayPromo && (
                <div className="text-[11px] text-emerald-200">10% Saturday Off</div>
              )}
            </div>
          </div>

          <div className="grid grid-cols-5 gap-1.5 pt-1">
            {statusSteps.map((step, idx) => (
              <div
                key={step.key}
                className={`h-1.5 rounded-full transition-colors ${
                  idx <= currentIdx ? 'bg-white' : 'bg-white/25'
                }`}
              />
            ))}
          </div>

          {activeRide.riderName && (
            <div className="pt-2.5 border-t border-white/15 flex items-center justify-between text-xs">
              <div>
                <div className="font-bold text-white">{activeRide.riderName}</div>
                <div className="text-white/75">
                  Plate {activeRide.riderPlate} · {activeRide.riderRating?.toFixed(2)} rating
                </div>
              </div>
              <div className="text-right tabular-nums text-emerald-200 font-semibold">
                +{formatPhp(activeRide.fare.riderIncentive)} rider bonus
              </div>
            </div>
          )}
        </div>

        <div className="p-3.5 rounded-2xl glass-surface space-y-2.5 text-xs">
          <div className="flex items-start gap-2.5">
            <MapPin className="w-4 h-4 text-[#0F522B] mt-0.5 shrink-0" />
            <div className="min-w-0">
              <div className="text-[#55625A]">Pickup</div>
              <div className="font-semibold text-[#101713] truncate">
                {activeRide.pickup.label}
              </div>
            </div>
          </div>
          <div className="flex items-start gap-2.5">
            <Navigation className="w-4 h-4 text-[#0F522B] mt-0.5 shrink-0" />
            <div className="min-w-0">
              <div className="text-[#55625A]">Destination</div>
              <div className="font-semibold text-[#101713] truncate">
                {activeRide.destination.label}
              </div>
            </div>
          </div>
        </div>

        <div className="flex gap-2.5">
          {activeRide.status !== 'completed' ? (
            <>
              <button
                type="button"
                onClick={onAdvanceRideStatus}
                className="flex-1 h-12 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-sm font-bold shadow-md transition-colors"
              >
                {activeRide.status === 'searching'
                  ? 'Match Rider Now'
                  : activeRide.status === 'accepted'
                  ? 'Simulate Arrival'
                  : activeRide.status === 'arriving'
                  ? 'Start Trip'
                  : 'Complete Ride'}
              </button>
              <button
                type="button"
                onClick={onCancelRide}
                className="px-4 h-12 rounded-xl glass-surface hover:bg-white text-[#101713] text-xs font-semibold transition-colors"
              >
                Cancel
              </button>
            </>
          ) : (
            <button
              type="button"
              onClick={onCancelRide}
              className="w-full h-12 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-sm font-bold flex items-center justify-center gap-2 shadow-md transition-colors"
            >
              <CheckCircle2 className="w-4 h-4 text-emerald-200" />
              <span>Book Another Ride</span>
            </button>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Saturday 10% Discount Hero Card (Deep Forest Green Glass) */}
      <div className="flex items-center justify-between gap-3 px-4 py-3 rounded-2xl glass-green text-white">
        <div className="flex items-center gap-3 min-w-0">
          <div className="w-9 h-9 rounded-xl bg-white/15 border border-white/20 flex items-center justify-center shrink-0">
            <Percent className="w-4 h-4 text-white" />
          </div>
          <div className="min-w-0">
            <div className="text-xs font-bold text-white truncate">
              {saturdayPromoActive
                ? 'Saturday 10% Passenger Discount Active'
                : 'Every Saturday: 10% Off + 5% Rider Bonus'}
            </div>
            <div className="text-[11px] text-white/80 truncate">
              {isActualSaturday
                ? 'Applied automatically today'
                : saturdayPromoActive
                ? 'Previewing Saturday discount rates'
                : 'Riders earn +5% extra on every ride'}
            </div>
          </div>
        </div>
        {!isActualSaturday && (
          <button
            type="button"
            onClick={onToggleSaturdayPreview}
            className={`min-h-[36px] px-3 rounded-xl text-[11px] font-bold whitespace-nowrap shrink-0 transition-colors ${
              saturdayPromoActive
                ? 'bg-white text-[#0F522B]'
                : 'bg-white/15 text-white border border-white/25 hover:bg-white/25'
            }`}
          >
            {saturdayPromoActive ? 'Sat On' : 'Preview Sat'}
          </button>
        )}
      </div>

      {/* Pickup & Destination Frosted Glass Inputs */}
      <div className="relative space-y-2.5">
        {/* Pickup Row */}
        <div className="flex items-center gap-1.5 glass-surface focus-within:border-[#0F522B] rounded-2xl px-3.5 py-2 transition-colors">
          <span className="w-2.5 h-2.5 rounded-full bg-[#101713] ring-4 ring-[#0F522B]/20 shrink-0" />
          <div className="flex-1 min-w-0">
            <div className="text-[10px] font-medium text-[#55625A]">Pickup Location</div>
            <input
              type="text"
              value={
                focusedField === 'pickup'
                  ? pickupQuery
                  : pickup?.label || ''
              }
              onFocus={() => {
                setFocusedField('pickup');
                setPickupQuery(pickup?.label || '');
              }}
              onChange={(e) => {
                setFocusedField('pickup');
                setPickupQuery(e.target.value);
              }}
              placeholder="Enter pickup or tap GPS"
              aria-label="Pickup location"
              className="w-full bg-transparent text-sm font-semibold text-[#101713] placeholder:text-[#86948C] focus:outline-none truncate"
            />
          </div>
          <button
            type="button"
            onClick={onUseCurrentGpsAsPickup}
            title="Use Current GPS Location"
            aria-label="Use current GPS as pickup"
            className="min-h-[38px] min-w-[38px] flex items-center justify-center rounded-xl text-[#0F522B] hover:bg-[#0F522B]/10 transition-colors"
          >
            <Crosshair className="w-4 h-4" />
          </button>
          <button
            type="button"
            onClick={() => onStartMapPick('pickup')}
            className="min-h-[38px] px-2.5 rounded-xl bg-[#EBF1ED] hover:bg-[#DCE7E0] text-[11px] font-semibold text-[#0F522B] whitespace-nowrap"
          >
            Map
          </button>
        </div>

        {/* Swap Button */}
        <div className="absolute right-14 top-1/2 -translate-y-1/2 z-10">
          <button
            type="button"
            onClick={onSwapStops}
            aria-label="Swap pickup and destination"
            className="w-8 h-8 rounded-full bg-white border border-[#0F522B]/20 flex items-center justify-center text-[#0F522B] hover:bg-[#0F522B] hover:text-white shadow-md transition-colors"
          >
            <ArrowUpDown className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* Destination Row */}
        <div className="flex items-center gap-1.5 glass-surface focus-within:border-[#0F522B] rounded-2xl px-3.5 py-2 transition-colors">
          <span className="w-2.5 h-2.5 rounded-sm bg-[#0F522B] shrink-0" />
          <div className="flex-1 min-w-0">
            <div className="text-[10px] font-medium text-[#55625A]">Destination</div>
            <input
              type="text"
              value={
                focusedField === 'destination'
                  ? destQuery
                  : destination?.label || ''
              }
              onFocus={() => {
                setFocusedField('destination');
                setDestQuery(destination?.label || '');
              }}
              onChange={(e) => {
                setFocusedField('destination');
                setDestQuery(e.target.value);
              }}
              placeholder="Where are you heading?"
              aria-label="Destination location"
              className="w-full bg-transparent text-sm font-semibold text-[#101713] placeholder:text-[#86948C] focus:outline-none truncate"
            />
          </div>
          <button
            type="button"
            onClick={() => onStartMapPick('destination')}
            className="min-h-[38px] px-2.5 rounded-xl bg-[#EBF1ED] hover:bg-[#DCE7E0] text-[11px] font-semibold text-[#0F522B] whitespace-nowrap"
          >
            Map
          </button>
        </div>

        {/* Autocomplete Suggestions List */}
        {focusedField && (suggestions.length > 0 || isLoading) && (
          <div className="glass-surface rounded-2xl overflow-hidden divide-y divide-[#0F522B]/10 shadow-xl">
            {isLoading && suggestions.length === 0 && (
              <div className="px-4 py-3 text-xs text-[#55625A]">
                Fetching Google Places suggestions...
              </div>
            )}
            {suggestions.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => void handleSelectSuggestion(item)}
                className="w-full min-h-[46px] px-3.5 py-2.5 text-left hover:bg-[#0F522B]/5 transition-colors flex items-start gap-2.5"
              >
                <MapPin className="w-4 h-4 text-[#0F522B] mt-0.5 shrink-0" />
                <div className="min-w-0">
                  <div className="text-xs font-semibold text-[#101713] truncate">
                    {item.mainText}
                  </div>
                  {item.secondaryText && (
                    <div className="text-[11px] text-[#55625A] truncate">
                      {item.secondaryText}
                    </div>
                  )}
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Vehicle Selection: Motorcycle vs Taxi */}
      <div className="grid grid-cols-2 gap-2.5">
        <button
          type="button"
          onClick={() => onSelectVehicle('motorcycle')}
          className={`p-3.5 rounded-2xl border text-left transition-all flex flex-col justify-between min-h-[108px] ${
            vehicleType === 'motorcycle'
              ? 'glass-green text-white'
              : 'glass-surface text-[#101713] hover:border-[#0F522B]/35'
          }`}
        >
          <div className="flex items-start justify-between gap-1">
            <ByaheroMotorcycleVector
              className="w-16 h-9"
              lightSurface={vehicleType !== 'motorcycle'}
            />
            <span
              className={`text-[11px] font-semibold tabular-nums ${
                vehicleType === 'motorcycle' ? 'text-emerald-200' : 'text-[#0F522B]'
              }`}
            >
              Base P40
            </span>
          </div>
          <div className="mt-2">
            <div className="text-sm font-bold">BYAHERO Moto</div>
            <div
              className={`text-[11px] ${
                vehicleType === 'motorcycle' ? 'text-white/80' : 'text-[#55625A]'
              }`}
            >
              Fast traffic-beat ride
            </div>
          </div>
        </button>

        <button
          type="button"
          onClick={() => onSelectVehicle('taxi')}
          className={`p-3.5 rounded-2xl border text-left transition-all flex flex-col justify-between min-h-[108px] ${
            vehicleType === 'taxi'
              ? 'glass-green text-white'
              : 'glass-surface text-[#101713] hover:border-[#0F522B]/35'
          }`}
        >
          <div className="flex items-start justify-between gap-1">
            <ByaheroTaxiVector
              className="w-16 h-9"
              lightSurface={vehicleType !== 'taxi'}
            />
            <span
              className={`text-[11px] font-semibold tabular-nums ${
                vehicleType === 'taxi' ? 'text-emerald-200' : 'text-[#0F522B]'
              }`}
            >
              Base P60
            </span>
          </div>
          <div className="mt-2">
            <div className="text-sm font-bold">BYAHERO Cab</div>
            <div
              className={`text-[11px] ${
                vehicleType === 'taxi' ? 'text-white/80' : 'text-[#55625A]'
              }`}
            >
              Air-conditioned 4-seater
            </div>
          </div>
        </button>
      </div>

      {/* Fare & Route Summary */}
      {pickup && destination && fare ? (
        <div className="p-3.5 rounded-2xl glass-surface space-y-2.5">
          <div className="flex items-center justify-between text-xs text-[#55625A] tabular-nums">
            <span className="inline-flex items-center gap-1.5">
              <RouteIcon className="w-3.5 h-3.5 text-[#0F522B]" />
              {distanceKm.toFixed(2)} km
            </span>
            <span className="inline-flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-[#0F522B]" />
              {durationMins} min ETA
            </span>
            <span className="text-[#0F522B] font-bold">
              +{formatPhp(fare.riderIncentive)} rider bonus
            </span>
          </div>

          <div className="pt-2 border-t border-[#0F522B]/10 flex items-baseline justify-between tabular-nums">
            <div className="text-xs text-[#55625A]">
              {fare.isSaturdayPromo
                ? `Subtotal ${formatPhp(fare.subtotal)} · -10% Saturday Discount`
                : 'Total Passenger Fare'}
            </div>
            <div className="flex items-baseline gap-2">
              {fare.isSaturdayPromo && (
                <span className="text-xs text-[#86948C] line-through">
                  {formatPhp(fare.subtotal)}
                </span>
              )}
              <span className="text-xl font-extrabold text-[#101713]">
                {formatPhp(fare.finalFare)}
              </span>
            </div>
          </div>
        </div>
      ) : (
        <div className="px-4 py-3 rounded-2xl glass-surface text-xs text-[#55625A]">
          Select a pickup and destination (or pick a restaurant or resort in Explore) to calculate live route and fare.
        </div>
      )}

      {/* Primary Booking CTA */}
      <button
        type="button"
        disabled={!pickup || !destination || !fare}
        onClick={onBookRide}
        className="w-full h-12 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] disabled:bg-[#DCE4DF] disabled:text-[#7E8C84] text-white font-bold text-sm transition-all active:scale-[0.99] shadow-lg"
      >
        {pickup && destination && fare
          ? `Book ${vehicleType === 'motorcycle' ? 'Moto' : 'Taxi'} · ${formatPhp(fare.finalFare)}`
          : 'Select Pickup & Destination'}
      </button>
    </div>
  );
};
