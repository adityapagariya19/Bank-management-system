import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Send, Trash2, UserPlus } from 'lucide-react';
import api, { errMsg } from '../api';
import { Contact } from '../types';
import { Empty, PageHead, useToast } from '../ui';

export default function Contacts() {
  const toast = useToast();
  const [list, setList] = useState<Contact[]>([]);
  const [vpa, setVpa] = useState('');
  const [nick, setNick] = useState('');
  const load = () => api.get('/c/contacts').then((r) => setList(r.data));
  useEffect(() => { load(); }, []);
  const add = async (e: FormEvent) => { e.preventDefault(); try { const r = await api.post('/c/contacts', { vpa, nickname: nick }); toast.ok(`${r.data.name} added`); setVpa(''); setNick(''); load(); } catch (err) { toast.err(errMsg(err)); } };
  return (
    <>
      <PageHead title="People" sub="Saved contacts are verified against registered accounts." />
      <div className="grid g2">
        <form className="card" onSubmit={add}><h3>Add contact</h3>
          <div className="field"><label>UPI ID or registered mobile</label><input className="input" value={vpa} onChange={(e) => setVpa(e.target.value)} required /></div>
          <div className="field"><label>Nickname (optional)</label><input className="input" value={nick} onChange={(e) => setNick(e.target.value)} /></div>
          <button className="btn btn-primary btn-block"><UserPlus size={16} /> Verify & save</button></form>
        <div className="card"><h3>Your contacts</h3>{list.length === 0 && <Empty text="No contacts yet" />}
          {list.map((c) => (<div className="tx" key={c.id}><div className="avatar" style={{ background: '#0c2340', color: '#fff' }}>{c.name[0]}</div><div className="tx-main"><b>{c.nickname || c.name}</b><span>{c.name} · {c.vpa}</span></div>
            <Link className="btn btn-sm" to={`/pay?tab=send`} onClick={() => sessionStorage.setItem('payto', c.vpa)}><Send size={13} /> Pay</Link><button className="icon-btn" onClick={async () => { await api.delete(`/c/contacts/${c.id}`); load(); }}><Trash2 size={16} /></button></div>))}</div>
      </div>
    </>
  );
}
