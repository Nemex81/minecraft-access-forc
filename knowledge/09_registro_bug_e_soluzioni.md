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

---

### Record 13 — Narration Shield Centralizzato (Raccolta Oggetti & Toast Ricette) e Bussola Acustica Tattile per Rotazione Continua (Tasti 4 e 6)
- **Problema**:
  1. Durante il recupero delle prede o blocchi, le notifiche di raccolta oggetti e i Toast di sblocco ricette venivano regolarmente zittiti o troncati a metà dal mirino (`NarrateCrosshair`) o dallo scanner ostacoli mentre il giocatore camminava.
  2. Durante la rotazione continua con i tasti `4` o `6` del tastierino numerico, la rapida successione di annunci vocali a $45^\circ$ (uno ogni 400ms con `interrupt: true`) causava il troncamento a raffica delle sillabe di NVDA, dando la sensazione che la voce non parlasse o si zittisse fino allo stop.
- **Causa Radice**:
  1. `ClientPacketListenerMixin` e `ToastManagerMixin` inviavano le notifiche con `interrupt: false` senza uno shield protettivo temporaneo contro le continue chiamate con `interrupt: true` emesse dal mirino a ogni cambio di blocco a terra.
  2. A $90^\circ/\text{s}$ di rotazione continua, una parola parlata richiede 600–800 ms per essere pronunciata per intero, mentre i cambi di settore a $45^\circ$ scattano ogni 400 ms, tagliando costantemente la parola precedente a metà.
- **Soluzione Definitiva**:
  - Creazione del modulo centralizzato `NarrationPriority.java` con finestra protetta ("Narration Shield") di 1.5 secondi per zittire le scansioni ambientali e accodare in sequenza pulita eventi salienti concorrenti (es. *Oggetto Raccolto* + *Ricetta Sbloccata*).
  - Introdotto debouncing di 2.0s sui `RecipeToast` per evitare lo spam di toast multipli scatenati dallo stesso item.
  - Sviluppo della Bussola Acustica Tattile in `NumpadControls.java`: durante l'hold continuo, il mirino tace ed emette un click audio leggero (`NOTE_BLOCK_HAT`) a ogni $45^\circ$ con pitch differenziato ($1.2\text{f}$ sui 4 cardinali principali, $0.9\text{f}$ sugli 8 ordinali); al rilascio del tasto (stop), vocalizza chiaramente la direzione finale precisa raggiunta (*"Sud-Est"*).
  - Introduzione dell'Enum `ContinuousFeedbackMode` (`SOUND_ONLY` default, `VOICE_ONLY`, `SOUND_AND_VOICE`, `OFF`) configurabile in GUI.
  - Conversione di `Orientation.ofHorizontal(angle)` a risoluzione puramente geometrica a 8 settori di $45^\circ$, priva di dipendenze da istanze client nulle.

---

### Record 14 — Macchina a Stati per Simulazione Mouse Hold (`wasDown()`) e Sincronizzazione Cache `options.txt`
- **Problema**: 
  1. I comandi del tastierino numerico assegnati ad attacco continuo (Tasto `0`) e uso (Tasto `Invio`) non rispondevano o bloccavano l'input di Minecraft.
  2. Le modifiche ai tasti predefiniti nel codice della mod non venivano applicate in gioco, mantenendo le vecchie associazioni sovrapposte.
- **Causa Radice**:
  1. In `NumpadControls.java`, il metodo `tick()` invocava `MouseUtils.Key.LEFT.press()` a ogni singolo frame (20 volte al secondo) senza verificare la transizione di stato con `wasDown()` e senza inviare `release()`. Questo saturava e mandava in deadlock la macchina a stati del mouse di Minecraft.
  2. Minecraft carica e preserva i binding salvati nel file `options.txt` dell'istanza; le modifiche ai valori di default in Java non sovrascrivono la cache esistente se la chiave è già presente.
- **Soluzione Definitiva**:
  - In `NumpadControls.tick()`, implementata la corretta transizione di stato:
    - Se `key.isDown() && !key.wasDown()` $\rightarrow$ invia `MouseUtils.Key.press()`.
    - Se `!key.isDown() && key.wasDown()` $\rightarrow$ invia `MouseUtils.Key.release()`.
  - Aggiornati programmaticamente i file `options.txt` di tutte le istanze PrismLauncher per sincronizzare all'istante le nuove assegnazioni dei tasti.

---

