# Piano Tecnico Implementativo — Fase 4: Esplorazione, Mirino e POI Cognitivi

**Riferimento strategico:** `docs/strategie/STRATEGIA_COGNITIVE_COORDINATOR.md`
**Stato:** `[SOTTO-FASE 1A — PIANO RIVISTO, DA CONVALIDARE]`
**Ramo operativo:** `feat/cognitive-orchestrator`
**Base verificata:** commit documentale `2fe2bc91`; albero Git pulito; ultimo report JUnit disponibile: 197 test, 0 failure, 0 error.
**Incremento AVF proposto:** `v1.13.0`, subordinato a conferma di Luca prima del rilascio.
**Autori:** Luca, Antigravity, revisione architetturale ChatGPT.

---

## 1. Decisione di avvio e obiettivo operativo

La Fase 4 non centralizza indiscriminatamente ogni suono o ogni chiamata a `MainClass.narrate`. Centralizza soltanto la decisione cognitiva relativa al feed automatico del mirino: priorità, deduplicazione, validità temporale e consegna della voce ambientale.

L'obiettivo osservabile è questo:

1. Se il giocatore cambia bersaglio o distanza mentre si muove, il mirino invia un evento `EXPLORATION / PASSIVE` al `CognitiveCoordinator`.
2. Se non esiste un evento più importante, il messaggio viene emesso una volta, senza troncare messaggi prioritari.
3. Se esiste un pericolo, un'interazione diretta o un avviso contestuale già dominante, il mirino automatico non parla tardi al termine della soppressione.
4. Se Luca richiede esplicitamente una lettura con `B`, ruota/centra la visuale o usa il radar POI, la risposta resta immediata e diretta; il contesto ambientale viene temporaneamente protetto, senza mai ritardare un evento `CRITICAL`.

Questa fase completa il primo dominio di esplorazione senza riaprire la Fase 3 della sicurezza.

---

## 2. Esito della revisione tecnica

### 2.1 Fondazioni già disponibili

- `CognitiveCoordinator` dispone di buffer a fine tick, priorità, TTL, deduplicazione, coda breve, memoria attentiva e bypass immediato per `CRITICAL`.
- Il coordinatore viene registrato dopo `NarrateCrosshair` e `ObstacleDetector`; gli eventi automatici prodotti nello stesso tick possono quindi essere arbitrati insieme.
- `CrosshairFeedbackManager` è già la Single Source of Truth per bersaglio, testo, distanza, debounce e composizione atomica di mirino/orientamento.
- `DirectInteractionShield` esiste e il coordinatore lo rispetta, ma nessun chiamante di produzione lo attiva ancora.
- La Fase 3 ha già risolto la voce ritardata con il pattern di **silent commit** durante la finestra di soppressione degli ostacoli.

### 2.2 Correzioni necessarie al testo strategico

Prima della chiusura della Fase 4, la strategia dovrà essere allineata ai fatti seguenti:

1. Il riferimento ASTRALIS deve passare da v2.5.5 a v2.6.2.
2. `DirectInteractionShield` non è più una proposta: è implementato, ma richiede i primi attivatori di produzione.
3. I template esistenti sono `minecraft_access.cognitive.join_safety_exploration`, `join_movement_safety` e `join_movement_exploration`; il piano non introduce una chiave separata `join_obstacle_target`.
4. Il punto “Ostacolo + Mirino” non autorizza a sostituire la composizione specialistica della Fase 3 con il join generico del coordinatore.
5. Il radar POI deve essere classificato come risposta a input esplicito, non come sorgente passiva da accodare.

---

## 3. Confine architetturale definitivo

### 3.1 Responsabilità che restano locali

1. `NarrateCrosshair` continua a fare raycast, filtro, lettura del narratore di mondo e calcolo del segnale 3D relativo.
2. `CrosshairFeedbackManager` continua a comporre i token configurati: bersaglio, distanza, cardinale, gradi, inclinazione ed elevazione relativa.
3. `POIBlocks` e `POIEntities` continuano a generare i loro beacon sonori 3D continui, che sono percezione spaziale grezza e non testo cognitivo in coda.
4. `ObstacleDetector` continua a classificare geometria, direzione e gravità dell'ostacolo.

