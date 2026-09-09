# Registro Attivo delle Revisioni & Affinamenti Post-Collaudo (RRU)
# Progetto: Minecraft Access (Fork 26.2 / 1.21.x)
# Autore: Luca (Sviluppatore & Collaudatore) & Antigravity (AI Pair Programmer)
# Percorso: docs/report/REGISTRO_REVISIONI.md
# Archivio Storico: docs/report/ARCHIVIO_REVISIONI.md

Questo documento costituisce il **Registro Attivo Snello** del progetto Minecraft Access. Ospita *esclusivamente* le revisioni aperte o in lavorazione. A collaudo positivo confermato da Luca, le voci vengono migrate nell'**Archivio Storico delle Revisioni** (`docs/report/ARCHIVIO_REVISIONI.md`), mantenendo questo file sempre leggero e rapido da consultare con NVDA. Ciascuna voce include il puntatore diretto al rispettivo **Report di Sessione** (`docs/report/REPORT_SESSIONE_[TASK].md`), che funge da Single Source of Truth per i file modificati, log e test correlati.

---

## ðŸ“‹ REVISIONI ATTIVE IN CORSO

> [!NOTE]
> **Buffer RRU Post-Strategia Cognitiva (Aggiornamento Convalida Luca)**:
> Su direttiva esplicita di Luca, tutte le revisioni e gli affinamenti oggi presenti in questo Registro vengono **formalmente posticipati fino al completamento e alla convalida della Fase 7**, che conclude tutti i punti della strategia cognitiva. Saranno affrontati nella **Fase 8 â€” Buffer Registro Revisioni Post-Strategia**, ciascuno con il proprio ciclo di verifica e collaudo, prima della validazione finale della Fase 9.

---



