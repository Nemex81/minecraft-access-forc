# Diario Modifiche Personali & Contributi Fork (AVF)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
# Progetto: Minecraft Access (Fork di nemex81 / Luca)
# Ambito: Cronologia Feature Personali & Disciplina I18N
# Riferimento Standard: Astralis Versioning Framework (AVF 3+1 Livelli)
# Baseline di Partenza: v26.2-1.12.0 (Fork Inception a inizio Agosto 2026)

Questo documento costituisce il **Diario Ufficiale delle Modifiche del Fork Personale in lingua Italiana**.
Poiché il `README.md` pubblico e la documentazione del repository upstream rimangono in lingua Inglese per la community internazionale con la sola sezione `## [Unreleased]`, tutte le novità, i refactoring e i miglioramenti sviluppati sui nostri rami (`mymaster`, `dev`) vengono tracciati qui secondo la disciplina AVF (`V.A.R[.M]`).

## 🚀 [v26.2-1.19.0-dev] — 2026-09-04 (Refactor Architetturale Cognitive Coordinator — Fasi 1, 2, 3, 4 — Branch feat/cognitive-orchestrator)

### 🧠 Fase 1: Nucleo Cognitivo Centralizzato Certificato (Commit e41c3f9d)
- **Fast-Path Emergenze a 0 ms**: Elaborazione immediata per eventi `CRITICAL` con micro-burst accodato per eventi critici concorrenti nel medesimo tick (prevenzione troncamento prime sillabe salvavita).
- **Arbitraggio Deterministico a Fine Tick**: Matrice gerarchica dinamica a 4 priorità (`CRITICAL`, `OPERATIONAL`, `CONTEXTUAL`, `PASSIVE`).
- **Scudo Critico Vincolante (`criticalShieldUntil`)**: Soppressione totale dei messaggi non critici per 1500 ms con custodia sicura degli `OPERATIONAL` in `shortQueue` ed emissione differita automatica.
- **Fusione Vocale Vincolata a I18N (`SpatialDirection`)**: Concatenazione ammessa unicamente con template I18N semantico autorizzato (`minecraft_access.cognitive.join_*`) e coerenza spaziale (stessa direzione o omni). Divieto assoluto di fallback hardcoded con punteggiatura fissa; differimento del secondario valido in coda breve.
- **14 Test Unitari Deterministiche a 0 ms**: Suite completa con iniezione temporale controllata.

### ⚙️ Fase 2: Configurazione Cloth Config & Facciata Retrocompatibile (Commit 88c3ddb7, 580c060a)
- **Categoria Cloth Config `cognitiveCoordinator`**: Gestione unificata con binding runtime di `cognitiveCoordinatorEnabled`, `chainedNarrationEnabled` e normalizzazione `deduplicationWindowMs` (500–5000 ms). Rinvio trasparente delle opzioni non ancora attive (anti-pattern controlli decorativi per screen reader).
- **Facciata `NarrationPriority` Trasparente**: Conservazione integrale delle 4 firme legacy, rimozione del blocco catch-all `Throwable` e introduzione di seam package-private dedicati (`scannerSuppressor`, `narrationConsumer`, `timeSupplier`) per test deterministici headless.
- **Localizzazioni IT/EN Rigorosamente Alfabetiche**: 7 nuove chiavi configurative e tooltip conformi ai controlli CI.
- **8 Nuovi Test Unitari di Fase 2**: Test mirati sulla facciata e sul binding configurativo (22 test cognitivi totali superati, intera suite del progetto verde in 21s).

