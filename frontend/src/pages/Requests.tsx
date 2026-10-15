import { FormEvent, useEffect, useState } from 'react';
import { HandCoins } from 'lucide-react';
import api, { errMsg, fmtDate, inr } from '../api';
import { PayReq } from '../types';
import { Empty, Modal, PageHead, Pill, useToast } from '../ui';
import PayForm from '../components/PayForm';

export default function Requests() {
  const toast = useToast();
  const [list, setList] = useState<PayReq[]>([]);
  const [payee, setPayee] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [paying, setPaying] = useState<PayReq | null>(null);
  const load = () => api.get('/c/requests').then((r) => setList(r.data));
  useEffect(() => { load(); }, []);
  const create = async (e: FormEvent) => {
    e.preventDefault();
    try { await api.post('/c/requests', { payee, amount: Number(amount), note }); toast.ok('Request sent'); setPayee(''); setAmount(''); setNote(''); load(); } catch (err) { toast.err(errMsg(err)); }
  };
  const act = async (id: number, what: 'decline' | 'cancel') => { try { await api.post(`/c/requests/${id}/${what}`); load(); } catch (e) { toast.err(errMsg(e)); } };
  const incoming = list.filter((r) => r.direction === 'INCOMING'), outgoing = list.filter((r) => r.direction === 'OUTGOING');
  return (
    <>
      <PageHead title="Payment requests" sub="Ask anyone on Meridian for money, or pay requests sent to you." />
      <div className="grid g2">
        <form className="card" onSubmit={create}><h3>Request money</h3>
          <div className="field"><label>From (UPI ID or mobile)</label><input className="input" value={payee} onChange={(e) => setPayee(e.target.value)} required /></div>
          <div className="field"><label>Amount (₹)</label><input className="input" type="number" min="1" value={amount} onChange={(e) => setAmount(e.target.value)} required /></div>
          <div className="field"><label>Note</label><input className="input" value={note} onChange={(e) => setNote(e.target.value)} /></div>
          <button className="btn btn-primary btn-block"><HandCoins size={16} /> Send request</button></form>
        <div className="card"><h3>Requests to you</h3>
          {incoming.length === 0 && <Empty text="No requests received" />}
          {incoming.map((r) => (<div className="tx" key={r.id}><div className="tx-main"><b>{r.requesterName} · {inr(r.amount)}</b><span>{r.note ? `${r.note} · ` : ''}{fmtDate(r.createdAt)}</span></div>
            {r.status === 'PENDING' ? <div className="row" style={{ gap: 6 }}><button className="btn btn-sm btn-primary" onClick={() => setPaying(r)}>Pay</button><button className="btn btn-sm" onClick={() => act(r.id, 'decline')}>Decline</button></div> : <Pill value={r.status} />}</div>))}</div>
      </div>
      <div className="card mt"><h3>Requests you sent</h3>
        {outgoing.length === 0 && <Empty text="You have not requested money yet" />}
        {outgoing.map((r) => (<div className="tx" key={r.id}><div className="tx-main"><b>{r.payerName} · {inr(r.amount)}</b><span>{r.note ? `${r.note} · ` : ''}{fmtDate(r.createdAt)}</span></div>
          {r.status === 'PENDING' ? <button className="btn btn-sm" onClick={() => act(r.id, 'cancel')}>Cancel</button> : <Pill value={r.status} />}</div>))}</div>
      {paying && <Modal title={`Pay ${paying.requesterName}`} onClose={() => setPaying(null)} wide><PayForm prefill={{ vpa: paying.requesterVpa ?? '', amount: String(paying.amount), remark: paying.note ?? '' }} lockRecipient lockAmount requestId={paying.id} onPaid={() => { setPaying(null); load(); }} /></Modal>}
    </>
  );
}
