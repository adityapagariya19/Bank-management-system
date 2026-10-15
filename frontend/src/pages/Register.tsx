import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errMsg } from '../api';
import { useToast } from '../ui';
import AuthShell from './AuthShell';

export default function Register() {
  const nav = useNavigate();
  const toast = useToast();
  const [f, setF] = useState({ fullName: '', email: '', phone: '', password: '' });
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value });
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try { const r = await api.post('/auth/register', f); toast.ok(r.data.message); nav(`/verify?id=${encodeURIComponent(f.email)}`); }
    catch (err) { toast.err(errMsg(err)); } finally { setBusy(false); }
  };
  return (
    <AuthShell>
      <h1>Create your account</h1>
      <p className="muted mb">Verify with a one-time code, complete KYC, then link bank accounts.</p>
      <form onSubmit={submit}>
        <div className="field"><label>Full name (as per ID)</label><input className="input" value={f.fullName} onChange={set('fullName')} required /></div>
        <div className="field"><label>Email</label><input className="input" type="email" value={f.email} onChange={set('email')} required /></div>
        <div className="field"><label>Mobile number</label><input className="input" inputMode="numeric" maxLength={10} value={f.phone} onChange={set('phone')} placeholder="10-digit number" required /></div>
        <div className="field"><label>Password</label><input className="input" type="password" value={f.password} onChange={set('password')} required /><span className="hint muted">Minimum 8 characters with letters and numbers</span></div>
        <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Please wait…' : 'Continue'}</button>
      </form>
      <p className="muted center mt">Already registered? <Link to="/login" className="link">Sign in</Link></p>
    </AuthShell>
  );
}
