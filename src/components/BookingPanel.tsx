import React, {useState} from 'react';
import {
  AlertCircle,
  ArrowUpDown,
  Banknote,
  Bookmark,
  CheckCircle2,
  Clock,
  CreditCard,
  Crosshair,
  MapPin,
  Navigation,
  Percent,
  Plus,
  Route as RouteIcon,
  Smartphone,
  Tag,
  X,
} from 'lucide-react';
import {
  FareBreakdown,
  FavoriteDestination,
  LatLngPoint,
  LocationStop,
  MapSelectionTarget,
  PaymentMethodType,
  RideBooking,
  SavedPaymentMethod,
  VehicleType,
} from '../types/byahero';
import {formatPhp} from '../utils/pricing';
import {areLocationsSame} from '../utils/locationMatch';
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
  locationError?: string | null;
  paymentMethods: SavedPaymentMethod[];
  selectedPaymentId: string;
  favorites: FavoriteDestination[];
  appliedPromoCode: string | null;
  onSelectPaymentMethod: (id: string) => void;
  onSavePaymentMethod: (method: SavedPaymentMethod) => void;
  onAddFavoriteDestination: (stop: LocationStop) => void;
  onOpenSettingsSection: (
    section: 'account' | 'favorites' | 'payments' | 'promos' | 'support' | 'about'
  ) => void;
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
  locationError,
  paymentMethods,
  selectedPaymentId,
  favorites,
  appliedPromoCode,
  onSelectPaymentMethod,
  onSavePaymentMethod,
  onAddFavoriteDestination,
  onOpenSettingsSection,
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
  const stopsAreIdentical = Boolean(
    pickup && destination && areLocationsSame(pickup, destination)
  );
  const [pickupQuery, setPickupQuery] = useState('');
  const [destQuery, setDestQuery] = useState('');
  const [focusedField, setFocusedField] = useState<'pickup' | 'destination' | null>(
    null
  );
  const [fallbackResults, setFallbackResults] = useState<LocationStop[]>([]);

  const [addingPaymentType, setAddingPaymentType] = useState<'gcash' | 'card' | null>(
    null
  );
  const [gcashNumber, setGcashNumber] = useState('');
  const [gcashName, setGcashName] = useState('');
  const [cardHolder, setCardHolder] = useState('');
  const [cardNumber, setCardNumber] = useState('');
  const [cardExpiry, setCardExpiry] = useState('');
  const [cardCvv, setCardCvv] = useState('');
  const [paymentFormError, setPaymentFormError] = useState<string | null>(null);

  const activeInput = focusedField === 'pickup' ? pickupQuery : destQuery;
  const {suggestions, isLoading, resolvePlace, resetSession} =
    useAutocompleteSuggestions(activeInput, userLocation);

  const selectedPayment =
    paymentMethods.find((m) => m.id === selectedPaymentId) || paymentMethods[0];
  const savedGcash = paymentMethods.find((m) => m.type === 'gcash');
  const savedCard = paymentMethods.find((m) => m.type === 'card');

  const isDestAlreadyFavorite = Boolean(
    destination &&
      favorites.some((f) =>
        areLocationsSame(
          {label: f.label, address: f.address, lat: f.lat, lng: f.lng},
          destination
        )
      )
  );

  const runFallbackSearch = async (q: string) => {
    const trimmed = q.trim();
    if (trimmed.length < 2) {
      setFallbackResults([]);
      return;
    }
    try {
      const delta = 0.35;
      const viewbox = `${userLocation.lng - delta},${userLocation.lat + delta},${userLocation.lng + delta},${userLocation.lat - delta}`;
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=jsonv2&q=${encodeURIComponent(trimmed)}&limit=5&viewbox=${viewbox}&bounded=0`,
        {headers: {Accept: 'application/json'}}
      );
      if (res.ok) {
        const rows = (await res.json()) as Array<{
          place_id: number;
          display_name: string;
          name?: string;
          lat: string;
          lon: string;
        }>;
        setFallbackResults(
          rows.map((r) => ({
            placeId: String(r.place_id),
            label: r.name || r.display_name.split(',')[0].trim(),
            address: r.display_name,
            lat: Number(r.lat),
            lng: Number(r.lon),
          }))
        );
      }
    } catch {
      setFallbackResults([]);
    }
  };

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
    setFallbackResults([]);
    setFocusedField(null);
    resetSession();
  };

  const handleSelectFallbackStop = (stop: LocationStop) => {
    if (focusedField === 'pickup') {
      onSetPickup(stop);
      setPickupQuery('');
    } else {
      onSetDestination(stop);
      setDestQuery('');
    }
    setFallbackResults([]);
    setFocusedField(null);
    resetSession();
  };

  const handleChooseQuickPaymentType = (type: PaymentMethodType) => {
    setPaymentFormError(null);
    if (type === 'cash') {
      const cashMethod = paymentMethods.find((m) => m.type === 'cash');
      if (cashMethod) onSelectPaymentMethod(cashMethod.id);
      setAddingPaymentType(null);
      return;
    }
    if (type === 'gcash') {
      if (savedGcash) {
        onSelectPaymentMethod(savedGcash.id);
        setAddingPaymentType(null);
      } else {
        setAddingPaymentType('gcash');
      }
      return;
    }
    if (type === 'card') {
      if (savedCard) {
        onSelectPaymentMethod(savedCard.id);
        setAddingPaymentType(null);
      } else {
        setAddingPaymentType('card');
      }
    }
  };

  const handleConnectGcash = (e: React.FormEvent) => {
    e.preventDefault();
    const digits = gcashNumber.replace(/\D/g, '');
    if (digits.length < 10) {
      setPaymentFormError('Enter a valid 11-digit GCash mobile number.');
      return;
    }
    const cleanName = gcashName.trim() || 'BYAHERO Passenger';
    const last4 = digits.slice(-4);
    const created: SavedPaymentMethod = {
      id: `pm_gcash_${Date.now()}`,
      type: 'gcash',
      label: `GCash (•••• ${last4})`,
      subtitle: `${cleanName} · Connected GCash`,
      accountNumberOrLast4: last4,
      holderName: cleanName,
      isDefault: true,
    };
    onSavePaymentMethod(created);
    setGcashNumber('');
    setGcashName('');
    setPaymentFormError(null);
    setAddingPaymentType(null);
  };

  const handleRegisterCard = (e: React.FormEvent) => {
    e.preventDefault();
    const digits = cardNumber.replace(/\D/g, '');
    if (digits.length < 12) {
      setPaymentFormError('Enter a valid 16-digit credit or debit card number.');
      return;
    }
    if (!cardExpiry.trim()) {
      setPaymentFormError('Enter card expiry date (MM/YY).');
      return;
    }
    if (cardCvv.replace(/\D/g, '').length < 3) {
      setPaymentFormError('Enter a valid 3-digit CVV.');
      return;
    }
    const cleanHolder = cardHolder.trim() || 'BYAHERO Cardholder';
    const last4 = digits.slice(-4);
    const brand = digits.startsWith('4') ? 'Visa' : digits.startsWith('5') ? 'Mastercard' : 'Card';
    const created: SavedPaymentMethod = {
      id: `pm_card_${Date.now()}`,
      type: 'card',
      label: `${brand} •••• ${last4}`,
      subtitle: `${cleanHolder} · Exp ${cardExpiry.trim()}`,
      accountNumberOrLast4: last4,
      holderName: cleanHolder,
      expiry: cardExpiry.trim(),
      isDefault: true,
    };
    onSavePaymentMethod(created);
    setCardHolder('');
    setCardNumber('');
    setCardExpiry('');
    setCardCvv('');
    setPaymentFormError(null);
    setAddingPaymentType(null);
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
                  : 'BYAHERO Cab'}{' '}
                · {activeRide.paymentMethodLabel || 'Cash'}
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
    <div className="space-y-3.5">
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

      <div className="relative space-y-2.5">
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
                setPickupQuery('');
              }}
              onChange={(e) => {
                setFocusedField('pickup');
                setPickupQuery(e.target.value);
                if (suggestions.length === 0 && e.target.value.trim().length >= 3) {
                  void runFallbackSearch(e.target.value);
                }
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  void runFallbackSearch(pickupQuery);
                }
              }}
              placeholder={pickup?.label || 'Search pickup place or tap GPS'}
              aria-label="Pickup location"
              className="w-full bg-transparent text-sm font-semibold text-[#101713] placeholder:text-[#55625A] focus:outline-none truncate"
            />
          </div>
          {focusedField === 'pickup' && pickupQuery && (
            <button
              type="button"
              onClick={() => {
                setPickupQuery('');
                setFallbackResults([]);
                resetSession();
              }}
              aria-label="Clear pickup search"
              className="min-h-[34px] min-w-[34px] flex items-center justify-center rounded-lg text-[#55625A] hover:text-[#101713]"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
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
                setDestQuery('');
              }}
              onChange={(e) => {
                setFocusedField('destination');
                setDestQuery(e.target.value);
                if (suggestions.length === 0 && e.target.value.trim().length >= 3) {
                  void runFallbackSearch(e.target.value);
                }
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  void runFallbackSearch(destQuery);
                }
              }}
              placeholder={destination?.label || 'Where are you heading?'}
              aria-label="Destination location"
              className="w-full bg-transparent text-sm font-semibold text-[#101713] placeholder:text-[#55625A] focus:outline-none truncate"
            />
          </div>
          {destination && !isDestAlreadyFavorite && (
            <button
              type="button"
              onClick={() => onAddFavoriteDestination(destination)}
              title="Save Destination to Favorites"
              aria-label="Save destination to favorites"
              className="min-h-[34px] px-2 rounded-lg text-[#0F522B] hover:bg-[#0F522B]/10 text-[10px] font-bold flex items-center gap-1 shrink-0"
            >
              <Bookmark className="w-3.5 h-3.5" />
              <span>Save</span>
            </button>
          )}
          {focusedField === 'destination' && destQuery && (
            <button
              type="button"
              onClick={() => {
                setDestQuery('');
                setFallbackResults([]);
                resetSession();
              }}
              aria-label="Clear destination search"
              className="min-h-[34px] min-w-[34px] flex items-center justify-center rounded-lg text-[#55625A] hover:text-[#101713]"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
          <button
            type="button"
            onClick={() => onStartMapPick('destination')}
            className="min-h-[38px] px-2.5 rounded-xl bg-[#EBF1ED] hover:bg-[#DCE7E0] text-[11px] font-semibold text-[#0F522B] whitespace-nowrap"
          >
            Map
          </button>
        </div>

        {focusedField && (suggestions.length > 0 || fallbackResults.length > 0 || isLoading) && (
          <div className="glass-surface rounded-2xl overflow-hidden divide-y divide-[#0F522B]/10 shadow-xl">
            {isLoading && suggestions.length === 0 && fallbackResults.length === 0 && (
              <div className="px-4 py-3 text-xs text-[#55625A]">
                Searching places...
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
            {suggestions.length === 0 &&
              fallbackResults.map((stop, idx) => {
                const conflictsOpposite =
                  focusedField === 'pickup'
                    ? areLocationsSame(stop, destination)
                    : areLocationsSame(stop, pickup);
                return (
                  <button
                    key={stop.placeId || `fb_${idx}`}
                    type="button"
                    onClick={() => handleSelectFallbackStop(stop)}
                    className="w-full min-h-[46px] px-3.5 py-2.5 text-left hover:bg-[#0F522B]/5 transition-colors flex items-center justify-between gap-2.5"
                  >
                    <div className="flex items-start gap-2.5 min-w-0">
                      <MapPin className="w-4 h-4 text-[#0F522B] mt-0.5 shrink-0" />
                      <div className="min-w-0">
                        <div className="text-xs font-semibold text-[#101713] truncate">
                          {stop.label}
                        </div>
                        <div className="text-[11px] text-[#55625A] truncate">
                          {stop.address}
                        </div>
                      </div>
                    </div>
                    {conflictsOpposite && (
                      <span className="text-[10px] font-bold text-red-700 shrink-0">
                        {focusedField === 'pickup' ? 'Selected as Dest' : 'Selected as Pickup'}
                      </span>
                    )}
                  </button>
                );
              })}
          </div>
        )}

        {(locationError || stopsAreIdentical) && (
          <div className="flex items-center gap-2 px-3.5 py-2.5 rounded-xl bg-red-50 border border-red-200 text-xs font-medium text-red-700">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
            <span>
              {locationError ||
                'Pickup and destination cannot be the same location.'}
            </span>
          </div>
        )}
      </div>

      {favorites.length > 0 && (
        <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-0.5">
          <span className="text-[10px] font-bold text-[#55625A] shrink-0 mr-1">
            Favorites:
          </span>
          {favorites.slice(0, 4).map((fav) => (
            <button
              key={fav.id}
              type="button"
              onClick={() =>
                onSetDestination({
                  label: fav.label,
                  address: fav.address,
                  lat: fav.lat,
                  lng: fav.lng,
                })
              }
              className="h-7 px-2.5 rounded-lg bg-[#EBF1ED] hover:bg-[#DCE7E0] text-[#0F522B] text-[11px] font-semibold whitespace-nowrap shrink-0 transition-colors"
            >
              {fav.label}
            </button>
          ))}
        </div>
      )}

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

      <div className="p-3.5 rounded-2xl glass-surface space-y-2.5">
        <div className="flex items-center justify-between">
          <div className="text-xs font-bold text-[#101713]">Payment Method</div>
          <button
            type="button"
            onClick={() => onOpenSettingsSection('payments')}
            className="text-[11px] font-bold text-[#0F522B] hover:underline"
          >
            Manage in Settings
          </button>
        </div>

        <div className="grid grid-cols-3 gap-2">
          <button
            type="button"
            onClick={() => handleChooseQuickPaymentType('cash')}
            className={`p-2.5 rounded-xl border text-left transition-all ${
              selectedPayment?.type === 'cash' && !addingPaymentType
                ? 'bg-[#0F522B] text-white border-[#0F522B]'
                : 'bg-white/80 text-[#101713] border-[#0F522B]/15 hover:border-[#0F522B]/40'
            }`}
          >
            <Banknote className="w-4 h-4 mb-1" />
            <div className="text-xs font-bold">Cash</div>
            <div
              className={`text-[10px] truncate ${
                selectedPayment?.type === 'cash' && !addingPaymentType
                  ? 'text-emerald-200'
                  : 'text-[#55625A]'
              }`}
            >
              Pay rider directly
            </div>
          </button>

          <button
            type="button"
            onClick={() => handleChooseQuickPaymentType('gcash')}
            className={`p-2.5 rounded-xl border text-left transition-all ${
              selectedPayment?.type === 'gcash' || addingPaymentType === 'gcash'
                ? 'bg-[#0F522B] text-white border-[#0F522B]'
                : 'bg-white/80 text-[#101713] border-[#0F522B]/15 hover:border-[#0F522B]/40'
            }`}
          >
            <Smartphone className="w-4 h-4 mb-1" />
            <div className="text-xs font-bold">GCash</div>
            <div
              className={`text-[10px] truncate ${
                selectedPayment?.type === 'gcash' || addingPaymentType === 'gcash'
                  ? 'text-emerald-200'
                  : 'text-[#55625A]'
              }`}
            >
              {savedGcash ? `•••• ${savedGcash.accountNumberOrLast4}` : '+ Connect GCash'}
            </div>
          </button>

          <button
            type="button"
            onClick={() => handleChooseQuickPaymentType('card')}
            className={`p-2.5 rounded-xl border text-left transition-all ${
              selectedPayment?.type === 'card' || addingPaymentType === 'card'
                ? 'bg-[#0F522B] text-white border-[#0F522B]'
                : 'bg-white/80 text-[#101713] border-[#0F522B]/15 hover:border-[#0F522B]/40'
            }`}
          >
            <CreditCard className="w-4 h-4 mb-1" />
            <div className="text-xs font-bold truncate">Credit/Debit</div>
            <div
              className={`text-[10px] truncate ${
                selectedPayment?.type === 'card' || addingPaymentType === 'card'
                  ? 'text-emerald-200'
                  : 'text-[#55625A]'
              }`}
            >
              {savedCard ? `•••• ${savedCard.accountNumberOrLast4}` : '+ Register Card'}
            </div>
          </button>
        </div>

        <div className="flex items-center justify-between pt-1 text-[11px]">
          <div className="text-[#55625A] truncate">
            Active: <span className="font-bold text-[#101713]">{selectedPayment?.label}</span>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={() =>
                setAddingPaymentType(addingPaymentType === 'gcash' ? null : 'gcash')
              }
              className="text-[#0F522B] font-semibold inline-flex items-center gap-0.5 hover:underline"
            >
              <Plus className="w-3 h-3" />
              <span>GCash</span>
            </button>
            <button
              type="button"
              onClick={() =>
                setAddingPaymentType(addingPaymentType === 'card' ? null : 'card')
              }
              className="text-[#0F522B] font-semibold inline-flex items-center gap-0.5 hover:underline"
            >
              <Plus className="w-3 h-3" />
              <span>Card</span>
            </button>
          </div>
        </div>

        {addingPaymentType === 'gcash' && (
          <form
            onSubmit={handleConnectGcash}
            className="pt-2.5 border-t border-[#0F522B]/10 space-y-2"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-[#0F522B]">
                Connect & Save GCash Account
              </span>
              <button
                type="button"
                onClick={() => {
                  setAddingPaymentType(null);
                  setPaymentFormError(null);
                }}
                className="text-[11px] text-[#55625A]"
              >
                Cancel
              </button>
            </div>
            <input
              type="tel"
              value={gcashNumber}
              onChange={(e) => setGcashNumber(e.target.value)}
              placeholder="GCash Mobile Number (e.g. 09171234567)"
              className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
            />
            <input
              type="text"
              value={gcashName}
              onChange={(e) => setGcashName(e.target.value)}
              placeholder="Account Full Name"
              className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
            />
            {paymentFormError && (
              <div className="text-[11px] font-medium text-red-600">{paymentFormError}</div>
            )}
            <button
              type="submit"
              className="w-full h-9 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold transition-colors"
            >
              Connect & Save GCash
            </button>
          </form>
        )}

        {addingPaymentType === 'card' && (
          <form
            onSubmit={handleRegisterCard}
            className="pt-2.5 border-t border-[#0F522B]/10 space-y-2"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-[#0F522B]">
                Register & Save Credit / Debit Card
              </span>
              <button
                type="button"
                onClick={() => {
                  setAddingPaymentType(null);
                  setPaymentFormError(null);
                }}
                className="text-[11px] text-[#55625A]"
              >
                Cancel
              </button>
            </div>
            <input
              type="text"
              value={cardHolder}
              onChange={(e) => setCardHolder(e.target.value)}
              placeholder="Cardholder Name"
              className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
            />
            <input
              type="text"
              value={cardNumber}
              onChange={(e) => setCardNumber(e.target.value)}
              placeholder="Card Number (16 digits)"
              maxLength={19}
              className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
            />
            <div className="grid grid-cols-2 gap-2">
              <input
                type="text"
                value={cardExpiry}
                onChange={(e) => setCardExpiry(e.target.value)}
                placeholder="MM/YY"
                maxLength={5}
                className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
              />
              <input
                type="password"
                value={cardCvv}
                onChange={(e) => setCardCvv(e.target.value)}
                placeholder="CVV"
                maxLength={4}
                className="w-full h-9 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-xs text-[#101713] focus:outline-none focus:border-[#0F522B]"
              />
            </div>
            {paymentFormError && (
              <div className="text-[11px] font-medium text-red-600">{paymentFormError}</div>
            )}
            <button
              type="submit"
              className="w-full h-9 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold transition-colors"
            >
              Register & Save Card
            </button>
          </form>
        )}
      </div>

      {pickup && destination && fare && !stopsAreIdentical ? (
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
            <div className="space-y-0.5">
              <div className="text-xs text-[#55625A]">
                {fare.isSaturdayPromo
                  ? `Subtotal ${formatPhp(fare.subtotal)} · -10% Discount`
                  : 'Total Passenger Fare'}
              </div>
              {appliedPromoCode && (
                <div className="text-[10px] font-bold text-[#0F522B] inline-flex items-center gap-1">
                  <Tag className="w-3 h-3" />
                  <span>Promo {appliedPromoCode} applied</span>
                </div>
              )}
            </div>
            <div className="flex items-baseline gap-2">
              {fare.passengerDiscount > 0 && (
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
          {stopsAreIdentical
            ? 'Please choose different pickup and destination locations to calculate your fare.'
            : 'Select a pickup and destination (or pick a restaurant or resort in Explore) to calculate live route and fare.'}
        </div>
      )}

      <button
        type="button"
        disabled={!pickup || !destination || !fare || stopsAreIdentical}
        onClick={onBookRide}
        className="w-full h-12 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] disabled:bg-[#DCE4DF] disabled:text-[#7E8C84] text-white font-bold text-sm transition-all active:scale-[0.99] shadow-lg"
      >
        {pickup && destination && fare && !stopsAreIdentical
          ? `Book ${vehicleType === 'motorcycle' ? 'Moto' : 'Taxi'} (${selectedPayment?.label || 'Cash'}) · ${formatPhp(fare.finalFare)}`
          : 'Select Pickup & Destination'}
      </button>
    </div>
  );
};
