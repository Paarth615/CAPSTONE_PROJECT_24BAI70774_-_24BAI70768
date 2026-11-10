import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { userApi } from '../api/endpoints';
import { errorMessage, fieldErrors } from '../api/client';
import { ErrorBox, Field, PageHeader } from '../components/ui';
import CityField from '../components/CityField';
import { BLOOD_GROUPS } from '../utils/constants';
import { formatDate, todayISO } from '../utils/format';

export default function ProfilePage() {
  const { user, setUser } = useAuth();
  const toast = useToast();
  const isDonor = user.role === 'DONOR';

  const [form, setForm] = useState({
    name: user.name || '',
    phone: user.phone || '',
    bloodGroup: user.bloodGroup || '',
    dateOfBirth: user.dateOfBirth || '',
    weightKg: user.weightKg ?? '',
    lastDonationDate: user.lastDonationDate || '',
  });
  const [place, setPlace] = useState({ city: user.city || '', latitude: user.latitude, longitude: user.longitude });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (ev) => {
    ev.preventDefault();
    const e = {};
    if (!form.name.trim()) e.name = 'Enter your name';
    if (!/^[+]?[0-9]{10,13}$/.test(form.phone.trim())) e.phone = 'Enter a 10 to 13 digit phone number';
    if (!place.city.trim()) e.city = 'Enter your city';
    if (isDonor) {
      if (!form.bloodGroup) e.bloodGroup = 'Choose your blood group';
      if (!form.dateOfBirth) e.dateOfBirth = 'Enter your date of birth';
      if (form.lastDonationDate && form.lastDonationDate > todayISO()) e.lastDonationDate = 'The date cannot be in the future';
    }
    setErrors(e);
    setFormError('');
    if (Object.keys(e).length) return;

    setBusy(true);
    try {
      const updated = await userApi.update({
        name: form.name.trim(),
        phone: form.phone.trim(),
        city: place.city.trim(),
        latitude: place.latitude,
        longitude: place.longitude,
        bloodGroup: isDonor ? form.bloodGroup : null,
        dateOfBirth: isDonor ? form.dateOfBirth : null,
        weightKg: isDonor && form.weightKg !== '' ? Number(form.weightKg) : null,
        lastDonationDate: isDonor && form.lastDonationDate ? form.lastDonationDate : null,
      });
      setUser(updated);
      toast.success('Profile saved');
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container page page-narrow">
      <PageHeader title="Your profile" subtitle={user.email} />

      {isDonor && (
        <div className={`panel eligibility ${user.eligible ? 'is-ready' : 'is-wait'}`}>
          <h2>{user.eligible ? 'You can donate now' : 'Not able to donate right now'}</h2>
          <p>{user.eligible ? 'Matching includes you.' : user.eligibilityNote}</p>
          {user.nextEligibleDate && new Date(user.nextEligibleDate) > new Date() && <p className="muted">Next eligible date: {formatDate(user.nextEligibleDate)}</p>}
        </div>
      )}

      <ErrorBox message={formError} />
      <form className="panel form-panel" onSubmit={submit} noValidate>
        <div className="form-grid">
          <Field label="Name" htmlFor="pname" error={errors.name}>
            <input id="pname" value={form.name} onChange={set('name')} maxLength={80} />
          </Field>
          <Field label="Phone number" htmlFor="pphone" error={errors.phone}>
            <input id="pphone" type="tel" value={form.phone} onChange={set('phone')} />
          </Field>
        </div>

        <CityField value={place} onChange={setPlace} error={errors.city} id="pcity" />

        {isDonor && (
          <div className="form-grid">
            <Field label="Blood group" htmlFor="pgroup" error={errors.bloodGroup}>
              <select id="pgroup" value={form.bloodGroup} onChange={set('bloodGroup')}>
                <option value="">Choose</option>
                {BLOOD_GROUPS.map((g) => (
                  <option key={g}>{g}</option>
                ))}
              </select>
            </Field>
            <Field label="Date of birth" htmlFor="pdob" error={errors.dateOfBirth}>
              <input id="pdob" type="date" value={form.dateOfBirth} onChange={set('dateOfBirth')} />
            </Field>
            <Field label="Weight (kg)" htmlFor="pweight" error={errors.weightKg}>
              <input id="pweight" type="number" min="30" max="250" step="0.5" value={form.weightKg} onChange={set('weightKg')} />
            </Field>
            <Field label="Last donation date" htmlFor="plast" error={errors.lastDonationDate} hint="Leave empty if you have never donated. Used for the 90-day rest rule.">
              <input id="plast" type="date" max={todayISO()} value={form.lastDonationDate} onChange={set('lastDonationDate')} />
            </Field>
          </div>
        )}

        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy ? 'Saving...' : 'Save changes'}
        </button>
      </form>
    </div>
  );
}
