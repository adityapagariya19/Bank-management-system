import { FormEvent, useEffect, useState } from 'react';
import { CheckCircle2, Trash2 } from 'lucide-react';
import api, { errMsg, fmtDate, fmtDay, label } from '../api';
import { useAuth } from '../auth';
import { Account, Activity, Kyc, Upi } from '../types';
import { PageHead, Pill, useToast } from '../ui';

export default function Profile() {
  const { user, refresh } = useAuth();
  const customer = user!.role === 'CUSTOMER';
  const [tab, setTab] = useState(customer ? 'kyc' : 'security');
  return (
    <>
      <PageHead title="Profile & security" sub={`${user!.fullName} · ${user!.email} · ${user!.phone}`} />
      <div className="tabs">
        {customer && <button className={`tab ${tab === 'kyc' ? 'on' : ''}`} onClick={() => setTab('kyc')}>KYC</button>}
        <button className={`tab ${tab === 'security' ? 'on' : ''}`} onClick={() => setTab('security')}>Security</button>
        {customer && <button className={`tab ${tab === 'upi' ? 'on' : ''}`} onClick={() => setTab('upi')}>UPI IDs</button>}
      </div>
      {tab === 'kyc' && <KycTab refresh={refresh} />}{tab === 'security' && <Security customer={customer} />}{tab === 'upi' && <UpiTab />}
    </>
  );
}

