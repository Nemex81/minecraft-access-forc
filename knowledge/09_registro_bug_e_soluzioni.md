# 09 — Registro Bug Risolti & Soluzioni Tecniche

Questo registro documenta i problemi tecnici complessi risolti nel tempo, preservando il know-how acquisito sul codice di `minecraft-access` e sulle API di Minecraft 26.2.

---

### Record 01 — Intercettazione Tasto `X` (Info Ricette) con GUI a Fuoco
- **Problema**: Premendo il tasto `X` per ascoltare i prerequisiti di una ricetta selezionata, l'evento non veniva catturato dallo screen reader se un elemento dell'interfaccia tratteneva il focus attivo.
- **Causa Radice**: Il sistema di registrazione input di Kuma non inoltrava gli eventi di pressione tasto quando un widget della schermata deteneva il focus grafico.
- **Soluzione Definitiva**: In `features/inventory_controls/InventoryControls.java`, è stato aggiunto `.ignoreScreenFocus()` alla configurazione del keybinding `inventory_controls.recipe_info`.

---

### Record 02 — Risoluzione Ricette ed Ingredienti su API Minecraft 26.2
- **Problema**: L'estrazione delle ricette tramite vecchi accessor Mixin falliva con errori di compilazione o crash a runtime in Minecraft 26.2.
- **Causa Radice**: Minecraft 26.2 ha riscritto il motore dei ricettari introducendo `RecipeDisplayEntry`, `RecipeDisplay` e la risoluzione condizionale tramite contesti.
- **Soluzione Definitiva**: 
  - Utilizzo di `SlotDisplay.resolveForFirstStack(ContextMap context)` alimentato con `SlotDisplayContext.fromLevel(client.level)`.
  - Gestione dei tipi specializzati (`ShapedCraftingRecipeDisplay`, `FurnaceRecipeDisplay`, `StonecutterRecipeDisplay`, `SmithingRecipeDisplay`).
  - Chiamata diretta all'API nativa `button.getCollection()`.

---

### Record 03 — Fallimento Pipeline CI GitHub per i File di Traduzione
- **Problema**: I controlli automatici di GitHub Actions (`linting.yml`) fallivano sistematicamente sulle modifiche ai file di lingua.
- **Causa Radice**: Il controllo `jq -e "keys != keys_unsorted"` esige che le chiavi JSON siano rigorosamente in ordine alfabetico crescente.
- **Soluzione Definitiva**: Qualsiasi inserimento o modifica di chiavi nei file `.json` in `src/main/resources/assets/minecraft_access/lang/` (in particolare `it_it.json` e `en_us.json`) deve essere ordinato alfabeticamente prima del commit.

---

### Record 04 — Falso Allarme "Impossibile posizionare il blocco" nei Comandi Assistiti
- **Problema**: L'esecuzione di comandi in-game per la posa guidata di blocchi generava messaggi di errore parlati da NVDA anche in assenza di effettivi problemi.
- **Causa Radice**: Se un comando `/setblock` o `/fill` colpisce coordinate dove il blocco identico è già presente, Minecraft genera il messaggio di errore `"Impossibile posizionare il blocco"`.
- **Soluzione Definitiva**: Implementata la regola di anti-ridondanza: se un blocco (es. recinzione a quota $Y$) è già stato creato da un lotto precedente, viene emesso solo il comando per il blocco superiore a quota $Y+1$ (es. torcia).

---

### Record 05 — Auto-Annullamento Immediato del Navigatore Automatico (Auto-Walk)
- **Problema**: L'avvio della marcia automatica tramite shortcut (`Alt + W` o `Alt + Numpad 0`) veniva istantaneamente interrotto al tick 0 con il messaggio vocale *"Navigazione automatica annullata"*.
- **Causa Radice**: La logica di Human Takeover campionava `keyUp.isDown()` come input di interruzione manuale. Poiché `keyUp` era tenuto premuto durante la combinazione `Alt + W` e veniva inoltre impostato a `true` dal controller per far avanzare il giocatore, il sistema rilevava il proprio stesso comando come un intervento manuale dell'utente, auto-annullandosi.
- **Soluzione Definitiva**: In `AutoWalkController.java`, introdotto un `startupGraceTicks = 10` (0.5s) per consentire il rilascio naturale dei tasti di avvio, e ristretto il monitoraggio del takeover manuale ai soli tasti di frenata/sterzata/accovacciamento (`keyDown` S, `keyLeft` A, `keyRight` D, `keyShift`).

---

