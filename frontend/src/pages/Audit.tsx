import { useEffect, useState } from 'react';
import api, { fmtDate, inr, label } from '../api';
import { Audit as AuditT, Page, Tx } from '../types';
import { PageHead, Pill, Skeleton } from '../ui';

export default function Audit() {
  const [tab, setTab] = useState('logs');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [logs, setLogs] = useState<Page<AuditT> | null>(null);
  const [txs, setTxs] = useState<Page<Tx> | null>(null);
  useEffect(() => { setPage(0); }, [tab, q]);
  useEffect(() => {
    if (tab === 'logs') api.get('/audit/logs', { params: { q, page, size: 15 } }).then((r) => setLogs(r.data));
    else api.get('/audit/transactions', { params: { page, size: 15 } }).then((r) => setTxs(r.data));
  }, [tab, q, page]);
  const pager = (p: Page<unknown> | null) => p && p.totalPages > 1 && (<div className="row between mt"><button className="btn btn-sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><span className="muted">Page {page + 1} of {p.totalPages} · {p.total} records</span><button className="btn btn-sm" disabled={page + 1 >= p.totalPages} onClick={() => setPage(page + 1)}>Next</button></div>);
  return (
    <>
      <PageHead title="Audit trail" sub="Every sign-in, PIN failure, payment and staff action is recorded." />
      <div className="tabs"><button className={`tab ${tab === 'logs' ? 'on' : ''}`} onClick={() => setTab('logs')}>Audit log</button><button className={`tab ${tab === 'tx' ? 'on' : ''}`} onClick={() => setTab('tx')}>All transactions</button></div>
      <div className="card">
        {tab === 'logs' ? (<><input className="input mb" placeholder="Filter by action, user or details (e.g. PIN_FAILED, LOGIN)" value={q} onChange={(e) => setQ(e.target.value)} />
          {!logs ? <Skeleton h={200} /> : <div className="table-wrap"><table className="table"><thead><tr><th>Time</th><th>User</th><th>Action</th><th>Entity</th><th>Details</th><th>IP</th><th>Result</th></tr></thead><tbody>
            {logs.items.map((a) => <tr key={a.id}><td>{fmtDate(a.createdAt)}</td><td>{a.actorEmail ?? '—'}</td><td><b>{a.action}</b></td><td>{a.entity} {a.entityId}</td><td title={a.details ?? ''}>{a.details}</td><td>{a.ip}</td><td><Pill value={a.success ? 'SUCCESS' : 'FAILED'} /></td></tr>)}</tbody></table></div>}{pager(logs)}</>)
        : (!txs ? <Skeleton h={200} /> : <><div className="table-wrap"><table className="table"><thead><tr><th>UTR</th><th>Time</th><th>Type</th><th>Channel</th><th>From</th><th>To</th><th>Amount</th><th>By</th></tr></thead><tbody>
          {txs.items.map((t) => <tr key={t.id}><td className="mono">{t.reference}</td><td>{fmtDate(t.createdAt)}</td><td>{label(t.type)}</td><td>{label(t.channel)}</td><td>{t.fromName ?? '—'}<div className="muted">{t.fromBank}</div></td><td>{t.toName ?? '—'}<div className="muted">{t.toBank}</div></td><td className="mono">{inr(t.amount)}</td><td>{t.performedBy}</td></tr>)}</tbody></table></div>{pager(txs)}</>)}
      </div>
    </>
  );
}
