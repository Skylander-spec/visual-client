import { lazy, Suspense, useEffect, useRef, useState } from 'react'
import type { Account, Profile } from '../env'
import { loaderInfo } from '../lib/loaders'
import { useT, useLang } from '../lib/i18n'
import { errorText } from '../lib/fehler'

// three.js/skinview3d ist schwer (~600 KB) — nur laden, wenn der Play-Screen
// wirklich angezeigt wird, statt beim App-Start.
const SkinView = lazy(() => import('../components/SkinView'))

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

  async function installVisuals(): Promise<void> {
    if (!active) return
    const r = await window.visual.visuals.install(active.id)
    setVisualsMsg(
      r.pack || r.mod
        ? `Installiert & aktiviert: ${[r.pack && 'Visual Pack', r.mod && 'VisualsMod'].filter(Boolean).join(' + ')}`
        : 'Assets fehlen — siehe README (Pipeline/Mod-Build).'
    )
  }

  const [perfBusy, setPerfBusy] = useState(false)
  async function boostFps(): Promise<void> {
    if (!active || perfBusy) return
    setPerfBusy(true)
    setVisualsMsg(t('home.optimizing'))
    try {
      const r = await window.visual.performance.apply(active.id)
      const parts = [
        'JVM-Flags gesetzt',
        `${r.options} Grafik-Settings optimiert`,
        r.modsInstalled.length ? `${r.modsInstalled.length} Performance-Mods installiert` : '',
        r.heavyDisabled.length ? `${r.heavyDisabled.length} FPS-Fresser deaktiviert` : ''
      ].filter(Boolean)
      setVisualsMsg('⚡ ' + parts.join(' · '))
    } catch (e) {
      setError(errorText(e, t))
    } finally {
      setPerfBusy(false)
    }
  }

  // Grosses Mod-Paket. Ueber 140 Eintraege — deshalb Fortschritt je Mod,
  // sonst sieht es minutenlang nach "haengt" aus.
  const [packBusy, setPackBusy] = useState(false)
  const [packText, setPackText] = useState('')
  useEffect(() => {
    return window.visual.pack.onStatus((s) => {
      setPackText(
        s.aktuell
          ? `${s.fertig}/${s.gesamt} · ${s.aktuell}`
          : `${s.installiert} ${t('home.packAdded')}, ${s.uebersprungen} ${t('home.packSkipped')}`
      )
    })
  }, [t])

  async function installPack(): Promise<void> {
    if (!active || packBusy) return
    setPackBusy(true)
    setPackText('')
    try {
      const r = await window.visual.pack.install(active.id)
      setVisualsMsg(
        `✦ ${r.installiert} ${t('home.packAdded')} · ${r.uebersprungen} ${t('home.packSkipped')}`
      )
    } catch (e) {
      setError(errorText(e, t))
    } finally {
      setPackBusy(false)
      setPackText('')
    }
  }

  const li = loaderInfo(active?.loader)
  const [skinAnim, setSkinAnim] = useState(() => localStorage.getItem('skinAnim') !== '0')

  function toggleAnim(): void {
    const next = !skinAnim
    setSkinAnim(next)
    localStorage.setItem('skinAnim', next ? '1' : '0')
  }

  return (
    <div className="play-grid">
      <div className="stage">
        <span className="corner">
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
              <span>{active.ramGb} GB</span>
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
                  {p.name} ({p.mcVersion} · {loaderInfo(p.loader).label})
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
        {visualsMsg && <div className="ok">{visualsMsg}</div>}
      </div>

      <aside className="news">
        <div className="head">▤ {t('news.title')}</div>
        <div
          className="ncard"
          style={{
            background:
              'linear-gradient(160deg, rgba(234,179,8,0.4), rgba(3,10,9,0.9) 70%), radial-gradient(200px 100px at 80% 20%, rgba(250,204,21,0.35), transparent), var(--bg-2)',
            opacity: perfBusy ? 0.6 : 1
          }}
          onClick={boostFps}
        >
          <div className="nt">{'⚡ ' + t('home.fpsBoost')}</div>
          <div className="nd">
            {perfBusy
              ? t('home.optimizing')
              : t('home.fpsBoostDesc')}
          </div>
        </div>
        <div
          className="ncard"
          style={{
            background:
              'linear-gradient(160deg, rgba(20,184,166,0.35), rgba(3,10,9,0.9) 70%), radial-gradient(200px 100px at 80% 20%, rgba(45,212,191,0.4), transparent), var(--bg-2)'
          }}
          onClick={installVisuals}
        >
          <div className="nt">✦ {t('news.visualPack')}</div>
          <div className="nd">{t('news.visualPackDesc')}</div>
        </div>
        <div
          className="ncard"
          style={{
            background:
              'linear-gradient(160deg, rgba(124,58,237,0.35), rgba(3,10,9,0.9) 70%), var(--bg-2)'
          }}
          onClick={onGoProfiles}
        >
          <div className="nt">▤ {t('nav.profiles')}</div>
          <div className="nd">{t('news.profilesDesc')}</div>
        </div>
        <div
          className="ncard"
          style={{
            background:
              'linear-gradient(160deg, rgba(234,179,8,0.3), rgba(3,10,9,0.9) 70%), var(--bg-2)'
          }}
        >
          <div className="nt">⚡ {t('news.ingame')}</div>
          <div className="nd">{t('news.ingameDesc')}</div>
        </div>
      </aside>
    </div>
  )
}
