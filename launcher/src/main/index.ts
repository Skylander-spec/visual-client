import { app, BrowserWindow, ipcMain, dialog, shell } from 'electron'
import path from 'path'
import { ensureDirs } from './paths'
import * as profiles from './profiles'
import * as modrinth from './modrinth'
import * as auth from './auth'
import * as launch from './launch'
import * as cosmetics from './cosmetics'
import { installVisuals } from './visuals'
import { applyPerformance } from './performance'
import { getSettings, saveSettings } from './settings'
import { checkForUpdate, checkGithub, updateState, applyUpdate, hasPending } from './updater'
import { toPayload } from './errors'
import {
  initDiscord,
  refreshDiscord,
  setDiscordIdle,
  setDiscordPlaying,
  stopDiscord
} from './discord'

let win: BrowserWindow | null = null

function createWindow(): void {
  win = new BrowserWindow({
    width: 1120,
    height: 720,
    minWidth: 940,
    minHeight: 620,
    frame: false,
    backgroundColor: '#030807',
    show: false,
    // Taskbar-Icon des laufenden Fensters (sonst Electron-Standard-Icon)
    icon: path.join(app.getAppPath(), 'build', 'visual.ico'),
    webPreferences: {
      preload: path.join(__dirname, '../preload/index.js'),
      contextIsolation: true,
      nodeIntegration: false,
      // Rechtschreibprüfung lädt Wörterbücher nach und bringt hier nichts,
      // die Entwicklerwerkzeuge braucht im fertigen Paket niemand.
      spellcheck: false,
      devTools: !app.isPackaged,
      backgroundThrottling: true
    }
  })
  win.once('ready-to-show', () => win?.show())
  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })

  if (process.env['ELECTRON_RENDERER_URL']) {
    win.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    win.loadFile(path.join(__dirname, '../renderer/index.html'))
  }
}

// AUMID nur setzen, wenn als gepackte "Visual Client.exe" gestartet.
// Im Dev-Modus (electron.exe) würde ein expliziter Wert das Taskbar-
// Grouping mit der Verknüpfung stören.
if (app.isPackaged) {
  app.setAppUserModelId('dev.visual.client')
}

// Nur EINE Launcher-Instanz zulassen. Sonst würden zwei Fenster gleichzeitig
// profiles.json/settings.json schreiben und sich gegenseitig überschreiben
// ("Einstellungen wechseln ständig"). Eine zweite Instanz beendet sich sofort
// und holt das bestehende Fenster in den Vordergrund.
const gotLock = app.isPackaged ? app.requestSingleInstanceLock() : true
if (!gotLock) {
  app.quit()
} else {
  app.on('second-instance', () => {
    // Zweiter Start (Doppelklick auf Verknüpfung) -> bestehendes Fenster
    // SICHTBAR nach vorne holen. Reines focus() reicht unter Windows nicht
    // (Vordergrund-Sperre) und ein verstecktes Fenster bliebe unsichtbar –
    // sonst wirkt es, als würde "nichts starten".
    if (win) {
      if (win.isMinimized()) win.restore()
      if (!win.isVisible()) win.show()
      win.setAlwaysOnTop(true)
      win.show()
      win.focus()
      win.setAlwaysOnTop(false)
    }
  })
}

app.whenReady().then(() => {
  if (!gotLock) return
  ensureDirs()
  registerIpc()
  createWindow()

  // Discord Rich Presence: Status an laufende Instanzen koppeln
  launch.setInstanceListener((count, last) => {
    if (count > 0 && last) setDiscordPlaying(last.version, last.profile)
    else setDiscordIdle()
  })
  initDiscord()

  // Erst der lokale Kanal (sofort), dann GitHub (Netz, darf dauern).
  // Die Abfrage laeuft nebenher — ohne Verbindung startet der Launcher
  // genauso schnell wie vorher.
  const melden = (version?: string): void => {
    if (!win) return
    if (win.webContents.isLoading()) {
      win.webContents.once('did-finish-load', () =>
        win?.webContents.send('update:available', { version })
      )
    } else {
      win.webContents.send('update:available', { version })
    }
  }
  const upd = checkForUpdate()
  if (upd.available) melden(upd.version)
  void checkGithub().then((r) => {
    if (r.available && r.version !== upd.version) melden(r.version)
  })
})

