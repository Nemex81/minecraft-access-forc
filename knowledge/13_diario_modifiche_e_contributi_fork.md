# Diario Modifiche Personali & Contributi Fork (AVF)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
# Progetto: Minecraft Access (Fork di nemex81 / Luca)
# Ambito: Cronologia Feature Personali & Disciplina I18N
# Riferimento Standard: Astralis Versioning Framework (AVF 3+1 Livelli)
# Baseline di Partenza: v26.2-1.12.0 (Fork Inception a inizio Agosto 2026)

Questo documento costituisce il **Diario Ufficiale delle Modifiche del Fork Personale in lingua Italiana**.
Poiché il `README.md` pubblico e la documentazione del repository upstream rimangono in lingua Inglese per la community internazionale con la sola sezione `## [Unreleased]`, tutte le novità, i refactoring e i miglioramenti sviluppati sui nostri rami (`mymaster`, `dev`) vengono tracciati qui secondo la disciplina AVF (`V.A.R[.M]`).

## 🚀 [v26.2-1.21.0] — 2026-09-12 (Rev. MC-26.23: AutoWalk Verticale & Assistente Tattico di Scalata Climb Assistant)

### 🧗 Rev MC-26.23: AutoWalk Verticale & Assistente Tattico di Scalata (Climb Assistant)
- **Cinematica Verticale e Motore Unificato (`ClimbKinematics`)**:
  - Estesa la navigazione automatica AutoWalk ai collegamenti verticali tramite scale a pioli, impalcature e rampicanti.
  - Creato l'assistente tattico di scalata semimanuale (`Alt+S`, tasto interazione con bypass su `Sneak`, voce dedicata in Access Menu) che riusa deterministicamente lo stesso motore di calcolo di AutoWalk.
  - FSM deterministica a stati discreti: `ALIGN -> APPROACH -> CAPTURE_WAIT -> TRANSIT -> DISMOUNT`.
  - Discesa controllata a gravità naturale (`GRAVITY_DESCENT`) con lease esclusiva su `SafetyMovementGuard` e `ControlledDescentPort` per prevenire falsi allarmi del freno anticaduta.
  - Sbarco ascendente biforcato con spinta di sollevamento residua (`keyUp=true`) e transfer verso il blocco calpestabile di destinazione.
  - Sbarco discendente deterministico con rilevazione immediata del contatto a terra (`playerOnGround=true`) sull'ultimo piolo e annuncio vocale *"Raggiunto piano stabile"*.
- **Probe Geometrici & Rilevamento Volumetrico**:
  - `ClimbContactProbe`: campionamento continuo AABB e rilevamento attraversamento varco (`sweptCrossing`) dall'alto senza attriti o incastri sull'orlo.
  - `ClimbLandingProbe`: tolleranza dinamica della fisica vanilla (`VELOCITY_EPSILON = 0.085`) per gestire il valore grezzo `Motion.y = -0.0784` a terra.
- **Bonifica Codice Obsoleto (D41)**:
  - Eliminato lo stato orfano `REACQUIRE` e il metodo non referenziato `evaluateReacquire()` in conformità al canone di igiene evolutiva.
- **Suite di Test & Validazione**:
  - 396/396 test JUnit 5 verdi a 0 ms.
  - Collaudo in-game completo condotto da Luca alla Torre del Belvedere con successo al 100% in salita e discesa sia automatica che manuale.

---

## 🚀 [v26.2-1.20.0] — 2026-09-09 (Rev. MC-26.21 & Rev. MC-26.22: Interruttori Sensori F1..F6, Didgeridoo, Control Destro, Neutralizzazione Mixin Vanilla & Batteria Suoni POI per Categoria)

### 🎛️ Rev MC-26.21: Batteria Interruttori Sensori Contigui (Ctrl+Alt+F1..F6), Univocità Acustica Didgeridoo & Supporto Simmetrico Control Destro
- **Univocità Acustica Assoluta per Radar Voragini Lontane**:
  - Sostituito il suono di `NOTE_BLOCK_BELL` con `NOTE_BLOCK_DIDGERIDOO` (timbro cavernoso tellurico a pitch `0.8f`) in `LongRangeFallDetector`, restituendo la campanella limpida in purezza esclusiva a POI e Waypoint.
  - Aggiornati i test di regressione headless con asserzioni dedicate sul suono didgeridoo.
