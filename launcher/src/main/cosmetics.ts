import fs from 'fs'
import path from 'path'
import { BrowserWindow, dialog } from 'electron'
import { DATA_DIR, COSMETICS_DIR } from './paths'
import { getActiveAccount, getMsAccessToken } from './auth'
import { VError } from './errors'

const SKINS_DIR = path.join(DATA_DIR, 'skins')
const CAPES_DIR = path.join(DATA_DIR, 'capes')

function ensure(): void {
  fs.mkdirSync(SKINS_DIR, { recursive: true })
  fs.mkdirSync(CAPES_DIR, { recursive: true })
}

export interface CosmeticFile {
  name: string
  dataUrl: string
}

function listPngs(dir: string): CosmeticFile[] {
  ensure()
  return fs
    .readdirSync(dir)
    .filter((f) => f.toLowerCase().endsWith('.png'))
    .map((f) => ({
      name: f.replace(/\.png$/i, ''),
      dataUrl: 'data:image/png;base64,' + fs.readFileSync(path.join(dir, f)).toString('base64')
    }))
}

async function addViaDialog(win: BrowserWindow, dir: string, title: string): Promise<CosmeticFile | null> {
  ensure()
  const res = await dialog.showOpenDialog(win, {
    title,
    filters: [{ name: 'PNG-Bild', extensions: ['png'] }],
    properties: ['openFile']
  })
  if (res.canceled || !res.filePaths[0]) return null
  const src = res.filePaths[0]
  const name = path.basename(src, '.png').replace(/[^\w\- ]/g, '').slice(0, 40) || 'eigen'
  const dest = path.join(dir, name + '.png')
  fs.copyFileSync(src, dest)
  return { name, dataUrl: 'data:image/png;base64,' + fs.readFileSync(dest).toString('base64') }
}

/* ---------------- Skins ---------------- */

export const listSkins = (): CosmeticFile[] => listPngs(SKINS_DIR)

/* Der Dateidialog gehoert dem Hauptprozess, die Sprache steht aber im
   Renderer (localStorage). Deshalb reicht der Renderer den fertigen
   Titel durch — derselbe Weg wie bei allen anderen sichtbaren Texten. */
export const addSkin = (win: BrowserWindow, titel: string): Promise<CosmeticFile | null> =>
  addViaDialog(win, SKINS_DIR, titel)

export function removeSkin(name: string): void {
  const f = path.join(SKINS_DIR, name + '.png')
  if (fs.existsSync(f)) fs.unlinkSync(f)
}

function cleanName(name: string): string {
  return name.replace(/[^\w\- ]/g, '').trim().slice(0, 40) || 'unbenannt'
}

/** Skin-Datei umbenennen. Gibt den (bereinigten) neuen Namen zurück. */
export function renameSkin(oldName: string, newName: string): string {
  const clean = cleanName(newName)
  const src = path.join(SKINS_DIR, oldName + '.png')
  const dest = path.join(SKINS_DIR, clean + '.png')
  if (fs.existsSync(src) && src !== dest && !fs.existsSync(dest)) {
    fs.renameSync(src, dest)
  }
  return clean
}

/**
 * Skin auf den Microsoft-Account hochladen (offizielle Mojang-API).
 * Offline-Accounts haben serverseitig keinen Skin — dafür kommt eine
 * verständliche Fehlermeldung zurück.
 */
export async function applySkin(name: string, variant: 'classic' | 'slim'): Promise<string> {
  const token = await getMsAccessToken()
  if (!token) {
    const acc = getActiveAccount()
    throw new VError(
      acc?.type === 'microsoft' ? 'error.sessionExpired' : 'error.needMicrosoft'
    )
  }
  const file = path.join(SKINS_DIR, name + '.png')
  if (!fs.existsSync(file)) throw new VError('error.skinMissing')

  const form = new FormData()
  form.append('variant', variant)
  form.append('file', new Blob([fs.readFileSync(file)], { type: 'image/png' }), 'skin.png')
  const res = await fetch('https://api.minecraftservices.com/minecraft/profile/skins', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form
  })
  if (!res.ok) {
    throw new VError('error.mojang', { status: res.status, detail: (await res.text()).slice(0, 160) })
  }
  return `Skin „${name}" (${variant === 'slim' ? 'Schlank' : 'Klassisch'}) ist jetzt aktiv.`
}

