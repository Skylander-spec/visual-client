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

  const [upd, setUpd] = useState<{ available: boolean; version: string | null; current: string }>({
    available: false,
    version: null,
    current: ''
  })
  const [sucht, setSucht] = useState(false)

  useEffect(() => {
    window.visual.update.state().then(setUpd)
  }, [])

  async function suchen(): Promise<void> {
    setSucht(true)
    try {
      setUpd(await window.visual.update.check())
    } finally {
      setSucht(false)
    }
  }

  const [alte, setAlte] = useState<{ version: string; datum: string; laufend: boolean }[] | null>(
    null
  )
  const [holt, setHolt] = useState<string | null>(null)
  const [fehler, setFehler] = useState(false)

  async function alteZeigen(): Promise<void> {
    if (alte) return setAlte(null)
    setAlte(await window.visual.update.versions())
  }

  async function alteHolen(version: string): Promise<void> {
    setHolt(version)
    setFehler(false)
    try {
      const r = await window.visual.update.pick(version)
      if (r.ok) setUpd(await window.visual.update.state())
      else setFehler(true)
    } finally {
      setHolt(null)
    }
  }

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

        <div className="pset-h">✨ {tr('update.settings')}</div>
        <div className="muted" style={{ marginBottom: 10 }}>
          {tr('update.hint')}
        </div>
        <div className="row" style={{ gap: 10, alignItems: 'center', marginBottom: 22 }}>
          <span className="muted">
            {tr('update.current')}: <b>v{upd.current}</b>
          </span>
          <div className="grow" />
          {upd.available ? (
            <>
              <span className="muted">{translate(lang, 'update.ready', { v: upd.version ?? '' })}</span>
              <button className="btn primary" onClick={() => window.visual.update.apply()}>
                {tr('update.install')}
              </button>
            </>
          ) : (
            <>
              <span className="muted">{sucht ? '' : tr('update.upToDate')}</span>
              <button className="btn" disabled={sucht} onClick={suchen}>
                {sucht ? tr('update.checking') : tr('update.check')}
              </button>
            </>
          )}
        </div>

        <div className="row" style={{ gap: 10, alignItems: 'center', marginBottom: 8 }}>
          <span className="muted">{tr('update.older')}</span>
          <div className="grow" />
          <button className="btn" onClick={alteZeigen}>
            {alte ? tr('update.hide') : tr('update.show')}
          </button>
        </div>
        {alte && (
          <div style={{ marginBottom: 22 }}>
            <div className="muted" style={{ marginBottom: 8 }}>
              {tr('update.olderHint')}
            </div>
            {fehler && (
              <div className="muted" style={{ marginBottom: 8 }}>
                {tr('update.failed')}
              </div>
            )}
            {alte.map((f) => (
              <div
                key={f.version}
                className="row"
                style={{ gap: 10, alignItems: 'center', padding: '6px 0' }}
              >
                <b>v{f.version}</b>
                <span className="muted">{f.datum}</span>
                <div className="grow" />
                {f.laufend ? (
                  <span className="muted">{tr('update.running')}</span>
                ) : (
                  <button
                    className="btn"
                    disabled={holt !== null}
                    onClick={() => alteHolen(f.version)}
                  >
                    {holt === f.version ? tr('update.loading') : tr('update.load')}
                  </button>
                )}
              </div>
            ))}
          </div>
        )}

        <div className="pset-h">{tr('settings.storage')}</div>
        <div className="muted" style={{ lineHeight: 1.7 }}>
          {tr('settings.storageDesc')}
        </div>
      </div>
    </Modal>
  )
}
