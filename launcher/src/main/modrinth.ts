import fs from 'fs'
import path from 'path'
import { pipeline } from 'stream/promises'
import { Readable, Transform } from 'stream'
import AdmZip from 'adm-zip'
import { instanceDir } from './paths'
import { getProfile, updateMods, InstalledMod } from './profiles'
import { VError } from './errors'

const API = 'https://api.modrinth.com/v2'
const UA = 'visual-client/1.0.0 (magel82@gmx.de)'

/**
 * Wählt die spielbare Datei einer Version. Wichtig: `-sources`/`-javadoc`/`-dev`
 * niemals installieren — deren fabric.mod.json enthält nur eine Vorlage
 * (`${mod_id}`), an der Fabric die gesamte Mod-Suche abbricht und das Spiel gar
 * nicht mehr startet.
 */
function pickFile<T extends { filename: string; primary: boolean }>(files: T[]): T {
  const usable = files.filter((f) => !/-(sources|javadoc|dev)\.jar$/i.test(f.filename))
  const pool = usable.length ? usable : files
  return pool.find((f) => f.primary) ?? pool[0]
}

async function api<T>(url: string): Promise<T> {
  const res = await fetch(url, { headers: { 'User-Agent': UA } })
  if (!res.ok) throw new VError('error.modrinth', { status: res.status })
  return res.json() as Promise<T>
}

export interface SearchHit {
  project_id: string
  title: string
  description: string
  icon_url: string | null
  downloads: number
  author: string
  categories: string[]
}

export type ProjectType = 'mod' | 'resourcepack' | 'shader'

export interface SearchOpts {
  query: string
  mcVersion: string
  loader: string
  projectType?: ProjectType
  offset?: number
  sort?: 'relevance' | 'downloads' | 'newest' | 'follows'
  category?: string
}

export async function searchMods(opts: SearchOpts): Promise<{ hits: SearchHit[]; total_hits: number }> {
  const type = opts.projectType ?? 'mod'
  const facetList = [[`versions:${opts.mcVersion}`], [`project_type:${type}`]]
  // Loader-Filter nur für Mods — Resourcepacks/Shader sind loader-unabhängig
  if (type === 'mod') facetList.unshift([`categories:${opts.loader}`])
  if (opts.category) facetList.push([`categories:${opts.category}`])
  const index = opts.sort ?? (opts.query ? 'relevance' : 'downloads')
  const url = `${API}/search?query=${encodeURIComponent(opts.query)}&facets=${encodeURIComponent(
    JSON.stringify(facetList)
  )}&limit=20&offset=${opts.offset ?? 0}&index=${index}`
  return api(url)
}

interface ModrinthVersion {
  id: string
  name: string
  version_number: string
  date_published: string
  files: { url: string; filename: string; primary: boolean }[]

  dependencies?: { project_id: string | null; dependency_type: string }[]
}

async function latestVersion(
  projectId: string,
  mcVersion: string,
  loader: string | null
): Promise<ModrinthVersion | null> {
  const loaderQuery = loader ? `loaders=${encodeURIComponent(JSON.stringify([loader]))}&` : ''
  const versions = await api<ModrinthVersion[]>(
    `${API}/project/${projectId}/version?${loaderQuery}` +
      `game_versions=${encodeURIComponent(JSON.stringify([mcVersion]))}`
  )
  return versions[0] ?? null
}

const TYPE_FOLDERS: Record<ProjectType, string> = {
  mod: 'mods',
  resourcepack: 'resourcepacks',
  shader: 'shaderpacks'
}

/** Bricht ab, wenn so lange kein einziges Byte mehr ankommt. */
const STILLSTAND_MS = 60_000

/**
 * Datei laden, optional mit Fortschritt.
 *
 * Ohne `onProgress` meldet ein großer Download nichts — beim Java-Paket
 * (rund 180 MB) stand die Oberfläche minutenlang still und wirkte
 * abgestürzt. Zusätzlich wacht ein Stillstands-Wächter: reißt die
 * Verbindung ab, hing der Vorgang vorher unbegrenzt, weil fetch von sich
 * aus keine Zeitgrenze kennt.
 */
