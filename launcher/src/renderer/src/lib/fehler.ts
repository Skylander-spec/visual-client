/**
 * Fehlertext für die Oberfläche.
 *
 * Der Hauptprozess wirft VError, dessen `message` aus Schlüssel und
 * Platzhaltern besteht (z. B. `error.noVersion {"version":"1.21.4"}`).
 * Electron reicht eine geworfene Ausnahme aber nur als Zeichenkette weiter
 * und stellt noch eigenen Text voran — deshalb wird der Schlüssel hier
 * wieder herausgelöst und übersetzt.
 *
 * Passt nichts, bleibt die Originalmeldung als Detail erhalten: lieber ein
 * unübersetzter Hinweis als gar keine Ursache.
 */
export function errorText(
  e: unknown,
  t: (key: string, vars?: Record<string, string | number>) => string
): string {
  const raw = e instanceof Error ? e.message : String(e)
  const m = raw.match(/(error\.[A-Za-z]+)(?:\s+(\{.*\}))?/)
  if (m) {
    let vars: Record<string, string | number> | undefined
    if (m[2]) {
      try {
        vars = JSON.parse(m[2])
      } catch {
        /* Platzhalter unlesbar — Text ohne sie ist immer noch richtig */
      }
    }
    return t(m[1], vars)
  }
  return t('error.unknown', { detail: raw })
}
