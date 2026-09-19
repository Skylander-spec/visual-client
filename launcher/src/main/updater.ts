import fs from 'fs'
import path from 'path'
import crypto from 'crypto'
import { app } from 'electron'
import { spawn } from 'child_process'
import { DATA_DIR } from './paths'

/**
 * Selbst-Update in zwei Stufen.
 *
 * Unten liegt der "Release-Kanal": der Ordner
 * %APPDATA%/.visualclient/release mit einer app.asar und einer latest.json
 * ({ version, hash }). Unterscheidet sich die dortige Datei von der
 * installierten, gilt ein Update als verfuegbar und wird beim Schliessen
 * darueberkopiert. Das bleibt der einzige Weg, wie eine neue Fassung
 * eingespielt wird — auch beim Entwickeln, wo der Kanal von Hand gefuellt
 * wird.
 *
 * Darueber liegt GitHub: beim Start fragt der Launcher das neueste Release
 * des Repos ab, und wenn dessen Version hoeher ist als die laufende, laedt
 * er app.asar und latest.json in genau diesen Kanal. Danach greift wieder
 * die Logik von unten. Zwei getrennte Stufen, damit ein Netzproblem nie
 * mehr kaputtmachen kann als "kein Update gefunden".
 *
 * Was ein Update NICHT anfasst: alles, was dem Spieler gehoert. Ersetzt
 * wird ausschliesslich die app.asar in der Installation. Profile, Konten,
 * Welten, selbst installierte Mods, Skins, Capes und Ressourcenpakete
 * liegen unter %APPDATA%/.visualclient bzw. in den Instanzordnern und
 * werden hier nirgends geschrieben oder geloescht.
 */
const RELEASE_DIR = path.join(DATA_DIR, 'release')
const RELEASE_ASAR = path.join(RELEASE_DIR, 'app.asar')
const RELEASE_JSON = path.join(RELEASE_DIR, 'latest.json')

/** Oeffentliches Repo — bewusst ohne Token, der Feed ist frei lesbar. */
const REPO = 'Skylander-spec/visual-client'
const RELEASE_API = `https://api.github.com/repos/${REPO}/releases/latest`

let pendingVersion: string | null = null
let applying = false

type Release = {
  tag_name?: string
  name?: string
  prerelease?: boolean
  draft?: boolean
  assets?: { name: string; browser_download_url: string }[]
}

/** "1.4.10" ist neuer als "1.4.9" — deshalb Zahl fuer Zahl, nicht als Text. */
function istNeuer(kandidat: string, laufend: string): boolean {
  const zahlen = (v: string): number[] =>
    v.replace(/^v/i, '').split('.').map((t) => parseInt(t, 10) || 0)
  const a = zahlen(kandidat)
  const b = zahlen(laufend)
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    const x = a[i] ?? 0
    const y = b[i] ?? 0
    if (x !== y) return x > y
  }
  return false
}

