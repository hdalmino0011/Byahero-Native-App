import {LocationStop} from '../types/byahero';
import {calculateHaversineKm} from './pricing';

function normalizeLabel(label: string | undefined | null): string {
  return (label || '').trim().toLowerCase().replace(/\s+/g, ' ');
}

function isPlaceholderLabel(label: string): boolean {
  return (
    !label ||
    label.startsWith('locating') ||
    label.startsWith('resolving') ||
    label === 'current gps location' ||
    label === 'selected location'
  );
}

/**
 * Determines whether two stops represent the same physical location or place,
 * preventing bookings or assignments where pickup and destination are identical.
 */
export function areLocationsSame(
  a: Pick<LocationStop, 'label' | 'address' | 'lat' | 'lng' | 'placeId'> | null | undefined,
  b: Pick<LocationStop, 'label' | 'address' | 'lat' | 'lng' | 'placeId'> | null | undefined
): boolean {
  if (!a || !b) return false;

  if (a.placeId && b.placeId && a.placeId === b.placeId) {
    return true;
  }

  // Within ~50 meters is considered the exact same spot
  const distKm = calculateHaversineKm(
    {lat: a.lat, lng: a.lng},
    {lat: b.lat, lng: b.lng}
  );
  if (Number.isFinite(distKm) && distKm < 0.05) {
    return true;
  }

  const labelA = normalizeLabel(a.label);
  const labelB = normalizeLabel(b.label);
  const addrA = normalizeLabel(a.address);
  const addrB = normalizeLabel(b.address);

  if (
    !isPlaceholderLabel(labelA) &&
    !isPlaceholderLabel(labelB) &&
    labelA === labelB &&
    (addrA === addrB || distKm < 0.25)
  ) {
    return true;
  }

  return false;
}
