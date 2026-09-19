import fs from 'fs'
import path from 'path'
import { DATA_DIR } from './paths'

const FILE = path.join(DATA_DIR, 'settings.json')

/**
 * Feste Discord-Application-ID der "Visual Client"-App. Bewusst hier
 * hartkodiert (nicht in den Nutzereinstellungen), damit sie nicht
 * geändert werden kann und die Präsenz überall automatisch funktioniert.
 */
export const DISCORD_CLIENT_ID = '1526118305625018510'

export interface AppSettings {
  discordEnabled: boolean
}

const DEFAULTS: AppSettings = {
  discordEnabled: true
}

export function getSettings(): AppSettings {
  try {
    const stored = JSON.parse(fs.readFileSync(FILE, 'utf8'))
    return { ...DEFAULTS, discordEnabled: stored.discordEnabled ?? DEFAULTS.discordEnabled }
  } catch {
    return { ...DEFAULTS }
  }
}

export function saveSettings(patch: Partial<AppSettings>): AppSettings {
  const merged = { ...getSettings(), ...patch }
  fs.writeFileSync(FILE, JSON.stringify(merged, null, 2))
  return merged
}