/* ---------------- Capes (lokal, gerendert von VisualsMod) ---------------- */

export const listCapes = (): CosmeticFile[] => listPngs(CAPES_DIR)

export const addCape = (win: BrowserWindow, titel: string): Promise<CosmeticFile | null> =>
  addViaDialog(win, CAPES_DIR, titel)

/** Von der Preset-Galerie im Renderer erzeugte Capes speichern. */
export function saveCapePreset(name: string, dataUrl: string): void {
  ensure()
  const clean = name.replace(/[^\w\- ]/g, '').slice(0, 40)
  const base64 = dataUrl.replace(/^data:image\/png;base64,/, '')
  fs.writeFileSync(path.join(CAPES_DIR, clean + '.png'), Buffer.from(base64, 'base64'))
}

export function removeCape(name: string): void {
  const f = path.join(CAPES_DIR, name + '.png')
  if (fs.existsSync(f)) fs.unlinkSync(f)
  const active = activeCape()
  if (active === name) {
    const target = path.join(COSMETICS_DIR, 'cape.png')
    if (fs.existsSync(target)) fs.unlinkSync(target)
  }
}

/** Cape-Datei umbenennen; aktiven-Marker mitziehen. Gibt neuen Namen zurück. */
export function renameCape(oldName: string, newName: string): string {
  const clean = cleanName(newName)
  const src = path.join(CAPES_DIR, oldName + '.png')
  const dest = path.join(CAPES_DIR, clean + '.png')
  if (fs.existsSync(src) && src !== dest && !fs.existsSync(dest)) {
    fs.renameSync(src, dest)
    if (activeCape() === oldName) {
      fs.writeFileSync(path.join(COSMETICS_DIR, 'cape.name'), clean)
    }
  }
  return clean
}

/** Cape aktivieren = nach cosmetics/cape.png kopieren (liest VisualsMod). */
export function setActiveCape(name: string | null): void {
  const target = path.join(COSMETICS_DIR, 'cape.png')
  const marker = path.join(COSMETICS_DIR, 'cape.name')
  if (!name) {
    if (fs.existsSync(target)) fs.unlinkSync(target)
    if (fs.existsSync(marker)) fs.unlinkSync(marker)
    return
  }
  fs.copyFileSync(path.join(CAPES_DIR, name + '.png'), target)
  fs.writeFileSync(marker, name)
}

/**
 * Aktuelle Texturen fürs 3D-Modell: Skin frisch von Mojang (ohne Cache,
 * zeigt Skin-Wechsel sofort) + aktives lokales Cape als Data-URLs.
 */
export async function profileTextures(
  uuid?: string
): Promise<{ skin: string | null; cape: string | null; slim: boolean }> {
  let skin: string | null = null
  let slim = false
  if (uuid) {
    try {
      const res = await fetch(
        `https://sessionserver.mojang.com/session/minecraft/profile/${uuid.replace(/-/g, '')}`
      )
      const data = (await res.json()) as { properties?: { name: string; value: string }[] }
      const prop = data.properties?.find((p) => p.name === 'textures')
      if (prop) {
        const tex = JSON.parse(Buffer.from(prop.value, 'base64').toString()) as {
          textures?: { SKIN?: { url: string; metadata?: { model?: string } } }
        }
        const skinInfo = tex.textures?.SKIN
        if (skinInfo?.url) {
          const png = await fetch(skinInfo.url)
          skin = 'data:image/png;base64,' + Buffer.from(await png.arrayBuffer()).toString('base64')
          slim = skinInfo.metadata?.model === 'slim'
        }
      }
    } catch {
      /* offline / Mojang nicht erreichbar -> Fallback im Renderer */
    }
  }
  let cape: string | null = null
  const capeFile = path.join(COSMETICS_DIR, 'cape.png')
  if (fs.existsSync(capeFile)) {
    cape = 'data:image/png;base64,' + fs.readFileSync(capeFile).toString('base64')
  }
  return { skin, cape, slim }
}

export function activeCape(): string | null {
  const marker = path.join(COSMETICS_DIR, 'cape.name')
  try {
    return fs.readFileSync(marker, 'utf8').trim() || null
  } catch {
    return null
  }
}
