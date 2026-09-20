import fs from 'fs'
import path from 'path'
import { leseAusZip } from './repair'
import { installMod } from './modrinth'

/**
 * Löst Mod-Konflikte auf, bevor das Spiel sie als Absturz meldet.
 *
 * Fabric-Mods schreiben ihre Unverträglichkeiten selbst in die
 * fabric.mod.json: `breaks` nennt Mods, mit denen sie nicht zusammen
 * laufen, `depends` die, die sie brauchen. In einem Profil mit 180 Mods
 * sind das 45 beziehungsweise 170 Einträge — genug, dass es von Hand
 * niemand durchschaut.
 *
 * Der Ablauf ist bewusst vorsichtig: zuerst wird versucht, die passende
 * Fassung nachzuladen (Sodium verträgt sich mit neuem Iris, nur nicht mit
 * altem). Geht das nicht, wird einer der beiden abgeschaltet statt
 * gelöscht — umbenannt auf `.disabled`, damit nichts verloren geht und man
 * es rückgängig machen kann.
 */

export interface ModInfo {
  datei: string
  id: string
  version: string
  depends: Record<string, unknown>
  breaks: Record<string, unknown>
  /** Kennungen aus `provides` und Pfade eingebetteter Jars. */
  liefert: string[]
}

/** Alle Mods eines Ordners mit ihren Angaben einlesen. */
export function lesen(modsOrdner: string): ModInfo[] {
  let dateien: string[] = []
  try {
    dateien = fs.readdirSync(modsOrdner).filter((f) => f.toLowerCase().endsWith('.jar'))
  } catch {
    return []
  }
  const out: ModInfo[] = []
  for (const f of dateien) {
    const roh = leseAusZip(path.join(modsOrdner, f), 'fabric.mod.json')
    if (!roh) continue
    try {
      const j = JSON.parse(roh)
      if (!j.id) continue
      out.push({
        datei: f,
        id: String(j.id),
        version: String(j.version ?? '0'),
        depends: j.depends ?? {},
        breaks: { ...(j.breaks ?? {}), ...(j.conflicts ?? {}) },
        // Was dieser Mod selbst mitbringt: eigene Kennungen aus `provides`
        // und die Dateinamen eingebetteter Jars. Ohne das haelt man zehn
        // Abhaengigkeiten fuer fehlend, die laengst im Profil stecken - und
        // installiert sie ein zweites Mal daneben.
        liefert: [
          ...(Array.isArray(j.provides) ? j.provides.map(String) : []),
          ...(Array.isArray(j.jars)
            ? j.jars.map((e: { file?: string }) => String(e?.file ?? ''))
            : [])
        ]
      })
    } catch {
      // Unlesbare Angaben - der Mod wird beim Loesen einfach nicht beachtet
    }
  }
  return out
}

/** Versionsangabe in Zahlen zerlegen, Vorabkennungen bleiben aussen vor. */
function zahlen(v: string): number[] {
  const kern = v.split(/[+\-]/)[0]
  return (kern.match(/\d+/g) ?? []).slice(0, 4).map((n) => parseInt(n, 10))
}

function vergleiche(a: string, b: string): number {
  const x = zahlen(a)
  const y = zahlen(b)
  for (let i = 0; i < Math.max(x.length, y.length); i++) {
    const d = (x[i] ?? 0) - (y[i] ?? 0)
    if (d !== 0) return d
  }
  return 0
}

/**
 * Trifft eine Version auf einen Fabric-Ausdruck zu?
 *
 * Fabric erlaubt `*`, `>=1.2`, `<1.3`, `~1.2`, `^1.2`, genaue Angaben und
 * Listen davon (die als Oder gelten). Unbekannte Formen gelten bewusst als
 * "trifft zu": lieber einen Konflikt melden, der keiner ist, als einen
 * echten zu uebersehen - gemeldet wird ohnehin erst, wenn beide Mods da sind.
 */
export function trifftZu(version: string, ausdruck: unknown): boolean {
  if (Array.isArray(ausdruck)) return ausdruck.some((a) => trifftZu(version, a))
  if (typeof ausdruck !== 'string') return true
  const a = ausdruck.trim()
  if (a === '*' || a === '') return true
  const m = a.match(/^(>=|<=|>|<|\^|~|=)?\s*(.+)$/)
  if (!m) return true
  const [, op, ziel] = m
  const c = vergleiche(version, ziel)
  switch (op) {
    case '>=':
      return c >= 0
    case '<=':
      return c <= 0
    case '>':
      return c > 0
    case '<':
      return c < 0
    case '~':
    case '^':
      return c >= 0
    default:
      return c === 0
  }
}

