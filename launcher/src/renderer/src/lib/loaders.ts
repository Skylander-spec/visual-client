import type { Loader } from '../env'

export const LOADERS: { id: Loader; label: string; className: string; mods: boolean }[] = [
  { id: 'forge', label: 'Forge', className: 'lb-forge', mods: true },
  { id: 'fabric', label: 'Fabric', className: 'lb-fabric', mods: true },
  { id: 'neoforge', label: 'NeoForge', className: 'lb-neoforge', mods: true },
  { id: 'quilt', label: 'Quilt', className: 'lb-quilt', mods: true },
  { id: 'vanilla', label: 'Vanilla', className: 'lb-vanilla', mods: false }
]

export function loaderInfo(id: Loader | undefined) {
  return LOADERS.find((l) => l.id === (id ?? 'forge')) ?? LOADERS[0]
}

export function avatarUrl(name: string, size = 64): string {
  return `https://mc-heads.net/avatar/${encodeURIComponent(name)}/${size}`
}
