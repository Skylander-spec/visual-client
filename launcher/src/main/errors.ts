/**
 * Fehler mit Übersetzungsschlüssel.
 *
 * Der Hauptprozess kennt die im Fenster gewählte Sprache nicht — ein fertig
 * formulierter Satz bliebe dort in jeder Sprache deutsch. Deshalb trägt ein
 * Fehler nur einen Schlüssel und seine Platzhalter; übersetzt wird erst im
 * Renderer, genau wie bei den Fortschrittsmeldungen.
 *
 * Die `message` bleibt trotzdem lesbar gefüllt (Schlüssel + Werte), damit
 * Logdateien und Abstürze ohne Renderer noch etwas aussagen.
 */
export class VError extends Error {
  constructor(
    public readonly key: string,
    public readonly vars?: Record<string, string | number>
  ) {
    super(vars ? `${key} ${JSON.stringify(vars)}` : key)
    this.name = 'VError'
  }
}

/** Beliebigen Fehler in die Form bringen, die über IPC zur Oberfläche geht. */
export function toPayload(err: unknown): {
  errorKey: string
  errorVars?: Record<string, string | number>
} {
  if (err instanceof VError) return { errorKey: err.key, errorVars: err.vars }
  // Unerwartetes (Netzwerk, Dateisystem): generischer Text plus Originalmeldung,
  // damit die Ursache nicht verloren geht.
  const detail = err instanceof Error ? err.message : String(err)
  return { errorKey: 'error.unknown', errorVars: { detail } }
}
