import { Link } from 'react-router-dom';

export function Loader({ label = 'Loading' }) {
  return (
    <div className="loader" role="status" aria-live="polite">
      <span className="loader-drop" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

export function ErrorBox({ message, onRetry }) {
  if (!message) return null;
  return (
    <div className="alert alert-error" role="alert">
      <span>{message}</span>
      {onRetry && (
        <button type="button" className="btn btn-small btn-ghost" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ title, text, actionLabel, actionTo }) {
  return (
    <div className="empty">
      <h3>{title}</h3>
      {text && <p>{text}</p>}
      {actionLabel && actionTo && (
        <Link to={actionTo} className="btn btn-primary">
          {actionLabel}
        </Link>
      )}
    </div>
  );
}

export function Field({ label, error, hint, htmlFor, children }) {
  return (
    <div className={`field${error ? ' field-invalid' : ''}`}>
      <label htmlFor={htmlFor}>{label}</label>
      {children}
      {hint && !error && <p className="field-hint">{hint}</p>}
      {error && (
        <p className="field-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

const STATUS_TONES = {
  OPEN: 'info',
  FULFILLED: 'ok',
  CANCELLED: 'muted',
  EXPIRED: 'muted',
  PENDING: 'plasma',
  ACCEPTED: 'info',
  COMPLETED: 'ok',
  DECLINED: 'muted',
  WITHDRAWN: 'muted',
};

const STATUS_LABELS = {
  OPEN: 'Open',
  FULFILLED: 'Fulfilled',
  CANCELLED: 'Cancelled',
  EXPIRED: 'Expired',
  PENDING: 'Waiting for reply',
  ACCEPTED: 'Accepted',
  COMPLETED: 'Donated',
  DECLINED: 'Declined',
  WITHDRAWN: 'Withdrawn',
};

export function StatusBadge({ status }) {
  return <span className={`badge badge-${STATUS_TONES[status] || 'muted'}`}>{STATUS_LABELS[status] || status}</span>;
}

export function UrgencyBadge({ urgency }) {
  const tone = urgency === 'CRITICAL' ? 'red' : urgency === 'URGENT' ? 'plasma' : 'muted';
  const label = urgency === 'CRITICAL' ? 'Critical' : urgency === 'URGENT' ? 'Urgent' : 'Planned';
  return <span className={`badge badge-${tone}`}>{label}</span>;
}

/** Blood group shown as a drop-shaped tag, like the label on a blood bag. */
export function BloodTag({ group, size = 'md', state }) {
  return (
    <span className={`drop drop-${size}${state ? ` drop-${state}` : ''}`} aria-label={`Blood group ${group}`}>
      <span className="drop-text">{group}</span>
    </span>
  );
}

export function Progress({ value, max, label }) {
  const pct = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0;
  return (
    <div className="progress" role="progressbar" aria-valuemin={0} aria-valuemax={max} aria-valuenow={value} aria-label={label}>
      <div className="progress-bar" style={{ width: `${pct}%` }} />
    </div>
  );
}

export function PageHeader({ title, subtitle, children }) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="subtitle">{subtitle}</p>}
      </div>
      {children && <div className="page-header-actions">{children}</div>}
    </header>
  );
}
