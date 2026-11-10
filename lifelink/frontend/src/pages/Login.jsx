import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { errorMessage, fieldErrors } from '../api/client';
import { ErrorBox, Field } from '../components/ui';
import { DEMO_ACCOUNTS } from '../utils/constants';

const SHOW_DEMO = import.meta.env.VITE_SHOW_DEMO !== 'false';

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />;

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e, creds = form) => {
    e?.preventDefault();
    const next = {};
    if (!creds.email.trim()) next.email = 'Enter your email address';
    if (!creds.password) next.password = 'Enter your password';
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length) return;

    setBusy(true);
    try {
      const u = await login(creds.email.trim(), creds.password);
      const back = location.state?.from;
      navigate(back && back !== '/login' ? back : u.role === 'ADMIN' ? '/admin' : '/dashboard', { replace: true });
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const tryDemo = (acct) => {
    setForm({ email: acct.email, password: acct.password });
    submit(null, { email: acct.email, password: acct.password });
  };

  return (
    <div className="container auth-wrap">
      <div className="auth-card">
        <h1>Sign in</h1>
        {params.get('expired') && (
          <div className="alert alert-info" role="status">
            Your session ended. Sign in again to continue.
          </div>
        )}
        <ErrorBox message={formError} />
        <form onSubmit={submit} noValidate>
          <Field label="Email" htmlFor="email" error={errors.email}>
            <input id="email" type="email" autoComplete="email" value={form.email} onChange={set('email')} />
          </Field>
          <Field label="Password" htmlFor="password" error={errors.password}>
            <input id="password" type="password" autoComplete="current-password" value={form.password} onChange={set('password')} />
          </Field>
          <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
            {busy ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
        <p className="auth-switch">
          New to LifeLink? <Link to="/register">Create an account</Link>
        </p>

        {SHOW_DEMO && (
          <div className="demo-box">
            <h2>Try a demo account</h2>
            <p className="muted">One click signs you in with sample data.</p>
            <div className="demo-buttons">
              {DEMO_ACCOUNTS.map((a) => (
                <button key={a.label} type="button" className="btn btn-small btn-outline" disabled={busy} onClick={() => tryDemo(a)}>
                  {a.label}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
