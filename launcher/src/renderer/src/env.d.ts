/// <reference types="vite/client" />

export interface Account {
  id: string
  type: 'microsoft' | 'offline'
  name: string
  uuid?: string
}

export interface InstalledMod {
  projectId: string
  versionId: string
  fileName: string
  name: string
  iconUrl?: string
  kind?: string
}

export type ProjectType = 'mod' | 'resourcepack' | 'shader'

export interface InstalledItem {
  name: string
  fileName: string
  kind: ProjectType
  projectId?: string
  iconUrl?: string
  enabled: boolean
}

export interface CosmeticFile {
  name: string
  dataUrl: string
}

export type Loader = 'forge' | 'fabric' | 'neoforge' | 'quilt' | 'vanilla'

export interface Profile {
  id: string
  name: string
  mcVersion: string
  loader: Loader
  forgeVersion?: string
  ramGb: number
  ramMb?: number
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

export interface SearchOpts {
  query: string
  mcVersion: string
  loader: string
  projectType?: ProjectType
  offset?: number
  sort?: 'relevance' | 'downloads' | 'newest' | 'follows'
  category?: string
}

export interface SearchHit {
  project_id: string
  title: string
  description: string
  icon_url: string | null
  downloads: number
  author: string
  categories: string[]
}

export interface UpdateInfo {
  projectId: string
  name: string
  currentVersionId: string
  latestVersionId: string
  latestName: string
}

declare global {
  interface Window {
    visual: {
      win: { minimize(): void; maximize(): void; close(): void }
      auth: {
        get(): Promise<Account | null>
        list(): Promise<Account[]>
        switch(id: string): Promise<Account | null>
        remove(id: string): Promise<void>
        microsoft(): Promise<Account>
        offline(name: string): Promise<Account>
      }
      profiles: {
        list(): Promise<Profile[]>
        save(p: Partial<Profile>): Promise<Profile>
        remove(id: string): Promise<void>
        duplicate(id: string): Promise<Profile | null>
        openFolder(id: string): Promise<void>
        exportZip(id: string): Promise<boolean>
        mcVersions(): Promise<string[]>
        systemRamMb(): Promise<number>
      }
      mods: {
        search(opts: SearchOpts): Promise<{ hits: SearchHit[]; total_hits: number }>
        install(profileId: string, projectId: string, kind?: ProjectType): Promise<InstalledMod>
        uninstall(profileId: string, projectId: string): Promise<void>
        updates(profileId: string): Promise<UpdateInfo[]>
        importMrpack(profileId: string): Promise<number>
        installed(profileId: string): Promise<InstalledItem[]>
        removeFile(profileId: string, kind: string, fileName: string): Promise<void>
        toggleFile(profileId: string, kind: string, fileName: string): Promise<boolean>
      }
      visuals: { install(profileId: string): Promise<{ pack: boolean; mod: boolean }> }
      performance: {
        apply(profileId: string): Promise<{
          jvm: boolean
          options: number
          modsInstalled: string[]
          heavyDisabled: string[]
        }>
      }
      skins: {
        list(): Promise<CosmeticFile[]>
        add(dialogTitel: string): Promise<CosmeticFile | null>
        remove(name: string): Promise<void>
        apply(name: string, variant: 'classic' | 'slim'): Promise<string>
        rename(oldName: string, newName: string): Promise<string>
        textures(): Promise<{ skin: string | null; cape: string | null; slim: boolean }>
      }
      openExternal(url: string): Promise<void>
      settings: {
        get(): Promise<{ discordEnabled: boolean }>
        save(patch: { discordEnabled?: boolean }): Promise<{ discordEnabled: boolean }>
      }
      appVersion(): Promise<string>
      update: {
        state(): Promise<{ available: boolean; version: string | null; current: string }>
        apply(): Promise<void>
        onAvailable(cb: (info: { version: string }) => void): () => void
      }
      capes: {
        list(): Promise<CosmeticFile[]>
        add(dialogTitel: string): Promise<CosmeticFile | null>
        savePreset(name: string, dataUrl: string): Promise<void>
        remove(name: string): Promise<void>
        rename(oldName: string, newName: string): Promise<string>
        setActive(name: string | null): Promise<void>
        active(): Promise<string | null>
      }
      launch: {
        start(profileId: string, lang: string): Promise<{
          ok: boolean
          errorKey?: string
          errorVars?: Record<string, string | number>
        }>
        instances(): Promise<number>
        onStatus(cb: (s: { key: string; vars?: Record<string, string | number>; progress: number }) => void): () => void
        onClosed(cb: (info: { code: number }) => void): () => void
        onInstances(cb: (info: { count: number }) => void): () => void
      }
    }
  }
}
