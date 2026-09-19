import fs from 'fs'
import path from 'path'

/**
 * Beschädigte Bibliotheken vor dem Start aussortieren.
 *
 * Hintergrund: minecraft-launcher-core lädt eine Bibliothek nur dann neu,
 * wenn die Datei GANZ fehlt. Die Prüfsummenkontrolle daneben ist wirkungslos —
 * dort steht `if (!this.checkSum(...))`, aber checkSum liefert ein Promise,
 * und `!Promise` ist immer falsch. Ein abgebrochener Download hinterlässt
 * also eine halbe Jar, die bei jedem weiteren Start unverändert wiederverwendet
 * wird. Fabric bricht dann mit „zip END header not found“ ab.
 *
 * Statt in fremdem Code herumzuflicken, werden solche Dateien hier gelöscht —
 * danach greift die vorhandene „fehlt ganz“-Regel und lädt sie sauber nach.
 */

/** Signatur des zentralen Verzeichnisses am Ende jeder Zip-Datei. */
const ENDE_SIGNATUR = Buffer.from([0x50, 0x4b, 0x05, 0x06])

/**
 * Prüft, ob eine Datei ein vollständiges Zip ist — genau das, woran Java
 * scheitert. Gelesen wird nur das Dateiende, nicht die ganze Datei: das
 * Endverzeichnis steht hinter höchstens 64 KB Kommentar.
 */
export function istVollstaendigesZip(datei: string): boolean {
  let fd: number | undefined
  try {
    const groesse = fs.statSync(datei).size
    if (groesse < 22) return false // kleiner als das Endverzeichnis selbst
    const laenge = Math.min(groesse, 65557)
    const puffer = Buffer.alloc(laenge)
    fd = fs.openSync(datei, 'r')
    fs.readSync(fd, puffer, 0, laenge, groesse - laenge)
    return puffer.lastIndexOf(ENDE_SIGNATUR) !== -1
  } catch {
    return false
  } finally {
    if (fd !== undefined) {
      try {
        fs.closeSync(fd)
      } catch {
        /* egal */
      }
    }
  }
}

/** Alle .jar-Dateien unterhalb der Ordner prüfen und kaputte entfernen. */
export function entferneKaputteJars(ordner: string[]): string[] {
  const entfernt: string[] = []

  const gehe = (dir: string): void => {
    let eintraege: fs.Dirent[]
    try {
      eintraege = fs.readdirSync(dir, { withFileTypes: true })
    } catch {
      return
    }
    for (const e of eintraege) {
      const p = path.join(dir, e.name)
      if (e.isDirectory()) {
        gehe(p)
      } else if (e.name.toLowerCase().endsWith('.jar') && !istVollstaendigesZip(p)) {
        try {
          fs.rmSync(p, { force: true })
          entfernt.push(e.name)
        } catch {
          /* gesperrt — nächster Start versucht es erneut */
        }
      }
    }
  }

  for (const o of ordner) if (fs.existsSync(o)) gehe(o)
  return entfernt
}
