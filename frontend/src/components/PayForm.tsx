import { useEffect, useState } from 'react';
import { BadgeCheck, Landmark, Smartphone, Star } from 'lucide-react';
import api, { errCode, errMsg, inr } from '../api';
import { Account, Contact, Payee, Tx } from '../types';
import { BankDot, useToast } from '../ui';
import PinModal from './PinModal';
import Receipt from './Receipt';

export interface Prefill { vpa?: string; accountNumber?: string; ifsc?: string; amount?: string; remark?: string }
interface Props { prefill?: Prefill; channel?: 'UPI_ID' | 'QR'; lockRecipient?: boolean; lockAmount?: boolean; requestId?: number; onPaid?: () => void }

/** Shared payment form: resolves the real registered account holder before any money moves. */
export default function PayForm({ prefill, channel = 'UPI_ID', lockRecipient, lockAmount, requestId, onPaid }: Props) {
  const toast = useToast();
  const [mode, setMode] = useState<'upi' | 'bank'>(prefill?.accountNumber ? 'bank' : 'upi');
  const [q, setQ] = useState(prefill?.vpa ?? sessionStorage.getItem('payto') ?? '');
  const [accNo, setAccNo] = useState(prefill?.accountNumber ?? '');
  const [ifsc, setIfsc] = useState(prefill?.ifsc ?? '');
  const [payee, setPayee] = useState<Payee | null>(null);
  const [lookupErr, setLookupErr] = useState('');
  const [searching, setSearching] = useState(false);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [from, setFrom] = useState<number>();
  const [amount, setAmount] = useState(prefill?.amount ?? '');
  const [remark, setRemark] = useState(prefill?.remark ?? '');
  const [pinOpen, setPinOpen] = useState(false);
  const [pinErr, setPinErr] = useState('');
  const [busy, setBusy] = useState(false);
  const [receipt, setReceipt] = useState<Tx | null>(null);

  const loadAccounts = () => api.get<Account[]>('/c/accounts').then((r) => {
    setAccounts(r.data);
    setFrom((f) => f ?? (r.data.find((a) => a.primary && a.status === 'ACTIVE') ?? r.data.find((a) => a.status === 'ACTIVE'))?.id);
  });
  useEffect(() => { sessionStorage.removeItem('payto'); loadAccounts(); api.get('/c/contacts').then((r) => setContacts(r.data)); }, []);

  useEffect(() => {
    setPayee(null); setLookupErr('');
    const ready = mode === 'upi' ? q.trim().length >= 3 : accNo.trim().length >= 9 && ifsc.trim().length === 11;
    if (!ready) return;
    setSearching(true);
    const h = setTimeout(async () => {
      try {
        const r = mode === 'upi' ? await api.get('/c/payees/lookup', { params: { q: q.trim() } })
          : await api.get('/c/payees/lookup-account', { params: { accountNumber: accNo.trim(), ifsc: ifsc.trim().toUpperCase() } });
        setPayee(r.data);
      } catch (e) { setLookupErr(errMsg(e)); } finally { setSearching(false); }
    }, 400);
    return () => { clearTimeout(h); setSearching(false); };
  }, [q, accNo, ifsc, mode]);

  const src = accounts.find((a) => a.id === from);
  const saved = !!payee?.vpa && contacts.some((c) => c.vpa === payee.vpa);
  const amt = Number(amount);
  const canPay = !!payee && !!from && amt >= 1 && !busy;

  const saveContact = async () => {
    try { const r = await api.post('/c/contacts', { vpa: payee!.vpa }); setContacts([...contacts, r.data]); toast.ok('Saved to contacts'); } catch (e) { toast.err(errMsg(e)); }
  };

  const submit = async (pin: string) => {
    setBusy(true); setPinErr('');
    try {
      const r = await api.post<Tx>('/c/pay', {
        fromAccountId: from, vpa: mode === 'upi' ? payee!.vpa : undefined, accountNumber: mode === 'bank' ? accNo.trim() : undefined,
        ifsc: mode === 'bank' ? ifsc.trim().toUpperCase() : undefined, amount: amt, remark, pin, channel: mode === 'upi' ? channel : 'BANK_ACCOUNT', requestId,
      });
      setPinOpen(false); setReceipt(r.data); loadAccounts(); onPaid?.();
    } catch (e) {
      if (errCode(e) === 'PIN_INVALID') setPinErr(errMsg(e)); else { setPinOpen(false); toast.err(errMsg(e)); }
    } finally { setBusy(false); }
  };

  return (
    <div className="card">
      {!lockRecipient && (
        <div className="tabs">
          <button className={`tab ${mode === 'upi' ? 'on' : ''}`} onClick={() => setMode('upi')}><Smartphone size={15} /> UPI ID / Mobile</button>
          <button className={`tab ${mode === 'bank' ? 'on' : ''}`} onClick={() => setMode('bank')}><Landmark size={15} /> Bank account</button>
        </div>
      )}
      {mode === 'upi' ? (
        <div className="field"><label>UPI ID or registered mobile number</label>
          <input className="input" value={q} readOnly={lockRecipient} onChange={(e) => setQ(e.target.value)} placeholder="9876543210 or name@meridian" /></div>
      ) : (
        <div className="grid g2">
          <div className="field"><label>Account number</label><input className="input" inputMode="numeric" value={accNo} readOnly={lockRecipient} onChange={(e) => setAccNo(e.target.value.replace(/\D/g, ''))} /></div>
          <div className="field"><label>IFSC</label><input className="input" value={ifsc} maxLength={11} readOnly={lockRecipient} onChange={(e) => setIfsc(e.target.value.toUpperCase())} placeholder="SBIN0MRD001" /></div>
        </div>
      )}
      {!lockRecipient && mode === 'upi' && contacts.length > 0 && (
        <div className="chips mb">{contacts.map((c) => <button key={c.id} className={`chip ${q === c.vpa ? 'on' : ''}`} onClick={() => setQ(c.vpa)}>{c.nickname || c.name}</button>)}</div>
      )}
      {searching && <p className="muted mb">Looking up…</p>}
      {lookupErr && <p className="hint err mb">{lookupErr}</p>}
      {payee && (
        <div className="payee">
          <BadgeCheck size={22} color="#15803d" />
          <div className="grow"><b>{payee.name}</b><div className="muted">{payee.vpa ?? payee.accountMasked} · {payee.bankName}</div></div>
          {!saved && !lockRecipient && payee.vpa && <button className="btn btn-sm" onClick={saveContact}><Star size={13} /> Save</button>}
        </div>
      )}

      <div className="field"><label>Amount</label>
        <div className="amt-wrap"><span>₹</span><input className="input big" type="number" min="1" inputMode="decimal" value={amount} readOnly={lockAmount} onChange={(e) => setAmount(e.target.value)} placeholder="0" /></div></div>
      {!lockAmount && <div className="chips mb">{[100, 500, 1000, 5000].map((n) => <button key={n} className="chip" onClick={() => setAmount(String((Number(amount) || 0) + n))}>+ ₹{n}</button>)}</div>}
      <div className="field"><label>Remark (optional)</label><input className="input" value={remark} maxLength={100} onChange={(e) => setRemark(e.target.value)} placeholder="Rent, dinner, gift…" /></div>

      <div className="field"><label>Pay from</label>
        {accounts.length === 0 && <p className="hint err">No bank account linked yet. Add one under Banks.</p>}
        {accounts.map((a) => (
          <button key={a.id} type="button" disabled={a.status !== 'ACTIVE'} className={`bank-pick ${from === a.id ? 'on' : ''}`} onClick={() => setFrom(a.id)}>
            <BankDot color={a.bankColor} name={a.bankShortName} />
            <div className="grow"><b>{a.bankName}</b><div className="muted">{a.type.toLowerCase()} · {a.accountNumber.slice(-4).padStart(8, 'X')}{a.status !== 'ACTIVE' ? ` · ${a.status.toLowerCase()}` : ''}</div></div>
            <b className="mono">{inr(a.balance)}</b>
          </button>
        ))}
        {src && amt > src.balance && <span className="hint err">Amount exceeds the balance of this account</span>}
      </div>
      <button className="btn btn-primary btn-block" disabled={!canPay} onClick={() => { setPinErr(''); setPinOpen(true); }}>Proceed to pay {amt > 0 ? inr(amt) : ''}</button>

      {pinOpen && <PinModal title="Enter bank PIN" subtitle={`Paying ${inr(amt)} to ${payee?.name} from ${src?.bankShortName}`} busy={busy} error={pinErr} onSubmit={submit} onClose={() => setPinOpen(false)} />}
      {receipt && <Receipt tx={receipt} success onClose={() => setReceipt(null)} />}
    </div>
  );
}