app.on('window-all-closed', () => {
  stopDiscord()
  // Ausstehendes Update beim Schließen einspielen (nächstes Öffnen = neu)
  if (hasPending()) applyUpdate(false)
  app.quit()
})

function registerIpc(): void {
  // Fenster-Steuerung (frameless Titlebar)
  ipcMain.on('win:minimize', () => win?.minimize())
  ipcMain.on('win:maximize', () => (win?.isMaximized() ? win.unmaximize() : win?.maximize()))
  ipcMain.on('win:close', () => win?.close())

  // Account
  ipcMain.handle('auth:get', () => auth.getActiveAccount())
  ipcMain.handle('auth:list', () => auth.listAccounts())
  ipcMain.handle('auth:switch', (_e, id: string) => auth.setActiveAccount(id))
  ipcMain.handle('auth:remove', (_e, id: string) => auth.removeAccount(id))
  ipcMain.handle('auth:microsoft', () => auth.loginMicrosoft())
  ipcMain.handle('auth:offline', (_e, name: string) => auth.loginOffline(name))

  // Profile
  ipcMain.handle('profiles:list', () => profiles.listProfiles())
  ipcMain.handle('profiles:save', (_e, p) => profiles.saveProfile(p))
  ipcMain.handle('profiles:delete', (_e, id: string) => profiles.deleteProfile(id))
  ipcMain.handle('profiles:duplicate', (_e, id: string) => profiles.duplicateProfile(id))
  ipcMain.handle('profiles:openFolder', (_e, id: string) => {
    const { instanceDir } = require('./paths') as typeof import('./paths')
    return shell.openPath(instanceDir(id))
  })
  ipcMain.handle('profiles:export', async (_e, id: string) => {
    const profile = profiles.getProfile(id)
    if (!profile) return false
    const res = await dialog.showSaveDialog(win!, {
      title: 'Profil exportieren',
      defaultPath: `${profile.name.replace(/[^\w\- ]/g, '')}.zip`,
      filters: [{ name: 'ZIP-Archiv', extensions: ['zip'] }]
    })
    if (res.canceled || !res.filePath) return false
    const AdmZip = (require('adm-zip') as typeof import('adm-zip'))
    const { instanceDir } = require('./paths') as typeof import('./paths')
    const zip = new AdmZip()
    zip.addLocalFolder(instanceDir(id))
    zip.writeZip(res.filePath)
    return true
  })
  ipcMain.handle('mc:versions', () => launch.listMcVersions())

  // Version + Selbst-Update
  ipcMain.handle('app:version', () => app.getVersion())
  ipcMain.handle('update:state', () => updateState())
  ipcMain.handle('update:apply', () => {
    applyUpdate(true)
    setTimeout(() => app.quit(), 300)
  })

  // App-Einstellungen (Discord Rich Presence)
  ipcMain.handle('settings:get', () => getSettings())
  ipcMain.handle('settings:save', async (_e, patch) => {
    const s = saveSettings(patch)
    await refreshDiscord()
    return s
  })
  ipcMain.handle('sys:ramMb', () => {
    const os = require('os') as typeof import('os')
    return Math.floor(os.totalmem() / 1024 / 1024)
  })

  // Modrinth
  ipcMain.handle('mods:search', (_e, opts: modrinth.SearchOpts) => modrinth.searchMods(opts))
  ipcMain.handle('mods:install', (_e, profileId: string, projectId: string, kind?: string) =>
    modrinth.installMod(profileId, projectId, (kind as modrinth.ProjectType) ?? 'mod')
  )
  ipcMain.handle('mods:uninstall', (_e, profileId: string, projectId: string) =>
    modrinth.uninstallMod(profileId, projectId)
  )
  ipcMain.handle('mods:updates', (_e, profileId: string) => modrinth.checkUpdates(profileId))
  ipcMain.handle('mods:installed', (_e, profileId: string) =>
    modrinth.listInstalledContent(profileId)
  )
  ipcMain.handle('mods:removeFile', (_e, profileId: string, kind: string, fileName: string) =>
    modrinth.removeContentFile(profileId, kind as modrinth.ProjectType, fileName)
  )
  ipcMain.handle('mods:toggleFile', (_e, profileId: string, kind: string, fileName: string) =>
    modrinth.toggleContentFile(profileId, kind as modrinth.ProjectType, fileName)
  )
  ipcMain.handle('mods:importMrpack', async (_e, profileId: string) => {
    const res = await dialog.showOpenDialog(win!, {
      title: 'Modrinth-Modpack importieren',
      filters: [{ name: 'Modrinth Modpack', extensions: ['mrpack'] }],
      properties: ['openFile']
    })
    if (res.canceled || !res.filePaths[0]) return 0
    return modrinth.importMrpack(profileId, res.filePaths[0])
  })

  // Skins & Capes
  ipcMain.handle('skins:list', () => cosmetics.listSkins())
  ipcMain.handle('skins:add', (_e, titel: string) => cosmetics.addSkin(win!, titel))
  ipcMain.handle('skins:remove', (_e, name: string) => cosmetics.removeSkin(name))
  ipcMain.handle('skins:apply', (_e, name: string, variant: 'classic' | 'slim') =>
    cosmetics.applySkin(name, variant)
  )
  ipcMain.handle('skins:rename', (_e, oldName: string, newName: string) =>
    cosmetics.renameSkin(oldName, newName)
  )
  ipcMain.handle('capes:list', () => cosmetics.listCapes())
  ipcMain.handle('capes:add', (_e, titel: string) => cosmetics.addCape(win!, titel))
  ipcMain.handle('capes:savePreset', (_e, name: string, dataUrl: string) =>
    cosmetics.saveCapePreset(name, dataUrl)
  )
  ipcMain.handle('capes:remove', (_e, name: string) => cosmetics.removeCape(name))
  ipcMain.handle('capes:rename', (_e, oldName: string, newName: string) =>
    cosmetics.renameCape(oldName, newName)
  )
  ipcMain.handle('sys:openExternal', (_e, url: string) => {
    if (/^https:\/\//.test(url)) shell.openExternal(url)
  })
  ipcMain.handle('capes:setActive', (_e, name: string | null) => cosmetics.setActiveCape(name))
  ipcMain.handle('capes:active', () => cosmetics.activeCape())
  ipcMain.handle('skins:textures', () => {
    const acc = auth.getActiveAccount()
    return cosmetics.profileTextures(acc?.type === 'microsoft' ? acc.uuid : undefined)
  })

  // Visuals + Performance + Start
  ipcMain.handle('visuals:install', (_e, profileId: string) => installVisuals(profileId))
  ipcMain.handle('perf:apply', (_e, profileId: string) => applyPerformance(profileId))
  ipcMain.handle('launch:start', (_e, profileId: string, lang?: string) =>
    launchProfileSafe(profileId, lang)
  )
  ipcMain.handle('launch:instances', () => launch.runningInstances())
}

async function launchProfileSafe(profileId: string, lang?: string): Promise<{
  ok: boolean
  errorKey?: string
  errorVars?: Record<string, string | number>
}> {
  try {
    await launch.launchProfile(win!, profileId, lang)
    return { ok: true }
  } catch (err) {
    // Nur Schlüssel + Werte über die Grenze — übersetzt wird im Fenster.
    return { ok: false, ...toPayload(err) }
  }
}