### Record 15 — Guard Rail di Modalità di Gioco per Missioni Accademia e Bussola Dinamica a 360°
- **Problema**:
  1. In modalità Sopravvivenza era possibile avviare missioni dell'Accademia impossibili da completare (es. la missione del volo).
  2. Ruotando la visuale a scatti di $15^\circ$ con i tasti `4` e `6`, la sintesi vocale ripeteva fino a 3 volte consecutive la stessa frase identica (es. *"Sud-est"*), generando disorientamento.
- **Causa Radice**:
  1. Il registro delle missioni (`MissionRegistry`) non filtrava la disponibilità in base al `GameType` del giocatore (Survival vs Creative).
  2. La suddivisione geografica classica usa 8 settori da $45^\circ$, quindi scatti da $15^\circ$ mantengono lo stesso nome di settore per 3 pressioni consecutive senza informare l'utente sull'angolo reale.
- **Soluzione Definitiva**:
  - In `AcademyManager.java`, aggiunto il controllo dei guard rail sulle abilità del giocatore (es. `player.getAbilities().mayfly`) con auto-avanzamento in caso di tappe già soddisfatte.
  - In `PlayerPositionUtils.java`, implementato il calcolo continuo dei gradi geografici standard a $360^\circ$:
    $$\text{degrees} = \text{round}\left( (\text{player.getYRot()} + 180 \bmod 360 + 360) \bmod 360 \right)$$
  - Introdotto l'Enum `RotationFeedbackMode` in `Config.NumpadControls` per offrire 5 stili di feedback selezionabili in GUI (Cardinale + Gradi, Suono + Voce, Solo Cardinale, Solo Suono, Off).

---

### Record 16 — Ottimizzazione Semantica delle Indicazioni Spaziali & Sistema di Occlusione Acustica Voxel a 5 Livelli
- **Problema**:
  1. Alla pressione del tasto `Home`, lo screen reader pronunciava frasi incoerenti e disorientanti per chi usa NVDA (es. *"5 lontano dai blocchi 2 sopra ai blocchi 3 a destra dei blocchi"*).
  2. I suoni di mob ed entità situati all'esterno dell'edificio venivano percepiti a volume pieno (come se fossero nella stessa stanza del giocatore), causando urti continui contro pareti e porte.
- **Causa Radice**:
  1. In `it_it.json`, le chiavi `minecraft_access.util.position_difference_*` contenevano un calco letterale errato dall'inglese (`"lontano dai blocchi"` anziché `"blocchi avanti"`).
  2. Il motore sonoro vanilla di Minecraft e il modulo POI non calcolavano l'occlusione materiale dei muri lungo la linea di vista del giocatore.
- **Soluzione Definitiva**:
  - In `it_it.json`, riformulate le 6 direzioni spaziali con formule naturali e lineari: `"%s blocchi avanti"`, `"%s blocchi indietro"`, `"%s blocchi in alto"`, `"%s blocchi in basso"`, `"%s blocchi a sinistra"`, `"%s blocchi a destra"`.
  - Creata la classe `AcousticOcclusion.java` per eseguire il raycast voxel discreto 3D lungo il vettore giocatore-bersaglio con scala di assorbimento a 5 livelli (Porte/Lastre $-10\%$, Assi $-18\%$, Tronchi $-28\%$, Pietra $-38\%$, Deepslate/Ossidiana $-50\%$) e soglia minima di sicurezza all'**$1\%$ (`0.01f`)**.
  - In `ObjectTracker.java` e `POIGroup.java`, integrata la modulazione del volume sonoro e l'avviso vocale automatico ` (oltre parete)` quando `totalOcclusion >= 20%`, configurabile in GUI tramite l'Enum `WallOcclusionFeedbackMode`.

---

### Record 17 — Prevenzione Falsi Allarmi nei Fluidi, Threat Sentinel Ravvicinata & Navigazione Guidata alle Porte
- **Problema**:
  1. Nuotando in mare o nei fiumi per sfuggire ai mostri notturni, il `FallDetector` entrava in un loop di allarmi continui a raffica (*"Attenzione: burrone 2 blocchi in basso, profondità 8 blocchi"*), saturando la sintesi vocale NVDA e impedendo al giocatore non vedente di percepire le minacce imminenti (es. Creeper).
  2. I mob ostili notturni (Zombie, Scheletri, Creeper, Ragni) non venivano segnalati con sufficiente tempestività e priorità audio durante l'esplorazione al buio.
  3. Durante la navigazione automatica con l'autopilota (`Alt + W`), in presenza di una porta o cancello chiuso il giocatore continuava a correre contro l'ostacolo senza ricevere indicazioni vocali sulle azioni da intraprendere.