async function holen(url: string, ms: number): Promise<Response> {
  const res = await fetch(url, {
    signal: AbortSignal.timeout(ms),
    headers: {
      // GitHub weist Anfragen ohne User-Agent ab.
      'User-Agent': 'visual-client-updater',
      Accept: 'application/vnd.github+json'
    }
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res
}

/**
 * Neuestes Release abfragen und bei Bedarf in den Kanal legen.
 *
 * Geladen wird erst in eine Datei daneben und nur nach Hash-Pruefung
 * umbenannt: ein abgebrochener Download darf nie als fertiges Update im
 * Kanal liegen bleiben, sonst kopiert die Stufe darunter Bruchstuecke
 * ueber die Installation.
 */
async function vonGithub(): Promise<boolean> {
  const rel = (await (await holen(RELEASE_API, 10_000)).json()) as Release
  if (rel.draft || rel.prerelease) return false
  const version = (rel.tag_name ?? rel.name ?? '').replace(/^v/i, '')
  if (!version || !istNeuer(version, app.getVersion())) return false

  const asar = rel.assets?.find((a) => a.name === 'app.asar')
  const meta = rel.assets?.find((a) => a.name === 'latest.json')
  if (!asar || !meta) return false

  const beschreibung = (await (await holen(meta.browser_download_url, 10_000)).json()) as {
    version?: string
    hash?: string
  }
  if (!beschreibung.hash) return false

  fs.mkdirSync(RELEASE_DIR, { recursive: true })
  const teil = RELEASE_ASAR + '.teil'
  const daten = Buffer.from(await (await holen(asar.browser_download_url, 300_000)).arrayBuffer())
  fs.writeFileSync(teil, daten)
  const geprueft = crypto.createHash('sha256').update(daten).digest('hex')
  if (geprueft !== beschreibung.hash) {
    fs.rmSync(teil, { force: true })
    return false
  }
  fs.renameSync(teil, RELEASE_ASAR)
  fs.writeFileSync(
    RELEASE_JSON,
    JSON.stringify({ version, hash: geprueft, source: REPO }, null, 2)
  )
  return true
}

/**
 * GitHub abfragen. Wirft nie — ohne Netz, ohne Repo oder bei einem
 * kaputten Download bleibt es schlicht bei der laufenden Fassung.
 */
export async function checkGithub(): Promise<{ available: boolean; version?: string }> {
  if (!app.isPackaged) return { available: false }
  try {
    if (await vonGithub()) return checkForUpdate()
  } catch {
    /* kein Netz, kein Release, kein Update */
  }
  return { available: !!pendingVersion, version: pendingVersion ?? undefined }
}

function sha256(file: string): string {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')
}

/**
 * Vergleicht die installierte app.asar mit der im Release-Kanal per Hash.
 * Ist die Kanal-Datei anders, gilt ein Update als verfuegbar — unabhaengig
 * von der Versionsnummer, damit auch ein von Hand gefuellter Kanal greift.
 */
export function checkForUpdate(): { available: boolean; version?: string } {
  if (!app.isPackaged) return { available: false }
  try {
    if (!fs.existsSync(RELEASE_ASAR)) return { available: false }
    const latest = JSON.parse(fs.readFileSync(RELEASE_JSON, 'utf8')) as {
      version?: string
      hash?: string
    }
    const channelHash = latest.hash ?? sha256(RELEASE_ASAR)
    const installedHash = sha256(app.getAppPath())
    if (channelHash !== installedHash) {
      pendingVersion = latest.version ?? 'neu'
      return { available: true, version: pendingVersion }
    }
  } catch {
    /* kein Release / kein Update */
  }
  return { available: false }
}

export function updateState(): { available: boolean; version: string | null; current: string } {
  return { available: !!pendingVersion, version: pendingVersion, current: app.getVersion() }
}

/**
 * Wendet das Update an: wartet (in einem separaten Prozess) bis Visual
 * Client beendet ist, kopiert die neue app.asar über die installierte und
 * startet optional neu. Die app.asar selbst ist im Betrieb gesperrt, daher
 * die Swap-per-Batch-Lösung.
 */
export function applyUpdate(relaunch: boolean): void {
  if (!pendingVersion || applying || !app.isPackaged) return
  applying = true
  const installedAsar = app.getAppPath() // …/resources/app.asar
  const exe = app.getPath('exe')

  if (process.platform === 'win32') {
    const cmdFile = path.join(app.getPath('temp'), 'visual-client-update.cmd')
    const lines = [
      '@echo off',
      ':wait',
      'tasklist /FI "IMAGENAME eq Visual Client.exe" 2>nul | find /I "Visual Client.exe" >nul && (ping -n 2 127.0.0.1 >nul & goto wait)',
      `copy /Y "${RELEASE_ASAR}" "${installedAsar}" >nul`,
      relaunch ? `start "" "${exe}"` : '',
      'del "%~f0"'
    ].filter(Boolean)
    fs.writeFileSync(cmdFile, lines.join('\r\n'))
    spawn('cmd.exe', ['/c', cmdFile], { detached: true, stdio: 'ignore', windowsHide: true }).unref()
    return
  }

  // macOS und Linux: dasselbe Prinzip als Shell-Skript. cmd.exe und tasklist
  // gibt es dort nicht — ohne diesen Zweig liefe das Update ins Leere.
  const shFile = path.join(app.getPath('temp'), 'visual-client-update.sh')
  const script = [
    '#!/bin/bash',
    `while kill -0 ${process.pid} 2>/dev/null; do sleep 1; done`,
    `cp -f "${RELEASE_ASAR}" "${installedAsar}"`,
    relaunch ? `open -a "${exe.replace(/\/Contents\/MacOS\/.*$/, '')}" || "${exe}" &` : '',
    'rm -f "$0"'
  ].filter(Boolean)
  fs.writeFileSync(shFile, script.join('\n'), { mode: 0o755 })
  spawn('/bin/bash', [shFile], { detached: true, stdio: 'ignore' }).unref()
}

/** Ob ein Update aussteht (für die Quit-Logik). */
export function hasPending(): boolean {
  return !!pendingVersion
}
