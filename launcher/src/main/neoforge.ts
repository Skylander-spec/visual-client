import fs from 'fs'
import type { StatusFn } from './launch'
import path from 'path'
import { spawn } from 'child_process'
import { MC_ROOT } from './paths'
import { downloadFile } from './modrinth'
import { VError } from './errors'

const MAVEN = 'https://maven.neoforged.net/releases/net/neoforged/neoforge'

/**
 * NeoForge-Version zur MC-Version finden (NeoForge-Schema: 1.20.4 -> "20.4.x").
 * NeoForge gibt es erst ab Minecraft 1.20.2.
 */
async function resolveNeoForgeVersion(mcVersion: string): Promise<string> {
  const m = mcVersion.match(/^1\.(\d+)(?:\.(\d+))?$/)
  if (!m) throw new VError('error.invalidVersion', { version: mcVersion })
  const prefix = `${m[1]}.${m[2] ?? '0'}.`
  const xml = await (await fetch(`${MAVEN}/maven-metadata.xml`)).text()
  const versions = [...xml.matchAll(/<version>([^<]+)<\/version>/g)].map((x) => x[1])
  const matching = versions.filter((v) => v.startsWith(prefix))
  const stable = matching.filter((v) => !v.includes('beta'))
  const pick = (stable.length ? stable : matching).pop()
  if (!pick) {
    throw new VError('error.neoforgeUnsupported', { version: mcVersion })
  }
  return pick
}

/**
 * NeoForge per offiziellem Installer headless installieren und die
 * Custom-Version-ID (z. B. "neoforge-20.4.237") zurückgeben.
 */
export async function ensureNeoForge(
  mcVersion: string,
  javaPath: string,
  onStatus: StatusFn
): Promise<string> {
  const version = await resolveNeoForgeVersion(mcVersion)
  const id = `neoforge-${version}`

  const versionJson = path.join(MC_ROOT, 'versions', id, `${id}.json`)
  if (fs.existsSync(versionJson)) return id

  onStatus('launch.neoforge', { version })
  const installerDir = path.join(MC_ROOT, 'forge-installers')
  fs.mkdirSync(installerDir, { recursive: true })
  const installer = path.join(installerDir, `neoforge-${version}-installer.jar`)
  if (!fs.existsSync(installer)) {
    await downloadFile(`${MAVEN}/${version}/neoforge-${version}-installer.jar`, installer)
  }

  // Der Installer verlangt eine launcher_profiles.json im Zielordner
  const profilesJson = path.join(MC_ROOT, 'launcher_profiles.json')
  if (!fs.existsSync(profilesJson)) {
    fs.writeFileSync(profilesJson, JSON.stringify({ profiles: {} }, null, 2))
  }

  await new Promise<void>((resolve, reject) => {
    const proc = spawn(javaPath, ['-jar', installer, '--installClient', MC_ROOT], {
      stdio: ['ignore', 'pipe', 'pipe']
    })
    let out = ''
    proc.stdout.on('data', (d) => (out += d))
    proc.stderr.on('data', (d) => (out += d))
    proc.on('error', reject)
    proc.on('close', (code) => {
      if (code === 0) resolve()
      else reject(new VError('error.neoforgeInstaller', { code: code ?? -1, detail: out.slice(-400) }))
    })
  })

  if (!fs.existsSync(versionJson)) {
    throw new VError('error.neoforgeIncomplete')
  }
  return id
}

/**
 * JVM-Argumente aus dem NeoForge-Versions-JSON extrahieren (Module-Path,
 * --add-opens usw.). minecraft-launcher-core übernimmt diese bei
 * Custom-Versionen nicht selbst — ohne sie crasht der BootstrapLauncher.
 */
export function neoforgeJvmArgs(id: string): string[] {
  const versionJson = path.join(MC_ROOT, 'versions', id, `${id}.json`)
  const data = JSON.parse(fs.readFileSync(versionJson, 'utf8')) as {
    arguments?: { jvm?: unknown[] }
  }
  const libDir = path.join(MC_ROOT, 'libraries')
  return (data.arguments?.jvm ?? [])
    .filter((a): a is string => typeof a === 'string')
    .map((a) =>
      a
        .replaceAll('${library_directory}', libDir)
        .replaceAll('${classpath_separator}', path.delimiter)
        .replaceAll('${version_name}', id)
    )
}
