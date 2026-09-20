import { useEffect, useState } from 'react'
import { useT } from '../lib/i18n'
import Logo from './Logo'

/**
 * Blockierender Bildschirm, solange fehlende Pflicht-Mods nachgeladen werden.
 *
 * Er lässt sich nicht wegklicken, und das ist Absicht: OneConfig fehlte
 * monatelang in allen Profilen, ohne dass irgendwo etwas stand — der Client
 * sah einfach anders aus als gedacht. Wer hier wegklicken könnte, stünde
 * wieder vor demselben stillen Fehler.
 *
 * Ist alles vorhanden, erscheint der Bildschirm gar nicht erst: der
 * Hauptprozess meldet dann kein `modcheck:start`.
 */
interface Stand {
  text: string
  fertig: number
  gesamt: number
  prozent: number
}

export default function ModCheck({ onFertig }: { onFertig: () => void }): JSX.Element {
  const t = useT()
  const [stand, setStand] = useState<Stand | null>(null)
  const [offen, setOffen] = useState<string[] | null>(null)

  useEffect(() => {
    const abStatus = window.visual.modcheck.onStatus((s) => setStand(s))
    const abFertig = window.visual.modcheck.onDone((r) => {
      if (r.offen > 0) setOffen(r.namen)
      else onFertig()
    })
    return () => {
      abStatus()
      abFertig()
    }
  }, [onFertig])

  return (
    <div className="modcheck">
      <div className="modcheck-box">
        <Logo size={56} />
        <h2>{t('modcheck.title')}</h2>
        {offen === null ? (
          <>
            <p className="modcheck-sub">{stand?.text ?? t('modcheck.checking')}</p>
            <div className="modcheck-bar">
              <div className="modcheck-fill" style={{ width: `${stand?.prozent ?? 0}%` }} />
            </div>
            <p className="modcheck-count">
              {stand ? `${stand.fertig} / ${stand.gesamt}` : ''}
            </p>
          </>
        ) : (
          <>
            {/* Ohne Netz bleibt es stehen - dann darf man weiter, sonst
                haenge man fest. Der Hinweis nennt die betroffenen Profile. */}
            <p className="modcheck-sub modcheck-warn">
              {t('modcheck.failed', { profile: offen.join(', ') })}
            </p>
            <button className="btn" onClick={onFertig}>
              {t('modcheck.continue')}
            </button>
          </>
        )}
      </div>
    </div>
  )
}