### 🔵 Rev MC-26.7 — Resilienza & Fallback Traduzioni per Blocchi di Mod Terze (es. Macaw's Doors)
- **Stato**: `[APERTA â€” DIFFERITA AL BUFFER RRU POST-STRATEGIA]`
- **Data Rilevamento**: 2026-09-01
- **Problema Riscontrato (Esperienza Luca)**: In presenza di mod terze (es. Macaw's Doors) prive di localizzazione italiana, il mirino o il raycast vocalizzano la chiave grezza (es. *"Ostacolo di block.mcwdoors.dark_oak_barn_door a 6 blocchi"*).
- **Evidenza Telemetrica / Log**: `Narrating=block.mcwdoors.dark_oak_barn_door`.
- **Causa Radice**: La chiave non ha traduzione in `it_it.json` e il sistema vanilla restituisce la chiave non tradotta.
- **Soluzione di Affinamento (PRAPI)**:
  1. Fallback su lingua inglese (`en_us`) in `ObstacleDetectionUtils` / `WorldNarrator` quando la stringa inizia con `block.` o manca in italiano;
  2. Formattazione leggibile dall'identificatore del blocco (es. estrazione di *"dark oak barn door"* dalla chiave);
  3. Override di dizionario per le mod del modpack ufficiale in `minecraft_access/lang/it_it.json`.
- **Piano Tecnico di Riferimento**: Da elaborare nella Fase 8 â€” Buffer RRU Post-Strategia.
- **Report di Sessione & File Correlati**: Da associare all'avvio della sessione in Fase 8.
- **Esito Collaudo**: In attesa del completamento della Fase 7 e della lavorazione nel Buffer RRU.

---

### ❤️ Rev MC-26.14 — Integrazione Cognitiva Dominio Vitalità e Stato Fisiologico
- **Stato**: `[PIANIFICATA — STRATEGIA ATTIVA]`
- **Data Apertura**: 2026-09-08
- **Autori**: Luca & Antigravity
- **Oggetto**: Migrazione dei messaggi vitali (`PlayerStatus.java` e `HUDStatus.java`) verso il `CognitiveCoordinator` tramite `StatusCognitiveEventFactory`.
- **Canali di Priorità**:
  - `CRITICAL` (Fast-Path 0 ms): Annegamento imminente, soffocamento in blocchi, fuoco/lava;
  - `OPERATIONAL` (Fine-Tick): Danni improvvisi da mob con provenienza spaziale;
  - `CONTEXTUAL` (Accodabile): Fame a 3 cosciotti, fine effetti pozioni.
- **Riferimento Strategico Master**: [`docs/strategie/attive/STRATEGIA_MIGRAZIONE_DOMINI_LEGACY_RESIDUI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_MIGRAZIONE_DOMINI_LEGACY_RESIDUI.md)
- **Piano Tecnico di Riferimento**: Da redigere in sessione dedicata.

---

### 🌲 Rev MC-26.15 — Integrazione Cognitiva Dominio Ambiente e Indicatori Spontanei
- **Stato**: `[PIANIFICATA — STRATEGIA ATTIVA]`
- **Data Apertura**: 2026-09-08
- **Autori**: Luca & Antigravity
- **Oggetto**: Migrazione delle notifiche ambientali spontanee (`BiomeIndicator`, `TimeIndicator`, `XPIndicator`, `Weather`, `LightLevel`, `FluidDetector`) verso eventi `CONTEXTUAL` o `PASSIVE`.
- **Scopo & Beneficio**: Eliminazione del chatter e dei troncamenti vocali durante la marcia o il combattimento. L'annuncio del bioma o del meteo cede sempre il passo al movimento e alla sicurezza.
- **Riferimento Strategico Master**: [`docs/strategie/attive/STRATEGIA_MIGRAZIONE_DOMINI_LEGACY_RESIDUI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_MIGRAZIONE_DOMINI_LEGACY_RESIDUI.md)
- **Piano Tecnico di Riferimento**: Da redigere in sessione dedicata.

---

### 🎓 Rev MC-26.16 — Integrazione Cognitiva Dominio Didattico & Mentore Contestuale (Fase 6)
- **Stato**: `[PIANIFICATA — STRATEGIA ATTIVA]`
- **Data Apertura**: 2026-09-08
- **Autori**: Luca & Antigravity
- **Oggetto**: Migrazione e coordinamento del `ContextualMentor`, di `Academy` e di `HelpNarrator` nel sistema cognitivo centrale.
- **Scopo & Beneficio**: I consigli didattici e i tutorial vengono subordinati alla sicurezza e alla navigazione attiva. Se il mentore parla e sopraggiunge un ostacolo o un cambio rotta, la voce didattica cede il passo all'istante senza sovrapporsi.
- **Riferimento Strategico Master**: [`docs/strategie/attive/STRATEGIA_MIGRAZIONE_DOMINI_LEGACY_RESIDUI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_MIGRAZIONE_DOMINI_LEGACY_RESIDUI.md)
- **Piano Tecnico di Riferimento**: Da redigere in sessione dedicata.

---

### 🎯 Rev MC-26.17 — Integrazione Cognitiva Dominio Radar Passivo Mob e POI Ambientali
- **Stato**: `[PIANIFICATA — STRATEGIA ATTIVA]`
- **Data Apertura**: 2026-09-08
- **Autori**: Luca & Antigravity
- **Oggetto**: Orchestrazione del rilevamento passivo di entità ostili e POI ambientali (`ObjectTracker`, `POIEntities`, `POIMarking`).
- **Scopo & Beneficio**: Segnalazione discreta e non invasiva della presenza di mostri nel perimetro ($< 6$ metri con priorità `OPERATIONAL`) senza intralciare il feed del mirino.
- **Riferimento Strategico Master**: [`docs/strategie/attive/STRATEGIA_MIGRAZIONE_DOMINI_LEGACY_RESIDUI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_MIGRAZIONE_DOMINI_LEGACY_RESIDUI.md)
- **Piano Tecnico di Riferimento**: Da redigere in sessione dedicata.


