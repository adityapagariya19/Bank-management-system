import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Mail, MessageSquare } from 'lucide-react';
import api, { fmtDate } from '../api';
import { Outbox } from '../types';
import { Logo } from '../components/Logo';

/** Simulated SMS / e-mail gateway viewer (reads the message_outbox table). */
export default function Inbox() {
  const [params] = useSearchParams();
  const [to, setTo] = useState(params.get('to') ?? '');
  const [msgs, setMsgs] = useState<Outbox[]>([]);
  const [err, setErr] = useState('');
  const load = useCallback(async () => {
    if (!to.trim()) return;
    try { setMsgs((await api.get('/dev/outbox', { params: { recipient: to } })).data); setErr(''); } catch { setErr('Simulated inbox is disabled on this server.'); }
  }, [to]);
  useEffect(() => { load(); const t = setInterval(load, 4000); return () => clearInterval(t); }, [load]);
  return (
    <div style={{ maxWidth: 640, margin: '0 auto', padding: 24 }}>
      <Logo />
      <h1 style={{ marginTop: 22 }}>Simulated inbox</h1>
      <p className="muted mb">Local development only: SMS and e-mails are not sent to real phones. Messages from the last 30 minutes appear here (auto-refresh). Enter the email <b>or</b> the mobile number the code was sent to.</p>
      <div className="row mb"><input className="input grow" placeholder="email or mobile number" value={to} onChange={(e) => setTo(e.target.value)} /><button className="btn btn-primary" onClick={load}>Refresh</button></div>
      {err && <p className="err-text">{err}</p>}
      {msgs.length === 0 && !err && <div className="empty">No messages yet for this recipient.</div>}
      {msgs.map((m) => (
        <div key={m.id} className="card mb rise">
          <div className="row between"><span className="row" style={{ gap: 6, fontWeight: 650 }}>{m.channel === 'SMS' ? <MessageSquare size={16} /> : <Mail size={16} />}{m.channel === 'SMS' ? 'SMS' : m.subject}</span><span className="muted">{fmtDate(m.createdAt)}</span></div>
          <p style={{ marginTop: 8 }}>{m.body}</p>
        </div>
      ))}
    </div>
  );
}