export async function downloadFile(
  url: string,
  dest: string,
  onProgress?: (geladen: number, gesamt: number) => void
): Promise<void> {
  const abbruch = new AbortController()
  let wecker: NodeJS.Timeout | undefined
  const anstossen = (): void => {
    if (wecker) clearTimeout(wecker)
    wecker = setTimeout(() => abbruch.abort(), STILLSTAND_MS)
  }

  try {
    anstossen()
    const res = await fetch(url, { headers: { 'User-Agent': UA }, signal: abbruch.signal })
    if (!res.ok || !res.body) throw new VError('error.download', { status: res.status })

    const gesamt = Number(res.headers.get('content-length') ?? 0)
    let geladen = 0
    const zaehlen = new Transform({
      transform(stueck, _kodierung, weiter) {
        geladen += stueck.length
        anstossen()
        onProgress?.(geladen, gesamt)
        weiter(null, stueck)
      }
    })
    await pipeline(Readable.fromWeb(res.body as never), zaehlen, fs.createWriteStream(dest))
  } catch (err) {
    // Angefangene Datei nicht liegen lassen — sonst gilt sie beim nächsten
    // Versuch als fertig vorhanden.
    try {
      fs.rmSync(dest, { force: true })
    } catch {
      /* egal */
    }
    if (abbruch.signal.aborted) throw new VError('error.downloadStalled')
    throw err
  } finally {
    if (wecker) clearTimeout(wecker)
  }
}

export async function installMod(
  profileId: string,
  projectId: string,
  kind: ProjectType = 'mod',
  depth = 0
): Promise<InstalledMod> {
  const profile = getProfile(profileId)
  if (!profile) throw new VError('error.profileMissing')
  const loader = profile.loader ?? 'forge'
  // Resourcepacks/Shader sind loader-unabhängig
  const version = await latestVersion(projectId, profile.mcVersion, kind === 'mod' ? loader : null)
  if (!version) {
    throw new VError(
      kind === 'mod' ? 'error.noLoaderVersion' : 'error.noVersion',
      { loader, version: profile.mcVersion }
    )
  }
  const file = pickFile(version.files)
  const project = await api<{ title: string; icon_url: string | null }>(`${API}/project/${projectId}`)

  const destDir = path.join(instanceDir(profileId), TYPE_FOLDERS[kind])
  fs.mkdirSync(destDir, { recursive: true })
  await downloadFile(file.url, path.join(destDir, file.filename))

  const installed: InstalledMod = {
    projectId,
    versionId: version.id,
    fileName: file.filename,
    name: project.title,
    iconUrl: project.icon_url ?? undefined,
    kind
  }
  const mods = profile.mods.filter((m) => m.projectId !== projectId)
  mods.push(installed)
  updateMods(profileId, mods)

  // Benötigte Abhängigkeiten automatisch mitinstallieren (max. 3 Ebenen, nur Mods)
  if (kind === 'mod' && depth < 3) {
    for (const dep of version.dependencies ?? []) {
      if (dep.dependency_type !== 'required' || !dep.project_id) continue
      const fresh = getProfile(profileId)
      if (fresh?.mods.some((m) => m.projectId === dep.project_id)) continue
      try {
        await installMod(profileId, dep.project_id, 'mod', depth + 1)
      } catch {
        // Abhängigkeit hat keine passende Version — Mod meldet es dann selbst
      }
    }
  }
  return installed
}

export function uninstallMod(profileId: string, projectId: string): void {
  const profile = getProfile(profileId)
  if (!profile) return
  const mod = profile.mods.find((m) => m.projectId === projectId)
  if (mod) {
    const folder = TYPE_FOLDERS[(mod.kind as ProjectType) ?? 'mod'] ?? 'mods'
    const file = path.join(instanceDir(profileId), folder, mod.fileName)
    if (fs.existsSync(file)) fs.unlinkSync(file)
  }
  updateMods(profileId, profile.mods.filter((m) => m.projectId !== projectId))
}

export interface UpdateInfo {
  projectId: string
  name: string
  currentVersionId: string
  latestVersionId: string
  latestName: string
}

export async function checkUpdates(profileId: string): Promise<UpdateInfo[]> {
  const profile = getProfile(profileId)
  if (!profile) return []
  const updates: UpdateInfo[] = []
  for (const mod of profile.mods) {
    try {
      const loader = (mod.kind ?? 'mod') === 'mod' ? profile.loader ?? 'forge' : null
      const latest = await latestVersion(mod.projectId, profile.mcVersion, loader)
      if (latest && latest.id !== mod.versionId) {
        updates.push({
          projectId: mod.projectId,
          name: mod.name,
          currentVersionId: mod.versionId,
          latestVersionId: latest.id,
          latestName: latest.version_number
        })
      }
    } catch {
      // Mod evtl. entfernt — ignorieren
    }
  }
  return updates
}

export interface InstalledItem {
  name: string
  fileName: string
  kind: ProjectType
  projectId?: string
  iconUrl?: string
  enabled: boolean
}

function prettyName(file: string): string {
  return file
    .replace(/\.(jar|zip|disabled)$/gi, '')
    .replace(/[-_]/g, ' ')
    .replace(/\s*\d[\d.+]*.*$/, '') // Versionsnummern hinten abschneiden
    .trim()
    .replace(/\b\w/g, (c) => c.toUpperCase()) || file
}

