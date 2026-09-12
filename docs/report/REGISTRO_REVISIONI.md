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



### 🧗 Rev MC-26.22 — AutoWalk Verticale & Assistente Tattico di Scalata (Climb Assistant)
- **Stato**: `[/] IMPLEMENTATI CONTRATTI D30..D40 — SUITE VERDE (396 TEST), DEPLOY ESEGUITO; IN ATTESA DEL 7° COLLAUDO IN-GAME DI LUCA & PROPOSTA BONIFICA D41`
- **Piano operativo corrente**: [Transizioni verticali e landing, D30–D41](../piani/attivi/PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md).
- **Report di Handover**: [Sezione 6 nel rapporto](REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md). JAR installato SHA-256 `5F3479D81F05869EC80849748D1005634D889030F6805A6F079AD5930999BA01`.
- **Data Apertura**: 2026-09-11
- **Autori**: Luca, Antigravity & GPT Codex
- **Oggetto**: Estensione dell'AutoWalk ai dislivelli verticali (scale a pioli, impalcature, rampicanti) e creazione del semi-autowalk a comando (Climb Assistant) per salita/discesa automatica fino al pianerottolo sicuro calpestabile.
- **Canali di Attivazione**: Tasto Interazione contestuale (`keyUse` / `KEY_RBRACKET`) con bypass su `Sneak`, Tasto dedicato `Alt+S` (S = Scala / Salita-Scendi) e voce in Access Menu.
- **Risoluzione Difetto Cinematica Salita & Discesa Manuale (Contratti D0..D9 - Iterazione 1)**:
  1. `ClimbKinematics.java`: modulo decisionale puro headless (Mount con 3 tick jump pulse, Transit progressivo per-rung, Dismount fine colonna + quota continua).
  2. `ClimbRouteAssembler.java`: costruttore e validatore topologico unificato della sequenza `MOUNT -> TRANSIT per-rung -> DISMOUNT`.
  3. `AutoWalkMotor.java`: bonifica dei metodi inline concorrenti, delegazione deterministica a `ClimbKinematics` e avanzamento waypoint protetto da revisione.
  4. `RouteNavigator.java`: installazione atomica del target (`landingPos`) e avanzamento protetto da revisione.
- **Risoluzione Difetto Top Descent Mount AutoWalk (Contratti D0..D8 - Iterazione 2 - SUCCESSO TOPOLOGICO)**:
  1. `ClimbEntryTransition.java`: record immutabile condiviso con semantica di transizione d'ingresso e supporto a botole opzionali.
  2. `ClimbTraversalAnalyzer.java`: helper puro `resolveTopDescentMount` con swept path, verifica facce parete, apertura libera e bottom landing continuo.
  3. `AutoWalkPathfinder.java`: introduzione di `SearchStateKey(pos, mode, columnId)` con modalità `WALK`, `CLIMB_UP`, `CLIMB_DOWN`; regola A.3 Top Descent Mount per connettere la superficie superiore al primo piolo sottostante; preservazione di `climbLeg` in `PathNode` e `NeighborMove`.
  4. Telemetria del 3° collaudo: Pathfinding globale dal tetto a Y=85 verso il piano terra/balconata a Y=81 completato con successo (`FOUND 52..83 nodi`) e avvio discesa (*"Discesa scala avviata"*).
- **Risoluzione Stallo Cinematico Discesa (Contratti D10..D15 - Iterazione 3 - COMPLETATA)**:
  1. **Contratto D10 (Politica Cinematica di Discesa)**: In discesa su scala a pioli/rampicanti (`WALL_MOUNTED`/`FREE`), il movimento verticale discendente avviene per caduta gravitazionale naturale controllata (`GRAVITY_DESCENT`). Nessuna iniezione di `keyDown` o propulsione artificiale.
  2. **Contratto D11 (Unico Proprietario Runtime Lease & Sneak)**: `ProximityFallDetector` e `AutoWalkMotor` condividono l'istanza singleton `SafetyMovementGuard.getDefaultInstance()`. La lease sopprime il falso freno anticaduta sulla specifica colonna durante Mount, Transit e Dismount.
  3. **Contratto D12 (Ordine Atomico Lease-Prima-Input)**: `AutoWalkMotor` acquisisce e rinnova la lease su `ControlledDescentPort` PRIMA di applicare qualsiasi comando virtuale client. Per scale a muro `requiresSneak = false` (rilascia lo sneak sintetico permettendo la discesa); per impalcature `requiresSneak = true`.
  4. **Contratto D13 (Progresso Watchdog Direzionale Firmato)**: In salita $dy = playerY - lastObservedY \ge 0.05\text{ m}$; in discesa $dy = lastObservedY - playerY \ge 0.05\text{ m}$. Nessun reset erroneo del watchdog per oscillazioni inverse.
  5. **Contratti D14/D15 (Test di Regressione & Deploy)**: 381 test totali verdi, compilazione `shadowJar` eseguita e sincronizzata in entrambe le istanze di gioco PrismLauncher.
- **Riferimento Strategico Master**: [`docs/strategie/attive/STRATEGIA_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md)
- **Piani tecnici precedenti, superati**:
  - Salita/Cinematica: [`docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_SCALATA_VERTICALE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_SCALATA_VERTICALE.md)
  - Discesa AutoWalk: [`docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md)
- **Handover Ingegneristico per Codex**: [`docs/report/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md)
- **Peer review e revisione Codex del 2026-09-12**: il codice ricevuto non implementava la Sezione 21 nella forma convalidata nonostante la suite verde. Codex ha introdotto `ClimbContactProbe`, swept AABB, FSM `ALIGN/APPROACH/CAPTURE_WAIT`, bottom crossing prioritario, landing volumetrico su due tick e telemetria reason-coded. Evidenze e direttive sono nelle Sezioni 21–22 del piano e nell'Appendice 4 dell'handover. JAR installato SHA-256 `56284E5478719016DB25E9F4699D18038A3C98ECB8EDF5A3B5956D6D6B249217`.
- **Esito 6° Collaudo & Diagnosi Antigravity (2026-09-12)**: Rilevata regressione nel dismount in salita alla sommità (`OUTSIDE_COLUMN_ABORT` per rilascio prematuro di `keyUp` indotto da `ClimbLandingProbe`). Analisi forense, log di gioco e strategia correttiva formalizzati nel [Report di Handover (Sez. 5)](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md) e nella [Strategia Correttiva](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_CORRETTIVA_DISMOUNT_SALITA_E_DISCESA.md).

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