### 🛡️ Fase 3: Migrazione Pilota Dominio Sicurezza (3A FallDetector & 3B ObstacleDetector)
- **Pilota 3A (`FallDetector`)**: Migrazione degli avvisi burrone e ciglio a `CognitiveEvent` con priorità `CRITICAL` / `OPERATIONAL`, preservando integra la logica di auto-sneak e il bypass per elementi arrampicabili e discesa assistita su scale a pioli (`Rev MC-26.8`).
- **Pilota 3B (`ObstacleDetector`)**:
  - **Factory Pura Eventi (`ObstacleSafetyEventFactory`)**: Normalizzazione angolare simmetrica in $[0^\circ, 360^\circ)$ su `SpatialDirection` (`FORWARD`, `RIGHT`, `BACK`, `LEFT`), generazione deterministica del `SoundCue` condiviso (`NOTE_BLOCK_PLING` 1.5f per `STEP_CLIMBABLE`, `NOTE_BLOCK_BASS` 0.6f per barriere) ed emissione con priorità `CONTEXTUAL` e TTL 2500 ms.
  - **Compositore di Testo Puro (`ObstacleNarrationComposer`)**: Utility condivisa per la formattazione dei messaggi ostacoli con distanza e mirino, identica tra percorso cognitivo e percorso legacy per tutte le modalità (`FOUR_DIRECTIONS`, `EIGHT_DIRECTIONS`, `OMIT_FORWARD`, `OFF`).
  - **Snapshot Contesto Mirino (`ObstacleNarrationContext`)**: Record immutabile per snapshot in sola lettura di target e distanza corrente.
  - **Armonizzazione Mirino (`CrosshairFeedbackManager`)**: Finestra temporale di soppressione monotona `suppressAutomaticMovementFeedback(100ms)` con `Math.max` e assorbimento silenzioso (`absorbAutomaticMovementFeedbackIfSuppressed`), eliminando qualsiasi doppia voce o annuncio arretrato durante il movimento, senza toccare la reattività istantanea dei comandi manuali (`Alt+V`, `B`).
  - **Doppio Percorso Deterministico**: Inoltro al `CognitiveCoordinator` se attivo, oppure bypass legacy con `legacyVoiceConsumer` e `legacyAudioConsumer` (con passaggio del `Level` locale e identico `SoundCue`).
  - **Suite di Test & Collaudo In-Game**: 29 test specifici aggiunti (totale 185 test del progetto al 100% verdi) e validazione sul campo completata con successo (oltre 1h 12m di gioco continuo senza warning o errori).

### 🎛️ Bonifica Anomalie GUI Post-Collaudo (Rev MC-26.9 & Rev MC-26.10 — Commit 6413d721)
- **Rev MC-26.9 (NullPointer Guard & Anti-Ghost in `InventoryControls`)**:
  - Predicato centrale `isActiveContainerScreen()` con verifica rigorosa di identità d'istanza (`activeScreen instanceof AbstractContainerScreen && activeScreen == currentScreen`).
  - Sincronizzazione ciclo di vita in `tick()` prima del debounce dell'intervallo con `clearNavigationState()`.
  - Guard a monte sui 18 handler Kuma e su tutti i metodi di navigazione/focus (`changeGroup`, `selectGroup`, `focusSlotItemAt`, `focusSlotItem`, `changeRecipeTab`, `changeCreativeInventoryTab`, `narrateRecipeInfo`).
  - Guard a valle in entrambi gli overload di `moveToSlotItem` (`if (slotItem == null || !isActiveContainerScreen()) return;`).
  - Inizializzazione difensiva di `interval` con `Interval.ms(150)` per disaccoppiamento totale dal ciclo di vita di `Config`.
  - 6 nuovi test unitari in `InventoryControlsLifecycleTest` superati al 100%.
- **Rev MC-26.10 (Soppressione Shift Sneak Hijack in GUI)**:
  - `RawCrouchIntentProvider` mantenuto puro al 100% come lettore hardware GLFW (Single Responsibility).
  - Metodo `suspendForGui()` in `SafetyMovementGuard` con ownership token rigoroso: rilascia il crouch con `applyIfChanged(false)` solo se `systemOverrideActive` era vero, senza toccare la postura manuale né interrogare il probe hardware.
  - Routing prioritario in `FallDetector.tick`: se `client.gui.screen() != null`, esecuzione immediata di `resetSafetyStateForGui()` (che invoca `suspendForGui()`), disaccoppiata dal reset ordinario nel mondo (`resetSafetyState()`).
  - Revoca immediata di `currentAllowedDescentId` e ripresa trasparente dello Shift manuale una volta chiusa la schermata GUI.
  - 6 nuovi test unitari in `SafetyMovementGuardTest` superati al 100%.