/**
 * Scannt die echten Profil-Ordner (mods/resourcepacks/shaderpacks) und listet
 * ALLE Inhalte — auch automatische Abhängigkeiten, den Branding-Mod und
 * manuell abgelegte Dateien. Namen/Icons kommen aus der Tracking-Liste,
 * wo vorhanden; sonst aus dem Dateinamen.
 */
export function listInstalledContent(profileId: string): InstalledItem[] {
  const profile = getProfile(profileId)
  const dir = instanceDir(profileId)
  const tracked = new Map<string, InstalledMod>()
  for (const m of profile?.mods ?? []) tracked.set(m.fileName, m)

  const items: InstalledItem[] = []
  const scan = (folder: string, kind: ProjectType, exts: string[]): void => {
    const p = path.join(dir, folder)
    if (!fs.existsSync(p)) return
    for (const f of fs.readdirSync(p)) {
      let isDir = false
      try {
        isDir = fs.statSync(path.join(p, f)).isDirectory()
      } catch {
        continue
      }
      const lower = f.toLowerCase()
      if (!isDir && !exts.some((e) => lower.endsWith(e) || lower.endsWith(e + '.disabled'))) continue
      const base = f.replace(/\.disabled$/i, '')
      const t = tracked.get(base) ?? tracked.get(f)
      items.push({
        name: t?.name ?? prettyName(base),
        fileName: f,
        kind,
        projectId: t?.projectId,
        iconUrl: t?.iconUrl,
        enabled: !lower.endsWith('.disabled')
      })
    }
  }
  scan('mods', 'mod', ['.jar'])
  scan('resourcepacks', 'resourcepack', ['.zip'])
  scan('shaderpacks', 'shader', ['.zip'])
  return items.sort((a, b) => a.name.localeCompare(b.name))
}

const TYPE_FOLDER_NAMES: Record<ProjectType, string> = {
  mod: 'mods',
  resourcepack: 'resourcepacks',
  shader: 'shaderpacks'
}

/** Beliebige Inhalts-Datei entfernen (auch untracked). */
export function removeContentFile(profileId: string, kind: ProjectType, fileName: string): void {
  const dir = instanceDir(profileId)
  const target = path.join(dir, TYPE_FOLDER_NAMES[kind], fileName)
  if (fs.existsSync(target)) {
    fs.rmSync(target, { recursive: true, force: true })
  }
  // aus der Tracking-Liste nehmen, falls vorhanden
  const profile = getProfile(profileId)
  if (profile) {
    const base = fileName.replace(/\.disabled$/i, '')
    updateMods(
      profileId,
      profile.mods.filter((m) => m.fileName !== fileName && m.fileName !== base)
    )
  }
}

/** Inhalt an/aus schalten (.jar <-> .jar.disabled). */
export function toggleContentFile(
  profileId: string,
  kind: ProjectType,
  fileName: string
): boolean {
  const dir = instanceDir(profileId)
  const folder = path.join(dir, TYPE_FOLDER_NAMES[kind])
  const src = path.join(folder, fileName)
  if (!fs.existsSync(src)) return false
  const disabled = fileName.toLowerCase().endsWith('.disabled')
  const dest = disabled
    ? path.join(folder, fileName.replace(/\.disabled$/i, ''))
    : path.join(folder, fileName + '.disabled')
  fs.renameSync(src, dest)
  return !disabled // neuer enabled-Status
}

/** .mrpack-Modpack in ein Profil importieren (Modrinth-Format). */
export async function importMrpack(profileId: string, mrpackPath: string): Promise<number> {
  const profile = getProfile(profileId)
  if (!profile) throw new VError('error.profileMissing')
  const zip = new AdmZip(mrpackPath)
  const indexEntry = zip.getEntry('modrinth.index.json')
  if (!indexEntry) throw new VError('error.mrpackIndex')
  const index = JSON.parse(indexEntry.getData().toString('utf8'))
  const dir = instanceDir(profileId)
  let count = 0
  for (const file of index.files ?? []) {
    const dest = path.join(dir, file.path)
    if (!path.resolve(dest).startsWith(path.resolve(dir))) continue // Zip-Slip-Schutz
    fs.mkdirSync(path.dirname(dest), { recursive: true })
    await downloadFile(file.downloads[0], dest)
    count++
  }
  // overrides/ direkt entpacken
  for (const entry of zip.getEntries()) {
    if (entry.entryName.startsWith('overrides/') && !entry.isDirectory) {
      const rel = entry.entryName.slice('overrides/'.length)
      const dest = path.join(dir, rel)
      if (!path.resolve(dest).startsWith(path.resolve(dir))) continue
      fs.mkdirSync(path.dirname(dest), { recursive: true })
      fs.writeFileSync(dest, entry.getData())
    }
  }
  return count
}
