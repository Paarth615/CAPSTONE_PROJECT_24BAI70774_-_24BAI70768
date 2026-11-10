import { Link } from 'react-router-dom';
import { useFetch } from '../utils/useFetch';
import { responseApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import { BloodTag, EmptyState, ErrorBox, Loader, PageHeader, StatusBadge, UrgencyBadge } from '../components/ui';
import { timeAgo } from '../utils/format';

export default function MyDonationsPage() {
  const toast = useToast();
  const { data, loading, error, reload } = useFetch(() => responseApi.mine(), []);

  const withdraw = async (id) => {
    try {
      await responseApi.setStatus(id, 'WITHDRAWN');
      toast.success('Offer withdrawn');
      reload();
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  return (
    <div className="container page">
      <PageHeader title="My donations" subtitle="Your offers and where each one stands." />
      {loading && <Loader />}
      <ErrorBox message={error} onRetry={reload} />
      {data && data.length === 0 && (
        <EmptyState title="You have not offered to donate yet" text="Browse open requests your blood group can help with." actionLabel="Find requests" actionTo="/requests" />
      )}
      <ul className="stack">
        {data &&
          data.map((o) => (
            <li key={o.id} className="panel offer">
              <BloodTag group={o.requestBloodGroup} />
              <div className="offer-body">
                <div className="req-top">
                  <h3>
                    <Link to={`/requests/${o.requestId}`}>{o.patientName}</Link>
                  </h3>
                  <div className="req-badges">
                    <UrgencyBadge urgency={o.urgency} />
                    <StatusBadge status={o.status} />
                  </div>
                </div>
                <p className="muted">
                  {o.hospital}, {o.requestCity} · offered {timeAgo(o.createdAt)}
                </p>
                {o.status === 'ACCEPTED' && (
                  <p className="callout">
                    Accepted. Call {o.requestContactPhone} to arrange the donation at {o.hospital}.
                  </p>
                )}
                {(o.status === 'PENDING' || o.status === 'ACCEPTED') && (
                  <button type="button" className="btn btn-small btn-ghost" onClick={() => withdraw(o.id)}>
                    Withdraw offer
                  </button>
                )}
              </div>
            </li>
          ))}
      </ul>
    </div>
  );
}
