import { useCallback, useEffect, useMemo, useState } from 'react'
import type { Loader, Profile } from '../env'
import { LOADERS, loaderInfo } from '../lib/loaders'
import Modal from './Modal'
import { useT } from '../lib/i18n'

type Tab = 'allgemein' | 'installation' | 'java' | 'fenster' | 'visuals'

const TABS: { id: Tab; labelKey: string; icon: string }[] = [
  { id: 'allgemein', labelKey: 'pset.tab.general', icon: '⚙' },
  { id: 'installation', labelKey: 'pset.tab.install', icon: '⬇' },
  { id: 'java', labelKey: 'pset.tab.java', icon: '</>' },
  { id: 'fenster', labelKey: 'pset.tab.window', icon: '▦' },
  { id: 'visuals', labelKey: 'pset.tab.visuals', icon: '✦' }
]

const RES_PRESETS: [string, number, number][] = [
  ['720p', 1280, 720],
  ['1080p', 1920, 1080],
  ['1440p', 2560, 1440],
  ['4K', 3840, 2160]
]

export default function ProfileSettings({
  profile,
  onClose,
  onSaved
}: {
  profile: Profile | null
  onClose: () => void
  onSaved: () => void
}): JSX.Element {
  const t = useT()
  const [tab, setTab] = useState<Tab>(profile ? 'allgemein' : 'installation')
  const [versions, setVersions] = useState<string[]>([])
  const [versionQuery, setVersionQuery] = useState('')
  const [sysRam, setSysRam] = useState(16384)
  const [msg, setMsg] = useState('')

  const [name, setName] = useState(profile?.name ?? 'Neues Profil')
  const [group, setGroup] = useState(profile?.group ?? '')
  const [mcVersion, setMcVersion] = useState(profile?.mcVersion ?? '1.21.9')
  const [loader, setLoader] = useState<Loader>(profile?.loader ?? 'fabric')
  const [ramMb, setRamMb] = useState(profile?.ramMb ?? (profile?.ramGb ?? 4) * 1024)
  const [customJava, setCustomJava] = useState(!!profile?.javaPath)
  const [javaPath, setJavaPath] = useState(profile?.javaPath ?? '')
  const [customArgs, setCustomArgs] = useState(!!profile?.javaArgs)
  const [javaArgs, setJavaArgs] = useState(profile?.javaArgs ?? '')
  const [winWidth, setWinWidth] = useState(profile?.winWidth ?? 854)
  const [winHeight, setWinHeight] = useState(profile?.winHeight ?? 480)
  const [fullscreen, setFullscreen] = useState(profile?.fullscreen ?? false)
  // Neues Profil: standardmaessig an — sonst startet ein frisch
  // eingerichteter Client ohne den Visual-Mod.
  const [autoVisuals, setAutoVisuals] = useState(profile ? (profile.autoVisuals ?? false) : true)
  const [repairing, setRepairing] = useState(false)

  const [versionsFehler, setVersionsFehler] = useState(false)

  /**
   * Die Versionsliste holen.
   *
   * Hier stand ein .catch(() => {}). Ein kurzer Netzhaenger liess die
   * Liste damit dauerhaft leer - und eine leere Liste heisst: keine
   * Kacheln, also keine Version waehlbar, ohne ein Wort dazu. Genau das
   * ist passiert.
   *
   * Jetzt wird der Fehler gezeigt, einmal nachgefasst, und es bleibt ein
   * Rueckfall auf die Versionen, die der Launcher ohnehin unterstuetzt -
   * damit ist die Auswahl nie leer.
   */
  const RUECKFALL = [
    '1.21.11', '1.21.10', '1.21.9', '1.21.8', '1.21.7', '1.21.6',
    '1.21.5', '1.21.4', '1.21.3', '1.21.2', '1.21.1', '1.21',
    '1.20.6', '1.20.4', '1.20.1', '1.19.4', '1.18.2', '1.16.5',
    '1.12.2', '1.8.9'
  ]

  const versionenLaden = useCallback(async (): Promise<void> => {
    try {
      const liste = await window.visual.profiles.mcVersions()
      if (liste.length > 0) {
        setVersions(liste)
        setVersionsFehler(false)
        return
      }
      throw new Error('leere Liste')
    } catch {
      setVersions(RUECKFALL)
      setVersionsFehler(true)
    }
  }, [])

  useEffect(() => {
    void versionenLaden()
    window.visual.profiles.systemRamMb().then(setSysRam).catch(() => {})
  }, [versionenLaden])

  // Kein Deckel mehr: die 60er-Grenze schnitt alles ab 1.14 abwärts weg —
  // ausgerechnet 1.8.9, die meistgespielte PvP-Version, fehlte dadurch.
  const filteredVersions = useMemo(() => {
    const q = versionQuery.trim()
    return q ? versions.filter((v) => v.includes(q)) : versions
  }, [versions, versionQuery])

  async function save(): Promise<void> {
    if (!name.trim()) {
      setMsg(t('pset.nameRequired'))
      return
    }
    await window.visual.profiles.save({
      id: profile?.id,
      name: name.trim(),
      group: group.trim() || undefined,
      mcVersion,
      loader,
      ramGb: Math.max(1, Math.round(ramMb / 1024)),
      ramMb,
      javaPath: customJava && javaPath.trim() ? javaPath.trim() : undefined,
      javaArgs: customArgs && javaArgs.trim() ? javaArgs.trim() : undefined,
      winWidth,
      winHeight,
      fullscreen,
      autoVisuals
    })
    onSaved()
    onClose()
  }

  async function repair(): Promise<void> {
    if (!profile) return
    setRepairing(true)
    setMsg('')
    let ok = 0
    for (const mod of profile.mods) {
      try {
        await window.visual.mods.install(profile.id, mod.projectId, (mod.kind as never) ?? 'mod')
        ok++
      } catch {
        /* einzelner Fehler — weiter */
      }
    }
    setMsg(t('pset.repairDone', { ok, gesamt: profile.mods.length }))
    setRepairing(false)
    onSaved()
  }

  const li = loaderInfo(loader)

  return (
    <Modal
      title={t('pset.title')}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose}>
            {t('pset.cancel')}
          </button>
          <div className="grow" />
          {msg && <span className="ok" style={{ marginRight: 12 }}>{msg}</span>}
          <button className="btn primary" onClick={save}>
            {t('pset.saveChanges')}
          </button>
        </>
      }
    >
      <div className="pset">
        <div className="pset-nav">
          {TABS.map((reiter) => (
            <button
              key={reiter.id}
              className={`pset-tab ${tab === reiter.id ? 'active' : ''}`}
              onClick={() => setTab(reiter.id)}
            >
              <span className="i">{reiter.icon}</span>
              {t(reiter.labelKey)}
            </button>
          ))}
        </div>

        <div className="pset-body">
          {tab === 'allgemein' && (
            <>
              <div className="row" style={{ alignItems: 'flex-end', gap: 16 }}>
                <label className="field grow">
                  {t('pset.profileName')}
                  <input value={name} onChange={(e) => setName(e.target.value)} />
                </label>
                <label className="field grow">
                  {t('pset.group')}
                  <input
                    value={group}
                    onChange={(e) => setGroup(e.target.value)}
                    placeholder={t('pset.groupHint')}
                  />
                </label>
              </div>
              {profile && (
                <div className="muted" style={{ marginTop: 16 }}>
                  {t('pset.createdInfo', {
                    datum: new Date(profile.createdAt).toLocaleDateString(),
                    n: profile.mods.length
                  })}
                </div>
              )}
            </>
          )}

          {tab === 'installation' && (
            <>
              <div className="pset-h">{t('pset.installedNow')}</div>
              <div className="row" style={{ marginBottom: 18 }}>
                <span style={{ fontFamily: 'var(--font-pixel)', fontSize: 15 }}>⛏ {mcVersion}</span>
                <span className={`lb ${li.className}`}>{li.label}</span>
              </div>
              <div className="pset-h">{t('pset.gameVersion')}</div>
              <input
                style={{ width: '100%', marginBottom: 12 }}
                placeholder={t('pset.searchVersions')}
                value={versionQuery}
                onChange={(e) => setVersionQuery(e.target.value)}
              />
              {versionsFehler && (
                <div className="row" style={{ gap: 8, marginBottom: 10, alignItems: 'center' }}>
                  <span className="muted">{t('pset.versionsOffline')}</span>
                  <button className="btn" onClick={() => void versionenLaden()}>
                    {t('pset.retry')}
                  </button>
                </div>
              )}
              <div className="vgrid">
                {filteredVersions.map((v) => (
                  <button
                    key={v}
                    className={`vtile ${v === mcVersion ? 'active' : ''}`}
                    onClick={() => setMcVersion(v)}
                  >
                    {v}
                  </button>
                ))}
              </div>
              <div className="pset-h" style={{ marginTop: 18 }}>
                {t('pset.platform')}
              </div>
              <div className="row" style={{ flexWrap: 'wrap', gap: 8 }}>
                {LOADERS.map((l) => (
                  <button
                    key={l.id}
                    className={`vtile ${loader === l.id ? 'active' : ''}`}
                    style={{ minWidth: 110 }}
                    onClick={() => setLoader(l.id)}
                  >
                    {l.label}
                  </button>
                ))}
              </div>
            </>
          )}

          {tab === 'java' && (
            <>
              <div className="pset-h">{t('pset.memory')}</div>
              <div className="rambox">
                <div style={{ fontFamily: 'var(--font-pixel)', fontSize: 17 }}>
                  {ramMb} MB ({(ramMb / 1024).toFixed(1)} GB)
                </div>
                <div className="row" style={{ justifyContent: 'space-between', marginTop: 6 }}>
                  <span className="muted">512 MB</span>
                  <span className="muted">{sysRam} MB</span>
                </div>
                <input
                  type="range"
                  min={512}
                  max={sysRam}
                  step={512}
                  value={Math.min(ramMb, sysRam)}
                  onChange={(e) => setRamMb(Number(e.target.value))}
                  style={{ width: '100%' }}
                />
                <div className="muted">{t('pset.recommended')}</div>
              </div>
              <label className="checkline">
                <input type="checkbox" checked={customJava} onChange={(e) => setCustomJava(e.target.checked)} />
                {t('pset.customJava')}
              </label>
              {customJava ? (
                <input
                  style={{ width: '100%', marginBottom: 12 }}
                  placeholder={t('pset.javaPath')}
                  value={javaPath}
                  onChange={(e) => setJavaPath(e.target.value)}
                />
              ) : (
                <div className="muted" style={{ marginBottom: 12 }}>
                  {t('pset.defaultJava')}
                </div>
              )}
              <label className="checkline">
                <input type="checkbox" checked={customArgs} onChange={(e) => setCustomArgs(e.target.checked)} />
                {t('pset.customArgs')}
              </label>
              {customArgs && (
                <input
                  style={{ width: '100%' }}
                  placeholder="-XX:+UseG1GC …"
                  value={javaArgs}
                  onChange={(e) => setJavaArgs(e.target.value)}
                />
              )}
            </>
          )}

          {tab === 'fenster' && (
            <>
              <div className="pset-h">{t('pset.windowSettings')}</div>
              <div className="muted" style={{ marginBottom: 14 }}>
                {t('pset.windowConfig')}
              </div>
              <div className="row" style={{ gap: 16 }}>
                <label className="field grow">
                  {t('pset.width')}
                  <input
                    type="number"
                    value={winWidth}
                    onChange={(e) => setWinWidth(Number(e.target.value))}
                  />
                </label>
                <label className="field grow">
                  {t('pset.height')}
                  <input
                    type="number"
                    value={winHeight}
                    onChange={(e) => setWinHeight(Number(e.target.value))}
                  />
                </label>
              </div>
              <div className="row" style={{ marginTop: 12, flexWrap: 'wrap', gap: 8 }}>
                <button
                  className="vtile"
                  onClick={() => {
                    setWinWidth(854)
                    setWinHeight(480)
                  }}
                >
                  Default
                </button>
                {RES_PRESETS.map(([label, w, h]) => (
                  <button
                    key={label}
                    className={`vtile ${winWidth === w && winHeight === h ? 'active' : ''}`}
                    onClick={() => {
                      setWinWidth(w)
                      setWinHeight(h)
                    }}
                  >
                    {label}
                  </button>
                ))}
              </div>
              <label className="checkline" style={{ marginTop: 16 }}>
                <input
                  type="checkbox"
                  checked={fullscreen}
                  onChange={(e) => setFullscreen(e.target.checked)}
                />
                {t('pset.fullscreen')}
              </label>
            </>
          )}

          {tab === 'visuals' && (
            <>
              <div className="pset-h">{t('pset.tab.info')}</div>
              <div className="muted" style={{ marginBottom: 16, lineHeight: 1.6 }}>
                {t('pset.visualsDesc')}
              </div>
              <label className="checkline">
                <input
                  type="checkbox"
                  checked={autoVisuals}
                  onChange={(e) => setAutoVisuals(e.target.checked)}
                />
                {t('pset.autoVisuals')}
              </label>
              <div className="pset-h" style={{ marginTop: 20 }}>
                {t('pset.repairTitle')}
              </div>
              <div className="muted" style={{ marginBottom: 10 }}>
                {t('pset.repairDesc')}
              </div>
              <button className="btn" onClick={repair} disabled={!profile || repairing}>
                {repairing ? t('pset.repairing') : '✓ ' + t('pset.repair')}
              </button>
            </>
          )}
        </div>
      </div>
    </Modal>
  )
}
