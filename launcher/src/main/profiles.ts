import fs from 'fs'
import crypto from 'crypto'
import { PROFILES_FILE, instanceDir } from './paths'

export interface InstalledMod {
  projectId: string
  versionId: string
  fileName: string
  name: string
  iconUrl?: string
  /** 'mod' (Standard) | 'resourcepack' | 'shader' */
  kind?: string
}

export type Loader = 'forge' | 'fabric' | 'neoforge' | 'quilt' | 'vanilla'

export interface Profile {
  id: string
  name: string
  mcVersion: string
  loader: Loader
  forgeVersion?: string // leer = neueste empfohlene wird beim Start ermittelt
  ramGb: number
  ramMb?: number // feinere Einstellung; hat Vorrang vor ramGb
  javaPath?: string
  javaArgs?: string
  winWidth?: number
  winHeight?: number
  fullscreen?: boolean
  group?: string
  autoVisuals?: boolean
  lastPlayed?: string
  mods: InstalledMod[]
  createdAt: string
}

function readAll(): Profile[] {
  try {
    return JSON.parse(fs.readFileSync(PROFILES_FILE, 'utf8'))
  } catch {
    return []
  }
}

function writeAll(profiles: Profile[]): void {
  fs.writeFileSync(PROFILES_FILE, JSON.stringify(profiles, null, 2))
}

export function listProfiles(): Profile[] {
  return readAll()
}

export function getProfile(id: string): Profile | undefined {
  return readAll().find((p) => p.id === id)
}

export function saveProfile(profile: Partial<Profile> & { name: string; mcVersion: string }): Profile {
  const profiles = readAll()
  let existing = profile.id ? profiles.find((p) => p.id === profile.id) : undefined
  if (existing) {
    Object.assign(existing, profile)
  } else {
    const { id: _ignored, ...rest } = profile
    existing = {
      id: crypto.randomUUID().slice(0, 8),
      loader: 'forge',
      ramGb: 4,
      mods: [],
      createdAt: new Date().toISOString(),
      // Neue Profile bringen den Visual-Mod von sich aus mit. Vorher stand
      // das auf "aus" — wer den Launcher frisch bekam, drückte auf Start und
      // landete in blankem Fabric, ohne zu ahnen, dass der Client-Mod noch
      // von Hand einzuschalten war. Abschaltbar bleibt es im Profildialog.
      autoVisuals: true,
      ...rest
    } as Profile
    profiles.push(existing)
  }
  writeAll(profiles)
  instanceDir(existing.id)
  return existing
}

export function deleteProfile(id: string): void {
  writeAll(readAll().filter((p) => p.id !== id))
}

/** Profil samt Instanz-Ordner (mods/resourcepacks/…) duplizieren. */
export function duplicateProfile(id: string): Profile | null {
  const src = getProfile(id)
  if (!src) return null
  const copy = saveProfile({
    ...src,
    id: undefined,
    name: src.name + ' (Kopie)',
    lastPlayed: undefined
  } as never)
  fs.cpSync(instanceDir(id), instanceDir(copy.id), { recursive: true })
  return copy
}

export function touchLastPlayed(id: string): void {
  const profiles = readAll()
  const p = profiles.find((x) => x.id === id)
  if (p) {
    p.lastPlayed = new Date().toISOString()
    writeAll(profiles)
  }
}

export function updateMods(id: string, mods: InstalledMod[]): void {
  const profiles = readAll()
  const p = profiles.find((x) => x.id === id)
  if (p) {
    p.mods = mods
    writeAll(profiles)
  }
}
