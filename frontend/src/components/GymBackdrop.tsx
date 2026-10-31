import { useLocation } from 'react-router-dom'

export default function GymBackdrop() {
  const { pathname } = useLocation()
  const hero = pathname === '/login' || pathname === '/register'

  return (
    <div aria-hidden="true" className={`gym-bg ${hero ? '' : 'gym-bg--dim'}`}>
      <div className="gym-deco">
        <div className="gym-stars" />
        <div className="gym-spot gym-spot-1" />
        <div className="gym-spot gym-spot-2" />
        <div className="gym-spot gym-spot-3" />
        <div className="gym-slats" />
        <div className="gym-floor" />

        <svg className="gym-obj gym-rack" viewBox="0 0 520 260">
          <g fill="#14171c" stroke="#2a2f38" strokeWidth="2">
            <rect x="20" y="70" width="480" height="14" rx="4" />
            <rect x="20" y="160" width="480" height="14" rx="4" />
            <rect x="40" y="70" width="12" height="190" />
            <rect x="468" y="70" width="12" height="190" />
            <rect x="70" y="40" width="26" height="30" rx="4" />
            <rect x="96" y="50" width="40" height="10" />
            <rect x="136" y="40" width="26" height="30" rx="4" />
            <rect x="190" y="40" width="26" height="30" rx="4" />
            <rect x="216" y="50" width="40" height="10" />
            <rect x="256" y="40" width="26" height="30" rx="4" />
            <rect x="310" y="32" width="30" height="38" rx="4" />
            <rect x="340" y="46" width="40" height="10" />
            <rect x="380" y="32" width="30" height="38" rx="4" />
            <rect x="70" y="130" width="26" height="30" rx="4" />
            <rect x="96" y="140" width="40" height="10" />
            <rect x="136" y="130" width="26" height="30" rx="4" />
            <rect x="190" y="124" width="30" height="36" rx="4" />
            <rect x="220" y="137" width="40" height="10" />
            <rect x="260" y="124" width="30" height="36" rx="4" />
            <rect x="320" y="118" width="34" height="42" rx="4" />
            <rect x="354" y="134" width="40" height="10" />
            <rect x="394" y="118" width="34" height="42" rx="4" />
          </g>
        </svg>

        <div className="gym-obj gym-plate">
          <div className="gym-plate-inner">
            <svg viewBox="0 0 200 200" style={{ width: '100%', height: '100%' }}>
              <circle cx="100" cy="100" r="96" fill="#171a20" stroke="#2f3540" strokeWidth="3" />
              <circle cx="100" cy="100" r="78" fill="none" stroke="#c6ff3d" strokeOpacity=".5" strokeWidth="2" />
              <circle cx="100" cy="100" r="58" fill="none" stroke="#2f3540" strokeWidth="2" />
              <circle cx="100" cy="100" r="16" fill="#0b0c0e" stroke="#3a414d" strokeWidth="3" />
            </svg>
          </div>
        </div>

        <div className="gym-obj gym-barbell">
          <div className="gym-barbell-inner">
            <svg viewBox="0 0 260 70" style={{ width: '100%', height: 'auto', opacity: 0.85 }}>
              <rect x="10" y="32" width="240" height="6" rx="3" fill="#3a414d" />
              <rect x="40" y="6" width="14" height="58" rx="4" fill="#1c2027" stroke="#c6ff3d" strokeOpacity=".6" />
              <rect x="56" y="12" width="10" height="46" rx="3" fill="#232832" stroke="#3a414d" />
              <rect x="206" y="6" width="14" height="58" rx="4" fill="#1c2027" stroke="#c6ff3d" strokeOpacity=".6" />
              <rect x="194" y="12" width="10" height="46" rx="3" fill="#232832" stroke="#3a414d" />
            </svg>
          </div>
        </div>
      </div>
      <div className="gym-vignette" />
    </div>
  )
}