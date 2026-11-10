import { Link } from 'react-router-dom';
import { EmptyState } from '../components/ui';

export default function NotFound() {
  return (
    <div className="container page">
      <EmptyState title="That page does not exist" text="Check the address, or go back to the start." />
      <p className="center">
        <Link to="/" className="btn btn-primary">
          Go to home
        </Link>
      </p>
    </div>
  );
}
