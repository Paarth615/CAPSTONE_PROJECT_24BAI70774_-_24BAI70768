import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useFetch } from '../utils/useFetch';
import { requestApi } from '../api/endpoints';
import { BLOOD_GROUPS } from '../utils/constants';
import { EmptyState, ErrorBox, Loader, PageHeader } from '../components/ui';
import RequestCard from '../components/RequestCard';

export default function RequestsPage() {
  const { user } = useAuth();
  const isDonor = user.role === 'DONOR';
  const [blood, setBlood] = useState('');
  const [city, setCity] = useState('');
  const [urgency, setUrgency] = useState('');
  const [forMe, setForMe] = useState(isDonor);

  const { data, loading, error, reload } = useFetch(
    () => requestApi.list({ blood: blood || undefined, city: city.trim() || undefined, urgency: urgency || undefined, forMe }),
    [blood, city, urgency, forMe]
  );

  return (
    <div className="container page">
      <PageHeader title="Blood requests" subtitle="Open requests, most urgent first." />

      <form className="filters" onSubmit={(e) => e.preventDefault()} aria-label="Filter requests">
        <label>
          Blood group needed
          <select value={blood} onChange={(e) => setBlood(e.target.value)}>
            <option value="">Any</option>
            {BLOOD_GROUPS.map((g) => (
              <option key={g}>{g}</option>
            ))}
          </select>
        </label>
        <label>
          City
          <input value={city} onChange={(e) => setCity(e.target.value)} placeholder="Any city" />
        </label>
        <label>
          Urgency
          <select value={urgency} onChange={(e) => setUrgency(e.target.value)}>
            <option value="">Any</option>
            <option value="CRITICAL">Critical</option>
            <option value="URGENT">Urgent</option>
            <option value="NORMAL">Planned</option>
          </select>
        </label>
        {isDonor && (
          <label className="check">
            <input type="checkbox" checked={forMe} onChange={(e) => setForMe(e.target.checked)} />
            Only requests I can donate to
          </label>
        )}
      </form>

      {loading && <Loader />}
      <ErrorBox message={error} onRetry={reload} />
      {data && data.length === 0 && (
        <EmptyState title="No open requests match" text="Try removing a filter or check back soon." />
      )}
      <div className="card-list card-list-wide">{data && data.map((r) => <RequestCard key={r.id} request={r} />)}</div>
    </div>
  );
}
