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
import {Crosshair, MapPin, Minus, Navigation, Plus, Search, Star, X} from 'lucide-react';
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

const HAS_CONFIGURED_MAPS_KEY = Boolean(
  (import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string) &&
    !(import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string)
      .toLowerCase()
      .includes('placeholder')
);

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
  onBookPlaceAsPickup: (place: DiscoveredPlace) => void;
  onBookPlaceAsDestination: (place: DiscoveredPlace) => void;
  onRecenterUser: () => void;
  onCancelMapTap: () => void;
  onSearchingPlacesChange: (loading: boolean, error: string | null) => void;
  triggerCategoryQuery: {category: 'fast_food' | 'resort_beach'; seq: number} | null;
}

function lngToWorldX(lng: number, scale: number): number {
  return ((lng + 180) / 360) * scale;
}

function latToWorldY(lat: number, scale: number): number {
  const clamped = Math.max(-85.0511, Math.min(85.0511, lat));
  const s = Math.sin((clamped * Math.PI) / 180);
  const yNorm = 0.5 - Math.log((1 + s) / (1 - s)) / (4 * Math.PI);
  return yNorm * scale;
}

function worldToLatLng(worldX: number, worldY: number, scale: number): LatLngPoint {
  const lng = Math.max(-180, Math.min(180, (worldX / scale) * 360 - 180));
  const n = Math.PI - 2 * Math.PI * (worldY / scale);
  const lat = Math.max(
    -85.0511,
    Math.min(85.0511, (180 / Math.PI) * Math.atan(Math.sinh(n)))
  );
  return {lat, lng};
}

