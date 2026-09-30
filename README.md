# Working Time Tracker

Premium Freelancer-Zeiterfassung für Android – dunkles UI mit animiertem Uhren-Hintergrund.

**Entwickler:** Willi Gering

## Funktionen

### Timer
- Projekt wählen und Zeit starten/stoppen
- Live-Timer mit optionaler Notiz
- Heute / Kalenderwoche / Monat inklusive laufender Sitzung

### Projekte
- Projekte anlegen mit Name, Kunde, Stundensatz
- Kunden in eigener Liste speichern und beim neuen Projekt aus dem Dropdown wählen
- Farbe pro Projekt
- Abrechenbar / nicht abrechenbar

### Verlauf
- Alle Sessions mit Projekt, Dauer und Verdienst
- Einträge nachtragen, bearbeiten, löschen und wiederherstellen
- Suche nach Projekt, Kunde und Notiz; Projekt- und Datumsfilter; Gruppen pro Tag
- CSV-, XLSX- und PDF-Export; Filter nach Zeitraum, Projekt und Kunde
- Zeitraumexport enthält nur den überlappenden Teil einer Sitzung
- Dauer und Verdienst verwenden vollständige Minuten

### Statistik
- Stunden & Verdienst: Heute, Woche, Monat
- Aufschlüsselung nach Projekt (Monat)

### Eigenes Logo
- Im Profil ein PNG-, JPG- oder WebP-Bild auswählen
- EXIF-Ausrichtung wird berücksichtigt; vor dem Speichern drehen oder mittig quadratisch zuschneiden
- Helle Vorschau und Exportkopf-Vorschau; das Logo erscheint in PDF-Exporten
- Ein fehlgeschlagener Import lässt das vorhandene Logo unverändert

## Tech-Stack

- Kotlin + Jetpack Compose + Material 3
- Lokale JSON-Speicherung im Hintergrund mit atomischen Schreibvorgängen (keine Cloud)
- Migration alter `sessions.json` / `active.txt` Daten

## Prüfen

GitHub Actions baut beide Sprachvarianten und prüft Abrechnung, Rückgängig, Datumsgrenzen, gespeicherte Notizen und Logo-Import. Ein manueller Test auf Android bleibt für Tastatur, große Schrift und Teilen notwendig.

```powershell
.\gradlew assembleDeDebug assembleEnDebug testDeDebugUnitTest lintDeDebug
```

## APK bauen

```powershell
.\gradlew assembleRelease
```

APK: `app\build\outputs\apk\de\release\app-de-release.apk`
