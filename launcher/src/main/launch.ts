import fs from 'fs'
import path from 'path'
import { BrowserWindow } from 'electron'
import { Client } from 'minecraft-launcher-core'
import { DATA_DIR, MC_ROOT, instanceDir } from './paths'
import { getProfile, touchLastPlayed } from './profiles'
import { mclcAuth } from './auth'
import { downloadFile } from './modrinth'
import { ensureFabric, ensureQuilt } from './fabric'
import { ensureNeoForge, neoforgeJvmArgs } from './neoforge'
import { installVisuals, installBranding } from './visuals'
import { VError } from './errors'
import { entferneKaputteJars } from './repair'

const FORGE_PROMOS = 'https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json'
const VERSION_MANIFEST = 'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json'

// Anzahl gleichzeitig laufender Spiel-Instanzen
let runningCount = 0
let onInstanceChange: ((count: number, last?: { version: string; profile: string }) => void) | null =
  null

export function runningInstances(): number {
  return runningCount
}

export function setInstanceListener(
  cb: (count: number, last?: { version: string; profile: string }) => void
): void {
  onInstanceChange = cb
}

/**
 * Fallback-Heuristik, falls kein Versions-JSON verfügbar (offline, erste
 * Installation). Das alte Schema (1.x.y) MUSS zuerst geprüft werden, sonst
 * matcht "1.21.9" die Kurzform-Regex mit major=1.
 */
/**
 * Fortschrittsmeldungen wandern als SCHLÜSSEL zur Oberfläche, nicht als
 * fertiger Text — nur dort ist die gewählte Sprache bekannt. Vorher
 * standen hier deutsche Sätze, die auch auf Englisch und Russisch
 * deutsch blieben.
 */
export type StatusFn = (key: string, vars?: Record<string, string | number>) => void

export function requiredJavaMajor(mcVersion: string): number {
  const legacy = mcVersion.match(/^1\.(\d+)(?:\.(\d+))?/)
  if (legacy) {
    const minor = Number(legacy[1])
    const patch = Number(legacy[2] ?? 0)
    if (minor > 20 || (minor === 20 && patch >= 5)) return 21
    if (minor >= 17) return 17
    return 8
  }
  // Neues Versionsschema (26.2, …)
  return 21
}

/** javaVersion.majorVersion aus einem Mojang-Versions-JSON ziehen. */
function javaMajorFromJson(jsonPath: string): number | null {
  try {
    const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8')) as {
      javaVersion?: { majorVersion?: number }
    }
    return data.javaVersion?.majorVersion ?? null
  } catch {
    return null
  }
}

/**
 * Benötigte Java-Version automatisch ermitteln — Quelle in dieser Reihenfolge:
 * 1. Lokales Mojang-Versions-JSON (liegt nach dem ersten Start im versions-Ordner,
 *    auch innerhalb von Fabric-/Quilt-Custom-Ordnern als <mcVersion>.json)
 * 2. Mojang-Versionsmanifest online (JSON der Version nachladen)
 * 3. Heuristik (offline-Fallback)
 */
export async function requiredJavaMajorAuto(mcVersion: string): Promise<number> {
  // 1. lokal suchen
  const direct = path.join(MC_ROOT, 'versions', mcVersion, `${mcVersion}.json`)
  const local = javaMajorFromJson(direct)
  if (local) return local
  const versionsDir = path.join(MC_ROOT, 'versions')
  if (fs.existsSync(versionsDir)) {
    for (const dir of fs.readdirSync(versionsDir)) {
      const nested = path.join(versionsDir, dir, `${mcVersion}.json`)
      if (fs.existsSync(nested)) {
        const major = javaMajorFromJson(nested)
        if (major) return major
      }
    }
  }
  // 2. online nachschlagen
  try {
    const manifest = (await (await fetch(VERSION_MANIFEST)).json()) as {
      versions: { id: string; url: string }[]
    }
    const entry = manifest.versions.find((v) => v.id === mcVersion)
    if (entry) {
      const versionJson = (await (await fetch(entry.url)).json()) as {
        javaVersion?: { majorVersion?: number }
      }
      if (versionJson.javaVersion?.majorVersion) return versionJson.javaVersion.majorVersion
    }
  } catch {
    /* offline — Heuristik */
  }
  // 3. Heuristik
  return requiredJavaMajor(mcVersion)
}

/**
 * Stellt sicher, dass ein Java der gewünschten Hauptversion vorhanden ist —
 * lädt es sonst automatisch von Adoptium (Temurin) in den Launcher-Ordner.
 */