- **Causa Radice**:
  1. I blocchi fluidi (acqua, correnti marine) hanno collision shape vuoto (`getCollisionShape().isEmpty() == true`). Di conseguenza, il vecchio algoritmo di scansione cadute scendeva lungo l'intera colonna d'acqua fino al fondale marino roccioso, interpretando la profondità dell'acqua come un burrone mortale. Inoltre, `player.isUnderWater()` in Minecraft è `true` solo quando gli occhi sono sommersi, ma è `false` mentre il giocatore galleggia o nuota a pelo d'acqua.
  2. Il modulo POI non prioritizzava in modo autonomo le entità di tipo `Enemy` entro la distanza critica ravvicinata di sopravvivenza (6 blocchi).
  3. `AutoWalkController` non monitorava lo stato aperto/chiuso dei blocchi `DoorBlock`, `FenceGateBlock` e `TrapDoorBlock` lungo il percorso.
- **Soluzione Definitiva**:
  - In `FallDetector.java`, introdotta la guardia di sicurezza estesa per tutti gli stati fluidi: `player.isInWater()`, `player.isInWaterOrRain()`, `player.isEyeInFluid(FluidTags.WATER)` con azzeramento istantaneo di sicurezza. Nei metodi `calculateDangerousDrop()` e `findDangerAhead()`, qualsiasi blocco contenente fluido (`!level.getFluidState(pos).isEmpty()`) restituisce una distanza di caduta pari a `0` (atterraggio sicuro in acqua), azzerando al 100% i falsi allarmi durante il nuoto.
  - In `POIEntities.java`, creata la **Threat Sentinel**: monitoraggio continuo dei mob `Enemy` entro 6 blocchi con riproduzione di segnale acustico 3D percussivo dedicato e annuncio vocale prioritario (*"Attenzione: %s %s"*).
  - In `AutoWalkController.java`, integrata la gestione interattiva collegata a `Config.getInstance().speechSettings.narrateHints`: arresto della marcia a 2 metri da porte o cancelli chiusi, puntamento visivo sulla porta, messaggio vocale di guida (*"Porta chiusa davanti a te. Premi Tasto Destro per aprire"*) e ripresa fluida del cammino all'apertura (*"Porta aperta. Procedi"*).

---

### Record 18 — Risoluzione Conflitto Scorciatoie su Handler Vanilla (`Alt + T` Chat), Ergonomia Mano Sinistra (`Alt + B`) e Precedenza `options.txt`
- **Data**: 2026-08-31
- **Modulo Coinvolto**: `SurvivalResourceTracker.java`, `AccessMenu.java`, `options.txt`
- **Sintomi**:
  1. La pressione di `Alt + T` apriva la schermata della chat di Minecraft anziché avviare la scansione delle risorse base.
  2. Dopo aver impostato `Alt + B` in Java, il client continuava ad attendere `R` e non rispondeva alla combinazione `Alt + B`.
- **Causa Radice**:
  1. In Minecraft Vanilla, il tasto `T` è intercettato a monte da `keyChat` nel gestore tastiera nativo, che non controlla lo stato di `Alt` e apre forzatamente la chat.
  2. Minecraft carica e preserva i keybinding salvati in `options.txt`, dando precedenza alla cache serializzata rispetto ai default definiti in `withDefault()`.
- **Soluzione Definitiva**:
  1. Riassegnato il comando a **`Alt + B`**, tasto neutro non associato a funzioni Vanilla e comodissimo per l'azionamento con la sola mano sinistra.
  2. Schermato `AccessMenu.java` con `ModifierUtils.hasAnyModifier()` per isolare `Alt + B` dal comando `B` singolo (`narrate_target`).
  3. Sincronizzato programmaticamente il file `options.txt` nell'istanza di gioco attiva.

---

### Record 19 — Conflitto Mod Grafica Iris Shaders, Modificatori Multipli Kuma e Rilevamento Colture
- **Data**: 2026-08-31
- **Modulo Coinvolto**: `DirectionalPathScanner.java`, `PathRaycaster.java`, `PathNarrationFormatter.java`
- **Sintomi**:
  1. I comandi dello scanner su Numpad (`Ctrl + Alt + 0..9`) non producevano alcun output.
  2. Su tastiera estesa (`Ctrl + Alt + I / K`), la pressione apriva il menu grafico di Iris Shaders e disabilitava gli shaderpack.
  3. Gli ortaggi commestibili piantati (Carote, Patate, Grano, Barbabietole) non venivano annunciati dalla sonda.
