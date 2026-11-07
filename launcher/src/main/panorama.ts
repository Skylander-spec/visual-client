import fs from 'fs'
import path from 'path'
import AdmZip from 'adm-zip'
import { app } from 'electron'
import { MC_ROOT } from './paths'

let cached: string[] | null | undefined

/** Use the player's installed assets; Minecraft textures are not bundled with Visual. */
export function panoramaFaces(): string[] | null {
  if (cached) return cached
  let best: Buffer[] | undefined
  let bestSize = 0
  const consider = (faces: Buffer[]): void => {
    const sizes = faces.map(face => {
      if (face.length < 24 || face.length > 8 * 1024 * 1024 || face.subarray(0, 8).toString('hex') !== '89504e470d0a1a0a') return 0
      const width = face.readUInt32BE(16), height = face.readUInt32BE(20)
      return width === height && width >= 256 && width <= 8192 ? width : 0
    })
    if (sizes.length !== 6 || sizes.some(size => size !== sizes[0]) || sizes[0] <= bestSize) return
    best = faces
    bestSize = sizes[0]
  }
  const official = process.platform === 'win32'
    ? path.join(app.getPath('appData'), '.minecraft')
    : process.platform === 'darwin' ? path.join(app.getPath('appData'), 'minecraft') : path.join(app.getPath('home'), '.minecraft')
  for (const root of [official, MC_ROOT]) {
    // Modern Minecraft stores real panoramas in the asset object store; JARs
    // contain placeholders. Read complete sets from a single installed index.
    const assets = path.join(root, 'assets')
    const indexes = path.join(assets, 'indexes')
    if (fs.existsSync(indexes)) {
      const names = fs.readdirSync(indexes).filter(name => name.endsWith('.json'))
        .sort((a, b) => b.localeCompare(a, undefined, { numeric: true })).slice(0, 16)
      for (const name of names) {
        try {
          const index = JSON.parse(fs.readFileSync(path.join(indexes, name), 'utf8'))
          const faces = Array.from({ length: 6 }, (_, i) => {
            const entry = index.objects?.[`minecraft/textures/gui/title/background/panorama_${i}.png`]
            if (!entry || !/^[a-f0-9]{40}$/.test(entry.hash) || entry.size > 8 * 1024 * 1024) throw new Error('Missing panorama face')
            return fs.readFileSync(path.join(assets, 'objects', entry.hash.slice(0, 2), entry.hash))
          })
          consider(faces)
        } catch { /* Incomplete or older asset index; try the next set. */ }
      }
    }
    const versions = path.join(root, 'versions')
    if (!fs.existsSync(versions)) continue
    const names = fs.readdirSync(versions).filter(name => /^\d+(\.\d+)+$/.test(name))
    // Classic panorama first; some modern client JARs contain placeholder images.
    const candidates = [...new Set(['1.8.9', ...names.sort((a, b) => b.localeCompare(a, undefined, { numeric: true }))])].slice(0, 12)
    for (const version of candidates) {
      const jar = path.join(versions, version, `${version}.jar`)
      if (!fs.existsSync(jar)) continue
      try {
        const zip = new AdmZip(jar)
        const entries = Array.from({ length: 6 }, (_, i) => zip.getEntry(`assets/minecraft/textures/gui/title/background/panorama_${i}.png`))
        if (entries.some(e => !e || e.header.size < 1000 || e.header.size > 8 * 1024 * 1024)) continue
        consider(entries.map(e => e!.getData()))
      } catch { /* Try another local version; keep the landscape fallback. */ }
    }
  }
  cached = best ? best.map(face => `data:image/png;base64,${face.toString('base64')}`) : null
  return cached
}
