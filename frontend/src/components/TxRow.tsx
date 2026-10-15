import { ArrowDownLeft, ArrowUpRight, Banknote, Repeat } from 'lucide-react';
import { fmtDate, inr, label } from '../api';
import { Tx } from '../types';

export function title(t: Tx) {
  if (t.type === 'CASH_DEPOSIT') return 'Cash deposit';
  if (t.type === 'CASH_WITHDRAWAL') return 'Cash withdrawal';
  if (t.direction === 'CREDIT') return `Received from ${t.fromName}`;
  if (t.direction === 'DEBIT') return `Paid to ${t.toName}`;
  if (t.direction === 'SELF') return 'Between your accounts';
  return `${t.fromName} → ${t.toName}`;
}
export function subtitle(t: Tx) {
  const bank = t.direction === 'CREDIT' ? t.fromBank : t.toBank;
  return [bank, label(t.channel), fmtDate(t.createdAt)].filter(Boolean).join(' · ');
}

export default function TxRow({ t, onClick }: { t: Tx; onClick?: () => void }) {
  const Icon = t.type !== 'TRANSFER' ? Banknote : t.direction === 'CREDIT' ? ArrowDownLeft : t.direction === 'DEBIT' ? ArrowUpRight : Repeat;
  const sign = t.direction === 'CREDIT' ? '+' : t.direction === 'DEBIT' ? '−' : '';
  return (
    <div className={`tx ${onClick ? 'clickable' : ''}`} onClick={onClick}>
      <div className={`tx-ic ${t.direction}`}><Icon size={19} /></div>
      <div className="tx-main"><b>{title(t)}</b><span>{t.remark ? `${t.remark} · ` : ''}{subtitle(t)}</span></div>
      <div className={`tx-amt ${t.direction === 'CREDIT' ? 'pos' : t.direction === 'DEBIT' ? 'neg' : ''}`}>{sign}{inr(t.amount)}</div>
    </div>
  );
}
