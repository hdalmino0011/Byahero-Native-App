import {useCallback, useEffect, useRef, useState} from 'react';
import {useMapsLibrary} from '@vis.gl/react-google-maps';
import {LatLngPoint} from '../types/byahero';

export interface PlaceSuggestionItem {
  id: string;
  mainText: string;
  secondaryText: string;
  fullText: string;
  fallbackCoords?: LatLngPoint;
  raw?: google.maps.places.AutocompleteSuggestion;
}

export function isQuotaError(err: unknown): boolean {
  const msg = String(err instanceof Error ? err.message : err).toUpperCase();
  return (
    msg.includes('RESOURCE_EXHAUSTED') ||
    msg.includes('OVER_QUERY_LIMIT') ||
    msg.includes('QUOTA') ||
    msg.includes('429')
  );
}

async function fetchPhotonSuggestions(
  query: string,
  biasCenter?: LatLngPoint
): Promise<PlaceSuggestionItem[]> {
  try {
    const latParam = biasCenter ? `&lat=${biasCenter.lat}&lon=${biasCenter.lng}` : '';
    const res = await fetch(
      `https://photon.komoot.io/api/?q=${encodeURIComponent(query)}${latParam}&limit=6&lang=en`,
      {headers: {Accept: 'application/json'}}
    );
    if (!res.ok) return [];
    const data = (await res.json()) as {
      features?: Array<{
        geometry?: {coordinates?: [number, number]};
        properties?: {
          osm_id?: number;
          name?: string;
          street?: string;
          district?: string;
          locality?: string;
          city?: string;
          state?: string;
          country?: string;
        };
      }>;
    };
    const features = data.features || [];
    const items: PlaceSuggestionItem[] = [];
    features.forEach((f, idx) => {
      const coords = f.geometry?.coordinates;
      const props = f.properties;
      if (!coords || coords.length < 2 || !props) return;
      const lng = coords[0];
      const lat = coords[1];
      const mainText =
        props.name || props.street || props.district || props.city || query;
      const secondaryParts = [
        props.street && props.street !== mainText ? props.street : '',
        props.district || props.locality || '',
        props.city || props.state || '',
      ].filter(Boolean);
      const secondaryText = secondaryParts.join(', ');
      const fullText = [mainText, secondaryText].filter(Boolean).join(', ');
      items.push({
        id: `ph_${props.osm_id || idx}_${idx}`,
        mainText,
        secondaryText,
        fullText,
        fallbackCoords: {lat, lng},
      });
    });
    return items;
  } catch {
    return [];
  }
}

export function useAutocompleteSuggestions(
  inputString: string,
  biasCenter?: LatLngPoint
) {
  const placesLib = useMapsLibrary('places');
  const sessionTokenRef =
    useRef<google.maps.places.AutocompleteSessionToken | null>(null);
  const [suggestions, setSuggestions] = useState<PlaceSuggestionItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const trimmed = inputString.trim();
    if (trimmed.length < 2) {
      setSuggestions([]);
      setIsLoading(false);
      return;
    }

    let cancelled = false;
    setIsLoading(true);

    const timer = window.setTimeout(async () => {
      if (placesLib) {
        try {
          const {AutocompleteSessionToken, AutocompleteSuggestion} = placesLib;
          if (!sessionTokenRef.current) {
            sessionTokenRef.current = new AutocompleteSessionToken();
          }
          const request: google.maps.places.AutocompleteRequest = {
            input: trimmed,
            sessionToken: sessionTokenRef.current!,
            ...(biasCenter
              ? {
                  locationBias: {
                    center: biasCenter,
                    radius: 25000,
                  },
                }
              : {}),
          };

          const res = await AutocompleteSuggestion.fetchAutocompleteSuggestions(request);
          if (cancelled) return;

          const mapped: PlaceSuggestionItem[] = (res.suggestions || [])
            .filter((s) => Boolean(s.placePrediction))
            .slice(0, 6)
            .map((s, idx) => {
              const pred = s.placePrediction!;
              return {
                id: pred.placeId || `sug_${idx}`,
                mainText: pred.mainText?.text || pred.text.text,
                secondaryText: pred.secondaryText?.text || '',
                fullText: pred.text.text,
                raw: s,
              };
            });

          if (mapped.length > 0) {
            setSuggestions(mapped);
            setIsLoading(false);
            return;
          }
        } catch (err) {
          if (isQuotaError(err)) {
            window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
          }
        }
      }

      const fallbackItems = await fetchPhotonSuggestions(trimmed, biasCenter);
      if (!cancelled) {
        setSuggestions(fallbackItems);
        setIsLoading(false);
      }
    }, 240);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [placesLib, inputString, biasCenter?.lat, biasCenter?.lng]);

  const resolvePlace = useCallback(
    async (suggestion: PlaceSuggestionItem) => {
      if (suggestion.fallbackCoords) {
        sessionTokenRef.current = null;
        setSuggestions([]);
        return {
          label: suggestion.mainText,
          address: suggestion.fullText || suggestion.mainText,
          lat: suggestion.fallbackCoords.lat,
          lng: suggestion.fallbackCoords.lng,
          placeId: suggestion.id,
        };
      }

      if (!placesLib || !suggestion.raw?.placePrediction) return null;
      try {
        const place = suggestion.raw.placePrediction.toPlace();
        await place.fetchFields({
          fields: ['displayName', 'formattedAddress', 'location', 'viewport'],
        });
        sessionTokenRef.current = null;
        setSuggestions([]);
        const loc = place.location;
        if (!loc) return null;
        return {
          label: place.displayName || suggestion.mainText,
          address: place.formattedAddress || suggestion.fullText,
          lat: loc.lat(),
          lng: loc.lng(),
          placeId: place.id,
        };
      } catch (err) {
        if (isQuotaError(err)) {
          window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
        }
        return null;
      }
    },
    [placesLib]
  );

  const resetSession = useCallback(() => {
    sessionTokenRef.current = null;
    setSuggestions([]);
  }, []);

  return {suggestions, isLoading, resolvePlace, resetSession};
}
