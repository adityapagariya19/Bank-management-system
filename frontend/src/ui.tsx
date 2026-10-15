import { createContext, ReactNode, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { CheckCircle2, X, XCircle } from 'lucide-react';

interface T { id: number; msg: string; kind: 'ok' | 'err' }
const ToastCtx = createContext<{ ok: (m: string) => void; err: (m: string) => void }>({ ok: () => {}, err: () => {} });
export const useToast = () => useContext(ToastCtx);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [list, setList] = useState<T[]>([]);
  const push = useCallback((msg: string, kind: 'ok' | 'err') => {
    const id = Date.now() + Math.random();
    setList((l) => [...l, { id, msg, kind }]);
    setTimeout(() => setList((l) => l.filter((t) => t.id !== id)), 4200);
  }, []);
  return (
    <ToastCtx.Provider value={{ ok: (m) => push(m, 'ok'), err: (m) => push(m, 'err') }}>
      {children}
      <div className="toasts">
        {list.map((t) => (
          <div key={t.id} className={`toast ${t.kind}`}>{t.kind === 'ok' ? <CheckCircle2 size={18} /> : <XCircle size={18} />}<span>{t.msg}</span></div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}

export function Modal({ title, onClose, children, wide }: { title: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  useEffect(() => {
    const h = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [onClose]);
  return (
    <div className="overlay" onMouseDown={onClose}>
      <div className={`modal ${wide ? 'wide' : ''}`} onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-h"><h3>{title}</h3><button className="icon-btn" onClick={onClose} aria-label="Close"><X size={18} /></button></div>
        {children}
      </div>
    </div>
  );
}

export const Spinner = () => <div className="spinner-wrap"><div className="spinner" /></div>;
export const Skeleton = ({ h = 90 }: { h?: number }) => <div className="skeleton" style={{ height: h }} />;
export const Empty = ({ text, icon }: { text: string; icon?: ReactNode }) => <div className="empty">{icon}<p>{text}</p></div>;
export const PageHead = ({ title, sub, children }: { title: string; sub?: string; children?: ReactNode }) => (
  <div className="page-head"><div><h1>{title}</h1>{sub && <p className="muted">{sub}</p>}</div><div className="row">{children}</div></div>
);

const TONES: Record<string, string> = {
  ACTIVE: 'green', APPROVED: 'green', SUCCESS: 'green', PAID: 'green', CREDIT: 'green',
  PENDING: 'amber', PENDING_VERIFICATION: 'amber', NOT_SUBMITTED: 'gray', FROZEN: 'amber',
  REJECTED: 'red', DISABLED: 'red', CLOSED: 'gray', DECLINED: 'red', CANCELLED: 'gray', FAILED: 'red',
};
export const Pill = ({ value, tone }: { value: string; tone?: string }) => (
  <span className={`pill ${tone ?? TONES[value] ?? 'blue'}`}>{value.replace(/_/g, ' ').toLowerCase()}</span>
);

/** Animated count-up for amounts. */
export function CountUp({ value, format }: { value: number; format: (n: number) => string }) {
  const [v, setV] = useState(0);
  const raf = useRef(0);
  useEffect(() => {
    const start = performance.now(), dur = 700, from = 0;
    const tick = (t: number) => {
      const p = Math.min(1, (t - start) / dur);
      setV(from + (value - from) * (1 - Math.pow(1 - p, 3)));
      if (p < 1) raf.current = requestAnimationFrame(tick);
    };
    raf.current = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf.current);
  }, [value]);
  return <>{format(v)}</>;
}

export function BankDot({ color, name, size = 36 }: { color: string; name: string; size?: number }) {
  return <span className="bank-dot" style={{ background: color, width: size, height: size, fontSize: size * 0.38 }}>{name.slice(0, 2).toUpperCase()}</span>;
}
