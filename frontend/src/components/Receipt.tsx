import { Printer } from 'lucide-react';
import { fmtDate, inr, label } from '../api';
import { Tx } from '../types';
import { Modal } from '../ui';

export function Tick() {
  return (
    <svg className="tick" viewBox="0 0 80 80" width="84" height="84">
      <circle className="tick-c" cx="40" cy="40" r="34" fill="none" strokeWidth="5" />
      <path className="tick-p" d="M24 41l11 11 21-23" fill="none" strokeWidth="6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function Receipt({ tx, success, onClose }: { tx: Tx; success?: boolean; onClose: () => void }) {
  const credit = tx.direction === 'CREDIT';
  return (
    <Modal title={success ? 'Payment successful' : 'Transaction details'} onClose={onClose}>
      <div className="receipt">
        {success && <Tick />}
        <div className={`rc-amt ${success ? '' : credit ? 'pos' : ''}`}>{inr(tx.amount)}</div>
        <div className="muted" style={{ marginBottom: 12 }}>{fmtDate(tx.createdAt)}</div>
        <div className="kv"><span>Status</span><span className="pos">Successful</span></div>
        {tx.fromName && <div className="kv"><span>From</span><span>{tx.fromName}<br /><small>{tx.fromBank} · {tx.fromAccount}</small></span></div>}
        {tx.toName && <div className="kv"><span>To</span><span>{tx.toName}<br /><small>{tx.toBank} · {tx.toAccount}</small></span></div>}
        {tx.toVpa && <div className="kv"><span>UPI ID</span><span>{tx.toVpa}</span></div>}
        <div className="kv"><span>Type</span><span>{label(tx.type)} via {label(tx.channel)}</span></div>
        {tx.remark && <div className="kv"><span>Remark</span><span>{tx.remark}</span></div>}
        {tx.performedBy && tx.type !== 'TRANSFER' && <div className="kv"><span>Handled by</span><span>{tx.performedBy}</span></div>}
        <div className="kv"><span>UTR / Reference</span><span className="mono">{tx.reference}</span></div>
        <div className="row mt" style={{ justifyContent: 'center' }}>
          <button className="btn" onClick={() => window.print()}><Printer size={16} /> Print</button>
          <button className="btn btn-primary" onClick={onClose}>Done</button>
        </div>
      </div>
    </Modal>
  );
}
