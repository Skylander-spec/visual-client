import { useEffect, useState } from 'react'
import { companions, hats, type ClientPreferences } from '../../../shared/client-preferences'
import type { Profile } from '../env'
import Capes from './Capes'
import landscape from '../assets/horizon-village.webp'

export default function ClientCosmetics(): JSX.Element {
  const [settings, setSettings] = useState<ClientPreferences | null>(null)
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)
  useEffect(() => {
    window.visual.clientPreferences.get().then(setSettings).catch(e => setMessage(String(e)))
    window.visual.profiles.list().then(setProfiles).catch(e => setMessage(String(e)))
  }, [])
  function choice(key: keyof ClientPreferences, title: string, options: string[]): JSX.Element | null {
    if (!settings) return null
    return <label className="field">{title}<select value={Number(settings[key])} onChange={e => setSettings({...settings, [key]:Number(e.target.value)})}>{options.map((text,i) => <option key={i} value={i}>{text}</option>)}</select></label>
  }
  function toggle(key: keyof ClientPreferences, title: string): JSX.Element | null {
    if (!settings) return null
    return <label className="wardrobe-toggle"><span>{title}</span><input type="checkbox" checked={Boolean(settings[key])} onChange={e => setSettings({...settings,[key]:e.target.checked})}/></label>
  }
  return <section className="client-wardrobe">
    <header className="wardrobe-hero" style={{backgroundImage:`linear-gradient(90deg,#09121be8,#09121b35),url(${landscape})`}}><span className="engine-eyebrow">DEIN VISUAL CLIENT</span><h1>Dein Look. Deine Begleiter.</h1><p>Einmal auswählen. In kompatiblen Java-Profilen wiederfinden.</p></header>
    <div className="wardrobe-layout">
      <div className="wardrobe-controls">
        <h2>Begleiter</h2>
        {settings && <>
          {toggle('miniMe','Begleiter aktivieren')}
          <div className="companion-list" role="group" aria-label="Begleiter auswählen">{companions.map((name,i) => <button key={name} className={settings.miniMeArt === i ? 'selected' : ''} aria-pressed={settings.miniMeArt === i} onClick={() => setSettings({...settings,miniMeArt:i,miniMe:true})}>{name}</button>)}</div>
          <div className="wardrobe-fields">
            {choice('miniMePos','Position',['Auf dem Kopf','Linke Schulter','Rechte Schulter','Folgt mir'])}
            {choice('miniMeFlight','Bewegung',['Passend zum Begleiter','Laufen','Fliegen'])}
            {choice('miniMeHut','Hut',hats)}
            {choice('miniMeScarf','Schal',['Keiner','Rosa','Blau','Mint'])}
            {choice('miniMeShoes','Schuhe',['Keine','Rosa','Blau','Hasenpantoffeln'])}
            <label className="field">Größe · {settings.miniMeSize}%<input type="range" min="15" max="70" value={settings.miniMeSize} onChange={e => setSettings({...settings,miniMeSize:Number(e.target.value)})}/></label>
          </div>
          {toggle('miniMeSitzt','Mini-Me sitzt auf Kopf oder Schulter')}
          {toggle('miniMeFluegel','Flügel')}
          <h2>Standardfunktionen</h2>
          {toggle('zoom','Zoom')}{toggle('customTitle','Visual-Startmenü')}{toggle('fps','FPS-Anzeige')}{toggle('keystrokes','Tastenanzeige')}
          <div className="wardrobe-save"><button className="btn primary" disabled={saving} onClick={async () => {setSaving(true);try {setSettings(await window.visual.clientPreferences.save(settings));setMessage('Gespeichert – gilt beim nächsten Start eines kompatiblen Java-Profils.')}catch(e){setMessage(String(e))}finally{setSaving(false)}}}>{saving ? 'Speichert …' : 'Auswahl speichern'}</button><span>Vorher laufende Spiele schließen.</span></div>
        </>}
        {message && <p role="status">{message}</p>}
      </div>
      <aside className="wardrobe-compatibility"><h2>Deine Profile</h2><p>Die Auswahl bleibt erhalten. Für die Anzeige im Spiel wird die passende Visual-Erweiterung benötigt.</p>{profiles.length === 0 && <p>Noch keine Profile angelegt.</p>}{profiles.map(p => <div key={p.id}><strong>{p.name}</strong><small>{p.mcVersion} · {p.engine === 'horizon' ? 'Rust-Portierung noch offen' : ['fabric','quilt'].includes(p.loader) && /^1\.21\.(2|3|4|5|6|7|8|9|10|11)$/.test(p.mcVersion) ? 'Visual-Erweiterung wird beim Start bereitgestellt' : 'Vollständiger Funktionsumfang nicht verifiziert'}</small></div>)}<p>Standardfunktionen werden zentral verwaltet. Die technische Erweiterung im Spiel bleibt versionsabhängig.</p></aside>
    </div>
    <Capes />
  </section>
}