- **Causa Radice**:
  1. In Kuma API, i keybinding registrati senza `KeyModifiers.of(...)` pretendono `KeyModifiers.NONE` e scartano l'evento se l'utente preme modificatori (`Ctrl + Alt`).
  2. Iris Shaders registra listener globali sul tasto grezzo GLFW `K` e `I` scavalcando qualsiasi combinazione di modificatori.
  3. Le piante coltivate sono blocchi non-solidi (`CropBlock`) a quota piedi con collisione vuota, ignorate dai filtri che cercavano solo `ItemEntity` o muri solidi bloccanti.
- **Soluzione Definitiva**:
  1. Registrazione esplicita di `KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)` su tutti i binding Numpad.
  2. Adozione delle **Frecce Direzionali** per la tastiera estesa (`Ctrl + Alt + Frecce`), immuni da conflitti con mod esterne.
  3. Campionamento combinato a quota piedi (`targetFeetPos`) per intercettare `CropBlock`, `SweetBerryBushBlock`, `MelonBlock`, `PumpkinBlock` e canne da zucchero insieme alle entità a terra.

---

### Record 20 — Resilienza Iniezioni Mixin Audio (`EntityMixin`) e Compatibilità Metodi Passi 26.2
- **Data**: 2026-08-31
- **Modulo Coinvolto**: `EntityMixin.java`, `PlayerStepSound.java`, `Config.java`
- **Sintomi**: All'avvio del gioco, Minecraft crashava con `InvalidInjectionException: @ModifyArg annotation on modifyPlayAmphibiousStepSoundVolume could not find any targets matching 'playAmphibiousStepSound' in net/minecraft/world/entity/Entity`.
- **Causa Radice**: `Entity.class` in Minecraft 26.2 non implementa `playAmphibiousStepSound`. Poiché la configurazione Mixin di Minecraft Access definisce `defaultRequire = 1`, la mancata corrispondenza di un target causa il blocco immediato all'avvio.
- **Soluzione Definitiva**:
  1. Rimozione del metodo inesistente in `EntityMixin.java`, concentrando l'iniezione sui 3 metodi effettivi dei passi: `playStepSound`, `playCombinationStepSounds` e `playMuffledStepSound`.
  2. Impostazione esplicita di `require = 0` su tutti i `@ModifyArg` per garantire la massima tolleranza e compatibilità a runtime.

---

### Record 21 — Disaccoppiamento a Due Zone per Auto-Sneak Anticaduta, Riconoscimento Blocchi Speciali (Cobweb) ed Eccezioni Discesa Scale a Pioli
- **Data**: 2026-09-01
- **Modulo Coinvolto**: `FallDetector.java`, `Config.java`, `it_it.json`, `en_us.json`
- **Sintomi**:
  1. L'auto-sneak forzava l'accovacciamento 2 o 3 blocchi prima del baratro, costringendo il giocatore a muoversi a passo d'uomo per metri.
  2. Tentando di scendere dal tetto di un edificio imboccando una scala a pioli appesa alla parete, l'anticaduta bloccava l'accesso considerandola caduta mortale.
  3. L'uso della classe inesistente `CobwebBlock` causava errori di compilazione in Minecraft 1.21.x / 26.2.
- **Causa Radice**:
  1. Mancanza di disaccoppiamento tra raggio look-ahead informativo ($d \le \text{slowdownDistance}$) e soglia fisica di ciglio ($d \le 0.85\text{ m}$).
  2. Mancata scansione della colonna verticale per blocchi con tag `BlockTags.CLIMBABLE`.
  3. In Fabric 1.21.x la ragnatela è un blocco generico identificato da `Blocks.COBWEB` e non da una classe dedicata `CobwebBlock`.
- **Soluzione Definitiva**:
  1. Strutturata l'architettura a due zone: pre-allerta a distanza $> 0.85\text{ m}$ (solo avviso/slowdown) e auto-sneak sul bordo immediato $\le 0.85\text{ m}$.
  2. Implementato `isSafeClimbableDescender` per validare le discese continue su scale a pioli, liane e impalcature.
  3. Sostituito `CobwebBlock` con il check di registro `state.is(Blocks.COBWEB)` e arricchita la matrice di landing con fieno, miele, slime e neve polverosa.
  4. Aggiunto l'Edge Bump debounced (1500 ms) configurabile in Cloth Config (`EdgeBumpFeedbackMode`).

