# Registro Attivo delle Revisioni & Affinamenti Post-Collaudo (RRU)
# Progetto: Minecraft Access (Fork 26.2 / 1.21.x)
# Autore: Luca (Sviluppatore & Collaudatore) & Antigravity (AI Pair Programmer)
# Percorso: docs/report/REGISTRO_REVISIONI.md
# Archivio Storico: docs/report/ARCHIVIO_REVISIONI.md

Questo documento costituisce il **Registro Attivo Snello** del progetto Minecraft Access. Ospita *esclusivamente* le revisioni aperte o in lavorazione. A collaudo positivo confermato da Luca, le voci vengono migrate nell'**Archivio Storico delle Revisioni** (`docs/report/ARCHIVIO_REVISIONI.md`), mantenendo questo file sempre leggero e rapido da consultare con NVDA.

---

## 📋 REVISIONI ATTIVE IN CORSO

> [!NOTE]
> **Buffer RRU Post-Strategia Cognitiva (Aggiornamento Convalida Luca)**:
> Su direttiva esplicita di Luca, tutte le revisioni e gli affinamenti oggi presenti in questo Registro vengono **formalmente posticipati fino al completamento e alla convalida della Fase 7**, che conclude tutti i punti della strategia cognitiva. Saranno affrontati nella **Fase 8 — Buffer Registro Revisioni Post-Strategia**, ciascuno con il proprio ciclo di verifica e collaudo, prima della validazione finale della Fase 9.

---

