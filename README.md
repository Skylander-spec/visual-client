# Visual Client — Minecraft PvP Visuals Gesamtpaket

Eigenständiger Minecraft-Launcher im Dark/Cyan-Look (Stil: Lunar Client / NoRisk Client) mit
**Forge, Fabric, NeoForge, Quilt und Vanilla**, Modrinth-Mod-Browser, Profilen mit
versionsspezifischen Mod-Empfehlungen, Microsoft- **und** Offline-Login — plus Forge-Mod für
HUD/Effekte/Cosmetics und ein passendes Resource Pack. Das Spielfenster heißt „Visual Client".

```
visual client/
├── launcher/       Electron + React + TypeScript Desktop-App (der eigentliche Client)
├── fabricmod/      Fabric-Mod für 1.21.2 – 1.21.11 (HUD, Cosmetics, eigene Menüs)
├── fabricmod189/   eigener Fabric-Mod für 1.8.9 (Legacy Fabric, Java 8)
├── brandingmod/    setzt den Fenstertitel auf „Visual Client"
├── mod/            älterer Forge-Mod (1.20.1), nicht mehr im Installer
├── resourcepack/   "Visual Pack" — Texturen, GUI, Sounds (+ Generator-Pipeline)
└── docs/           Offene Punkte, Referenzen
```

## Updates veröffentlichen

Der Launcher fragt beim Start das neueste Release dieses Repos ab. Ist dessen
Version höher als die laufende, lädt er die neue `app.asar` herunter, prüft sie
gegen den in `latest.json` veröffentlichten SHA-256 und spielt sie beim
Schließen ein.

```
# einmalig
winget install --id GitHub.cli -e
gh auth login

# je Update
# 1. "version" in launcher/package.json erhöhen
# 2.
cd launcher && npm run publish
```

`npm run publish` baut den Launcher samt Installer, schreibt `latest.json` und
hängt `app.asar`, `latest.json` und den Installer an ein Release `v<version>`.

**Was ein Update nicht anfasst:** ersetzt wird ausschließlich die `app.asar` in
der Installation. Profile, Konten, Welten, selbst installierte Mods, Skins,
Capes und Ressourcenpakete liegen unter `%APPDATA%/.visualclient` und bleiben
unverändert.

## Voraussetzungen

| Tool        | Version   | Wofür                              | Status auf diesem PC |
|-------------|-----------|------------------------------------|----------------------|
| Node.js     | ≥ 20      | Launcher bauen/starten             | ✅ v24 installiert    |
| Python      | ≥ 3.10    | Resource-Pack-Pipeline             | ✅ 3.14 (`py`)        |
| Java (JDK)  | 17        | Forge-Mod bauen + MC 1.20.x        | ❌ fehlt — [Temurin 17](https://adoptium.net/temurin/releases/?version=17) installieren |
| Java (JRE)  | 8         | MC 1.8.9 spielen                   | ❌ fehlt (nur für 1.8.9 nötig) |

> Der Launcher lädt Minecraft selbst herunter, braucht aber ein installiertes Java zum Starten
> des Spiels. In den Launcher-Settings kann der Java-Pfad pro Profil gesetzt werden.

## Schnellstart

### 1. Launcher

```powershell
cd launcher
npm install
npm run dev        # Entwicklungsmodus (Fenster öffnet sich)
npm run build      # Produktions-Build
```

Beim ersten Start legt der Launcher `%APPDATA%\.visualclient\` an (Profile, Mods, Cosmetics).

- **Login:** „Mit Microsoft anmelden" (offizieller MSA-Flow) oder „Ohne Account spielen"
  (Name frei wählbar; funktioniert im Singleplayer und auf `online-mode=false`-Servern).
- **Profile:** MC-Version + Forge-Version + RAM + eigener Mod-Ordner pro Profil.
- **Mods:** Modrinth-Suche gefiltert auf Forge + Profilversion, Ein-Klick-Install,
  Update-Check, `.mrpack`-Modpack-Import.
- **Visuals:** Button „Visual Pack installieren" kopiert Resource Pack + VisualsMod ins Profil.

### 2. Resource Pack

```powershell
cd resourcepack
py tools/generate_textures.py    # erzeugt alle PNGs (Items, GUI, Emissive) nach pack/
py tools/generate_sounds.py      # erzeugt Hit-/Kill-/UI-Sounds nach pack/
```

Der fertige Ordner `resourcepack/pack/` ist direkt als Resource Pack nutzbar
(in `resourcepacks/` kopieren oder vom Launcher automatisch installieren lassen).
`pack.mcmeta` liegt in einer 1.20.x-Variante bei; für 1.8.9 `pack_format` auf `1` setzen.

### 3. Forge-Mod (VisualsMod)

```powershell
cd mod
.\gradlew build        # Jar landet in build/libs/
.\gradlew runClient    # Dev-Client mit Mod starten
```

Benötigt JDK 17. Der erste Build lädt Forge-Toolchain herunter (dauert einige Minuten).

**Module (alle einzeln im In-Game-Menü abschaltbar — Standard-Taste `Rechts-Shift`):**
Crosshair (Styles/Farbe/Glow) · Keystrokes + CPS · FPS-Anzeige · Armor-Status ·
Scoreboard-Restyle · Tablisten-Restyle · Hit-Partikel (Cyan Sparks) · Kill-Effekt + Kill-Sound ·
Custom Cape (Textur aus `%APPDATA%\.visualclient\cosmetics\cape.png`)

Konfiguration: `%APPDATA%\.visualclient\visualsmod.json` (wird auch vom Launcher gelesen/geschrieben).

## Performance

Alle Effekte sind einzeln abschaltbar, Partikelzahl ist begrenzt, HUD rendert ohne
Shader/Framebuffer-Effekte. Auf schwacher Hardware: Kill-Effekt-Partikel im Settings-Menü
reduzieren oder deaktivieren.

## Rechtliches / Assets

Alle Texturen und Sounds in diesem Paket sind prozedural generierte **Originale** —
keine kopierten Assets von reallyvisuals, Lunar Client, LabyMod o. ä.
Der MS-Login nutzt ausschließlich den offiziellen Microsoft-Auth-Flow (msmc).

## Offene Punkte

Siehe [docs/OFFENE_PUNKTE.md](docs/OFFENE_PUNKTE.md).
