import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Database, UserPlus } from 'lucide-react';
import api, { errMsg, fmtDate, inr, label } from '../api';
import { AdminStats, AdminUser } from '../types';
import { Modal, PageHead, Pill, Skeleton, useToast } from '../ui';

export default function Admin() {
  const toast = useToast();
  const [s, setS] = useState<AdminStats | null>(null);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [q, setQ] = useState('');
  const [role, setRole] = useState('');
  const [open, setOpen] = useState(false);
  const [f, setF] = useState({ fullName: '', email: '', phone: '', role: 'TELLER', password: '' });
  const load = () => { api.get('/admin/stats').then((r) => setS(r.data)); api.get('/admin/users', { params: { q, role } }).then((r) => setUsers(r.data)); };
  useEffect(() => { load(); /* eslint-disable-next-line */ }, [q, role]);
  const act = async (url: string, msg: string) => { try { await api.post(url); toast.ok(msg); load(); } catch (e) { toast.err(errMsg(e)); } };
  const create = async (e: FormEvent) => { e.preventDefault(); try { await api.post('/admin/staff', f); toast.ok('Staff account created'); setOpen(false); load(); } catch (err) { toast.err(errMsg(err)); } };
  if (!s) return <Skeleton h={200} />;
  return (
    <>
      <PageHead title="Administration" sub="Users, roles and system health.">
        <Link className="btn" to="/database"><Database size={15} /> Database explorer</Link><button className="btn btn-primary" onClick={() => setOpen(true)}><UserPlus size={15} /> New staff</button></PageHead>
      <div className="grid g4 mb">
        <div className="card stat"><div className="l">USERS</div><div className="v">{s.users}</div><div className="muted">{s.customers} customers · {s.staff} staff</div></div>
        <div className="card stat"><div className="l">ACCOUNTS · BANKS</div><div className="v">{s.accounts} · {s.banks}</div></div>
        <div className="card stat"><div className="l">TRANSACTIONS</div><div className="v">{s.transactions}</div><div className="muted">{s.transactionsToday} today</div></div>
        <div className="card stat dark"><div className="l">TOTAL DEPOSITS</div><div className="v mono">{inr(s.totalDeposits)}</div><div className="l">{s.lockedUsers} locked user(s)</div></div>
      </div>
      <div className="card"><div className="row mb"><input className="input grow" placeholder="Search name, email or mobile" value={q} onChange={(e) => setQ(e.target.value)} />
        <select className="input" style={{ width: 160 }} value={role} onChange={(e) => setRole(e.target.value)}><option value="">All roles</option>{['CUSTOMER', 'TELLER', 'MANAGER', 'AUDITOR', 'ADMIN'].map((r) => <option key={r} value={r}>{label(r)}</option>)}</select></div>
        <div className="table-wrap"><table className="table"><thead><tr><th>User</th><th>Role</th><th>Status</th><th>KYC</th><th>Last sign-in</th><th /></tr></thead><tbody>
          {users.map((u) => (<tr key={u.id}><td><b>{u.fullName}</b><div className="muted">{u.email} · {u.phone}</div></td><td>{label(u.role)}</td><td><Pill value={u.status} />{u.locked && <> <Pill value="LOCKED" tone="red" /></>}</td><td>{u.kycStatus ? <Pill value={u.kycStatus} /> : '—'}</td><td>{fmtDate(u.lastLoginAt)}</td>
            <td><div className="row" style={{ gap: 6, flexWrap: 'nowrap' }}>{(u.locked || u.failedAttempts > 0) && <button className="btn btn-sm" onClick={() => act(`/admin/users/${u.id}/unlock`, 'User unlocked')}>Unlock</button>}
              {u.status === 'ACTIVE' ? <button className="btn btn-sm btn-danger" onClick={() => act(`/admin/users/${u.id}/status?value=DISABLED`, 'User disabled')}>Disable</button> : u.status === 'DISABLED' && <button className="btn btn-sm" onClick={() => act(`/admin/users/${u.id}/status?value=ACTIVE`, 'User enabled')}>Enable</button>}</div></td></tr>))}</tbody></table></div></div>
      {open && <Modal title="Create staff account" onClose={() => setOpen(false)}><form onSubmit={create}>
        <div className="field"><label>Full name</label><input className="input" value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} required /></div>
        <div className="field"><label>Email</label><input className="input" type="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} required /></div>
        <div className="field"><label>Mobile</label><input className="input" maxLength={10} value={f.phone} onChange={(e) => setF({ ...f, phone: e.target.value })} required /></div>
        <div className="field"><label>Role</label><select className="input" value={f.role} onChange={(e) => setF({ ...f, role: e.target.value })}>{['TELLER', 'MANAGER', 'AUDITOR', 'ADMIN'].map((r) => <option key={r} value={r}>{label(r)}</option>)}</select></div>
        <div className="field"><label>Initial password</label><input className="input" type="password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} required /></div>
        <button className="btn btn-primary btn-block">Create</button></form></Modal>}
    </>
  );
}
