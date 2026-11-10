import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useFetch } from '../utils/useFetch';
import { requestApi, responseApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { BloodTag, EmptyState, ErrorBox, Loader, Progress, StatusBadge, UrgencyBadge } from '../components/ui';
import { dueLabel, formatDate, plural, timeAgo } from '../utils/format';

function ScoreRing({ score }) {
  return (
    <div className="score" style={{ '--pct': `${score}%` }} role="img" aria-label={`Match score ${score} out of 100`}>
      <span>{score}</span>
    </div>
  );
}

function OwnerPanel({ request, onChanged }) {
  const toast = useToast();
  const [tab, setTab] = useState('offers');
  const [busy, setBusy] = useState(false);
  const responses = useFetch(() => requestApi.responses(request.id), [request.id, request.status]);
  const matches = useFetch(() => requestApi.matches(request.id), [request.id, request.status]);
  const open = request.status === 'OPEN';

  const run = async (fn, okMessage) => {
    setBusy(true);
    try {
      await fn();
      if (okMessage) toast.success(okMessage);
      responses.reload();
      matches.reload();
      onChanged();
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const setOffer = (id, status, msg) => run(() => responseApi.setStatus(id, status), msg);

  const alertAgain = () =>
    run(async () => {
      const res = await requestApi.notifyMatches(request.id);
      toast.info(`Alerted ${plural(res.notified, 'matching donor')}`);
    });

  const close = (status) => {
    const text = status === 'CANCELLED' ? 'Cancel this request? Donors who offered will be told.' : 'Mark this request as fulfilled?';
    if (window.confirm(text)) run(() => requestApi.setStatus(request.id, status), status === 'CANCELLED' ? 'Request cancelled' : 'Request marked fulfilled');
  };

  return (
    <section className="panel" aria-labelledby="owner-title">
      <div className="section-head">
        <h2 id="owner-title">Donors for this request</h2>
        {open && (
          <div className="btn-row">
            <button type="button" className="btn btn-small btn-outline" onClick={alertAgain} disabled={busy}>
              Alert top matches again
            </button>
            <button type="button" className="btn btn-small btn-outline" onClick={() => close('FULFILLED')} disabled={busy}>
              Mark fulfilled
            </button>
            <button type="button" className="btn btn-small btn-danger" onClick={() => close('CANCELLED')} disabled={busy}>
              Cancel request
            </button>
          </div>
        )}
      </div>

      <div className="segmented" role="tablist" aria-label="Donor lists">
        <button type="button" role="tab" aria-selected={tab === 'offers'} className={tab === 'offers' ? 'on' : ''} onClick={() => setTab('offers')}>
          Offers{responses.data ? ` (${responses.data.length})` : ''}
        </button>
        <button type="button" role="tab" aria-selected={tab === 'matches'} className={tab === 'matches' ? 'on' : ''} onClick={() => setTab('matches')}>
          Smart matches{matches.data ? ` (${matches.data.length})` : ''}
        </button>
      </div>

      {tab === 'offers' && (
        <>
          {responses.loading && !responses.data && <Loader />}
          <ErrorBox message={responses.error} onRetry={responses.reload} />
          {responses.data && responses.data.length === 0 && (
            <EmptyState title="No offers yet" text="Donors who offer to help will appear here. Check the Smart matches tab to see who was alerted." />
          )}
          <ul className="stack">
            {responses.data &&
              responses.data.map((o) => (
                <li key={o.id} className="offer-row">
                  <BloodTag group={o.donorBloodGroup} size="sm" />
                  <div className="offer-body">
                    <div className="req-top">
                      <h3>{o.donorName}</h3>
                      <StatusBadge status={o.status} />
                    </div>
                    <p className="muted">
                      {o.donorCity} · {o.donorPhone} · {timeAgo(o.createdAt)}
                    </p>
                    {o.message && <p className="quote">{o.message}</p>}
                    <div className="btn-row">
                      {o.status === 'PENDING' && open && (
                        <>
                          <button type="button" className="btn btn-small btn-primary" disabled={busy} onClick={() => setOffer(o.id, 'ACCEPTED', 'Offer accepted. The donor has your contact details.')}>
                            Accept
                          </button>
                          <button type="button" className="btn btn-small btn-ghost" disabled={busy} onClick={() => setOffer(o.id, 'DECLINED', 'Offer declined')}>
                            Decline
                          </button>
                        </>
                      )}
                      {o.status === 'ACCEPTED' && open && (
                        <button type="button" className="btn btn-small btn-primary" disabled={busy} onClick={() => setOffer(o.id, 'COMPLETED', 'Donation recorded. Thank you.')}>
                          Mark as donated
                        </button>
                      )}
                    </div>
                  </div>
                </li>
              ))}
          </ul>
        </>
      )}

      {tab === 'matches' && (
        <>
          {matches.loading && !matches.data && <Loader />}
          <ErrorBox message={matches.error} onRetry={matches.reload} />
          {matches.data && matches.data.length === 0 && (
            <EmptyState title={open ? 'No eligible donors right now' : 'Matching is only for open requests'} text={open ? 'No available donor is compatible and rested. Try again later or widen the search by contacting nearby blood banks.' : undefined} />
          )}
          <ul className="stack">
            {matches.data &&
              matches.data.map((m) => (
                <li key={m.donorId} className="offer-row">
                  <ScoreRing score={m.score} />
                  <div className="offer-body">
                    <div className="req-top">
                      <h3>{m.name}</h3>
                      <BloodTag group={m.bloodGroup} size="sm" />
                    </div>
                    <p className="muted">
                      {m.city}
                      {m.distanceKm != null ? ` · ${m.distanceKm} km` : ''} · {m.phone}
                    </p>
                    <ul className="reasons">
                      {m.reasons.map((r) => (
                        <li key={r}>{r}</li>
                      ))}
                    </ul>
                  </div>
                </li>
              ))}
          </ul>
        </>
      )}
    </section>
  );
}

function DonorPanel({ request, onChanged }) {
  const { user } = useAuth();
  const toast = useToast();
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const mine = useFetch(() => responseApi.mine(), [request.id, request.myResponseStatus]);
  const myOffer = (mine.data || []).find((o) => o.requestId === request.id);

  const open = request.status === 'OPEN';
  let blocker = '';
  if (!open) blocker = 'This request is no longer open.';
  else if (!request.compatibleWithMe) blocker = `Your blood group (${user.bloodGroup}) cannot donate to a ${request.bloodGroup} patient.`;
  else if (!user.eligible) blocker = user.eligibilityNote;

  const offer = async () => {
    setBusy(true);
    try {
      await requestApi.respond(request.id, message.trim() || null);
      toast.success('Offer sent. The requester has been notified.');
      onChanged();
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const withdraw = async () => {
    setBusy(true);
    try {
      await responseApi.setStatus(myOffer.id, 'WITHDRAWN');
      toast.success('Offer withdrawn');
      onChanged();
    } catch (err) {
      toast.error(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const active = request.myResponseStatus && request.myResponseStatus !== 'WITHDRAWN';

  return (
    <section className="panel" aria-labelledby="donor-title">
      <h2 id="donor-title">Can you help?</h2>
      {active ? (
        <>
          <p>
            Your offer: <StatusBadge status={request.myResponseStatus} />
          </p>
          {request.myResponseStatus === 'ACCEPTED' && (
            <p className="callout">
              Accepted. Call {request.contactPhone} to arrange the donation at {request.hospital}.
            </p>
          )}
          {myOffer && (request.myResponseStatus === 'PENDING' || request.myResponseStatus === 'ACCEPTED') && (
            <button type="button" className="btn btn-ghost" disabled={busy} onClick={withdraw}>
              Withdraw offer
            </button>
          )}
        </>
      ) : blocker ? (
        <p className="callout callout-warn">{blocker}</p>
      ) : (
        <>
          <p className="muted">Tell the requester when you can reach the hospital. They will see your phone number once they accept.</p>
          <div className="field">
            <label htmlFor="msg">Message (optional)</label>
            <textarea id="msg" rows={3} maxLength={500} value={message} onChange={(e) => setMessage(e.target.value)} placeholder="e.g. I can come this evening after 6" />
          </div>
          <button type="button" className="btn btn-primary btn-large" disabled={busy} onClick={offer}>
            {busy ? 'Sending...' : 'Offer to donate'}
          </button>
        </>
      )}
    </section>
  );
}

export default function RequestDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const { data: r, loading, error, reload } = useFetch(() => requestApi.get(id), [id]);

  if (loading && !r) return <Loader />;
  if (error && !r) {
    return (
      <div className="container page">
        <ErrorBox message={error} onRetry={reload} />
        <p>
          <Link to="/requests">Back to requests</Link>
        </p>
      </div>
    );
  }
  if (!r) return null;

  const isOwner = r.requesterId === user.id || user.role === 'ADMIN';
  const open = r.status === 'OPEN';

  return (
    <div className="container page">
      <p className="crumb">
        <Link to={isOwner && user.role !== 'ADMIN' ? '/my-requests' : '/requests'}>Back</Link>
      </p>

      <div className={`panel detail-head req-${r.urgency.toLowerCase()}`}>
        <BloodTag group={r.bloodGroup} size="lg" />
        <div className="detail-main">
          <div className="req-top">
            <h1>{r.patientName}</h1>
            <div className="req-badges">
              <StatusBadge status={r.status} />
              {open && <UrgencyBadge urgency={r.urgency} />}
            </div>
          </div>
          <p className="muted">
            {r.hospital}, {r.city}
            {r.distanceKm != null ? ` · ${r.distanceKm} km from you` : ''}
          </p>
          <dl className="facts">
            <div>
              <dt>Needed by</dt>
              <dd>
                {formatDate(r.requiredBy)}
                {open && <span className={r.urgency === 'CRITICAL' ? ' due due-hot' : ' due'}> · {dueLabel(r.requiredBy)}</span>}
              </dd>
            </div>
            <div>
              <dt>Units</dt>
              <dd>
                {r.unitsFulfilled} of {r.unitsRequired} received
              </dd>
            </div>
            <div>
              <dt>Raised by</dt>
              <dd>{r.requesterName}</dd>
            </div>
            <div>
              <dt>Hospital contact</dt>
              <dd>{r.contactPhone}</dd>
            </div>
          </dl>
          <Progress value={r.unitsFulfilled} max={r.unitsRequired} label="Units received" />
          {r.notes && <p className="quote">{r.notes}</p>}
        </div>
      </div>

      {isOwner ? <OwnerPanel request={r} onChanged={reload} /> : user.role === 'DONOR' ? <DonorPanel request={r} onChanged={reload} /> : null}
    </div>
  );
}