### 3.2 Responsabilità del coordinatore

1. Riceve soltanto gli eventi automatici già interpretati dal dominio Esplorazione.
2. Decide se emetterli, differirli o scartarli in base a priorità, TTL, deduplicazione e scudi.
3. Non rifà raycast, non interpreta voxel, non genera nomi di blocchi e non ricompone la grammatica interna del mirino.

### 3.3 Regola su voce e suono

Nella Fase 4 viene centralizzata la **voce automatica** del mirino. Il cue 3D continuo del mirino resta locale in questa iterazione.

Motivo: `SoundCue` usa oggi una `BlockPos`, mentre il mirino usa anche posizioni esatte di entità e raggi. Convertirlo ora a coordinate discrete degraderebbe la localizzazione acustica. Una futura evoluzione del contratto audio dovrà essere progettata separatamente, con compatibilità `Vec3`, test e collaudo dedicato.

---

## 4. Delimitazione negativa: cosa non toccare

Durante tutta la Fase 4 è vietato modificare, rifattorizzare o “semplificare” i seguenti componenti, salvo una nuova revisione formalmente approvata:

- `features/safety/FallDetector.java` e `features/safety/traversal/SafetyMovementGuard.java`;
- `RawCrouchIntentProvider`, `SneakOverridePort` e ogni logica di ownership dello Shift;
- `ObstacleDetector.java`, `ObstacleSafetyEventFactory.java`, `ObstacleNarrationComposer.java` e `ObstacleNarrationContext.java`;
- la finestra monotona di 100 ms e il silent commit della Fase 3B;
- `InventoryControls.java`, i mixin GUI e le correzioni Rev MC-26.9 / MC-26.10;
- geometria voxel, micro-raymarch, filtri ostacoli e range già convalidati;
- suoni 3D continui di `POIBlocks`, `POIEntities` e del mirino;
- tasti, modificatori e loro mapping preesistenti;
- `mymaster`, merge, deploy e backup fino al collaudo manuale positivo.

In particolare, non si deve trasformare il radar POI, il lock-on o la scansione direzionale in un flusso passivo durante questa fase.

---

## 5. Matrice delle invarianti anti-regressione

| Invariante | Condizione da preservare | Evidenza richiesta |
|---|---|---|
| Sicurezza critica | Burrone, lava e altri `CRITICAL` parlano subito anche con scudo attivo. | Test del fast-path e prova in-game. |
| Fase 3B | Ostacolo frontale non duplica il mirino; ostacolo laterale/retro conserva il riferimento frontale già validato. | Test di regressione esistenti più casi Fase 4. |
| Input esplicito | `B`, rotazione, centramento e radar POI restano a latenza zero e non entrano nel buffer ambientale. | Test con seam e collaudo tastiera. |
| Silent commit | Un feed automatico soppresso aggiorna lo stato e non viene pronunciato in ritardo. | `CrosshairMovementSuppressionTest` invariato ed esteso. |
| Configurazione | Tutte le opzioni `NarrateCrosshair` mantengono testo, filtro, ordine e frequenza attuali. | Test parametrizzati delle modalità. |
| Audio spaziale | Il cue locale di mirino/POI non cambia volume, posizione o cadenza per effetto del routing vocale. | Confronto in-game e assenza di modifiche ai produttori audio. |
| Transizioni | Nessun evento passivo resta in memoria dopo disconnessione, cambio mondo/dimensione, morte o respawn. | Test del reset e telemetria. |

---

## 6. Classificazione vincolante degli eventi