- **Batteria Contigua dei 6 Interruttori Sensoriali (`Ctrl + Alt + F1..F6`)**:
  - `Ctrl + Alt + F1`: Faro acustico Waypoint / Traccia rotte (`POIWaypoints.toggleAudioBeacon()`).
  - `Ctrl + Alt + F2`: Rilevatore ostacoli (`ObstacleDetector.toggleObstacleDetector()`).
  - `Ctrl + Alt + F3`: Rilevatore buche corto raggio 1..6m (`CentralFallSafetyManager.toggleProximityFallDetector()`).
  - `Ctrl + Alt + F4`: Radar orografico buche lontane 7..24m (`CentralFallSafetyManager.toggleLongRangeFallDetector()`).
  - `Ctrl + Alt + F5`: Suono arpeggio elevazione mirino (`NarrateCrosshair.toggleCrosshairAudio()`).
  - `Ctrl + Alt + F6`: Sentinella minacce ostili ravvicinate 6m con allarme basedrum (`POIEntities.toggleHostileRadar()`).
  - Notifica vocale bilingue con annuncio di stato ("Attivo" / "Disattivato") e persistenza automatica su configurazione `minecraft-access.json`.
- **Supporto Simmetrico Hardware Control/Alt Destro**:
  - Aggiornato `ModifierUtils` con interrogazione hardware GLFW per `GLFW_KEY_RIGHT_CONTROL` e `GLFW_KEY_RIGHT_ALT`, rendendo tutti i comandi `Ctrl` e `Ctrl+Alt` (incluso `Ctrl+Alt+Home` per `ObjectTracker`) accessibili indifferentemente da entrambi i lati della tastiera.

### 🛡️ Rev MC-26.22: Neutralizzazione Mixin Tasti Vanilla F1..F6 & Batteria Interruttori Suoni Radar POI per Categoria
- **Neutralizzazione Incondizionata Tasti Funzione Vanilla (F1, F3, F5)**:
  - `DebugScreenEntryListMixin`: Intercetta e cancella `toggleDebugOverlay()` a monte quando `ModifierUtils.hasControlAndAlt()` è attivo, azzerando al 100% l'apertura indesiderata della schermata di debug di F3 sia alla pressione che al rilascio.
  - `KeyboardHandlerMixin`: Firma corretta `(KeyEvent event, CallbackInfoReturnable<Boolean> cir)` per bloccare le combinazioni debug vanilla senza interferire con il motore di gioco.
  - `MinecraftMixin`: All'inizio di `handleKeybinds()`, se `Ctrl+Alt` sono premuti, svuota a vuoto i click pendenti di `keyTogglePerspective` (F5) e `keyToggleGui` (F1), impedendo cambi involontari di prospettiva telecamera o scomparsa dell'HUD.
- **Controllo Granulare Suoni Radar POI per Categoria via `BooleanSupplier`**:
  - Architettura a Inversion of Control in `POIGroup`: aggiunto `BooleanSupplier soundEnabledSupplier` e guardia difensiva in `playSoundForGroupItems()`.
  - Estensione configurazione: 7 flag per blocchi (`soundEnabledOre`, `soundEnabledFunctional`, `soundEnabledDoor`, `soundEnabledPortal`, `soundEnabledLadder`, `soundEnabledFluid`, `soundEnabledGui`) e 9 flag per entità (`soundEnabledHostile`, `soundEnabledPassive`, ecc.), tutti attivi di default (`true`).
  - Batteria Kuma 9 nuovi interruttori su tastiera IT:
    * `Ctrl + Alt + F7`: Suono Minerali (ORE)
    * `Ctrl + Alt + F8`: Suono Blocchi Funzionali (FUNCTIONAL)
    * `Ctrl + Alt + F9`: Suono Porte e botole (DOOR)
    * `Ctrl + Alt + F10`: Suono Portali (PORTAL)
    * `Ctrl + Alt + F11`: Suono Scale a pioli (LADDER)
    * `Ctrl + Alt + F12`: Suono Fluidi (FLUID)
    * `Ctrl + Alt + G`: Suono GUI e forzieri (HAVE_INTERFACE)
    * `Ctrl + Alt + H`: Suono Radar Ostili periodico (NOTE_BLOCK_BELL 24m, DISTINTO dalla sentinella F6)
    * `Ctrl + Alt + P`: Suono Animali Passivi (PASSIVE)