function KycTab({ refresh }: { refresh: () => Promise<void> }) {
  const toast = useToast();
  const [k, setK] = useState<Kyc | null>(null);
  const [f, setF] = useState({ dateOfBirth: '', address: '', city: '', state: '', pincode: '', idType: 'AADHAAR', idNumber: '' });
  const load = () => api.get('/c/kyc').then((r) => setK(r.data));
  useEffect(() => { load(); }, []);
  const set = (key: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setF({ ...f, [key]: e.target.value });
  const submit = async (e: FormEvent) => { e.preventDefault(); try { await api.post('/c/kyc', f); toast.ok('KYC submitted for review'); load(); refresh(); } catch (err) { toast.err(errMsg(err)); } };
  if (!k) return null;
  const form = k.status === 'NOT_SUBMITTED' || k.status === 'REJECTED';
  return (
    <div className="grid g2">
      <div className="card"><div className="row between mb"><h3 style={{ margin: 0 }}>Identity verification</h3><Pill value={k.status} /></div>
        {k.status === 'REJECTED' && <p className="err-text mb">Rejected: {k.reviewNote}. Please correct and resubmit.</p>}
        {form ? (<form onSubmit={submit}>
          <div className="field"><label>Date of birth</label><input className="input" type="date" value={f.dateOfBirth} onChange={set('dateOfBirth')} required /></div>
          <div className="field"><label>Address</label><input className="input" value={f.address} onChange={set('address')} required /></div>
          <div className="grid g2"><div className="field"><label>City</label><input className="input" value={f.city} onChange={set('city')} required /></div><div className="field"><label>State</label><input className="input" value={f.state} onChange={set('state')} required /></div></div>
          <div className="field"><label>PIN code</label><input className="input" inputMode="numeric" maxLength={6} value={f.pincode} onChange={set('pincode')} required /></div>
          <div className="grid g2"><div className="field"><label>ID type</label><select className="input" value={f.idType} onChange={set('idType')}><option value="AADHAAR">Aadhaar</option><option value="PAN">PAN</option><option value="PASSPORT">Passport</option><option value="VOTER_ID">Voter ID</option></select></div>
            <div className="field"><label>ID number</label><input className="input" value={f.idNumber} onChange={set('idNumber')} required /></div></div>
          <p className="muted mb">Only the last 4 characters of your ID are stored.</p><button className="btn btn-primary btn-block">Submit for verification</button></form>) : (<>
          <div className="kv"><span>Date of birth</span><span>{fmtDay(k.dateOfBirth)}</span></div><div className="kv"><span>Address</span><span>{k.address}, {k.city}, {k.state} {k.pincode}</span></div>
          <div className="kv"><span>ID</span><span>{label(k.idType ?? '')} {k.idNumberMasked}</span></div><div className="kv"><span>Submitted</span><span>{fmtDate(k.submittedAt)}</span></div>
          {k.reviewedBy && <div className="kv"><span>Reviewed by</span><span>{k.reviewedBy} · {fmtDate(k.reviewedAt)}</span></div>}</>)}</div>
      <div className="card"><h3>How verification works</h3>
        {['Submit your details', 'A bank manager reviews them', 'Link bank accounts & start paying'].map((s, i) => <div key={s} className="row mb"><CheckCircle2 size={20} color={k.status === 'APPROVED' || (i === 0 && k.status !== 'NOT_SUBMITTED') ? '#15803d' : '#cbd5e1'} />{s}</div>)}</div>
    </div>
  );
}

function Security({ customer }: { customer: boolean }) {
  const toast = useToast();
  const [cur, setCur] = useState('');
  const [nw, setNw] = useState('');
  const [act, setAct] = useState<Activity[]>([]);
  useEffect(() => { if (customer) api.get('/c/security/activity').then((r) => setAct(r.data)); }, [customer]);
  const change = async (e: FormEvent) => { e.preventDefault(); try { await api.post('/me/password', { currentPassword: cur, newPassword: nw }); toast.ok('Password changed'); setCur(''); setNw(''); } catch (err) { toast.err(errMsg(err)); } };
  return (
    <div className="grid g2">
      <form className="card" onSubmit={change}><h3>Change password</h3>
        <div className="field"><label>Current password</label><input className="input" type="password" value={cur} onChange={(e) => setCur(e.target.value)} required /></div>
        <div className="field"><label>New password</label><input className="input" type="password" value={nw} onChange={(e) => setNw(e.target.value)} required /></div>
        <button className="btn btn-primary">Update password</button></form>
      {customer && <div className="card"><h3>Recent security activity</h3>{act.length === 0 && <p className="muted">No activity yet</p>}
        {act.map((a, i) => <div className="kv" key={i}><span>{label(a.action)}{!a.success && ' (failed)'}</span><span>{fmtDate(a.createdAt)}</span></div>)}</div>}
    </div>
  );
}

function UpiTab() {
  const toast = useToast();
  const [upis, setUpis] = useState<Upi[]>([]);
  const [accs, setAccs] = useState<Account[]>([]);
  const [handle, setHandle] = useState('');
  const [acc, setAcc] = useState('');
  const load = () => { api.get('/c/upi').then((r) => setUpis(r.data)); api.get('/c/accounts').then((r) => { setAccs(r.data); setAcc((a) => a || String(r.data[0]?.id ?? '')); }); };
  useEffect(() => { load(); }, []);
  const add = async (e: FormEvent) => { e.preventDefault(); try { await api.post('/c/upi', { handle, accountId: Number(acc) }); toast.ok('UPI ID created'); setHandle(''); load(); } catch (err) { toast.err(errMsg(err)); } };
  const relink = async (id: number, accountId: string) => { try { await api.post(`/c/upi/${id}/account`, { accountId: Number(accountId) }); toast.ok('Receiving account updated'); load(); } catch (err) { toast.err(errMsg(err)); } };
  return (
    <div className="grid g2">
      <div className="card"><h3>Your UPI IDs</h3>{upis.length === 0 && <p className="muted">Link a bank account to get your UPI ID.</p>}
        {upis.map((u) => (<div key={u.id} className="mb"><div className="row between"><b>{u.vpa}</b><span className="row" style={{ gap: 6 }}>{u.primary && <Pill value="Primary" tone="blue" />}{!u.primary && <button className="icon-btn" onClick={async () => { await api.delete(`/c/upi/${u.id}`); load(); }}><Trash2 size={15} /></button>}</span></div>
          <select className="input" style={{ marginTop: 6 }} value={u.accountId} onChange={(e) => relink(u.id, e.target.value)}>{accs.map((a) => <option key={a.id} value={a.id}>Receive in {a.bankShortName} · {a.accountNumber.slice(-4)}</option>)}</select></div>))}</div>
      <form className="card" onSubmit={add}><h3>Create a new UPI ID</h3>
        <div className="field"><label>Handle</label><div className="row" style={{ flexWrap: 'nowrap' }}><input className="input" value={handle} onChange={(e) => setHandle(e.target.value.toLowerCase())} placeholder="aarav.sharma" required /><span className="muted">@meridian</span></div></div>
        <div className="field"><label>Receive money in</label><select className="input" value={acc} onChange={(e) => setAcc(e.target.value)}>{accs.map((a) => <option key={a.id} value={a.id}>{a.bankName} · {a.accountNumber.slice(-4)}</option>)}</select></div>
        <button className="btn btn-primary" disabled={!accs.length}>Create UPI ID</button></form>
    </div>
  );
}
