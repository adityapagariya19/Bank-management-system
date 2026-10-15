import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Download, Printer, Search } from 'lucide-react';
import api, { fmtDate, inr, label } from '../api';
import { Account, Page, Tx } from '../types';
import { Empty, PageHead, Skeleton } from '../ui';
import TxRow, { title } from '../components/TxRow';
import Receipt from '../components/Receipt';

export default function History() {
  const [params] = useSearchParams();
  const [accs, setAccs] = useState<Account[]>([]);
  const [account, setAccount] = useState(params.get('account') ?? '');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Tx> | null>(null);
  const [open, setOpen] = useState<Tx | null>(null);
  useEffect(() => { api.get('/c/accounts').then((r) => setAccs(r.data)); }, []);
  useEffect(() => { setPage(0); }, [account, from, to]);
  useEffect(() => {
    setData(null);
    api.get('/c/transactions', { params: { accountId: account || undefined, from: from || undefined, to: to || undefined, page, size: 12 } }).then((r) => setData(r.data));
  }, [account, from, to, page]);
  const rows = useMemo(() => (data?.items ?? []).filter((t) => !q || `${title(t)} ${t.remark ?? ''} ${t.reference}`.toLowerCase().includes(q.toLowerCase())), [data, q]);
  const csv = () => {
    const head = 'UTR,Date,Type,Channel,Details,Debit,Credit,Balance\n';
    const body = rows.map((t) => [t.reference, fmtDate(t.createdAt), label(t.type), label(t.channel), `"${title(t)}${t.remark ? ' - ' + t.remark : ''}"`, t.direction === 'DEBIT' ? t.amount : '', t.direction === 'CREDIT' ? t.amount : '', t.balanceAfter ?? ''].join(',')).join('\n');
    const url = URL.createObjectURL(new Blob([head + body], { type: 'text/csv' }));
    Object.assign(document.createElement('a'), { href: url, download: 'meridian-statement.csv' }).click(); URL.revokeObjectURL(url);
  };
  return (
    <>
      <PageHead title="Transaction history" sub="Filter by account and date. Pick one account to see running balances.">
        <button className="btn" onClick={csv}><Download size={15} /> CSV</button><button className="btn" onClick={() => window.print()}><Printer size={15} /> Print</button></PageHead>
      <div className="card">
        <div className="row mb">
          <select className="input" style={{ width: 230 }} value={account} onChange={(e) => setAccount(e.target.value)}><option value="">All accounts</option>{accs.map((a) => <option key={a.id} value={a.id}>{a.bankShortName} · {a.accountNumber.slice(-4)}</option>)}</select>
          <input className="input" type="date" style={{ width: 160 }} value={from} onChange={(e) => setFrom(e.target.value)} /><input className="input" type="date" style={{ width: 160 }} value={to} onChange={(e) => setTo(e.target.value)} />
          <div className="grow" style={{ position: 'relative', minWidth: 180 }}><Search size={15} style={{ position: 'absolute', left: 12, top: 13, color: '#64748b' }} /><input className="input" style={{ paddingLeft: 34 }} placeholder="Search name, remark, UTR" value={q} onChange={(e) => setQ(e.target.value)} /></div>
        </div>
        {!data ? <Skeleton h={200} /> : rows.length === 0 ? <Empty text="No transactions found" /> : rows.map((t) => (<div key={t.id}><TxRow t={t} onClick={() => setOpen(t)} />{t.balanceAfter != null && <div className="muted" style={{ textAlign: 'right', marginTop: -8, paddingBottom: 6, fontSize: '.72rem' }}>Balance {inr(t.balanceAfter)}</div>}</div>))}
        {data && data.totalPages > 1 && <div className="row between mt"><button className="btn btn-sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><span className="muted">Page {page + 1} of {data.totalPages} · {data.total} transactions</span><button className="btn btn-sm" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Next</button></div>}
      </div>
      {open && <Receipt tx={open} onClose={() => setOpen(null)} />}
    </>
  );
}
