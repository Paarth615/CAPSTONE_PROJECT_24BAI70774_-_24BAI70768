import { useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { errorMessage, fieldErrors } from '../api/client';
import { ErrorBox, Field } from '../components/ui';
import CityField from '../components/CityField';
import { BLOOD_GROUPS } from '../utils/constants';
import { useToast } from '../context/ToastContext';

function ageFrom(dob) {
  if (!dob) return null;
  const d = new Date(dob);
  const now = new Date();
  let age = now.getFullYear() - d.getFullYear();
  const m = now.getMonth() - d.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < d.getDate())) age -= 1;
  return age;
}

export default function Register() {
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [params] = useSearchParams();
  const initialRole = params.get('role') === 'REQUESTER' ? 'REQUESTER' : 'DONOR';

  const [role, setRole] = useState(initialRole);
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    phone: '',
    bloodGroup: '',
    dateOfBirth: '',
    weightKg: '',
  });
  const [place, setPlace] = useState({ city: '', latitude: null, longitude: null });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />;

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const validate = () => {
    const e = {};
    if (!form.name.trim()) e.name = 'Enter your name';
    if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) e.email = 'Enter a valid email address';
    if (form.password.length < 8) e.password = 'Password must be at least 8 characters';
    if (!/^[+]?[0-9]{10,13}$/.test(form.phone.trim())) e.phone = 'Enter a 10 to 13 digit phone number';
    if (!place.city.trim()) e.city = 'Enter your city';
    if (role === 'DONOR') {
      if (!form.bloodGroup) e.bloodGroup = 'Choose your blood group';
      const age = ageFrom(form.dateOfBirth);
      if (age === null) e.dateOfBirth = 'Enter your date of birth';
      else if (age < 18 || age > 65) e.dateOfBirth = 'Donors must be between 18 and 65 years old';
      if (form.weightKg && (Number(form.weightKg) < 30 || Number(form.weightKg) > 250)) e.weightKg = 'Enter a weight between 30 and 250 kg';
    }
    return e;
  };

  const submit = async (ev) => {
    ev.preventDefault();
    const e = validate();
    setErrors(e);
    setFormError('');
    if (Object.keys(e).length) return;

    const payload = {
      name: form.name.trim(),
      email: form.email.trim(),
      password: form.password,
      role,
      phone: form.phone.trim(),
      city: place.city.trim(),
      latitude: place.latitude,
      longitude: place.longitude,
    };
    if (role === 'DONOR') {
      payload.bloodGroup = form.bloodGroup;
      payload.dateOfBirth = form.dateOfBirth;
      payload.weightKg = form.weightKg ? Number(form.weightKg) : null;
    }

    setBusy(true);
    try {
      await register(payload);
      toast.success('Account created. Welcome to LifeLink.');
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container auth-wrap">
      <div className="auth-card auth-card-wide">
        <h1>Create your account</h1>

        <fieldset className="role-pick">
          <legend>I want to</legend>
          <label className={`role-option${role === 'DONOR' ? ' is-on' : ''}`}>
            <input type="radio" name="role" value="DONOR" checked={role === 'DONOR'} onChange={() => setRole('DONOR')} />
            <strong>Donate blood</strong>
            <span>Get alerts for requests you can help with.</span>
          </label>
          <label className={`role-option${role === 'REQUESTER' ? ' is-on' : ''}`}>
            <input type="radio" name="role" value="REQUESTER" checked={role === 'REQUESTER'} onChange={() => setRole('REQUESTER')} />
            <strong>Request blood</strong>
            <span>For a patient, as a family member or hospital.</span>
          </label>
        </fieldset>

        <ErrorBox message={formError} />

        <form onSubmit={submit} noValidate>
          <div className="form-grid">
            <Field label={role === 'REQUESTER' ? 'Your name or organisation' : 'Full name'} htmlFor="name" error={errors.name}>
              <input id="name" value={form.name} onChange={set('name')} autoComplete="name" maxLength={80} />
            </Field>
            <Field label="Phone number" htmlFor="phone" error={errors.phone} hint="Shared only with people you work with on a request">
              <input id="phone" type="tel" value={form.phone} onChange={set('phone')} autoComplete="tel" placeholder="9876543210" />
            </Field>
            <Field label="Email" htmlFor="email" error={errors.email}>
              <input id="email" type="email" value={form.email} onChange={set('email')} autoComplete="email" />
            </Field>
            <Field label="Password" htmlFor="password" error={errors.password} hint="At least 8 characters">
              <input id="password" type="password" value={form.password} onChange={set('password')} autoComplete="new-password" />
            </Field>
          </div>

          <CityField value={place} onChange={setPlace} error={errors.city} />

          {role === 'DONOR' && (
            <div className="form-grid">
              <Field label="Blood group" htmlFor="bloodGroup" error={errors.bloodGroup}>
                <select id="bloodGroup" value={form.bloodGroup} onChange={set('bloodGroup')}>
                  <option value="">Choose</option>
                  {BLOOD_GROUPS.map((g) => (
                    <option key={g} value={g}>
                      {g}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Date of birth" htmlFor="dob" error={errors.dateOfBirth}>
                <input id="dob" type="date" value={form.dateOfBirth} onChange={set('dateOfBirth')} />
              </Field>
              <Field label="Weight (kg), optional" htmlFor="weight" error={errors.weightKg} hint="Donors need to weigh at least 45 kg">
                <input id="weight" type="number" min="30" max="250" step="0.5" value={form.weightKg} onChange={set('weightKg')} />
              </Field>
            </div>
          )}

          <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
            {busy ? 'Creating account...' : 'Create account'}
          </button>
        </form>
        <p className="auth-switch">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