### 🎯 Fase 4: Migrazione Esplorazione, Mirino Automatico & POI Cognitivi (Commit b05ea8f9, 80c8d66d, 4bc424c3)
- **Sotto-Fase 4A — Gate di Rollout & Reset di Sessione (Commit `b05ea8f9`)**:
  - Introduzione del gate condizionale `explorationCognitiveRoutingEnabled` (disattivato per default): l'instradamento cognitivo dell'esplorazione si attiva solo se sia l'impostazione globale `cognitiveCoordinatorEnabled` sia questo gate sono attivi.
  - Reset deterministico di sessione: azzeramento atomico di buffer, scudi e memorie brevi del `CognitiveCoordinator` su cambio dimensione, morte e respawn del giocatore.
- **Sotto-Fase 4B — Routing Cognitivo Mirino Automatico & ID Canonici (Commit `80c8d66d`)**:
  - Creazione di `CrosshairExplorationEventFactory`: costruzione pura di record `CognitiveEvent` con dominio `EXPLORATION` e priorità `PASSIVE` per il feed automatico in movimento.
  - Identità canonica immutabile basata su tipo blocco/entità e bucket di coordinate voxel; deduplicazione deterministica per prevenire il chatter vocale durante il cammino.
  - Fallback trasparente: conservazione integrale del percorso legacy diretto con `interrupt=true` in caso di coordinatore disattivato.
- **Sotto-Fase 4C — DirectInteractionShield per Comandi Espliciti & Radar POI (Commit `4bc424c3`)**:
  - Implementazione di `DirectInteractionShield`: scudo temporale dedicato per le interazioni esplicite dell'utente.
  - Protezione a latenza zero per comandi manuali: lettura mirino su tasto `B`, centramento/livellamento visuale orizzonte e comandi radar/lock POI (`X`).
  - Gli annunci espliciti mantengono priorità assoluta e vocalizzazione immediata con `interrupt=true`, impedendo qualsiasi soppressione o ritardo da parte del feed passivo del mirino.
- **Suite di Test & Collaudo In-Game**:
  - 23 nuovi test unitari deterministici (suite totale portata a 208 test JUnit verdi, 0 failure, 0 error).
  - Collaudo empirico sul campo con NVDA superato al 100%: navigazione fluida tra blocchi, tracciamento dinamico e abbattimento mucca con radar POI, raccolta drop (`Cuoio`, `Manzo crudo`), rotazioni di sguardo istantanee e zero eccezioni di runtime.

---

## 🚀 [v26.2-1.18.0] — 2026-09-02 (Feedback Dislivello Adattivo, Verbosità Faccia, Micro-Voxel Raymarch & Armonizzazione SSOT Mirino/Ostacoli)

### 🌟 Feedback Dislivello Adattivo & Altezza Cubi (Rev MC-29.0)
- **4 Modalità Operative (`SoundCueMode`)**: `SOUND_AND_VOICE`, `SOUND_ONLY`, `VOICE_ONLY`, `OFF` configurabili in Cloth Config.
- **3 Stili Vocali (`NarrationStyle`)**: `DESCRIPTIVE` (*"1 blocco sopra"*), `COMPACT` (*"+1Y"*), `DELTA_ONLY` (*"+1"*).
- **Toggle Quota Zero**: `narrateSameLevel` per escludere o includere la pronuncia a livello del terreno (*"Stesso livello"*).
- **Calcolo Deterministico**: Calcolo matematico di $\Delta Y = Y_{\text{target}} - Y_{\text{player\_feet}}$ su blocchi ed entità.

