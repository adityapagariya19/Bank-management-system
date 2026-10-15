import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from 'react';
import api from './api';
import { User } from './types';

interface Ctx {
  user: User | null; loading: boolean;
  signIn: (data: { token: string; user: User }) => void;
  refresh: () => Promise<void>;
  logout: () => void;
}
const AuthContext = createContext<Ctx>(null as unknown as Ctx);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => { setUser((await api.get('/me')).data); }, []);
  useEffect(() => {
    if (!localStorage.getItem('token')) { setLoading(false); return; }
    refresh().catch(() => localStorage.removeItem('token')).finally(() => setLoading(false));
  }, [refresh]);

  const signIn = (d: { token: string; user: User }) => { localStorage.setItem('token', d.token); setUser(d.user); };
  const logout = () => { localStorage.removeItem('token'); setUser(null); };
  return <AuthContext.Provider value={{ user, loading, signIn, refresh, logout }}>{children}</AuthContext.Provider>;
}
