import { FormEvent, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { KeyRound, Smartphone } from 'lucide-react';
import api, { errCode, errMsg } from '../api';
import { useAuth } from '../auth';
import { useToast } from '../ui';
import AuthShell from './AuthShell';

export default function Login() {
  const { signIn } = useAuth();
  const nav = useNavigate();
  const toast = useToast();
  const [params] = useSearchParams();
  const [mode, setMode] = useState<'pw' | 'otp'>('pw');
  const [id, setId] = useState('');
  const [pw, setPw] = useState('');
  const [code, setCode] = useState('');
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);

  const wrap = async (fn: () => Promise<void>) => {
    setBusy(true);
    try { await fn(); } catch (e) {
      if (errCode(e) === 'NEEDS_VERIFICATION') { toast.err(errMsg(e)); nav(`/verify?id=${encodeURIComponent(id)}&resend=1`); }
      else toast.err(errMsg(e));
    } finally { setBusy(false); }
  };
  const passwordLogin = (e: FormEvent) => { e.preventDefault(); wrap(async () => { signIn((await api.post('/auth/login', { identifier: id, password: pw })).data); nav('/'); }); };
  const sendOtp = (e: FormEvent) => { e.preventDefault(); wrap(async () => { const r = await api.post('/auth/login/otp/request', { identifier: id }); toast.ok(r.data.message); setSent(true); }); };
  const verifyOtp = (e: FormEvent) => { e.preventDefault(); wrap(async () => { signIn((await api.post('/auth/login/otp/verify', { identifier: id, code })).data); nav('/'); }); };

  return (
    <AuthShell>
      <h1>Sign in</h1>
      <p className="muted mb">{params.get('expired') ? 'Your session expired. Please sign in again.' : 'Welcome back. Choose how you want to sign in.'}</p>
      <div className="tabs" style={{ width: '100%' }}>
        <button className={`tab ${mode === 'pw' ? 'on' : ''}`} style={{ flex: 1, justifyContent: 'center' }} onClick={() => { setMode('pw'); setSent(false); }}><KeyRound size={15} /> Password</button>
        <button className={`tab ${mode === 'otp' ? 'on' : ''}`} style={{ flex: 1, justifyContent: 'center' }} onClick={() => { setMode('otp'); setSent(false); }}><Smartphone size={15} /> OTP</button>
      </div>
      {mode === 'pw' ? (
        <form onSubmit={passwordLogin}>
          <div className="field"><label>Email or mobile number</label><input className="input" value={id} onChange={(e) => setId(e.target.value)} autoFocus required /></div>
          <div className="field"><label>Password</label><input className="input" type="password" value={pw} onChange={(e) => setPw(e.target.value)} required /></div>
          <div className="row between mb"><span /><Link to="/forgot" className="link">Forgot password?</Link></div>
          <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        </form>
      ) : !sent ? (
        <form onSubmit={sendOtp}>
          <div className="field"><label>Email or mobile number</label><input className="input" value={id} onChange={(e) => setId(e.target.value)} autoFocus required /></div>
          <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Sending…' : 'Send one-time code'}</button>
        </form>
      ) : (
        <form onSubmit={verifyOtp}>
          <div className="field"><label>Enter the 6-digit code</label><input className="input otp" inputMode="numeric" maxLength={6} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} autoFocus required /></div>
          <button className="btn btn-primary btn-block" disabled={busy || code.length !== 6}>{busy ? 'Verifying…' : 'Verify & sign in'}</button>
          <p className="muted center mt">Code sent to {id}. <a className="link" href={`/inbox?to=${encodeURIComponent(id)}`} target="_blank" rel="noreferrer">Open simulated inbox</a></p>
        </form>
      )}
      <div className="divider">New to Meridian?</div>
      <Link to="/register" className="btn btn-block">Create an account</Link>
    </AuthShell>
  );
}
