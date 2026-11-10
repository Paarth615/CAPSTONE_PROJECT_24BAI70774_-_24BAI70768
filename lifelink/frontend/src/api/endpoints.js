import { api } from './client';

const data = (p) => p.then((r) => r.data);

export const authApi = {
  register: (body) => data(api.post('/auth/register', body)),
  login: (body) => data(api.post('/auth/login', body)),
};

export const userApi = {
  me: () => data(api.get('/users/me')),
  update: (body) => data(api.put('/users/me', body)),
  setAvailability: (available) => data(api.put('/users/me/availability', { available })),
};

export const requestApi = {
  create: (body) => data(api.post('/requests', body)),
  list: (params) => data(api.get('/requests', { params })),
  mine: () => data(api.get('/requests/mine')),
  get: (id) => data(api.get(`/requests/${id}`)),
  matches: (id) => data(api.get(`/requests/${id}/matches`)),
  responses: (id) => data(api.get(`/requests/${id}/responses`)),
  notifyMatches: (id) => data(api.post(`/requests/${id}/notify-matches`)),
  setStatus: (id, status) => data(api.put(`/requests/${id}/status`, { status })),
  respond: (id, message) => data(api.post(`/requests/${id}/respond`, { message })),
};

export const responseApi = {
  mine: () => data(api.get('/responses/mine')),
  setStatus: (id, status) => data(api.put(`/responses/${id}/status`, { status })),
};

export const notificationApi = {
  list: () => data(api.get('/notifications')),
  markRead: (id) => api.put(`/notifications/${id}/read`),
  markAllRead: () => api.put('/notifications/read-all'),
};

export const publicApi = {
  stats: () => data(api.get('/public/stats')),
};

export const adminApi = {
  stats: () => data(api.get('/admin/stats')),
  users: () => data(api.get('/admin/users')),
  setEnabled: (id, enabled) => data(api.put(`/admin/users/${id}/enabled`, { enabled })),
  requests: () => data(api.get('/admin/requests')),
  deleteRequest: (id) => api.delete(`/admin/requests/${id}`),
};
