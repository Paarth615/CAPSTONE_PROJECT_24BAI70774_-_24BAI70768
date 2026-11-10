import { useEffect, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { notificationApi } from '../api/endpoints';

function linksFor(role) {
  if (role === 'DONOR') {
    return [
      { to: '/dashboard', label: 'Dashboard' },
      { to: '/requests', label: 'Blood requests' },
      { to: '/my-donations', label: 'My donations' },
      { to: '/requests/new', label: 'Raise a request' },
    ];
  }
  if (role === 'REQUESTER') {
    return [
      { to: '/dashboard', label: 'Dashboard' },
      { to: '/requests/new', label: 'New request' },
      { to: '/my-requests', label: 'My requests' },
      { to: '/requests', label: 'All requests' },
    ];
  }
  if (role === 'ADMIN') {
    return [
      { to: '/admin', label: 'Admin' },
      { to: '/requests', label: 'All requests' },
    ];
  }
  return [];
}

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);

  useEffect(() => setOpen(false), [location.pathname]);

  // Poll for new notifications while signed in
  useEffect(() => {
    if (!user) {
      setUnread(0);
      return undefined;
    }
    let alive = true;
    const load = () =>
      notificationApi
        .list()
        .then((p) => alive && setUnread(p.unread))
        .catch(() => {});
    load();
    const timer = setInterval(load, 30000);
    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [user, location.pathname]);

  const signOut = () => {
    logout();
    navigate('/');
  };

  return (
    <header className="nav">
      <div className="container nav-inner">
        <Link to={user ? (user.role === 'ADMIN' ? '/admin' : '/dashboard') : '/'} className="brand" aria-label="LifeLink home">
          <svg viewBox="0 0 64 64" width="26" height="26" aria-hidden="true">
            <path d="M32 4C32 4 12 26 12 40a20 20 0 0 0 40 0C52 26 32 4 32 4z" fill="currentColor" />
            <path d="M26 38h12M32 32v12" stroke="#fff" strokeWidth="4" strokeLinecap="round" />
          </svg>
          <span>LifeLink</span>
        </Link>

        <button
          type="button"
          className="nav-toggle"
          aria-expanded={open}
          aria-controls="nav-menu"
          onClick={() => setOpen((v) => !v)}
        >
          {open ? 'Close' : 'Menu'}
        </button>

        <nav id="nav-menu" className={`nav-menu${open ? ' nav-menu-open' : ''}`} aria-label="Main">
          {user && (
            <ul className="nav-links">
              {linksFor(user.role).map((l) => (
                <li key={l.to}>
                  <NavLink to={l.to} end className={({ isActive }) => (isActive ? 'active' : undefined)}>
                    {l.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          )}
          <div className="nav-actions">
            {user ? (
              <>
                <Link to="/notifications" className="bell" aria-label={`Notifications, ${unread} unread`}>
                  Alerts
                  {unread > 0 && <span className="bell-count">{unread > 99 ? '99+' : unread}</span>}
                </Link>
                <Link to="/profile" className="nav-user">
                  {user.name.split(' ')[0]}
                </Link>
                <button type="button" className="btn btn-small btn-ghost" onClick={signOut}>
                  Sign out
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn btn-small btn-ghost">
                  Sign in
                </Link>
                <Link to="/register" className="btn btn-small btn-primary">
                  Create account
                </Link>
              </>
            )}
          </div>
        </nav>
      </div>
    </header>
  );
}
