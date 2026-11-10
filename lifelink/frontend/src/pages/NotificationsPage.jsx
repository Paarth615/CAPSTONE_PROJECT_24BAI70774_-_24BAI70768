import { useNavigate } from 'react-router-dom';
import { useFetch } from '../utils/useFetch';
import { notificationApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import { EmptyState, ErrorBox, Loader, PageHeader } from '../components/ui';
import { timeAgo } from '../utils/format';

export default function NotificationsPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const { data, loading, error, reload } = useFetch(() => notificationApi.list(), []);

  const open = async (n) => {
    try {
      if (!n.seen) await notificationApi.markRead(n.id);
    } catch {
      /* navigating matters more than the read flag */
    }
    if (n.link) navigate(n.link);
    else reload();
  };

  const markAll = async () => {
    try {
      await notificationApi.markAllRead();
      reload();
    } catch (err) {
      toast.error(errorMessage(err));
    }
  };

  return (
    <div className="container page page-narrow">
      <PageHeader title="Alerts" subtitle="Requests that match you and updates on your offers.">
        {data && data.unread > 0 && (
          <button type="button" className="btn btn-outline" onClick={markAll}>
            Mark all as read
          </button>
        )}
      </PageHeader>
      {loading && !data && <Loader />}
      <ErrorBox message={error} onRetry={reload} />
      {data && data.items.length === 0 && <EmptyState title="No alerts yet" text="When a request matches you, or someone replies to yours, it shows up here." />}
      <ul className="stack">
        {data &&
          data.items.map((n) => (
            <li key={n.id}>
              <button type="button" className={`notice${n.seen ? '' : ' is-new'}`} onClick={() => open(n)}>
                <span>{n.message}</span>
                <time dateTime={n.createdAt}>{timeAgo(n.createdAt)}</time>
              </button>
            </li>
          ))}
      </ul>
    </div>
  );
}