| Origine | Natura | Percorso Fase 4 | Priorità |
|---|---|---|---|
| Cambio automatico bersaglio mirino | Ambientale | `CognitiveEvent` | `PASSIVE` |
| Progresso distanza dello stesso bersaglio durante movimento | Ambientale | `CognitiveEvent` | `PASSIVE` |
| Tasto `B` / funzione `NarrateTarget` | Input esplicito | Diretto + `DirectInteractionShield` | Diretta, non accodata |
| Rotazione e centramento visuale | Input esplicito | Diretto + `DirectInteractionShield` | Diretta, non accodata |
| Selezione/ciclo/aggancio radar POI | Input esplicito | Diretto + `DirectInteractionShield` | Diretta, non accodata |
| Beacon continui POI e cue relativo del mirino | Percezione spaziale continua | Locale, invariato | Fuori dalla coda |
| Minaccia ostile automatica da POI | Sicurezza futura | Nessuna migrazione in Fase 4 | Fuori ambito |

Un evento automatico del mirino usa sempre `SpatialDirection.FORWARD`: descrive il bersaglio sul raggio di sguardo, non la frase dell'ostacolo né la direzione della marcia.

---

## 7. Sotto-fase 4A — Fondazioni di routing e ciclo di vita

### 7.1 Gate di rollout per il solo dominio Esplorazione

In `Config.CognitiveSettings` viene aggiunto il solo flag:

`explorationCognitiveRoutingEnabled`

Valore iniziale: `false` per il primo deploy sperimentale controllato.

Regola sequenziale:

1. Se `cognitiveCoordinatorEnabled` è falso, il mirino usa il percorso legacy storico.
2. Altrimenti, se `explorationCognitiveRoutingEnabled` è falso, il mirino usa lo stesso percorso legacy storico.
3. Altrimenti, soltanto il feed automatico del mirino usa il coordinatore.
4. Il valore predefinito può essere promosso a `true` soltanto dopo il collaudo positivo di Luca e una decisione esplicita di chiusura.

Questo evita che la verifica del nuovo dominio obblighi a disabilitare anche la sicurezza già convalidata.

L'aggiunta richiede chiavi IT/EN ordinate alfabeticamente, binding di configurazione e test del caricamento, senza introdurre un nuovo menu o un nuovo tasto.

### 7.2 Reset di sessione del coordinatore

`CognitiveCoordinator.clearAllBuffers()` deve diventare raggiungibile in produzione.

Contratto:

1. Alla disconnessione o al cambio sessione, svuotare buffer tick, coda breve, deduplicazione, memoria attentiva e `DirectInteractionShield`.
2. All'ingresso in una nuova istanza `ClientLevel`, effettuare il reset prima del primo flush della nuova sessione.
3. Su morte e respawn, usare l'hook client già disponibile nel progetto oppure un guard di identità/stato del giocatore; non inventare callback non supportate dalle API Balm/Fabric/NeoForge.
4. Se il player o il level è nullo, nessun flush deve generare una narrazione fantasma.

Il codice deve usare gli hook di lifecycle già presenti nel repository. Prima dell'editing, Antigravity verifica la firma API esatta con l'IDE e riusa un pattern esistente, senza Mixin nuovo.

---

## 8. Sotto-fase 4B — Evento automatico del mirino

### 8.1 Nuovo contratto puro

Creare `features/crosshair/CrosshairExplorationEventFactory.java` come factory pura e testabile. Riceve dati già calcolati dal mirino e costruisce, quando esiste un output vocale valido, un `CognitiveEvent` con:

- `SourceDomain.EXPLORATION`;
- `CognitivePriority.PASSIVE`;
- chiave semantica stabile del tipo `exploration.crosshair.target` oppure `exploration.crosshair.distance`;
- `StateSignature` basata su bucket distanza e identità canonica del bersaglio, mai sul testo italiano o inglese;
- posizione del blocco/entità mirata quando disponibile;
- `SpatialDirection.FORWARD`;
- TTL passivo già definito dal contratto cognitivo;
- `canChain = false` per questa migrazione.

L'identità canonica deve derivare da ID blocco o tipo entità, non dalla frase localizzata. Il testo già composto da `CrosshairFeedbackManager` resta il `narrationText` dell'evento.