- **Aiuto In-Game & Rigore I18N**:
  - Aggiunta la Categoria 8 *"Interruttori Suoni Radar POI"* in `QuickKeysHelpScreen` (`F1`).
  - Localizzazioni complete in `it_it.json` ed `en_us.json` con rigoroso ordinamento alfabetico crescente conforme alla CI.
- **Suite di Test & Verifica Empirica**:
  - Test JUnit 5 headless: 354/354 test verdi a 0 ms.
  - Collaudo in-game (Luca): sequenza di tutti i toggle testata con successo, azzeramento selettivo dei suoni confermato, zero conflitti grafici o visivi.

---

## 🚀 [v26.2-1.19.4] — 2026-09-09 (Rev. MC-26.20: Null Safety in ObjectTracker.isObjectValid() su Selezione Vuota)

### 🎯 Rev MC-26.20: Null Safety in ObjectTracker.isObjectValid() su Selezione Vuota
- **Risoluzione NullPointerException su Pattern Switch (Java 21+)**:
  - Inserita guardia difensiva `if (object == null) return false;` in testa a `ObjectTracker.isObjectValid(Object object)`;
  - Prevenuta l'eccezione implicita `Objects.requireNonNull(object)` generata dal compilatore Java sul pattern switch quando l'oggetto selezionato è nullo (`currentObject == null`).
- **Integrità Flussi di Navigazione e Feedback Vocale NVDA**:
  - Puntamento (`lookAtCurrentObject`) e lettura coordinate (`narrateCoordinatesOfCurrentObject`) pronunciano regolarmente l'avviso vocale: *"Nessun punto di interesse selezionato"*;
  - Scarto pulito degli elementi nulli nei flussi `Stream` interni di filtraggio POI.
