import { useEffect, useRef, useState } from 'react'
import type { Account } from '../env'
import { avatarUrl } from '../lib/loaders'
import { useT } from '../lib/i18n'

export default function Titlebar({
  account,
  accounts = [],
  instances = 0,
  version,
  onHome,
  onSwitch,
  onAddMicrosoft,
  onAddOffline,
  onRemove
}: {
  account?: Account | null
  accounts?: Account[]
  instances?: number
  version?: string
  onHome?: () => void
  onSwitch?: (id: string) => void
  onAddMicrosoft?: () => void
  onAddOffline?: (name: string) => void
  onRemove?: (id: string) => void
}): JSX.Element {
  const w = window.visual.win
  const t = useT()
  const [open, setOpen] = useState(false)
  const [offlineName, setOfflineName] = useState('')
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const close = (e: MouseEvent): void => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  return (
    <div className="titlebar">
      <div className="navarrows">
        <button onClick={() => onHome?.()} title={t('common.home')}>
          ←
        </button>
        <button style={{ opacity: 0.35 }}>→</button>
      </div>
      <div className="brand">
        <span className="word">
          VISUAL<b>CLIENT</b>
        </span>
        <span className="ver">v{version || '…'}</span>
      </div>
      <div className="spacer" />

      <span className={`chip ${instances > 0 ? 'hot' : ''}`}>
        ▣ {instances > 0 ? t('top.instances', { n: instances }) : t('top.noInstances')}
      </span>

      {account && (
        <div className="acctwrap" ref={ref}>
          <span className="chip acct" onClick={() => setOpen((o) => !o)}>
            <img src={avatarUrl(account.name, 20)} alt="" />
            {account.name}
            <span style={{ color: 'var(--text-faint)' }}>▾</span>
          </span>
          {open && (
            <div className="acctmenu">
              <div className="acctmenu-h">{t('top.accounts')}</div>
              {accounts.map((a) => (
                <div key={a.id} className={`acctrow ${a.id === account.id ? 'active' : ''}`}>
                  <img
                    src={avatarUrl(a.name, 22)}
                    alt=""
                    onClick={() => {
                      onSwitch?.(a.id)
                      setOpen(false)
                    }}
                  />
                  <div
                    className="grow"
                    onClick={() => {
                      onSwitch?.(a.id)
                      setOpen(false)
                    }}
                  >
                    <div className="an">{a.name}</div>
                    <div className="at">{t('acct.' + a.type)}</div>
                  </div>
                  {a.id === account.id && <span className="dot">●</span>}
                  {accounts.length > 1 && (
                    <button className="rm" title={t('profiles.delete')} onClick={() => onRemove?.(a.id)}>
                      ✕
                    </button>
                  )}
                </div>
              ))}
              <hr />
              <button className="acctadd" onClick={() => onAddMicrosoft?.()}>
                <span className="ms">⊞</span> {t('top.addMicrosoft')}
              </button>
              <div className="acctoff">
                <input
                  placeholder={t('top.offlineName')}
                  value={offlineName}
                  maxLength={16}
                  onChange={(e) => setOfflineName(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && offlineName.trim()) {
                      onAddOffline?.(offlineName.trim())
                      setOfflineName('')
                    }
                  }}
                />
                <button
                  className="btn"
                  disabled={!offlineName.trim()}
                  onClick={() => {
                    onAddOffline?.(offlineName.trim())
                    setOfflineName('')
                  }}
                >
                  {t('top.add')}
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      <div className="winbtns">
        <button onClick={() => w.minimize()} aria-label={t('common.minimize')}>
          —
        </button>
        <button onClick={() => w.maximize()} aria-label={t('common.maximize')}>
          ▢
        </button>
        <button className="close" onClick={() => w.close()} aria-label={t('common.close')}>
          ✕
        </button>
      </div>
    </div>
  )
}
