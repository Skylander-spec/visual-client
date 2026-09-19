import { useEffect, useState } from 'react'
import Modal from './Modal'
import { LANGS, Lang, translate } from '../lib/i18n'

const ACCENTS: { key: string; a: string; a2: string }[] = [
  { key: 'color.teal', a: '#14b8a6', a2: '#2dd4bf' },
  { key: 'color.cyan', a: '#22d3ee', a2: '#67e8f9' },
  { key: 'color.blue', a: '#3b82f6', a2: '#60a5fa' },
  { key: 'color.violet', a: '#a855f7', a2: '#c084fc' },
  { key: 'color.pink', a: '#ec4899', a2: '#f472b6' },
  { key: 'color.green', a: '#22c55e', a2: '#4ade80' },
  { key: 'color.orange', a: '#f97316', a2: '#fb923c' },
  { key: 'color.red', a: '#ef4444', a2: '#f87171' },
  { key: 'color.yellow', a: '#eab308', a2: '#facc15' }
]

export function applyAccent(a: string, a2: string): void {
  const root = document.documentElement.style
  root.setProperty('--accent', a)
  root.setProperty('--accent-2', a2)
  root.setProperty('--accent-bg', a + '20')
  root.setProperty('--accent-soft', a)
}

export function bootAccent(): void {
  try {
    const saved = JSON.parse(localStorage.getItem('accent') ?? 'null')
    if (saved) applyAccent(saved.a, saved.a2)
  } catch {
    /* Standard-Teal */
  }
}

export default function SettingsModal({
  lang,
  onLang,
  onClose
}: {
  lang: Lang
  onLang: (l: Lang) => void
  onClose: () => void
}): JSX.Element {
  const tr = (k: string): string => translate(lang, k)
  const [current, setCurrent] = useState<string>(() => {
    try {
      return JSON.parse(localStorage.getItem('accent') ?? 'null')?.a ?? '#14b8a6'
    } catch {
      return '#14b8a6'
    }
  })

  function pick(a: string, a2: string): void {
    applyAccent(a, a2)
    localStorage.setItem('accent', JSON.stringify({ a, a2 }))
    setCurrent(a)
  }

  const [discordEnabled, setDiscordEnabled] = useState(true)

  useEffect(() => {
    window.visual.settings.get().then((s) => setDiscordEnabled(s.discordEnabled))
  }, [])

  async function saveDiscord(patch: { discordEnabled?: boolean }): Promise<void> {
    const s = await window.visual.settings.save(patch)
    setDiscordEnabled(s.discordEnabled)
  }

  return (
    <Modal
      title={tr('settings.title')}
      width={760}
      onClose={onClose}
      footer={
        <>
          <div className="grow" />
          <button className="btn primary" onClick={onClose}>
            {tr('common.done')}
          </button>
        </>
      }
    >
      <div style={{ padding: '4px 6px' }}>
        <div className="pset-h">🌐 {tr('settings.language')}</div>
        <div className="muted" style={{ marginBottom: 10 }}>
          {tr('settings.languageDesc')}
        </div>
        <div className="row" style={{ marginBottom: 22, flexWrap: 'wrap', gap: 8 }}>
          {LANGS.map((l) => (
            <button
              key={l.id}
              className={`vtile ${lang === l.id ? 'active' : ''}`}
              style={{ minWidth: 130 }}
              onClick={() => onLang(l.id)}
            >
              {l.flag} {l.label}
            </button>
          ))}
        </div>

        <div className="pset-h">{tr('settings.accent')}</div>
        <div className="muted" style={{ marginBottom: 10 }}>
          {tr('settings.accentDesc')}
        </div>
        <div className="row" style={{ flexWrap: 'wrap', gap: 10, marginBottom: 22 }}>
          {ACCENTS.map((c) => (
            <button
              key={c.key}
              title={tr(c.key)}
              onClick={() => pick(c.a, c.a2)}
              style={{
                width: 44,
                height: 44,
                borderRadius: 3,
                cursor: 'pointer',
                background: c.a,
                border: current === c.a ? '2px solid #fff' : '2px solid transparent',
                display: 'grid',
                placeItems: 'center',
                color: '#fff',
                fontSize: 16
              }}
            >
              {current === c.a ? '✓' : ''}
            </button>
          ))}
        </div>

        <div className="pset-h">💬 Discord</div>
        <div className="muted" style={{ marginBottom: 10 }}>
          {tr('settings.discordDesc')}
        </div>
        <label className="checkline" style={{ marginBottom: 8 }} onClick={(e) => e.preventDefault()}>
          <span
            className={`switch ${discordEnabled ? 'on' : ''}`}
            role="switch"
            aria-checked={discordEnabled}
            onClick={() => saveDiscord({ discordEnabled: !discordEnabled })}
          />
          {tr('settings.discordToggle')}
        </label>
        <div className="muted" style={{ marginBottom: 8 }}>
          {discordEnabled
            ? tr('settings.discordHint')
            : tr('settings.discordOff')}
        </div>

        <div className="pset-h">{tr('settings.storage')}</div>
        <div className="muted" style={{ lineHeight: 1.7 }}>
          {tr('settings.storageDesc')}
        </div>
      </div>
    </Modal>
  )
}
