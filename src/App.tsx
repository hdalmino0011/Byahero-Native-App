import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {APIProvider} from '@vis.gl/react-google-maps';
import {
  Bike,
  ChevronDown,
  ChevronUp,
  Compass,
  Navigation,
  Smartphone,
  User,
} from 'lucide-react';
import {
  ActiveMode,
  AppTab,
  DiscoveredPlace,
  LatLngPoint,
  LocationStop,
  MapSelectionTarget,
  RideBooking,
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
import {RiderDashboardPanel} from './components/RiderDashboardPanel';
import {AccountPanel} from './components/AccountPanel';
import {ByaheroLogoMark} from './components/VectorGraphics';

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
    status: 'searching',
    createdAt: new Date().toISOString(),
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

  // Location & Route State
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

  // Saturday Pricing
  const actualSaturday = useMemo(() => isTodaySaturday(), []);
  const [saturdayPreview, setSaturdayPreview] = useState<boolean>(actualSaturday);
  const saturdayPromoActive = actualSaturday || saturdayPreview;

  // Discovery Places State
  const [discoveredPlaces, setDiscoveredPlaces] = useState<DiscoveredPlace[]>([]);
  const [discoveryLabel, setDiscoveryLabel] = useState<string>('Explore Nearby');
  const [selectedPlaceId, setSelectedPlaceId] = useState<string | null>(null);
  const [searchingPlaces, setSearchingPlaces] = useState<boolean>(false);
  const [placesError, setPlacesError] = useState<string | null>(null);
  const [triggerCategoryQuery, setTriggerCategoryQuery] = useState<{
    category: 'fast_food' | 'resort_beach';
    seq: number;
  } | null>(null);

  // Booking & Rider Dispatch State
  const [activeRide, setActiveRide] = useState<RideBooking | null>(null);
  const [incomingRequests, setIncomingRequests] = useState<RideBooking[]>(
    SEED_DISPATCH_REQUESTS
  );
  const [activeRiderJob, setActiveRiderJob] = useState<RideBooking | null>(null);

  // Tier 1 & 2 Quota Listener
  useEffect(() => {
    const handler = () => setQuotaExceeded(true);
    window.addEventListener('gmp-quota-exceeded', handler);
    return () => window.removeEventListener('gmp-quota-exceeded', handler);
  }, []);

  // Real-time Browser Geolocation Tracking
  useEffect(() => {
    if (!('geolocation' in navigator)) return;

    const onPosition = (pos: GeolocationPosition) => {
      const next: LatLngPoint = {
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
      };
      setUserLocation(next);
      setPickup((prev) => {
        if (!prev || prev.label === 'Current GPS Location') {
          return {
            label: 'Current GPS Location',
            address: `${next.lat.toFixed(4)}, ${next.lng.toFixed(4)}`,
            lat: next.lat,
            lng: next.lng,
          };
        }
        return prev;
      });
    };

    navigator.geolocation.getCurrentPosition(onPosition, () => {}, {
      enableHighAccuracy: true,
      timeout: 8000,
    });

    const watchId = navigator.geolocation.watchPosition(onPosition, () => {}, {
      enableHighAccuracy: true,
      maximumAge: 10000,
    });

    return () => navigator.geolocation.clearWatch(watchId);
  }, []);

  // Supabase Auth Session Sync (when configured)
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
      return Number(Math.max(0.8, calculateHaversineKm(pickup, destination) * 1.25).toFixed(2));
    }
    return 0;
  }, [distanceKm, pickup, destination]);

  const fare = useMemo(() => {
    if (!pickup || !destination || computedDistanceKm <= 0) return null;
    return calculateByaheroFare(computedDistanceKm, vehicleType, saturdayPromoActive);
  }, [pickup, destination, computedDistanceKm, vehicleType, saturdayPromoActive]);

  const handleRouteCalculated = useCallback((km: number, mins: number) => {
    setDistanceKm(km);
    setDurationMins(mins);
  }, []);

  const handleMapTapSelect = useCallback(
    (coords: LatLngPoint) => {
      const stop: LocationStop = {
        label: `Pinned (${coords.lat.toFixed(4)}, ${coords.lng.toFixed(4)})`,
        address: `${coords.lat.toFixed(5)}, ${coords.lng.toFixed(5)}`,
        lat: coords.lat,
        lng: coords.lng,
      };
      if (mapSelectionTarget === 'pickup') {
        setPickup(stop);
      } else if (mapSelectionTarget === 'destination') {
        setDestination(stop);
      }
      setMapSelectionTarget(null);
      setDistanceKm(0);
    },
    [mapSelectionTarget]
  );

  const handleUseCurrentGpsAsPickup = useCallback(() => {
    setPickup({
      label: 'Current GPS Location',
      address: `${userLocation.lat.toFixed(4)}, ${userLocation.lng.toFixed(4)}`,
      lat: userLocation.lat,
      lng: userLocation.lng,
    });
    setDistanceKm(0);
  }, [userLocation]);

  const handleSwapStops = useCallback(() => {
    setPickup(destination);
    setDestination(pickup);
    setDistanceKm(0);
  }, [pickup, destination]);

  const handleBookRide = useCallback(() => {
    if (!pickup || !destination || !fare) return;
    const newRide: RideBooking = {
      id: `ride_${Date.now()}`,
      passengerName: profile.fullName,
      vehicleType,
      pickup,
      destination,
      distanceKm: computedDistanceKm,
      durationMins: durationMins || Math.max(5, Math.round(computedDistanceKm * 3)),
      fare,
      status: 'searching',
      createdAt: new Date().toISOString(),
    };
    setActiveRide(newRide);
    setMobileMapExpanded(false);
    void publishRideToSupabase(newRide, profile.id);
  }, [pickup, destination, fare, profile.fullName, profile.id, vehicleType, computedDistanceKm, durationMins]);

  const handleAdvanceRideStatus = useCallback(() => {
    setActiveRide((prev) => {
      if (!prev) return null;
      if (prev.status === 'searching') {
        return {
          ...prev,
          status: 'accepted',
          riderName: prev.vehicleType === 'motorcycle' ? 'Kuya Carlo D.' : 'Kuya Ramon V.',
          riderPlate: prev.vehicleType === 'motorcycle' ? 'MTO-8421' : 'TXI-3902',
          riderRating: 4.96,
        };
      }
      if (prev.status === 'accepted') return {...prev, status: 'arriving'};
      if (prev.status === 'arriving') return {...prev, status: 'in_progress'};
      if (prev.status === 'in_progress') return {...prev, status: 'completed'};
      return prev;
    });
  }, []);

  const handleBookPlaceAsDestination = useCallback((place: DiscoveredPlace) => {
    setDestination({
      label: place.name,
      address: place.address,
      lat: place.lat,
      lng: place.lng,
      placeId: place.id,
    });
    setDistanceKm(0);
    setActiveTab('ride');
    setMobileMapExpanded(false);
  }, []);

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
    setProfile((prev) => {
      const updated = {
        ...prev,
        totalRides: prev.totalRides + 1,
        riderEarningsPhp:
          Math.round((prev.riderEarningsPhp + activeRiderJob.fare.riderTotalPayout) * 100) /
          100,
        riderIncentivesPhp:
          Math.round((prev.riderIncentivesPhp + activeRiderJob.fare.riderIncentive) * 100) /
          100,
      };
      void syncProfileToSupabase(updated);
      return updated;
    });
    setActiveRiderJob(null);
  }, [activeRiderJob]);

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
      {/* Top Bar (Mobile) */}
      <header
        className={`shrink-0 h-14 px-4 bg-white/80 backdrop-blur-xl border-b border-[#0F522B]/10 flex items-center justify-between gap-3 z-30 ${
          mobilePreviewFrame ? '' : 'lg:hidden'
        }`}
      >
        <div className="flex items-center gap-2.5 min-w-0">
          <ByaheroLogoMark className="w-7 h-7 shrink-0" />
          <span className="text-sm font-extrabold tracking-wider text-[#101713] whitespace-nowrap">
            BYAHERO
          </span>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {profile.role === 'both' && (
            <button
              type="button"
              onClick={() =>
                handleSwitchActiveMode(
                  profile.activeMode === 'passenger' ? 'rider' : 'passenger'
                )
              }
              className="h-8 px-3 rounded-xl bg-[#0F522B] text-[11px] font-bold text-white whitespace-nowrap shadow-sm"
            >
              {profile.activeMode === 'passenger' ? 'Passenger' : 'Rider Mode'}
            </button>
          )}
          <button
            type="button"
            onClick={() => setAuthModalOpen(true)}
            className="h-8 px-3 rounded-xl bg-white border border-[#0F522B]/20 text-[#101713] text-[11px] font-bold whitespace-nowrap shadow-xs"
          >
            {profile.fullName.split(' ')[0]}
          </button>
        </div>
      </header>

      {/* Left Control Column on Desktop / Frosted Sheet + Docked Green Nav on Mobile */}
      <aside
        className={`order-2 ${
          mobilePreviewFrame
            ? 'w-full flex-none flex flex-col bg-white/70 backdrop-blur-2xl border-t border-white z-20'
            : 'lg:order-1 lg:w-[440px] xl:w-[470px] lg:h-full lg:border-r lg:border-t-0 lg:flex-none flex flex-col bg-white/70 backdrop-blur-2xl border-t border-white z-20'
        }`}
      >
        {/* Desktop Header */}
        <div
          className={`hidden ${
            mobilePreviewFrame ? '' : 'lg:flex'
          } items-center justify-between px-5 py-4 border-b border-[#0F522B]/10 shrink-0`}
        >
          <div className="flex items-center gap-3">
            <ByaheroLogoMark className="w-9 h-9" />
            <div>
              <div className="text-base font-extrabold tracking-[0.1em] text-[#101713] leading-none">
                BYAHERO
              </div>
              <div className="text-[11px] text-[#0F522B] font-semibold mt-1">
                by HDDEV · Travel & Explore
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
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
              onClick={() => setAuthModalOpen(true)}
              className="h-10 px-3.5 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-bold whitespace-nowrap shadow-sm transition-colors"
            >
              {profile.fullName.split(' ')[0]}
            </button>
          </div>
        </div>

        {/* Mobile Sheet Expand / Collapse Handle */}
        <div
          className={`flex items-center justify-between px-4 py-2 border-b border-[#0F522B]/10 ${
            mobilePreviewFrame ? '' : 'lg:hidden'
          }`}
        >
          <span className="text-xs font-bold text-[#0F522B]">
            {activeTab === 'ride'
              ? 'Book Motorcycle or Taxi'
              : activeTab === 'explore'
              ? 'Fast Food & Resorts Nearby'
              : activeTab === 'rider'
              ? 'Rider Dispatch (+5% Bonus)'
              : 'Account & Role'}
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

        {/* Desktop Navigation Tabs */}
        <nav
          aria-label="Primary navigation"
          className={`hidden ${
            mobilePreviewFrame ? '' : 'lg:grid'
          } grid-cols-4 gap-1 p-1.5 mx-4 mt-3 rounded-2xl bg-[#EBF1ED]/90 border border-[#0F522B]/10 shrink-0`}
        >
          {([
            {id: 'ride', label: 'Ride', icon: Navigation},
            {id: 'explore', label: 'Explore', icon: Compass},
            {id: 'rider', label: 'Rider', icon: Bike},
            {id: 'account', label: 'Account', icon: User},
          ] as const).map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                className={`h-10 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all whitespace-nowrap ${
                  active
                    ? 'bg-[#0F522B] text-white shadow-sm'
                    : 'text-[#55625A] hover:text-[#101713]'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </nav>

        {/* Scrollable Panel Body */}
        <div
          className={`overflow-y-auto px-4 py-4 transition-all ${
            mobileMapExpanded
              ? 'hidden'
              : mobilePreviewFrame
              ? 'h-[350px]'
              : 'max-h-[42dvh] lg:max-h-none lg:flex-1'
          }`}
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
              onSetPickup={(stop) => {
                setPickup(stop);
                setDistanceKm(0);
              }}
              onSetDestination={(stop) => {
                setDestination(stop);
                setDistanceKm(0);
              }}
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
              onCancelRide={() => setActiveRide(null)}
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
              onBookRideToPlace={handleBookPlaceAsDestination}
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

          {activeTab === 'account' && (
            <AccountPanel
              profile={profile}
              onOpenAuthModal={() => setAuthModalOpen(true)}
              onSwitchRole={handleSwitchRole}
              onSwitchActiveMode={handleSwitchActiveMode}
              onLogout={() => {
                setIsAuthenticated(false);
                setActiveTab('ride');
                setActiveRide(null);
                setActiveRiderJob(null);
              }}
            />
          )}
        </div>

        {/* Mobile Bottom Thumb-Zone Rounded Deep Forest Green Dock */}
        <div
          className={`px-3 pt-1 pb-2.5 pb-safe shrink-0 ${
            mobilePreviewFrame ? '' : 'lg:hidden'
          }`}
        >
          <nav
            aria-label="Mobile bottom navigation"
            className="grid grid-cols-4 items-center h-14 rounded-2xl glass-green text-white px-2"
          >
            {([
              {id: 'ride', label: 'Ride', icon: Navigation},
              {id: 'explore', label: 'Explore', icon: Compass},
              {id: 'rider', label: 'Rider', icon: Bike},
              {id: 'account', label: 'Account', icon: User},
            ] as const).map((tab) => {
              const Icon = tab.icon;
              const active = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => {
                    setActiveTab(tab.id);
                    setMobileMapExpanded(false);
                  }}
                  className={`h-10 rounded-xl flex flex-col items-center justify-center gap-0.5 transition-all ${
                    active
                      ? 'bg-white/20 text-white'
                      : 'text-white/70 hover:text-white'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span className="text-[10px] font-semibold tracking-tight">
                    {tab.label}
                  </span>
                </button>
              );
            })}
          </nav>
        </div>
      </aside>

      {/* Live Google Maps Viewport */}
      <main
        className={`order-1 flex-1 min-h-0 relative ${
          mobilePreviewFrame ? 'w-full' : 'lg:order-2 lg:h-full'
        }`}
      >
        <GoogleMapsWorkspace
          userLocation={userLocation}
          pickup={pickup}
          destination={destination}
          vehicleType={vehicleType}
          discoveredPlaces={discoveredPlaces}
          selectedPlaceId={selectedPlaceId}
          mapSelectionTarget={mapSelectionTarget}
          onMapTapSelect={handleMapTapSelect}
          onRouteCalculated={handleRouteCalculated}
          onDiscoveredPlacesChange={handleDiscoveredPlacesChange}
          onSelectDiscoveredPlace={(p) => setSelectedPlaceId(p.id || null)}
          onBookPlaceAsDestination={handleBookPlaceAsDestination}
          onRecenterUser={() => {
            if ('geolocation' in navigator) {
              navigator.geolocation.getCurrentPosition(
                (pos) =>
                  setUserLocation({
                    lat: pos.coords.latitude,
                    lng: pos.coords.longitude,
                  }),
                () => {}
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
    <APIProvider
      apiKey={MAPS_API_KEY}
      libraries={['places', 'routes', 'marker', 'geometry']}
      language="en"
    >
      <SplashIntro visible={showSplash} onFinish={() => setShowSplash(false)} />

      {/* Case A: Google Maps Quota Exceeded Sticky Banner */}
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

      {!isAuthenticated ? (
        <AuthGateScreen
          onAuthenticated={(nextProfile) => {
            setProfile(nextProfile);
            setActiveTab(nextProfile.activeMode === 'rider' ? 'rider' : 'ride');
            setIsAuthenticated(true);
            void syncProfileToSupabase(nextProfile);
          }}
        />
      ) : mobilePreviewFrame ? (
        <div className="min-h-[100dvh] w-full bg-[#E8EFEA] flex flex-col items-center justify-center p-4 gap-3">
          <div className="flex items-center gap-3 text-xs text-[#55625A]">
            <span>Mobile Phone Viewport Preview (390 x 820)</span>
            <button
              type="button"
              onClick={() => setMobilePreviewFrame(false)}
              className="px-3 py-1.5 rounded-lg bg-[#0F522B] text-white font-semibold hover:bg-[#146C3A]"
            >
              Exit Phone Preview
            </button>
          </div>
          {shellContent}
        </div>
      ) : (
        shellContent
      )}

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
  );
}
