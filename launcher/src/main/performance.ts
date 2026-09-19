import fs from 'fs'
import path from 'path'
import AdmZip from 'adm-zip'
import { instanceDir } from './paths'
import { getProfile, saveProfile } from './profiles'
import { installMod, listInstalledContent } from './modrinth'
import { VError } from './errors'

/**
 * Liest aus allen installierten Jars, welche Mod-IDs dort per `breaks` bzw.
 * `conflicts` ausgeschlossen sind. Fabric verweigert den Start, wenn eine
 * ausgeschlossene Mod trotzdem im Ordner liegt.
 */
function blockedModIds(modsDir: string): Set<string> {
  const blocked = new Set<string>()
  if (!fs.existsSync(modsDir)) return blocked
  for (const file of fs.readdirSync(modsDir)) {
    if (!file.toLowerCase().endsWith('.jar')) continue
    try {
      const entry = new AdmZip(path.join(modsDir, file)).getEntry('fabric.mod.json')
      if (!entry) continue
      const meta = JSON.parse(entry.getData().toString('utf8')) as {
        breaks?: Record<string, unknown>
        conflicts?: Record<string, unknown>
      }
      for (const id of [...Object.keys(meta.breaks ?? {}), ...Object.keys(meta.conflicts ?? {})]) {
        blocked.add(id.toLowerCase().replace(/[-_]/g, ''))
      }
    } catch {
      /* unlesbare Jar überspringen */
    }
  }
  return blocked
}

/**
 * Optimierte JVM-Flags (G1GC-Tuning à la Aikar) — verbessert vor allem die
 * Frametime-Stabilität (weniger Ruckler/GC-Spikes).
 */
const JVM_FLAGS = [
  '-XX:+UseG1GC',
  '-XX:+ParallelRefProcEnabled',
  '-XX:MaxGCPauseMillis=200',
  '-XX:+UnlockExperimentalVMOptions',
  '-XX:+DisableExplicitGC',
  '-XX:+AlwaysPreTouch',
  '-XX:G1NewSizePercent=30',
  '-XX:G1MaxNewSizePercent=40',
  '-XX:G1HeapRegionSize=8M',
  '-XX:G1ReservePercent=20',
  '-XX:G1HeapWastePercent=5',
  '-XX:G1MixedGCCountTarget=4',
  '-XX:InitiatingHeapOccupancyPercent=15',
  '-XX:G1MixedGCLiveThresholdPercent=90',
  '-XX:G1RSetUpdatingPauseTimePercent=5',
  '-XX:SurvivorRatio=32',
  '-XX:+PerfDisableSharedMem',
  '-XX:MaxTenuringThreshold=1'
].join(' ')

/** Maximal-FPS options.txt-Werte (aggressiv — der größte Roh-FPS-Hebel). */
const PERF_OPTIONS: Record<string, string> = {
  renderDistance: '5',
  simulationDistance: '5',
  graphicsMode: '0', // Fast
  ao: '0', // Smooth Lighting aus
  particles: '2', // minimal
  entityShadows: 'false',
  entityDistanceScaling: '0.5',
  maxFps: '260', // Sodium: unbegrenzt
  enableVsync: 'false',
  renderClouds: '"false"',
  biomeBlendRadius: '0',
  mipmapLevels: '0',
  bobView: 'false',
  screenEffectScale: '0.0',
  fovEffectScale: '0.0',
  darknessEffectScale: '0.0',
  glintSpeed: '0.0',
  glintStrength: '0.0',
  prioritizeChunkUpdates: '2',
  smoothLighting: 'false'
}

/**
 * options.txt für 1.8.9. Die Schlüssel oben stammen aus modernen Versionen
 * und existieren hier gar nicht — 1.8.9 kennt weder `graphicsMode` noch
 * `simulationDistance`. Die Namen sind aus der gemappten GameOptions-Klasse
 * der Version selbst entnommen.
 *
 * Zum FPS-Deckel: `maxFps` ist bei 260 exakt der Maximalwert der Option.
 * MinecraftClient.isFramerateValid() vergleicht genau dagegen und ruft
 * Display.sync() nur unterhalb davon auf — bei 260 begrenzt das Spiel also
 * überhaupt nicht mehr. Zusammen mit abgeschaltetem VSync (sonst Deckel auf
 * die Bildwiederholrate des Monitors) ist die Bildrate damit offen.
 *
 * `useVbo` ist in 1.8.9 der grösste Einzelhebel: Vertex-Buffer statt der
 * alten Display-Listen.
 */
