import fs from 'fs'
import path from 'path'
import crypto from 'crypto'
import { spawn, execFile } from 'child_process'
import { promisify } from 'util'
import AdmZip from 'adm-zip'
import { app } from 'electron'
import { DATA_DIR, MC_ROOT, instanceDir } from './paths'
import { getProfile, touchLastPlayed } from './profiles'
import { downloadFile } from './modrinth'
import { VError } from './errors'

const GAME = 'v0.2.97'
const exec = promisify(execFile)
const busy = new Set<string>()
const hash = (file: string, algorithm = 'sha256'): string => crypto.createHash(algorithm).update(fs.readFileSync(file)).digest('hex')

function preparationError(err: unknown, id: string): Error {
  if (err instanceof VError) return err
  const failure = err as { message?: string; stderr?: string; stdout?: string; code?: string | number }
  const output = String(failure.stderr || failure.message || err)
  const detail = output.trim().split(/\r?\n/).filter(Boolean).slice(-1).join('').slice(0, 400)
  try {
    fs.writeFileSync(path.join(instanceDir(id), 'rust-setup.log'),
      `${new Date().toISOString()}\nExit: ${failure.code ?? 'unknown'}\n${failure.stdout || ''}\n${output}\n`)
  } catch { /* A logging failure must not hide the original error. */ }
  return new VError('engine.prepareDetail', { detail })
}

async function verifiedDownload(url: string, dest: string, sha1: string): Promise<void> {
  const parsed = new URL(url)
  if (!/^[a-f0-9]{40}$/.test(sha1) || parsed.protocol !== 'https:' ||
      !['piston-meta.mojang.com', 'piston-data.mojang.com', 'launcher.mojang.com', 'launchermeta.mojang.com', 'resources.download.minecraft.net', 'libraries.minecraft.net'].includes(parsed.hostname)) throw new VError('engine.archive')
  if (fs.existsSync(dest) && hash(dest, 'sha1') === sha1) return
  fs.mkdirSync(path.dirname(dest), { recursive: true })
  const temp = `${dest}.${crypto.randomUUID()}.tmp`
  try {
    await downloadFile(url, temp)
    if (hash(temp, 'sha1') !== sha1) throw new VError('engine.checksum')
    fs.renameSync(temp, dest)
  } finally {
    fs.rmSync(temp, { force: true })
  }
}

function visualExecutable(): string {
  const bundled = path.join(app.getAppPath(), 'build', 'rust')
  const name = process.platform === 'win32' ? 'visual-client.exe' : 'visual-client'
  const manifest = path.join(bundled, 'build.json')
  if (!fs.existsSync(manifest) || !fs.existsSync(path.join(bundled, name))) throw new VError('engine.visualBuildMissing')
  const info = JSON.parse(fs.readFileSync(manifest, 'utf8'))
  if (!/^[a-f0-9]{64}$/.test(info.sha256)) throw new VError('engine.checksum')
  const target = path.join(DATA_DIR, 'runtimes', `visual-rust-${info.sha256}`, name)
  if (!fs.existsSync(target)) {
    const binary = fs.readFileSync(path.join(bundled, name))
    if (crypto.createHash('sha256').update(binary).digest('hex') !== info.sha256) throw new VError('engine.checksum')
    fs.mkdirSync(path.dirname(target), { recursive: true })
    fs.writeFileSync(target + '.tmp', binary, { mode: 0o755 })
    fs.renameSync(target + '.tmp', target)
  }
  return target
}

function profileRoot(id: string): string {
  const profile = getProfile(id)
  if (!profile || profile.engine !== 'horizon' || profile.mcVersion !== '26.3') throw new VError('engine.invalid')
  return instanceDir(id)
}

