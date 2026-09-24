# Ingame-Teststand für Visual Client 1.9.0 – 24. September 2026

## Änderungen

- 17 Begleiter: Mini-Ich, zehn Minecraft-Tiere/Helfer und sechs eigene Modelle.
  Eigene Galerie mit animierten Illustrationen und ausgewähltem 3D-Modell im
  Ingame-Spiegel. Panda und weitere eigene Modelle blinzeln und bewegen sich.
- Folgen mit begrenzter Wegsuche, Körperausrichtung, Kollisionsprüfung,
  Höhenunterschieden und Schwerkraft; wahlweise Fliegen. Schulter- und Kopfplätze
  bleiben verfügbar. Begleiter sind rein kosmetisch.
- Hüte, Schals, Schuhe und Flügel für das Spieler-Mini-Me sowie lokaler Import
  von 64×64-PNG-Skins. Tiermodelle tragen diese Spieler-Accessoires nicht.
- 15 Cape-Vorlagen einschließlich eines weißen Monster-Energy-Fanmotivs.
- Originales OneConfig-Modulmenü, Verknüpfungen zu allen Modulen, Cosmetics und
  HUD-Editor. Eigener Titelbildschirm mit Panorama, Schnellstart und Serverkarten.
- Geschlossene Compose-Menüs werden neu erzeugt, wenn man zurückkehrt;
  damit wird der Fehler einer bereits geschlossenen ComposeScene vermieden.
  Unterseiten kehren dabei zur jeweiligen Menüübersicht zurück.
- Capes und Mini-Skins werden im Hintergrund geladen, Vorschaubilder und
  Renderzustände wiederverwendet. Einstellungen werden gebündelt gespeichert.
- Die Desktop-Launcher-Oberfläche in `launcher/src` wurde nicht geändert.

## Durchgeführte Prüfungen

- Finale Gradle-Builds erfolgreich für alle zehn Ziele 1.21.2–1.21.11 und
  den Legacy-Mod 1.8.9; Launcher-Produktionsbuild erfolgreich. Gepackte
  Minecraft-Abhängigkeiten, Klassen, 15 Capes und Theme-SVGs geprüft.

- `AnimationCheck.java`: Himmelsrichtungen, stetige und begrenzte Winkbewegung,
  Hüpfen nur bei Bewegung.
- `MotionCheck.java`: Zielwechsel beim Drehen, erhöhter Boden, Weg um eine Wand,
  Fliegen, Landen, Graben und kein schwebender Spawn über tiefem Boden.
- `node --test cosmetics-server/server.test.mjs`: Datenvalidierung einschließlich
  neuer Begleiter und Accessoires, zwei HTTP-Clients, Identitätsbindung und
  Schutz gegen erneut verwendete Login-Challenges. Mojang-Prüfung dabei simuliert.
- Isolierte Minecraft-1.21.11-Instanz mit Java 21, Fabric und OneConfig 1.0.13:
  Startbildschirm, natives Modulmenü, Einstellungsseite, HUD-Editor und Rückkehr,
  Weltenauswahl und Rückkehr, Laden einer lokalen Testwelt.
- In der Welt: Schulterplätze von vorn/hinten, Körperdrehung, 3D-Spiegel,
  Galerie, Panda, eigenes Mini-Ich, Hasenohren, rosa Schal und Schuhe; alle
  15 Cape-Karten geladen und weißes Monster-Fanmotiv ausgewählt.
- Lokale 64×64-Test-PNG über den nativen Dateidialog importiert; Kopie und
  gespeicherte Auswahl geprüft. Im Titelmenü zeigt der Spiegel den Hauptskin;
  Begleiter werden dort erst nach dem Betreten einer Welt in 3D dargestellt.
- Das Menü öffnet über den Pausenmenü-Knopf sowie über eine nur im Testprofil
  auf F8 geänderte Tastenbelegung. Rechts-Shift ließ sich mit der automatisierten
  Eingabe nicht verlässlich prüfen; die ausgelieferte Standardbelegung bleibt.
- Vorladen der Menü-Skintextur: Beim erneuten Öffnen des nativen Menüs trat der
  zuvor beobachtete Hintergrundthread-Texturfehler nicht mehr auf.

## Grenzen

- Die letzte Bildschirmsteuerung wurde vom Nutzer mit Escape beendet. Die
  anschließend ergänzten Theme-Vorschaubilder und das Aktualisieren asynchroner
  Statuszeilen wurden gebaut; eine erneute Sichtprüfung entfiel.

- Moderne Build-Ziele: Fabric **1.21.2 bis 1.21.11**. Laufzeitsichtprüfung hier
  auf 1.21.11; ein erfolgreicher Build ersetzt keinen Test jeder Modkombination.
- 1.8.9 hat einen separaten Legacy-Mod mit gebündeltem Speichern. Die neuen
  Begleiter und Menüs sind dort nicht implementiert. Weitere Minecraft-Versionen,
  Forge und NeoForge haben keine entsprechende Funktionsabdeckung.
- Kein öffentlich bereitgestellter Cosmetics-Dienst: Sichtbarkeit bei Freunden
  ist noch nicht einsatzbereit. Ein Test mit zwei echten Minecraft-Accounts fehlt.
- Keine FPS-Messung auf dem Rechner des Freundes; keine garantierte FPS-Steigerung.
- OneConfig 1.0.13 protokolliert einen Fehler seines ModMenu-Kompatibilitätshooks
  (`net.minecraft.network.chat.Component` fehlt). Die hier geprüften Menüs
  blieben bedienbar. Der Testaccount ist offline; Realms-Authentifizierungsmeldungen
  sind deshalb kein Test eines angemeldeten Accounts.
- Wegsuche ist lokal und begrenzt: keine vollständige Minecraft-Mob-KI, keine
  Interaktionen mit Türen und keine serverseitigen Begleiter.

Quellen und Abgrenzung zum Vorbild: [INGAME_QUELLEN.md](INGAME_QUELLEN.md).