export interface Konflikt {
  /** Der Mod, der die Unvertraeglichkeit erklaert. */
  melder: ModInfo
  /** Der Mod, mit dem er nicht laufen kann. */
  gegner: ModInfo
  /** Der Ausdruck, auf den es zutrifft. */
  ausdruck: unknown
}

/** Alle echten Konflikte finden: beide Mods da und die Version passt. */
export function finden(mods: ModInfo[]): Konflikt[] {
  const nachId = new Map(mods.map((m) => [m.id, m]))
  const out: Konflikt[] = []
  for (const m of mods) {
    for (const [id, ausdruck] of Object.entries(m.breaks)) {
      const gegner = nachId.get(id)
      if (!gegner) continue
      if (trifftZu(gegner.version, ausdruck)) out.push({ melder: m, gegner, ausdruck })
    }
  }
  return out
}

/** Fehlende Abhaengigkeiten, ohne die Fabric gar nicht erst startet. */
export function fehlendeAbhaengigkeiten(mods: ModInfo[]): string[] {
  // Diese stellt die Umgebung, nicht ein Mod
  const gestellt = new Set([
    'minecraft',
    'java',
    'fabricloader',
    'fabric',
    'fabric-api',
    'mixinextras'
  ])
  const da = new Set(mods.map((m) => m.id))
  // Eingebettete Jars und `provides` zaehlen als vorhanden
  const mitgebracht = mods.flatMap((m) => m.liefert).join(' ').toLowerCase()
  const stecktDrin = (id: string): boolean =>
    mitgebracht.includes(id.toLowerCase().replace(/[_-]/g, '')) ||
    mitgebracht.includes(id.toLowerCase())

  const fehlt = new Set<string>()
  for (const m of mods) {
    for (const id of Object.keys(m.depends)) {
      if (gestellt.has(id) || da.has(id) || id.startsWith('fabric-')) continue
      if (stecktDrin(id)) continue
      fehlt.add(id)
    }
  }
  return [...fehlt]
}

/** Wie viele andere Mods haengen von diesem ab? Entscheidet, wer bleibt. */
function gewicht(id: string, mods: ModInfo[]): number {
  return mods.filter((m) => Object.prototype.hasOwnProperty.call(m.depends, id)).length
}

export interface Ergebnis {
  abgeschaltet: string[]
  nachinstalliert: string[]
  ungeloest: string[]
}

/**
 * Konflikte aufloesen. Erst wird versucht, den Gegner auf eine Fassung zu
 * heben, die nicht mehr unter den Ausdruck faellt; klappt das nicht, wird
 * der weniger gebrauchte der beiden abgeschaltet.
 */
export async function loesen(
  profileId: string,
  modsOrdner: string,
  melden?: (text: string) => void
): Promise<Ergebnis> {
  const erg: Ergebnis = { abgeschaltet: [], nachinstalliert: [], ungeloest: [] }
  let mods = lesen(modsOrdner)

  // 1. Fehlende Abhaengigkeiten nachladen
  for (const id of fehlendeAbhaengigkeiten(mods)) {
    melden?.(id)
    try {
      await installMod(profileId, id, 'mod')
      erg.nachinstalliert.push(id)
    } catch {
      // Kennt Modrinth nicht unter diesem Namen - dann bleibt es offen
    }
  }
  mods = lesen(modsOrdner)

  // 2. Konflikte
  for (const k of finden(mods)) {
    melden?.(`${k.melder.id} / ${k.gegner.id}`)
    // Erst die neuere Fassung des Gegners versuchen
    let geloest = false
    try {
      await installMod(profileId, k.gegner.id, 'mod')
      const neu = lesen(modsOrdner).find((m) => m.id === k.gegner.id)
      if (neu && !trifftZu(neu.version, k.ausdruck)) {
        erg.nachinstalliert.push(k.gegner.id)
        geloest = true
      }
    } catch {
      // Kein Treffer bei Modrinth - dann abschalten
    }
    if (geloest) continue

    // Sonst den abschalten, an dem weniger haengt
    const wMelder = gewicht(k.melder.id, mods)
    const wGegner = gewicht(k.gegner.id, mods)
    const opfer = wMelder <= wGegner ? k.melder : k.gegner
    try {
      fs.renameSync(
        path.join(modsOrdner, opfer.datei),
        path.join(modsOrdner, opfer.datei + '.disabled')
      )
      erg.abgeschaltet.push(opfer.datei)
    } catch {
      erg.ungeloest.push(`${k.melder.id} / ${k.gegner.id}`)
    }
  }
  return erg
}
