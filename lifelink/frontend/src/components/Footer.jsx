import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <p>
          <strong>LifeLink</strong> connects blood requests with eligible donors. It does not replace a hospital blood
          bank. In an emergency, call your local emergency number first.
        </p>
        <ul className="footer-links">
          <li>
            <Link to="/register">Become a donor</Link>
          </li>
          <li>
            <Link to="/login">Sign in</Link>
          </li>
        </ul>
      </div>
    </footer>
  );
}
