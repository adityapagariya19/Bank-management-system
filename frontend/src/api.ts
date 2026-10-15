import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((c) => {
  const t = localStorage.getItem('token');
  if (t) c.headers.Authorization = `Bearer ${t}`;
  return c;
});

api.interceptors.response.use(
  (r) => r,
  (e) => {
    const url: string = e.config?.url ?? '';
    if (e.response?.status === 401 && !url.startsWith('/auth') && !location.pathname.startsWith('/login')) {
      localStorage.removeItem('token');
      location.href = '/login?expired=1';
    }
    return Promise.reject(e);
  },
);
export default api;

export const errMsg = (e: any): string =>
  e?.response?.data?.message ?? (e?.code === 'ERR_NETWORK' || e?.response?.status === 502 || e?.response?.status === 504
    ? 'Cannot reach the server. Make sure the backend is running on port 8080.' : 'Something went wrong. Please try again.');
export const errCode = (e: any): string | undefined => e?.response?.data?.code;

const fmt = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
export const inr = (n: number | null | undefined) => fmt.format(n ?? 0);
export const fmtDate = (s: string | null | undefined) =>
  s ? new Date(s).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—';
export const fmtDay = (s: string | null | undefined) => (s ? new Date(s).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—');
export const maskAcc = (n: string) => 'XXXX ' + n.slice(-4);
export const initials = (n: string) => n.split(' ').map((w) => w[0]).slice(0, 2).join('').toUpperCase();
export const label = (s: string) => s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
