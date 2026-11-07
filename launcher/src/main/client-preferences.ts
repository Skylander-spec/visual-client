import fs from 'fs'
import path from 'path'
import crypto from 'crypto'
import { DATA_DIR } from './paths'
import { clientDefaults as defaults, validPreference, type ClientPreferences } from '../shared/client-preferences'

const file = path.join(process.platform === 'win32' ? DATA_DIR : path.join(process.env.HOME || DATA_DIR, '.visualclient'), 'visualsfabric.json')
function read(): Record<string, unknown> {
  if (!fs.existsSync(file)) return {}
  const value = JSON.parse(fs.readFileSync(file, 'utf8'))
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('Invalid client configuration')
  return value
}
export function clientPreferences(): ClientPreferences {
  const current = read()
  return Object.fromEntries(Object.entries(defaults).map(([key, fallback]) => [key, validPreference(key as keyof ClientPreferences, current[key]) ? current[key] : fallback])) as ClientPreferences
}
export function saveClientPreferences(input: ClientPreferences): ClientPreferences {
  if (!input || typeof input !== 'object') throw new Error('Invalid preferences')
  for (const key of Object.keys(defaults) as (keyof ClientPreferences)[]) if (!validPreference(key, input[key])) throw new Error('Invalid preference: ' + key)
  const current = read()
  const next = { ...current, ...Object.fromEntries(Object.keys(defaults).map(key => [key, input[key as keyof ClientPreferences]])), miniMeStand: 1 }
  fs.mkdirSync(path.dirname(file), { recursive: true })
  const tmp = `${file}.${crypto.randomUUID()}.tmp`
  try { fs.writeFileSync(tmp, JSON.stringify(next, null, 2)); fs.renameSync(tmp, file) }
  finally { if (fs.existsSync(tmp)) fs.unlinkSync(tmp) }
  return clientPreferences()
}
