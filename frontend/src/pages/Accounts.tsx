import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, KeyRound, Plus, Star } from 'lucide-react';
import api, { errMsg, fmtDay, inr, label } from '../api';
import { useAuth } from '../auth';
import { Account, Bank } from '../types';
import { BankDot, Empty, Modal, PageHead, Pill, Skeleton, useToast } from '../ui';

export default function Accounts() {
  const toast = useToast();
  const nav = useNavigate();
  const { user } = useAuth();
  const [accs, setAccs] = useState<Account[] | null>(null);
  const [wizard, setWizard] = useState(false);
  const [sel, setSel] = useState<Account | null>(null);
  const load = () => api.get('/c/accounts').then((r) => { setAccs(r.data); setSel((s) => (s ? r.data.find((a: Account) => a.id === s.id) ?? null : null)); });
  useEffect(() => { load(); }, []);
  const kycOk = user?.kycStatus === 'APPROVED';
  return (
    <>
      <PageHead title="Banks & accounts" sub="Link accounts from multiple banks and manage each bank PIN separately.">
        <button className="btn btn-primary" onClick={() => (kycOk ? setWizard(true) : toast.err('Complete KYC in your profile first'))}><Plus size={16} /> Add bank account</button>
      </PageHead>
      {!kycOk && <div className="banner"><div className="grow"><b>KYC required</b><div className="muted">Bank accounts can be linked only after your KYC is approved by a bank manager.</div></div><button className="btn btn-gold btn-sm" onClick={() => nav('/profile')}>Go to KYC</button></div>}
      {!accs ? <Skeleton h={170} /> : accs.length === 0 ? <div className="card"><Empty text="No bank accounts linked yet." /></div> : (
        <div className="grid g3">{accs.map((a, i) => (
          <div key={a.id} className={`acct rise ${a.status !== 'ACTIVE' ? 'off' : ''}`} onClick={() => setSel(a)} style={{ background: `linear-gradient(135deg, ${a.bankColor}, ${a.bankColor}cc)`, animationDelay: `${i * 60}ms` }}>
            <div className="row between"><b>{a.bankName}</b><span className="row" style={{ gap: 6 }}>{a.primary && <Star size={15} fill="#fff" />}{a.status !== 'ACTIVE' && <span className="pill gray">{a.status.toLowerCase()}</span>}</span></div>
            <div><small>{label(a.type)} account · {a.ifsc}</small><div className="bal mono">{inr(a.balance)}</div></div>
            <div className="num">{a.accountNumber.slice(0, 4)} {a.accountNumber.slice(4, 8)} {a.accountNumber.slice(8)}</div>
          </div>))}</div>)}
      {wizard && <Wizard onClose={() => setWizard(false)} onDone={() => { setWizard(false); load(); }} />}
      {sel && <Detail a={sel} onClose={() => setSel(null)} reload={load} />}
    </>
  );
}

function Wizard({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const toast = useToast();
  const [banks, setBanks] = useState<Bank[]>([]);
  const [step, setStep] = useState(1);
  const [bank, setBank] = useState<Bank | null>(null);
  const [branchId, setBranchId] = useState<number>();
  const [type, setType] = useState('SAVINGS');
  const [pin, setPin] = useState('');
  const [pin2, setPin2] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => { api.get('/c/banks').then((r) => setBanks(r.data)); }, []);
  const create = async () => {
    setBusy(true);
    try { await api.post('/c/accounts', { bankId: bank!.id, branchId, type, pin }); toast.ok(`${bank!.shortName} account linked`); onDone(); } catch (e) { toast.err(errMsg(e)); } finally { setBusy(false); }
  };
  return (
    <Modal title="Add bank account" onClose={onClose}>
      <div className="steps">{[1, 2, 3].map((n) => <span key={n} className={step >= n ? 'on' : ''} />)}</div>
      {step === 1 && (<><p className="muted mb">Choose your bank</p>{banks.map((b) => (
        <button key={b.id} className={`bank-pick ${bank?.id === b.id ? 'on' : ''}`} onClick={() => { setBank(b); setBranchId(undefined); }}><BankDot color={b.color} name={b.shortName} /><div className="grow"><b>{b.name}</b><div className="muted">Head office: {b.headquarters}</div></div></button>))}
        <button className="btn btn-primary btn-block mt" disabled={!bank} onClick={() => setStep(2)}>Continue</button></>)}
      {step === 2 && bank && (<>
        <div className="field"><label>Branch</label><select className="input" value={branchId ?? ''} onChange={(e) => setBranchId(Number(e.target.value))}><option value="">Select branch</option>{bank.branches.map((b) => <option key={b.id} value={b.id}>{b.name} — {b.ifsc}</option>)}</select></div>
        <div className="field"><label>Account type</label><select className="input" value={type} onChange={(e) => setType(e.target.value)}><option value="SAVINGS">Savings</option><option value="CURRENT">Current</option></select></div>
        <div className="row"><button className="btn" onClick={() => setStep(1)}><ArrowLeft size={15} /> Back</button><button className="btn btn-primary grow" disabled={!branchId} onClick={() => setStep(3)}>Continue</button></div></>)}
      {step === 3 && (<>
        <p className="muted mb">Set a 4 or 6 digit PIN for this account. You will enter it for every payment.</p>
        <div className="field"><label>New PIN</label><input className="input otp" type="password" inputMode="numeric" maxLength={6} value={pin} onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))} /></div>
        <div className="field"><label>Confirm PIN</label><input className="input otp" type="password" inputMode="numeric" maxLength={6} value={pin2} onChange={(e) => setPin2(e.target.value.replace(/\D/g, ''))} />{pin2 && pin !== pin2 && <span className="hint err">PINs do not match</span>}</div>
        <div className="row"><button className="btn" onClick={() => setStep(2)}><ArrowLeft size={15} /> Back</button><button className="btn btn-primary grow" disabled={busy || pin.length < 4 || pin !== pin2} onClick={create}>{busy ? 'Linking…' : 'Link account'}</button></div></>)}
    </Modal>
  );
}

