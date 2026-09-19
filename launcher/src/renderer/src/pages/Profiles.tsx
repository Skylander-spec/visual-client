import { useEffect, useRef, useState } from 'react'
import type { Profile } from '../env'
import { loaderInfo } from '../lib/loaders'
import ProfileSettings from '../components/ProfileSettings'
import { useT } from '../lib/i18n'

/**
 * Relative Zeitangabe. Die Einheiten standen fest auf Deutsch („vor 4T“) —
 * sie kommen jetzt aus dem Wörterbuch, damit sie der Sprache folgen.
 */
function timeAgo(
  iso: string | undefined,
  t: (key: string, vars?: Record<string, string | number>) => string
): string {
  if (!iso) return t('time.never')
  const s = (Date.now() - new Date(iso).getTime()) / 1000
  if (s < 3600) return t('time.minutes', { n: Math.max(1, Math.floor(s / 60)) })
  if (s < 86400) return t('time.hours', { n: Math.floor(s / 3600) })
  if (s < 86400 * 30) return t('time.days', { n: Math.floor(s / 86400) })
  return t('time.months', { n: Math.floor(s / 86400 / 30) })
}

export default function Profiles({
  profiles,
  onChanged,
  onPlay,
  onMods
}: {
  profiles: Profile[]
  onChanged: () => void
  onPlay: (id: string) => void
  onMods: (id: string) => void
}): JSX.Element {
  const t = useT()
  const [editing, setEditing] = useState<Profile | null>(null)
  const [creating, setCreating] = useState(false)
  const [menuFor, setMenuFor] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const close = (e: MouseEvent): void => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuFor(null)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const shown = query.trim()
    ? profiles.filter((p) => p.name.toLowerCase().includes(query.toLowerCase()))
    : profiles

  return (
    <>
      <div className="row" style={{ marginBottom: 16 }}>
        <div className="grow">
          <h1>{t('profiles.title')}</h1>
        </div>
        <button className="btn tabchip" onClick={() => setCreating(true)}>
          {'⊞ ' + t('profiles.create')}
        </button>
      </div>
      <div className="row" style={{ marginBottom: 16 }}>
        <input
          className="grow"
          placeholder={t('profiles.search')}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      <div className="list">
        {shown.map((p) => {
          const li = loaderInfo(p.loader)
          return (
            <div key={p.id} className="card hover row prow" style={{ position: 'relative' }}>
              <div className={`ptile ${li.className}`}>{li.label.slice(0, 1)}</div>
              <div className="grow">
                <div className="row" style={{ gap: 8 }}>
                  <span className="pname">{p.name}</span>
                  {p.group && <span className="badge gray">{p.group}</span>}
                </div>
                <div
                  className="muted"
                  style={{ marginTop: 4, fontFamily: 'var(--font-pixel)', fontSize: 11 }}
                >
                  ⛏ {p.mcVersion} <span style={{ opacity: 0.4 }}>|</span> ◆ {li.label}{' '}
                  <span style={{ opacity: 0.4 }}>|</span> {timeAgo(p.lastPlayed, t)}
                </div>
              </div>
              <button className="btn primary tabchip" onClick={() => onPlay(p.id)}>
                {'▶ ' + t('play.launch')}
              </button>
              <button className="btn tabchip" onClick={() => onMods(p.id)}>
                ◫ Mods
              </button>
              <button
                className="btn"
                title={t('common.options')}
                onClick={() => setMenuFor(menuFor === p.id ? null : p.id)}
              >
                ⚙
              </button>
              {menuFor === p.id && (
                <div className="ctxmenu" ref={menuRef}>
                  <button
                    onClick={() => {
                      setMenuFor(null)
                      setEditing(p)
                    }}
                  >
                    ⚙ Profil bearbeiten
                  </button>
                  <button
                    onClick={async () => {
                      setMenuFor(null)
                      await window.visual.profiles.duplicate(p.id)
                      onChanged()
                    }}
                  >
                    ⧉ Duplizieren
                  </button>
                  <button
                    onClick={async () => {
                      setMenuFor(null)
                      await window.visual.profiles.exportZip(p.id)
                    }}
                  >
                    ⬇ Exportieren
                  </button>
                  <button
                    onClick={() => {
                      setMenuFor(null)
                      window.visual.profiles.openFolder(p.id)
                    }}
                  >
                    ▤ Ordner öffnen
                  </button>
                  <hr />
                  <button
                    className="danger"
                    onClick={async () => {
                      setMenuFor(null)
                      await window.visual.profiles.remove(p.id)
                      onChanged()
                    }}
                  >
                    ✕ Löschen
                  </button>
                </div>
              )}
            </div>
          )
        })}
        {shown.length === 0 && (
          <div className="muted">
            {profiles.length === 0 ? t('profiles.none') : t('profiles.noMatch')}
          </div>
        )}
      </div>

      {(editing || creating) && (
        <ProfileSettings
          profile={editing}
          onClose={() => {
            setEditing(null)
            setCreating(false)
          }}
          onSaved={onChanged}
        />
      )}
    </>
  )
}
