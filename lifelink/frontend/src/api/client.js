import axios from 'axios';

const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');
export const TOKEN_KEY = 'lifelink_token';

export const api = axios.create({
  baseURL: `${API_URL}/api`,
  // Free hosting tiers can take up to a minute to wake the server on the first request.
  timeout: 70000,
});

api.interceptors.request.use((config) => {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) config.headers.Authorization = `Bearer ${token}`;
  } catch {
    /* storage unavailable */
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isAuthCall = err.config?.url?.startsWith('/auth/');
    if (err.response?.status === 401 && !isAuthCall) {
      try {
        localStorage.removeItem(TOKEN_KEY);
      } catch {
        /* ignore */
      }
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login?expired=1');
      }
    }
    return Promise.reject(err);
  }
);

/** Human-readable message for any failed request. */
export function errorMessage(err) {
  if (err?.response?.data?.message) return err.response.data.message;
  if (err?.code === 'ECONNABORTED') return 'The server took too long to answer. Please try again.';
  if (err?.request && !err.response) return 'Cannot reach the server. Check your connection and try again.';
  return 'Something went wrong. Please try again.';
}

/** Field-level validation messages from the API, e.g. { email: "Enter a valid email address" }. */
export function fieldErrors(err) {
  return err?.response?.data?.errors || {};
}