const PERF_OPTIONS_189: Record<string, string> = {
  maxFps: '260',
  enableVsync: 'false',
  useVbo: 'true',
  renderDistance: '4',
  fancyGraphics: 'false',
  ao: '0',
  particles: '2',
  entityShadows: 'false',
  renderClouds: 'false',
  mipmapLevels: '0',
  bobView: 'false',
  anaglyph3d: 'false',
  fboEnable: 'true'
}

/** 1.8.9 und älter brauchen eigene Schlüssel und haben keine Sodium-Welt. */
function isLegacy(mcVersion?: string): boolean {
  const m = mcVersion?.match(/^1\.(\d+)/)
  return !!m && Number(m[1]) < 14
}

/** Performance-Mods je Loader (Slug -> Erkennungs-Stichwort). */
const PERF_MODS: Record<string, { slug: string; keyword: string }[]> = {
  fabric: [
    { slug: 'sodium', keyword: 'sodium' },
    { slug: 'sodium-extra', keyword: 'sodiumextra' },
    { slug: 'reeses-sodium-options', keyword: 'reeses' },
    { slug: 'lithium', keyword: 'lithium' },
    { slug: 'ferrite-core', keyword: 'ferritecore' },
    { slug: 'immediatelyfast', keyword: 'immediatelyfast' },
    { slug: 'moreculling', keyword: 'moreculling' },
    { slug: 'entityculling', keyword: 'entityculling' },
    { slug: 'dynamic-fps', keyword: 'dynamicfps' },
    { slug: 'modernfix', keyword: 'modernfix' },
    { slug: 'badoptimizations', keyword: 'badoptimizations' },
    { slug: 'noisium', keyword: 'noisium' },
    { slug: 'ksyxis', keyword: 'ksyxis' }
  ],
  forge: [
    { slug: 'embeddium', keyword: 'embeddium' },
    { slug: 'ferrite-core', keyword: 'ferritecore' },
    { slug: 'entityculling', keyword: 'entityculling' },
    { slug: 'moreculling', keyword: 'moreculling' }
  ]
}

/**
 * Sodium verweigert den Start bei zu altem Intel-Grafiktreiber und bricht
 * das ganze Spiel ab („Unsupported Driver“). Da der FPS-Boost Sodium von
 * sich aus installiert, machte er ältere Rechner damit unstartbar.
 *
 * Geprüft wird deshalb vorher: Steckt eine andere Grafikkarte im Rechner
 * (AMD, NVIDIA), ist alles gut. Gibt es nur Intel, muss der Treiber
 * mindestens 10.18.10.5161 sein — genau die Grenze, die Sodium selbst zieht.
 */
const SODIUM_MIN_INTEL = [10, 18, 10, 5161]

function versionKleiner(ist: string, mindestens: number[]): boolean {
  const teile = ist.split('.').map((n) => Number(n) || 0)
  for (let i = 0; i < mindestens.length; i++) {
    const a = teile[i] ?? 0
    if (a !== mindestens[i]) return a < mindestens[i]
  }
  return false
}

function sodiumLaeuft(): boolean {
  if (process.platform !== 'win32') return true
  try {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const { execFileSync } = require('child_process') as typeof import('child_process')
    const roh = execFileSync(
      'powershell',
      [
        '-NoProfile',
        '-Command',
        "Get-CimInstance Win32_VideoController | ForEach-Object { $_.AdapterCompatibility + '|' + $_.DriverVersion }"
      ],
      { encoding: 'utf8', timeout: 10_000, windowsHide: true }
    )
    const karten = roh
      .split(/\r?\n/)
      .map((z) => z.trim())
      .filter(Boolean)
      .map((z) => {
        const [hersteller, version] = z.split('|')
        return { intel: /intel/i.test(hersteller ?? ''), version: version ?? '' }
      })
    if (!karten.length) return true
    // Eine dedizierte Karte übernimmt das Rendern — dann ist Sodium unkritisch
    if (karten.some((k) => !k.intel)) return true
    return !karten.every((k) => versionKleiner(k.version, SODIUM_MIN_INTEL))
  } catch {
    // Abfrage nicht möglich: lieber installieren als grundlos verzichten
    return true
  }
}

/** Sehr schwere Mods, die viel FPS kosten — werden deaktiviert (nicht gelöscht). */
const HEAVY_MODS = ['distanthorizons', 'distant_horizons', 'iris', 'oculus', 'shader']

