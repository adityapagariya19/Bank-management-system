import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import Layout from './components/Layout';
import { LogoMark } from './components/Logo';
import { Role } from './types';
import Login from './pages/Login';
import Register from './pages/Register';
import Verify from './pages/Verify';
import Forgot from './pages/Forgot';
import Inbox from './pages/Inbox';
import Home from './pages/Home';
import Pay from './pages/Pay';
import Accounts from './pages/Accounts';
import History from './pages/History';
import Requests from './pages/Requests';
import Contacts from './pages/Contacts';
import Profile from './pages/Profile';
import Teller from './pages/Teller';
import Manager from './pages/Manager';
import Audit from './pages/Audit';
import Admin from './pages/Admin';
import Database from './pages/Database';

const HOME: Record<Role, string> = { CUSTOMER: '/', TELLER: '/teller', MANAGER: '/manager', AUDITOR: '/audit', ADMIN: '/admin' };

function Splash() { return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><LogoMark size={64} animate /></div>; }

function Guard({ roles }: { roles?: Role[] }) {
  const { user, loading } = useAuth();
  if (loading) return <Splash />;
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to={HOME[user.role]} replace />;
  return <Layout><Outlet /></Layout>;
}

function Root() {
  const { user } = useAuth();
  return user && user.role !== 'CUSTOMER' ? <Navigate to={HOME[user.role]} replace /> : <Home />;
}

export default function App() {
  const { user } = useAuth();
  const pub = (el: JSX.Element) => (user ? <Navigate to={HOME[user.role]} replace /> : el);
  return (
    <Routes>
      <Route path="/login" element={pub(<Login />)} /><Route path="/register" element={pub(<Register />)} />
      <Route path="/verify" element={<Verify />} /><Route path="/forgot" element={pub(<Forgot />)} /><Route path="/inbox" element={<Inbox />} />
      <Route element={<Guard roles={['CUSTOMER', 'TELLER', 'MANAGER', 'AUDITOR', 'ADMIN']} />}>
        <Route path="/" element={<Root />} /><Route path="/profile" element={<Profile />} />
      </Route>
      <Route element={<Guard roles={['CUSTOMER']} />}>
        <Route path="/pay" element={<Pay />} /><Route path="/accounts" element={<Accounts />} /><Route path="/history" element={<History />} />
        <Route path="/requests" element={<Requests />} /><Route path="/contacts" element={<Contacts />} />
      </Route>
      <Route element={<Guard roles={['TELLER', 'MANAGER', 'ADMIN']} />}><Route path="/teller" element={<Teller />} /></Route>
      <Route element={<Guard roles={['MANAGER', 'ADMIN']} />}><Route path="/manager" element={<Manager />} /></Route>
      <Route element={<Guard roles={['AUDITOR', 'MANAGER', 'ADMIN']} />}><Route path="/audit" element={<Audit />} /></Route>
      <Route element={<Guard roles={['AUDITOR', 'ADMIN']} />}><Route path="/database" element={<Database />} /></Route>
      <Route element={<Guard roles={['ADMIN']} />}><Route path="/admin" element={<Admin />} /></Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