### 🧱 Regolatore di Verbosità Faccia del Blocco (Rev MC-29.1)
- **4 Modalità (`BlockFaceVerbosity`)**: `DESCRIPTIVE` (*"lato ovest"*), `TOP_BOTTOM_ONLY`, `COMPACT`, `OFF`.
- Localizzazioni complete in Italiano (`it_it.json`) e Inglese (`en_us.json`) con ordinamento alfabetico crescente.

### 🎯 Architettura SSOT & Centralizzazione Mirino (Rev MC-29.2, Rev MC-29.3, Rev MC-29.4)
- **`CrosshairFeedbackManager.java` (Presentation Coordinator)**: Single Source of Truth per la sincronizzazione dei canali di puntamento (Tick movimento, Centramento orizzonte tasto 5/M, Lettura manuale B).
- **Bonifica a 5 Barriere**: Eliminazione di dead code, campi e metodi legacy, soppressione del doppio raycast in `MinecraftAccess.narrate`.
- **Armonizzazione Concorrenza**: Soppressione dei loop vocali da fermi e protezione atomica delle transizioni.

### 👣 Podometro di Cadenza & Aggancio Volumetrico Voxel Lamine Sottili (Rev MC-29.5, Rev MC-29.6)
- **Podometro Ritmico**: Feedback di cadenza continuo metro per metro lungo le pareti (*"Assi di quercia, a 1 blocco"*).
- **Micro-Voxel Raymarch Continuo ($0.05\text{m}$)**: Campionamento a passo $0.10\text{m}$ a partire da $d = 0.05\text{m}$ per porte, vetri, staccionate e sbarre, efficace anche a coordinate negative ($X < 0$).
- **Dispacciamento Diretto Ostacoli (`onObstacleDetected`)**: Invocazione diretta da `ObstacleDetector` a `CrosshairFeedbackManager` con sincronia assoluta audio/voce e armonizzazione orizzontale colonna unica $XZ$ (*"Davanti: Ostacolo di Pannello di vetro, a 3 blocchi"*).

---

## 🚀 [v26.2-1.17.1] — 2026-09-02 (Salto Automatico Pilota & Auto-Focus Menu di Pausa Esc)

### 🌟 Pilota Automatico & Movimento (Rev MC-28.0)
- **Calibrazione Fisica Salto Automatico (`AutoWalkController.java`)**:
  - Superata la soglia di compenetrazione impossibile `distH < 0.65` con una finestra di approccio naturale $\text{distH} \le 1.25\text{ m}$ o contatto d'impatto con la parete (`player.horizontalCollision`).
  - Spinta verticale prolungata a `jumpHoldingTicks = 4` (200ms) per garantire il superamento completo del gradino $+1$.
  - Rispetto assoluto della guardia Cloth Config: se `config.autoJump == false`, il pilota non salta e si arresta regolarmente per il controllo manuale.

### 🎛️ Menu & Accessibilità Tastiera (Rev MC-28.1)
- **Auto-Focus Immediato Menu di Pausa `Esc` (`MenuFix.java`)**:
  - Inclusione di `PauseScreen.class` in `MENUS_NEED_FIX`.
  - Posizionamento automatico del focus logico sul primo pulsante attivo (*"Torna al gioco"*) all'apertura dello schermo con `ensureInitialFocus(screen)`.
  - Navigazione con le 4 Frecce (Su/Giù) immediatamente operativa al primo tocco senza dover premere `Tab`.

---

## 🚀 [v26.2-1.17.0] — 2026-09-02 (Feed Mirino WASD, Riqualificazione Tasto B, Alt+B & Mentor Adattivo)

### 🌟 Mirino, Movimento & Tasti Rapidi
- **Feed Mirino in Movimento (`WASD`)**:
  - Tracciamento automatico dei blocchi sui passi laterali (`A`/`D`) e della distanza sui passi frontali (`W`/`S`) in `NarrateCrosshair.java`.
  - Nuove modalità in Cloth Config (`MovementFeedbackMode`): `TARGET_AND_DISTANCE` (default), `TARGET_ONLY`, `FULL_FORMAT` e `OFF`.
