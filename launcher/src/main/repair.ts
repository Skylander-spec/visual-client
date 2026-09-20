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

/**
 * Entfernt doppelte Mods: dieselbe Mod-Kennung in mehreren Jars.
 *
 * Anlass war Sodium, das in zwei Fassungen gleichzeitig im Ordner lag
 * (0.8.11 und 0.8.14) und den Start mit einer Unvereinbarkeitsmeldung
 * abbrach. Passieren kann das, wenn ein Mod unter einem anderen Dateinamen
 * nachinstalliert wird oder als Abhaengigkeit eines anderen Mods mitkommt.
 *
 * Behalten wird jeweils die Datei mit der neueren Version, bei gleichem
 * Stand die zuletzt geaenderte. Zurueck kommen die geloeschten Dateinamen.
 */
export function entferneDoppelte(modsOrdner: string): string[] {
  const geloescht: string[] = []
  let dateien: string[] = []
  try {
    dateien = fs.readdirSync(modsOrdner).filter((f) => f.toLowerCase().endsWith('.jar'))
  } catch {
    return geloescht
  }

  // Kennung -> Kandidaten
  const nachId = new Map<string, { datei: string; version: string; zeit: number }[]>()
  for (const f of dateien) {
    const voll = path.join(modsOrdner, f)
    let id = ''
    let version = ''
    try {
      const roh = leseAusZip(voll, 'fabric.mod.json')
      if (!roh) continue
      const j = JSON.parse(roh)
      id = String(j.id ?? '')
      version = String(j.version ?? '')
    } catch {
      continue
    }
    if (!id) continue
    let zeit = 0
    try {
      zeit = fs.statSync(voll).mtimeMs
    } catch {
      /* egal */
    }
    const liste = nachId.get(id) ?? []
    liste.push({ datei: f, version, zeit })
    nachId.set(id, liste)
  }

  for (const [, liste] of nachId) {
    if (liste.length < 2) continue
    liste.sort((a, b) => {
      const v = vergleicheVersion(b.version, a.version)
      return v !== 0 ? v : b.zeit - a.zeit
    })
    for (const alt of liste.slice(1)) {
      try {
        fs.rmSync(path.join(modsOrdner, alt.datei), { force: true })
        geloescht.push(alt.datei)
      } catch {
        /* dann bleibt sie eben liegen */
      }
    }
  }
  return geloescht
}

/** Versionen der Form 1.2.3 vergleichen; alles Weitere landet dahinter. */
function vergleicheVersion(a: string, b: string): number {
  // Sechs Gruppen statt vier: bei voicechat lautet die Version
  // "1.21.11-2.6.22", die ersten vier Zahlen sind bei beiden Fassungen
  // gleich und der Vergleich waere auf die Dateizeit zurueckgefallen.
  const teile = (s: string): number[] =>
    (s.match(/\d+/g) ?? []).slice(0, 6).map((n) => parseInt(n, 10))
  const x = teile(a)
  const y = teile(b)
  for (let i = 0; i < Math.max(x.length, y.length); i++) {
    const d = (x[i] ?? 0) - (y[i] ?? 0)
    if (d !== 0) return d
  }
  return 0
}

/** Eine Datei aus einem Zip lesen, ohne eine Bibliothek dafuer zu brauchen. */
export function leseAusZip(zipDatei: string, name: string): string | null {
  let fd: number | null = null
  try {
    const groesse = fs.statSync(zipDatei).size
    fd = fs.openSync(zipDatei, 'r')
    // Zentrales Verzeichnis am Ende suchen
    const schwanzLaenge = Math.min(groesse, 66000)
    const schwanz = Buffer.alloc(schwanzLaenge)
    fs.readSync(fd, schwanz, 0, schwanzLaenge, groesse - schwanzLaenge)
    const eocd = schwanz.lastIndexOf(Buffer.from([0x50, 0x4b, 0x05, 0x06]))
    if (eocd < 0) return null
    const cdVersatz = schwanz.readUInt32LE(eocd + 16)
    const cdGroesse = schwanz.readUInt32LE(eocd + 12)
    const cd = Buffer.alloc(cdGroesse)
    fs.readSync(fd, cd, 0, cdGroesse, cdVersatz)

    let p = 0
    while (p + 46 <= cd.length) {
      if (cd.readUInt32LE(p) !== 0x02014b50) break
      const nLen = cd.readUInt16LE(p + 28)
      const eLen = cd.readUInt16LE(p + 30)
      const kLen = cd.readUInt16LE(p + 32)
      const lokal = cd.readUInt32LE(p + 42)
      const methode = cd.readUInt16LE(p + 10)
      const komprimiert = cd.readUInt32LE(p + 20)
      const eintrag = cd.toString('utf8', p + 46, p + 46 + nLen)
      if (eintrag === name) {
        const kopf = Buffer.alloc(30)
        fs.readSync(fd, kopf, 0, 30, lokal)
        const lnLen = kopf.readUInt16LE(26)
        const leLen = kopf.readUInt16LE(28)
        const daten = Buffer.alloc(komprimiert)
        fs.readSync(fd, daten, 0, komprimiert, lokal + 30 + lnLen + leLen)
        if (methode === 0) return daten.toString('utf8')
        return require('zlib').inflateRawSync(daten).toString('utf8')
      }
      p += 46 + nLen + eLen + kLen
    }
    return null
  } catch {
    return null
  } finally {
    if (fd !== null) {
      try {
        fs.closeSync(fd)
      } catch {
        /* egal */
      }
    }
  }
}
