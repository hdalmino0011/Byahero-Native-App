import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {AnimatePresence, motion, MotionConfig} from 'motion/react';
import {APIProvider} from '@vis.gl/react-google-maps';
import {
  AlertCircle,
  Bike,
  ChevronDown,
  ChevronUp,
  Compass,
  History,
  Navigation,
  Settings,
  Smartphone,
} from 'lucide-react';
import {
  ActiveMode,
  AppTab,
  DiscoveredPlace,
  FavoriteDestination,
  LatLngPoint,
  LocationStop,
  MapSelectionTarget,
  PromoCodeItem,
  RideBooking,
  SavedPaymentMethod,
  SettingsSection,
  UserProfile,
  UserRole,
  VehicleType,
} from './types/byahero';
import {
  calculateByaheroFare,
  calculateHaversineKm,
  isTodaySaturday,
} from './utils/pricing';
import {
  detectApproximateBrowserLocation,
  reverseGeocodePoint,
} from './utils/reverseGeocode';
import {areLocationsSame} from './utils/locationMatch';
import {
  publishRideToSupabase,
  supabase,
  syncProfileToSupabase,
  updateRoleInSupabase,
} from './lib/supabase';
import {SplashIntro} from './components/SplashIntro';
import {AuthGateScreen} from './components/AuthGateScreen';
import {AuthModal} from './components/AuthModal';
import {GoogleMapsWorkspace} from './components/GoogleMapsWorkspace';
import {BookingPanel} from './components/BookingPanel';
import {ExploreNearbyPanel} from './components/ExploreNearbyPanel';
import {OrderHistoryPanel} from './components/OrderHistoryPanel';
import {RiderDashboardPanel} from './components/RiderDashboardPanel';
import {SettingsPanel} from './components/SettingsPanel';
import {ByaheroLogoMark, ByaheroWordmarkVector} from './components/VectorGraphics';

const MAPS_API_KEY = (import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string) || '';

const DEFAULT_CENTER: LatLngPoint = {lat: 14.5995, lng: 120.9842};

const INITIAL_PROFILE: UserProfile = {
  id: 'local_user_1',
  email: 'alex@byahero.app',
  fullName: 'Alex Rivera',
  authProvider: 'google',
  role: 'both',
  activeMode: 'passenger',
  vehicleType: 'motorcycle',
  plateNumber: 'BYH-774',
  isOnline: true,
  totalRides: 14,
  riderEarningsPhp: 3840,
  riderIncentivesPhp: 192,
};

const SEED_DISPATCH_REQUESTS: RideBooking[] = [
  {
    id: 'req_101',
    passengerName: 'Marco Santos',
    vehicleType: 'motorcycle',
    pickup: {
      label: 'Ayala Triangle Gardens',
      address: 'Makati Ave, Makati',
      lat: 14.5568,
      lng: 121.0235,
    },
    destination: {
      label: 'SM Mall of Asia',
      address: 'Seaside Blvd, Pasay',
      lat: 14.5351,
      lng: 120.9822,
    },
    distanceKm: 6.4,
    durationMins: 19,
    fare: calculateByaheroFare(6.4, 'motorcycle', isTodaySaturday()),
    paymentMethodType: 'cash',
    paymentMethodLabel: 'Cash',
    status: 'searching',
    createdAt: new Date().toISOString(),
  },
  {
    id: 'req_102',
    passengerName: 'Claire Mendoza',
    vehicleType: 'taxi',
    pickup: {
      label: 'Bonifacio High Street',
      address: 'BGC, Taguig',
      lat: 14.5509,
      lng: 121.0503,
    },
    destination: {
      label: 'NAIA Terminal 3',
      address: 'Andrews Ave, Pasay',
      lat: 14.5194,
      lng: 121.0155,
    },
    distanceKm: 8.2,
    durationMins: 24,
    fare: calculateByaheroFare(8.2, 'taxi', isTodaySaturday()),
    paymentMethodType: 'gcash',
    paymentMethodLabel: 'GCash',
    status: 'searching',
    createdAt: new Date().toISOString(),
  },
];

const ORDER_HISTORY_STORAGE_KEY = 'byahero_order_history_v1';
const PAYMENT_METHODS_STORAGE_KEY = 'byahero_payment_methods_v1';
const FAVORITES_STORAGE_KEY = 'byahero_favorite_destinations_v1';
const PROMO_STORAGE_KEY = 'byahero_applied_promo_v1';

const DEFAULT_PAYMENT_METHODS: SavedPaymentMethod[] = [
  {
    id: 'pm_cash',
    type: 'cash',
    label: 'Cash',
    subtitle: 'Pay rider directly upon arrival',
    isDefault: true,
  },
];

const DEFAULT_FAVORITES: FavoriteDestination[] = [
  {
    id: 'fav_1',
    label: 'SM City Cebu',
    address: 'North Reclamation Area, Cebu City',
    lat: 10.3119,
    lng: 123.9182,
    ordersCount: 8,
  },
  {
    id: 'fav_2',
    label: 'IT Park, Lahug',
    address: 'Cebu IT Park, Apas, Cebu City',
    lat: 10.3304,
    lng: 123.9056,
    ordersCount: 6,
  },
  {
    id: 'fav_3',
    label: 'Ayala Center Cebu',
    address: 'Cebu Business Park, Cebu City',
    lat: 10.3181,
    lng: 123.9049,
    ordersCount: 4,
  },
];

const AVAILABLE_PROMO_CODES: PromoCodeItem[] = [
  {
    code: 'BYAHERO10',
    title: '10% Off Any Ride',
    description: 'Save 10% (up to P50) on any BYAHERO Moto or Cab trip.',
    discountPercent: 10,
    maxDiscountPhp: 50,
  },
  {
    code: 'SATPROMO',
    title: 'Saturday 10% Off + 5% Rider Bonus',
    description: 'Weekend special: 10% passenger discount while riders earn +5% extra.',
    discountPercent: 10,
    maxDiscountPhp: 80,
  },
  {
    code: 'FIRST50',
    title: '15% Welcome Ride Saver',
    description: 'Enjoy 15% off (up to P50) on your booked destination.',
    discountPercent: 15,
    maxDiscountPhp: 50,
  },
];

