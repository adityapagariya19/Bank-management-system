import { useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import api, { errMsg, fmtDate, fmtDay, inr, label } from '../api';
import { AdminAccount, Kyc, ManagerOverview } from '../types';
import { Empty, PageHead, Pill, Skeleton, useToast } from '../ui';

export default function Manager() {
  const toast = useToast();
  const [tab, setTab] = useState('kyc');
  const [ov, setOv] = useState<ManagerOverview | null>(null);
  const [kyc, setKyc] = useState<Kyc[]>([]);
  const [accs, setAccs] = useState<AdminAccount[]>([]);
  const [q, setQ] = useState('');
  const [notes, setNotes] = useState<Record<number, string>>({});
  const loadAll = () => { api.get('/manager/overview').then((r) => setOv(r.data)); api.get('/manager/kyc').then((r) => setKyc(r.data)); };
  const loadAccs = () => api.get('/manager/accounts', { params: { q } }).then((r) => setAccs(r.data));
  useEffect(() => { loadAll(); }, []);
  useEffect(() => { if (tab === 'accounts') loadAccs(); /* eslint-disable-next-line */ }, [tab, q]);
  const review = async (id: number, approve: boolean) => { try { await api.post(`/manager/kyc/${id}/review`, { approve, note: notes[id] ?? '' }); toast.ok(approve ? 'KYC approved' : 'KYC rejected'); loadAll(); } catch (e) { toast.err(errMsg(e)); } };
  const setStatus = async (id: number, value: string) => { try { await api.post(`/manager/accounts/${id}/status`, null, { params: { value } }); toast.ok(`Account ${value.toLowerCase()}`); loadAccs(); loadAll(); } catch (e) { toast.err(errMsg(e)); } };
  if (!ov) return <Skeleton h={200} />;
  return (
    <>
      <PageHead title="Branch desk" sub="KYC approvals, account control and bank-wise position." />
      <div className="grid g4 mb">
        <div className="card stat"><div className="l">PENDING KYC</div><div className="v">{ov.pendingKyc}</div></div><div className="card stat"><div className="l">CUSTOMERS</div><div className="v">{ov.customers}</div></div>
        <div className="card stat"><div className="l">ACCOUNTS · FROZEN</div><div className="v">{ov.accounts} · {ov.frozenAccounts}</div></div><div className="card stat dark"><div className="l">TOTAL DEPOSITS</div><div className="v mono">{inr(ov.totalDeposits)}</div></div>
      </div>
      <div className="tabs"><button className={`tab ${tab === 'kyc' ? 'on' : ''}`} onClick={() => setTab('kyc')}>KYC queue ({kyc.length})</button><button className={`tab ${tab === 'accounts' ? 'on' : ''}`} onClick={() => setTab('accounts')}>Accounts</button><button className={`tab ${tab === 'banks' ? 'on' : ''}`} onClick={() => setTab('banks')}>Bank position</button></div>
      {tab === 'kyc' && (kyc.length === 0 ? <div className="card"><Empty text="No KYC applications waiting" /></div> : <div className="grid g2">{kyc.map((k) => (
        <div key={k.id} className="card rise"><div className="row between"><h3 style={{ margin: 0 }}>{k.userName}</h3><Pill value="PENDING" /></div><p className="muted mb">{k.email} · {k.phone}</p>
          <div className="kv"><span>Date of birth</span><span>{fmtDay(k.dateOfBirth)}</span></div><div className="kv"><span>Address</span><span>{k.address}, {k.city}, {k.state} {k.pincode}</span></div>
          <div className="kv"><span>{label(k.idType ?? '')}</span><span className="mono">{k.idNumberMasked}</span></div><div className="kv"><span>Submitted</span><span>{fmtDate(k.submittedAt)}</span></div>
          <input className="input mt" placeholder="Note (required to reject)" value={notes[k.id] ?? ''} onChange={(e) => setNotes({ ...notes, [k.id]: e.target.value })} />
          <div className="row mt"><button className="btn btn-primary grow" onClick={() => review(k.id, true)}>Approve</button><button className="btn btn-danger grow" onClick={() => review(k.id, false)}>Reject</button></div></div>))}</div>)}
      {tab === 'accounts' && (<div className="card"><input className="input mb" placeholder="Search name, email, account no. or bank" value={q} onChange={(e) => setQ(e.target.value)} />
        <div className="table-wrap"><table className="table"><thead><tr><th>Holder</th><th>Bank</th><th>Account</th><th>Type</th><th>Balance</th><th>Status</th><th>Opened</th><th /></tr></thead><tbody>
          {accs.map((a) => (<tr key={a.id}><td><b>{a.holderName}</b><div className="muted">{a.email}</div></td><td>{a.bankName}<div className="muted">{a.ifsc}</div></td><td className="mono">{a.accountNumber}</td><td>{label(a.type)}</td><td className="mono">{inr(a.balance)}</td><td><Pill value={a.status} /></td><td>{fmtDay(a.openedAt)}</td>
            <td><div className="row" style={{ gap: 6, flexWrap: 'nowrap' }}>{a.status === 'ACTIVE' && <button className="btn btn-sm" onClick={() => setStatus(a.id, 'FROZEN')}>Freeze</button>}{a.status === 'FROZEN' && <button className="btn btn-sm" onClick={() => setStatus(a.id, 'ACTIVE')}>Unfreeze</button>}{a.status !== 'CLOSED' && <button className="btn btn-sm btn-danger" onClick={() => setStatus(a.id, 'CLOSED')}>Close</button>}</div></td></tr>))}</tbody></table></div>
        {accs.length === 0 && <Empty text="No accounts found" />}</div>)}
      {tab === 'banks' && (<div className="card"><h3>Deposits by bank</h3><div style={{ height: 280 }}><ResponsiveContainer><BarChart data={ov.banks}><CartesianGrid stroke="#eef2f7" vertical={false} /><XAxis dataKey="bank" fontSize={11} tickLine={false} axisLine={false} /><YAxis fontSize={12} tickLine={false} axisLine={false} /><Tooltip formatter={(v: number) => inr(v)} /><Bar dataKey="balance" radius={[6, 6, 0, 0]}>{ov.banks.map((b) => <Cell key={b.bank} fill={b.color} />)}</Bar></BarChart></ResponsiveContainer></div>
        <table className="table mt"><thead><tr><th>Bank</th><th>Accounts</th><th>Balance held</th></tr></thead><tbody>{ov.banks.map((b) => <tr key={b.bank}><td>{b.bank}</td><td>{b.accounts}</td><td className="mono">{inr(b.balance)}</td></tr>)}</tbody></table></div>)}
    </>
  );
}