export async function ensureJava(
  major: number,
  onStatus: StatusFn
): Promise<string> {
  const existing = findJava(undefined, major)
  if (existing !== 'java') {
    const gefunden = Number(existing.match(/(?:jdk|jre)-?(\d+)/i)?.[1] ?? NaN)
    /*
     * Ein NEUERES Java genügt für modernes Minecraft — 1.17 bis 1.20.4
     * verlangen zwar 17, laufen aber ebenso unter 21. Nur die alten Ausgaben
     * sind wählerisch: 1.16 und älter brauchen wirklich Java 8, dort bringt
     * eine neuere Fassung das Spiel zum Absturz.
     *
     * Vorher wurde hier auf exakte Gleichheit geprüft. findJava liefert
     * absichtlich auch ein höheres Java zurück — das wurde verworfen und
     * stattdessen ein 180-MB-Paket geladen, obwohl ein passendes Java längst
     * installiert war. Genau das sah aus wie „Java lädt und nichts passiert“.
     */
    const passt = major >= 17 ? gefunden >= major : gefunden === major
    if (passt) return existing
  }
  onStatus('launch.javaDownload', { major, pct: 0 })
  const javaDir = path.join(DATA_DIR, 'java')
  fs.mkdirSync(javaDir, { recursive: true })

  // Betriebssystem und Architektur bestimmen — vorher stand hier fest
  // "windows/x64", auf einem Mac wäre also ein Windows-JDK gelandet.
  const os =
    process.platform === 'darwin' ? 'mac' : process.platform === 'linux' ? 'linux' : 'windows'
  const arch = process.arch === 'arm64' ? 'aarch64' : 'x64'
  const isZip = os === 'windows'
  const archive = path.join(javaDir, `temurin-${major}.${isZip ? 'zip' : 'tar.gz'}`)
  /*
   * JRE statt JDK: Minecraft wird nur ausgeführt, nie übersetzt — das
   * Entwicklerpaket ist dafür vier Mal so groß wie nötig (Java 17: 182 MB
   * gegenüber 42 MB). Auf einem frischen Rechner war genau dieser Download
   * die gefühlte Ewigkeit, in der „nichts passiert“.
   *
   * Sollte für eine Kombination kein JRE angeboten werden, wird das JDK
   * als Rückfallebene geladen.
   */
  const quelle = (art: 'jre' | 'jdk'): string =>
    `https://api.adoptium.net/v3/binary/latest/${major}/ga/${os}/${arch}/${art}/hotspot/normal/eclipse`

  // Fortschritt nur bei ganzen Prozentschritten melden — sonst käme pro
  // Datenpaket eine Nachricht über die Prozessgrenze.
  let zuletzt = -1
  const fortschritt = (geladen: number, gesamt: number): void => {
    if (!gesamt) return
    const pct = Math.floor((geladen / gesamt) * 100)
    if (pct !== zuletzt) {
      zuletzt = pct
      onStatus('launch.javaDownload', { major, pct })
    }
  }
  try {
    await downloadFile(quelle('jre'), archive, fortschritt)
  } catch {
    zuletzt = -1
    await downloadFile(quelle('jdk'), archive, fortschritt)
  }

  onStatus('launch.javaExtract', { major })
  if (isZip) {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const AdmZip = require('adm-zip') as typeof import('adm-zip')
    new AdmZip(archive).extractAllTo(javaDir, true)
  } else {
    // macOS und Linux bringen tar mit; adm-zip kann kein tar.gz
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const { execFileSync } = require('child_process') as typeof import('child_process')
    execFileSync('tar', ['-xzf', archive, '-C', javaDir])
  }
  fs.unlinkSync(archive)
  const found = findJava(undefined, major)
  const check = found.match(/(?:jdk|jre)-?(\d+)/i)
  if (!check || Number(check[1]) !== major) {
    throw new VError('error.javaInstall', { major })
  }
  return found
}

/**
 * Java finden: expliziter Profil-Pfad > Launcher-eigene JDKs
 * (%APPDATA%/.visualclient/java) > passende Adoptium-Version >
 * irgendein JDK > "java" aus dem PATH.
 */
