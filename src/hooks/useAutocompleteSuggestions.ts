import {useCallback, useEffect, useRef, useState} from 'react';
import {useMapsLibrary} from '@vis.gl/react-google-maps';
import {LatLngPoint} from '../types/byahero';

export interface PlaceSuggestionItem {
  id: string;
  mainText: string;
  secondaryText: string;
  fullText: string;
  raw: google.maps.places.AutocompleteSuggestion;
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
    if (!placesLib) return;

    const trimmed = inputString.trim();
    if (trimmed.length < 2) {
      setSuggestions([]);
      setIsLoading(false);
      return;
    }

    const {AutocompleteSessionToken, AutocompleteSuggestion} = placesLib;
    if (!sessionTokenRef.current) {
      sessionTokenRef.current = new AutocompleteSessionToken();
    }

    let cancelled = false;
    setIsLoading(true);

    const timer = window.setTimeout(async () => {
      try {
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

        setSuggestions(mapped);
      } catch (err) {
        if (!cancelled) {
          if (isQuotaError(err)) {
            window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
          }
          setSuggestions([]);
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }, 220);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [placesLib, inputString, biasCenter?.lat, biasCenter?.lng]);

  const resolvePlace = useCallback(
    async (suggestion: PlaceSuggestionItem) => {
      if (!placesLib || !suggestion.raw.placePrediction) return null;
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
