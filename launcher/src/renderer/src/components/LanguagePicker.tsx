import { LANGS, Lang } from '../lib/i18n'

/**
 * Sprachwahl beim allerersten Start.
 *
 * Bewusst ohne übersetzte Texte: wer hier steht, hat noch keine Sprache
 * gewählt. Die Schaltflächen tragen die Sprachnamen in der jeweiligen
 * Sprache selbst — das versteht man ohne Übersetzung. Die Überschrift steht
 * in allen drei, damit sich niemand ausgeschlossen fühlt.
 */
export default function LanguagePicker({
  onPick
}: {
  onPick: (lang: Lang) => void
}): JSX.Element {
  return (
    <div className="login">
      <div className="panel panelfade" style={{ textAlign: 'center' }}>
        <div className="mark" style={{ fontSize: 40, marginBottom: 6 }}>
          🌐
        </div>
        <h1 style={{ marginBottom: 6 }}>Choose your language</h1>
        <p className="sub" style={{ marginBottom: 22 }}>
          Sprache wählen · Выберите язык
        </p>
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: 10,
            width: 'min(320px, 80vw)',
            margin: '0 auto'
          }}
        >
          {LANGS.map((l) => (
            <button
              key={l.id}
              className="btn"
              style={{ justifyContent: 'center', padding: '12px 16px', fontSize: 14 }}
              onClick={() => onPick(l.id)}
            >
              {l.flag} {l.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
