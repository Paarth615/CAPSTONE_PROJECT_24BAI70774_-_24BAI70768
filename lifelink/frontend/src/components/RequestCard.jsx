import { Link } from 'react-router-dom';
import { BloodTag, Progress, StatusBadge, UrgencyBadge } from './ui';
import { dueLabel, plural } from '../utils/format';

export default function RequestCard({ request: r, showStatus = false }) {
  const open = r.status === 'OPEN';
  return (
    <Link to={`/requests/${r.id}`} className={`req-card req-${r.urgency.toLowerCase()}${open ? '' : ' req-closed'}`}>
      <BloodTag group={r.bloodGroup} size="md" />
      <div className="req-body">
        <div className="req-top">
          <h3>{r.patientName}</h3>
          <div className="req-badges">
            {showStatus && <StatusBadge status={r.status} />}
            {open && <UrgencyBadge urgency={r.urgency} />}
          </div>
        </div>
        <p className="req-place">
          {r.hospital}, {r.city}
          {r.distanceKm != null && <span className="req-distance"> · {r.distanceKm} km away</span>}
        </p>
        <div className="req-meta">
          <span className={r.urgency === 'CRITICAL' && open ? 'due due-hot' : 'due'}>{dueLabel(r.requiredBy)}</span>
          <span>
            {r.unitsFulfilled} of {plural(r.unitsRequired, 'unit')} received
          </span>
        </div>
        <Progress value={r.unitsFulfilled} max={r.unitsRequired} label="Units received" />
        {r.compatibleWithMe && open && !r.myResponseStatus && <p className="req-hint">Your blood group can help with this</p>}
        {r.myResponseStatus && (
          <p className="req-hint">
            Your offer: <StatusBadge status={r.myResponseStatus} />
          </p>
        )}
      </div>
    </Link>
  );
}
