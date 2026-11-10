import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { requestApi } from '../api/endpoints';
import { errorMessage, fieldErrors } from '../api/client';
import { ErrorBox, Field, PageHeader } from '../components/ui';
import CityField from '../components/CityField';
import { BLOOD_GROUPS, URGENCIES, findCity } from '../utils/constants';
import { addDaysISO, todayISO } from '../utils/format';

export default function RequestFormPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const known = findCity(user.city);

  const [form, setForm] = useState({
    patientName: '',
    bloodGroup: '',
    unitsRequired: 1,
    hospital: '',
    urgency: 'URGENT',
    requiredBy: addDaysISO(2),
    contactPhone: user.phone || '',
    notes: '',
  });
  const [place, setPlace] = useState({
    city: user.city || '',
    latitude: user.latitude ?? (known ? known.lat : null),
    longitude: user.longitude ?? (known ? known.lng : null),
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const validate = () => {
    const e = {};
    if (!form.patientName.trim()) e.patientName = 'Enter the patient name';
    if (!form.bloodGroup) e.bloodGroup = 'Choose the blood group needed';
    const units = Number(form.unitsRequired);
    if (!Number.isInteger(units) || units < 1 || units > 10) e.unitsRequired = 'Enter 1 to 10 units';
    if (!form.hospital.trim()) e.hospital = 'Enter the hospital name';
    if (!place.city.trim()) e.city = 'Enter the city';
    if (!form.requiredBy) e.requiredBy = 'Choose the date blood is needed by';
    else if (form.requiredBy < todayISO()) e.requiredBy = 'The date cannot be in the past';
    if (!/^[+]?[0-9]{10,13}$/.test(form.contactPhone.trim())) e.contactPhone = 'Enter a 10 to 13 digit phone number';
    return e;
  };

  const submit = async (ev) => {
    ev.preventDefault();
    const e = validate();
    setErrors(e);
    setFormError('');
    if (Object.keys(e).length) return;

    setBusy(true);
    try {
      const created = await requestApi.create({
        patientName: form.patientName.trim(),
        bloodGroup: form.bloodGroup,
        unitsRequired: Number(form.unitsRequired),
        hospital: form.hospital.trim(),
        city: place.city.trim(),
        latitude: place.latitude,
        longitude: place.longitude,
        urgency: form.urgency,
        requiredBy: form.requiredBy,
        contactPhone: form.contactPhone.trim(),
        notes: form.notes.trim() || null,
      });
      toast.success('Request raised. Matching donors have been alerted.');
      navigate(`/requests/${created.id}`);
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container page page-narrow">
      <PageHeader title="Raise a blood request" subtitle="Matching donors are alerted as soon as you submit." />
      <ErrorBox message={formError} />
      <form className="panel form-panel" onSubmit={submit} noValidate>
        <div className="form-grid">
          <Field label="Patient name" htmlFor="patient" error={errors.patientName}>
            <input id="patient" value={form.patientName} onChange={set('patientName')} maxLength={80} />
          </Field>
          <Field label="Blood group needed" htmlFor="group" error={errors.bloodGroup}>
            <select id="group" value={form.bloodGroup} onChange={set('bloodGroup')}>
              <option value="">Choose</option>
              {BLOOD_GROUPS.map((g) => (
                <option key={g} value={g}>
                  {g}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Units needed" htmlFor="units" error={errors.unitsRequired}>
            <input id="units" type="number" min="1" max="10" value={form.unitsRequired} onChange={set('unitsRequired')} />
          </Field>
          <Field label="Needed by" htmlFor="by" error={errors.requiredBy}>
            <input id="by" type="date" min={todayISO()} value={form.requiredBy} onChange={set('requiredBy')} />
          </Field>
        </div>

        <fieldset className="chip-group">
          <legend>How urgent is it?</legend>
          {URGENCIES.map((u) => (
            <label key={u.value} className={`chip${form.urgency === u.value ? ' is-on' : ''}`}>
              <input type="radio" name="urgency" value={u.value} checked={form.urgency === u.value} onChange={set('urgency')} />
              <strong>{u.label}</strong>
              <span>{u.hint}</span>
            </label>
          ))}
        </fieldset>

        <div className="form-grid">
          <Field label="Hospital" htmlFor="hospital" error={errors.hospital}>
            <input id="hospital" value={form.hospital} onChange={set('hospital')} maxLength={120} />
          </Field>
          <Field label="Contact phone at the hospital or family" htmlFor="cphone" error={errors.contactPhone} hint="Shown to donors you accept">
            <input id="cphone" type="tel" value={form.contactPhone} onChange={set('contactPhone')} />
          </Field>
        </div>

        <CityField value={place} onChange={setPlace} error={errors.city} label="Hospital city" id="reqcity" />

        <Field label="Notes for donors (optional)" htmlFor="notes" error={errors.notes}>
          <textarea id="notes" rows={3} maxLength={1000} value={form.notes} onChange={set('notes')} />
        </Field>

        <button type="submit" className="btn btn-primary btn-large" disabled={busy}>
          {busy ? 'Raising request...' : 'Raise request'}
        </button>
      </form>
    </div>
  );
}
