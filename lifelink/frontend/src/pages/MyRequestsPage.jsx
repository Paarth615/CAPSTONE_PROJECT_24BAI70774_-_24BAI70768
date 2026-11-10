import { useFetch } from '../utils/useFetch';
import { requestApi } from '../api/endpoints';
import { EmptyState, ErrorBox, Loader, PageHeader } from '../components/ui';
import RequestCard from '../components/RequestCard';
import { Link } from 'react-router-dom';

export default function MyRequestsPage() {
  const { data, loading, error, reload } = useFetch(() => requestApi.mine(), []);

  return (
    <div className="container page">
      <PageHeader title="My requests" subtitle="Everything you have raised, newest first.">
        <Link to="/requests/new" className="btn btn-primary">
          Raise a request
        </Link>
      </PageHeader>
      {loading && <Loader />}
      <ErrorBox message={error} onRetry={reload} />
      {data && data.length === 0 && (
        <EmptyState title="Nothing here yet" text="Requests you raise will appear here." actionLabel="Raise a request" actionTo="/requests/new" />
      )}
      <div className="card-list card-list-wide">{data && data.map((r) => <RequestCard key={r.id} request={r} showStatus />)}</div>
    </div>
  );
}
