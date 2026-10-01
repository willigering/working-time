# Versionshinweise — Working Time

## v2.6.2 (Build 37) — 2026-10-01
- Oberfläche im Layout Dunkelblau/Gold: Heute, Zeiten, Projekte, Einstellungen.
- Laufender Timer mit Goldring, Stop und Tageswerten. Übersicht mit Woche, letzten Zeiten und neuem Eintrag.
- Vollbilduhr per Tippen auf den Ring. Zeiten nach Tag, Woche und Monat.
- Projektdetail mit Satz, Gesamtzeit, Umsatz, Farbe, Archiv und Löschen.
- Einträge als eigene Seite. Export, Profil, Kunden und Unterstützung unter Einstellungen.

## v2.6.1 (Build 36) — 2026-10-01

- Premium-Farbpalette: dunklere Flächen, Gold-Akzent, hellere Lesetexte.
- Analog-Uhr größer und hinter der Laufzeit im Timer-Hintergrund.
- Uhr zurückhaltender gezeichnet, damit die erfasste Zeit im Vordergrund bleibt.
- Release-Build erzeugt zusätzlich versionierte APK-Dateinamen.

## v2.6.0 (Build 35) — 2026-09-30

- Historische Stundensätze und Abrechenbarkeit bleiben beim Löschen von Projekten erhalten.
- Wiederherstellen erhält zwischenzeitliche Änderungen und neue Timerstarts.
- Stundensätze werden validiert; deutsche und englische Zahlenformate unterstützt.
- Zeiteinträge nachtragen und bearbeiten; Suche, Projekt- und Datumsfilter sowie Tagesgruppen.
- Einheitliche Abrechnung aus vollständigen Minuten; Zeitraumexport beschneidet überlappende Einträge.
- Kalenderwoche ab Montag, Tagesgrenzen mit Zeitumstellung und laufende Zeiten in Statistiken.
- Notizen laufender Sitzungen werden gespeichert; Kunden aus der Auswahl entfernen, ohne Historie zu löschen.
- JSON-Dateien im Hintergrund atomisch speichern; beschädigte Dateien nicht überschreiben und Speicherfehler anzeigen.
- Logo-Import im Hintergrund mit begrenztem Speicherbedarf, EXIF-Ausrichtung, Drehung, mittigem quadratischen Zuschnitt und heller Vorschau.
- Fehlgeschlagene Logo-Imports erhalten das vorherige Logo; Vorschau des Exportkopfs im Profil.
- Größere Beschriftungen und Touchflächen, bessere Textkontraste, scrollbarer Timer und umbrechende Farbauswahl.
- Hintergrundaktualisierungen pausieren, historische Summen zwischenspeichern.
- Analoge Uhr verwendet APIs, die auch auf Android 7 (minSdk 24) verfügbar sind.
- Android-Build und Regressionstests über GitHub Actions.

## v2.5.9 (Build 34) — 2026-09-06

- Lösch-Balken zeigt „Wiederherstellen“ und stellt per Tippen wieder her

## v2.5.8 (Build 33) — 2026-09-05

- Rückgängig per Wischen funktioniert wieder (Geste wurde nicht mehr abgebrochen)
- Hinweistext „5 Sekunden zum Rückgängig“ entfernt

## v2.5.7 (Build 32) — 2026-09-05

- Projekt löschen: Option per Tippen auswählen, Löschen und Abbrechen im selben Dialog
- Nach dem Löschen 5 Sekunden zum Rückgängig machen durch Wischen

## v2.5.6 (Build 31) — 2026-09-05

- Projekt löschen in zwei Schritten: erst Einträge behalten / Alles löschen wählen, dann mit „Jetzt löschen“ bestätigen

## v2.5.5 (Build 30) — 2026-09-05

- Verlauf: vor dem Löschen eines Eintrags erscheint eine Bestätigung
- Projekt löschen: klare, volle Buttons für „Einträge behalten“ und „Alles löschen“
- Lösch-Dialoge ohne bräunlichen Material-Ton, mit Warn-Icon und Abbrechen-Button

## v2.5.4 (Build 29) — 2026-08-15

- Kunden-Dropdown: nach einer Auswahl bleiben alle Kunden wählbar (Liste wird nicht mehr auf den gewählten Namen gefiltert)

## v2.5.3 (Build 28) — 2026-08-15

- Kunden-Dropdown: sichtbare Trennlinien, abwechselnde Zeilenfarben, Icon und Haken für die Auswahl

## v2.5.2 (Build 27) — 2026-08-15

- Analog-Uhr bleibt beim Swipen sichtbar (kein Ausblenden/Neuaufbau mehr)

## v2.5.1 (Build 26) — 2026-08-15

- Timer-Tab nicht mehr scrollbar
- Analog-Uhr dezenter und nur so groß wie der freie Platz zwischen Button und Statistik

## v2.5.0 (Build 25) — 2026-08-15

- Kunden als eigene Datenbank-Einträge (clients.json)
- Beim Anlegen/Bearbeiten eines Projekts wird der Kunde gespeichert und kann per Dropdown gewählt werden
- Eigene Kunden-Verwaltung unter Projekte
- Premium-Design: Orange nur als Akzent, dunklere Flächen, Timer als Mittelpunkt
- Analog-Uhr größer mit 12/3/6/9, klareren Markierungen und Glow
- Start-Button Anthrazit / Stop-Button Orange
- Projektkarten mit linkem Akzentbalken, Overflow-Menü, Empty-State