- **Riqualificazione Tasto `B` (Mano Sinistra)**:
  - Comando rapido a 1 tocco per la lettura istantanea e completa del mirino integrato in `CrosshairFeedbackManager.java` con supporto al vuoto (*"Nessun bersaglio nel mirino"*).
- **Blindatura Tracciatore Risorse (`Alt + B`)**:
  - Separazione netta dei modificatori in Kuma/GLFW per garantire piena parità di funzionamento con `Alt + Numpad 7`.
- **Armonizzazione con `ObstacleDetector`**:
  - Finestra di grazia preventiva (250ms) per azzerare troncamenti vocali in cammino.

### 🧠 Mentor Vocale Adattivo (Rev MC-27.1)
- **Direzione Spaziale Dinamica**: Riconoscimento dell'asse reale WASD di collisione (*"a sinistra"*, *"a destra"*, *"davanti"*, *"dietro"*).
- **Keybinding Introspection**: Risoluzione dinamica dei tasti a runtime da Minecraft/Kuma per Salto (*"Spazio"*) e Ispezione Ostacolo (*"Alt + V"*).
- **Eliminazione Prefisso Spurio**: Risoluzione diretta `I18n.get(key, args)` senza artefatti di formato.

---

## 🚀 [v26.2-1.16.1] — 2026-09-02 (Feedback Eventi Visivi & Auto-Focus Schermate Specialistiche)

### 🌟 Accessibilità & Interfacce Specialistiche (GUI)
- **Tagliapietre (`StonecutterScreen`)**:
  - Vocalizzazione dinamica delle forme disponibili (*"%d forme disponibili per il taglio"*).
  - Auto-focus automatico sul primo elemento tagliabile (`recipesGroup`) all'inserimento del blocco.
- **Telaio (`LoomScreen`)**:
  - Tracciamento e annuncio motivi disponibili (*"%d motivi disponibili per lo stendardo"*) all'inserimento di stendardo e colorante.
  - Selezione automatica del gruppo motivi per la navigazione immediata.
- **Fornaci & Alambicco (`AbstractFurnaceMenu`, `BrewingStandMenu`)**:
  - Notifiche vocali discrete al completamento del ciclo di cottura (*"Cottura completata"*) e distillazione (*"Distillazione completata"*).
- **Nuovo Helper di Navigazione**: Metodo `selectGroupByKey(groupKey, interrupt)` in `InventoryControls.java` per l'aggiornamento e l'atterraggio istantaneo sul gruppo richiesto.

---

## 🚀 [v26.2-1.16.0] — 2026-09-02 (Avanzamento GUI Ricettario 26.2, 4 Frecce & Stabilità)

### 🌟 Accessibilità & Interfacce (GUI)
- **Navigazione Ricettario Avanzata (`InventoryControls.java`)**:
  - *Cambio Categoria (`V` / `Shift+V`)*: Suono click, risoluzione dinamica nomi in italiano (*"Costruzione"*, *"Attrezzatura"*, *"Varie"*, *"Meccanismi e Redstone"*), auto-focus sulla prima ricetta e annuncio statistiche aggregate.
  - *Cambio Pagina (`Shift+I` / `Shift+K`)*: Accessor Mixin `RecipeBookPageAccessor`, conteggio dinamico pagine e gestione intelligente dei limiti (*"Prima pagina"*, *"Ultima pagina"*, *"Unica pagina"*).
  - *Concordanza Singolare/Plurale*: Flessione grammaticale dinamica in italiano e inglese per conteggi unitari ($1$) e multipli ($>1$).
  - *Navigazione Griglia a 4 Frecce*: Supporto nativo per le 4 Frecce Direzionali su tutti i container con disaccoppiamento automatico dalle caselle di testo `EditBox`.

### 🐞 Bugfix & Stabilità
- Risolta `ClassCastException` al cambio scheda nel ricettario (`Rev MC-26.0A`).
- Risolto warning Cloth Config con annotazione `@ConfigEntry.Gui.Excluded` su singleton `Config.instance` (`Rev MC-26.0B`).
- Guardie difensive anti-NPE su gruppi slot inventario.

