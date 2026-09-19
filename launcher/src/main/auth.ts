import fs from 'fs'
import crypto from 'crypto'
import { safeStorage } from 'electron'
import { Auth } from 'msmc'
import { ACCOUNT_FILE, ACCOUNTS_FILE } from './paths'

export interface Account {
  id: string
  type: 'microsoft' | 'offline'
  name: string
  uuid?: string
  /** verschlüsselter msmc-Refresh-Token (nur microsoft) */
  saved?: string
}

interface MinecraftToken {
  mclc: () => unknown
  profile?: { name?: string; id?: string }
  mcToken?: string
}

interface AccountsFile {
  active: string | null
  accounts: Account[]
}

// MS-Sitzungen im Speicher (id -> Token), lazy wiederhergestellt
const msTokens = new Map<string, MinecraftToken>()

function encrypt(value: string): string | undefined {
  try {
    return safeStorage.isEncryptionAvailable()
      ? safeStorage.encryptString(value).toString('base64')
      : Buffer.from(value).toString('base64')
  } catch {
    return undefined
  }
}
function decrypt(value: string): string | null {
  try {
    return safeStorage.isEncryptionAvailable()
      ? safeStorage.decryptString(Buffer.from(value, 'base64'))
      : Buffer.from(value, 'base64').toString()
  } catch {
    return null
  }
}

function read(): AccountsFile {
  try {
    return JSON.parse(fs.readFileSync(ACCOUNTS_FILE, 'utf8'))
  } catch {
    // Migration vom alten Einzel-Account-Format
    try {
      const old = JSON.parse(fs.readFileSync(ACCOUNT_FILE, 'utf8'))
      if (old?.type) {
        const acc: Account = { id: crypto.randomUUID().slice(0, 8), ...old }
        const data = { active: acc.id, accounts: [acc] }
        write(data)
        return data
      }
    } catch {
      /* nichts zu migrieren */
    }
    return { active: null, accounts: [] }
  }
}
function write(data: AccountsFile): void {
  fs.writeFileSync(ACCOUNTS_FILE, JSON.stringify(data, null, 2))
}

export function listAccounts(): Account[] {
  return read().accounts.map(({ saved: _s, ...a }) => a)
}

export function getActiveAccount(): Account | null {
  const data = read()
  const acc = data.accounts.find((a) => a.id === data.active) ?? data.accounts[0] ?? null
  return acc ? (({ saved: _s, ...rest }) => rest)(acc) : null
}

export function setActiveAccount(id: string): Account | null {
  const data = read()
  if (data.accounts.some((a) => a.id === id)) {
    data.active = id
    write(data)
  }
  return getActiveAccount()
}

export function removeAccount(id: string): void {
  const data = read()
  data.accounts = data.accounts.filter((a) => a.id !== id)
  if (data.active === id) data.active = data.accounts[0]?.id ?? null
  msTokens.delete(id)
  write(data)
}

function upsert(acc: Account): void {
  const data = read()
  const idx = data.accounts.findIndex(
    (a) => (acc.uuid && a.uuid === acc.uuid) || (a.type === acc.type && a.name === acc.name)
  )
  if (idx >= 0) {
    acc.id = data.accounts[idx].id
    data.accounts[idx] = acc
  } else {
    data.accounts.push(acc)
  }
  data.active = acc.id
  write(data)
}

/** Öffnet das offizielle Microsoft-Login-Fenster (msmc, Electron-Modus). */
export async function loginMicrosoft(): Promise<Account> {
  const authManager = new Auth('select_account')
  const xbox = await authManager.launch('electron')
  const token = (await xbox.getMinecraft()) as unknown as MinecraftToken
  let saved: string | undefined
  try {
    const raw = (xbox as unknown as { save: () => string }).save()
    if (raw) saved = encrypt(typeof raw === 'string' ? raw : JSON.stringify(raw))
  } catch {
    /* Refresh nicht verfügbar */
  }
  const acc: Account = {
    id: crypto.randomUUID().slice(0, 8),
    type: 'microsoft',
    name: token.profile?.name ?? 'Spieler',
    uuid: token.profile?.id,
    saved
  }
  upsert(acc)
  const stored = read().accounts.find((a) => a.uuid === acc.uuid)
  if (stored) msTokens.set(stored.id, token)
  return getActiveAccount()!
}

export function loginOffline(name: string): Account {
  const clean = name.replace(/[^A-Za-z0-9_]/g, '').slice(0, 16) || 'Spieler'
  upsert({ id: crypto.randomUUID().slice(0, 8), type: 'offline', name: clean })
  return getActiveAccount()!
}

/** MS-Session eines Accounts aus gespeichertem Refresh-Token herstellen. */
async function ensureMsSession(id: string): Promise<boolean> {
  if (msTokens.has(id)) return true
  const acc = read().accounts.find((a) => a.id === id)
  if (acc?.type !== 'microsoft' || !acc.saved) return false
  const raw = decrypt(acc.saved)
  if (!raw) return false
  try {
    const authManager = new Auth('select_account')
    const xbox = await authManager.refresh(raw)
    const token = (await xbox.getMinecraft()) as unknown as MinecraftToken
    msTokens.set(id, token)
    try {
      const fresh = (xbox as unknown as { save: () => string }).save()
      if (fresh) {
        const data = read()
        const a = data.accounts.find((x) => x.id === id)
        if (a) {
          a.saved = encrypt(typeof fresh === 'string' ? fresh : JSON.stringify(fresh))
          write(data)
        }
      }
    } catch {
      /* alter Token bleibt */
    }
    return true
  } catch {
    return false
  }
}

/** Mojang-API-Access-Token des aktiven Accounts (Skin-Upload). */
export async function getMsAccessToken(): Promise<string | null> {
  const acc = getActiveAccount()
  if (acc?.type !== 'microsoft') return null
  if (!(await ensureMsSession(acc.id))) return null
  const t = msTokens.get(acc.id)!
  if (t.mcToken) return t.mcToken
  const mclc = t.mclc() as { access_token?: string } | undefined
  return mclc?.access_token ?? null
}

/** Auth-Objekt für minecraft-launcher-core (aktiver Account). */
export async function mclcAuth(): Promise<unknown | null> {
  const acc = getActiveAccount()
  if (!acc) return null
  if (acc.type === 'microsoft') {
    if (await ensureMsSession(acc.id)) return msTokens.get(acc.id)!.mclc()
    return null // Re-Login nötig
  }
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { Authenticator } = require('minecraft-launcher-core')
  return Authenticator.getAuth(acc.name)
}
