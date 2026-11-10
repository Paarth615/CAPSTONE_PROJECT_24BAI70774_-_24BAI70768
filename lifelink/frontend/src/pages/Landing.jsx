import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import CompatibilityExplorer from '../components/CompatibilityExplorer';
import { BloodTag } from '../components/ui';
import { publicApi } from '../api/endpoints';
import { BLOOD_GROUPS } from '../utils/constants';

export default function Landing() {
  const [stats, setStats] = useState(null);
  const [slow, setSlow] = useState(false);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let alive = true;
    // Free hosting can sleep; tell visitors why the numbers are late.
    const timer = setTimeout(() => alive && setSlow(true), 4000);
    publicApi
      .stats()
      .then((s) => alive && setStats(s))
      .catch(() => alive && setFailed(true))
      .finally(() => clearTimeout(timer));
    return () => {
      alive = false;
      clearTimeout(timer);
    };
  }, []);

  const maxGroup = stats ? Math.max(1, ...Object.values(stats.availableByGroup)) : 1;

  return (
    <>
      <section className="hero">
        <div className="container hero-grid">
          <div className="hero-copy">
            <h1>Every blood request, matched to donors who can safely give.</h1>
            <p className="lede">
              LifeLink checks blood-group compatibility, distance, rest since the last donation and past reliability, then
              alerts the best-matched donors the moment a request is raised.
            </p>
            <div className="hero-cta">
              <Link to="/register?role=REQUESTER" className="btn btn-primary btn-large">
                Request blood
              </Link>
              <Link to="/register?role=DONOR" className="btn btn-outline btn-large">
                Become a donor
              </Link>
            </div>
            <p className="hero-note">Free to use. Donors are only shown requests their blood group can actually help.</p>
          </div>
          <CompatibilityExplorer />
        </div>
      </section>

      <section className="section" aria-labelledby="live-title">
        <div className="container">
          <h2 id="live-title">Donors available right now</h2>
          {!stats && !failed && (
            <p className="muted">
              {slow ? 'The server is waking up. This can take up to a minute on the first visit.' : 'Loading live numbers...'}
            </p>
          )}
          {failed && <p className="muted">Live numbers are unavailable right now.</p>}
          {stats && (
            <div className="live-grid">
              <dl className="numbers">
                <div>
                  <dt>Available donors</dt>
                  <dd>{stats.availableDonors}</dd>
                </div>
                <div>
                  <dt>Open requests</dt>
                  <dd>{stats.openRequests}</dd>
                </div>
                <div>
                  <dt>Donations completed</dt>
                  <dd>{stats.donationsCompleted}</dd>
                </div>
                <div>
                  <dt>Cities covered</dt>
                  <dd>{stats.cities}</dd>
                </div>
              </dl>
              <ul className="group-bars" aria-label="Available donors by blood group">
                {BLOOD_GROUPS.map((g) => {
                  const n = stats.availableByGroup[g] || 0;
                  return (
                    <li key={g}>
                      <BloodTag group={g} size="sm" />
                      <div className="group-bar-track">
                        <div className="group-bar-fill" style={{ width: `${(n / maxGroup) * 100}%` }} />
                      </div>
                      <span className="group-bar-n">{n}</span>
                    </li>
                  );
                })}
              </ul>
            </div>
          )}
        </div>
      </section>

      <section className="section section-tint" aria-labelledby="how-title">
        <div className="container">
          <h2 id="how-title">How a request moves</h2>
          <ol className="steps">
            <li>
              <h3>Raise a request</h3>
              <p>Enter the patient&apos;s blood group, units, hospital, urgency and the date blood is needed.</p>
            </li>
            <li>
              <h3>Smart matching ranks donors</h3>
              <p>
                Only compatible, rested, available donors are considered. They are scored out of 100 and the top matches
                are alerted straight away.
              </p>
            </li>
            <li>
              <h3>Donor offers, requester confirms</h3>
              <p>
                A donor offers to help, the requester accepts, and once the donation is done the donor&apos;s rest period
                starts and the request moves toward fulfilled.
              </p>
            </li>
          </ol>
        </div>
      </section>

      <section className="section" aria-labelledby="rules-title">
        <div className="container rules">
          <div>
            <h2 id="rules-title">Safety rules are built in</h2>
            <p className="muted">Donors who do not meet these rules are never matched or allowed to respond.</p>
          </div>
          <ul className="rule-list">
            <li>
              <strong>Age 18 to 65</strong>
              <span>Checked against the date of birth on the donor profile.</span>
            </li>
            <li>
              <strong>At least 45 kg</strong>
              <span>Applied whenever a weight is on file.</span>
            </li>
            <li>
              <strong>90 days between donations</strong>
              <span>Starts automatically when a donation is confirmed.</span>
            </li>
            <li>
              <strong>Donor controls availability</strong>
              <span>Switch off at any time and you stop receiving alerts.</span>
            </li>
          </ul>
        </div>
      </section>
    </>
  );
}
