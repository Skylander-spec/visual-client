import fs from 'fs'
import path from 'path'
import { BrowserWindow } from 'electron'
import { instanceDir } from './paths'
import { listProfiles } from './profiles'
import { installMod } from './modrinth'
import { entferneDoppelte } from './repair'

/**
 * Prüft beim Start jedes Profil auf die Mods, die der Client selbst braucht.
 *
 * Es hat einen konkreten Anlass: OneConfig stand zwar im Mod-Paket, lag aber
 * in keinem einzigen Profil — und weil das Paket freiwillig ist, fiel das
 * monatelang nicht auf. Der Client sah dann anders aus als gedacht, ohne dass
 * irgendwo eine Meldung erschien. Eine stille Lücke dieser Art soll es nicht
 * mehr geben.
 */

/** Ein Mod, ohne den der Client nicht so läuft wie vorgesehen. */
interface Pflicht {
  /** Modrinth-Kennung zum Nachladen. */
  slug: string
  /** Muster, an dem eine bereits vorhandene Datei erkannt wird. */
  muster: RegExp
  /** Anzeigename für den Bildschirm. */
  name: string
}

const PFLICHT: Pflicht[] = [
  { slug: 'fabric-api', muster: /^fabric[-_]api/i, name: 'Fabric API' },
  { slug: 'oneconfig', muster: /oneconfig/i, name: 'OneConfig' }
]

export interface Fehlend {
  profilId: string
  profilName: string
  mcVersion: string
  mods: Pflicht[]
}

/** Nur Fabric und Quilt brauchen diese Mods. */
function betrifft(loader?: string): boolean {
  return loader === 'fabric' || loader === 'quilt'
}

/**
 * Für 26.x und neuer gibt es die Mods noch nicht — dort würde der Download
 * jedes Mal scheitern und der Bildschirm nie verschwinden.
 */
function unterstuetzt(mcVersion?: string): boolean {
  return !!mcVersion && /^1\.21(\.\d+)?$/.test(mcVersion)
}

/** Alle Profile durchsehen; leeres Ergebnis heißt: alles da. */
export function pruefen(): Fehlend[] {
  const offen: Fehlend[] = []
  for (const p of listProfiles()) {
    if (!betrifft(p.loader) || !unterstuetzt(p.mcVersion)) continue
    let dateien: string[] = []
    try {
      dateien = fs.readdirSync(path.join(instanceDir(p.id), 'mods'))
    } catch {
      // Profil noch nie gestartet — dann fehlt schlicht alles
    }
    // Doppelte gleich mit wegraeumen: zwei Fassungen desselben Mods lassen
    // das Spiel mit einer Unvereinbarkeitsmeldung gar nicht erst starten.
    try {
      const doppelt = entferneDoppelte(path.join(instanceDir(p.id), 'mods'))
      if (doppelt.length > 0) {
        console.log(`[visuals] ${p.name}: ${doppelt.length} doppelte Mods entfernt`)
        dateien = fs.readdirSync(path.join(instanceDir(p.id), 'mods'))
      }
    } catch {
      // Ordner nicht lesbar - dann bleibt es beim Pruefen
    }
    const jars = dateien.filter((f) => f.toLowerCase().endsWith('.jar'))
    const fehlen = PFLICHT.filter((m) => !jars.some((f) => m.muster.test(f)))
    if (fehlen.length > 0) {
      offen.push({ profilId: p.id, profilName: p.name, mcVersion: p.mcVersion, mods: fehlen })
    }
  }
  return offen
}

/**
 * Das Fehlende nachladen und den Fortschritt melden. Gibt zurück, was danach
 * noch fehlt — ohne Netz bleibt das stehen, und der Bildschirm sagt es.
 */
export async function nachholen(win: BrowserWindow | null, offen: Fehlend[]): Promise<Fehlend[]> {
  const gesamt = offen.reduce((n, e) => n + e.mods.length, 0)
  let fertig = 0
  const gescheitert: Fehlend[] = []

  const melden = (text: string): void => {
    if (win && !win.isDestroyed()) {
      win.webContents.send('modcheck:status', {
        text,
        fertig,
        gesamt,
        prozent: gesamt === 0 ? 100 : Math.round((fertig / gesamt) * 100)
      })
    }
  }

  for (const eintrag of offen) {
    const rest: Pflicht[] = []
    for (const mod of eintrag.mods) {
      melden(`${eintrag.profilName}: ${mod.name}`)
      try {
        await installMod(eintrag.profilId, mod.slug, 'mod')
      } catch {
        rest.push(mod)
      }
      fertig++
      melden(`${eintrag.profilName}: ${mod.name}`)
    }
    if (rest.length > 0) gescheitert.push({ ...eintrag, mods: rest })
  }
  return gescheitert
}