export function findJava(explicit?: string, preferredMajor?: number): string {
  if (explicit && fs.existsSync(explicit)) return explicit
  const win = process.platform === 'win32'
  const exe = win ? 'java.exe' : 'java'
  const roots = [path.join(DATA_DIR, 'java')]
  if (win) {
    roots.push(
      'C:\\Program Files\\Eclipse Adoptium',
      'C:\\Program Files\\Java',
      'C:\\Program Files (x86)\\Eclipse Adoptium'
    )
  } else if (process.platform === 'darwin') {
    roots.push('/Library/Java/JavaVirtualMachines')
  } else {
    roots.push('/usr/lib/jvm')
  }

  const candidates: { major: number; java: string }[] = []
  for (const root of roots) {
    if (!fs.existsSync(root)) continue
    for (const dir of fs.readdirSync(root)) {
      const major = Number(dir.match(/(?:jdk|jre)-?(\d+)/i)?.[1] ?? NaN)
      if (Number.isNaN(major)) continue
      // Auf macOS liegt die ausführbare Datei unter Contents/Home/bin
      for (const rel of [
        [dir, 'bin', exe],
        [dir, 'Contents', 'Home', 'bin', exe]
      ]) {
        const java = path.join(root, ...rel)
        if (fs.existsSync(java)) {
          candidates.push({ major, java })
          break
        }
      }
    }
  }
  candidates.sort((a, b) => a.major - b.major)
  if (preferredMajor) {
    const exact = candidates.find((c) => c.major === preferredMajor)
    if (exact) return exact.java
    // sonst: kleinstes Java >= Anforderung (neuere Majors sind kompatibel)
    const higher = candidates.find((c) => c.major > preferredMajor)
    if (higher) return higher.java
  }
  const any = candidates.pop()
  return any?.java ?? 'java'
}

export async function listMcVersions(): Promise<string[]> {
  const res = await fetch(VERSION_MANIFEST)
  const data = (await res.json()) as { versions: { id: string; type: string }[] }
  return data.versions.filter((v) => v.type === 'release').map((v) => v.id)
}

/** Empfohlene (sonst neueste) Forge-Version für eine MC-Version ermitteln. */
export async function resolveForgeVersion(mcVersion: string): Promise<string | null> {
  const res = await fetch(FORGE_PROMOS)
  const data = (await res.json()) as { promos: Record<string, string> }
  return data.promos[`${mcVersion}-recommended`] ?? data.promos[`${mcVersion}-latest`] ?? null
}

async function ensureForgeInstaller(mcVersion: string, forgeVersion: string): Promise<string> {
  const full = `${mcVersion}-${forgeVersion}`
  const dir = path.join(MC_ROOT, 'forge-installers')
  fs.mkdirSync(dir, { recursive: true })
  const dest = path.join(dir, `forge-${full}-installer.jar`)
  if (!fs.existsSync(dest)) {
    const url = `https://maven.minecraftforge.net/net/minecraftforge/forge/${full}/forge-${full}-installer.jar`
    await downloadFile(url, dest)
  }
  return dest
}

function send(win: BrowserWindow, channel: string, payload: unknown): void {
  if (!win.isDestroyed()) win.webContents.send(channel, payload)
}

/**
 * Setzt beim allerersten Start die Spielsprache auf die des Launchers.
 * Der In-Game-Mod uebersetzt sich ueber Minecrafts Sprachwahl — ohne das
 * hier staende der Launcher auf Russisch und das Menue im Spiel auf
 * Englisch. Steht der Schluessel schon in der Datei, bleibt die Wahl des
 * Spielers unangetastet.
 */
function spracheVorbelegen(dir: string, lang: string, legacy: boolean): void {
  try {
    const codes: Record<string, [string, string]> = {
      de: ['de_de', 'de_DE'],
      en: ['en_us', 'en_US'],
      ru: ['ru_ru', 'ru_RU']
    }
    const paar = codes[lang]
    if (!paar) return
    fs.mkdirSync(dir, { recursive: true })
    const datei = path.join(dir, 'options.txt')
    const zeilen = fs.existsSync(datei)
      ? fs.readFileSync(datei, 'utf8').split(/\r?\n/)
      : []
    if (zeilen.some((z) => z.startsWith('lang:'))) return
    zeilen.push(`lang:${legacy ? paar[1] : paar[0]}`)
    fs.writeFileSync(datei, zeilen.filter((z) => z !== '').join('\n') + '\n')
  } catch {
    // Sprache ist Beiwerk — ein Schreibfehler darf den Start nicht stoppen
  }
}

