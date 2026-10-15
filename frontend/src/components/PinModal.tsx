import { useEffect, useState } from 'react';
import { Delete, Lock, ShieldCheck } from 'lucide-react';
import { Modal } from '../ui';

interface Props { title?: string; subtitle?: string; busy?: boolean; error?: string; maxLen?: number; onSubmit: (pin: string) => void; onClose: () => void }

/** Numeric PIN pad (4 or 6 digits). Also accepts keyboard input. */
export default function PinModal({ title = 'Enter your PIN', subtitle, busy, error, maxLen = 6, onSubmit, onClose }: Props) {
  const [pin, setPin] = useState('');
  useEffect(() => { if (error) setPin(''); }, [error]);
  useEffect(() => {
    const h = (e: KeyboardEvent) => {
      if (busy) return;
      if (/^\d$/.test(e.key)) setPin((p) => (p.length < maxLen ? p + e.key : p));
      else if (e.key === 'Backspace') setPin((p) => p.slice(0, -1));
    };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [busy, maxLen]);

  const ready = pin.length === 4 || pin.length === 6;
  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'x', '0', 'ok'];
  return (
    <Modal title={title} onClose={onClose}>
      <div className="pin-wrap">
        {subtitle && <p className="muted center">{subtitle}</p>}
        <div className="pin-dots">{Array.from({ length: maxLen }).map((_, i) => <span key={i} className={i < pin.length ? 'on' : ''} />)}</div>
        <div className="pin-msg">{error ? <span className="err-text">{error}</span> : <span className="muted"><Lock size={12} /> 4 or 6 digit bank PIN</span>}</div>
        <div className="keypad">
          {keys.map((k) => k === 'x' ? (
            <button key={k} className="key" onClick={() => setPin(pin.slice(0, -1))} disabled={busy} aria-label="Backspace"><Delete size={20} /></button>
          ) : k === 'ok' ? (
            <button key={k} className="key ok" disabled={!ready || busy} onClick={() => onSubmit(pin)}>{busy ? <span className="mini-spin" /> : <ShieldCheck size={22} />}</button>
          ) : (
            <button key={k} className="key" disabled={busy} onClick={() => setPin((p) => (p.length < maxLen ? p + k : p))}>{k}</button>
          ))}
        </div>
      </div>
    </Modal>
  );
}
