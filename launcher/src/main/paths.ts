import { app } from 'electron'
import path from 'path'
import fs from 'fs'

/** Alle Launcher-Daten liegen unter %APPDATA%/.visualclient */
export const DATA_DIR = path.join(
  process.env.APPDATA ?? app.getPath('appData'),
  '.visualclient'
)
export const MC_ROOT = path.join(DATA_DIR, 'minecraft') // geteilte versions/libraries/assets
export const INSTANCES_DIR = path.join(DATA_DIR, 'instances')
export const COSMETICS_DIR = path.join(DATA_DIR, 'cosmetics')
export const PROFILES_FILE = path.join(DATA_DIR, 'profiles.json')
export const ACCOUNT_FILE = path.join(DATA_DIR, 'account.json') // Altformat (Migration)
export const ACCOUNTS_FILE = path.join(DATA_DIR, 'accounts.json')

export function ensureDirs(): void {
  for (const d of [DATA_DIR, MC_ROOT, INSTANCES_DIR, COSMETICS_DIR]) {
    fs.mkdirSync(d, { recursive: true })
  }
}

export function instanceDir(profileId: string): string {
  const dir = path.join(INSTANCES_DIR, profileId)
  fs.mkdirSync(path.join(dir, 'mods'), { recursive: true })
  fs.mkdirSync(path.join(dir, 'resourcepacks'), { recursive: true })
  return dir
}