/** Direct engine distribution: no Horizon launcher process or second account store. */
async function engine(root: string): Promise<string> {
  const prior = path.join(root, 'horizon-launcher', 'versions', GAME)
  const binary = process.platform === 'win32' ? 'horizon-client.exe' : 'horizon-client'
  if (fs.existsSync(path.join(prior, binary))) return prior
  const dir = path.join(root, 'rust-engine', GAME)
  const platform = process.platform === 'win32' && process.arch === 'x64'
    ? { folder: 'horizon-windows-x64', sha256: '29d3cef224aecdafea314cf6a3acb5caf8228edef9a3c5d73a0b4fe3b2696fca' }
    : process.platform === 'darwin' && process.arch === 'arm64'
      ? { folder: 'horizon-macos-arm64', sha256: '587fb8a653324e6f1b3bf59f1f2227ad99201ccaaf3f31024db25ccd4f37c63e' }
      : null
  if (!platform) throw new VError('engine.directPlatform')
  const unpacked = path.join(dir, platform.folder)
  const preparer = process.platform === 'win32' ? 'horizon-prepare.exe' : 'horizon-prepare'
  const installed = fs.existsSync(path.join(unpacked, binary)) ? unpacked : dir
  if (fs.existsSync(path.join(dir, '.visual-ready')) && fs.existsSync(path.join(installed, binary))) return installed
  fs.mkdirSync(dir, { recursive: true })
  const archive = path.join(dir, 'engine.zip')
  await downloadFile(`https://github.com/horizon-rust/horizon/releases/download/${GAME}/${platform.folder}.zip`, archive)
  if (hash(archive) !== platform.sha256) throw new VError('engine.checksum')
  const zip = new AdmZip(archive)
  for (const entry of zip.getEntries()) {
    const target = path.resolve(dir, entry.entryName)
    if (!target.startsWith(path.resolve(dir) + path.sep)) throw new VError('engine.archive')
  }
  zip.extractAllTo(dir, true)
  if (!fs.existsSync(path.join(unpacked, binary)) || !fs.existsSync(path.join(unpacked, preparer)) || !fs.existsSync(path.join(unpacked, 'research-lock.json'))) throw new VError('engine.archive')
  if (process.platform === 'darwin') fs.chmodSync(path.join(unpacked, preparer), 0o755)
  fs.writeFileSync(path.join(dir, '.visual-ready'), GAME)
  return unpacked
}

async function prepare(id: string, status: (key: string) => void): Promise<{ dir: string; resources: string; home: string }> {
  const root = profileRoot(id)
  status('engine.installing')
  const dir = await engine(root)
  const resources = path.join(dir, '.local', 'horizon-client', '26.3')
  const home = path.join(root, 'horizon') // Preserve existing worlds.
  fs.mkdirSync(home, { recursive: true })
  if (fs.existsSync(path.join(resources, 'horizon-resources.json'))) return { dir, resources, home }
  status('engine.preparing')
  const pins = JSON.parse(fs.readFileSync(path.join(dir, 'research-lock.json'), 'utf8')).minecraft
  // Bootstrap an isolated vanilla installation from Mojang's pinned manifest.
  // A fresh Visual installation must not depend on a prior Horizon/Java launch.
  const pinnedManifest = path.join(dir, '.local', 'minecraft', '26.3', 'manifest.json')
  await verifiedDownload(`https://piston-meta.mojang.com/v1/packages/${pins.manifest_sha1}/26.3.json`, pinnedManifest, pins.manifest_sha1)
  const manifest = JSON.parse(fs.readFileSync(pinnedManifest, 'utf8'))
  const vanilla = path.join(MC_ROOT, 'versions', '26.3')
  await verifiedDownload(manifest.downloads.client.url, path.join(vanilla, '26.3.jar'), pins.client_sha1)
  fs.copyFileSync(pinnedManifest, path.join(vanilla, '26.3.json'))
  // The resource exporter needs the Java client classpath as well as its JAR.
  // Install all declared main artifacts, including dependencies used by exporters.
  const libraryRoot = path.resolve(MC_ROOT, 'libraries')
  for (const library of manifest.libraries || []) {
    const artifact = library.downloads?.artifact
    if (!artifact) continue
    const destination = path.resolve(libraryRoot, artifact.path)
    if (!destination.startsWith(libraryRoot + path.sep)) throw new VError('engine.archive')
    await verifiedDownload(artifact.url, destination, artifact.sha1)
  }
  const index = manifest.assetIndex
  if (!/^[a-zA-Z0-9_.-]+$/.test(index.id)) throw new VError('engine.archive')
  const indexFile = path.join(MC_ROOT, 'assets', 'indexes', `${index.id}.json`)
  await verifiedDownload(index.url, indexFile, index.sha1)
  const objects = Object.values(JSON.parse(fs.readFileSync(indexFile, 'utf8')).objects) as { hash: string }[]
  let next = 0
  // Bound parallel downloads; objects are content-addressed and safe to reuse.
  const workers = await Promise.allSettled(Array.from({ length: 6 }, async () => {
    while (next < objects.length) {
      const entry = objects[next++]
      if (!/^[a-f0-9]{40}$/.test(entry.hash)) throw new VError('engine.archive')
      const key = `${entry.hash.slice(0, 2)}/${entry.hash}`
      await verifiedDownload(`https://resources.download.minecraft.net/${key}`, path.join(MC_ROOT, 'assets', 'objects', key), entry.hash)
    }
  }))
  const failed = workers.find(r => r.status === 'rejected')
  if (failed?.status === 'rejected') throw failed.reason
  const server = path.join(dir, 'server-26.3.jar')
  if (!fs.existsSync(server) || hash(server, 'sha1') !== pins.server_sha1) {
    const url = new URL(manifest.downloads.server.url)
    if (url.protocol !== 'https:' || !['piston-data.mojang.com', 'launcher.mojang.com'].includes(url.hostname)) throw new VError('engine.archive')
    await downloadFile(url.href, server)
    if (hash(server, 'sha1') !== pins.server_sha1) throw new VError('engine.checksum')
  }
  const { ensureJava } = await import('./launch')
  const java = await ensureJava(pins.java_major, status)
  const javaHome = path.dirname(path.dirname(java))
  const preparer = path.join(dir, process.platform === 'win32' ? 'horizon-prepare.exe' : 'horizon-prepare')
  await exec(preparer, ['--root', dir, '--prepare', '--server-jar', server, '--minecraft-dir', MC_ROOT], {
    cwd: dir, env: { ...process.env, JAVA_HOME: javaHome, PATH: `${path.dirname(java)}${path.delimiter}${process.env.PATH || ''}` },
    windowsHide: true, timeout: 30 * 60 * 1000, maxBuffer: 32 * 1024 * 1024
  })
  if (!fs.existsSync(path.join(resources, 'horizon-resources.json'))) throw new VError('engine.prepareFailed')
  return { dir, resources, home }
}