### 8.2 Perché `canChain = false`

Il coordinatore possiede join generici `SAFETY + EXPLORATION`, ma non può sostituire il compositore specialistico della Fase 3:

1. Per un ostacolo frontale, il join generico rischierebbe di ripetere il medesimo blocco già descritto dall'ostacolo.
2. Per un ostacolo laterale o posteriore, il join generico rifiuterebbe correttamente direzioni differenti, ma perderebbe la frase specialistica che conserva sia l'ostacolo sia il mirino frontale.
3. Il messaggio già composto dall'ostacolo deve quindi dominare; il passivo del mirino concorrente viene scartato come obsoleto, senza emissione ritardata.

La verifica richiesta dalla strategia “Ostacolo + Mirino” diventa dunque una verifica di **parità semantica e assenza di duplicazione**, non un nuovo merge testuale generico.

### 8.3 Modifiche ammesse nel flusso del mirino

1. `NarrateCrosshair` continua a produrre i dati di percezione e a chiamare `CrosshairFeedbackManager`.
2. `CrosshairFeedbackManager` conserva tutti i metodi pubblici e l'attuale composizione atomica.
3. Nel solo ramo automatico di `processCrosshairTick`, quando il routing Esplorazione è attivo, il manager consegna l'evento alla factory e al coordinatore tramite una seam package-private per test headless.
4. Quando il routing è inattivo, il manager mantiene il percorso legacy: voce diretta con la semantica storica di `interrupt = true` e cue locale invariato.
5. Il percorso automatico cognitivo usa la normale emissione passiva del coordinatore, quindi non interrompe una voce più importante.

### 8.4 Regola di commit dello stato

Lo stato del mirino rappresenta la percezione corrente, non soltanto l'ultima frase effettivamente pronunciata.

Regola:

1. Se non esiste nessun output abilitato, preservare la semantica esistente.
2. Se esiste un evento automatico vocalizzabile e viene consegnato al coordinatore, aggiornare `currentTarget`, `currentNarration` e `currentDistance` nello stesso tick.
3. Se il coordinatore lo scarta per pericolo o scudo, non riannunciarlo alla scadenza del blocco: il silent commit impedisce il replay obsoleto.
4. Se il routing legacy è attivo, mantenere l'attuale commit atomico dopo l'emissione diretta.

---

## 9. Sotto-fase 4C — Interazioni dirette e radar POI

### 9.1 Politica unica di protezione diretta

Estendere `DirectInteractionShield` con una sola policy riutilizzabile di durata della protezione vocale. La durata viene calcolata a partire dal testo da leggere, con formula sequenziale:

`durata = min(2500 ms, numero parole × 280 ms + 600 ms)`.

La formula resta confinata allo scudo; nessun chiamante deve distribuire costanti temporali non motivate.

Effetto:

1. Gli eventi `PASSIVE` vengono scartati durante la risposta diretta.
2. Al massimo un evento `CONTEXTUAL` valido resta differito secondo la policy già implementata.
3. Gli eventi `OPERATIONAL` validi restano differiti entro TTL.
4. Gli eventi `CRITICAL` bypassano sempre lo scudo senza attesa.

La breve `NarrateCrosshair.suppressNarration(100/150)` rimane nel suo ruolo locale anti-doppio-tick; non viene sostituita né riutilizzata come scudo globale.

### 9.2 Chiamanti da proteggere

1. `CrosshairFeedbackManager.onManualCrosshairRequested()` per il tasto `B` e `NarrateTarget`.
2. `CrosshairFeedbackManager.onCameraRotated(...)`.
3. `CrosshairFeedbackManager.onCameraCentered(...)` e il percorso di centramento Numpad già delegato al manager.
4. I soli entry point pubblici di `ObjectTracker` che rispondono ai comandi radar: ciclo gruppo/oggetto, bersaglio più vicino e lettura/aggancio dell'oggetto corrente.
5. Gli eventuali entry point di lock POI direttamente invocati dalla tastiera, ma non le scansioni di background.

