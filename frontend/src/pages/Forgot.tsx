import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errMsg } from '../api';
import { useToast } from '../ui';
import AuthShell from './AuthShell';

export default function Forgot() {
  const nav = useNavigate();
  const toast = useToast();
  const [id, setId] = useState('');
  const [code, setCode] = useState('');
  const [pw, setPw] = useState('');
  const [step, setStep] = useState(1);
  const [busy, setBusy] = useState(false);
  const run = async (fn: () => Promise<void>) => { setBusy(true); try { await fn(); } catch (e) { toast.err(errMsg(e)); } finally { setBusy(false); } };
  const send = (e: FormEvent) => { e.preventDefault(); run(async () => { const r = await api.post('/auth/password/forgot', { identifier: id }); toast.ok(r.data.message); setStep(2); }); };
  const reset = (e: FormEvent) => { e.preventDefault(); run(async () => { await api.post('/auth/password/reset', { identifier: id, code, newPassword: pw }); toast.ok('Password updated. Please sign in.'); nav('/login'); }); };
  return (
    <AuthShell>
      <h1>Reset password</h1>
      <p className="muted mb">{step === 1 ? 'We will send a one-time code to your registered mobile and email.' : 'Enter the code and choose a new password.'}</p>
      {step === 1 ? (
        <form onSubmit={send}>
          <div className="field"><label>Email or mobile number</label><input className="input" value={id} onChange={(e) => setId(e.target.value)} autoFocus required /></div>
          <button className="btn btn-primary btn-block" disabled={busy}>Send code</button>
        </form>
      ) : (
        <form onSubmit={reset}>
          <div className="field"><label>6-digit code</label><input className="input otp" inputMode="numeric" maxLength={6} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} required /></div>
          <div className="field"><label>New password</label><input className="input" type="password" value={pw} onChange={(e) => setPw(e.target.value)} required /></div>
          <button className="btn btn-primary btn-block" disabled={busy || code.length !== 6}>Update password</button>
          <p className="muted center mt"><a className="link" href={`/inbox?to=${encodeURIComponent(id)}`} target="_blank" rel="noreferrer">Open simulated inbox</a></p>
        </form>
      )}
      <p className="muted center mt"><Link to="/login" className="link">Back to sign in</Link></p>
    </AuthShell>
  );
}
