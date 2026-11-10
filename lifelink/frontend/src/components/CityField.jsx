import { useState } from 'react';
import { CITIES, findCity } from '../utils/constants';
import { Field } from './ui';

/**
 * City input with suggestions. Picking a known city fills in coordinates (used for distance matching);
 * "Use my location" fills them from the browser instead.
 */
export default function CityField({ value, onChange, error, id = 'city', label = 'City' }) {
  const [geoState, setGeoState] = useState('');

  const handleCity = (e) => {
    const name = e.target.value;
    const known = findCity(name);
    onChange({
      city: name,
      latitude: known ? known.lat : null,
      longitude: known ? known.lng : null,
    });
    setGeoState('');
  };

  const useLocation = () => {
    if (!navigator.geolocation) {
      setGeoState('Location is not available in this browser');
      return;
    }
    setGeoState('Finding your location...');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        onChange({
          city: value.city,
          latitude: Number(pos.coords.latitude.toFixed(5)),
          longitude: Number(pos.coords.longitude.toFixed(5)),
        });
        setGeoState('Exact location saved');
      },
      () => setGeoState('Location permission was denied'),
      { timeout: 10000 }
    );
  };

  const hasCoords = value.latitude != null && value.longitude != null;

  return (
    <Field
      label={label}
      htmlFor={id}
      error={error}
      hint={hasCoords ? 'Distance-based matching is on for this location' : 'Pick a city from the list for distance-based matching'}
    >
      <input
        id={id}
        list={`${id}-list`}
        value={value.city}
        onChange={handleCity}
        autoComplete="off"
        placeholder="e.g. Lucknow"
        maxLength={60}
      />
      <datalist id={`${id}-list`}>
        {CITIES.map((c) => (
          <option key={c.name} value={c.name} />
        ))}
      </datalist>
      <div className="geo-row">
        <button type="button" className="btn btn-small btn-ghost" onClick={useLocation}>
          Use my location
        </button>
        {geoState && <span className="geo-state">{geoState}</span>}
      </div>
    </Field>
  );
}
