import { contextBridge, ipcRenderer } from 'electron'

const api = {
  win: {
    minimize: () => ipcRenderer.send('win:minimize'),
    maximize: () => ipcRenderer.send('win:maximize'),
    close: () => ipcRenderer.send('win:close')
  },
  auth: {
    get: () => ipcRenderer.invoke('auth:get'),
    list: () => ipcRenderer.invoke('auth:list'),
    switch: (id: string) => ipcRenderer.invoke('auth:switch', id),
    remove: (id: string) => ipcRenderer.invoke('auth:remove', id),
    microsoft: () => ipcRenderer.invoke('auth:microsoft'),
    offline: (name: string) => ipcRenderer.invoke('auth:offline', name)
  },
  profiles: {
    list: () => ipcRenderer.invoke('profiles:list'),
    save: (p: unknown) => ipcRenderer.invoke('profiles:save', p),
    remove: (id: string) => ipcRenderer.invoke('profiles:delete', id),
    duplicate: (id: string) => ipcRenderer.invoke('profiles:duplicate', id),
    openFolder: (id: string) => ipcRenderer.invoke('profiles:openFolder', id),
    openMods: (id: string) => ipcRenderer.invoke('profiles:openMods', id),
    exportZip: (id: string) => ipcRenderer.invoke('profiles:export', id),
    mcVersions: () => ipcRenderer.invoke('mc:versions'),
    systemRamMb: () => ipcRenderer.invoke('sys:ramMb')
  },
  mods: {
    search: (opts: {
      query: string
      mcVersion: string
      loader: string
      projectType?: string
      offset?: number
      sort?: string
      category?: string
    }) => ipcRenderer.invoke('mods:search', opts),
    install: (profileId: string, projectId: string, kind?: string) =>
      ipcRenderer.invoke('mods:install', profileId, projectId, kind),
    uninstall: (profileId: string, projectId: string) =>
      ipcRenderer.invoke('mods:uninstall', profileId, projectId),
    updates: (profileId: string) => ipcRenderer.invoke('mods:updates', profileId),
    importMrpack: (profileId: string) => ipcRenderer.invoke('mods:importMrpack', profileId),
    installed: (profileId: string) => ipcRenderer.invoke('mods:installed', profileId),
    removeFile: (profileId: string, kind: string, fileName: string) =>
      ipcRenderer.invoke('mods:removeFile', profileId, kind, fileName),
    toggleFile: (profileId: string, kind: string, fileName: string) =>
      ipcRenderer.invoke('mods:toggleFile', profileId, kind, fileName)
  },
  visuals: {
    install: (profileId: string) => ipcRenderer.invoke('visuals:install', profileId)
  },
  // Mod-Pruefung beim Start: blockierender Bildschirm, bis alles da ist
  modcheck: {
    onStart: (cb: (i: { profile: number; mods: number }) => void) => {
      const h = (_e: unknown, i: { profile: number; mods: number }): void => cb(i)
      ipcRenderer.on('modcheck:start', h)
      return () => ipcRenderer.removeListener('modcheck:start', h)
    },
    onStatus: (cb: (s: { text: string; fertig: number; gesamt: number; prozent: number }) => void) => {
      const h = (_e: unknown, s: { text: string; fertig: number; gesamt: number; prozent: number }): void => cb(s)
      ipcRenderer.on('modcheck:status', h)
      return () => ipcRenderer.removeListener('modcheck:status', h)
    },
    onDone: (cb: (r: { offen: number; namen: string[] }) => void) => {
      const h = (_e: unknown, r: { offen: number; namen: string[] }): void => cb(r)
      ipcRenderer.on('modcheck:done', h)
      return () => ipcRenderer.removeListener('modcheck:done', h)
    }
  },
  pack: {
    install: (profileId: string) => ipcRenderer.invoke('pack:install', profileId),
    onStatus: (cb: (s: unknown) => void) => {
      const h = (_e: unknown, s: unknown): void => cb(s)
      ipcRenderer.on('pack:status', h)
      return () => ipcRenderer.removeListener('pack:status', h)
    }
  },
  performance: {
    apply: (profileId: string) => ipcRenderer.invoke('perf:apply', profileId)
  },
  skins: {
    list: () => ipcRenderer.invoke('skins:list'),
    add: (titel: string) => ipcRenderer.invoke('skins:add', titel),
    remove: (name: string) => ipcRenderer.invoke('skins:remove', name),
    apply: (name: string, variant: string) => ipcRenderer.invoke('skins:apply', name, variant),
    rename: (oldName: string, newName: string) =>
      ipcRenderer.invoke('skins:rename', oldName, newName),
    textures: () => ipcRenderer.invoke('skins:textures')
  },
  openExternal: (url: string) => ipcRenderer.invoke('sys:openExternal', url),
  settings: {
    get: () => ipcRenderer.invoke('settings:get'),
    save: (patch: { discordEnabled?: boolean }) => ipcRenderer.invoke('settings:save', patch)
  },
  appVersion: () => ipcRenderer.invoke('app:version'),
  update: {
    state: () => ipcRenderer.invoke('update:state'),
    apply: () => ipcRenderer.invoke('update:apply'),
    onQuit: (erlauben: boolean) => ipcRenderer.invoke('update:onQuit', erlauben),
    check: () => ipcRenderer.invoke('update:check'),
    versions: () => ipcRenderer.invoke('update:versions'),
    pick: (version: string) => ipcRenderer.invoke('update:pick', version),
    onAvailable: (cb: (info: { version: string }) => void) => {
      const listener = (_e: unknown, info: { version: string }) => cb(info)
      ipcRenderer.on('update:available', listener)
      return () => ipcRenderer.removeListener('update:available', listener)
    }
  },
  capes: {
    list: () => ipcRenderer.invoke('capes:list'),
    add: (titel: string) => ipcRenderer.invoke('capes:add', titel),
    savePreset: (name: string, dataUrl: string) =>
      ipcRenderer.invoke('capes:savePreset', name, dataUrl),
    remove: (name: string) => ipcRenderer.invoke('capes:remove', name),
    rename: (oldName: string, newName: string) =>
      ipcRenderer.invoke('capes:rename', oldName, newName),
    setActive: (name: string | null) => ipcRenderer.invoke('capes:setActive', name),
    active: () => ipcRenderer.invoke('capes:active')
  },
  launch: {
    start: (profileId: string, lang: string) =>
      ipcRenderer.invoke('launch:start', profileId, lang),
    instances: () => ipcRenderer.invoke('launch:instances'),
    onStatus: (cb: (s: { key: string; vars?: Record<string, string | number>; progress: number }) => void) => {
      const listener = (_e: unknown, s: { key: string; vars?: Record<string, string | number>; progress: number }) => cb(s)
      ipcRenderer.on('launch:status', listener)
      return () => ipcRenderer.removeListener('launch:status', listener)
    },
    onClosed: (cb: (info: { code: number }) => void) => {
      const listener = (_e: unknown, info: { code: number }) => cb(info)
      ipcRenderer.on('launch:closed', listener)
      return () => ipcRenderer.removeListener('launch:closed', listener)
    },
    onInstances: (cb: (info: { count: number }) => void) => {
      const listener = (_e: unknown, info: { count: number }) => cb(info)
      ipcRenderer.on('launch:instances', listener)
      return () => ipcRenderer.removeListener('launch:instances', listener)
    }
  }
}

contextBridge.exposeInMainWorld('visual', api)

export type VisualApi = typeof api