### 🔵 Rev MC-26.7 — Resilienza & Fallback Traduzioni per Blocchi di Mod Terze (es. Macaw's Doors)
- **Stato**: `[APERTA — DIFFERITA AL BUFFER RRU POST-STRATEGIA]`
- **Data Rilevamento**: 2026-09-01
- **Problema Riscontrato (Esperienza Luca)**: In presenza di mod terze (es. Macaw's Doors) prive di localizzazione italiana, il mirino o il raycast vocalizzano la chiave grezza (es. *"Ostacolo di block.mcwdoors.dark_oak_barn_door a 6 blocchi"*).
- **Evidenza Telemetrica / Log**: `Narrating=block.mcwdoors.dark_oak_barn_door`.
- **Causa Radice**: La chiave non ha traduzione in `it_it.json` e il sistema vanilla restituisce la chiave non tradotta.
- **Soluzione di Affinamento (PRAPI)**:
  1. Fallback su lingua inglese (`en_us`) in `ObstacleDetectionUtils` / `WorldNarrator` quando la stringa inizia con `block.` o manca in italiano;
  2. Formattazione leggibile dall'identificatore del blocco (es. estrazione di *"dark oak barn door"* dalla chiave);
  3. Override di dizionario per le mod del modpack ufficiale in `minecraft_access/lang/it_it.json`.
- **Piano Tecnico di Riferimento**: Da elaborare nella Fase 8 — Buffer RRU Post-Strategia.
- **Esito Collaudo**: In attesa del completamento della Fase 7 e della lavorazione nel Buffer RRU.

---

### 🟡 Rev MC-26.8 — Interruttore Diagnostico del Cognitive Coordinator (Ctrl+Alt+C)
- **Stato**: `[DIFFERITA AL BUFFER RRU POST-STRATEGIA — FASE 8]`
- **Data Revisione**: 2026-09-04
- **Pianificazione Operativa (Aggiornamento Luca)**: Posticipata fino alla chiusura e convalida della Fase 7, quindi affrontata nel Buffer RRU della Fase 8, prima della validazione finale.
- **Ramo Git**: `feat/cognitive-orchestrator`
- **Verifica Funzionale Scale (Collaudo Luca)**: Il problema storico dello sticky-sneak sulle scale a pioli a parete è **risolto**: davanti a una scala il sistema vocalizza correttamente *"discesa sicura"* e il giocatore può attraversare la scala e scendere liberamente.
- **Componenti Congelati e Protetti (Zero Modifiche)**:
  - Nessuna modifica a `FallDetector`, `TraversalSafetyAnalyzer`, `SafetyMovementGuard` o ai relativi test;
  - Nessuna introduzione di `TraversalSafetyEventFactory` (evitata sovraingegnerizzazione);
  - Nessuna rimozione preventiva dei rami storici di `calculateDangerousDrop`;
  - Nessuna macchina a stati aggiuntiva per la transizione sulle scale;
  - Fase 3B (`ObstacleDetector`, mirino) rigorosamente confermata come chiusa e protetta.
- **Scope Operativo Esclusivo di MC-26.8**:
  1. Interruttore diagnostico globale volatile di sessione per `CognitiveCoordinator`: combinazione `Ctrl+Alt+C` registrata nella categoria esistente `KeyMappingCategories.OTHER`;
  2. Stato solo di sessione in memoria (nessuna scrittura nella configurazione salvata su disco);
  3. Svuotamento atomico immediato di buffer di tick, code, memorie e scudi (`clearAllBuffers()`) a ogni commutazione (ON -> OFF e OFF -> ON);
  4. Instradamento dinamico: bypass dell'arbitraggio, con inoltro diretto ai consumatori legacy configurati quando disattivato, e ripresa dell'arbitraggio centrale quando attivato;
  5. Notifica vocale diretta tramite `MainClass.narrate(msg, true)`, indipendente dall'arbitraggio cognitivo;
  6. Suite di test dedicati per il toggle e il routing A/B cognitivo/legacy.
- **Piano Tecnico di Riferimento**: `docs/piani/attivi/PIANO_TECNICO_REV_MC-26.8_TRAVERSAL_SAFETY_ANALYZER.md` (allineato e differito al Buffer RRU post-strategia).
- **Esito Collaudo**: In attesa di avvio lavorazione nella Fase 8.

---

### 🟣 Rev MC-26.9 — Interruttore Maestro del Modulo Tastierino Numerico (NumpadControls)
- **Stato**: `[PIANIFICATA / DIFFERITA AL BUFFER RRU POST-STRATEGIA]`
- **Data Rilevamento**: 2026-09-04
- **Oggetto**: Interruttore maestro globale per abilitare/disabilitare l'intero modulo Tastierino Numerico (`NumpadControls`).
- **Scorciatoia Definitiva**: `Ctrl+Alt+F8` (binding Kuma con `InputConstants.KEY_F8` e modificatori `CONTROL + ALT`; sostituisce definitivamente ogni ipotesi su NumLock o Numpad +).
- **Scopo & Requisiti**:
  1. Consentire all'utente di spegnere/accendere completamente l'ascolto e l'elaborazione del tastierino numerico in tempo reale;
  2. Quando disattivato, nessun input da tastierino numerico deve essere intercettato o consumato dal mod, consentendo il comportamento libero o nativo di Minecraft;
  3. Fornire feedback vocale e sonoro immediato all'attivazione e disattivazione;
  4. Nessuna interferenza o modifica al codice durante la stabilizzazione del coordinatore cognitivo.
- **Differimento Formale & Vincolo Inderogabile**: Nessuna implementazione prima del completamento e della convalida formale della Fase 7; revisione differita alla Fase 8, dopo tutti i punti della roadmap cognitiva.
- **Vincolo di Sessione**: Zero modifiche a `NumpadControls` o al relativo package nella sessione corrente.
- **Piano Tecnico di Riferimento**: Da redigere nella sessione dedicata.

---

### ⚪ Rev MC-26.10 — Perfezionamento Soglia Dislivello Minimo per Annuncio Discesa Sicura
- **Stato**: `[PIANIFICATA / DIFFERITA AL BUFFER RRU POST-STRATEGIA]`
- **Data Rilevamento**: 2026-09-04
- **Problema Riscontrato**: La vocalizzazione *"discesa sicura"* viene talvolta annunciata anche in presenza di dislivelli minimi e non significativi lungo il cammino.
- **Azione di Affinamento Futura (PRAPI)**:
  1. Riprodurre con precisione lo scenario in-game rilevando coordinate, dislivello $\Delta Y$ esatto e blocchi coinvolti;
  2. Implementare una condizione/soglia mirata con test dedicato per silenziare l'annuncio superfluo senza intaccare in alcun modo l'attuale comportamento protettivo e l'attraversamento delle scale;
  3. Non intervenire in MC-26.8 per evitare regressioni o sovraingegnerizzazione su un comportamento funzionale già collaudato positivamente.
- **Piano Tecnico di Riferimento**: Da redigere nella Fase 8 — Buffer RRU Post-Strategia.
