import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useFetch } from '../utils/useFetch';
import { adminApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import { BloodTag, ErrorBox, Loader, PageHeader, StatusBadge, UrgencyBadge } from '../components/ui';
import { BLOOD_GROUPS } from '../utils/constants';
import { formatDate } from '../utils/format';

function Overview() {
  const { data, loading, error, reload } = useFetch(() => adminApi.stats(), []);
  if (loading) return <Loader />;
  if (error) return <ErrorBox message={error} onRetry={reload} />;
  const max = Math.max(1, ...Object.values(data.donorsByGroup));
  return (
    <>
      <dl className="numbers numbers-panel">
        <div>
          <dt>Total users</dt>
          <dd>{data.totalUsers}</dd>
        </div>
        <div>
          <dt>Donors</dt>
          <dd>{data.usersByRole.DONOR}</dd>
        </div>
        <div>
          <dt>Requesters</dt>
          <dd>{data.usersByRole.REQUESTER}</dd>
        </div>
        <div>
          <dt>Total requests</dt>
          <dd>{data.totalRequests}</dd>
        </div>
      </dl>
      <div className="two-col">
        <section className="panel">
          <h2>Requests by status</h2>
          <ul className="plain-list">
            {Object.entries(data.requestsByStatus).map(([k, v]) => (
              <li key={k}>
                <StatusBadge status={k} />
                <strong>{v}</strong>
              </li>
            ))}
          </ul>
          <h2 className="mt">Offers by status</h2>
          <ul className="plain-list">
            {Object.entries(data.responsesByStatus).map(([k, v]) => (
              <li key={k}>
                <StatusBadge status={k} />
                <strong>{v}</strong>
              </li>
            ))}
          </ul>
        </section>
        <section className="panel">
          <h2>Registered donors by blood group</h2>
          <ul className="group-bars">
            {BLOOD_GROUPS.map((g) => (
              <li key={g}>
                <BloodTag group={g} size="sm" />
                <div className="group-bar-track">
                  <div className="group-bar-fill" style={{ width: `${((data.donorsByGroup[g] || 0) / max) * 100}%` }} />
                </div>
                <span className="group-bar-n">{data.donorsByGroup[g] || 0}</span>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </>
  );
}

function Users() {
  const toast = useToast();
  const { data, loading, error, reload } = useFetch(() => adminApi.users(), []);
  const [busyId, setBusyId] = useState(null);

  const toggle = async (u) => {
    setBusyId(u.id);
    try {
      await adminApi.setEnabled(u.id, !u.enabled);
      toast.success(u.enabled ? `${u.name} disabled` : `${u.name} enabled`);
      reload();
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setBusyId(null);
    }
  };

  if (loading && !data) return <Loader />;
  if (error) return <ErrorBox message={error} onRetry={reload} />;
  return (
    <div className="table-wrap">
      <table className="table">
        <caption className="sr-only">All users</caption>
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Role</th>
            <th scope="col">Blood</th>
            <th scope="col">City</th>
            <th scope="col">Joined</th>
            <th scope="col">Status</th>
            <th scope="col">
              <span className="sr-only">Action</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {data.map((u) => (
            <tr key={u.id}>
              <td>
                {u.name}
                <br />
                <span className="muted small">{u.email}</span>
              </td>
              <td>{u.role}</td>
              <td>{u.bloodGroup || '-'}</td>
              <td>{u.city}</td>
              <td>{formatDate(u.createdAt?.slice(0, 10))}</td>
              <td>{u.enabled ? <span className="badge badge-ok">Active</span> : <span className="badge badge-muted">Disabled</span>}</td>
              <td>
                {u.role !== 'ADMIN' && (
                  <button type="button" className="btn btn-small btn-ghost" disabled={busyId === u.id} onClick={() => toggle(u)}>
                    {u.enabled ? 'Disable' : 'Enable'}
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Requests() {
  const toast = useToast();
  const { data, loading, error, reload } = useFetch(() => adminApi.requests(), []);

  const remove = async (r) => {
    if (!window.confirm(`Delete the request for ${r.patientName}? This also removes its offers.`)) return;
    try {
      await adminApi.deleteRequest(r.id);
      toast.success('Request deleted');
      reload();
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  if (loading && !data) return <Loader />;
  if (error) return <ErrorBox message={error} onRetry={reload} />;
  return (
    <div className="table-wrap">
      <table className="table">
        <caption className="sr-only">All blood requests</caption>
        <thead>
          <tr>
            <th scope="col">Patient</th>
            <th scope="col">Blood</th>
            <th scope="col">Hospital</th>
            <th scope="col">Urgency</th>
            <th scope="col">Status</th>
            <th scope="col">Offers</th>
            <th scope="col">
              <span className="sr-only">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {data.map((r) => (
            <tr key={r.id}>
              <td>
                <Link to={`/requests/${r.id}`}>{r.patientName}</Link>
              </td>
              <td>{r.bloodGroup}</td>
              <td>
                {r.hospital}, {r.city}
              </td>
              <td>
                <UrgencyBadge urgency={r.urgency} />
              </td>
              <td>
                <StatusBadge status={r.status} />
              </td>
              <td>{r.responseCount}</td>
              <td>
                <button type="button" className="btn btn-small btn-danger" onClick={() => remove(r)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function AdminPage() {
  const [tab, setTab] = useState('overview');
  const tabs = [
    ['overview', 'Overview'],
    ['users', 'Users'],
    ['requests', 'Requests'],
  ];
  return (
    <div className="container page">
      <PageHeader title="Administration" subtitle="Monitor the network and moderate accounts and requests." />
      <div className="segmented" role="tablist" aria-label="Admin sections">
        {tabs.map(([key, label]) => (
          <button key={key} type="button" role="tab" aria-selected={tab === key} className={tab === key ? 'on' : ''} onClick={() => setTab(key)}>
            {label}
          </button>
        ))}
      </div>
      <div className="block">
        {tab === 'overview' && <Overview />}
        {tab === 'users' && <Users />}
        {tab === 'requests' && <Requests />}
      </div>
    </div>
  );
}
