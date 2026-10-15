import { FormEvent, useEffect, useState } from 'react';
import { Banknote, Search } from 'lucide-react';
import api, { errCode, errMsg, inr } from '../api';
import { AccountLookup, Tx } from '../types';
import { BankDot, Empty, PageHead, Pill, useToast } from '../ui';
import TxRow from '../components/TxRow';
import Receipt from '../components/Receipt';

export default function Teller() {
  const toast = useToast();
  const [no, setNo] = useState('');
  const [acc, setAcc] = useState<AccountLookup | null>(null);
  const [mode, setMode] = useState<'deposit' | 'withdraw'>('deposit');
  const [amount, setAmount] = useState('');
  const [remark, setRemark] = useState('');
  const [pin, setPin] = useState('');
  const [recent, setRecent] = useState<Tx[]>([]);
  const [receipt, setReceipt] = useState<Tx | null>(null);
  const [busy, setBusy] = useState(false);
  const loadRecent = () => api.get('/teller/recent').then((r) => setRecent(r.data));
  useEffect(() => { loadRecent(); }, []);
  const find = async (e: FormEvent) => { e.preventDefault(); setAcc(null); try { setAcc((await api.get('/teller/lookup', { params: { accountNumber: no } })).data); } catch (err) { toast.err(errMsg(err)); } };
  const submit = async (e: FormEvent) => {
    e.preventDefault(); setBusy(true);
    try {
      const r = await api.post(`/teller/${mode}`, { accountNumber: acc!.accountNumber, amount: Number(amount), remark, pin: mode === 'withdraw' ? pin : undefined });
      setReceipt(r.data); setAmount(''); setRemark(''); setPin('');
      setAcc((await api.get('/teller/lookup', { params: { accountNumber: acc!.accountNumber } })).data); loadRecent();
    } catch (err) { toast.err(errMsg(err)); if (errCode(err) === 'PIN_INVALID') setPin(''); } finally { setBusy(false); }
  };
  return (
    <>
      <PageHead title="Counter operations" sub="Cash deposits and withdrawals handled by branch staff. Every entry is written to the ledger and audit trail." />
      <div className="grid g2">
        <div>
          <form className="card mb" onSubmit={find}><h3>Find customer account</h3>
            <div className="row" style={{ flexWrap: 'nowrap' }}><input className="input" inputMode="numeric" placeholder="12-digit account number" value={no} onChange={(e) => setNo(e.target.value.replace(/\D/g, ''))} required /><button className="btn btn-primary"><Search size={15} /> Find</button></div></form>
          {acc && (<form className="card rise" onSubmit={submit}>
            <div className="row mb"><BankDot color="#0c2340" name={acc.bankName} /><div className="grow"><b>{acc.holderName}</b><div className="muted">{acc.bankName} · {acc.branchName} · {acc.ifsc}</div></div><Pill value={acc.status} /></div>
            <div className="kv"><span>Account</span><span className="mono">{acc.accountNumber}</span></div><div className="kv"><span>Available balance</span><span className="mono">{inr(acc.balance)}</span></div>
            <div className="tabs mt"><button type="button" className={`tab ${mode === 'deposit' ? 'on' : ''}`} onClick={() => setMode('deposit')}>Cash deposit</button><button type="button" className={`tab ${mode === 'withdraw' ? 'on' : ''}`} onClick={() => setMode('withdraw')}>Cash withdrawal</button></div>
            <div className="field"><label>Amount (₹)</label><input className="input" type="number" min="1" value={amount} onChange={(e) => setAmount(e.target.value)} required /></div>
            <div className="field"><label>Remark</label><input className="input" value={remark} onChange={(e) => setRemark(e.target.value)} /></div>
            {mode === 'withdraw' && <div className="field"><label>Customer's PIN (entered by the customer)</label><input className="input otp" type="password" inputMode="numeric" maxLength={6} value={pin} onChange={(e) => setPin(e.target.value.replace(/\D/g, ''))} required /></div>}
            <button className="btn btn-primary btn-block" disabled={busy || !amount}><Banknote size={16} /> {busy ? 'Posting…' : mode === 'deposit' ? 'Deposit cash' : 'Withdraw cash'}</button></form>)}
        </div>
        <div className="card"><h3>My recent counter transactions</h3>{recent.length === 0 ? <Empty text="No counter transactions yet" /> : recent.map((t) => <TxRow key={t.id} t={t} />)}</div>
      </div>
      {receipt && <Receipt tx={receipt} success onClose={() => setReceipt(null)} />}
    </>
  );
}