### Record 06 — Transitabilità Varchi, Porte e Cancelletti nel Pathfinder A* Voxel
- **Problema**: Bersagli o waypoint salvati sulla soglia di una porta o oltre un cancello restituivano il messaggio *"Nessun percorso sicuro trovato"* anche a pochi blocchi di distanza.
- **Causa Radice**: `state.getCollisionShape()` di una porta chiusa (`DoorBlock`), cancelletto (`FenceGateBlock`) o botola (`TrapDoorBlock`) non è vuoto, venendo scartato come blocco solido impenetrabile dal pathfinder A*. Inoltre, per i `Waypoint`, la coordinata esatta poteva trovarsi incassata nello stipite o sollevata di 1 blocco.
- **Soluzione Definitiva**: 
  - In `AutoWalkPathfinder.isPassable()`, introdotto il riconoscimento esplicito di `DoorBlock`, `FenceGateBlock` e `TrapDoorBlock` come varchi transitabili.
  - In `resolveValidGoalPositions()`, per i bersagli di tipo `Waypoint`, inclusione sia della coordinata diretta sia di tutti i blocchi calpestabili limitrofi in orizzontale e verticale ($+1Y$, $-1Y$).

---

### Record 07 — Marcia Corsa/Camminata con Isteresi Anti-Chattering dello Sprint
- **Problema**: Durante la corsa con pilota automatico, nelle curve la telecamera subiva continui zoom avanti/indietro (FOV chattering) e l'animazione di movimento risultava visivamente spigolosa e sobbalzante.
- **Causa Radice**: La commutazione tra corsa e camminata avveniva ad ogni singolo tick senza isteresi temporale. Nei tratti con nodi ravvicinati, lo sprint si accendeva e spegneva freneticamente più volte al secondo.
- **Soluzione Definitiva**: In `AutoWalkController.java`, implementata un'isteresi temporale a 20 tick (1 secondo): quando inizia una curva ($|\Delta\text{Yaw}| > 15^\circ$), lo sprint scala a camminata una sola volta e rimane a passo stabile per tutta la manovra e per 1 secondo successivo, con rotazione fluida a $20^\circ$/tick.

---

### Record 08 — Protezione Vocale Bilaterale all'Arrivo a Destinazione
- **Problema**: Al raggiungimento del traguardo, la sintesi vocale di NVDA non pronunciava il messaggio *"Arrivato a destinazione: [Nome]"* o veniva troncata dopo pochi millisecondi.
- **Causa Radice**: All'arrivo, il riorientamento dello sguardo verso il bersaglio (`lookAtTarget`) faceva scattare sia il mirino (`NarrateCrosshair`) sia il rilevatore ostacoli (`ObstacleDetector`, rilevando la porta o il blocco a pochi centimetri dai piedi) che inviavano chiamate `MainClass.narrate(..., interrupt: true)`, tagliando istantaneamente la voce di NVDA.
- **Soluzione Definitiva**: Inserito un timer di soppressione temporanea di $1.5$ secondi sia in `NarrateCrosshair` (`suppressNarration(1500)`) sia in `ObstacleDetector` (`suppressWarnings(1500)`), garantendo una finestra protetta e ininterrotta per la pronuncia dell'arrivo.

---

### Record 09 — Routing Segnali Acustici di Feedback su Canale Primario `SoundSource.PLAYERS`
- **Problema**: Il suono della campanella di arrivo risultava debole o non udibile.
- **Causa Radice**: Il suono era instradato sulla categoria `SoundSource.BLOCKS` (Blocchi) con volume attenuato da moltiplicatori, venendo coperto da passi, porte o suoni ambientali.
- **Soluzione Definitiva**: Instradamento del segnale acustico di arrivo (`NOTE_BLOCK_BELL`) sul canale prioritario del giocatore `SoundSource.PLAYERS` con volume nitido a $0.8$ e pitch $1.2$.

---

