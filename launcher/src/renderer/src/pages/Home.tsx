import { lazy, Suspense, useEffect, useRef, useState } from 'react'
import type { Account, Profile } from '../env'
import { loaderInfo } from '../lib/loaders'
import { useT, useLang } from '../lib/i18n'
import { errorText } from '../lib/fehler'

// three.js/skinview3d ist schwer (~600 KB) — nur laden, wenn der Play-Screen
// wirklich angezeigt wird, statt beim App-Start.
const SkinView = lazy(() => import('../components/SkinView'))
const Panorama = lazy(() => import('../components/Panorama'))

export default function Home({
  profiles,
  active,
  onSelect,
  onGoProfiles,
  account,
  autoPlay,
  onAutoPlayed
}: {
  profiles: Profile[]
  active: Profile | null
  onSelect: (id: string) => void
  onGoProfiles: () => void
  account: Account
  autoPlay?: boolean
  onAutoPlayed?: () => void
}): JSX.Element {
  const t = useT()
  const lang = useLang()
  const [status, setStatus] = useState<{
    key: string
    vars?: Record<string, string | number>
    progress: number
  } | null>(null)
  const [launching, setLaunching] = useState(false)
  const [error, setError] = useState('')
  const [visualsMsg, setVisualsMsg] = useState('')
  const resetTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    const offStatus = window.visual.launch.onStatus((s) => {
      setStatus(s)
      setLaunching(true)
      // Sobald das Spiel gestartet ist, Button wieder freigeben
      // (mehrere Instanzen möglich)
      if (s.progress >= 100) {
        if (resetTimer.current) clearTimeout(resetTimer.current)
        resetTimer.current = setTimeout(() => {
          setLaunching(false)
          setStatus(null)
        }, 1500)
      }
    })
    return () => {
      offStatus()
      if (resetTimer.current) clearTimeout(resetTimer.current)
    }
  }, [])

  useEffect(() => {
    if (autoPlay && active) {
      onAutoPlayed?.()
      play()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoPlay, active?.id])

  async function play(): Promise<void> {
    if (!active) return
    setError('')
    setVisualsMsg('')
    setLaunching(true)
    setStatus({ key: 'play.starting', progress: 0 })
    const res = await window.visual.launch.start(active.id, lang)
    if (!res.ok) {
      setError(res.errorKey ? t(res.errorKey, res.errorVars) : t('home.launchFailed'))
      setLaunching(false)
      setStatus(null)
    }
  }

  const rust = active?.engine === 'horizon'
  const li = rust ? { label: 'Rust · Beta' } : loaderInfo(active?.loader)
  const [skinAnim, setSkinAnim] = useState(() => localStorage.getItem('skinAnim') !== '0')
  const [panoramaAnim, setPanoramaAnim] = useState(() => localStorage.getItem('panoramaAnim') !== '0')

  function toggleAnim(): void {
    const next = !skinAnim
    setSkinAnim(next)
    localStorage.setItem('skinAnim', next ? '1' : '0')
  }

  return (
    <div className="play-grid visual-play">
      <div className="stage landscape-stage">
        <Suspense fallback={null}><Panorama animated={panoramaAnim} /></Suspense>
        <span className="corner">
          <button className="panorama-toggle" aria-pressed={panoramaAnim} onClick={() => setPanoramaAnim(value => { localStorage.setItem('panoramaAnim', value ? '0' : '1'); return !value })}>{t('engine.panorama')} {panoramaAnim ? 'Ⅱ' : '▶'}</button>
          <label className="checkline" style={{ margin: 0, cursor: 'pointer' }} onClick={toggleAnim}>
            <span
              className={`switch ${skinAnim ? 'on' : ''}`}
              role="switch"
              aria-checked={skinAnim}
            />
            {t('play.skinAnim')}
          </label>
        </span>
        <div className="pname-float">{account.name}</div>
        <Suspense
          fallback={
            <div
              style={{
                height: 380,
                display: 'grid',
                placeItems: 'center',
                color: 'var(--text-faint)',
                fontFamily: 'var(--font-pixel)',
                fontSize: 11
              }}
            >
              lädt …
            </div>
          }
        >
          <SkinView name={account.name} animate={skinAnim} />
        </Suspense>
        <div className="status-line">
          {active ? (
            <>
              <span>⛏ {active.mcVersion}</span>
              <span>·</span>
              <span>
                {active.mods.length} {t('play.mods')}
              </span>
              <span>·</span>
              <span>{rust ? 'Rust · Beta' : `${active.ramGb} GB`}</span>
            </>
          ) : (
            <span>{t('play.noProfile')}</span>
          )}
        </div>

        {profiles.length === 0 ? (
          <div className="launchbar">
            <button className="main" onClick={onGoProfiles}>
              {t('play.createProfile')}
            </button>
          </div>
        ) : (
          <div className="launchbar">
            <button className="main" onClick={play} disabled={launching || !active}>
              {launching ? (status ? t(status.key, status.vars) : t('play.starting')) : t('play.launch')}
              {!launching && active && (
                <small>
                  {li.label} {active.mcVersion} — {active.name}
                </small>
              )}
            </button>
            <select
              value={active?.id ?? ''}
              onChange={(e) => onSelect(e.target.value)}
              title={t('home.chooseProfile')}
            >
              {profiles.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.mcVersion} · {p.engine === 'horizon' ? 'Rust · Beta' : loaderInfo(p.loader).label})
                </option>
              ))}
            </select>
          </div>
        )}
        {launching && status && (
          <div className="progress">
            <div style={{ width: `${status.progress}%` }} />
          </div>
        )}
        {error && <div className="error">{error}</div>}
        {rust && <button className="btn" disabled={launching} onClick={async () => {
          if (!active) return
          try { await window.visual.horizon.setup(active.id); setError(''); setVisualsMsg(t('engine.setupOpened')) }
          catch (err) { setError(errorText(err, t)) }
        }}>{t('engine.setup')} ↗</button>}
        {visualsMsg && <div className="ok">{visualsMsg}</div>}
      </div>


    </div>
  )
}
