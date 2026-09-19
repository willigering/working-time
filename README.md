# Working Time Tracker

Premium Freelancer-Zeiterfassung für Android – dunkles UI mit animiertem Uhren-Hintergrund.

## Funktionen

### Timer
- Projekt wählen und Zeit starten/stoppen
- Live-Timer mit optionaler Notiz
- Live-Verdienst-Anzeige (bei Stundensatz)
- Heute / Woche Zusammenfassung

### Projekte
- Projekte anlegen mit Name, Kunde, Stundensatz
- Kunden in eigener Liste speichern und beim neuen Projekt aus dem Dropdown wählen
- Farbe pro Projekt
- Abrechenbar / nicht abrechenbar

### Verlauf
- Alle Sessions mit Projekt, Dauer und Verdienst
- Einträge löschen
- CSV-Export (Projekt, Kunde, Satz, Verdienst, Notizen)

### Statistik
- Stunden & Verdienst: Heute, Woche, Monat
- Aufschlüsselung nach Projekt (Monat)

## Tech-Stack

- Kotlin + Jetpack Compose + Material 3
- Lokale JSON-Speicherung (keine Cloud)
- Migration alter `sessions.json` / `active.txt` Daten

## APK bauen

```powershell
cd C:\Users\wital\ArbeitszeitTracker
.\gradlew assembleDebug
```

APK: `app\build\outputs\apk\debug\app-debug.apk`

Release:
```powershell
.\gradlew assembleRelease
```

---

**Entwickler:** Willi Gering

v2.0.0 – Upgrade von ArbeitszeitTracker mit Compose-UI & Freelancer-Features