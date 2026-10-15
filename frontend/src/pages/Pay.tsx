import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import { Html5Qrcode } from 'html5-qrcode';
import { Camera, Copy, ImageUp, QrCode, ScanLine, Send, X } from 'lucide-react';
import api from '../api';
import { Account, Upi } from '../types';
import { PageHead, useToast } from '../ui';
import PayForm, { Prefill } from '../components/PayForm';

const upiUrl = (vpa: string, name: string, amount?: string, note?: string) => {
  const p = new URLSearchParams({ pa: vpa, pn: name, cu: 'INR' });
  if (amount && Number(amount) > 0) p.set('am', amount);
  if (note) p.set('tn', note);
  return `upi://pay?${p.toString()}`;
};
const parseQr = (text: string): Prefill => {
  try {
    if (text.toLowerCase().startsWith('upi://')) {
      const u = new URL(text); const pa = u.searchParams.get('pa');
      if (pa) return { vpa: pa, amount: u.searchParams.get('am') ?? undefined, remark: u.searchParams.get('tn') ?? undefined };
    }
  } catch { /* plain text */ }
  return { vpa: text.trim() };
};

export default function Pay() {
  const [params, setParams] = useSearchParams();
  const tab = params.get('tab') ?? 'send';
  const go = (t: string) => setParams({ tab: t });
  return (
    <>
      <PageHead title="Pay & receive" sub="Send money to any registered Meridian user — the payee is verified before you pay." />
      <div className="tabs">
        <button className={`tab ${tab === 'send' ? 'on' : ''}`} onClick={() => go('send')}><Send size={15} /> Send</button>
        <button className={`tab ${tab === 'scan' ? 'on' : ''}`} onClick={() => go('scan')}><ScanLine size={15} /> Scan QR</button>
        <button className={`tab ${tab === 'receive' ? 'on' : ''}`} onClick={() => go('receive')}><QrCode size={15} /> Receive</button>
      </div>
      <div style={{ maxWidth: tab === 'receive' ? 900 : 560 }}>
        {tab === 'send' && <PayForm />}
        {tab === 'scan' && <Scan />}
        {tab === 'receive' && <Receive />}
      </div>
    </>
  );
}

function Scan() {
  const toast = useToast();
  const [scanning, setScanning] = useState(false);
  const [found, setFound] = useState<Prefill | null>(null);
  const ref = useRef<Html5Qrcode | null>(null);
  const file = useRef<HTMLInputElement>(null);
  const stop = async () => { try { if (ref.current?.isScanning) await ref.current.stop(); ref.current?.clear(); } catch { /* ignore */ } setScanning(false); };
  useEffect(() => () => { stop(); }, []);
  const start = async () => {
    try {
      setScanning(true); await new Promise((r) => setTimeout(r, 60));
      const s = new Html5Qrcode('reader'); ref.current = s;
      await s.start({ facingMode: 'environment' }, { fps: 10, qrbox: 230 }, (txt) => { setFound(parseQr(txt)); stop(); }, () => {});
    } catch { setScanning(false); toast.err('Camera not available. Allow camera access (works on localhost) or upload a QR image.'); }
  };
  const fromFile = async (f?: File) => {
    if (!f) return;
    try { setFound(parseQr(await new Html5Qrcode('reader-file').scanFile(f, false))); } catch { toast.err('No QR code found in that image'); }
  };
  if (found) return (<><div className="row between mb"><b>QR scanned</b><button className="btn btn-sm" onClick={() => setFound(null)}><X size={14} /> Scan again</button></div>
    <PayForm key={found.vpa} prefill={found} channel="QR" lockRecipient lockAmount={!!found.amount} /></>);
  return (
    <div className="card qr-wrap">
      <div id="reader" style={{ display: scanning ? 'block' : 'none' }} /><div id="reader-file" style={{ display: 'none' }} />
      {!scanning && <ScanLine size={54} color="#0c2340" />}
      <div><b>{scanning ? 'Point the camera at a Meridian QR code' : 'Scan a QR code to pay'}</b><p className="muted">The payee name is shown before you confirm with your PIN.</p></div>
      <div className="row" style={{ justifyContent: 'center' }}>
        {scanning ? <button className="btn btn-danger" onClick={stop}><X size={15} /> Stop</button> : <button className="btn btn-primary" onClick={start}><Camera size={15} /> Open camera</button>}
        <button className="btn" onClick={() => file.current?.click()}><ImageUp size={15} /> Upload QR image</button>
        <input ref={file} type="file" accept="image/*" hidden onChange={(e) => fromFile(e.target.files?.[0])} />
      </div>
    </div>
  );
}

function Receive() {
  const toast = useToast();
  const [upis, setUpis] = useState<Upi[]>([]);
  const [accs, setAccs] = useState<Account[]>([]);
  const [vpa, setVpa] = useState('');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const name = accs[0]?.holderName ?? '';
  useEffect(() => { api.get('/c/upi').then((r) => { setUpis(r.data); setVpa(r.data[0]?.vpa ?? ''); }); api.get('/c/accounts').then((r) => setAccs(r.data)); }, []);
  const cur = upis.find((u) => u.vpa === vpa);
  if (!cur) return <div className="card empty">Link a bank account first to get your UPI ID and QR code.</div>;
  return (
    <div className="grid g2">
      <div className="card"><h3>Request a payment</h3>
        <div className="field"><label>UPI ID</label><select className="input" value={vpa} onChange={(e) => setVpa(e.target.value)}>{upis.map((u) => <option key={u.id} value={u.vpa}>{u.vpa}</option>)}</select>
          <span className="hint muted">Money lands in {cur.bankName} ({cur.accountMasked})</span></div>
        <div className="field"><label>Fixed amount (optional)</label><input className="input" type="number" min="1" value={amount} onChange={(e) => setAmount(e.target.value)} /></div>
        <div className="field"><label>Note (optional)</label><input className="input" value={note} onChange={(e) => setNote(e.target.value)} /></div></div>
      <div className="card qr-wrap"><div className="qr-box"><QRCodeSVG value={upiUrl(vpa, name, amount, note)} size={220} level="M" /></div>
        <div><b>{name}</b><div className="muted">{vpa}</div></div>
        <button className="btn" onClick={() => { navigator.clipboard?.writeText(vpa); toast.ok('UPI ID copied'); }}><Copy size={15} /> Copy UPI ID</button></div>
    </div>
  );
}