export async function launchProfile(
  win: BrowserWindow,
  profileId: string,
  lang = 'en'
): Promise<void> {
  // Mehrere Instanzen sind erlaubt — kein globaler Block.
  const profile = getProfile(profileId)
  if (!profile) throw new VError('error.profileMissing')
  spracheVorbelegen(instanceDir(profileId), lang, profile.mcVersion.startsWith('1.8'))
  const auth = await mclcAuth()
  if (!auth) throw new VError('error.notLoggedIn')

  const launcher = new Client()
  try {
    const loader = profile.loader ?? 'forge'
    // Abgebrochene Downloads aus frueheren Versuchen aussortieren - sie
    // werden sonst ewig weiterverwendet und reissen Fabric ab.
    send(win, 'launch:status', { key: 'launch.verify', progress: 0 })
    const kaputt = entferneKaputteJars([
      path.join(MC_ROOT, 'libraries'),
      path.join(MC_ROOT, 'versions')
    ])
    if (kaputt.length) console.log('Beschaedigte Dateien entfernt:', kaputt.join(', '))

    send(win, 'launch:status', { key: 'launch.java', progress: 0 })
    const javaMajor = await requiredJavaMajorAuto(profile.mcVersion)
    const javaPath =
      profile.javaPath && fs.existsSync(profile.javaPath)
        ? profile.javaPath
        : await ensureJava(javaMajor, (key, vars) =>
            // pct treibt zusätzlich den Fortschrittsbalken
            send(win, 'launch:status', { key, vars, progress: Number(vars?.pct ?? 0) })
          )

    console.log('Minecraft-Version:', profile.mcVersion)
    console.log('Benötigte Java-Version (Mojang-JSON):', javaMajor)
    console.log('Gefundenes Java:', javaPath)
    let forgeInstaller: string | undefined
    let customVersion: string | undefined
    let extraJvmArgs: string[] = []
    if (loader === 'fabric') {
      send(win, 'launch:status', { key: 'launch.fabric', progress: 0 })
      customVersion = await ensureFabric(profile.mcVersion)
    } else if (loader === 'quilt') {
      send(win, 'launch:status', { key: 'launch.quilt', progress: 0 })
      customVersion = await ensureQuilt(profile.mcVersion)
    } else if (loader === 'neoforge') {
      customVersion = await ensureNeoForge(profile.mcVersion, javaPath, (key, vars) =>
        send(win, 'launch:status', { key, vars, progress: 0 })
      )
      extraJvmArgs = neoforgeJvmArgs(customVersion)
    } else if (loader === 'vanilla') {
      send(win, 'launch:status', { key: 'launch.minecraft', progress: 0 })
    } else {
      send(win, 'launch:status', { key: 'launch.forge', progress: 0 })
      const forgeVersion = profile.forgeVersion || (await resolveForgeVersion(profile.mcVersion))
      forgeInstaller = forgeVersion
        ? await ensureForgeInstaller(profile.mcVersion, forgeVersion)
        : undefined
    }

    launcher.on('progress', (e: { type: string; task: number; total: number }) => {
      const pct = e.total > 0 ? Math.round((e.task / e.total) * 100) : 0
      send(win, 'launch:status', { key: 'launch.downloading', vars: { type: e.type, pct }, progress: pct })
    })
    launcher.on('debug', (line: string) => send(win, 'launch:log', line))
    launcher.on('data', (line: string) => send(win, 'launch:log', line))
    let counted = false
    const startInstance = (): void => {
      if (!counted) {
        counted = true
        runningCount++
        send(win, 'launch:instances', { count: runningCount })
        onInstanceChange?.(runningCount, { version: profile.mcVersion, profile: profile.name })
      }
    }
    launcher.on('close', (code: number) => {
      if (counted) {
        counted = false
        runningCount = Math.max(0, runningCount - 1)
        send(win, 'launch:instances', { count: runningCount })
        onInstanceChange?.(runningCount, { version: profile.mcVersion, profile: profile.name })
      }
      send(win, 'launch:closed', { code })
    })
    // Sobald das Spiel Ausgaben liefert, gilt die Instanz als gestartet
    launcher.on('data', () => startInstance())

    if (profile.javaArgs) {
      extraJvmArgs = extraJvmArgs.concat(profile.javaArgs.split(/\s+/).filter(Boolean))
    }
    // Branding-Mod (Fenstertitel + V-Icon) automatisch in Fabric/Quilt-Profile
    installBranding(profile.id, loader, profile.mcVersion)
    if (profile.autoVisuals) {
      try {
        installVisuals(profile.id)
      } catch {
        // Visual-Pack-Assets fehlen — Start geht trotzdem weiter
      }
    }
    const maxMem = profile.ramMb ? `${profile.ramMb}M` : `${profile.ramGb}G`
    const gameArgs = ['--title', `Visual Client ${profile.mcVersion}`]
    if (profile.winWidth && profile.winHeight && !profile.fullscreen) {
      gameArgs.push('--width', String(profile.winWidth), '--height', String(profile.winHeight))
    }
    if (profile.fullscreen) gameArgs.push('--fullscreen')

    await launcher.launch({
      authorization: auth as never,
      root: MC_ROOT,
      version: { number: profile.mcVersion, type: 'release', custom: customVersion },
      forge: forgeInstaller,
      memory: { max: maxMem, min: '1G' },
      javaPath,
      customArgs: extraJvmArgs.length ? extraJvmArgs : undefined,
      // MC ab 1.20 übernimmt --title/--width/--height; ältere Versionen
      // ignorieren unbekannte Argumente einfach
      customLaunchArgs: gameArgs,
      overrides: { gameDirectory: instanceDir(profile.id) }
    } as never)
    touchLastPlayed(profile.id)
    send(win, 'launch:status', { key: 'launch.started', progress: 100 })
  } catch (err) {
    throw err
  }
}
