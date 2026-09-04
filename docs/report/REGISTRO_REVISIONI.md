# Registro Attivo delle Revisioni & Affinamenti Post-Collaudo (RRU)
# Progetto: Minecraft Access (Fork 26.2 / 1.21.x)
# Autore: Luca (Sviluppatore & Collaudatore) & Antigravity (AI Pair Programmer)
# Percorso: docs/report/REGISTRO_REVISIONI.md
# Archivio Storico: docs/report/ARCHIVIO_REVISIONI.md

Questo documento costituisce il **Registro Attivo Snello** del progetto Minecraft Access. Ospita *esclusivamente* le revisioni aperte o in lavorazione. A collaudo positivo confermato da Luca, le voci vengono migrate nell'**Archivio Storico delle Revisioni** (`docs/report/ARCHIVIO_REVISIONI.md`), mantenendo questo file sempre leggero e rapido da consultare con NVDA.

---

## 📋 REVISIONI ATTIVE IN CORSO

---

### 🔵 Rev MC-26.7 — Resilienza & Fallback Traduzioni per Blocchi di Mod Terze (es. Macaw's Doors)
- **Stato**: `[APERTA]`
- **Data Rilevamento**: 2026-09-01
- **Problema Riscontrato (Esperienza Luca)**: In presenza di mod terze (es. Macaw's Doors) prive di localizzazione italiana, il mirino o il raycast vocalizzano la chiave grezza (es. *"Ostacolo di block.mcwdoors.dark_oak_barn_door a 6 blocchi"*).
- **Evidenza Telemetrica / Log**: `Narrating=block.mcwdoors.dark_oak_barn_door`.
- **Causa Radice**: La chiave non ha traduzione in `it_it.json` e il sistema vanilla restituisce la chiave non tradotta.
- **Soluzione di Affinamento (PRAPI)**:
  1. Fallback su lingua inglese (`en_us`) in `ObstacleDetectionUtils` / `WorldNarrator` quando la stringa inizia con `block.` o manca in italiano;
  2. Formattazione leggibile dall'identificatore del blocco (es. estrazione di *"dark oak barn door"* dalla chiave);
  3. Override di dizionario per le mod del modpack ufficiale in `minecraft_access/lang/it_it.json`.
- **Piano Tecnico di Riferimento**: In fase di pianificazione.
- **Esito Collaudo**: In attesa di lavorazione e collaudo.