async function fetchFallbackPlaces2026(
  category: 'fast_food' | 'resort_beach' | 'search',
  textQuery: string,
  center: LatLngPoint,
  userLocation: LatLngPoint
): Promise<DiscoveredPlace[]> {
  const results: DiscoveredPlace[] = [];

  if (category === 'fast_food' || category === 'resort_beach') {
    try {
      const radius = category === 'resort_beach' ? 35000 : 10000;
      const overpassQuery =
        category === 'fast_food'
          ? `[out:json][timeout:10];(node["amenity"="fast_food"](around:${radius},${center.lat},${center.lng});way["amenity"="fast_food"](around:${radius},${center.lat},${center.lng});node["name"~"Jollibee|McDonald|KFC|Chowking|Burger King|Mang Inasal",i](around:${radius},${center.lat},${center.lng}););out center 16;`
          : `[out:json][timeout:10];(node["tourism"~"resort|hotel"](around:${radius},${center.lat},${center.lng});way["tourism"="resort"](around:${radius},${center.lat},${center.lng});node["natural"="beach"](around:${radius},${center.lat},${center.lng});way["natural"="beach"](around:${radius},${center.lat},${center.lng}););out center 16;`;
      const res = await fetch('https://overpass-api.de/api/interpreter', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: `data=${encodeURIComponent(overpassQuery)}`,
      });
      if (res.ok) {
        const data = (await res.json()) as {
          elements?: Array<{
            id?: number;
            lat?: number;
            lon?: number;
            center?: {lat?: number; lon?: number};
            tags?: Record<string, string>;
          }>;
        };
        (data.elements || []).forEach((el, idx) => {
          const name = el.tags?.name?.trim();
          const lat = el.lat ?? el.center?.lat;
          const lng = el.lon ?? el.center?.lon;
          if (!name || typeof lat !== 'number' || typeof lng !== 'number') return;
          const addr =
            el.tags?.['addr:street'] ||
            el.tags?.['addr:city'] ||
            'Nearby map location';
          results.push({
            id: `ov_${el.id || idx}`,
            name,
            address: addr,
            lat,
            lng,
            category,
            distanceKm: Number(
              calculateHaversineKm(userLocation, {lat, lng}).toFixed(2)
            ),
          });
        });
      }
    } catch {
      // fall through to Photon search
    }
  }

  if (results.length === 0) {
    const queries =
      category === 'fast_food'
        ? ['Jollibee', "McDonald's", 'KFC']
        : category === 'resort_beach'
        ? ['beach resort', 'resort']
        : [textQuery];

    for (const q of queries) {
      if (!q.trim()) continue;
      try {
        const res = await fetch(
          `https://photon.komoot.io/api/?q=${encodeURIComponent(q)}&lat=${center.lat}&lon=${center.lng}&limit=10&lang=en`,
          {headers: {Accept: 'application/json'}}
        );
        if (res.ok) {
          const data = (await res.json()) as {
            features?: Array<{
              geometry?: {coordinates?: [number, number]};
              properties?: {
                osm_id?: number;
                name?: string;
                street?: string;
                district?: string;
                city?: string;
              };
            }>;
          };
          (data.features || []).forEach((f, idx) => {
            const coords = f.geometry?.coordinates;
            const props = f.properties;
            if (!coords || coords.length < 2 || !props) return;
            const lng = coords[0];
            const lat = coords[1];
            const name = props.name || props.street || props.district || q;
            const address = [props.street, props.district, props.city]
              .filter(Boolean)
              .join(', ');
            results.push({
              id: `ph_${props.osm_id || idx}_${idx}`,
              name,
              address: address || name,
              lat,
              lng,
              category,
              distanceKm: Number(
                calculateHaversineKm(userLocation, {lat, lng}).toFixed(2)
              ),
            });
          });
        }
      } catch {
        // ignore network error
      }
    }
  }

  const seen = new Set<string>();
  return results
    .filter((item) => {
      const key = `${item.name.toLowerCase()}_${item.lat.toFixed(3)}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    })
    .sort((a, b) => (a.distanceKm ?? 999) - (b.distanceKm ?? 999))
    .slice(0, 12);
}

const Interactive2026TileMap: React.FC<{
  userLocation: LatLngPoint;
  pickup: LocationStop | null;
  destination: LocationStop | null;
  discoveredPlaces: DiscoveredPlace[];
  selectedPlace: DiscoveredPlace | null;
  centerTarget: LatLngPoint;
  zoom: number;
  onZoomChange: (nextZoom: number) => void;
  onCenterChange: (nextCenter: LatLngPoint) => void;
  onMapTap: (coords: LatLngPoint) => void;
  onSelectPlace: (place: DiscoveredPlace) => void;
  onSetPickup: (place: DiscoveredPlace) => void;
  onBookHere: (place: DiscoveredPlace) => void;
}> = ({
  userLocation,
  pickup,
  destination,
  discoveredPlaces,
  selectedPlace,
  centerTarget,
  zoom,
  onZoomChange,
  onCenterChange,
  onMapTap,
  onSelectPlace,
  onSetPickup,
  onBookHere,
}) => {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [size, setSize] = useState({width: 800, height: 600});
  const dragStateRef = useRef<{
    dragging: boolean;
    startX: number;
    startY: number;
    moved: boolean;
    startCenter: LatLngPoint;
  }>({
    dragging: false,
    startX: 0,
    startY: 0,
    moved: false,
    startCenter: centerTarget,
  });

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const updateSize = () => {
      const rect = el.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        setSize({width: rect.width, height: rect.height});
      }
    };
    updateSize();
    const ro = new ResizeObserver(updateSize);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const tilePx = 256;
  const scale = tilePx * Math.pow(2, zoom);
  const centerWorldX = lngToWorldX(centerTarget.lng, scale);
  const centerWorldY = latToWorldY(centerTarget.lat, scale);
  const halfW = size.width / 2;
  const halfH = size.height / 2;
  const maxTileIdx = 1 << zoom;

  const minTx = Math.floor((centerWorldX - halfW) / tilePx);
  const maxTx = Math.floor((centerWorldX + halfW) / tilePx);
  const minTy = Math.max(0, Math.floor((centerWorldY - halfH) / tilePx));
  const maxTy = Math.min(
    maxTileIdx - 1,
    Math.floor((centerWorldY + halfH) / tilePx)
  );

  const tiles: Array<{key: string; url: string; left: number; top: number}> = [];
  for (let ty = minTy; ty <= maxTy; ty++) {
    for (let txRaw = minTx; txRaw <= maxTx; txRaw++) {
      const tx = ((txRaw % maxTileIdx) + maxTileIdx) % maxTileIdx;
      const shard = (tx + ty) & 3;
      const left = Math.round(txRaw * tilePx - centerWorldX + halfW);
      const top = Math.round(ty * tilePx - centerWorldY + halfH);
      tiles.push({
        key: `${zoom}_${txRaw}_${ty}`,
        url: `https://mt${shard}.google.com/vt/lyrs=m&x=${tx}&y=${ty}&z=${zoom}&hl=en&scale=2`,
        left,
        top,
      });
    }
  }

  const project = (pt: LatLngPoint) => ({
    x: lngToWorldX(pt.lng, scale) - centerWorldX + halfW,
    y: latToWorldY(pt.lat, scale) - centerWorldY + halfH,
  });

  const userPos = project(userLocation);
  const pickupPos = pickup ? project({lat: pickup.lat, lng: pickup.lng}) : null;
  const destPos = destination
    ? project({lat: destination.lat, lng: destination.lng})
    : null;

  return (
    <div
      ref={containerRef}
      onPointerDown={(e) => {
        if ((e.target as HTMLElement).closest('button')) return;
        e.currentTarget.setPointerCapture(e.pointerId);
        dragStateRef.current = {
          dragging: true,
          startX: e.clientX,
          startY: e.clientY,
          moved: false,
          startCenter: centerTarget,
        };
      }}
      onPointerMove={(e) => {
        if (!dragStateRef.current.dragging) return;
        const dx = e.clientX - dragStateRef.current.startX;
        const dy = e.clientY - dragStateRef.current.startY;
        if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
          dragStateRef.current.moved = true;
        }
        const startWx = lngToWorldX(dragStateRef.current.startCenter.lng, scale);
        const startWy = latToWorldY(dragStateRef.current.startCenter.lat, scale);
        onCenterChange(worldToLatLng(startWx - dx, startWy - dy, scale));
      }}
      onPointerUp={(e) => {
        if (!dragStateRef.current.dragging) return;
        const wasMoved = dragStateRef.current.moved;
        dragStateRef.current.dragging = false;
        if (!wasMoved) {
          const rect = e.currentTarget.getBoundingClientRect();
          const clickX = e.clientX - rect.left;
          const clickY = e.clientY - rect.top;
          const wx = centerWorldX + (clickX - halfW);
          const wy = centerWorldY + (clickY - halfH);
          onMapTap(worldToLatLng(wx, wy, scale));
        }
      }}
      className="relative w-full h-full bg-[#E8EEF1] overflow-hidden select-none cursor-grab active:cursor-grabbing touch-none"
    >
      {/* 2026 High-DPI Google Maps Road Tiles */}
      {tiles.map((t) => (
        <img
          key={t.key}
          src={t.url}
          alt=""
          draggable={false}
          style={{
            position: 'absolute',
            left: `${t.left}px`,
            top: `${t.top}px`,
            width: `${tilePx}px`,
            height: `${tilePx}px`,
          }}
          className="pointer-events-none max-w-none"
        />
      ))}

      {/* Route Line between Pickup and Destination */}
      {pickupPos && destPos && (
        <svg className="absolute inset-0 w-full h-full pointer-events-none">
          <line
            x1={pickupPos.x}
            y1={pickupPos.y}
            x2={destPos.x}
            y2={destPos.y}
            stroke="#FFFFFF"
            strokeWidth="8"
            strokeLinecap="round"
          />
          <line
            x1={pickupPos.x}
            y1={pickupPos.y}
            x2={destPos.x}
            y2={destPos.y}
            stroke="#0F522B"
            strokeWidth="5"
            strokeDasharray="12 7"
            strokeLinecap="round"
          />
        </svg>
      )}

      {/* User Live GPS Marker */}
      <div
        style={{
          left: `${userPos.x}px`,
          top: `${userPos.y}px`,
          transform: 'translate(-50%, -50%)',
        }}
        className="absolute pointer-events-none flex items-center justify-center"
      >
        <span className="absolute w-9 h-9 rounded-full bg-[#0F522B]/30 animate-ping" />
        <span className="relative w-4 h-4 rounded-full bg-[#0F522B] border-2 border-white shadow-md" />
      </div>

      {/* Pickup Marker */}
      {pickupPos && (
        <div
          style={{
            left: `${pickupPos.x}px`,
            top: `${pickupPos.y}px`,
            transform: 'translate(-50%, -115%)',
          }}
          className="absolute pointer-events-none px-2.5 py-1 rounded-lg bg-white text-[#101713] border-2 border-[#0F522B] shadow-xl text-[11px] font-bold whitespace-nowrap"
        >
          Pickup
        </div>
      )}

      {/* Destination Marker */}
      {destPos && (
        <div
          style={{
            left: `${destPos.x}px`,
            top: `${destPos.y}px`,
            transform: 'translate(-50%, -115%)',
          }}
          className="absolute pointer-events-none px-2.5 py-1 rounded-lg bg-[#0F522B] text-white border-2 border-white shadow-xl text-[11px] font-bold whitespace-nowrap"
        >
          Drop-off
        </div>
      )}

      {/* Discovered Places Markers */}
      {discoveredPlaces.map((place) => {
        const pt = project({lat: place.lat, lng: place.lng});
        const isSelected = selectedPlace?.id === place.id;
        const isFastFood = place.category === 'fast_food';
        const isSearch = place.category === 'search';
        return (
          <div
            key={place.id}
            style={{
              left: `${pt.x}px`,
              top: `${pt.y}px`,
              transform: 'translate(-50%, -100%)',
            }}
            className="absolute z-10"
          >
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                onSelectPlace(place);
              }}
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
                <FastFoodVectorIcon
                  className={`w-3.5 h-3.5 shrink-0 ${
                    isSelected ? 'text-amber-300' : 'text-amber-600'
                  }`}
                />
              ) : isSearch ? (
                <MapPin className="w-3.5 h-3.5 shrink-0" />
              ) : (
                <ResortBeachVectorIcon className="w-3.5 h-3.5 shrink-0" />
              )}
              <span className="text-[11px] font-bold max-w-[110px] truncate whitespace-nowrap">
                {place.name}
              </span>
            </button>

            {isSelected && (
              <div
                onClick={(e) => e.stopPropagation()}
                className="mt-1.5 w-56 p-2.5 rounded-2xl bg-white text-[#101713] border border-[#0F522B]/20 shadow-2xl"
              >
                <div className="font-bold text-xs leading-tight truncate">
                  {place.name}
                </div>
                <div className="text-[11px] text-[#55625A] mt-0.5 line-clamp-2">
                  {place.address}
                </div>
                {place.distanceKm !== undefined && (
                  <div className="text-[11px] text-[#0F522B] font-semibold mt-1">
                    {place.distanceKm.toFixed(1)} km away
                  </div>
                )}
                <div className="mt-2 flex gap-1.5">
                  <button
                    type="button"
                    onClick={() => onSetPickup(place)}
                    className="flex-1 h-8 rounded-lg bg-[#EBF1ED] text-[#0F522B] text-[11px] font-bold hover:bg-[#DCE7E0]"
                  >
                    Set Pickup
                  </button>
                  <button
                    type="button"
                    onClick={() => onBookHere(place)}
                    className="flex-1 h-8 rounded-lg bg-[#0F522B] text-white text-[11px] font-bold hover:bg-[#146C3A]"
                  >
                    Set Dest
                  </button>
                </div>
              </div>
            )}
          </div>
        );
      })}

      {/* Bottom-left Google 2026 Attribution */}
      <div className="pointer-events-none absolute bottom-2 left-2.5 px-2 py-0.5 rounded bg-white/88 text-[10px] text-[#55625A] flex items-center gap-1.5 shadow-sm">
        <span className="font-bold text-[#3C4043]">Google</span>
        <span>Map data ©2026 Google</span>
      </div>

      {/* Bottom-right Zoom Controls */}
      <div className="absolute bottom-3 right-3 flex flex-col gap-1.5 z-10">
        <button
          type="button"
          onClick={() => onZoomChange(Math.min(19, zoom + 1))}
          aria-label="Zoom in"
          className="w-9 h-9 rounded-full bg-white/95 hover:bg-white border border-[#0F522B]/15 shadow-md flex items-center justify-center text-[#101713]"
        >
          <Plus className="w-4 h-4" />
        </button>
        <button
          type="button"
          onClick={() => onZoomChange(Math.max(3, zoom - 1))}
          aria-label="Zoom out"
          className="w-9 h-9 rounded-full bg-white/95 hover:bg-white border border-[#0F522B]/15 shadow-md flex items-center justify-center text-[#101713]"
        >
          <Minus className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