const SEED_ORDER_HISTORY: RideBooking[] = [
  {
    id: 'hist_001',
    passengerName: 'Alex Rivera',
    riderName: 'Kuya Carlo D.',
    riderPlate: 'MTO-8421',
    riderRating: 4.96,
    vehicleType: 'motorcycle',
    pickup: {
      label: 'IT Park, Lahug',
      address: 'Cebu IT Park, Cebu City',
      lat: 10.3304,
      lng: 123.9056,
    },
    destination: {
      label: 'SM City Cebu',
      address: 'North Reclamation Area, Cebu City',
      lat: 10.3119,
      lng: 123.9182,
    },
    distanceKm: 3.8,
    durationMins: 12,
    fare: calculateByaheroFare(3.8, 'motorcycle', false),
    paymentMethodType: 'cash',
    paymentMethodLabel: 'Cash',
    status: 'completed',
    roleContext: 'passenger',
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 26).toISOString(),
  },
  {
    id: 'hist_002',
    passengerName: 'Alex Rivera',
    riderName: 'Kuya Ramon V.',
    riderPlate: 'TXI-3902',
    riderRating: 4.94,
    vehicleType: 'taxi',
    pickup: {
      label: 'Ayala Center Cebu',
      address: 'Cebu Business Park, Cebu City',
      lat: 10.3181,
      lng: 123.9049,
    },
    destination: {
      label: 'Mactan Shrine Resort Strip',
      address: 'Lapu-Lapu City, Cebu',
      lat: 10.3106,
      lng: 124.0151,
    },
    distanceKm: 13.4,
    durationMins: 32,
    fare: calculateByaheroFare(13.4, 'taxi', true),
    paymentMethodType: 'gcash',
    paymentMethodLabel: 'GCash',
    status: 'completed',
    roleContext: 'passenger',
    createdAt: new Date(Date.now() - 1000 * 60 * 60 * 74).toISOString(),
  },
];

