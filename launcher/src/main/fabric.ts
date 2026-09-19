import fs from 'fs'
import path from 'path'
import { MC_ROOT } from './paths'
import { VError } from './errors'

/**
 * Fabric & Quilt ohne Installer aufsetzen: Beide Meta-APIs liefern das
 * fertige Versions-JSON, das nur nach versions/<id>/<id>.json geschrieben
 * werden muss — die Libraries lädt minecraft-launcher-core dann selbst.
 * Gibt die Custom-Version-ID zurück (z. B. "fabric-loader-0.16.9-1.20.1").
 */
async function ensureMetaLoader(
  mcVersion: string,
  displayName: string,
  metaBase: string
): Promise<string> {
  const res = await fetch(`${metaBase}/versions/loader/${mcVersion}`)
  const loaders = (await res.json()) as { loader: { version: string; stable?: boolean } }[]
  if (!Array.isArray(loaders) || !loaders.length) {
    throw new VError('error.loaderUnsupported', { loader: displayName, version: mcVersion })
  }
  const loaderVersion = (loaders.find((l) => l.loader.stable) ?? loaders[0]).loader.version

  const profileRes = await fetch(
    `${metaBase}/versions/loader/${mcVersion}/${loaderVersion}/profile/json`
  )
  const profile = (await profileRes.json()) as {
    id: string
    libraries?: { name: string; url?: string; natives?: Record<string, string> }[]
  }
  const id = profile.id

  stripNativesLibraries(profile)

  const jsonPath = path.join(MC_ROOT, 'versions', id, `${id}.json`)
  fs.mkdirSync(path.dirname(jsonPath), { recursive: true })
  // Immer neu schreiben: eine früher abgelegte Fassung kann noch die
  // fehlerhaften Natives-Einträge enthalten.
  fs.writeFileSync(jsonPath, JSON.stringify(profile, null, 2))
  return id
}

/**
 * Natives-Bibliotheken aus dem Loader-Profil entfernen.
 *
 * Legacy Fabric führt org.lwjgl.lwjgl:lwjgl-platform im Fabric-Format auf:
 * nur `name`, `url` und eine `natives`-Zuordnung. minecraft-launcher-core
 * kommt damit nicht zurecht — es entpackt Natives ausschließlich für
 * Einträge mit `downloads.classifiers` und lädt sonst „<name>.jar“, die es
 * für ein reines Natives-Paket gar nicht gibt. Das Ergebnis war eine
 * 404-HTML-Seite mit Jar-Endung, die im Klassenpfad landete; Fabric brach
 * dann beim Öffnen als Zip ab (ZipException).
 *
 * Ohne diesen Eintrag greift wieder Mojangs eigener lwjgl-platform-Eintrag
 * — der bringt vollständige `downloads.classifiers` mit und wird korrekt
 * geladen und entpackt. Die gepatchten Java-Bibliotheken (lwjgl, lwjgl_util)
 * bleiben erhalten, nur die Natives kommen von Mojang.
 */
function stripNativesLibraries(profile: {
  libraries?: { name: string; url?: string; natives?: Record<string, string> }[]
}): void {
  if (!Array.isArray(profile.libraries)) return
  const entfernt = profile.libraries.filter((l) => l.natives)
  profile.libraries = profile.libraries.filter((l) => !l.natives)
  for (const lib of entfernt) removeBogusJar(lib.name)
}

/** Eine früher fälschlich geladene Datei (in Wahrheit eine Fehlerseite) löschen. */
function removeBogusJar(name: string): void {
  const [group, artifact, version] = name.split(':')
  if (!group || !artifact || !version) return
  const file = path.join(
    MC_ROOT,
    'libraries',
    ...group.split('.'),
    artifact,
    version,
    `${artifact}-${version}.jar`
  )
  try {
    if (fs.existsSync(file)) fs.rmSync(file, { force: true })
  } catch {
    /* nicht kritisch — beim nächsten Start erneut versuchen */
  }
}

/**
 * Fabric selbst beginnt erst bei 1.14. Für die älteren Versionen — allen
 * voran 1.8.9, die PvP-Version — pflegt das Legacy-Fabric-Projekt eigene
 * Loader und Mappings. Die Meta-API hat dieselbe Form, nur eine andere
 * Adresse, deshalb genügt der Wechsel der Basis.
 */
function fabricMetaFor(mcVersion: string): string {
  const m = mcVersion.match(/^1\.(\d+)/)
  const minor = m ? Number(m[1]) : 99
  return minor < 14 ? 'https://meta.legacyfabric.net/v2' : 'https://meta.fabricmc.net/v2'
}

export function ensureFabric(mcVersion: string): Promise<string> {
  return ensureMetaLoader(mcVersion, 'Fabric', fabricMetaFor(mcVersion))
}

export function ensureQuilt(mcVersion: string): Promise<string> {
  return ensureMetaLoader(mcVersion, 'Quilt', 'https://meta.quiltmc.org/v3')
}