const PlaceMarkerWithInfo: React.FC<{
  place: DiscoveredPlace;
  isSelected: boolean;
  onSelect: (place: DiscoveredPlace) => void;
  onSetPickup: (place: DiscoveredPlace) => void;
  onBookHere: (place: DiscoveredPlace) => void;
}> = ({place, isSelected, onSelect, onSetPickup, onBookHere}) => {
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
            <FastFoodVectorIcon
              className={`w-3.5 h-3.5 shrink-0 ${
                isSelected ? 'text-amber-300' : 'text-amber-600'
              }`}
            />
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
            <div className="mt-2.5 flex gap-1.5">
              <button
                type="button"
                onClick={() => onSetPickup(place)}
                className="flex-1 h-9 rounded-lg bg-[#EBF1ED] text-[#0F522B] text-xs font-semibold hover:bg-[#DCE7E0] transition-colors"
              >
                Set Pickup
              </button>
              <button
                type="button"
                onClick={() => onBookHere(place)}
                className="flex-1 h-9 rounded-lg bg-[#0F522B] text-white text-xs font-semibold hover:bg-[#146C3A] transition-colors"
              >
                Set Dest
              </button>
            </div>
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
  onBookPlaceAsPickup,
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
  const [fallbackCenter, setFallbackCenter] = useState<LatLngPoint>(userLocation);
  const [fallbackZoom, setFallbackZoom] = useState<number>(15);

  const useFallbackTileMap =
    !HAS_CONFIGURED_MAPS_KEY ||
    loadingStatus === APILoadingStatus.FAILED ||
    loadingStatus === APILoadingStatus.AUTH_FAILURE;

  const {suggestions, isLoading: suggestionsLoading, resolvePlace, resetSession} =
    useAutocompleteSuggestions(searchInput, userLocation);

  const prevLocRef = useRef<LatLngPoint | null>(null);
  useEffect(() => {
    const prev = prevLocRef.current;
    if (
      !prev ||
      Math.abs(prev.lat - userLocation.lat) > 0.002 ||
      Math.abs(prev.lng - userLocation.lng) > 0.002
    ) {
      setFallbackCenter(userLocation);
      if (map) {
        map.panTo(userLocation);
      }
      prevLocRef.current = userLocation;
    }
  }, [map, userLocation]);

  useEffect(() => {
    if (pickup && destination) {
      const origin = {lat: pickup.lat, lng: pickup.lng};
      const dest = {lat: destination.lat, lng: destination.lng};
      setFallbackCenter({
        lat: (origin.lat + dest.lat) / 2,
        lng: (origin.lng + dest.lng) / 2,
      });
      setFallbackZoom(13);

      if (useFallbackTileMap) {
        const straightKm = Math.max(0.8, calculateHaversineKm(origin, dest) * 1.28);
        const speedKmh = vehicleType === 'motorcycle' ? 28 : 22;
        const mins = Math.max(4, Math.round((straightKm / speedKmh) * 60));
        onRouteCalculated(Number(straightKm.toFixed(2)), mins);
      }
    }
  }, [pickup, destination, vehicleType, useFallbackTileMap, onRouteCalculated]);

  useEffect(() => {
    if (!routesLib || !map || useFallbackTileMap) return;

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
            computeRoutes: (req: unknown) => Promise<{
              routes: Array<{
                distanceMeters?: number;
                durationMillis?: number;
                viewport?: google.maps.LatLngBounds | google.maps.LatLngBoundsLiteral;
                createPolylines: () => google.maps.Polyline[];
              }>;
            }>;
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
  }, [routesLib, map, pickup, destination, vehicleType, useFallbackTileMap, onRouteCalculated]);

  const runPlacesDiscovery = useCallback(
    async (category: 'fast_food' | 'resort_beach' | 'search', customQuery?: string) => {
      onSearchingPlacesChange(true, null);

      const center =
        !useFallbackTileMap && map?.getCenter()
          ? {lat: map.getCenter()!.lat(), lng: map.getCenter()!.lng()}
          : fallbackCenter || userLocation;

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

      const label =
        category === 'fast_food'
          ? 'Nearby Fast Food'
          : category === 'resort_beach'
          ? 'Nearest Resorts & Beaches'
          : `Results for "${textQuery}"`;

      if (placesLib && !useFallbackTileMap) {
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

          if (mapped.length > 0) {
            onDiscoveredPlacesChange(mapped, label);
            if (map) {
              const bounds = new google.maps.LatLngBounds();
              bounds.extend(userLocation);
              mapped.slice(0, 6).forEach((m) => bounds.extend({lat: m.lat, lng: m.lng}));
              map.fitBounds(bounds, {top: 90, right: 40, bottom: 100, left: 40});
            }
            onSearchingPlacesChange(false, null);
            return;
          }
        } catch (err) {
          if (isQuotaError(err)) {
            window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
          }
        }
      }

      const fallbackMapped = await fetchFallbackPlaces2026(
        category,
        textQuery,
        center,
        userLocation
      );
      onDiscoveredPlacesChange(fallbackMapped, label);
      if (fallbackMapped.length > 0) {
        setFallbackCenter({lat: fallbackMapped[0].lat, lng: fallbackMapped[0].lng});
        setFallbackZoom(14);
      }
      onSearchingPlacesChange(
        false,
        fallbackMapped.length === 0
          ? `No places found nearby for ${label}. Try panning the map.`
          : null
      );
    },
    [
      placesLib,
      map,
      useFallbackTileMap,
      fallbackCenter,
      userLocation,
      onDiscoveredPlacesChange,
      onSearchingPlacesChange,
    ]
  );

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

  const handleSearchSuggestionSelect = async (
    item: PlaceSuggestionItem,
    assignAs?: 'pickup' | 'destination'
  ) => {
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
    if (assignAs === 'pickup') {
      onBookPlaceAsPickup(discovered);
    } else if (assignAs === 'destination') {
      onBookPlaceAsDestination(discovered);
    }
    setFallbackCenter({lat: resolved.lat, lng: resolved.lng});
    setFallbackZoom(15);
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
              <div className="px-4 py-3 text-xs text-[#55625A]">Searching places...</div>
            )}
            {suggestions.map((s) => (
              <div
                key={s.id}
                className="w-full min-h-[48px] px-3.5 py-2.5 hover:bg-[#0F522B]/5 transition-colors flex items-center justify-between gap-2"
              >
                <button
                  type="button"
                  onClick={() => void handleSearchSuggestionSelect(s)}
                  className="flex-1 min-w-0 text-left flex items-start gap-2.5"
                >
                  <MapPin className="w-4 h-4 text-[#0F522B] mt-0.5 shrink-0" />
                  <div className="min-w-0">
                    <div className="text-xs font-semibold text-[#101713] truncate">
                      {s.mainText}
                    </div>
                    {s.secondaryText && (
                      <div className="text-[11px] text-[#55625A] truncate">
                        {s.secondaryText}
                      </div>
                    )}
                  </div>
                </button>
                <div className="flex items-center gap-1 shrink-0">
                  <button
                    type="button"
                    onClick={() => void handleSearchSuggestionSelect(s, 'pickup')}
                    className="h-7 px-2 rounded-lg bg-[#EBF1ED] hover:bg-[#DCE7E0] text-[#0F522B] text-[10px] font-bold whitespace-nowrap"
                  >
                    Pickup
                  </button>
                  <button
                    type="button"
                    onClick={() => void handleSearchSuggestionSelect(s, 'destination')}
                    className="h-7 px-2 rounded-lg bg-[#0F522B] hover:bg-[#146C3A] text-white text-[10px] font-bold whitespace-nowrap"
                  >
                    Dest
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Quick Nearby Vector Filter Buttons */}
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
              setFallbackCenter(userLocation);
              setFallbackZoom(15);
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

      {useFallbackTileMap ? (
        <Interactive2026TileMap
          userLocation={userLocation}
          pickup={pickup}
          destination={destination}
          discoveredPlaces={discoveredPlaces}
          selectedPlace={selectedPlace}
          centerTarget={fallbackCenter}
          zoom={fallbackZoom}
          onZoomChange={setFallbackZoom}
          onCenterChange={setFallbackCenter}
          onMapTap={(coords) => {
            if (mapSelectionTarget) {
              onMapTapSelect(coords);
            }
          }}
          onSelectPlace={onSelectDiscoveredPlace}
          onSetPickup={onBookPlaceAsPickup}
          onBookHere={onBookPlaceAsDestination}
        />
      ) : (
        <>
          {!isApiLoaded && (
            <div className="absolute inset-0 z-10 flex items-center justify-center bg-[#F4F7F5]">
              <div className="text-xs font-semibold text-[#0F522B] tracking-wide">
                Initializing Google Maps...
              </div>
            </div>
          )}

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
            <AdvancedMarker position={userLocation} title="Your Real-Time Location">
              <div className="relative flex items-center justify-center">
                <span className="absolute w-9 h-9 rounded-full bg-[#0F522B]/30 animate-ping" />
                <span className="relative w-4 h-4 rounded-full bg-[#0F522B] border-2 border-white shadow-md" />
              </div>
            </AdvancedMarker>

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

            {discoveredPlaces.map((place) => (
              <PlaceMarkerWithInfo
                key={place.id}
                place={place}
                isSelected={selectedPlace?.id === place.id}
                onSelect={onSelectDiscoveredPlace}
                onSetPickup={onBookPlaceAsPickup}
                onBookHere={onBookPlaceAsDestination}
              />
            ))}
          </GoogleMap>
        </>
      )}
    </div>
  );
};
