import { useEffect, useState } from 'react'
import { useT } from '../lib/i18n'

export default function Settings(): JSX.Element {
  const t = useT()
  // Die Version stand hier fest als „1.0.0“ im Text — sie kommt jetzt aus
  // der App selbst, sonst veraltet sie mit jedem Release stillschweigend.
  const [version, setVersion] = useState('…')

  useEffect(() => {
    window.visual
      .appVersion()
      .then(setVersion)
      .catch(() => setVersion('—'))
  }, [])

  const karten = [
    { titel: 'settings.dataFolder', text: 'settings.dataFolderDesc' },
    { titel: 'settings.inGame', text: 'settings.inGameDesc' },
    { titel: 'settings.customCape', text: 'settings.customCapeDesc' }
  ]

  return (
    <>
      <h1>{t('settings.title')}</h1>
      <p className="sub">{t('settings.info')}</p>
      <div className="list">
        {karten.map((k) => (
          <div className="card" key={k.titel}>
            <div className="mod-title">{t(k.titel)}</div>
            <div className="muted" style={{ marginTop: 4 }}>
              {t(k.text)}
            </div>
          </div>
        ))}
        <div className="card">
          <div className="mod-title">{t('settings.version')}</div>
          <div className="muted" style={{ marginTop: 4 }}>
            {t('settings.versionDesc', { version })}
          </div>
        </div>
      </div>
    </>
  )
}
