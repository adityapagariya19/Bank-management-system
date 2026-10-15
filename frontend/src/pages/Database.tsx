import { useEffect, useMemo, useState } from 'react';
import { Eye, Key, Link2, Play, Table2 } from 'lucide-react';
import api, { errMsg } from '../api';
import { QueryResult, Relation, TableData, TableInfo } from '../types';
import { PageHead, Skeleton, useToast } from '../ui';

const SAMPLES: [string, string][] = [
  ['Balance per bank', 'SELECT b.name AS bank, COUNT(a.id) AS accounts, SUM(a.balance) AS total_balance\nFROM banks b LEFT JOIN bank_accounts a ON a.bank_id = b.id\nGROUP BY b.id, b.name ORDER BY total_balance DESC'],
  ['Customer statement (JOIN)', 'SELECT t.reference, t.created_at, t.type, t.channel, t.amount, fu.full_name AS payer, tu.full_name AS payee\nFROM transactions t\nLEFT JOIN bank_accounts fa ON fa.id = t.from_account_id LEFT JOIN users fu ON fu.id = fa.owner_id\nLEFT JOIN bank_accounts ta ON ta.id = t.to_account_id LEFT JOIN users tu ON tu.id = ta.owner_id\nORDER BY t.created_at DESC'],
  ['Running balance (window fn)', 'SELECT account_number, created_at, amount,\n  SUM(amount) OVER (PARTITION BY account_number ORDER BY created_at) AS running_credit\nFROM (SELECT a.account_number, t.created_at, t.amount FROM transactions t JOIN bank_accounts a ON a.id = t.to_account_id) x'],
  ['Users above avg balance (subquery)', 'SELECT u.full_name, SUM(a.balance) AS total\nFROM users u JOIN bank_accounts a ON a.owner_id = u.id\nGROUP BY u.id, u.full_name\nHAVING total > (SELECT AVG(balance) FROM bank_accounts)'],
  ['Audit: failed events', "SELECT created_at, actor_email, action, details FROM audit_logs WHERE success = 0 ORDER BY created_at DESC"],
  ['View: bank summary', 'SELECT * FROM v_bank_summary'],
];

export default function Database() {
  const [tab, setTab] = useState('tables');
  return (
    <>
      <PageHead title="Database explorer" sub="Live, read-only view of the MySQL schema, data and relationships — built for the demo." />
      <div className="tabs"><button className={`tab ${tab === 'tables' ? 'on' : ''}`} onClick={() => setTab('tables')}><Table2 size={15} /> Tables & data</button><button className={`tab ${tab === 'er' ? 'on' : ''}`} onClick={() => setTab('er')}><Link2 size={15} /> ER diagram</button><button className={`tab ${tab === 'sql' ? 'on' : ''}`} onClick={() => setTab('sql')}><Play size={15} /> SQL console</button></div>
      {tab === 'tables' && <Tables />}{tab === 'er' && <ER />}{tab === 'sql' && <Sql />}
    </>
  );
}

