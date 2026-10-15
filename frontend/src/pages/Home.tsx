import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { ArrowDownLeft, ArrowUpRight, HandCoins, Landmark, QrCode, ScanLine, Send, Wallet } from 'lucide-react';
import api, { inr } from '../api';
import { useAuth } from '../auth';
import { Account, Dashboard } from '../types';
import { BankDot, CountUp, Empty, PageHead, Skeleton } from '../ui';
import TxRow from '../components/TxRow';

export default function Home() {
  const { user } = useAuth();
  const [d, setD] = useState<Dashboard | null>(null);
  const [accs, setAccs] = useState<Account[]>([]);
  useEffect(() => { api.get('/c/dashboard').then((r) => setD(r.data)); api.get('/c/accounts').then((r) => setAccs(r.data)); }, []);
  if (!d) return <div className="grid g3"><Skeleton /><Skeleton /><Skeleton /></div>;
  const h = new Date().getHours();
  const tiles = [
    { to: '/pay?tab=scan', icon: ScanLine, label: 'Scan & pay' }, { to: '/pay', icon: Send, label: 'Send money' },
    { to: '/pay?tab=receive', icon: QrCode, label: 'Receive' }, { to: '/requests', icon: HandCoins, label: 'Request' }, { to: '/accounts', icon: Landmark, label: 'Add bank' },
  ];
  return (
    <>
      <PageHead title={`${h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'}, ${user!.fullName.split(' ')[0]}`} sub="Your money across all linked banks." />
      {d.kycStatus !== 'APPROVED' && (
        <div className="banner rise"><Landmark size={22} color="#b45309" /><div className="grow"><b>{d.kycStatus === 'PENDING' ? 'KYC under review' : d.kycStatus === 'REJECTED' ? 'KYC needs attention' : 'Complete your KYC'}</b>
          <div className="muted">{d.kycStatus === 'PENDING' ? 'A bank manager is verifying your documents.' : 'Verification is required before you can link a bank account.'}</div></div>
          {d.kycStatus !== 'PENDING' && <Link className="btn btn-gold btn-sm" to="/profile">Complete KYC</Link>}</div>
      )}
      {d.pendingRequests > 0 && <div className="banner rise" style={{ background: '#eef4ff', borderColor: '#c9d9f7' }}><HandCoins size={22} color="#2457c5" /><div className="grow"><b>{d.pendingRequests} payment request{d.pendingRequests > 1 ? 's' : ''} waiting</b></div><Link className="btn btn-sm" to="/requests">View</Link></div>}

      <div className="grid g4 mb">{tiles.map((t, i) => (
        <Link key={t.to + t.label} to={t.to} className="card hover-lift rise row" style={{ animationDelay: `${i * 60}ms`, flexWrap: 'nowrap' }}><t.icon size={22} color="#0c2340" /><b>{t.label}</b></Link>
      ))}</div>

      <div className="grid g4">
        <div className="card stat dark rise"><div className="l"><Wallet size={15} /> TOTAL BALANCE</div><div className="v mono"><CountUp value={d.totalBalance} format={inr} /></div><div className="l" style={{ marginTop: 6 }}>{d.accountCount} linked account{d.accountCount !== 1 ? 's' : ''}</div></div>
        <div className="card stat rise" style={{ animationDelay: '80ms' }}><div className="l"><ArrowDownLeft size={15} /> RECEIVED · 30 DAYS</div><div className="v pos mono"><CountUp value={d.income30} format={inr} /></div></div>
        <div className="card stat rise" style={{ animationDelay: '160ms' }}><div className="l"><ArrowUpRight size={15} /> SPENT · 30 DAYS</div><div className="v neg mono"><CountUp value={d.expense30} format={inr} /></div></div>
        <div className="card stat rise" style={{ animationDelay: '240ms' }}><div className="l"><Landmark size={15} /> BANKS</div><div className="v">{new Set(accs.map((a) => a.bankId)).size}</div></div>
      </div>

      {accs.length > 0 && (<div className="grid g3 mt">{accs.map((a, i) => (
        <Link to="/accounts" key={a.id} className={`acct rise ${a.status !== 'ACTIVE' ? 'off' : ''}`} style={{ background: `linear-gradient(135deg, ${a.bankColor}, ${a.bankColor}cc)`, animationDelay: `${i * 70}ms` }}>
          <div className="row between"><b>{a.bankName}</b>{a.primary && <span className="pill gray">Primary</span>}</div>
          <div><small>{a.type.toLowerCase()} account</small><div className="bal mono">{inr(a.balance)}</div></div>
          <div className="num">{a.accountNumber.slice(0, 4)} •••• {a.accountNumber.slice(-4)}</div>
        </Link>))}</div>)}

      <div className="grid g-main mt">
        <div className="card"><h3>Cash flow · last 7 days</h3>
          <div style={{ height: 250 }}><ResponsiveContainer><BarChart data={d.chart} margin={{ left: -12 }}>
            <CartesianGrid stroke="#eef2f7" vertical={false} /><XAxis dataKey="date" fontSize={12} tickLine={false} axisLine={false} stroke="#64748b" />
            <YAxis fontSize={12} tickLine={false} axisLine={false} stroke="#64748b" /><Tooltip formatter={(v: number) => inr(v)} />
            <Bar dataKey="income" name="Received" fill="#2f855a" radius={[5, 5, 0, 0]} /><Bar dataKey="expense" name="Spent" fill="#0c2340" radius={[5, 5, 0, 0]} /></BarChart></ResponsiveContainer></div></div>
        <div className="card"><h3>Balance by bank</h3>
          {d.bankSplit.length === 0 ? <Empty text="Link a bank account to see your split" /> : (<>
            <div style={{ height: 170 }}><ResponsiveContainer><PieChart><Pie data={d.bankSplit} dataKey="balance" nameKey="bank" innerRadius={48} outerRadius={78} paddingAngle={2}>{d.bankSplit.map((s) => <Cell key={s.bank} fill={s.color} />)}</Pie><Tooltip formatter={(v: number) => inr(v)} /></PieChart></ResponsiveContainer></div>
            {d.bankSplit.map((s) => <div key={s.bank} className="row between" style={{ padding: '4px 0' }}><span className="row" style={{ gap: 8 }}><BankDot color={s.color} name={s.bank} size={22} />{s.bank}</span><b className="mono">{inr(s.balance)}</b></div>)}</>)}
        </div>
      </div>
      <div className="card mt"><div className="row between"><h3>Recent activity</h3><Link to="/history" className="link">View all</Link></div>
        {d.recent.length ? d.recent.map((t) => <TxRow key={t.id} t={t} />) : <Empty text="No transactions yet. Deposit cash at a bank counter, then start paying." />}</div>
    </>
  );
}
