import {LatLngPoint} from '../types/byahero';

export interface ResolvedStopAddress {
  label: string;
  address: string;
}

function pickBestGooglePlaceLabel(results: google.maps.GeocoderResult[]): ResolvedStopAddress | null {
  if (!results || results.length === 0) return null;

  // Prefer named POI, premise, establishment, route, or neighborhood over plus codes
  const preferred =
    results.find(
      (r) =>
        !r.types.includes('plus_code') &&
        (r.types.includes('point_of_interest') ||
          r.types.includes('establishment') ||
          r.types.includes('premise') ||
          r.types.includes('route') ||
          r.types.includes('sublocality') ||
          r.types.includes('neighborhood'))
    ) ||
    results.find((r) => !r.types.includes('plus_code')) ||
    results[0];

  const comps = preferred.address_components || [];
  const poi = comps.find((c) => c.types.includes('point_of_interest') || c.types.includes('establishment'))?.short_name;
  const route = comps.find((c) => c.types.includes('route'))?.short_name;
  const sublocality = comps.find(
    (c) => c.types.includes('sublocality_level_1') || c.types.includes('sublocality') || c.types.includes('neighborhood')
  )?.short_name;
  const city = comps.find((c) => c.types.includes('locality'))?.short_name;

  const parts = [poi || route, sublocality || city].filter(Boolean);
  const label =
    parts.length > 0
      ? parts.join(', ')
      : preferred.formatted_address?.split(',').slice(0, 2).join(', ').trim() || 'Selected Location';

  return {
    label,
    address: preferred.formatted_address || label,
  };
}

export async function reverseGeocodePoint(coords: LatLngPoint): Promise<ResolvedStopAddress> {
  // 1. Try Google Maps Geocoder when loaded
  if (typeof window !== 'undefined' && window.google?.maps?.Geocoder) {
    try {
      const geocoder = new window.google.maps.Geocoder();
      const response = await geocoder.geocode({location: coords});
      const parsed = pickBestGooglePlaceLabel(response.results || []);
      if (parsed && !parsed.label.includes('+')) {
        return parsed;
      }
    } catch {
      // Fallback cleanly to Nominatim below
    }
  }

  // 2. Fallback to OpenStreetMap Nominatim reverse geocoding
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${coords.lat}&lon=${coords.lng}&zoom=18&addressdetails=1`,
      {headers: {Accept: 'application/json'}}
    );
    if (res.ok) {
      const data = (await res.json()) as {
        name?: string;
        display_name?: string;
        address?: {
          amenity?: string;
          shop?: string;
          tourism?: string;
          building?: string;
          road?: string;
          neighbourhood?: string;
          suburb?: string;
          village?: string;
          town?: string;
          city?: string;
        };
      };
      const a = data.address || {};
      const primary =
        data.name ||
        a.amenity ||
        a.shop ||
        a.tourism ||
        a.building ||
        a.road ||
        a.neighbourhood ||
        a.suburb ||
        a.village ||
        a.town ||
        a.city;
      const secondary =
        a.suburb || a.village || a.town || a.city;
      const label =
        primary && secondary && primary !== secondary
          ? `${primary}, ${secondary}`
          : primary || data.display_name?.split(',').slice(0, 2).join(',').trim() || 'Selected Location';

      return {
        label,
        address: data.display_name || label,
      };
    }
  } catch {
    // Ignore network failures
  }

  return {
    label: 'Selected Map Location',
    address: `${coords.lat.toFixed(5)}, ${coords.lng.toFixed(5)}`,
  };
}
