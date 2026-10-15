import { ReactNode, useEffect, useState } from 'react';
import api from '../api';
import { Bank } from '../types';
import { Logo } from '../components/Logo';

export default function AuthShell({ children }: { children: ReactNode }) {
  const [banks, setBanks] = useState<Bank[]>([]);
  useEffect(() => { api.get('/public/banks').then((r) => setBanks(r.data)).catch(() => {}); }, []);
  return (
    <div className="auth">
      <div className="auth-hero">
        <Logo onDark size={44} animate />
        <div>
          <h2>One account hub for <em>every bank</em> you use.</h2>
          <p>Link accounts from multiple banks, pay with UPI ID, QR or bank transfer, and keep every rupee traceable — with PIN-protected payments, OTP sign-in and a complete audit trail.</p>
        </div>
        <div>
          <p style={{ fontSize: '.75rem', letterSpacing: '.08em', textTransform: 'uppercase', marginBottom: 10 }}>Participating banks</p>
          <div className="bank-chips">{banks.map((b, i) => <span key={b.id} style={{ animationDelay: `${i * 80}ms` }}>{b.name}</span>)}</div>
        </div>
      </div>
      <div className="auth-form"><div className="auth-card">{children}</div></div>
    </div>
  );
}
