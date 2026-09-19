import type { Profile } from './env'

/**
 * Browser-Preview-Mock: läuft die Renderer-Seite außerhalb von Electron
 * (z. B. im Browser über den Vite-Devserver), wird window.visual hiermit
 * ersetzt. Profile liegen im localStorage, die Modrinth-Suche geht direkt
 * über fetch (API erlaubt CORS). Spielstart ist deaktiviert.
 */
export function installMockApi(): void {
  if (window.visual) return

  const store = {
    read(): Profile[] {
      try {
        return JSON.parse(localStorage.getItem('mock-profiles') ?? '[]')
      } catch {
        return []
      }
    },
    write(p: Profile[]) {
      localStorage.setItem('mock-profiles', JSON.stringify(p))
    }
  }

  window.visual = {
    win: { minimize() {}, maximize() {}, close() {} },
    auth: {
      get: async () => {
        const list = JSON.parse(localStorage.getItem('mock-accounts') ?? '[]')
        const active = localStorage.getItem('mock-active')
        return list.find((a: { id: string }) => a.id === active) ?? list[0] ?? null
      },
      list: async () => JSON.parse(localStorage.getItem('mock-accounts') ?? '[]'),
      switch: async (id: string) => {
        localStorage.setItem('mock-active', id)
        const list = JSON.parse(localStorage.getItem('mock-accounts') ?? '[]')
        return list.find((a: { id: string }) => a.id === id) ?? null
      },
      remove: async (id: string) => {
        const list = JSON.parse(localStorage.getItem('mock-accounts') ?? '[]').filter(
          (a: { id: string }) => a.id !== id
        )
        localStorage.setItem('mock-accounts', JSON.stringify(list))
      },
      microsoft: async () => {
        throw new Error('Nur in der Desktop-App verfügbar')
      },
      offline: async (name: string) => {
        const list = JSON.parse(localStorage.getItem('mock-accounts') ?? '[]')
        const account = { id: Math.random().toString(36).slice(2, 8), type: 'offline' as const, name }
        list.push(account)
        localStorage.setItem('mock-accounts', JSON.stringify(list))
        localStorage.setItem('mock-active', account.id)
        return account
      }
    },
    profiles: {
      list: async () => store.read(),
      save: async (p) => {
        const all = store.read()
        const existing = p.id ? all.find((x) => x.id === p.id) : undefined
        if (existing) {
          Object.assign(existing, p)
          store.write(all)
          return existing
        }
        const { id: _ignored, ...rest } = p as Profile
        const created: Profile = {
          ...rest,
          id: Math.random().toString(36).slice(2, 10),
          loader: rest.loader ?? 'forge',
          ramGb: (rest.ramGb as number) ?? 4,
          mods: rest.mods ?? [],
          createdAt: new Date().toISOString()
        }
        all.push(created)
        store.write(all)
        return created
      },
      remove: async (id) => store.write(store.read().filter((p) => p.id !== id)),
      duplicate: async (id) => {
        const all = store.read()
        const src = all.find((p) => p.id === id)
        if (!src) return null
        const copy = { ...src, id: Math.random().toString(36).slice(2, 10), name: src.name + ' (Kopie)' }
        all.push(copy)
        store.write(all)
        return copy
      },
      openFolder: async () => {},
      exportZip: async () => false,
      systemRamMb: async () => 16384,
      mcVersions: async () => {
        const res = await fetch('https://launchermeta.mojang.com/mc/game/version_manifest_v2.json')
        const data = await res.json()
        return data.versions
          .filter((v: { type: string }) => v.type === 'release')
          .map((v: { id: string }) => v.id)
      }
    },
    mods: {
      search: async (opts) => {
        const type = opts.projectType ?? 'mod'
        const facetList = [[`versions:${opts.mcVersion}`], [`project_type:${type}`]]
        if (type === 'mod') facetList.unshift([`categories:${opts.loader}`])
        if (opts.category) facetList.push([`categories:${opts.category}`])
        const facets = encodeURIComponent(JSON.stringify(facetList))
        const index = opts.sort ?? (opts.query ? 'relevance' : 'downloads')
        const res = await fetch(
          `https://api.modrinth.com/v2/search?query=${encodeURIComponent(opts.query)}&facets=${facets}&limit=20&offset=${opts.offset ?? 0}&index=${index}`
        )
        return res.json()
      },
      install: async () => {
        throw new Error('Installation nur in der Desktop-App')
      },
      uninstall: async () => {},
      updates: async () => [],
      importMrpack: async () => 0,
      installed: async () => [],
      removeFile: async () => {},
      toggleFile: async () => true
    },
    visuals: { install: async () => ({ pack: false, mod: false }) },
    performance: {
      apply: async () => ({ jvm: true, options: 14, modsInstalled: [], heavyDisabled: [] })
    },
    skins: {
      list: async () => [],
      add: async () => null,
      remove: async () => {},
      apply: async () => {
        throw new Error('Nur in der Desktop-App verfügbar')
      },
      rename: async (_o: string, n: string) => n,
      textures: async () => ({ skin: null, cape: null, slim: false })
    },
    openExternal: async (url: string) => {
      window.open(url, '_blank')
    },
    settings: {
      get: async () => ({ discordEnabled: localStorage.getItem('mock-discord') !== '0' }),
      save: async (patch: { discordEnabled?: boolean }) => {
        if (patch.discordEnabled !== undefined)
          localStorage.setItem('mock-discord', patch.discordEnabled ? '1' : '0')
        return { discordEnabled: localStorage.getItem('mock-discord') !== '0' }
      }
    },
    appVersion: async () => '1.4.0-dev',
    update: {
      state: async () => ({ available: false, version: null, current: '1.4.0-dev' }),
      apply: async () => {},
      onAvailable: () => () => {}
    },
    capes: {
      list: async () => JSON.parse(localStorage.getItem('mock-capes') ?? '[]'),
      add: async () => null,
      savePreset: async (name: string, dataUrl: string) => {
        const capes = JSON.parse(localStorage.getItem('mock-capes') ?? '[]')
        if (!capes.some((c: { name: string }) => c.name === name)) capes.push({ name, dataUrl })
        localStorage.setItem('mock-capes', JSON.stringify(capes))
      },
      remove: async (name: string) => {
        const capes = JSON.parse(localStorage.getItem('mock-capes') ?? '[]')
        localStorage.setItem(
          'mock-capes',
          JSON.stringify(capes.filter((c: { name: string }) => c.name !== name))
        )
      },
      rename: async (_o: string, n: string) => n,
      setActive: async (name: string | null) => {
        if (name) localStorage.setItem('mock-cape-active', name)
        else localStorage.removeItem('mock-cape-active')
      },
      active: async () => localStorage.getItem('mock-cape-active')
    },
    launch: {
      start: async () => ({ ok: false, error: 'Spielstart nur in der Desktop-App (npm run dev)' }),
      instances: async () => 0,
      onStatus: () => () => {},
      onClosed: () => () => {},
      onInstances: () => () => {}
    }
  }
}
