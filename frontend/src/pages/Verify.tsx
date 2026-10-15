import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api, { errMsg } from '../api';
import { useAuth } from '../auth';
import { useToast } from '../ui';
import AuthShell from './AuthShell';

export default function Verify() {
  const [params] = useSearchParams();
  const id = params.get('id') ?? '';
  const nav = useNavigate();
  const { signIn } = useAuth();
  const toast = useToast();
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [wait, setWait] = useState(30);
  useEffect(() => { if (params.get('resend')) resend(); /* eslint-disable-next-line */ }, []);
  useEffect(() => { if (wait <= 0) return; const t = setTimeout(() => setWait(wait - 1), 1000); return () => clearTimeout(t); }, [wait]);

  const resend = async () => {
    try { const r = await api.post('/auth/register/resend', { identifier: id }); toast.ok(r.data.message); setWait(30); } catch (e) { toast.err(errMsg(e)); }
  };
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try { signIn((await api.post('/auth/register/verify', { identifier: id, code })).data); toast.ok('Account verified. Welcome to Meridian!'); nav('/'); }
    catch (err) { toast.err(errMsg(err)); } finally { setBusy(false); }
  };
  return (
    <AuthShell>
      <h1>Verify your account</h1>
      <p className="muted mb">Enter the 6-digit code sent to your mobile and email.</p>
      <form onSubmit={submit}>
        <div className="field"><label>Verification code</label><input className="input otp" inputMode="numeric" maxLength={6} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} autoFocus required /></div>
        <button className="btn btn-primary btn-block" disabled={busy || code.length !== 6}>{busy ? 'Verifying…' : 'Verify & continue'}</button>
      </form>
      <div className="card mt" style={{ background: '#f8fafc', boxShadow: 'none' }}>
        <b style={{ fontSize: '.86rem' }}>Where is my code?</b>
        <p className="muted" style={{ marginTop: 4 }}>This local build has no real SMS gateway. Messages are written to the database and shown in the simulated inbox.</p>
        <a className="btn btn-sm mt" href={`/inbox?to=${encodeURIComponent(id)}`} target="_blank" rel="noreferrer">Open simulated inbox</a>
      </div>
      <p className="muted center mt">{wait > 0 ? `Resend code in ${wait}s` : <button className="link" onClick={resend}>Resend code</button>}</p>
    </AuthShell>
  );
}
