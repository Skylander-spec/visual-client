import { lazy, Suspense, useCallback, useEffect, useState } from 'react'
import Titlebar from './components/Titlebar'
import Sidebar, { Page } from './components/Sidebar'
import Login from './pages/Login'
import type { Account, Profile } from './env'
import { LangContext, getLang, langChosen, Lang, translate } from './lib/i18n'
import LanguagePicker from './components/LanguagePicker'
import ModCheck from './components/ModCheck'

// Seiten erst laden, wenn sie geöffnet werden — kleinere Start-Ladung.
const Home = lazy(() => import('./pages/Home'))
const Profiles = lazy(() => import('./pages/Profiles'))
const Mods = lazy(() => import('./pages/Mods'))
const Skins = lazy(() => import('./pages/Skins'))
const Capes = lazy(() => import('./pages/Capes'))
const SettingsModal = lazy(() => import('./components/SettingsModal'))

export default function App(): JSX.Element {
  // Mod-Pruefung beim Start. Meldet der Hauptprozess nichts, ist alles da
  // und der Bildschirm erscheint nie.
  const [pruefung, setPruefung] = useState(false)
  useEffect(() => {
    return window.visual.modcheck.onStart(() => setPruefung(true))
  }, [])
  const [account, setAccount] = useState<Account | null>(null)
  const [accounts, setAccounts] = useState<Account[]>([])
  const [booted, setBooted] = useState(false)
  const [page, setPage] = useState<Page>('home')
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [activeId, setActiveId] = useState<string | null>(null)
  const [autoPlay, setAutoPlay] = useState(false)
  const [instances, setInstances] = useState(0)
  const [showSettings, setShowSettings] = useState(false)
  const [lang, setLangState] = useState<Lang>(getLang())
  // Beim allerersten Start zuerst die Sprache erfragen.
  const [spracheGewaehlt, setSpracheGewaehlt] = useState(langChosen())
  const [update, setUpdate] = useState<{ version: string } | null>(null)
  const [appVersion, setAppVersion] = useState('')
  const [modsRequest, setModsRequest] = useState<{ tab: 'browse' | 'installed'; n: number }>({
    tab: 'installed',
    n: 0
  })

  const reloadProfiles = useCallback(async () => {
    const list = await window.visual.profiles.list()
    setProfiles(list)
    setActiveId((cur) => (cur && list.some((p) => p.id === cur) ? cur : (list[0]?.id ?? null)))
  }, [])

  const reloadAccounts = useCallback(async () => {
    const [active, list] = await Promise.all([window.visual.auth.get(), window.visual.auth.list()])
    setAccount(active)
    setAccounts(list)
  }, [])

  useEffect(() => {
    Promise.all([window.visual.auth.get(), window.visual.auth.list()]).then(([a, list]) => {
      setAccount(a)
      setAccounts(list)
      setBooted(true)
    })
    reloadProfiles()
    window.visual.launch.instances().then(setInstances)
    window.visual.appVersion().then(setAppVersion)
    window.visual.update.state().then((s) => {
      if (s.available && s.version) setUpdate({ version: s.version })
    })
    const offInst = window.visual.launch.onInstances((info) => setInstances(info.count))
    const offUpd = window.visual.update.onAvailable((info) => setUpdate({ version: info.version }))
    return () => {
      offInst()
      offUpd()
    }
  }, [reloadProfiles])

  function changeLang(l: Lang): void {
    localStorage.setItem('lang', l)
    setLangState(l)
  }

  if (!booted) return <div className="app" />

  if (!spracheGewaehlt) {
    return (
      <LangContext.Provider value={lang}>
        <div className="app">
          <Titlebar />
          <LanguagePicker
            onPick={(l) => {
              changeLang(l)
              setSpracheGewaehlt(true)
            }}
          />
        </div>
      </LangContext.Provider>
    )
  }

  if (pruefung) {
    return (
      <LangContext.Provider value={lang}>
        <div className="app">
          <Titlebar />
          <ModCheck onFertig={() => setPruefung(false)} />
        </div>
      </LangContext.Provider>
    )
  }

  if (!account) {
    return (
      <LangContext.Provider value={lang}>
        <div className="app">
          <Titlebar />
          <Login onLogin={reloadAccounts} />
        </div>
      </LangContext.Provider>
    )
  }

  const active = profiles.find((p) => p.id === activeId) ?? null

  const pageEl =
    page === 'home' ? (
      <Home
        profiles={profiles}
        active={active}
        onSelect={setActiveId}
        onGoProfiles={() => setPage('profiles')}
        account={account}
        autoPlay={autoPlay}
        onAutoPlayed={() => setAutoPlay(false)}
      />
    ) : page === 'profiles' ? (
      <Profiles
        profiles={profiles}
        onChanged={reloadProfiles}
        onPlay={(id) => {
          setActiveId(id)
          setAutoPlay(true)
          setPage('home')
        }}
        onMods={(id) => {
          setActiveId(id)
          setModsRequest((r) => ({ tab: 'installed', n: r.n + 1 }))
          setPage('mods')
        }}
      />
    ) : page === 'mods' ? (
      <Mods
        profiles={profiles}
        active={active}
        onSelect={setActiveId}
        onChanged={reloadProfiles}
        request={modsRequest}
      />
    ) : page === 'skins' ? (
      <Skins account={account} />
    ) : (
      <Capes />
    )

  return (
    <LangContext.Provider value={lang}>
      <div className="app">
        <Titlebar
          account={account}
          accounts={accounts}
          instances={instances}
          version={appVersion}
          onSwitch={async (id) => {
            await window.visual.auth.switch(id)
            reloadAccounts()
          }}
          onAddMicrosoft={async () => {
            try {
              await window.visual.auth.microsoft()
              reloadAccounts()
            } catch {
              /* Login abgebrochen */
            }
          }}
          onAddOffline={async (name) => {
            await window.visual.auth.offline(name)
            reloadAccounts()
          }}
          onRemove={async (id) => {
            await window.visual.auth.remove(id)
            reloadAccounts()
          }}
        />
        {update && (
          <div className="updatebar">
            <span>
              ✦ {translate(lang, 'update.available')} — <b>v{update.version}</b>
            </span>
            <div className="grow" />
            <button className="btn" onClick={() => setUpdate(null)}>
              {translate(lang, 'update.onClose')}
            </button>
            <button className="btn primary" onClick={() => window.visual.update.apply()}>
              {translate(lang, 'update.restartNow')}
            </button>
          </div>
        )}
        <div className="shell">
          <Sidebar
            page={page}
            onNavigate={(p) => {
              if (p === 'settings') return setShowSettings(true)
              if (p === 'mods') setModsRequest((r) => ({ tab: 'browse', n: r.n + 1 }))
              setPage(p)
            }}
          />
          <main className="content">
            <div key={page} className="pagefade">
              <Suspense fallback={<div className="muted" style={{ padding: 20 }}>{translate(lang, 'common.loading')}</div>}>
                {pageEl}
              </Suspense>
            </div>
          </main>
        </div>
        {showSettings && (
          <Suspense fallback={null}>
            <SettingsModal lang={lang} onLang={changeLang} onClose={() => setShowSettings(false)} />
          </Suspense>
        )}
      </div>
    </LangContext.Provider>
  )
}
