/** Meridian mark: an "M" formed by two rising strokes, crowned by a gold sun-dot (the meridian). */
export function LogoMark({ size = 40, animate = false, onDark = false }: { size?: number; animate?: boolean; onDark?: boolean }) {
  return (
    <svg width={size} height={size} viewBox="0 0 48 48" className={animate ? 'logo-anim' : ''} aria-label="Meridian">
      <rect width="48" height="48" rx="12" fill={onDark ? '#ffffff' : '#0c2340'} />
      <path d="M12 35V17l12 12 12-12v18" stroke={onDark ? '#0c2340' : '#ffffff'} strokeWidth="4" fill="none" strokeLinecap="round" strokeLinejoin="round" className="logo-path" />
      <circle cx="24" cy="11" r="3.2" fill="#d4a24c" className="logo-dot" />
    </svg>
  );
}

export function Logo({ onDark = false, size = 38, animate = false }: { onDark?: boolean; size?: number; animate?: boolean }) {
  return (
    <div className="logo-lockup">
      <LogoMark size={size} onDark={onDark} animate={animate} />
      <div className="logo-text">
        <span className="logo-name" style={{ color: onDark ? '#fff' : '#0c2340' }}>Meridian</span>
        <span className="logo-tag" style={{ color: onDark ? 'rgba(255,255,255,.65)' : '#64748b' }}>Banking & Payments</span>
      </div>
    </div>
  );
}
