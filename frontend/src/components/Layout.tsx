import { ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { ArrowLeftRight, Banknote, Bell, Contact, Database, FileSearch, HandCoins, Landmark, LayoutDashboard, LogOut, QrCode, ShieldCheck, UserCog, UserRound, Users } from 'lucide-react';
import api, { fmtDate, initials, label } from '../api';
import { useAuth } from '../auth';
import { Notice, Role } from '../types';
import { Logo, LogoMark } from './Logo';

type Item = { to: string; icon: any; label: string };
const NAV: Record<Role, Item[]> = {
  CUSTOMER: [
    { to: '/', icon: LayoutDashboard, label: 'Home' }, { to: '/pay', icon: QrCode, label: 'Pay' }, { to: '/accounts', icon: Landmark, label: 'Banks' },
    { to: '/history', icon: ArrowLeftRight, label: 'History' }, { to: '/requests', icon: HandCoins, label: 'Requests' }, { to: '/contacts', icon: Contact, label: 'People' }, { to: '/profile', icon: UserRound, label: 'Profile' },
  ],
  TELLER: [{ to: '/teller', icon: Banknote, label: 'Counter' }, { to: '/profile', icon: UserRound, label: 'Profile' }],
  MANAGER: [{ to: '/manager', icon: ShieldCheck, label: 'Branch desk' }, { to: '/teller', icon: Banknote, label: 'Counter' }, { to: '/audit', icon: FileSearch, label: 'Audit trail' }, { to: '/profile', icon: UserRound, label: 'Profile' }],
  AUDITOR: [{ to: '/audit', icon: FileSearch, label: 'Audit trail' }, { to: '/database', icon: Database, label: 'Database' }, { to: '/profile', icon: UserRound, label: 'Profile' }],
  ADMIN: [{ to: '/admin', icon: UserCog, label: 'Administration' }, { to: '/manager', icon: ShieldCheck, label: 'Branch desk' }, { to: '/teller', icon: Banknote, label: 'Counter' }, { to: '/audit', icon: FileSearch, label: 'Audit trail' }, { to: '/database', icon: Database, label: 'Database' }, { to: '/profile', icon: UserRound, label: 'Profile' }],
};

function Bell_() {
  const [open, setOpen] = useState(false);
  const [n, setN] = useState<{ items: Notice[]; unread: number }>({ items: [], unread: 0 });
  const ref = useRef<HTMLDivElement>(null);
  const load = useCallback(() => api.get('/notifications').then((r) => setN(r.data)).catch(() => {}), []);
  useEffect(() => { load(); const t = setInterval(load, 15000); return () => clearInterval(t); }, [load]);
  useEffect(() => {
    const h = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false); };
    document.addEventListener('mousedown', h);
    return () => document.removeEventListener('mousedown', h);
  }, []);
  const toggle = async () => { setOpen(!open); if (!open && n.unread) { await api.post('/notifications/read-all'); setTimeout(load, 1200); } };
  return (
    <div ref={ref} style={{ position: 'relative' }}>
      <button className="icon-btn" onClick={toggle} aria-label="Notifications"><Bell size={20} />{n.unread > 0 && <span className="dot">{n.unread}</span>}</button>
      {open && (
        <div className="dropdown">
          <div className="note"><b>Notifications</b></div>
          {n.items.length === 0 && <div className="empty">Nothing here yet</div>}
          {n.items.map((x) => <div key={x.id} className={`note ${x.seen ? '' : 'new'}`}><b>{x.title}</b><p>{x.message}</p><p style={{ fontSize: '.7rem' }}>{fmtDate(x.createdAt)}</p></div>)}
        </div>
      )}
    </div>
  );
}

export default function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const loc = useLocation();
  const items = NAV[user!.role];
  return (
    <div className="shell">
      <aside className="sidebar">
        <Logo onDark size={36} />
        <nav>
          <div className="nav-sec">{user!.role === 'CUSTOMER' ? 'Menu' : label(user!.role)}</div>
          {items.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.to === '/'} className={({ isActive }) => 'nav-item' + (isActive ? ' active' : '')}><n.icon size={18} />{n.label === 'Pay' ? 'Pay & Receive' : n.label}</NavLink>
          ))}
        </nav>
        <div className="side-user">
          <div className="avatar">{initials(user!.fullName)}</div>
          <div className="grow"><b>{user!.fullName}</b><span>{label(user!.role)}</span></div>
          <button className="icon-btn" style={{ color: '#9fb3cf' }} title="Sign out" onClick={logout}><LogOut size={17} /></button>
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <div className="mobile-only" style={{ marginRight: 'auto' }}><LogoMark size={30} /></div>
          <Bell_ />
          <button className="icon-btn mobile-logout" onClick={logout} title="Sign out"><LogOut size={19} /></button>
        </header>
        <main className="content" key={loc.pathname}>{children}</main>
      </div>
      <nav className="mobile-nav">
        {items.slice(0, 5).map((n) => <NavLink key={n.to} to={n.to} end={n.to === '/'} className={({ isActive }) => (isActive ? 'active' : '')}><n.icon size={20} />{n.label}</NavLink>)}
      </nav>
    </div>
  );
}
