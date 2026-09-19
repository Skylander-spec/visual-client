import { useCallback, useEffect, useState } from 'react'
import type { InstalledItem, Profile, ProjectType, SearchHit, UpdateInfo } from '../env'
import { loaderInfo } from '../lib/loaders'
import { useT } from '../lib/i18n'
import { errorText } from '../lib/fehler'

const TYPES: { id: ProjectType; label: string }[] = [
  { id: 'mod', label: 'Mods' },
  { id: 'resourcepack', label: 'Resourcepacks' },
  { id: 'shader', label: 'Shader' }
]

const KIND_LABEL: Record<string, string> = {
  mod: 'Mod',
  resourcepack: 'Resourcepack',
  shader: 'Shader'
}

const KIND_GROUP: { kind: ProjectType; label: string; icon: string }[] = [
  { kind: 'mod', label: 'Mods', icon: '◧' },
  { kind: 'resourcepack', label: 'Resource Packs', icon: '▤' },
  { kind: 'shader', label: 'Shader', icon: '✦' }
]

const MOD_CATEGORIES: [string, string][] = [
  ['', 'Alle'],
  ['optimization', 'Performance'],
  ['utility', 'Utility'],
  ['adventure', 'Adventure'],
  ['decoration', 'Deko'],
  ['equipment', 'Equipment'],
  ['library', 'Libraries']
]