export default function App() {
  const [showSplash, setShowSplash] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [quotaExceeded, setQuotaExceeded] = useState(false);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [mobilePreviewFrame, setMobilePreviewFrame] = useState(false);
  const [mobileMapExpanded, setMobileMapExpanded] = useState(false);

  const [profile, setProfile] = useState<UserProfile>(INITIAL_PROFILE);
  const [activeTab, setActiveTab] = useState<AppTab>('ride');
  const [settingsSection, setSettingsSection] =
    useState<SettingsSection>('account');
  const [settingsShowMenuList, setSettingsShowMenuList] =
    useState<boolean>(true);

  const [userLocation, setUserLocation] = useState<LatLngPoint>(DEFAULT_CENTER);
  const [pickup, setPickup] = useState<LocationStop | null>({
    label: 'Current GPS Location',
    address: 'Live GPS Position',
    lat: DEFAULT_CENTER.lat,
    lng: DEFAULT_CENTER.lng,
  });
  const [destination, setDestination] = useState<LocationStop | null>(null);
  const [vehicleType, setVehicleType] = useState<VehicleType>('motorcycle');
  const [distanceKm, setDistanceKm] = useState<number>(0);
  const [durationMins, setDurationMins] = useState<number>(0);
  const [mapSelectionTarget, setMapSelectionTarget] =
    useState<MapSelectionTarget>(null);

  const actualSaturday = useMemo(() => isTodaySaturday(), []);
  const [saturdayPreview, setSaturdayPreview] = useState<boolean>(actualSaturday);
  const saturdayPromoActive = actualSaturday || saturdayPreview;

  const [discoveredPlaces, setDiscoveredPlaces] = useState<DiscoveredPlace[]>([]);
  const [discoveryLabel, setDiscoveryLabel] = useState<string>('Explore Nearby');
  const [selectedPlaceId, setSelectedPlaceId] = useState<string | null>(null);
  const [searchingPlaces, setSearchingPlaces] = useState<boolean>(false);
  const [placesError, setPlacesError] = useState<string | null>(null);
  const [triggerCategoryQuery, setTriggerCategoryQuery] = useState<{
    category: 'fast_food' | 'resort_beach';
    seq: number;
  } | null>(null);

  const [locationError, setLocationError] = useState<string | null>(null);
  const [activeRide, setActiveRide] = useState<RideBooking | null>(null);
  const [isLocatingGps, setIsLocatingGps] = useState<boolean>(false);
  const [isResolvingPickup, setIsResolvingPickup] = useState<boolean>(false);
  const [isResolvingDestination, setIsResolvingDestination] = useState<boolean>(false);
  const [isCalculatingRoute, setIsCalculatingRoute] = useState<boolean>(false);
  const [isBookingRide, setIsBookingRide] = useState<boolean>(false);

  const [paymentMethods, setPaymentMethods] = useState<SavedPaymentMethod[]>(() => {
    try {
      const saved = window.localStorage.getItem(PAYMENT_METHODS_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as SavedPaymentMethod[];
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch {
      return DEFAULT_PAYMENT_METHODS;
    }
    return DEFAULT_PAYMENT_METHODS;
  });

  const [selectedPaymentId, setSelectedPaymentId] = useState<string>(() => {
    const def = paymentMethods.find((m) => m.isDefault) || paymentMethods[0];
    return def ? def.id : 'pm_cash';
  });

  const [favorites, setFavorites] = useState<FavoriteDestination[]>(() => {
    try {
      const saved = window.localStorage.getItem(FAVORITES_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as FavoriteDestination[];
        if (Array.isArray(parsed)) return parsed;
      }
    } catch {
      return DEFAULT_FAVORITES;
    }
    return DEFAULT_FAVORITES;
  });

  const [appliedPromoCode, setAppliedPromoCode] = useState<string | null>(() => {
    try {
      return window.localStorage.getItem(PROMO_STORAGE_KEY) || null;
    } catch {
      return null;
    }
  });

  const [orderHistory, setOrderHistory] = useState<RideBooking[]>(() => {
    try {
      const saved = window.localStorage.getItem(ORDER_HISTORY_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as RideBooking[];
        if (Array.isArray(parsed)) return parsed;
      }
    } catch {
      return SEED_ORDER_HISTORY;
    }
    return SEED_ORDER_HISTORY;
  });

  const [incomingRequests, setIncomingRequests] = useState<RideBooking[]>(
    SEED_DISPATCH_REQUESTS
  );
  const [activeRiderJob, setActiveRiderJob] = useState<RideBooking | null>(null);

  useEffect(() => {
    try {
      window.localStorage.setItem(
        ORDER_HISTORY_STORAGE_KEY,
        JSON.stringify(orderHistory)
      );
    } catch {
      // storage unavailable
    }
  }, [orderHistory]);

  useEffect(() => {
    try {
      window.localStorage.setItem(
        PAYMENT_METHODS_STORAGE_KEY,
        JSON.stringify(paymentMethods)
      );
    } catch {
      // storage unavailable
    }
  }, [paymentMethods]);

  useEffect(() => {
    try {
      window.localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify(favorites));
    } catch {
      // storage unavailable
    }
  }, [favorites]);

  useEffect(() => {
    try {
      if (appliedPromoCode) {
        window.localStorage.setItem(PROMO_STORAGE_KEY, appliedPromoCode);
      } else {
        window.localStorage.removeItem(PROMO_STORAGE_KEY);
      }
    } catch {
      // storage unavailable
    }
  }, [appliedPromoCode]);

  useEffect(() => {
    if (!locationError) return;
    const timer = window.setTimeout(() => setLocationError(null), 4500);
    return () => window.clearTimeout(timer);
  }, [locationError]);

  const recordOrderInHistory = useCallback((entry: RideBooking) => {
    setOrderHistory((prev) => {
      const filtered = prev.filter((item) => item.id !== entry.id);
      return [entry, ...filtered];
    });
  }, []);

  useEffect(() => {
    const handler = () => setQuotaExceeded(true);
    window.addEventListener('gmp-quota-exceeded', handler);
    return () => window.removeEventListener('gmp-quota-exceeded', handler);
  }, []);

  useEffect(() => {
    let resolvedViaGps = false;

    const applyCoordinates = (next: LatLngPoint) => {
      setUserLocation(next);
      setIsLocatingGps(false);
      setIsResolvingPickup(true);
      void reverseGeocodePoint(next)
        .then((resolved) => {
          const candidate: LocationStop = {
            label: resolved.label,
            address: resolved.address,
            lat: next.lat,
            lng: next.lng,
          };
          setPickup((prev) => {
            if (
              !prev ||
              prev.label === 'Current GPS Location' ||
              prev.address === 'Live GPS Position'
            ) {
              return candidate;
            }
            return prev;
          });
        })
        .finally(() => {
          setIsResolvingPickup(false);
        });
    };

    const onPosition = (pos: GeolocationPosition) => {
      resolvedViaGps = true;
      applyCoordinates({
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
      });
    };

    const fallbackToApproximate = () => {
      if (resolvedViaGps) return;
      void detectApproximateBrowserLocation().then((approx) => {
        if (approx && !resolvedViaGps) {
          applyCoordinates(approx);
        }
      });
    };

    if (!('geolocation' in navigator)) {
      fallbackToApproximate();
      return;
    }

    navigator.geolocation.getCurrentPosition(onPosition, fallbackToApproximate, {
      enableHighAccuracy: true,
      timeout: 6000,
    });

    const watchId = navigator.geolocation.watchPosition(onPosition, () => {}, {
      enableHighAccuracy: true,
      maximumAge: 8000,
    });

    return () => navigator.geolocation.clearWatch(watchId);
  }, []);

  useEffect(() => {
    if (!supabase) return;
    supabase.auth.getSession().then(({data}) => {
      const user = data.session?.user;
      if (user) {
        setProfile((prev) => ({
          ...prev,
          id: user.id,
          email: user.email || prev.email,
          fullName:
            (user.user_metadata?.full_name as string) ||
            user.email?.split('@')[0] ||
            prev.fullName,
          avatarUrl: user.user_metadata?.avatar_url as string | undefined,
          authProvider: 'google',
        }));
      }
    });

    const {data: sub} = supabase.auth.onAuthStateChange((_event, session) => {
      const user = session?.user;
      if (user) {
        const nextProfile: UserProfile = {
          ...profile,
          id: user.id,
          email: user.email || profile.email,
          fullName:
            (user.user_metadata?.full_name as string) ||
            user.email?.split('@')[0] ||
            profile.fullName,
          avatarUrl: user.user_metadata?.avatar_url as string | undefined,
          authProvider: 'google',
        };
        setProfile(nextProfile);
        void syncProfileToSupabase(nextProfile);
      }
    });

    return () => {
      sub.subscription.unsubscribe();
    };
  }, []);

  const computedDistanceKm = useMemo(() => {
    if (distanceKm > 0) return distanceKm;
    if (pickup && destination) {
      return Number(
        Math.max(0.8, calculateHaversineKm(pickup, destination) * 1.25).toFixed(2)
      );
    }
    return 0;
  }, [distanceKm, pickup, destination]);

  const fare = useMemo(() => {
    if (!pickup || !destination || computedDistanceKm <= 0) return null;
    const base = calculateByaheroFare(
      computedDistanceKm,
      vehicleType,
      saturdayPromoActive
    );
    if (!appliedPromoCode) return base;
    const promo = AVAILABLE_PROMO_CODES.find((p) => p.code === appliedPromoCode);
    if (!promo || saturdayPromoActive) return base;
    const extraDiscount = Math.min(
      promo.maxDiscountPhp,
      Math.round(base.subtotal * (promo.discountPercent / 100) * 100) / 100
    );
    const nextFinal = Math.max(30, Math.round((base.subtotal - extraDiscount) * 100) / 100);
    return {
      ...base,
      isSaturdayPromo: true,
      passengerDiscount: extraDiscount,
      finalFare: nextFinal,
    };
  }, [
    pickup,
    destination,
    computedDistanceKm,
    vehicleType,
    saturdayPromoActive,
    appliedPromoCode,
  ]);

  const handleRouteCalculated = useCallback((km: number, mins: number) => {
    setDistanceKm(km);
    setDurationMins(mins);
  }, []);

  const trySetPickup = useCallback(
    (stop: LocationStop | null): boolean => {
      if (stop && destination && areLocationsSame(stop, destination)) {
        setLocationError('Pickup and destination cannot be the same location.');
        return false;
      }
      setLocationError(null);
      setPickup(stop);
      setDistanceKm(0);
      return true;
    },
    [destination]
  );

  const trySetDestination = useCallback(
    (stop: LocationStop | null): boolean => {
      if (stop && pickup && areLocationsSame(stop, pickup)) {
        setLocationError('Destination cannot be the same as your pickup location.');
        return false;
      }
      setLocationError(null);
      setDestination(stop);
      setDistanceKm(0);
      return true;
    },
    [pickup]
  );

  const handleSavePaymentMethod = useCallback((method: SavedPaymentMethod) => {
    setPaymentMethods((prev) => {
      const cleaned = prev.map((m) => ({...m, isDefault: false}));
      return [...cleaned, {...method, isDefault: true}];
    });
    setSelectedPaymentId(method.id);
  }, []);

  const handleSelectPaymentMethod = useCallback((id: string) => {
    setSelectedPaymentId(id);
    setPaymentMethods((prev) =>
      prev.map((m) => ({...m, isDefault: m.id === id}))
    );
  }, []);

  const handleRemovePaymentMethod = useCallback(
    (id: string) => {
      setPaymentMethods((prev) => {
        const remaining = prev.filter((m) => m.id !== id);
        if (remaining.length === 0) return DEFAULT_PAYMENT_METHODS;
        if (selectedPaymentId === id) {
          setSelectedPaymentId(remaining[0].id);
        }
        return remaining;
      });
    },
    [selectedPaymentId]
  );

  const handleAddFavoriteDestination = useCallback((stop: LocationStop) => {
    setFavorites((prev) => {
      const exists = prev.some((f) =>
        areLocationsSame(
          {label: f.label, address: f.address, lat: f.lat, lng: f.lng},
          stop
        )
      );
      if (exists) return prev;
      return [
        {
          id: `fav_${Date.now()}`,
          label: stop.label,
          address: stop.address,
          lat: stop.lat,
          lng: stop.lng,
          ordersCount: 1,
        },
        ...prev,
      ];
    });
  }, []);

  const handleMapTapSelect = useCallback(
    (coords: LatLngPoint) => {
      const target = mapSelectionTarget;
      const previousPickup = pickup;
      const previousDestination = destination;
      const loadingStop: LocationStop = {
        label: 'Resolving place name...',
        address: 'Fetching location name',
        lat: coords.lat,
        lng: coords.lng,
      };
      if (target === 'pickup') {
        setIsResolvingPickup(true);
        setPickup(loadingStop);
      } else if (target === 'destination') {
        setIsResolvingDestination(true);
        setDestination(loadingStop);
      }
      setMapSelectionTarget(null);
      setDistanceKm(0);

      void reverseGeocodePoint(coords)
        .then((resolved) => {
          const stop: LocationStop = {
            label: resolved.label,
            address: resolved.address,
            lat: coords.lat,
            lng: coords.lng,
          };
          if (target === 'pickup') {
            if (previousDestination && areLocationsSame(stop, previousDestination)) {
              setPickup(previousPickup);
              setLocationError('Pickup and destination cannot be the same location.');
              return;
            }
            setLocationError(null);
            setPickup(stop);
          } else if (target === 'destination') {
            if (previousPickup && areLocationsSame(stop, previousPickup)) {
              setDestination(previousDestination);
              setLocationError('Destination cannot be the same as your pickup location.');
              return;
            }
            setLocationError(null);
            setDestination(stop);
          }
        })
        .finally(() => {
          if (target === 'pickup') setIsResolvingPickup(false);
          if (target === 'destination') setIsResolvingDestination(false);
        });
    },
    [mapSelectionTarget, pickup, destination]
  );

  const handleUseCurrentGpsAsPickup = useCallback(() => {
    const previousPickup = pickup;
    setIsResolvingPickup(true);
    setPickup({
      label: 'Locating current address...',
      address: 'Resolving GPS address',
      lat: userLocation.lat,
      lng: userLocation.lng,
    });
    setDistanceKm(0);
    void reverseGeocodePoint(userLocation)
      .then((resolved) => {
        const gpsStop: LocationStop = {
          label: resolved.label,
          address: resolved.address,
          lat: userLocation.lat,
          lng: userLocation.lng,
        };
        if (destination && areLocationsSame(gpsStop, destination)) {
          setPickup(previousPickup);
          setLocationError('Pickup and destination cannot be the same location.');
          return;
        }
        setLocationError(null);
        setPickup(gpsStop);
      })
      .finally(() => {
        setIsResolvingPickup(false);
      });
  }, [userLocation, pickup, destination]);

  const handleSwapStops = useCallback(() => {
    if (pickup && destination && areLocationsSame(pickup, destination)) {
      setLocationError('Pickup and destination cannot be the same location.');
      return;
    }
    setLocationError(null);
    setPickup(destination);
    setDestination(pickup);
    setDistanceKm(0);
  }, [pickup, destination]);

  const handleBookRide = useCallback(() => {
    if (!pickup || !destination || !fare || isBookingRide) return;
    if (areLocationsSame(pickup, destination)) {
      setLocationError('Pickup and destination cannot be the same location.');
      return;
    }
    setLocationError(null);
    setIsBookingRide(true);
    const chosenPayment =
      paymentMethods.find((m) => m.id === selectedPaymentId) || paymentMethods[0];
    const newRide: RideBooking = {
      id: `ride_${Date.now()}`,
      passengerName: profile.fullName,
      vehicleType,
      pickup,
      destination,
      distanceKm: computedDistanceKm,
      durationMins: durationMins || Math.max(5, Math.round(computedDistanceKm * 3)),
      fare,
      paymentMethodType: chosenPayment?.type || 'cash',
      paymentMethodLabel: chosenPayment?.label || 'Cash',
      promoCode: appliedPromoCode || undefined,
      status: 'searching',
      roleContext: 'passenger',
      createdAt: new Date().toISOString(),
    };
    window.setTimeout(() => {
      setActiveRide(newRide);
      setFavorites((prev) =>
        prev.map((f) =>
          areLocationsSame(
            {label: f.label, address: f.address, lat: f.lat, lng: f.lng},
            destination
          )
            ? {...f, ordersCount: f.ordersCount + 1}
            : f
        )
      );
      setMobileMapExpanded(false);
      setIsBookingRide(false);
      void publishRideToSupabase(newRide, profile.id);
    }, 320);
  }, [
    pickup,
    destination,
    fare,
    isBookingRide,
    paymentMethods,
    selectedPaymentId,
    profile.fullName,
    profile.id,
    vehicleType,
    computedDistanceKm,
    durationMins,
    appliedPromoCode,
  ]);

  const handleAdvanceRideStatus = useCallback(() => {
    setActiveRide((prev) => {
      if (!prev) return null;
      if (prev.status === 'searching') {
        return {
          ...prev,
          status: 'accepted',
          riderName:
            prev.vehicleType === 'motorcycle' ? 'Kuya Carlo D.' : 'Kuya Ramon V.',
          riderPlate:
            prev.vehicleType === 'motorcycle' ? 'MTO-8421' : 'TXI-3902',
          riderRating: 4.96,
        };
      }
      if (prev.status === 'accepted') return {...prev, status: 'arriving'};
      if (prev.status === 'arriving') return {...prev, status: 'in_progress'};
      if (prev.status === 'in_progress') {
        const completed = {...prev, status: 'completed' as const};
        recordOrderInHistory(completed);
        return completed;
      }
      return prev;
    });
  }, [recordOrderInHistory]);

  const handleCancelActiveRide = useCallback(() => {
    setActiveRide((prev) => {
      if (prev && prev.status !== 'completed') {
        recordOrderInHistory({
          ...prev,
          status: 'cancelled',
        });
      }
      return null;
    });
  }, [recordOrderInHistory]);

  const handleRebookOrder = useCallback((order: RideBooking) => {
    if (areLocationsSame(order.pickup, order.destination)) {
      setLocationError('Pickup and destination cannot be the same location.');
      return;
    }
    setLocationError(null);
    setPickup(order.pickup);
    setDestination(order.destination);
    setVehicleType(order.vehicleType);
    setDistanceKm(0);
    setActiveRide(null);
    setActiveTab('ride');
    setMobileMapExpanded(false);
  }, []);

  const handleBookPlaceAsPickup = useCallback(
    (place: DiscoveredPlace) => {
      const candidate: LocationStop = {
        label: place.name,
        address: place.address,
        lat: place.lat,
        lng: place.lng,
        placeId: place.id,
      };
      trySetPickup(candidate);
      setActiveTab('ride');
      setMobileMapExpanded(false);
    },
    [trySetPickup]
  );

  const handleBookPlaceAsDestination = useCallback(
    (place: DiscoveredPlace) => {
      const candidate: LocationStop = {
        label: place.name,
        address: place.address,
        lat: place.lat,
        lng: place.lng,
        placeId: place.id,
      };
      trySetDestination(candidate);
      setActiveTab('ride');
      setMobileMapExpanded(false);
    },
    [trySetDestination]
  );

  const handleDiscoverCategory = useCallback(
    (category: 'fast_food' | 'resort_beach') => {
      setTriggerCategoryQuery((prev) => ({
        category,
        seq: (prev?.seq ?? 0) + 1,
      }));
    },
    []
  );

  const handleDiscoveredPlacesChange = useCallback(
    (places: DiscoveredPlace[], categoryLabel: string) => {
      setDiscoveredPlaces(places);
      setDiscoveryLabel(categoryLabel);
      if (places.length > 0) {
        setSelectedPlaceId(places[0].id);
      } else {
        setSelectedPlaceId(null);
      }
    },
    []
  );

  const handleSwitchRole = useCallback(
    (nextRole: UserRole) => {
      const nextMode: ActiveMode =
        nextRole === 'rider'
          ? 'rider'
          : nextRole === 'passenger'
          ? 'passenger'
          : profile.activeMode;

      setProfile((prev) => ({
        ...prev,
        role: nextRole,
        activeMode: nextMode,
      }));
      if (nextMode === 'rider') setActiveTab('rider');
      if (nextMode === 'passenger' && activeTab === 'rider') setActiveTab('ride');
      void updateRoleInSupabase(profile.id, nextRole, nextMode);
    },
    [profile.id, profile.activeMode, activeTab]
  );

  const handleSwitchActiveMode = useCallback(
    (nextMode: ActiveMode) => {
      setProfile((prev) => ({...prev, activeMode: nextMode}));
      setActiveTab(nextMode === 'rider' ? 'rider' : 'ride');
      void updateRoleInSupabase(profile.id, profile.role, nextMode);
    },
    [profile.id, profile.role]
  );

  const handleAcceptRideRequest = useCallback(
    (req: RideBooking) => {
      setIncomingRequests((prev) => prev.filter((r) => r.id !== req.id));
      setActiveRiderJob({
        ...req,
        status: 'accepted',
        riderName: profile.fullName,
        riderPlate: profile.plateNumber,
      });
      setPickup(req.pickup);
      setDestination(req.destination);
      setVehicleType(req.vehicleType);
    },
    [profile.fullName, profile.plateNumber]
  );

  const handleCompleteRiderJob = useCallback(() => {
    if (!activeRiderJob) return;
    recordOrderInHistory({
      ...activeRiderJob,
      status: 'completed',
      roleContext: 'rider',
      createdAt: new Date().toISOString(),
    });
    setProfile((prev) => {
      const updated = {
        ...prev,
        totalRides: prev.totalRides + 1,
        riderEarningsPhp:
          Math.round(
            (prev.riderEarningsPhp + activeRiderJob.fare.riderTotalPayout) * 100
          ) / 100,
        riderIncentivesPhp:
          Math.round(
            (prev.riderIncentivesPhp + activeRiderJob.fare.riderIncentive) * 100
          ) / 100,
      };
      void syncProfileToSupabase(updated);
      return updated;
    });
    setActiveRiderJob(null);
  }, [activeRiderJob, recordOrderInHistory]);

  const firstName = profile.fullName.split(' ')[0] || 'User';

  const shellContent = (
    <div
      className={`relative flex flex-col lg:flex-row text-[#101713] overflow-hidden ${
        mobilePreviewFrame
          ? 'w-[390px] h-[820px] max-h-[92dvh] rounded-[40px] border-[6px] border-[#101713] shadow-2xl mx-auto my-auto !flex-col'
          : 'w-full h-[100dvh]'
      }`}
      style={{
        background:
          'radial-gradient(circle at 12% 10%, rgba(15, 82, 43, 0.09) 0%, transparent 40%), radial-gradient(circle at 88% 85%, rgba(16, 185, 129, 0.10) 0%, transparent 45%), #F4F7F5',
      }}
    >
      <header
        className={`shrink-0 h-14 px-4 bg-white/80 backdrop-blur-xl border-b border-[#0F522B]/10 flex items-center justify-between gap-3 z-30 ${
          mobilePreviewFrame ? '' : 'lg:hidden'
        }`}
      >
        <div className="flex items-center gap-2.5 min-w-0">
          <ByaheroLogoMark className="w-7 h-7 shrink-0" />
          <ByaheroWordmarkVector lightSurface={true} className="h-5 w-auto" />
        </div>

        <button
          type="button"
          onClick={() => {
            setSettingsSection('account');
            setSettingsShowMenuList(false);
            setActiveTab('settings');
            setMobileMapExpanded(false);
          }}
          title={profile.fullName}
          className="w-10 h-10 rounded-full bg-[#0F522B] hover:bg-[#146C3A] text-white text-[11px] font-bold flex items-center justify-center shadow-sm transition-colors shrink-0 overflow-hidden px-1"
        >
          <span className="truncate">{firstName}</span>
        </button>
      </header>

      <aside
        className={`order-2 ${
          activeTab === 'settings'
            ? 'w-full flex-1 h-full flex flex-col bg-white/70 backdrop-blur-2xl z-20'
            : mobilePreviewFrame
            ? 'w-full flex-none flex flex-col bg-white/70 backdrop-blur-2xl border-t border-white z-20'
            : 'lg:order-1 lg:w-[440px] xl:w-[470px] lg:h-full lg:border-r lg:border-t-0 lg:flex-none flex flex-col bg-white/70 backdrop-blur-2xl border-t border-white z-20'
        }`}
      >
        <div
          className={`hidden ${
            mobilePreviewFrame ? '' : 'lg:flex'
          } items-center justify-between px-5 py-4 border-b border-[#0F522B]/10 shrink-0`}
        >
          <div className="flex items-center gap-3">
            <ByaheroLogoMark className="w-9 h-9" />
            <div>
              <ByaheroWordmarkVector lightSurface={true} className="h-6 w-auto" />
              <div className="text-[11px] text-[#0F522B] font-semibold mt-1">
                by HDDEV · Travel & Explore
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2.5">
            <button
              type="button"
              onClick={() => setMobilePreviewFrame((v) => !v)}
              title="Toggle Phone Screen Preview"
              aria-label="Toggle phone screen preview"
              className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl bg-white/80 hover:bg-white border border-[#0F522B]/15 text-[#101713] transition-colors"
            >
              <Smartphone className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={() => {
                setSettingsSection('account');
                setSettingsShowMenuList(false);
                setActiveTab('settings');
              }}
              title={profile.fullName}
              className="w-10 h-10 rounded-full bg-[#0F522B] hover:bg-[#146C3A] text-white text-[11px] font-bold flex items-center justify-center shadow-sm transition-colors shrink-0 overflow-hidden px-1"
            >
              <span className="truncate">{firstName}</span>
            </button>
          </div>
        </div>

        <div
          className={`${
            activeTab === 'settings'
              ? 'hidden'
              : `flex items-center justify-between px-4 py-2 border-b border-[#0F522B]/10 ${
                  mobilePreviewFrame ? '' : 'lg:hidden'
                }`
          }`}
        >
          <span className="text-xs font-bold text-[#0F522B]">
            {activeTab === 'ride'
              ? 'Book Motorcycle or Taxi'
              : activeTab === 'explore'
              ? 'Fast Food & Resorts Nearby'
              : activeTab === 'history'
              ? 'Order History'
              : activeTab === 'rider'
              ? 'Rider Dispatch (+5% Bonus)'
              : 'App Settings'}
          </span>
          <button
            type="button"
            onClick={() => setMobileMapExpanded((v) => !v)}
            className="min-h-[32px] px-2.5 rounded-lg bg-[#EBF1ED] text-[11px] font-semibold text-[#0F522B] flex items-center gap-1"
          >
            {mobileMapExpanded ? (
              <>
                <ChevronUp className="w-3.5 h-3.5" />
                <span>Show Panel</span>
              </>
            ) : (
              <>
                <ChevronDown className="w-3.5 h-3.5" />
                <span>Full Map</span>
              </>
            )}
          </button>
        </div>

        <nav
          aria-label="Primary navigation"
          className={`hidden ${
            mobilePreviewFrame ? '' : 'lg:grid'
          } grid-cols-5 gap-1 p-1.5 mx-4 mt-3 rounded-2xl bg-[#EBF1ED]/90 border border-[#0F522B]/10 shrink-0 ${
            activeTab === 'settings' ? 'max-w-2xl w-full lg:mx-auto' : ''
          }`}
        >
          {([
            {id: 'ride', label: 'Ride', icon: Navigation},
            {id: 'explore', label: 'Explore', icon: Compass},
            {id: 'history', label: 'History', icon: History},
            {id: 'rider', label: 'Rider', icon: Bike},
            {id: 'settings', label: 'Settings', icon: Settings},
          ] as const).map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
                <button
                key={tab.id}
                type="button"
                onClick={() => {
                  setActiveTab(tab.id);
                  if (tab.id === 'settings') {
                    setSettingsShowMenuList(true);
                  }
                }}
                className={`relative h-10 rounded-xl text-[11px] font-bold flex items-center justify-center gap-1 transition-colors whitespace-nowrap smooth-press ${
                  active ? 'text-white' : 'text-[#55625A] hover:text-[#101713]'
                }`}
              >
                {active && (
                  <motion.span
                    layoutId="desktop-nav-active"
                    transition={{type: 'spring', stiffness: 420, damping: 32}}
                    className="absolute inset-0 rounded-xl bg-[#0F522B] shadow-sm"
                  />
                )}
                <Icon className="relative z-10 w-3.5 h-3.5 shrink-0" />
                <span className="relative z-10">{tab.label}</span>
              </button>
            );
          })}
        </nav>

        <div
          className={`overflow-y-auto px-4 py-4 transition-all ${
            activeTab === 'settings'
              ? 'flex-1 min-h-0'
              : mobileMapExpanded
              ? 'hidden'
              : mobilePreviewFrame
              ? 'h-[350px]'
              : 'max-h-[42dvh] lg:max-h-none lg:flex-1'
          }`}
        >
          <AnimatePresence mode="wait" initial={false}>
            <motion.div
              key={activeTab}
              initial={{opacity: 0, y: 8}}
              animate={{opacity: 1, y: 0}}
              exit={{opacity: 0, y: -6}}
              transition={{duration: 0.18, ease: [0.16, 1, 0.3, 1]}}
            >
              {activeTab === 'ride' && (
                <BookingPanel
                  userLocation={userLocation}
                  pickup={pickup}
                  destination={destination}
                  vehicleType={vehicleType}
                  distanceKm={computedDistanceKm}
                  durationMins={durationMins}
                  fare={fare}
                  saturdayPromoActive={saturdayPromoActive}
                  isActualSaturday={actualSaturday}
                  activeRide={activeRide}
                  locationError={locationError}
                  paymentMethods={paymentMethods}
                  selectedPaymentId={selectedPaymentId}
                  favorites={favorites}
                  appliedPromoCode={appliedPromoCode}
                  isResolvingPickup={isResolvingPickup}
                  isResolvingDestination={isResolvingDestination}
                  isCalculatingRoute={isCalculatingRoute}
                  isBookingRide={isBookingRide}
                  onSelectPaymentMethod={handleSelectPaymentMethod}
                  onSavePaymentMethod={handleSavePaymentMethod}
                  onAddFavoriteDestination={handleAddFavoriteDestination}
                  onOpenSettingsSection={(sec) => {
                    setSettingsSection(sec);
                    setSettingsShowMenuList(false);
                    setActiveTab('settings');
                  }}
                  onSetPickup={trySetPickup}
                  onSetDestination={trySetDestination}
                  onSwapStops={handleSwapStops}
                  onSelectVehicle={setVehicleType}
                  onToggleSaturdayPreview={() => setSaturdayPreview((v) => !v)}
                  onStartMapPick={(target) => {
                    setMapSelectionTarget(target);
                    setMobileMapExpanded(true);
                  }}
                  onUseCurrentGpsAsPickup={handleUseCurrentGpsAsPickup}
                  onBookRide={handleBookRide}
                  onAdvanceRideStatus={handleAdvanceRideStatus}
                  onCancelRide={handleCancelActiveRide}
                />
              )}

              {activeTab === 'explore' && (
                <ExploreNearbyPanel
                  places={discoveredPlaces}
                  categoryLabel={discoveryLabel}
                  isLoading={searchingPlaces}
                  error={placesError}
                  selectedPlaceId={selectedPlaceId}
                  onDiscoverCategory={handleDiscoverCategory}
                  onSelectPlace={(p) => {
                    setSelectedPlaceId(p.id);
                    setMobileMapExpanded(true);
                  }}
                  onBookRideAsPickup={handleBookPlaceAsPickup}
                  onBookRideToPlace={handleBookPlaceAsDestination}
                />
              )}

              {activeTab === 'history' && (
                <OrderHistoryPanel
                  orders={orderHistory}
                  onRebookOrder={handleRebookOrder}
                />
              )}

              {activeTab === 'rider' && (
                <RiderDashboardPanel
                  profile={profile}
                  incomingRequests={incomingRequests}
                  activeRiderJob={activeRiderJob}
                  onEnableRiderRole={() => handleSwitchRole('both')}
                  onToggleOnline={() =>
                    setProfile((prev) => ({...prev, isOnline: !prev.isOnline}))
                  }
                  onAcceptRideRequest={handleAcceptRideRequest}
                  onCompleteRiderJob={handleCompleteRiderJob}
                />
              )}

              {activeTab === 'settings' && (
                <SettingsPanel
                  profile={profile}
                  activeSection={settingsSection}
                  showMenuList={settingsShowMenuList}
                  favorites={favorites}
                  paymentMethods={paymentMethods}
                  selectedPaymentId={selectedPaymentId}
                  promoCodes={AVAILABLE_PROMO_CODES}
                  appliedPromoCode={appliedPromoCode}
                  onSelectSection={(sec) => {
                    setSettingsSection(sec);
                    setSettingsShowMenuList(false);
                  }}
                  onBackToMenu={() => setSettingsShowMenuList(true)}
                  onOpenAuthModal={() => setAuthModalOpen(true)}
                  onSwitchRole={handleSwitchRole}
                  onSwitchActiveMode={handleSwitchActiveMode}
                  onSelectFavoriteAsDestination={(stop) => {
                    if (trySetDestination(stop)) {
                      setActiveTab('ride');
                      setMobileMapExpanded(false);
                    }
                  }}
                  onAddFavorite={(fav) => setFavorites((prev) => [fav, ...prev])}
                  onRemoveFavorite={(id) =>
                    setFavorites((prev) => prev.filter((f) => f.id !== id))
                  }
                  onSelectPaymentMethod={handleSelectPaymentMethod}
                  onSavePaymentMethod={handleSavePaymentMethod}
                  onRemovePaymentMethod={handleRemovePaymentMethod}
                  onApplyPromoCode={setAppliedPromoCode}
                  onLogout={() => {
                    setIsAuthenticated(false);
                    setActiveTab('ride');
                    setActiveRide(null);
                    setActiveRiderJob(null);
                  }}
                />
              )}
            </motion.div>
          </AnimatePresence>
        </div>

        <div
          className={`px-3 pt-1 pb-2.5 pb-safe shrink-0 ${
            mobilePreviewFrame ? '' : 'lg:hidden'
          }`}
        >
          <nav
            aria-label="Mobile bottom navigation"
            className="grid grid-cols-5 items-center h-14 rounded-2xl glass-green text-white px-1.5"
          >
            {([
              {id: 'ride', label: 'Ride', icon: Navigation},
              {id: 'explore', label: 'Explore', icon: Compass},
              {id: 'history', label: 'History', icon: History},
              {id: 'rider', label: 'Rider', icon: Bike},
              {id: 'settings', label: 'Settings', icon: Settings},
            ] as const).map((tab) => {
              const Icon = tab.icon;
              const active = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => {
                    setActiveTab(tab.id);
                    if (tab.id === 'settings') {
                      setSettingsShowMenuList(true);
                    }
                    setMobileMapExpanded(false);
                  }}
                  className={`relative h-10 rounded-xl flex flex-col items-center justify-center gap-0.5 transition-colors smooth-press ${
                    active ? 'text-white' : 'text-white/70 hover:text-white'
                  }`}
                >
                  {active && (
                    <motion.span
                      layoutId="mobile-nav-active"
                      transition={{type: 'spring', stiffness: 420, damping: 32}}
                      className="absolute inset-0 rounded-xl bg-white/20"
                    />
                  )}
                  <Icon className="relative z-10 w-4 h-4" />
                  <span className="relative z-10 text-[10px] font-semibold tracking-tight">
                    {tab.label}
                  </span>
                </button>
              );
            })}
          </nav>
        </div>
      </aside>

      <main
        className={`order-1 flex-1 min-h-0 relative ${
          activeTab === 'settings'
            ? 'hidden'
            : mobilePreviewFrame
            ? 'w-full'
            : 'lg:order-2 lg:h-full'
        }`}
      >
        <AnimatePresence>
          {locationError && (
            <motion.div
              initial={{opacity: 0, y: 12}}
              animate={{opacity: 1, y: 0}}
              exit={{opacity: 0, y: 12}}
              transition={{duration: 0.2, ease: [0.16, 1, 0.3, 1]}}
              className="pointer-events-none absolute bottom-3 left-3 right-3 z-30 flex justify-center"
            >
              <div className="pointer-events-auto flex items-center gap-2 px-3.5 py-2 rounded-xl bg-red-600 text-white text-xs font-semibold shadow-xl">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{locationError}</span>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
        <GoogleMapsWorkspace
          userLocation={userLocation}
          pickup={pickup}
          destination={destination}
          vehicleType={vehicleType}
          discoveredPlaces={discoveredPlaces}
          selectedPlaceId={selectedPlaceId}
          mapSelectionTarget={mapSelectionTarget}
          isSearchingPlaces={searchingPlaces}
          isLocatingGps={isLocatingGps}
          isCalculatingRoute={isCalculatingRoute}
          onCalculatingRouteChange={setIsCalculatingRoute}
          onMapTapSelect={handleMapTapSelect}
          onRouteCalculated={handleRouteCalculated}
          onDiscoveredPlacesChange={handleDiscoveredPlacesChange}
          onSelectDiscoveredPlace={(p) => setSelectedPlaceId(p.id || null)}
          onBookPlaceAsPickup={handleBookPlaceAsPickup}
          onBookPlaceAsDestination={handleBookPlaceAsDestination}
          onRecenterUser={() => {
            if ('geolocation' in navigator) {
              setIsLocatingGps(true);
              const fallbackTimer = window.setTimeout(() => {
                setIsLocatingGps(false);
              }, 5000);
              navigator.geolocation.getCurrentPosition(
                (pos) => {
                  window.clearTimeout(fallbackTimer);
                  setUserLocation({
                    lat: pos.coords.latitude,
                    lng: pos.coords.longitude,
                  });
                  setIsLocatingGps(false);
                },
                () => {
                  window.clearTimeout(fallbackTimer);
                  setIsLocatingGps(false);
                },
                {enableHighAccuracy: true, timeout: 4500}
              );
            }
          }}
          onCancelMapTap={() => setMapSelectionTarget(null)}
          onSearchingPlacesChange={(loading, err) => {
            setSearchingPlaces(loading);
            setPlacesError(err);
          }}
          triggerCategoryQuery={triggerCategoryQuery}
        />
      </main>
    </div>
  );

  return (
    <MotionConfig reducedMotion="user">
      <APIProvider
        apiKey={MAPS_API_KEY}
        libraries={['places', 'routes', 'marker', 'geometry']}
        language="en"
      >
        <SplashIntro visible={showSplash} onFinish={() => setShowSplash(false)} />

        {quotaExceeded && (
          <div className="bg-amber-50 border-b border-amber-200 text-amber-900 px-4 py-2.5 text-xs md:text-sm text-center sticky top-0 z-50 shadow-sm">
            <span>
              Google Maps Platform quota reached. If you are the app owner, visit{' '}
              <a
                href="https://developers.google.com/maps/ai/ai-studio?utm_campaign=gmp_mcp_codeassist_v1_aistudio#quota_exceeded_errors"
                target="_blank"
                rel="noopener noreferrer"
                className="underline font-semibold text-amber-950 hover:text-amber-800"
              >
                maps developer site
              </a>{' '}
              for instructions to update your account.
            </span>
          </div>
        )}

        <AnimatePresence mode="wait">
          {!isAuthenticated ? (
            <motion.div
              key="auth-gate"
              initial={{opacity: 0}}
              animate={{opacity: 1}}
              exit={{opacity: 0}}
              transition={{duration: 0.22}}
            >
              <AuthGateScreen
                onAuthenticated={(nextProfile) => {
                  setProfile(nextProfile);
                  setActiveTab(nextProfile.activeMode === 'rider' ? 'rider' : 'ride');
                  setIsAuthenticated(true);
                  void syncProfileToSupabase(nextProfile);
                }}
              />
            </motion.div>
          ) : mobilePreviewFrame ? (
            <motion.div
              key="mobile-preview-shell"
              initial={{opacity: 0}}
              animate={{opacity: 1}}
              exit={{opacity: 0}}
              transition={{duration: 0.22}}
              className="min-h-[100dvh] w-full bg-[#E8EFEA] flex flex-col items-center justify-center p-4 gap-3"
            >
              <div className="flex items-center gap-3 text-xs text-[#55625A]">
                <span>Mobile Phone Viewport Preview (390 x 820)</span>
                <button
                  type="button"
                  onClick={() => setMobilePreviewFrame(false)}
                  className="px-3 py-1.5 rounded-lg bg-[#0F522B] text-white font-semibold hover:bg-[#146C3A] smooth-press"
                >
                  Exit Phone Preview
                </button>
              </div>
              {shellContent}
            </motion.div>
          ) : (
            <motion.div
              key="main-shell"
              initial={{opacity: 0}}
              animate={{opacity: 1}}
              exit={{opacity: 0}}
              transition={{duration: 0.22}}
              className="w-full h-[100dvh]"
            >
              {shellContent}
            </motion.div>
          )}
        </AnimatePresence>

        <AuthModal
          open={authModalOpen}
          currentProfile={profile}
          onClose={() => setAuthModalOpen(false)}
          onAuthenticate={(nextProfile) => {
            setProfile(nextProfile);
            setIsAuthenticated(true);
            if (nextProfile.activeMode === 'rider') setActiveTab('rider');
            void syncProfileToSupabase(nextProfile);
          }}
        />
      </APIProvider>
    </MotionConfig>
  );
}
