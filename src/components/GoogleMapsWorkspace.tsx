import React, {useCallback, useEffect, useRef, useState} from 'react';
import {
  AdvancedMarker,
  InfoWindow,
  Map as GoogleMap,
  MapMouseEvent,
  useAdvancedMarkerRef,
  useApiIsLoaded,
  useApiLoadingStatus,
  useMap,
  useMapsLibrary,
  APILoadingStatus,
} from '@vis.gl/react-google-maps';
import {Crosshair, MapPin, Navigation, Search, Star, X} from 'lucide-react';
import {
  DiscoveredPlace,
  LatLngPoint,
  LocationStop,
  MapSelectionTarget,
  VehicleType,
} from '../types/byahero';
import {calculateHaversineKm} from '../utils/pricing';
import {
  isQuotaError,
  PlaceSuggestionItem,
  useAutocompleteSuggestions,
} from '../hooks/useAutocompleteSuggestions';
import {FastFoodVectorIcon, ResortBeachVectorIcon} from './VectorGraphics';

interface GoogleMapsWorkspaceProps {
  userLocation: LatLngPoint;
  pickup: LocationStop | null;
  destination: LocationStop | null;
  vehicleType: VehicleType;
  discoveredPlaces: DiscoveredPlace[];
  selectedPlaceId: string | null;
  mapSelectionTarget: MapSelectionTarget;
  onMapTapSelect: (coords: LatLngPoint) => void;
  onRouteCalculated: (distanceKm: number, durationMins: number) => void;
  onDiscoveredPlacesChange: (places: DiscoveredPlace[], categoryLabel: string) => void;
  onSelectDiscoveredPlace: (place: DiscoveredPlace) => void;
  onBookPlaceAsDestination: (place: DiscoveredPlace) => void;
  onRecenterUser: () => void;
  onCancelMapTap: () => void;
  onSearchingPlacesChange: (loading: boolean, error: string | null) => void;
  triggerCategoryQuery: {category: 'fast_food' | 'resort_beach'; seq: number} | null;
}