La protezione deve essere attivata prima della prima voce della risposta. Il messaggio resta diretto; non viene convertito a `CognitiveEvent`.

---

## 10. File previsti e responsabilità

| File | Intervento autorizzato | Responsabilità non modificabile |
|---|---|---|
| `Config.java` | Flag di rollout Esplorazione e binding. | Opzioni esistenti del mirino. |
| `it_it.json`, `en_us.json` | Due set di chiavi I18N ordinate. | Traduzioni estranee alla Fase 4. |
| `CognitiveCoordinator.java` | Hook minimo di reset sessione. | Politiche CRITICAL della Fase 3. |
| `DirectInteractionShield.java` | Policy unica di durata. | Garanzia di bypass `CRITICAL`. |
| `NarrateCrosshair.java` | Passaggio dei dati canonici necessari al routing. | Raycast, filtri e cue 3D. |
| `CrosshairFeedbackManager.java` | Routing automatico, fallback e attivazione scudo diretto. | SSOT dei token e silent commit 3B. |
| `CrosshairExplorationEventFactory.java` | Nuova factory pura. | Nessun accesso al client o scansione mondo. |
| `ObjectTracker.java` | Solo attivazione dello scudo sugli entry point radar diretti. | Scansione, ordinamento e voce diretta POI. |
| Test cognitivi/crosshair dedicati | Nuovi casi headless e regressioni. | Nessuna rimozione di test Fase 3. |

`MainClass`, `ObstacleDetector`, `FallDetector`, `SafetyMovementGuard`, `POIBlocks`, `POIEntities`, `CameraControls` e `NumpadControls` non sono file di editing previsto, salvo evidenza tecnica nuova e revisione esplicita del piano.

---

## 11. Piano di test automatico

### 11.1 Factory e deduplicazione

1. Blocco e entità producono identità canoniche differenti anche se la traduzione visualizzata coincide.
2. Stesso target e stesso bucket distanza vengono deduplicati nella finestra configurata.
3. Cambio bucket distanza, blocco, entità o posizione costituisce una variazione valida.
4. L'evento automatico è `EXPLORATION / PASSIVE / FORWARD / canChain=false`.
5. Nessun evento viene creato se il testo vocale è vuoto e nessuna voce è configurata.

### 11.2 Routing e fallback

1. Con coordinatore e routing Esplorazione attivi, il manager invia un solo evento e non chiama direttamente la voce automatica.
2. Con routing Esplorazione disattivo, la voce legacy resta `interrupt=true` e il risultato testuale è identico alla versione precedente.
3. Con coordinatore globale disattivo, il fallback resta locale e non dipende dal bypass generico del coordinatore.
4. Un evento automatico scartato da scudo o pericolo aggiorna lo stato e non riappare tardivamente.
5. Le quattro modalità `OFF`, `TARGET_ONLY`, `TARGET_AND_DISTANCE`, `FULL_FORMAT` conservano il comportamento stabilito.

### 11.3 Interazione diretta

1. `B` attiva lo scudo e chiama la voce direttamente nello stesso flusso.
2. Rotazione e centramento attivano lo scudo, preservando la frase atomica già composta dal manager.
3. Un comando radar POI attiva lo scudo senza essere convertito in evento passivo.
4. Durante lo scudo un `PASSIVE` viene eliminato, un `CONTEXTUAL` è differito e un `CRITICAL` è immediato.
5. Una seconda attivazione più breve non riduce una protezione già attiva.

### 11.4 Regressione Fase 3 e lifecycle

1. Ostacolo frontale più mirino non pronuncia due volte lo stesso bersaglio.
2. Ostacolo laterale/retro conserva la composizione già validata e non ottiene un secondo annuncio passivo tardivo.
3. I test `CrosshairMovementSuppressionTest`, `ObstacleNarrationComposerTest`, `ObstacleDetectorCognitiveDispatchTest`, `FallDetectorCognitiveDispatchTest` e `SafetyMovementGuardTest` restano verdi senza riscritture opportunistiche.
4. Disconnessione, cambio `ClientLevel`, morte e respawn eliminano code, deduplica e scudi; il tick seguente non ha narrazioni fantasma.

