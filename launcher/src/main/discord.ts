import { Client } from '@xhayper/discord-rpc'
import { getSettings, DISCORD_CLIENT_ID } from './settings'

let client: Client | null = null
let ready = false
let connecting = false
let startedAt = Date.now()
let retryTimer: ReturnType<typeof setInterval> | null = null
let current: { playing: boolean; version?: string; profile?: string } = { playing: false }

/**
 * Discord Rich Presence: zeigt "Visual Client" mit Status (im Launcher /
 * spielt Minecraft <version>). Scheitert leise, wenn Discord nicht läuft
 * oder keine Client-ID gesetzt ist.
 */
async function tryConnect(): Promise<void> {
  const { discordEnabled } = getSettings()
  if (!discordEnabled) return
  if (client || connecting) return
  connecting = true
  try {
    const c = new Client({ clientId: DISCORD_CLIENT_ID })
    c.on('ready', () => {
      ready = true
      push()
    })
    c.on('disconnected', () => {
      // Discord wurde geschlossen — beim nächsten Retry neu verbinden
      client = null
      ready = false
    })
    await c.login()
    client = c
  } catch {
    // Discord nicht offen / ID ungültig — später erneut versuchen
    client = null
    ready = false
  } finally {
    connecting = false
  }
}

/**
 * Startet die automatische Verbindung + Retry-Schleife. Verbindet sich
 * von selbst, sobald Discord läuft (auch wenn es erst nach dem Launcher
 * gestartet wird) — der User muss nichts tun.
 */
export function initDiscord(): void {
  tryConnect()
  if (!retryTimer) {
    retryTimer = setInterval(() => {
      if (!client && !connecting) tryConnect()
    }, 20000)
  }
}

export function stopDiscord(): void {
  if (retryTimer) {
    clearInterval(retryTimer)
    retryTimer = null
  }
  try {
    client?.destroy()
  } catch {
    /* egal */
  }
  client = null
  ready = false
}

/** Nach Einstellungsänderung neu verbinden/trennen. */
export async function refreshDiscord(): Promise<void> {
  stopDiscord()
  startedAt = Date.now()
  initDiscord()
}

export function setDiscordIdle(): void {
  current = { playing: false }
  push()
}

export function setDiscordPlaying(version: string, profile: string): void {
  current = { playing: true, version, profile }
  push()
}

function push(): void {
  if (!client || !ready) return
  try {
    if (current.playing) {
      client.user?.setActivity({
        details: `Spielt Minecraft ${current.version ?? ''}`.trim(),
        state: current.profile ? `Profil: ${current.profile}` : undefined,
        largeImageKey: 'logo',
        largeImageText: 'Visual Client',
        startTimestamp: startedAt
      })
    } else {
      client.user?.setActivity({
        details: 'Im Launcher',
        state: 'Bereit zum Spielen',
        largeImageKey: 'logo',
        largeImageText: 'Visual Client',
        startTimestamp: startedAt
      })
    }
  } catch {
    /* Verbindung weg — ignorieren */
  }
}