export default function Mods({
  profiles,
  active,
  onSelect,
  onChanged,
  request
}: {
  profiles: Profile[]
  active: Profile | null
  onSelect: (id: string) => void
  onChanged: () => void
  request?: { tab: 'browse' | 'installed'; n: number }
}): JSX.Element {
  const t = useT()
  const [query, setQuery] = useState('')
  const [hits, setHits] = useState<SearchHit[]>([])
  const [total, setTotal] = useState(0)
  const [offset, setOffset] = useState(0)
  const [busy, setBusy] = useState<string | null>(null)
  const [updates, setUpdates] = useState<UpdateInfo[] | null>(null)
  const [msg, setMsg] = useState('')
  const [tab, setTab] = useState<'browse' | 'installed'>('installed')
  const [contentType, setContentType] = useState<ProjectType>('mod')
  const [sort, setSort] = useState<'relevance' | 'downloads' | 'newest' | 'follows'>('downloads')
  const [category, setCategory] = useState('')
  const [installed, setInstalled] = useState<InstalledItem[]>([])

  // Tab von außen setzen (z. B. Klick auf „Mods" beim Profil -> Installiert)
  useEffect(() => {
    if (request) setTab(request.tab)
  }, [request?.n]) // eslint-disable-line react-hooks/exhaustive-deps

  const reloadInstalled = useCallback(async () => {
    if (!active) return
    setInstalled(await window.visual.mods.installed(active.id))
  }, [active?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    reloadInstalled()
  }, [reloadInstalled, active?.mods.length])

  const search = useCallback(
    async (q: string, off = 0) => {
      if (!active) return
      setBusy('search')
      setMsg('')
      try {
        const res = await window.visual.mods.search({
          query: q,
          mcVersion: active.mcVersion,
          loader: active.loader ?? 'forge',
          projectType: contentType,
          offset: off,
          sort,
          category: contentType === 'mod' && category ? category : undefined
        })
        setHits(res.hits)
        setTotal(res.total_hits)
        setOffset(off)
      } catch {
        setMsg(t('mods.offline'))
      } finally {
        setBusy(null)
      }
    },
    [active, sort, category, contentType]
  )

  useEffect(() => {
    if (active && tab === 'browse') search('')
  }, [active?.id, active?.mcVersion, active?.loader, sort, category, contentType, tab]) // eslint-disable-line react-hooks/exhaustive-deps

  if (!active) {
    return (
      <>
        <h1>{t('mods.title')}</h1>
        <p className="sub">{t('mods.needProfile')}</p>
      </>
    )
  }

  const modsAllowed = loaderInfo(active.loader).mods
  const installedIds = new Set(active.mods.map((m) => m.projectId))

  async function install(projectId: string): Promise<void> {
    setBusy(projectId)
    setMsg('')
    try {
      const mod = await window.visual.mods.install(active!.id, projectId, contentType)
      setMsg(
        contentType === 'mod'
          ? `„${mod.name}" installiert — inkl. benötigter Abhängigkeiten.`
          : `„${mod.name}" installiert.`
      )
      onChanged()
      reloadInstalled()
    } catch (e) {
      setMsg(errorText(e, t))
    } finally {
      setBusy(null)
    }
  }

  async function uninstall(projectId: string): Promise<void> {
    await window.visual.mods.uninstall(active!.id, projectId)
    onChanged()
    reloadInstalled()
  }

  async function removeFile(item: InstalledItem): Promise<void> {
    await window.visual.mods.removeFile(active!.id, item.kind, item.fileName)
    onChanged()
    reloadInstalled()
  }

  async function toggleFile(item: InstalledItem): Promise<void> {
    await window.visual.mods.toggleFile(active!.id, item.kind, item.fileName)
    reloadInstalled()
  }

  async function checkUpdates(): Promise<void> {
    setBusy('updates')
    try {
      setUpdates(await window.visual.mods.updates(active!.id))
    } finally {
      setBusy(null)
    }
  }

  async function importPack(): Promise<void> {
    setBusy('mrpack')
    try {
      const n = await window.visual.mods.importMrpack(active!.id)
      if (n > 0) setMsg(t('mods.mrpackDone', { n }))
    } finally {
      setBusy(null)
      onChanged()
      reloadInstalled()
    }
  }

  const total_installed = installed.length

  return (
    <>
      <h1>{t('mods.title')}</h1>
      <p className="sub">
        {active.name} · {active.mcVersion} · {loaderInfo(active.loader).label}
      </p>

      <div className="row" style={{ marginBottom: 12, flexWrap: 'wrap' }}>
        <button
          className={`btn tabchip ${tab === 'installed' ? 'primary' : ''}`}
          onClick={() => setTab('installed')}
        >
          {'✓ ' + t('mods.installedTab')} ({total_installed})
        </button>
        {TYPES.map((t) => (
          <button
            key={t.id}
            className={`btn tabchip ${contentType === t.id && tab === 'browse' ? 'primary' : ''}`}
            onClick={() => {
              setContentType(t.id)
              setTab('browse')
            }}
          >
            + {t.label}
          </button>
        ))}
        <div className="grow" />
        <select value={active.id} onChange={(e) => onSelect(e.target.value)}>
          {profiles.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name} ({p.mcVersion} · {loaderInfo(p.loader).label})
            </option>
          ))}
        </select>
        <button className="btn" onClick={importPack} disabled={busy !== null}>
          .mrpack
        </button>
      </div>

      {tab === 'installed' && (
        <>
          <div className="row" style={{ marginBottom: 14 }}>
            <button className="btn" onClick={checkUpdates} disabled={busy !== null}>
              {busy === 'updates' ? t('mods.checking') : t('mods.checkUpdates')}
            </button>
            {updates && (
              <span className="muted">
                {updates.length === 0 ? t('mods.allCurrent') + ' ✓' : t('mods.updatesAvailable', { n: updates.length })}
              </span>
            )}
            <div className="grow" />
            <button className="btn ghost" onClick={reloadInstalled}>
              {'⟳ ' + t('common.reset')}
            </button>
          </div>
          {msg && <div className="ok" style={{ marginBottom: 10 }}>{msg}</div>}

          {total_installed === 0 && (
            <div className="muted">
              {t('mods.nothingInstalled')}
            </div>
          )}

          {KIND_GROUP.map((g) => {
            const group = installed.filter((i) => i.kind === g.kind)
            if (group.length === 0) return null
            return (
              <div key={g.kind} style={{ marginBottom: 18 }}>
                <div className="grouphead">
                  {g.icon} {g.label} <span className="muted">({group.length})</span>
                </div>
                <div className="list">
                  {group.map((item) => {
                    const upd = item.projectId
                      ? updates?.find((u) => u.projectId === item.projectId)
                      : undefined
                    return (
                      <div
                        key={item.fileName}
                        className={'card row modrow' + (item.enabled ? '' : ' aus')}
                      >
                        {item.iconUrl ? (
                          <img className="mod-icon" src={item.iconUrl} alt="" />
                        ) : (
                          <div className="mod-icon" style={{ display: 'grid', placeItems: 'center' }}>
                            {g.icon}
                          </div>
                        )}
                        <div className="grow">
                          <div className="row" style={{ gap: 8 }}>
                            <span className="mod-title">{item.name}</span>
                            <span className="badge gray">{KIND_LABEL[item.kind]}</span>
                            {!item.enabled && <span className="badge gray">{t('mods.disabled')}</span>}
                          </div>
                          <div className="muted">{item.fileName}</div>
                        </div>
                        {upd && item.projectId && (
                          <button
                            className="btn primary"
                            onClick={() => {
                              setContentType(item.kind)
                              install(item.projectId!)
                            }}
                            disabled={busy !== null}
                          >
                            Update
                          </button>
                        )}
                        <button
                          className="switchbtn"
                          title={item.enabled ? t('common.disable') : t('common.enable')}
                          aria-pressed={item.enabled}
                          onClick={() => toggleFile(item)}
                        >
                          <span className={'switch' + (item.enabled ? ' on' : '')} />
                        </button>
                        <button
                          className="btn ghost iconbtn"
                          title={t('common.remove')}
                          onClick={() =>
                            item.projectId ? uninstall(item.projectId) : removeFile(item)
                          }
                        >
                          ✕
                        </button>
                      </div>
                    )
                  })}
                </div>
              </div>
            )
          })}
        </>
      )}

      {tab === 'browse' && (
        <>
          {contentType === 'mod' && !modsAllowed && (
            <div className="muted" style={{ marginBottom: 14 }}>
              {t('mods.vanillaHint')}
            </div>
          )}
          <div className="row" style={{ marginBottom: 10 }}>
            <input
              className="grow"
              placeholder={`${KIND_LABEL[contentType]}s für ${active.mcVersion} suchen …`}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && search(query)}
            />
            <select value={sort} onChange={(e) => setSort(e.target.value as typeof sort)}>
              <option value="downloads">{t('mods.sort.downloads')}</option>
              <option value="relevance">{t('mods.sort.relevance')}</option>
              <option value="newest">{t('mods.sort.newest')}</option>
              <option value="follows">{t('mods.sort.follows')}</option>
            </select>
            <button className="btn primary" onClick={() => search(query)} disabled={busy === 'search'}>
              {busy === 'search' ? 'Sucht …' : 'Suchen'}
            </button>
          </div>
          {contentType === 'mod' && (
            <div className="row" style={{ marginBottom: 16, flexWrap: 'wrap', gap: 6 }}>
              {MOD_CATEGORIES.map(([id, label]) => (
                <button
                  key={id}
                  className={`btn ${category === id ? 'primary' : ''}`}
                  style={{ padding: '4px 10px', fontSize: 12 }}
                  onClick={() => setCategory(id)}
                >
                  {label}
                </button>
              ))}
            </div>
          )}
          {msg && <div className="muted" style={{ marginBottom: 10 }}>{msg}</div>}
          <div className="list">
            {hits.map((hit) => (
              <div key={hit.project_id} className="card hover row">
                {hit.icon_url ? (
                  <img className="mod-icon" src={hit.icon_url} alt="" />
                ) : (
                  <div className="mod-icon" />
                )}
                <div className="grow">
                  <div className="row" style={{ gap: 8 }}>
                    <span className="mod-title">{hit.title}</span>
                    <span className="badge gray">{formatDownloads(hit.downloads)} Downloads</span>
                  </div>
                  <div className="mod-desc">{hit.description}</div>
                </div>
                {installedIds.has(hit.project_id) ? (
                  <button className="btn danger" onClick={() => uninstall(hit.project_id)}>
                    Entfernen
                  </button>
                ) : (
                  <button
                    className="btn primary"
                    onClick={() => install(hit.project_id)}
                    disabled={busy !== null || (contentType === 'mod' && !modsAllowed)}
                  >
                    {busy === hit.project_id ? t('common.loading') : t('mods.install')}
                  </button>
                )}
              </div>
            ))}
          </div>
          {total > 20 && (
            <div className="row" style={{ marginTop: 14, justifyContent: 'center' }}>
              <button className="btn" disabled={offset === 0} onClick={() => search(query, offset - 20)}>
                ← Zurück
              </button>
              <span className="muted">
                {offset + 1}–{Math.min(offset + 20, total)} von {total}
              </span>
              <button
                className="btn"
                disabled={offset + 20 >= total}
                onClick={() => search(query, offset + 20)}
              >
                Weiter →
              </button>
            </div>
          )}
        </>
      )}
    </>
  )
}

function formatDownloads(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)} M`
  if (n >= 1_000) return `${Math.round(n / 1_000)} k`
  return String(n)
}