---

## 🚀 [v26.2-1.15.0] — 2026-08-28 (Sonda Direzionale, Survival Tracker, Sentinella Minacce & Occlusione 5 Livelli)

### 🌟 Movimento, Suono & Consapevolezza Tattica
- **Sonda Direzionale di Percorso (`DirectionalPathScanner`)**: Scansione progressiva a passo singolo con rilevamento automatico di terreni zappati (`farmland`) e stadi di crescita delle colture (Layer 3 Numpad).
- **Survival Resource Tracker (`Alt+B` / `Alt+Numpad 7`)**: Conteggio acustico istantaneo delle scorte vitali (cibo, attrezzi, blocchi solidi, frecce) con isolamento dei modificatori.
- **Sentinella Minacce Ostili & Sicurezza Fluidi**: Protezione del `FallDetector` su margini fluidi (lava/acqua), sentinella proattiva mob ostili e navigazione intelligente delle porte.
- **Occlusione Sonora a 5 Livelli**: Smorzamento acustico raycast attraverso materiali (Legno, Pietra, Vetro, Metallo, Fogliame) e vocalizzazione bersagli "dietro la parete".
- **Footstep Proprioception**: Regolazione del volume dei passi del giocatore locale con tasti rapidi `Alt+PageUp` / `Alt+PageDown`.
- **Crosshair Feedback Manager**: Coordinatore modulare a token per la vocalizzazione atomica di coordinate voxel, orientamento cardinale, proprietà dei blocchi e livello di luce.

---

## 🚀 [v26.2-1.14.0] — 2026-08-20 (Accademia Novizi, Mentor Contestuale & Bussola Acustica 360°)

### 🌟 Apprendimento & Orientamento Spaziale
- **Onboarding Interattivo & Accademia Novizi**: Missioni tutorial guidate per movimento, orientamento, raccolta legna, crafting e combattimento con guardrail di sicurezza per modalità di gioco.
- **Contextual Mentor & Snapshot Engine**: Motore di campionamento del contesto in tempo reale (`PlayerContextEngine`) e supporto audio gentile per situazioni di blocco, collisione, buio e fame.
- **Priority Speech Shield (`HelpNarrator`)**: Protezione delle istruzioni tutorial vocali dal troncamento causato da scanner o eventi secondari.
- **Bussola Acustica Continua a 360°**: Calcolo dinamico dell'angolo ($0^\circ \dots 359^\circ$) con modulazione di pitch e frequenza per l'orientamento a 360 gradi.
- **Telecamera Diagonale 2D**: Mappatura controlli a passo fisso e centratura orizzonte.

---

## 🚀 [v26.2-1.13.0] — 2026-08-10 (AutoWalk, Gestione Waypoints & Numpad Ergonomico)

### 🌟 Sistemi Core di Navigazione
- **Motore AutoWalk & Pathfinding**: Camminata automatica assistita con calcolo del percorso continuo e superamento ostacoli in tempo reale (`AutoWalkController`, `AutoWalkPathfinder`).
- **Gestione Waypoints & POI**: Schermate accessibili per salvare, gestire e tracciare Waypoints personalizzati (`ManageWaypointsScreen`, `SaveWaypointScreen`, `POIWaypoints`).
- **Numpad Ergonomico a 3 Layer (Zero-Shift)**: Mappatura ergonomica priva di tasto Shift per evitare accovacciamenti involontari (Layer 0 Azioni dirette, Layer 1 Coordinate/Snap, Layer 2 Ispezione/Status).

---

## 🚀 [v26.2-1.12.0] — 2026-08-01 (Inception Fork & Setup Ambiente)

### 📦 Setup Iniziale
- Fork del repository ufficiale a monte su Minecraft 26.2 / 1.21.x con Fabric + NeoForge, Architectury Loom e Java 25.
- Setup della base di conoscenza locale in `knowledge/` (00..12) e delle pipeline di collaudo con PrismLauncher e NVDA.