function Tables() {
  const toast = useToast();
  const [tables, setTables] = useState<TableInfo[] | null>(null);
  const [sel, setSel] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<TableData | null>(null);
  useEffect(() => { api.get('/db/tables').then((r) => { setTables(r.data); setSel(r.data.find((t: TableInfo) => t.name === 'users')?.name ?? r.data[0]?.name ?? ''); }).catch((e) => toast.err(errMsg(e))); }, []); // eslint-disable-line
  useEffect(() => { if (!sel) return; setData(null); api.get(`/db/tables/${sel}`, { params: { page, size: 12 } }).then((r) => setData(r.data)).catch((e) => toast.err(errMsg(e))); }, [sel, page]); // eslint-disable-line
  if (!tables) return <Skeleton h={240} />;
  return (
    <div className="grid" style={{ gridTemplateColumns: '260px minmax(0,1fr)' }}>
      <div className="card" style={{ padding: 12, alignSelf: 'start' }}>
        {['TABLE', 'VIEW'].map((kind) => (<div key={kind}><div className="nav-sec" style={{ color: '#64748b' }}>{kind === 'TABLE' ? 'Tables' : 'Views'}</div>
          {tables.filter((t) => t.type === kind).map((t) => (<button key={t.name} className={`bank-pick ${sel === t.name ? 'on' : ''}`} style={{ padding: '8px 10px', marginBottom: 4 }} onClick={() => { setSel(t.name); setPage(0); }}>
            {kind === 'VIEW' ? <Eye size={15} /> : <Table2 size={15} />}<span className="grow" style={{ fontSize: '.85rem', fontWeight: 600 }}>{t.name}</span><span className="muted">{t.rows}</span></button>))}</div>))}
      </div>
      <div className="card" style={{ minWidth: 0 }}>
        {!data ? <Skeleton h={200} /> : (<>
          <div className="row between mb"><h3 style={{ margin: 0 }}>{data.table}</h3><span className="muted">{data.total} rows · {data.columns.length} columns</span></div>
          <div className="table-wrap mb"><table className="table"><thead><tr><th>Column</th><th>Type</th><th>Key</th><th>Null</th></tr></thead><tbody>{data.columns.map((c) => <tr key={c.name}><td><b>{c.name}</b></td><td>{c.type}</td><td>{c.key === 'PRI' ? <span className="row" style={{ gap: 4 }}><Key size={13} color="#d4a24c" />PK</span> : c.key === 'MUL' ? 'FK / index' : c.key === 'UNI' ? 'Unique' : ''}</td><td>{c.nullable ? 'YES' : 'NO'}</td></tr>)}</tbody></table></div>
          <div className="table-wrap"><table className="table"><thead><tr>{data.columns.map((c) => <th key={c.name}>{c.name}</th>)}</tr></thead><tbody>{data.rows.map((r, i) => <tr key={i}>{r.map((v, j) => <td key={j} title={String(v ?? '')}>{v === null ? <i className="muted">NULL</i> : String(v)}</td>)}</tr>)}</tbody></table></div>
          <div className="row between mt"><button className="btn btn-sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button><span className="muted">Page {page + 1}</span><button className="btn btn-sm" disabled={(page + 1) * data.size >= data.total} onClick={() => setPage(page + 1)}>Next</button></div></>)}
      </div>
    </div>
  );
}

function ER() {
  const [tables, setTables] = useState<TableInfo[]>([]);
  const [rels, setRels] = useState<Relation[]>([]);
  useEffect(() => { api.get('/db/tables').then((r) => setTables(r.data.filter((t: TableInfo) => t.type === 'TABLE'))); api.get('/db/relations').then((r) => setRels(r.data)); }, []);
  const layout = useMemo(() => {
    const W = 190, H = 62, cols = 4, gx = 70, gy = 70;
    const pos: Record<string, { x: number; y: number }> = {};
    tables.forEach((t, i) => { pos[t.name] = { x: 30 + (i % cols) * (W + gx), y: 30 + Math.floor(i / cols) * (H + gy) }; });
    return { pos, W, H, width: 60 + cols * (W + gx), height: 60 + Math.ceil(tables.length / cols) * (H + gy) };
  }, [tables]);
  if (!tables.length) return <Skeleton h={240} />;
  const { pos, W, H } = layout;
  return (
    <div className="card"><p className="muted mb">Each arrow is a foreign key (child → parent), read live from <code>information_schema</code>.</p>
      <div className="er"><svg width={layout.width} height={layout.height}>
        <defs><marker id="ar" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0 0L10 5L0 10z" fill="#64748b" /></marker></defs>
        {rels.map((r, i) => { const a = pos[r.table], b = pos[r.refTable]; if (!a || !b || r.table === r.refTable) return null;
          const x1 = a.x + W / 2, y1 = a.y + H / 2, x2 = b.x + W / 2, y2 = b.y + H / 2; return <g key={i}><line x1={x1} y1={y1} x2={x2} y2={y2} stroke="#94a3b8" strokeWidth="1.4" markerEnd="url(#ar)" /><text x={(x1 + x2) / 2} y={(y1 + y2) / 2 - 3} fontSize="9" fill="#64748b" textAnchor="middle">{r.column}</text></g>; })}
        {tables.map((t) => (<g key={t.name}><rect x={pos[t.name].x} y={pos[t.name].y} width={W} height={H} rx="10" fill="#fff" stroke="#0c2340" strokeWidth="1.6" /><rect x={pos[t.name].x} y={pos[t.name].y} width={W} height={26} rx="10" fill="#0c2340" /><rect x={pos[t.name].x} y={pos[t.name].y + 16} width={W} height={10} fill="#0c2340" />
          <text x={pos[t.name].x + 12} y={pos[t.name].y + 18} fontSize="12" fontWeight="700" fill="#fff">{t.name}</text><text x={pos[t.name].x + 12} y={pos[t.name].y + 46} fontSize="11" fill="#475569">{t.columns} columns · {t.rows} rows</text></g>))}
      </svg></div>
      <table className="table mt"><thead><tr><th>Child table</th><th>Column</th><th>References</th></tr></thead><tbody>{rels.map((r, i) => <tr key={i}><td>{r.table}</td><td>{r.column}</td><td>{r.refTable}.{r.refColumn}</td></tr>)}</tbody></table></div>
  );
}

function Sql() {
  const toast = useToast();
  const [sql, setSql] = useState(SAMPLES[0][1]);
  const [res, setRes] = useState<QueryResult | null>(null);
  const [err, setErr] = useState('');
  const run = async () => { setErr(''); try { setRes((await api.post('/db/query', { sql })).data); } catch (e) { setRes(null); setErr(errMsg(e)); toast.err('Query failed'); } };
  return (
    <div className="card">
      <div className="chips mb">{SAMPLES.map(([n, q]) => <button key={n} className="chip" onClick={() => setSql(q)}>{n}</button>)}</div>
      <textarea className="input sql" value={sql} onChange={(e) => setSql(e.target.value)} onKeyDown={(e) => { if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') run(); }} spellCheck={false} />
      <div className="row between mt"><span className="muted">Read-only: SELECT / SHOW / DESCRIBE / EXPLAIN · max 200 rows · Ctrl+Enter to run</span><button className="btn btn-primary" onClick={run}><Play size={15} /> Run query</button></div>
      {err && <p className="err-text mt">{err}</p>}
      {res && (<div className="mt"><p className="muted mb">{res.count} row(s) in {res.ms} ms{res.truncated ? ' (truncated)' : ''}</p><div className="table-wrap"><table className="table"><thead><tr>{res.columns.map((c) => <th key={c}>{c}</th>)}</tr></thead><tbody>{res.rows.map((r, i) => <tr key={i}>{r.map((v, j) => <td key={j}>{v === null ? <i className="muted">NULL</i> : String(v)}</td>)}</tr>)}</tbody></table></div></div>)}
    </div>
  );
}