const PlaceMarkerWithInfo: React.FC<{
  place: DiscoveredPlace;
  isSelected: boolean;
  onSelect: (place: DiscoveredPlace) => void;
  onBookHere: (place: DiscoveredPlace) => void;
}> = ({place, isSelected, onSelect, onBookHere}) => {
  const [markerRef, marker] = useAdvancedMarkerRef();
  const isFastFood = place.category === 'fast_food';
  const isSearch = place.category === 'search';

  return (
    <>
      <AdvancedMarker
        ref={markerRef}
        position={{lat: place.lat, lng: place.lng}}
        title={place.name}
        onClick={() => onSelect(place)}
      >
        <div
          className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-full border shadow-lg transition-transform backdrop-blur-md ${
            isSelected
              ? 'scale-110 bg-[#0F522B] text-white border-white'
              : isFastFood
              ? 'bg-white/95 text-[#101713] border-amber-500'
              : isSearch
              ? 'bg-[#0F522B]/95 text-white border-white'
              : 'bg-white/95 text-[#0F522B] border-[#0F522B]'
          }`}
        >
          {isFastFood ? (
            <FastFoodVectorIcon className={`w-3.5 h-3.5 shrink-0 ${isSelected ? 'text-amber-300' : 'text-amber-600'}`} />
          ) : isSearch ? (
            <MapPin className="w-3.5 h-3.5 shrink-0" />
          ) : (
            <ResortBeachVectorIcon className="w-3.5 h-3.5 shrink-0" />
          )}
          <span className="text-[11px] font-bold max-w-[110px] truncate whitespace-nowrap">
            {place.name}
          </span>
        </div>
      </AdvancedMarker>

      {isSelected && marker && (
        <InfoWindow
          anchor={marker}
          maxWidth={240}
          onCloseClick={() => onSelect({...place, id: ''})}
        >
          <div className="p-1 text-[#101713] font-sans">
            <div className="font-bold text-sm leading-tight">{place.name}</div>
            <div className="text-xs text-[#55625A] mt-1 line-clamp-2">{place.address}</div>
            <div className="flex items-center gap-2 mt-1.5 text-xs text-[#101713] tabular-nums">
              {place.rating !== undefined && (
                <span className="inline-flex items-center gap-1 font-semibold">
                  <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-500" />
                  {place.rating.toFixed(1)}
                </span>
              )}
              {place.distanceKm !== undefined && (
                <span className="text-[#0F522B] font-semibold">
                  {place.distanceKm.toFixed(1)} km away
                </span>
              )}
            </div>
            <button
              type="button"
              onClick={() => onBookHere(place)}
              className="mt-2.5 w-full h-9 rounded-lg bg-[#0F522B] text-white text-xs font-semibold hover:bg-[#146C3A] transition-colors"
            >
              Ride Here with BYAHERO
            </button>
          </div>
        </InfoWindow>
      )}
    </>
  );
};

export const GoogleMapsWorkspace: React.FC<GoogleMapsWorkspaceProps> = ({
  userLocation,
  pickup,
  destination,
  vehicleType,
  discoveredPlaces,
  selectedPlaceId,
  mapSelectionTarget,
  onMapTapSelect,
  onRouteCalculated,
  onDiscoveredPlacesChange,
  onSelectDiscoveredPlace,
  onBookPlaceAsDestination,
  onRecenterUser,
  onCancelMapTap,
  onSearchingPlacesChange,
  triggerCategoryQuery,
}) => {
  const map = useMap();
  const isApiLoaded = useApiIsLoaded();
  const loadingStatus = useApiLoadingStatus();
  const placesLib = useMapsLibrary('places');
  const routesLib = useMapsLibrary('routes');

  const polylinesRef = useRef<google.maps.Polyline[]>([]);
  const [searchInput, setSearchInput] = useState('');
  const [activeSearchFocus, setActiveSearchFocus] = useState(false);

  const {suggestions, isLoading: suggestionsLoading, resolvePlace, resetSession} =
    useAutocompleteSuggestions(searchInput, userLocation);

  // Pan map when userLocation changes meaningfully (initial GPS lock or recenter)
  const prevLocRef = useRef<LatLngPoint | null>(null);
  useEffect(() => {
    if (!map) return;
    const prev = prevLocRef.current;
    if (
      !prev ||
      Math.abs(prev.lat - userLocation.lat) > 0.002 ||
      Math.abs(prev.lng - userLocation.lng) > 0.002
    ) {
      map.panTo(userLocation);
      prevLocRef.current = userLocation;
    }
  }, [map, userLocation]);

  // Modern Route calculation using Route.computeRoutes (Zero-Legacy)
  useEffect(() => {
    if (!routesLib || !map) return;

    polylinesRef.current.forEach((p) => p.setMap(null));
    polylinesRef.current = [];

    if (!pickup || !destination) return;

    let cancelled = false;
    const origin = {lat: pickup.lat, lng: pickup.lng};
    const dest = {lat: destination.lat, lng: destination.lng};
    const mode = vehicleType === 'motorcycle' ? 'TWO_WHEELER' : 'DRIVING';

    const compute = async () => {
      try {
        const request = {
          origin,
          destination: dest,
          travelMode: mode,
          fields: ['path', 'distanceMeters', 'durationMillis', 'viewport'],
        };

        const {routes} = await (
          routesLib.Route as unknown as {
            computeRoutes: (req: unknown) => Promise<{routes: Array<{
              distanceMeters?: number;
              durationMillis?: number;
              viewport?: google.maps.LatLngBounds | google.maps.LatLngBoundsLiteral;
              createPolylines: () => google.maps.Polyline[];
            }>}>;
          }
        ).computeRoutes(request);

        if (cancelled) return;

        if (routes && routes.length > 0) {
          const primaryRoute = routes[0];
          const newPolylines = primaryRoute.createPolylines();
          newPolylines.forEach((polyline) => {
            polyline.setOptions({
              strokeColor: '#0F522B',
              strokeOpacity: 0.95,
              strokeWeight: 6,
            });
            polyline.setMap(map);
          });
          polylinesRef.current = newPolylines;

          if (primaryRoute.viewport) {
            map.fitBounds(primaryRoute.viewport, {
              top: 90,
              right: 40,
              bottom: 120,
              left: 40,
            });
          }

          const distKm = Math.max(0.5, (primaryRoute.distanceMeters ?? 2500) / 1000);
          const mins = Math.max(3, Math.round((primaryRoute.durationMillis ?? 600000) / 60000));
          onRouteCalculated(Number(distKm.toFixed(2)), mins);
        }
      } catch (err) {
        if (cancelled) return;
        if (isQuotaError(err)) {
          window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
        }
        const straightKm = Math.max(0.8, calculateHaversineKm(origin, dest) * 1.28);
        const speedKmh = vehicleType === 'motorcycle' ? 28 : 22;
        const mins = Math.max(4, Math.round((straightKm / speedKmh) * 60));
        const fallbackLine = new google.maps.Polyline({
          path: [origin, dest],
          geodesic: true,
          strokeColor: '#0F522B',
          strokeOpacity: 0.85,
          strokeWeight: 4,
          map,
        });
        polylinesRef.current = [fallbackLine];
        onRouteCalculated(Number(straightKm.toFixed(2)), mins);
      }
    };

    void compute();

    return () => {
      cancelled = true;
      polylinesRef.current.forEach((p) => p.setMap(null));
      polylinesRef.current = [];
    };
  }, [routesLib, map, pickup, destination, vehicleType, onRouteCalculated]);

  // Nearby Places Discovery using Places API (New): Place.searchByText
  const runPlacesDiscovery = useCallback(
    async (category: 'fast_food' | 'resort_beach' | 'search', customQuery?: string) => {
      if (!placesLib) {
        onSearchingPlacesChange(false, 'Google Maps Places library is loading.');
        return;
      }

      onSearchingPlacesChange(true, null);

      const center = map?.getCenter()
        ? {lat: map.getCenter()!.lat(), lng: map.getCenter()!.lng()}
        : userLocation;

      const textQuery =
        category === 'fast_food'
          ? 'McDonalds Jollibee KFC Burger King Chowking fast food restaurant'
          : category === 'resort_beach'
          ? 'beach resort coastal resort swimming resort'
          : (customQuery || '').trim();

      if (!textQuery) {
        onSearchingPlacesChange(false, null);
        return;
      }

      try {
        const request: google.maps.places.SearchByTextRequest = {
          textQuery,
          fields: [
            'displayName',
            'location',
            'formattedAddress',
            'rating',
            'userRatingCount',
            'businessStatus',
            'googleMapsURI',
          ],
          locationBias: {
            center,
            radius: category === 'resort_beach' ? 35000 : 10000,
          },
          maxResultCount: 10,
          language: 'en',
          internalUsageAttributionIds: ['gmp_mcp_codeassist_v1_aistudio'],
        } as google.maps.places.SearchByTextRequest;

        const {places} = await placesLib.Place.searchByText(request);
        const mapped: DiscoveredPlace[] = (places || [])
          .filter((p) => Boolean(p.location))
          .map((p) => {
            const loc = p.location!;
            const lat = loc.lat();
            const lng = loc.lng();
            const name = p.displayName || 'Unnamed Place';
            const lower = name.toLowerCase();
            let brandTag: string | undefined;
            if (lower.includes('jollibee')) brandTag = 'Jollibee';
            else if (lower.includes('mcdonald')) brandTag = "McDonald's";
            else if (lower.includes('kfc')) brandTag = 'KFC';
            else if (lower.includes('burger king')) brandTag = 'Burger King';
            else if (lower.includes('chowking')) brandTag = 'Chowking';

            return {
              id: p.id,
              name,
              address: p.formattedAddress || 'Address available on map',
              lat,
              lng,
              rating: p.rating ?? undefined,
              userRatingCount: p.userRatingCount ?? undefined,
              businessStatus: p.businessStatus ?? undefined,
              category,
              brandTag,
              googleMapsUri: p.googleMapsURI ?? undefined,
              distanceKm: Number(
                calculateHaversineKm(userLocation, {lat, lng}).toFixed(2)
              ),
            };
          })
          .sort((a, b) => (a.distanceKm ?? 999) - (b.distanceKm ?? 999));

        const label =
          category === 'fast_food'
            ? 'Nearby Fast Food'
            : category === 'resort_beach'
            ? 'Nearest Resorts & Beaches'
            : `Results for "${textQuery}"`;

        onDiscoveredPlacesChange(mapped, label);

        if (map && mapped.length > 0) {
          const bounds = new google.maps.LatLngBounds();
          bounds.extend(userLocation);
          mapped.slice(0, 6).forEach((m) => bounds.extend({lat: m.lat, lng: m.lng}));
          map.fitBounds(bounds, {top: 90, right: 40, bottom: 100, left: 40});
        }

        onSearchingPlacesChange(
          false,
          mapped.length === 0 ? `No places found nearby for ${label}. Try panning the map.` : null
        );
      } catch (err) {
        if (isQuotaError(err)) {
          window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
        }
        onSearchingPlacesChange(
          false,
          'Unable to load nearby places right now. Check your connection or API quota.'
        );
      }
    },
    [placesLib, map, userLocation, onDiscoveredPlacesChange, onSearchingPlacesChange]
  );

  // Trigger discovery when requested from Explore tab
  const lastSeqRef = useRef<number>(0);
  useEffect(() => {
    if (triggerCategoryQuery && triggerCategoryQuery.seq !== lastSeqRef.current) {
      lastSeqRef.current = triggerCategoryQuery.seq;
      void runPlacesDiscovery(triggerCategoryQuery.category);
    }
  }, [triggerCategoryQuery, runPlacesDiscovery]);

  const handleMapClick = useCallback(
    (ev: MapMouseEvent) => {
      if (!mapSelectionTarget || !ev.detail.latLng) return;
      onMapTapSelect({
        lat: ev.detail.latLng.lat,
        lng: ev.detail.latLng.lng,
      });
    },
    [mapSelectionTarget, onMapTapSelect]
  );

  const handleSearchSuggestionSelect = async (item: PlaceSuggestionItem) => {
    const resolved = await resolvePlace(item);
    if (!resolved) return;

    setSearchInput(resolved.label);
    setActiveSearchFocus(false);
    resetSession();

    const discovered: DiscoveredPlace = {
      id: resolved.placeId || `search_${Date.now()}`,
      name: resolved.label,
      address: resolved.address,
      lat: resolved.lat,
      lng: resolved.lng,
      category: 'search',
      distanceKm: Number(
        calculateHaversineKm(userLocation, {
          lat: resolved.lat,
          lng: resolved.lng,
        }).toFixed(2)
      ),
    };

    onDiscoveredPlacesChange([discovered], 'Searched Location');
    onSelectDiscoveredPlace(discovered);
    if (map) {
      map.panTo({lat: resolved.lat, lng: resolved.lng});
      map.setZoom(15);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setActiveSearchFocus(false);
    resetSession();
    void runPlacesDiscovery('search', searchInput);
  };

  const selectedPlace =
    discoveredPlaces.find((p) => p.id === selectedPlaceId) || null;

  return (
    <div className="relative w-full h-full bg-[#E8EFEA] overflow-hidden">
      {/* Top Floating Frosted Glass Search & Quick Discovery Controls */}
      <div className="absolute top-3 left-3 right-3 z-20 flex flex-col gap-2 pointer-events-none">
        <form
          onSubmit={handleSearchSubmit}
          className="pointer-events-auto relative flex items-center bg-white/85 backdrop-blur-xl border border-white/90 rounded-2xl shadow-[0_10px_28px_-8px_rgba(15,82,43,0.2)]"
        >
          <Search className="w-4 h-4 text-[#0F522B] ml-3.5 shrink-0" />
          <input
            type="text"
            value={searchInput}
            onFocus={() => setActiveSearchFocus(true)}
            onChange={(e) => {
              setSearchInput(e.target.value);
              setActiveSearchFocus(true);
            }}
            placeholder="Search any location, McDonald's, resort..."
            aria-label="Search any location on Google Maps"
            className="w-full h-11 px-3 bg-transparent text-sm text-[#101713] placeholder:text-[#7B8880] focus:outline-none"
          />
          {searchInput && (
            <button
              type="button"
              onClick={() => {
                setSearchInput('');
                resetSession();
              }}
              aria-label="Clear map search"
              className="min-h-[44px] min-w-[44px] flex items-center justify-center text-[#55625A] hover:text-[#101713]"
            >
              <X className="w-4 h-4" />
            </button>
          )}
          <button
            type="submit"
            className="mr-1.5 px-3.5 h-8 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-white text-xs font-semibold whitespace-nowrap shrink-0 transition-colors"
          >
            Find
          </button>
        </form>

        {/* Autocomplete Dropdown */}
        {activeSearchFocus && (suggestions.length > 0 || suggestionsLoading) && (
          <div className="pointer-events-auto bg-white/92 backdrop-blur-xl border border-white rounded-2xl shadow-2xl overflow-hidden divide-y divide-[#0F522B]/10">
            {suggestionsLoading && suggestions.length === 0 && (
              <div className="px-4 py-3 text-xs text-[#55625A]">Searching Google Places...</div>
            )}
            {suggestions.map((s) => (
              <button
                key={s.id}
                type="button"
                onClick={() => void handleSearchSuggestionSelect(s)}
                className="w-full min-h-[46px] px-4 py-2.5 text-left hover:bg-[#0F522B]/5 transition-colors flex items-start gap-2.5"
              >
                <MapPin className="w-4 h-4 text-[#0F522B] mt-0.5 shrink-0" />
                <div className="min-w-0">
                  <div className="text-xs font-semibold text-[#101713] truncate">{s.mainText}</div>
                  {s.secondaryText && (
                    <div className="text-[11px] text-[#55625A] truncate">{s.secondaryText}</div>
                  )}
                </div>
              </button>
            ))}
          </div>
        )}

        {/* Quick Nearby Vector Filter Buttons (Frosted Glass) */}
        <div className="pointer-events-auto flex items-center gap-2 overflow-x-auto no-scrollbar pb-0.5">
          <button
            type="button"
            onClick={() => void runPlacesDiscovery('fast_food')}
            className="min-h-[38px] px-3.5 py-1.5 rounded-xl bg-white/85 backdrop-blur-xl border border-white/90 hover:border-[#0F522B] text-xs font-semibold text-[#101713] flex items-center gap-1.5 whitespace-nowrap shrink-0 shadow-md transition-colors"
          >
            <FastFoodVectorIcon className="w-3.5 h-3.5 text-amber-600" />
            <span>Fast Food Nearby</span>
          </button>
          <button
            type="button"
            onClick={() => void runPlacesDiscovery('resort_beach')}
            className="min-h-[38px] px-3.5 py-1.5 rounded-xl bg-white/85 backdrop-blur-xl border border-white/90 hover:border-[#0F522B] text-xs font-semibold text-[#101713] flex items-center gap-1.5 whitespace-nowrap shrink-0 shadow-md transition-colors"
          >
            <ResortBeachVectorIcon className="w-3.5 h-3.5 text-[#0F522B]" />
            <span>Resorts & Beaches</span>
          </button>
          <button
            type="button"
            onClick={() => {
              onRecenterUser();
              if (map) {
                map.panTo(userLocation);
                map.setZoom(15);
              }
            }}
            aria-label="Recenter on my GPS location"
            className="min-h-[38px] px-3 py-1.5 rounded-xl bg-[#0F522B]/92 backdrop-blur-xl border border-white/25 text-xs font-semibold text-white flex items-center gap-1.5 whitespace-nowrap shrink-0 shadow-md hover:bg-[#146C3A] transition-colors"
          >
            <Crosshair className="w-3.5 h-3.5" />
            <span>My GPS</span>
          </button>
        </div>

        {/* Map Tap Picker Banner */}
        {mapSelectionTarget && (
          <div className="pointer-events-auto flex items-center justify-between gap-3 px-3.5 py-2.5 rounded-xl glass-green text-white shadow-lg">
            <div className="flex items-center gap-2 text-xs font-semibold">
              <Navigation className="w-4 h-4 shrink-0" />
              <span>
                Tap anywhere on the map to set{' '}
                {mapSelectionTarget === 'pickup' ? 'Pickup' : 'Destination'}
              </span>
            </div>
            <button
              type="button"
              onClick={onCancelMapTap}
              className="px-2.5 py-1 rounded-lg bg-white/20 text-xs font-semibold hover:bg-white/30"
            >
              Cancel
            </button>
          </div>
        )}
      </div>

      {/* Map Loading / Fallback States */}
      {(loadingStatus === APILoadingStatus.FAILED ||
        loadingStatus === APILoadingStatus.AUTH_FAILURE) && (
        <div className="absolute inset-0 z-10 flex items-center justify-center bg-[#F4F7F5] p-6 text-center">
          <div className="max-w-sm space-y-2 glass-surface p-6 rounded-2xl">
            <p className="text-sm font-bold text-[#101713]">Map temporarily unavailable</p>
            <p className="text-xs text-[#55625A]">
              Unable to load live map tiles right now. Please check your connection and try again.
            </p>
          </div>
        </div>
      )}

      {!isApiLoaded &&
        loadingStatus !== APILoadingStatus.FAILED &&
        loadingStatus !== APILoadingStatus.AUTH_FAILURE && (
          <div className="absolute inset-0 z-10 flex items-center justify-center bg-[#F4F7F5]">
            <div className="text-xs font-semibold text-[#0F522B] tracking-wide">
              Initializing Google Maps...
            </div>
          </div>
        )}

      {/* Main Google Map Canvas */}
      <GoogleMap
        defaultCenter={userLocation}
        defaultZoom={14}
        mapId="DEMO_MAP_ID"
        gestureHandling="greedy"
        disableDefaultUI={true}
        onClick={handleMapClick}
        internalUsageAttributionIds={['gmp_mcp_codeassist_v1_aistudio']}
        style={{width: '100%', height: '100%'}}
      >
        {/* User Live GPS Marker */}
        <AdvancedMarker position={userLocation} title="Your Real-Time Location">
          <div className="relative flex items-center justify-center">
            <span className="absolute w-9 h-9 rounded-full bg-[#0F522B]/30 animate-ping" />
            <span className="relative w-4 h-4 rounded-full bg-[#0F522B] border-2 border-white shadow-md" />
          </div>
        </AdvancedMarker>

        {/* Pickup Marker */}
        {pickup && (
          <AdvancedMarker
            position={{lat: pickup.lat, lng: pickup.lng}}
            title={`Pickup: ${pickup.label}`}
          >
            <div className="px-2.5 py-1 rounded-lg bg-white text-[#101713] border-2 border-[#0F522B] shadow-xl text-[11px] font-bold whitespace-nowrap">
              Pickup
            </div>
          </AdvancedMarker>
        )}

        {/* Destination Marker */}
        {destination && (
          <AdvancedMarker
            position={{lat: destination.lat, lng: destination.lng}}
            title={`Destination: ${destination.label}`}
          >
            <div className="px-2.5 py-1 rounded-lg bg-[#0F522B] text-white border-2 border-white shadow-xl text-[11px] font-bold whitespace-nowrap">
              Drop-off
            </div>
          </AdvancedMarker>
        )}

        {/* Discovered Nearby Fast Food / Resorts / Search Places */}
        {discoveredPlaces.map((place) => (
          <PlaceMarkerWithInfo
            key={place.id}
            place={place}
            isSelected={selectedPlace?.id === place.id}
            onSelect={onSelectDiscoveredPlace}
            onBookHere={onBookPlaceAsDestination}
          />
        ))}
      </GoogleMap>
    </div>
  );
};
