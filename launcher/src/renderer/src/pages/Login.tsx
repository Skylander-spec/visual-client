import { useState } from 'react'
import Logo from '../components/Logo'
import { useT } from '../lib/i18n'

export default function Login({ onLogin }: { onLogin: () => void }): JSX.Element {
  const t = useT()
  const [name, setName] = useState('')
  const [busy, setBusy] = useState<'ms' | 'offline' | null>(null)
  const [error, setError] = useState('')

  async function microsoft(): Promise<void> {
    setBusy('ms')
    setError('')
    try {
      await window.visual.auth.microsoft()
      onLogin()
    } catch (e) {
      setError(t('login.microsoftFailed'))
    } finally {
      setBusy(null)
    }
  }

  async function offline(): Promise<void> {
    if (!name.trim()) {
      setError(t('login.enterName'))
      return
    }
    setBusy('offline')
    await window.visual.auth.offline(name.trim())
    onLogin()
    setBusy(null)
  }

  return (
    <div className="login">
      <div className="panel panelfade">
        <div className="mark">
          <Logo size={64} />
        </div>
        <h1>Visual Client</h1>
        <p className="sub" style={{ marginBottom: 8 }}>
          PvP · Forge · Modrinth
        </p>
        <button className="btn primary" onClick={microsoft} disabled={busy !== null}>
          {busy === 'ms' ? t('common.loading') : t('top.addMicrosoft')}
        </button>
        <div className="muted">{t('common.or')}</div>
        <div className="row">
          <input
            className="grow"
            placeholder={t('top.offlineName')}
            value={name}
            maxLength={16}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && offline()}
          />
          <button className="btn" onClick={offline} disabled={busy !== null}>
            {t('login.offlinePlay')}
          </button>
        </div>
        {error && <div className="error">{error}</div>}
        <div className="muted" style={{ marginTop: 6 }}>
          {t('login.offlineHint')}
        </div>
      </div>
    </div>
  )
}