---

### Record 22 — NullPointerException in InventoryControls.changeGroup per Liste Slot Non Inizializzate
- **Data**: 2026-09-01
- **Modulo Coinvolto**: `features/inventory_controls/InventoryControls.java`
- **Sintomi**: Nei log di chiusura sessione si registrava `java.lang.NullPointerException: Cannot invoke "java.util.List.size()" because "this.currentSlotsGroupList" is null`.
- **Causa Radice**: La pressione di un tasto di navigazione rapida a gruppi dell'inventario veniva inoltrata dal gestore input anche quando la schermata inventario non aveva ancora popolato o aveva già liberato la lista `currentSlotsGroupList`.
- **Soluzione Definitiva**: Inseriti guard check difensivi `if (currentSlotsGroupList == null || currentSlotsGroupList.isEmpty()) return;` all'inizio dei metodi `changeGroup()`, `selectGroup()`, `refreshGroupListAndSelectFirstGroup()` e nel loop di `tick()`.

---

### Record 23 — Bypass da Salto sul Ciglio dei Baratri e Neutralizzazione Mixin di `LivingEntity.jumpFromGround()`
- **Data**: 2026-09-01
- **Modulo Coinvolto**: `LivingEntityMixin.java`, `FallDetector.java`
- **Sintomi**: Nonostante l'auto-accovacciamento fosse attivo sul ciglio del burrone, premendo la barra spaziatrice il personaggio spiccava il salto, staccava i piedi da terra e precipitava nel vuoto (cadute registrate da 20 blocchi di altezza).
- **Causa Radice**:
  1. *Fisica Minecraft*: Lo sneak blocca l'oltrepassamento del bordo solo a contatto con il suolo (`player.onGround() == true`). In aria, l'inerzia orizzontale fa superare il ciglio.
  2. *Corsa dei Tick*: Nel tick del client, `LocalPlayer.aiStep()` elabora l'input della tastiera e invoca `LivingEntity.jumpFromGround()` *prima* che l'evento `ClientPlayingTick.AFTER` possa intervenire. Azzerare `keyJump.setDown(false)` a posteriori interveniva a salto già avvenuto.
- **Soluzione Definitiva**:
  1. Iniezione cancellabile in `LivingEntityMixin.java` su `jumpFromGround()` con `@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)` che chiama `ci.cancel()` se `FallDetector.isAutoSneakActive()` è `true`.
  2. Rimozione dell'istruzione inefficace `keyJump.setDown(false)` da `FallDetector.java`.
  3. Il salto torna libero all'istante non appena ci si allontana dal ciglio con `S` o si disattiva la protezione con `Ctrl + Alt + F`.

---

### Record 24 — Corsa Critica Mirino/Visuale e Coordinatore `CrosshairFeedbackManager` a Token Componibili (Punto 15)
- **Data**: 2026-09-01
- **Moduli Coinvolti**: `CrosshairFeedbackManager.java`, `CrosshairReadingOrder.java`, `NarrateCrosshair.java`, `CameraControls.java`, `NumpadControls.java`, `Config.java`
- **Sintomi**: Durante la rotazione della visuale (tastierino o tasti camera) o il centramento dell'orizzonte (Numpad 5), la voce dello screen reader NVDA subiva troncamenti sistematici a metà frase, alternando disordinatamente frammenti di direzione e nomi di blocchi.
- **Causa Radice**: Disaccoppiamento architetturale tra il raycast periodico di `NarrateCrosshair` e i comandi di orientamento di `CameraControls`/`NumpadControls`. Entrambi i moduli emettevano chiamate concorrenti `MainClass.narrate(..., interrupt: true)` nello stesso tick o in tick ravvicinati.
- **Soluzione Definitiva**:
  1. *Pattern Single Source of Truth & Coordinator*: Introdotto `CrosshairFeedbackManager` come unico punto di assemblaggio ed emissione vocale per mirino e orientamento.
  2. *Modello a 5 Token Indipendenti*: Separazione dei dati in 5 toggle booleani configurabili in Cloth Config (`includeBlock`, `includeDistance`, `includeCardinal`, `includeCompassDegrees`, `includePitchAngle`).
  3. *Enum di Ordinamento Strutturale*: `CrosshairReadingOrder` (`TARGET_FIRST`, `ORIENTATION_FIRST`, `TARGET_CARDINAL_INLINE`) per governare la sintassi della frase vocale senza combinatoria rigida.
  4. *Debouncing & Sincronizzazione Temporale*: Soppressione a 100ms e allineamento dello stato `previousTarget` durante le rotazioni per garantire un'unica emissione atomica priva di duplicazioni.