function writeOptions(dir: string, options: Record<string, string>): number {
  const file = path.join(dir, 'options.txt')
  const lines: string[] = fs.existsSync(file)
    ? fs.readFileSync(file, 'utf8').split(/\r?\n/)
    : []
  let changed = 0
  for (const [key, val] of Object.entries(options)) {
    const idx = lines.findIndex((l) => l.startsWith(key + ':'))
    const line = `${key}:${val}`
    if (idx >= 0) lines[idx] = line
    else lines.push(line)
    changed++
  }
  fs.writeFileSync(file, lines.filter((l) => l !== '').join('\n') + '\n')
  return changed
}

export interface PerfResult {
  jvm: boolean
  options: number
  modsInstalled: string[]
  heavyDisabled: string[]
}

/**
 * Wendet das Performance-Preset auf ein Profil an: JVM-Flags, options.txt,
 * fehlende Performance-Mods nachinstallieren (duplikatsicher) und die
 * größten FPS-Fresser deaktivieren. Alles reversibel.
 */
export async function applyPerformance(profileId: string): Promise<PerfResult> {
  const profile = getProfile(profileId)
  if (!profile) throw new VError('error.profileMissing')
  const dir = instanceDir(profileId)
  const loader = profile.loader ?? 'forge'
  const legacy = isLegacy(profile.mcVersion)

  // 1. JVM-Flags setzen (überschreibt nur, wenn nicht ohnehin schon gesetzt)
  saveProfile({ ...profile, javaArgs: JVM_FLAGS })

  // 2. options.txt optimieren
  const optCount = writeOptions(dir, legacy ? PERF_OPTIONS_189 : PERF_OPTIONS)

  // 3. Fehlende Performance-Mods nachinstallieren — nur, wenn nicht schon da
  const modsInstalled: string[] = []
  const heavyDisabled: string[] = []
  // Für 1.8.9 gibt es auf Modrinth praktisch keine Performance-Mods (Sodium
  // und Co. setzen modernes Fabric voraus). Die Suche liefe nur in Fehler,
  // deshalb bleibt es dort bei options.txt und den JVM-Flags.
  if (!legacy && (loader === 'fabric' || loader === 'quilt' || loader === 'forge' || loader === 'neoforge')) {
    const installed = listInstalledContent(profileId)
    const haveKeyword = (kw: string): boolean =>
      installed.some((i) => i.fileName.toLowerCase().replace(/[-_]/g, '').includes(kw.replace(/[-_]/g, '')))

    // Mod-IDs, die bereits installierte Mods ausdrücklich ausschließen. Ohne
    // diese Prüfung installiert der FPS-Boost z. B. ImmediatelyFast, obwohl
    // eine vorhandene Mod es per "breaks" blockiert — Fabric verweigert dann
    // den Start.
    const blocked = blockedModIds(path.join(dir, 'mods'))

    const set = PERF_MODS[loader === 'forge' || loader === 'neoforge' ? 'forge' : 'fabric']
    // Einmal abfragen, nicht je Mod — die WMI-Abfrage kostet Zeit.
    const sodiumOk = sodiumLaeuft()
    for (const { slug, keyword } of set) {
      if (haveKeyword(keyword)) continue
      if (blocked.has(keyword) || blocked.has(slug.replace(/-/g, ''))) continue
      // Auf zu altem Intel-Treiber macht Sodium das Spiel unstartbar
      if (!sodiumOk && /sodium|reeses/.test(keyword)) continue
      try {
        const mod = await installMod(profileId, slug, 'mod')
        modsInstalled.push(mod.name)
      } catch {
        /* Version passt nicht -> überspringen */
      }
    }

    // 4. Schwere FPS-Fresser deaktivieren (Datei -> .disabled)
    const modsDir = path.join(dir, 'mods')
    if (fs.existsSync(modsDir)) {
      for (const f of fs.readdirSync(modsDir)) {
        const low = f.toLowerCase()
        if (!low.endsWith('.jar')) continue
        if (HEAVY_MODS.some((h) => low.replace(/[-_]/g, '').includes(h.replace(/[-_]/g, '')))) {
          try {
            fs.renameSync(path.join(modsDir, f), path.join(modsDir, f + '.disabled'))
            heavyDisabled.push(f)
          } catch {
            /* egal */
          }
        }
      }
    }
  }

  return { jvm: true, options: optCount, modsInstalled, heavyDisabled }
}