---

## 12. Collaudo manuale NVDA

La Fase 2 viene avviata solo dopo build e suite verdi. Luca esegue i casi nell'ordine seguente:

1. Mirino fermo su blocco, entità, vuoto e blocco filtrato.
2. Cammino avanti, indietro e laterale con cambio bersaglio e sola variazione distanza.
3. Prove delle quattro modalità di feedback in movimento e dei token mirino configurabili.
4. Avviso ostacolo frontale durante il cammino verso un blocco.
5. Avviso ostacolo laterale e retro con mirino frontale su un secondo blocco.
6. Lettura `B`, rotazione, Numpad 5 e rotazione continua: la risposta diretta deve precedere il ritorno del feed automatico.
7. Ciclo radar POI, selezione bersaglio e lock diretto: nessun feed automatico deve troncare la risposta richiesta.
8. Pericolo critico durante lettura diretta: il pericolo deve interrompere immediatamente.
9. Apertura GUI, disconnessione, cambio dimensione, morte e respawn: nessuna voce residua, NPE o hijack dello Shift.

La telemetria deve riportare assenza di `ERROR`, eccezioni e stack trace relativi ai pacchetti coinvolti.

---

## 13. Validazione ASTRALIS a 7 assi

1. **Validità:** usa contratti, tick e callback client già presenti; non richiede Mixin, rete o API non verificate.
2. **Efficacia:** elimina il mirino automatico come voce concorrente non arbitrata, senza sacrificare le richieste esplicite.
3. **Coerenza:** mantiene il manager come SSOT, il coordinatore come arbitro e gli scanner come sensori locali.
4. **Completezza:** copre configurazione, fallback, lifecycle, interazioni dirette, ostacoli, POI e cambio sessione.
5. **Precisione:** deduplica con ID canonico, posizione e bucket distanza; non usa testo localizzato come chiave tecnica.
6. **Affidabilità e prestazioni:** nessun raycast o scan aggiuntivo; factory pura, buffer già limitato, cue 3D invariati.
7. **Assenza di regressioni:** gate di rollout separato, fallback locale, invarianti Fase 3 esplicite e collaudo NVDA obbligatorio.

---

## 14. Procedura esecutiva dopo approvazione

1. **Pre-flight:** verificare JDK 25, stato Git pulito e nessuna istanza PrismLauncher da sovrascrivere durante la compilazione.
2. **Implementazione chirurgica:** rispettare l'ordine 4A, 4B, 4C; un commit coerente per sotto-fase.
3. **Verifica locale:** eseguire `.\gradlew.bat --no-daemon --no-watch-fs compileJava compileTestJava` e poi `.\gradlew.bat --no-daemon --no-watch-fs test`.
4. **Pacchetto:** eseguire `.\gradlew.bat --no-daemon --no-watch-fs shadowJar` solo dopo test verdi.
5. **Deploy provvisorio:** installare esclusivamente il JAR con hash verificato nelle istanze indicate da Luca.
6. **Collaudo manuale:** seguire la matrice NVDA della sezione 12 e analizzare log/telemetria.
7. **Chiusura:** aggiornare strategia, changelog, conoscenza e registro revisioni; archiviare il piano solo dopo conferma positiva di Luca.

---

## 15. Checkpoint vincolante

Questo documento conclude la Sotto-Fase 1A. Autorizza soltanto la pianificazione e la revisione documentale qui contenuta.

Non autorizza modifiche a sorgenti Java, configurazione, localizzazioni, test, build, JAR, deploy, merge, `mymaster` o backup.

Per avviare la Sotto-Fase 1B serve un ordine esplicito di Luca, ad esempio:

> “Approvo il piano Fase 4: procedi con l'implementazione della sotto-fase 4A.”
