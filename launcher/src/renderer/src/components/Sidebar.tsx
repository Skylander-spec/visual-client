import Logo from './Logo'
import { useT } from '../lib/i18n'

export type Page = 'home' | 'profiles' | 'mods' | 'skins' | 'capes' | 'settings'

const NAV: { id: Page; key: string; icon: string }[] = [
  { id: 'home', key: 'nav.play', icon: '▶' },
  { id: 'profiles', key: 'nav.profiles', icon: '▤' },
  { id: 'mods', key: 'nav.mods', icon: '⬇' },
  { id: 'skins', key: 'nav.skins', icon: '☻' },
  { id: 'capes', key: 'nav.capes', icon: '▧' }
]

export default function Sidebar({
  page,
  onNavigate
}: {
  page: Page
  onNavigate: (p: Page) => void
}): JSX.Element {
  const t = useT()
  return (
    <nav className="sidebar">
      <div className="logo-top">
        <Logo size={34} />
      </div>
      {NAV.map((n) => (
        <button
          key={n.id}
          className={`nav ${page === n.id ? 'active' : ''}`}
          onClick={() => onNavigate(n.id)}
        >
          <span className="ico">{n.icon}</span>
          {t(n.key)}
        </button>
      ))}
      <div className="foot">
        <button
          className={`nav ${page === 'settings' ? 'active' : ''}`}
          style={{ width: '100%' }}
          onClick={() => onNavigate('settings')}
        >
          <span className="ico">⚙</span>
        </button>
      </div>
    </nav>
  )
}