## v2.4.4 (Build 23) — 2026-08-03

- Verlauf: kaputte Sonderzeichen behoben (`17:28 – 17:28`, `·` statt `â€“` / `Â·`)

## v2.4.3 (Build 22) — 2026-07-28

- Animierte Uhr ohne Bildrahmen und ohne Zoom-Animation

## v2.4.2 (Build 21) — 2026-07-28

- QR-Codes im Support-Bereich entfernt (auf dem Smartphone unnötig)
- Kompakte Krypto-Karten: Name, gekürzte Adresse, Kopieren
- ZXing-Abhängigkeit entfernt

## v2.4.1 (Build 20) — 2026-07-28

- PayPal aktiv: https://paypal.me/willigeringDE (Support-Karte öffnet den Link)

## v2.4.0 (Build 19) — 2026-07-28

- **Support the Project**-Bereich (Stats): professionell statt klassischer Spendenbitte
- Hero mit Herz, klare Botschaft „Working Time bleibt kostenlos“
- Krypto-Karten im 2-Spalten-Grid: QR-Code, gekürzte Adresse, Kopieren-Button, Coin-Farben
- BTC, Ethereum, Solana, XRP, USDT (ERC-20)
- PayPal-Karte als sichtbarer Platzhalter („Demnächst verfügbar“)
- Abschnitte „Warum spenden?“ und Dank an Unterstützer
- Hintergrund dezent auf #111, Glassmorphism-Karten

## v2.3.1 (Build 18) — 2026-07-25

- PDF-Logo schärfer (volle Auflösung, kein Herunterskalieren auf Pixel)
- Logo rechts neben dem Namen im PDF-Export
- Kein zusätzlicher Name mehr in der PDF-Fußzeile
- Spendentext ohne „dezent gemeint“

## v2.3.0 (Build 17) — 2026-07-25

- Neuer Tab **Profil**: Name, optionale Firma, eigenes Logo hochladen
- Empfohlenes Logoformat und -größe im Profil angezeigt
- PDF/Excel-Exporte mit User-Name und User-Logo (kein App-Name, kein Entwickler, kein Working-Time-Logo)
- Stats: „Über die App“ mit Entwickler, Version und dezenten Spendenadressen (BTC, Solana, ETH, XRP, kopierbar)

## v2.2.0 (Build 16) — 2026-07-25

- Export-Dialog im Verlauf mit Filter: Zeitraum, Projekt, Kunde, alle Daten
- CSV-Export (Excel/LibreOffice/Sheets) mit Spalten Datum, Projekt, Beginn, Ende, Pause, Arbeitszeit, Stundensatz, Verdienst, Notiz
- Excel-Export (.xlsx) mit Kopfzeile, AutoFilter und Summenzeile
- PDF-Stundennachweis zum Versenden an Kunden
- PDF-Monatsbericht (Vor-Rechnung) mit Logo, Projekt, Kunde, Stunden und Gesamtbetrag

## v2.1.3 (Build 15) — 2026-07-25

- Projektliste: nur noch Stundensatz (€/h), ohne „abrechenbar“
- Projekt löschen: Option „nur Projekt“ oder „Projekt und Einträge“
- Verlauf behält bei gelöschten Projekten Name, Kunde, Zeit und Stundensatz
- Tab-Reihenfolge: Timer → Projekte → Verlauf → Stats

## v2.1.2 (Build 14) — 2026-07-25

- Beim Löschen eines Projekts erscheint eine Bestätigungsabfrage
- App dreht sich nicht mehr mit dem Smartphone (Portrait fest)
- Ohne gewähltes/angelegtes Projekt: „Erstelle dein erstes Projekt“ statt „Allgemein“
- Im Projekt-Dropdown: Eintrag „Neues Projekt erstellen“
- Kein automatisches Standard-Projekt „Allgemein“ mehr bei Neuinstallation

## v2.1.1 (Build 13) — 2026-07-23

- `targetSdk` und `compileSdk` auf API 35 angehoben (Google-Play-Anforderung)
- `minSdk` bleibt 24 (Geräte ab Android 7.0 weiterhin unterstützt)
- Neue signierte AAB/APK für den Play-Upload

## v2.1.0 (Build 12) — 2026-07-21

- Release-Signierung mit eigenem Upload-Keystore (nicht mehr Debug-Key)
- Play Console akzeptiert die AAB nun als Release-Build
- Package: `de.willigering.workingtime`
- Upload-Key: `keystore/working-time-upload.jks` (lokal, nicht committen)

## v2.0.9 (Build 11) — 2026-07-21

- Package-ID geändert auf `de.willigering.workingtime` (Play-Store-tauglich)
- App-Icon mehrfach aktualisiert (aktuell: Stoppuhr-Design ohne Text)
- „Gestartet um …“ unter der **animierten Uhr**, solange eine Session läuft
- Animierte Uhr bleibt auch während laufender Session sichtbar
- Release-APK und AAB für Google Play gebaut

## v2.0.8 (Build 10) — 2026-07-21

- Neues Launcher-/Play-Store-Icon
- Anzeige „Gestartet um HH:mm“ für die laufende Session (Timer-Tab)
- Version für Store-Build angehoben

## v2.0.7 (Build 9) und älter

- Freelancer-Zeiterfassung mit Timer, Projekten, Verlauf, Statistik
- Kotlin + Jetpack Compose, dunkles UI
- CSV-Export, Stundensätze, lokale JSON-Speicherung
- Entwickler: Willi Gering
