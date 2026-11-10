import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useFetch } from '../utils/useFetch';
import { requestApi, responseApi, userApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { EmptyState, ErrorBox, Loader, PageHeader, StatusBadge } from '../components/ui';
import RequestCard from '../components/RequestCard';
import { formatDate } from '../utils/format';

function DonorDashboard() {
  const { user, setUser } = useAuth();
  const toast = useToast();
  const feed = useFetch(() => requestApi.list({ forMe: true }), []);
  const mine = useFetch(() => responseApi.mine(), []);
  const [busy, setBusy] = useState(false);

  const toggle = async () => {
    setBusy(true);
    try {
      const u = await userApi.setAvailability(!user.available);
      setUser(u);
      toast.success(u.available ? 'You are available for requests' : 'You will not receive new alerts');
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const pending = (mine.data || []).filter((r) => r.status === 'PENDING' || r.status === 'ACCEPTED').length;

  return (
    <>
      <PageHeader title={`Hello, ${user.name.split(' ')[0]}`} subtitle="Requests your blood group can help with, most urgent first." />

      <div className="dash-grid">
        <section className={`panel eligibility ${user.eligible ? 'is-ready' : 'is-wait'}`} aria-labelledby="elig-title">
          <h2 id="elig-title">{user.eligible ? 'You can donate now' : 'Not able to donate right now'}</h2>
          <p>{user.eligible ? 'You are visible to matching and can respond to requests.' : user.eligibilityNote}</p>
          <dl className="facts">
            <div>
              <dt>Blood group</dt>
              <dd>{user.bloodGroup}</dd>
            </div>
            <div>
              <dt>Last donation</dt>
              <dd>{user.lastDonationDate ? formatDate(user.lastDonationDate) : 'None yet'}</dd>
            </div>
            <div>
              <dt>Donations on LifeLink</dt>
              <dd>{user.completedDonations}</dd>
            </div>
            <div>
              <dt>Active offers</dt>
              <dd>{pending}</dd>
            </div>
          </dl>
          <button type="button" className={`btn ${user.available ? 'btn-outline' : 'btn-primary'}`} onClick={toggle} disabled={busy}>
            {user.available ? 'Pause alerts' : 'Resume alerts'}
          </button>
        </section>

        <section aria-labelledby="feed-title">
          <div className="section-head">
            <h2 id="feed-title">Requests for you</h2>
            <Link to="/requests">See all requests</Link>
          </div>
          {feed.loading && <Loader />}
          <ErrorBox message={feed.error} onRetry={feed.reload} />
          {feed.data && feed.data.length === 0 && (
            <EmptyState title="No matching requests right now" text="You will get an alert when someone needs your blood group." />
          )}
          <div className="card-list">{feed.data && feed.data.slice(0, 4).map((r) => <RequestCard key={r.id} request={r} />)}</div>
        </section>
      </div>

      {mine.data && mine.data.length > 0 && (
        <section aria-labelledby="offers-title" className="block">
          <div className="section-head">
            <h2 id="offers-title">Your recent offers</h2>
            <Link to="/my-donations">All donations</Link>
          </div>
          <ul className="plain-list">
            {mine.data.slice(0, 3).map((o) => (
              <li key={o.id}>
                <Link to={`/requests/${o.requestId}`}>
                  {o.patientName} · {o.hospital}, {o.requestCity}
                </Link>
                <StatusBadge status={o.status} />
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}

function RequesterDashboard() {
  const { user } = useAuth();
  const mine = useFetch(() => requestApi.mine(), []);
  const list = mine.data || [];
  const open = list.filter((r) => r.status === 'OPEN');
  const fulfilled = list.filter((r) => r.status === 'FULFILLED');
  const offers = open.reduce((n, r) => n + r.responseCount, 0);

  return (
    <>
      <PageHeader title={`Hello, ${user.name.split(' ')[0]}`} subtitle="Track your requests and the donors responding to them.">
        <Link to="/requests/new" className="btn btn-primary">
          Raise a request
        </Link>
      </PageHeader>

      <dl className="numbers numbers-panel">
        <div>
          <dt>Open requests</dt>
          <dd>{open.length}</dd>
        </div>
        <div>
          <dt>Donor offers received</dt>
          <dd>{offers}</dd>
        </div>
        <div>
          <dt>Fulfilled</dt>
          <dd>{fulfilled.length}</dd>
        </div>
      </dl>

      <section aria-labelledby="my-title" className="block">
        <div className="section-head">
          <h2 id="my-title">Your requests</h2>
          <Link to="/my-requests">View all</Link>
        </div>
        {mine.loading && <Loader />}
        <ErrorBox message={mine.error} onRetry={mine.reload} />
        {mine.data && list.length === 0 && (
          <EmptyState title="You have not raised a request yet" text="Raise one and matching donors are alerted immediately." actionLabel="Raise a request" actionTo="/requests/new" />
        )}
        <div className="card-list">{list.slice(0, 4).map((r) => <RequestCard key={r.id} request={r} showStatus />)}</div>
      </section>
    </>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  return <div className="container page">{user.role === 'DONOR' ? <DonorDashboard /> : <RequesterDashboard />}</div>;
}
