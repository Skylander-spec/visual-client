import fs from 'fs'
import path from 'path'
import AdmZip from 'adm-zip'
import { app } from 'electron'
import { instanceDir } from './paths'
import { getProfile } from './profiles'

/**
 * Installiert das Visual Pack (Resource Pack) und — falls gebaut — die
 * VisualsMod-Jar ins Profil. Im Dev-Modus liegen beide im Monorepo
 * neben dem launcher/-Ordner.
 */
function repoRoot(): string {
  // dev: <repo>/launcher/out/main -> drei Ebenen hoch
  return path.resolve(app.getAppPath(), '..', '..')
}

export function installVisuals(profileId: string): { pack: boolean; mod: boolean } {
  const dir = instanceDir(profileId)
  const result = { pack: false, mod: false }

  const packDir = path.join(repoRoot(), 'resourcepack', 'pack')
  if (fs.existsSync(path.join(packDir, 'pack.mcmeta'))) {
    const zip = new AdmZip()
    zip.addLocalFolder(packDir)
    zip.writeZip(path.join(dir, 'resourcepacks', 'VisualPack.zip'))
    activateResourcePack(dir, 'file/VisualPack.zip')
    result.pack = true
  }

  // VisualsMod ist ein Forge-Mod — nur in Forge-Profile kopieren
  const profile = getProfile(profileId)
  if ((profile?.loader ?? 'forge') === 'forge') {
    const libsDir = path.join(repoRoot(), 'mod', 'build', 'libs')
    if (fs.existsSync(libsDir)) {
      const jar = fs.readdirSync(libsDir).find((f) => f.endsWith('.jar'))
      if (jar) {
        fs.copyFileSync(path.join(libsDir, jar), path.join(dir, 'mods', jar))
        result.mod = true
      }
    }
  }
  return result
}

/**
 * Findet die gebündelte Branding-Mod-Jar (setzt Fenstertitel + V-Icon).
 * Gepackte App: <appPath>/build; Dev: <repo>/brandingmod/build/libs.
 */
function brandingJarPath(): string | null {
  const packed = path.join(app.getAppPath(), 'build', 'visual-branding.jar')
  if (fs.existsSync(packed)) return packed
  const libs = path.join(repoRoot(), 'brandingmod', 'build', 'libs')
  if (fs.existsSync(libs)) {
    const jar = fs.readdirSync(libs).find((f) => f.endsWith('.jar') && !f.endsWith('-sources.jar'))
    if (jar) return path.join(libs, jar)
  }
  return null
}

/**
 * Client-Mods (Branding: Fenstertitel + Icon, VisualsFabric: In-Game-Menü)
 * in Fabric/Quilt-Profile legen. Wird beim Start aufgerufen; Fehler sind
 * unkritisch.
 */
/**
 * VisualsFabric wird pro MC-Version gebaut (die APIs unterscheiden sich, z. B.
 * KeyBinding.Category erst ab 1.21.9). Liefert den Basisnamen der passenden
 * Jar oder null, wenn es für die Version keinen Build gibt.
 * Branding (nur GLFW) ist dagegen versionsunabhängig.
 */
function visualsModBase(mcVersion?: string): string | null {
  if (!mcVersion) return null
  const m = mcVersion.match(/^1\.(\d+)(?:\.(\d+))?$/)
  // Neues Versionsschema (26.x) hat andere Intermediary-Namen — dafür gibt es
  // (noch) keinen Build. Lieber kein Menü als ein Absturz beim Start.
  if (!m) return null
  const major = Number(m[1])
  const minor = Number(m[2] ?? 0)
  // 1.8.9 ist eine eigene Ausgabe gegen Legacy Fabric (Java 8, andere
  // Render-API) — sie liegt als eigene Jar bereit.
  const legacy = major === 8 && minor === 9
  if (!legacy && (major !== 21 || minor < 2)) return null
  // Mixins hängen an den Intermediary-Namen der jeweiligen Version, deshalb
  // gibt es pro Version eine eigene Jar und KEINE Rückfallebene: lieber kein
  // In-Game-Menü als ein Absturz beim Start.
  const exact = `visuals-fabric-${mcVersion}`
  return clientModJarPath(exact) ? exact : null
}

/**
 * Auch die Branding-Mod ist gegen 1.21.x gebaut: sie referenziert
 * MinecraftClient, dessen Intermediary-Name (class_310) im neuen Schema (26.x)
 * nicht mehr existiert — dort reißt sie das Spiel beim Start ab.
 */
function brandingSupported(mcVersion?: string): boolean {
  return !!mcVersion && /^1\.21(\.\d+)?$/.test(mcVersion)
}

/** Eine zuvor installierte Client-Jar wieder aus dem Profil nehmen. */
function removeJar(dir: string, name: string): void {
  try {
    fs.rmSync(path.join(dir, 'mods', name), { force: true })
  } catch {
    /* unkritisch */
  }
}

export function installBranding(profileId: string, loader: string, mcVersion?: string): void {
  if (loader !== 'fabric' && loader !== 'quilt') return
  const dir = instanceDir(profileId)
  const jars: [string | null, string][] = []
  if (brandingSupported(mcVersion)) {
    jars.push([brandingJarPath(), 'visual-branding.jar'])
  } else {
    removeJar(dir, 'visual-branding.jar')
  }
  const visuals = visualsModBase(mcVersion)
  if (visuals) {
    jars.push([clientModJarPath(visuals), 'visuals-fabric.jar'])
  } else {
    // Auf nicht unterstützten Versionen eine früher kopierte Jar wieder
    // entfernen, sonst crasht das Profil dauerhaft beim Start.
    removeJar(dir, 'visuals-fabric.jar')
  }
  for (const [src, name] of jars) {
    if (!src) continue
    try {
      fs.copyFileSync(src, path.join(dir, 'mods', name))
    } catch {
      /* unkritisch */
    }
  }
}

/** Gebündelte Client-Mod-Jar finden (gepackte App oder Dev-Monorepo). */
function clientModJarPath(baseName: string): string | null {
  const packed = path.join(app.getAppPath(), 'build', `${baseName}.jar`)
  if (fs.existsSync(packed)) return packed
  const libs = path.join(repoRoot(), 'fabricmod', 'build', 'libs')
  if (fs.existsSync(libs)) {
    const jar = fs
      .readdirSync(libs)
      .find((f) => f.startsWith(baseName) && f.endsWith('.jar') && !f.includes('sources'))
    if (jar) return path.join(libs, jar)
  }
  return null
}

/**
 * Pack in der options.txt der Instanz aktivieren, damit es beim nächsten
 * Start sofort an ist — kein manuelles Aktivieren im Spiel nötig.
 */
function activateResourcePack(instanceDirPath: string, packEntry: string): void {
  const optionsFile = path.join(instanceDirPath, 'options.txt')
  let lines: string[] = []
  if (fs.existsSync(optionsFile)) {
    lines = fs.readFileSync(optionsFile, 'utf8').split(/\r?\n/)
  }
  const idx = lines.findIndex((l) => l.startsWith('resourcePacks:'))
  let packs: string[] = ['vanilla']
  if (idx >= 0) {
    try {
      packs = JSON.parse(lines[idx].slice('resourcePacks:'.length))
    } catch {
      /* kaputte Zeile -> Standard */
    }
  }
  if (!packs.includes(packEntry)) packs.push(packEntry)
  const line = 'resourcePacks:' + JSON.stringify(packs)
  if (idx >= 0) lines[idx] = line
  else lines.push(line)
  fs.writeFileSync(optionsFile, lines.filter((l) => l !== '').join('\n') + '\n')
}