### Record 10 — Filtraggio Barriere, Davanzali, Finestre Ermetiche & Struttura Completa Scale nel Fall Detector 2.4
- **Problema**: Il rilevatore di cadute generava falsi allarmi:
  1. Davanti a finestre con davanzale/muretto a quota piedi e vetro a quota testa (il raggio di look-ahead scavalcava il blocco solido di 1m scansionando il vuoto esterno).
  2. Sulle colonne della tromba delle scale sovrastate dai gradini superiori (il fondo dell'intercapedine è il pavimento del piano terra in assi piene).
- **Causa Radice**: 
  1. Mancanza di arresto immediato (`break;`) su blocchi solidi a quota piedi con ostacoli a quota testa in `findDangerAhead`.
  2. Mancanza di riconoscimento dei gradini sovrastanti lungo la colonna verticale in `isSafeWalkableStaircase`.
- **Soluzione Definitiva**: In `FallDetector.java`:
  - Inserito l'arresto immediato su davanzali e ostacoli solido+vetro in `findDangerAhead` e `isInsurmountableBarrier`.
  - Esteso `isSafeWalkableStaircase` per riconoscere la presenza di qualsiasi gradino `StairBlock` / `SlabBlock` lungo l'intera colonna verticale della tromba delle scale.

---

### Record 11 — Reset Impostazioni Minecraft per BOM UTF-8 in `options.txt`, Mapping Iris Shader & Adattività GPU
- **Problema**: All'avvio del gioco, Minecraft resettava lingua (`en_us`), schermo intero e impostazioni personalizzate con l'errore nei log: `java.lang.NumberFormatException: For input string: "key.keyboard.1" at OptionsKeyLwjgl3Fix`. Inoltre, il toggle degli shader entrava in conflitto con i tasti di navigazione.
- **Causa Radice**:
  1. *BOM UTF-8*: I comandi PowerShell `Set-Content -Encoding UTF8` inseriscono 3 byte di intestazione Unicode BOM (`0xEF 0xBB 0xBF`). Minecraft leggeva la prima riga come `\uFEFFversion:4903`; il check `line.startsWith("version:")` falliva impostando `version = 0`. Minecraft interpretava il file come versione legacy e attivava il DataFixerUpper obsoleto (`OptionsKeyLwjgl3Fix`) che crashava sui tasti moderni resettando tutto.
  2. *Conflitto Tasti*: Iris assegna di default il tasto `K` (conflitto con navigazione celle `I/K/J/L`) e `F8` è riservato a comandi personali di Luca.
- **Soluzione Definitiva**:
  - **Scrittura UTF-8 No-BOM Tassativa**: Qualsiasi modifica a `options.txt` deve usare `[System.IO.File]::WriteAllLines(..., New-Object System.Text.UTF8Encoding($false))` garantendo il primo byte `0x76` (`v`).
  - **Mapping Tasti Protetto**: Iris shader toggle assegnato al tasto libero **`F7`** (`key_key.iris.toggleShaders:key.keyboard.f7`) e selezione pacchetto disattivata (`key_key.iris.shaderPackSelection:key.keyboard.unknown`).
  - **Adattività Topologia GPU (Auto-Detect)**: In `instance.cfg`, analizzare dinamicamente le schede grafiche installate su qualsiasi macchina (PC Portatile o Fisso Salotto) e abilitare l'adattatore più prestante disponibile (es. GPU discreta/dedicata) con `UseDiscreteGpu=true` e `LaunchMaximized=true`.

---

### Record 12 — Diagonali 2D Tastierino Numerico (7, 9, 1, 3), Risoluzione Snap Yaw al Centraggio (Tasto 5) e Singolarità Nadir/Zenith
- **Problema**:
  1. Spostando lo sguardo in diagonale e ricentrando all'orizzonte con `5` del tastierino numerico, alla ripresa del movimento la visuale subiva un salto/reset all'indietro alla posizione angolare precedente.
  2. L'uso di Nadir (Piedi, $+90^\circ$) e Zenith (Cielo, $-90^\circ$) corrompeva istantaneamente l'orientamento orizzontale reale forzando lo Yaw a Est ($-90^\circ$).
  3. I tasti fisici `7`, `9`, `1`, `3` nel Layer 0 erano limitati a pitch puro o nadir/zenith invece di consentire un moto vettoriale 2D fluido a 8 direzioni.
- **Causa Radice**:
  1. `centerCameraHorizon()` (Tasto 5) invocava `rotateCameraTo(PlayerPositionUtils.getHorizontalFacing())`, che tramite `player.lookAt` ricalcolava e forzava lo snap dello Yaw alla griglia cardinale/ordinale discreta ($0^\circ, 45^\circ, \dots$), distruggendo lo Yaw continuo reale.
  2. `rotateCameraTo` applicato ai vettori verticali puri $(0, \pm 1, 0)$ di Nadir/Zenith scatenava la singolarità matematica $\text{atan2}(0, 0) - 90^\circ = -90^\circ$ (Est).
  3. Il crossing dell'orizzonte in `rotateCameraBy` richiamava `rotateCameraTo` forzando uno snap orizzontale ad ogni attraversamento dello $0^\circ$ di pitch.
- **Soluzione Definitiva**:
  - In `NumpadControls.java` e `CameraControls.java`:
    - `centerCameraHorizon()` azzera **esclusivamente il Pitch** (`player.setXRot(0.0f); player.xRotO = 0.0f;`), preservando intatto al 100% lo Yaw orizzontale reale del giocatore.
    - Creazione del metodo `rotateCameraToPitch(pitchDegrees)` per impostare direttamente il Pitch a $\pm 90^\circ$ senza passare da `lookAt`, eliminando alla radice la singolarità verso Est.
    - In `rotateCameraBy`, al crossing dell'orizzonte azzeramento atomico di `setXRot(0.0f)` senza invocare `rotateCameraTo`.
    - Mappatura completa dei tasti `7`, `9`, `1`, `3` nel Layer 0 come diagonali 2D atomiche $(\Delta H, \Delta V)$ con architettura Dual-Mode (tap discreto di $15^\circ$ e hold continuo $\ge 200\text{ ms}$ a $4.5^\circ/\text{tick}$).
    - Ricollocazione di `Look Nadir` e `Look Zenith` su `Alt + 1` e `Alt + 3` nel Layer 3.