export async function setupHorizon(id: string): Promise<void> {
  if (busy.has(id)) throw new VError('engine.busy')
  busy.add(id)
  try { await prepare(id, () => {}) }
  catch (err) { throw preparationError(err, id) }
  finally { busy.delete(id) }
}

export async function playHorizon(id: string, status: (key: string) => void, onStarted: () => void = () => {}, language = 'en'): Promise<void> {
  if (busy.has(id)) throw new VError('engine.busy')
  busy.add(id)
  try {
    const executable = visualExecutable()
    const { dir, resources, home } = await prepare(id, status)
    const bundledShader = path.join(app.getAppPath(), 'build', 'rust', 'shaderpacks', 'VisualGoldenHour')
    const profileShader = path.join(home, 'shaderpacks', 'VisualGoldenHour')
    if (fs.existsSync(bundledShader) && !fs.existsSync(profileShader)) {
      fs.cpSync(bundledShader, profileShader, { recursive: true })
    }
    const graphics = path.join(home, 'horizon-graphics.txt')
    // Older launcher builds wrote an unused filename. Never replace real engine settings.
    const legacyGraphics = path.join(home, 'horizon.txt')
    if (!fs.existsSync(graphics) && fs.existsSync(legacyGraphics)) fs.copyFileSync(legacyGraphics, graphics)
    // Initial profile only: preserve deliberate graphics choices on subsequent starts.
    if (!fs.existsSync(graphics)) fs.writeFileSync(graphics, 'lodDistance:256\nfarDetail:reduced\ncaveCulling:true\nframePacing:efficient\n')
    // One-time clear-distance preference; later user choices remain untouched.
    const clearDistance = path.join(home, '.visual-clear-distance-v2')
    if (!fs.existsSync(clearDistance)) {
      const values = fs.readFileSync(graphics, 'utf8').split(/\r?\n/).filter(line => line && !line.startsWith('fog:'))
      fs.writeFileSync(graphics, [...values, 'fog:false', ''].join('\n'))
      fs.writeFileSync(clearDistance, '1')
    }
    const options = path.join(home, 'options.txt')
    const locale = ({ de: 'de_de', en: 'en_us', ru: 'ru_ru' } as Record<string, string>)[language] || 'en_us'
    const lines = (fs.existsSync(options) ? fs.readFileSync(options, 'utf8') : '').split(/\r?\n/).filter(line => line && !line.startsWith('lang:'))
    fs.writeFileSync(options, [...lines, `lang:${locale}`, ''].join('\n'))
    status('engine.starting')
    await new Promise<void>((resolve, reject) => {
      const child = spawn(executable, ['--resources', resources], {
        cwd: home, env: { ...process.env, HORIZON_HOME: home }, windowsHide: true, shell: false, stdio: 'ignore'
      })
      child.once('error', reject)
      child.once('spawn', () => { touchLastPlayed(id); onStarted() })
      child.once('close', code => code === 0 ? resolve() : reject(new VError('engine.playFailed')))
    })
  } catch (err) { throw preparationError(err, id) }
  finally { busy.delete(id) }
}