function Detail({ a, onClose, reload }: { a: Account; onClose: () => void; reload: () => void }) {
  const toast = useToast();
  const nav = useNavigate();
  const [view, setView] = useState<'main' | 'change' | 'forgot'>('main');
  const [oldPin, setOldPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [code, setCode] = useState('');
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);
  const run = async (fn: () => Promise<void>) => { setBusy(true); try { await fn(); } catch (e) { toast.err(errMsg(e)); } finally { setBusy(false); } };
  const digits = (set: (v: string) => void) => (e: React.ChangeEvent<HTMLInputElement>) => set(e.target.value.replace(/\D/g, ''));
  return (
    <Modal title={a.bankName} onClose={onClose}>
      {view === 'main' && (<>
        <div className="row mb"><BankDot color={a.bankColor} name={a.bankShortName} /><div className="grow"><b className="mono" style={{ fontSize: '1.4rem' }}>{inr(a.balance)}</b></div><Pill value={a.status} /></div>
        <div className="kv"><span>Account number</span><span className="mono">{a.accountNumber}</span></div>
        <div className="kv"><span>IFSC</span><span>{a.ifsc}</span></div><div className="kv"><span>Branch</span><span>{a.branchName}</span></div>
        <div className="kv"><span>Type</span><span>{label(a.type)}</span></div><div className="kv"><span>Opened</span><span>{fmtDay(a.openedAt)}</span></div>
        {a.pinLocked && <p className="err-text mt">PIN is locked after wrong attempts. Reset it with an OTP.</p>}
        <div className="row mt">
          <button className="btn" onClick={() => nav(`/history?account=${a.id}`)}>Statement</button>
          {!a.primary && a.status === 'ACTIVE' && <button className="btn" onClick={() => run(async () => { await api.post(`/c/accounts/${a.id}/primary`); toast.ok('Primary account updated'); reload(); })}><Star size={14} /> Make primary</button>}
          <button className="btn" onClick={() => setView('change')}><KeyRound size={14} /> Change PIN</button>
          <button className="btn" onClick={() => setView('forgot')}>Forgot PIN</button></div></>)}
      {view === 'change' && (<>
        <div className="field"><label>Current PIN</label><input className="input otp" type="password" inputMode="numeric" maxLength={6} value={oldPin} onChange={digits(setOldPin)} /></div>
        <div className="field"><label>New PIN</label><input className="input otp" type="password" inputMode="numeric" maxLength={6} value={newPin} onChange={digits(setNewPin)} /></div>
        <div className="row"><button className="btn" onClick={() => setView('main')}>Back</button><button className="btn btn-primary grow" disabled={busy || oldPin.length < 4 || newPin.length < 4} onClick={() => run(async () => { await api.post(`/c/accounts/${a.id}/pin/change`, { oldPin, newPin }); toast.ok('PIN changed'); reload(); setView('main'); })}>Change PIN</button></div></>)}
      {view === 'forgot' && (<>
        {!sent ? (<><p className="muted mb">We will send a one-time code to your registered mobile and email.</p>
          <div className="row"><button className="btn" onClick={() => setView('main')}>Back</button><button className="btn btn-primary grow" disabled={busy} onClick={() => run(async () => { await api.post(`/c/accounts/${a.id}/pin/otp`); toast.ok('Code sent'); setSent(true); })}>Send code</button></div></>) : (<>
          <div className="field"><label>6-digit code</label><input className="input otp" inputMode="numeric" maxLength={6} value={code} onChange={digits(setCode)} /></div>
          <div className="field"><label>New PIN</label><input className="input otp" type="password" inputMode="numeric" maxLength={6} value={newPin} onChange={digits(setNewPin)} /></div>
          <button className="btn btn-primary btn-block" disabled={busy || code.length !== 6 || newPin.length < 4} onClick={() => run(async () => { await api.post(`/c/accounts/${a.id}/pin/reset`, { code, newPin }); toast.ok('PIN reset'); reload(); setView('main'); })}>Reset PIN</button>
          <p className="muted center mt"><a className="link" href="/inbox" target="_blank" rel="noreferrer">Open simulated inbox</a></p></>)}</>)}
    </Modal>
  );
}