---

### Record 25 — Diagnostica Telemetria: ClassCastException al Cambio Tab Ricettario e Annotation @Excluded in Config
- **Data**: 2026-09-01
- **Moduli Coinvolto**: `InventoryControls.java`, `Config.java`
- **Sintomi**:
  1. Alla pressione di `V` / `Shift+V` per cambiare scheda nel ricettario / crafting screen, l'input si bloccava con eccezione `ClassCastException: RecipeBookCategory cannot be cast to SearchRecipeBookCategory`.
  2. All'apertura della schermata impostazioni grafiche, loggato errore `No GUI provider registered for field 'Config.instance'`.
- **Causa Radice**:
  1. In `InventoryControls.java:837`, l'istruzione di debug forzava il cast `((SearchRecipeBookCategory) category).name()` su un oggetto che in Minecraft 26.2 è un `RecipeBookCategory` generico.
  2. In `Config.java:24`, il singleton statico `instance` non possedeva l'annotazione `@ConfigEntry.Gui.Excluded`, inducendo Cloth Config a tentare la generazione di un widget di modifica GUI per la classe di configurazione stessa.
- **Strategia Correttiva Certificata**:
  1. In `InventoryControls.java`, rimozione del cast insicuro in favore di `category != null ? category.toString() : "null"` con protezione try-catch difensiva.
  2. In `Config.java`, aggiunta di `@ConfigEntry.Gui.Excluded` sul campo `instance`.
- **Riferimento Dettagliato**: [`docs/report/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)

---

### Record 26 — Risoluzione Categorie Ricettario 26.2 (BuiltInRegistries vs toString), Statistiche di Pagina e Navigazione a 4 Frecce (Rev 26.1 - 26.6)
- **Data**: 2026-09-02
- **Moduli Coinvolti**: `InventoryControls.java`, `RecipeBookPageAccessor.java`, `it_it.json`, `en_us.json`
- **Sintomi**:
  1. Il cambio scheda ricettario (`V`/`Shift+V`) pronunciava la stringa Java grezza `net.minecraft.world.item.crafting.RecipeBookCategory@78b56307` anziché il nome in italiano.
  2. Le categorie vuote pronunciavano la chiave non localizzata `minecraft_access.inventory_controls.recipe_category_empty`.
  3. Il cambio pagina girava visivamente ma non comunicava all'utente non vedente lo stato delle ricette, né distingueva i limiti di inizio/fine lista.
  4. La categoria Redstone era tradotta letteralmente come *"Pietrarossa"*, inducendo confusione con i materiali da costruzione edili.
- **Causa Radice**:
  1. In Minecraft 26.2, `RecipeBookCategory` è data-driven e non sovrascrive `.toString()`, richiedendo l'estrazione della chiave tramite `BuiltInRegistries.RECIPE_BOOK_CATEGORY`.
  2. Mancanza della chiave `recipe_category_empty` nei dizionari linguistici.
  3. Mancanza di accessors per `currentPage` / `totalPages` e di un modulo di calcolo statistiche aggregate sui `RecipeButton` visibili.
- **Soluzione Definitiva**:
  1. Risoluzione dei nomi categoria tramite `BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey()` e `SearchRecipeBookCategory`.
  2. Adozione della dicitura contestuale *"Meccanismi e Redstone"* in `it_it.json`.
  3. Esposizione di `getCurrentPage()` e `getTotalPages()` in `RecipeBookPageAccessor`.
  4. Implementazione del modulo `RecipePageStats` per comporre dinamicamente la sintesi delle ricette (totali, realizzabili, non realizzabili) con concordanza grammaticale singolare/plurale.
  5. Boundary gating intelligente: blocco preventivo dei click a vuoto e annuncio differenziato per *"Prima pagina"*, *"Ultima pagina"* e *"Unica pagina"*.
  6. Mappatura delle 4 Frecce Direzionali per navigare tutti i container con disaccoppiamento da `EditBox`.