- **Suite di Test Unitari Headless (0 ms)**:
  - Creata la suite [`ObjectTrackerTest.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/test/java/org/mcaccess/minecraftaccess/features/point_of_interest/ObjectTrackerTest.java) con 4 test dedicati (null, stato iniziale, `Waypoint`, tipo sconosciuto);
  - Suite complessiva portata a 354/354 test verdi a 0 ms senza dipendenze grafiche.
- **Collaudo In-Game**:
  - Testato e convalidato al 100% in-game da Luca: azionamento comandi a selezione vuota fluido, sintesi vocale immediata, zero errori nel log.

---

## 🚀 [v26.2-1.19.3] — 2026-09-09 (Rev. MC-26.19: Quiete Sensoriale AutoWalk & Navigatore, Verbosità di Progressione & Silenziamento Mirino e Incudine)

### 🚶 Rev MC-26.19: Quiete Sensoriale AutoWalk & Navigatore, Verbosità di Progressione & Silenziamento Mirino e Incudine
- **Verbosità di Progressione Regolabile (`ProgressionFeedbackMode`)**:
  - Introdotto l'enum `ProgressionFeedbackMode` con 4 livelli in `Config.AutoWalk`: `SOUND_AND_VOICE` (default storico Luca), `SOUND_ONLY`, `VOICE_ONLY`, `OFF`;
  - Svincolato il conteggio vocale dei passi rimanenti (`callback.onProgression`) dalla variabile globale dei suggerimenti didattici `narrateHints`;
  - Cadenza vocale autonoma a intervalli debounced di 5 passi (*"Ancora 25 passi"*, *"Ancora 20 passi"*...);
  - Rigore I18N rispettato al 100% in `it_it.json` ed `en_us.json` con ordinamento alfabetico crescente obbligatorio.
- **Silenziamento Chirurgico Cue Arpa Mirino (`NarrateCrosshair`)**:
  - Soppressa l'emissione del suono `playRelativePositionSoundCue` (`NOTE_BLOCK_HARP`) durante le sterzate della visuale dell'AutoWalk se `silenceCrosshairDuringWalk` è abilitato;
  - Preservata al 100% l'interrogazione manuale immediata ad alta priorità tramite tasto `B` (`DirectInteractionShield`).
- **Silenziamento Cue Acustico Anticaduta (Incudine) in Marcia**:
  - Soppresso il pre-freno acustico d'emergenza (`SoundEvents.ANVIL_LAND`) e l'Edge Bump debounced in `ProximityFallDetector` durante la deambulazione assistita da pathfinder A*;
  - Ripristino immediato (0 ms) della protezione acustica totale dell'incudine su marcia manuale, arresto, arrivo a meta o interruzione da stallo/takeover (`keySneak` / WASD); auto-sneak fisico sul ciglio preservato intatto.
- **Conformità Headless & Robustezza Modulare**:
  - Resi pubblici i costruttori di `Config.ObstacleDetector` e `Config.NarrateCrosshair` per compatibilità headless e Cloth Config;
  - Creati test seams deterministici a 0 ms senza dipendenze grafiche.
- **Suite di Test & Convalida Empirica**:
  - Suite JUnit 5 headless: 350/350 test verdi a 0 ms;
  - Collaudo in-game (Luca): tre navigazioni verificate con successo, ambiente acustico pulito e ritmato, telemetria nitida, zero falsi allarmi.

---

## 🚀 [v26.2-1.19.2] — 2026-09-08 (Rev. MC-26.18: De-monolitizzazione & Architettura Duale Cadute)

### 🕳️ Rev MC-26.18: Architettura Duale Anticaduta (Prossimità 1..6m & Lungo Raggio 7..24m)
- **De-monolitizzazione Modulare nel Package `features.safety.fall`**:
  - `CentralFallSafetyManager`: orchestratore unico client-tick, coordina i due sensori, gestisce la mutua esclusione, la quiete sensoriale in AutoWalk e il trigger manuale (`Alt+F` / `Ctrl+Alt+F`).
  - `ProximityFallDetector`: raggio $1..6\text{ m}$ suddiviso in tre fasce:
    * *Zona 1* ($2..6\text{ m}$): pre-allerta con xilofono (`NOTE_BLOCK_IRON_XYLOPHONE`), rallentamento sprint e notifica vocale.
    * *Zona 2A* ($1.0..1.5\text{ m}$): pre-freno acustico d'emergenza con incudine su canale `PLAYERS` e mutua esclusione acustica dallo xilofono.
    * *Zona 2B* ($\le 0.85\text{ m}$): aggancio meccanico di sicurezza con auto-accovacciamento forzato (`autoSneak`) sul ciglio.
  - `LongRangeFallDetector`: radar orografico periodico ($7..24\text{ m}$) a scansione 3.5s, rintocco di campanella 3D attenuata (`NOTE_BLOCK_BELL`), decadimento volume lento al $50\%$ e proiezione vettoriale OpenAL $[2.5 .. 12.0]\text{ m}$.
  - `FallDetector`: ridotto a facciata leggera (~140 righe) con conservazione al 100% della retrocompatibilità binaria.
- **Potenziamento Acustico Pre-Freno & Escalation Dinamica (PRAPI & PRAPI-B)**:
  - Sostituito `SoundEvents.ANVIL_HIT` con `SoundEvents.ANVIL_LAND` (`block.anvil.land`), dotato di transiente metallico ad altissima energia penetrante attraverso la voce NVDA;
  - Allineato il bus audio a `SoundSource.PLAYERS` per garantire immunità totale dai cursori di attenuazione dei blocchi ambientali;
  - Implementato `isStatusEscalation` per suonare prontamente l'incudine durante l'avvicinamento continuo alla voragine.
- **Quiete Sensoriale Assoluta in AutoWalk**:
  - Campanella lungo raggio, xilofono e descrizioni vocali 100% soppressi durante la marcia autonoma; preservato l'auto-sneak salvavita sul ciglio.
- **Suite di Test & Verifica Empirica**:
  - Suite JUnit 5 headless: 344/344 test verdi a 0 ms.
  - Collaudo in-game (Luca): escalation impeccabile, incudine squillante e perentoria, zero errori nei log.

---

## 🚀 [v26.2-1.19.1] — 2026-09-08 (Rev. MC-26.10: Disaccoppiamento Soglie Anticaduta & Pervietà Corridoi Verticali)

### 🛡️ Rev MC-26.10: Disaccoppiamento Soglie Anticaduta (Zona 1 Percezione vs Zona 2 Intervento)
- **Disaccoppiamento Soglie in Cloth Config**:
  - Introdotto `warningDepth = 3` (default): dislivello minimo in blocchi per attivare la pre-allerta parlata (*"Attenzione: burrone"*) e il rallentamento cinetico (`autoSlowdown`), senza vincolo meccanico sui movimenti.
  - Introdotto `autoSneakDepth = 4` (default): dislivello minimo per attivare l'accovacciamento protettivo forzato sul bordo (*"Sul ciglio: burrone"*), tarato esattamente sulla soglia di danno da caduta vanilla.
  - Mantenuta retrocompatibilità con `depth = 4` (`@Deprecated`) e implementato getter difensivo `getEffectiveWarningDepth() = Math.min(warningDepth, autoSneakDepth)`.
- **Comportamento nella Finestra $[3, 4)$ Blocchi**:
  - Sui dislivelli da 3 blocchi (danno nullo): l'utente viene pre-allertato a voce ma può scendere o saltare liberamente con `W`, eliminando l'incollamento spiacevole sul ciglio.
  - Sui baratri da $\ge 4$ blocchi: l'ancoraggio protettivo sul ciglio interviene al 100% bloccando la caduta letale.

### 🌊 Contratto D2.1: Pervietà del Corridoio Verticale per Discesa in Acqua (Affinamento PRAPI)
- **Risoluzione Caso Limite Falde Sotterranee Coperte**:
  - Nel ciclo verticale verso il basso di `findDescentCandidate`, inserito il controllo di pervietà fisica continua (Cancello 3 Inner Codex): se prima di raggiungere il fluido si incontra un blocco solido impenetrabile (`!probeState.getCollisionShape(level, waterProbe).isEmpty()`), la scansione si arresta all'istante (`break;`).
  - Azzerato al 100% il falso allarme di *"Discesa sicura"* generato da falde acquifere o caverne sotterranee sepolte sotto terreno solido.
- **Silenziamento Discese Minime su Scale a Pioli**:
  - Le discese con dislivello complessivo $\Delta Y < warningDepthThreshold$ (1-2 blocchi) vengono classificate come cammino calpestabile ordinario (`NOT_APPLICABLE`), azzerando l'inquinamento vocale durante il normale deambulare su rampe o cordoli.

### 🧪 Suite di Test & Convalida Empirica
- **Test Unitari Headless (0 ms)**: 327 test eseguiti con successo (0 fallimenti, 0 ignorati). Introdotti Test 10 (acqua sotterranea coperta da pietra) e Test 11 (tuffo in aria aperta).
- **Collaudo Empirico In-Game (Luca)**: Convalidato sul campo su mondo `scuola di sopravvivenza mondo 2 (1)`: zero annunci spuri allo spawn sul sentiero di terra, perfetta fluidità sui dislivelli da 3 blocchi, protezione e salute intatta a 20.0 cuori.

---

## 🚀 [v26.2-1.19.0] — 2026-09-07 (Release Ufficiale: Cognitive Coordinator, Gestione Porte & Navigatore AutoWalk)

### 🌐 Governance & Multi-AI: Allineamento Ecosistema ASTRALIS v2.8.0 (Commit bac2c87b)
- **Allineamento Rete a 4 Nodi & Profilo di Resilienza**:
  - `GEMINI.md`: Aggiornato al framework ASTRALIS v2.8.0 con Profilo di Resilienza a 4 canali (VCS, Ponte Hot, Cold Archive, No-Git Resiliente), normalizzato senza righe vuote multiple (184 righe).
  - `AGENTS.md`: Aggiornato ad ASTRALIS v2.8.0, integrato con la Rete a 4 Nodi (inclusa `docs/strategie/attive/`), vincoli di non-concorrenza con Antigravity e conformità ai 6 Canoni di Meta-Governance (54 righe).
- **Integrazione della Guardia Ausiliaria On-Demand**:
  - Il repository è pienamente conforme e monitorato dalla sentinella silente Multi-AI, garantendo perfetta omogeneità tra le direttive machine-level di GPT Codex (`%USERPROFILE%\.codex\AGENTS.md`) e le direttive locali del repository.



### 🚶 Fase 5: Navigatore, AutoWalk & Disaccoppiamento Cinematica/Pathfinding (Revisioni 5A, 5B, 5C, 5D, 5D.1 - 5D.7-R3)
- **Disaccoppiamento a 3 Livelli (`MovementCoordinator`, `RouteNavigator`, `AutoWalkMotor`)**:
  - `MovementCoordinator`: arbitraggio eventi di navigazione verso `CognitiveCoordinator` e vocalizzazioni semantiche.
  - `RouteNavigator`: ciclo ad anello chiuso su waypoints e percorso.
  - `AutoWalkMotor`: esecuzione cinematica con smoothing yaw 20°/tick e controllo di trazione.
- **Two-Pass Pathfinding Deterministico (`AutoWalkPathfinder`)**:
  - Passaggio 1: calcolo rigoroso a porte aperte.
  - Passaggio 2: fallback con attraversamento varchi chiusi a costo calibrato (penalità 5.0) e budget esteso a 5.000 nodi.
- **Sblocco Visuale One-Shot su Varchi Chiusi (Rev 5D.2)**:
  - Eliminato il lock della telecamera a 20 Hz davanti a porte chiuse: Luca mantiene il controllo libero al 100% della visuale da tastiera per esplorare la stanza durante l'attesa.
- **Geometria Voxel Continua e Taglio Diagonali (Rev 5D.3)**:
  - Scansione AABB nativa su collision box Minecraft per partenza da varchi chiusi.
  - Divieto di taglio diagonale con compenetrazione di stipiti o soffitti bassi.
- **Modello Voxel a 4 Pilastri per Scale a Pioli (`LadderBlock` — Rev 5D.7-R2)**:
  - `isPassable` e `isClearHeadroom` permissive su `LadderBlock` (hitbox 0.6m transita nello spazio rimanente di 0.8125m).
  - `isStandable` categoricamente falso per impedire cadute nel vuoto o salite spurie su botole/tetti.
  - Trasparenza in `isSolid` per scansioni di discesa verticale e linea di vista.
- **Disaccoppiamento Shift Hardware da Sneak di Sicurezza (Contratto D7 — Rev 5D.7-R3)**:
  - Integrazione di `CrouchIntentProbe` / `RawCrouchIntentProvider` (GLFW nativo) in `AutoWalkMotor`.
  - Lo sneak sintetico imposto da `SafetyMovementGuard` su cigli o curve a gomito non innesca più il falso annullamento della navigazione, consentendo l'avanzamento sicuro. Solo la pressione fisica reale dei tasti Shift da parte dell'utente interrompe l'AutoWalk.
- **Clearance Volumetrica Occhi/Testa in FallDetector (Contratto D8 — Rev 5D.7-R3)**:
  - Verifica dello spazio libero ad altezza occhi (`stepPos.above()`) nel presidio ciglio e nel look-ahead.
  - Se il passaggio a quota testa è ostruito da blocchi solidi o barriere, la caduta è fisicamente impossibile e la cella viene scartata a monte, eliminando i falsi positivi di burrone sotto soffitti bassi, trombe scale o feritoie.
- **Suite di Test Completa**: 299/299 test automatici verdi (`BUILD SUCCESSFUL in 43s`).
- **Collaudo Empirico al 100%**: Validata con successo in-game da Luca su percorsi indoor/outdoor a lungo raggio (90m verso stalla cava in 27s) e salita/discesa ininterrotta alla torre Belvedere (81m in 21s).

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
