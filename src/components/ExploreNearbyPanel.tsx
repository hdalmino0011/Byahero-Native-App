import React from 'react';
import {ExternalLink, MapPin, Navigation, Star} from 'lucide-react';
import {DiscoveredPlace} from '../types/byahero';
import {FastFoodVectorIcon, ResortBeachVectorIcon} from './VectorGraphics';

interface ExploreNearbyPanelProps {
  places: DiscoveredPlace[];
  categoryLabel: string;
  isLoading: boolean;
  error: string | null;
  selectedPlaceId: string | null;
  onDiscoverCategory: (category: 'fast_food' | 'resort_beach') => void;
  onSelectPlace: (place: DiscoveredPlace) => void;
  onBookRideToPlace: (place: DiscoveredPlace) => void;
}

export const ExploreNearbyPanel: React.FC<ExploreNearbyPanelProps> = ({
  places,
  categoryLabel,
  isLoading,
  error,
  selectedPlaceId,
  onDiscoverCategory,
  onSelectPlace,
  onBookRideToPlace,
}) => {
  return (
    <div className="space-y-4">
      {/* Category Discovery Cards */}
      <div className="grid grid-cols-2 gap-2.5">
        <button
          type="button"
          onClick={() => onDiscoverCategory('fast_food')}
          className="min-h-[72px] p-3.5 rounded-2xl glass-surface hover:border-[#0F522B] text-left transition-all flex flex-col justify-between"
        >
          <FastFoodVectorIcon className="w-5 h-5 text-amber-600" />
          <div className="mt-2">
            <div className="text-xs font-bold text-[#101713]">Fast Food Nearby</div>
            <div className="text-[11px] text-[#55625A] truncate">
              McDonald&apos;s, Jollibee, KFC
            </div>
          </div>
        </button>

        <button
          type="button"
          onClick={() => onDiscoverCategory('resort_beach')}
          className="min-h-[72px] p-3.5 rounded-2xl glass-surface hover:border-[#0F522B] text-left transition-all flex flex-col justify-between"
        >
          <ResortBeachVectorIcon className="w-5 h-5 text-[#0F522B]" />
          <div className="mt-2">
            <div className="text-xs font-bold text-[#101713]">Resorts & Beaches</div>
            <div className="text-[11px] text-[#55625A] truncate">
              Nearest coastal & pool spots
            </div>
          </div>
        </button>
      </div>

      {/* Results Header Banner */}
      <div className="flex items-center justify-between px-4 py-2.5 rounded-2xl glass-green text-white text-xs">
        <span className="font-bold">{categoryLabel}</span>
        <span className="text-white/85 tabular-nums font-medium">
          {isLoading ? 'Searching...' : `${places.length} found`}
        </span>
      </div>

      {error && (
        <div className="p-3.5 rounded-2xl bg-amber-50 border border-amber-200 text-xs text-amber-900">
          {error}
        </div>
      )}

      {isLoading && (
        <div className="space-y-2.5">
          {[1, 2, 3].map((n) => (
            <div
              key={n}
              className="h-20 rounded-2xl glass-surface animate-pulse"
            />
          ))}
        </div>
      )}

      {!isLoading && places.length === 0 && !error && (
        <div className="p-5 rounded-2xl glass-surface text-center space-y-2">
          <p className="text-xs font-bold text-[#101713]">
            Discover nearby dining & getaways via Google Maps
          </p>
          <p className="text-[11px] text-[#55625A]">
            Tap Fast Food Nearby or Resorts & Beaches above, or search any location in the map bar.
          </p>
        </div>
      )}

      {/* Places List */}
      <div className="space-y-2.5">
        {places.map((place) => {
          const isSelected = place.id === selectedPlaceId;
          return (
            <div
              key={place.id}
              className={`p-3.5 rounded-2xl border transition-all ${
                isSelected
                  ? 'bg-white border-[#0F522B] shadow-md'
                  : 'glass-surface'
              }`}
            >
              <div className="flex items-start justify-between gap-2">
                <button
                  type="button"
                  onClick={() => onSelectPlace(place)}
                  className="text-left flex-1 min-w-0"
                >
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-[#101713] truncate">
                      {place.name}
                    </span>
                    {place.brandTag && (
                      <span className="text-[11px] font-semibold text-[#0F522B] shrink-0">
                        · {place.brandTag}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-[#55625A] truncate mt-0.5">{place.address}</p>
                  <div className="flex items-center gap-3 mt-1.5 text-[11px] text-[#55625A] tabular-nums">
                    {place.rating !== undefined && (
                      <span className="inline-flex items-center gap-1 font-medium text-[#101713]">
                        <Star className="w-3 h-3 fill-amber-400 text-amber-500" />
                        {place.rating.toFixed(1)}
                        {place.userRatingCount ? ` (${place.userRatingCount})` : ''}
                      </span>
                    )}
                    {place.distanceKm !== undefined && (
                      <span className="inline-flex items-center gap-1 text-[#0F522B] font-semibold">
                        <MapPin className="w-3 h-3" />
                        {place.distanceKm.toFixed(2)} km
                      </span>
                    )}
                  </div>
                </button>

                {place.googleMapsUri && (
                  <a
                    href={place.googleMapsUri}
                    target="_blank"
                    rel="noopener noreferrer"
                    title="Open in Google Maps"
                    aria-label={`Open ${place.name} in Google Maps`}
                    className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl bg-[#EBF1ED] hover:bg-[#DCE7E0] text-[#0F522B] shrink-0"
                  >
                    <ExternalLink className="w-4 h-4" />
                  </a>
                )}
              </div>

              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  onClick={() => onSelectPlace(place)}
                  className="flex-1 h-10 rounded-xl bg-[#EBF1ED] hover:bg-[#DCE7E0] text-xs font-semibold text-[#101713] transition-colors"
                >
                  Show on Map
                </button>
                <button
                  type="button"
                  onClick={() => onBookRideToPlace(place)}
                  className="flex-1 h-10 rounded-xl bg-[#0F522B] hover:bg-[#146C3A] text-xs font-bold text-white inline-flex items-center justify-center gap-1.5 shadow-sm transition-colors"
                >
                  <Navigation className="w-3.5 h-3.5" />
                  <span>Ride Here</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
