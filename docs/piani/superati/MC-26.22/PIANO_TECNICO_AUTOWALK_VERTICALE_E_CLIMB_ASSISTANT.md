# Piano Tecnico Correttivo Formale MC-26.22 — AutoWalk Verticale e Climb Assistant

> Documento storico superato, conservato il 2026-09-12 senza dichiarazione di collaudo concluso. Riferimento operativo corrente: `docs/piani/attivi/PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md`. Stati e checklist sottostanti descrivono la precedente iterazione; restano consultabili le specifiche architetturali non sostituite dal nuovo piano.

- **ID revisione**: MC-26.22
- **Tipologia**: Correttivo strutturale con estensione implementativa del sottosistema AutoWalk
- **Autore del piano**: GPT Codex / ChatGPT, copilota ausiliario e senior reviewer
- **Committente**: Luca
- **Revisori previsti**: Luca e Antigravity
- **Data e ora**: 2026-09-11 10:52 CEST
- **Stato operativo**: `[IN STESURA 1A — REDATTO, IN ATTESA DI APPROVAZIONE DI LUCA]`
- **Ramo osservato**: `feat/dual-fall-safety-subsystem`
- **Target AVF**: incremento **MINOR** (`M + 1`) per nuova capacità utente; il numero numerico esatto resta da calcolare in Fase 3 da un riferimento Git leggibile e pulito
- **Audit Protocollo 12**: completato sul piano; non costituisce validazione di codice o runtime
- **Strategia correlata**: `docs/strategie/attive/STRATEGIA_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md`
- **Handover correlato**: `docs/report/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md`
- **Registro RRU**: `docs/report/REGISTRO_REVISIONI.md`, voce Rev MC-26.22

---

## 0. Sommario operativo e checkpoint di gating

- [x] `[ESEGUITO IN 1A]` Letti handover, strategia, hub `GEMINI.md` e sole schede `knowledge/04`, `knowledge/05`, `knowledge/10`.
- [x] `[ESEGUITO IN 1A]` Verificato il working tree e analizzati in sola lettura i contratti effettivi di AutoWalk, botole, anticaduta, input, Access Menu e I18N.
- [x] `[ESEGUITO IN 1A]` Redatti i Named Contracts D0..D9 e la matrice preventiva a tre livelli.
- [ ] `[DA CONVALIDARE]` Approvazione formale di Luca del presente piano.
- [ ] `[DA AVVIARE SOLO DOPO APPROVAZIONE]` Sotto-Fase 1B: modifica dei sorgenti, test e build.
- [ ] `[DA AVVIARE DOPO 1B VERDE]` Fase 2: deploy provvisorio e collaudo manuale in-game con NVDA.
- [ ] `[DA AVVIARE SOLO DOPO COLLAUDO DI LUCA]` Fase 3: chiusura, AVF numerico, archiviazione e integrazione.

### Stop obbligatorio

Questo documento autorizza esclusivamente la pianificazione formale. La sua redazione non autorizza modifiche a codice, configurazioni, traduzioni, test, build, JAR, istanze PrismLauncher, registro RRU, strategia o storia Git.

---

## 1. Obiettivo verificabile

Realizzare due modalità che condividono la stessa geometria e la stessa cinematica:

1. **AutoWalk verticale globale**:
   - Se una destinazione raggiungibile richiede una colonna arrampicabile valida, allora A* deve includere nella rotta ingresso, transito verticale e sbarco.
   - Se la colonna è interrotta, ostruita, pericolosa o priva di pianerottolo sicuro, allora A* non deve generare l’arco verticale.
2. **Climb Assistant tattico**:
   - Se Luca invoca il comando in una posizione di salita o discesa non ambigua e geometricamente valida, allora il sistema deve raggiungere il primo pianerottolo sicuro, stabilizzarsi e restituire il controllo manuale.
   - Se il contesto è ambiguo o non sicuro, allora nessun input motorio deve essere iniettato e deve essere fornito un solo feedback accessibile ad alto segnale.

Il risultato richiesto non è “premere avanti più a lungo”, ma modellare una transizione topologica verticale esplicita, con contratto geometrico, tipo di movimento, proprietario degli input e condizioni terminali verificabili.

---

## 2. Baseline tecnica verificata nel ramo attivo

1. `AutoWalkPathfinder` restituisce oggi un `PathResult` basato su `List<BlockPos>`; il tipo fisico del segmento non sopravvive alla ricostruzione del percorso.
2. `getValidNeighbors` genera movimento piano, step-up e drop-down laterali, ma non genera ingresso, transito e uscita lungo una colonna arrampicabile.
3. `LadderBlock` è già passabile e non calpestabile: questa distinzione storica va preservata. Una scala a pioli non deve diventare falsamente un pavimento.
4. `isWithinBounds` limita X e Z ma non Y. L’introduzione di vicini verticali richiede un limite tridimensionale e i limiti di costruzione del mondo.
5. Il costo attuale contiene penalità di movimento e `PathResult.totalDistance` riceve il costo A*. Con un rapporto temporale di scalata, costo e metri geometrici devono essere separati per non narrare come distanza un valore che non è una distanza.
6. L’euristica verticale attuale usa moltiplicatori 1.5 e 2.5. Con un nuovo arco verticale di costo fisico circa 1.83, il valore 2.5 può sovrastimare il costo residuo e non può essere assunto come euristica ammissibile.
7. `RouteNavigator` conserva nodi e indice, ma non espone il tipo del segmento attivo.
8. `AutoWalkMotor` possiede gli stati `IDLE`, `WALKING`, `JUMPING`, `SWIMMING`, `ARRIVED`, `CANCELLED`; l’avanzamento ordinario rifiuta differenze verticali pari o superiori a un blocco.
9. Il takeover attuale distingue lo Shift fisico dallo Shift sintetico, ma non dispone ancora di un probe raw completo per tutti gli input che il motore può possedere.
10. `TraversalSafetyAnalyzer` riconosce già ladder, vine, scaffolding e `BlockTags.CLIMBABLE`; `SafeDescentCandidate` descrive una discesa validata; `SafetyMovementGuard` è l’unico proprietario ammesso dello sneak sintetico. Questi contratti vanno riusati, non duplicati.
11. `DoorInteractionManager` possiede già `activeTrapdoorSessions`, `TrapdoorPassageSession` e Passage Renewal. Tuttavia l’auto-apertura di prossimità è eseguita solo quando AutoWalk non è attivo: una scalata AutoWalk richiede quindi un’API cooperativa esplicita verso quel gestore.
12. `KEY_RBRACKET` appartiene a `MouseSimulation` e simula il tasto destro. Non è tecnicamente lo stesso oggetto del key mapping vanilla `keyUse`; l’intercettazione deve avvenire prima della simulazione e deve mantenere il comportamento storico quando il Climb Assistant non accetta il contesto.
13. La ricerca nel solo codice del repository non mostra assegnazioni di `Alt+S`. Ciò prova l’assenza di conflitto interno noto, non l’assenza assoluta di conflitti con vanilla, launcher o altre mod: la verifica runtime resta obbligatoria.
14. `addon.accessmenu.AutoWalk` e `CoreAddon` forniscono il modello di registrazione da seguire per `addon.accessmenu.AutoClimb`.
15. Lo stato Git locale segnala errori di permesso su alcuni oggetti loose. Il ramo e il working tree sono stati osservati, ma non si certifica in questa fase la relazione completa con remote o tag.

---

## 3. Perimetro e non-obiettivi

### 3.1 Incluso

- Scale a pioli a parete.
- Viti a parete, incluse configurazioni con più facce valide.
- Impalcature, con salto sintetico in salita e sneak sintetico centralizzato in discesa.
- Blocchi riconosciuti da `BlockTags.CLIMBABLE`, comprese piante rampicanti verticali, solo se la loro cinematica è classificata esplicitamente.
- Pianerottolo inferiore e superiore, botola cooperativa, headroom, collisioni reali, pericoli e mutazioni del mondo durante il transito.
- AutoWalk verso POI, entità e waypoint su quote diverse.
- Climb Assistant senza necessità di un POI selezionato.
- Attivazione tramite `Alt+S`, interazione contestuale su `KEY_RBRACKET` e Access Menu.

### 3.2 Escluso

- Volare, elitre, nuoto verticale, ascensori d’acqua, bolle, teletrasporto e parkour.
- Movimento verticale libero in aria o su pareti non arrampicabili.
- Aumenti del budget A*, ritardi artificiali o pesi magici come sostituto dell’arco mancante.
- Apertura autonoma di botole di ferro o varchi non interagibili.
- Scelta automatica pericolosa quando salita e discesa sono entrambe plausibili.
- Modifiche ai volumi audio 3D congelati.

---

## 4. Invarianti trasversali

1. **Fail closed**: se geometria, input raw, landing o lease di sicurezza non sono affidabili, il movimento non parte o si arresta sul primo punto sicuro.
2. **Un solo modello geometrico**: pathfinder, assistente tattico e anticaduta condividono classificazione e identità della colonna.
3. **Semantica del segmento preservata**: una scalata non può essere dedotta soltanto da due coordinate dopo la ricostruzione del percorso.
4. **Corpo continuo**: ogni verifica usa il prisma del giocatore e le `VoxelShape`, non un punto al centro del voxel.
5. **Input con proprietario**: ogni tasto sintetico viene rilasciato solo dal componente che lo ha acquisito; lo sneak resta sotto `SafetyMovementGuard`.
6. **Takeover fisico prioritario**: un input manuale raw affidabile interrompe subito l’assistenza, salvo il breve latch che assorbe il medesimo tasto usato per attivarla.
7. **Sbarco prima del repath**: durante una colonna verticale non si abbandona la scalata per inseguire un’entità mobile; il repath avviene dopo un landing stabile o dopo cancellazione sicura.
8. **Quiete sensoriale**: nessuna voce a ogni blocco verticale; inizio, errore, takeover e arrivo sono gli eventi vocali principali. La progressione può essere sonora e limitata.
9. **Compatibilità orizzontale**: porte, curve a L, step, drop e nuoto preesistenti non cambiano semantica quando la rotta non contiene scalate.

---

## 5. Named Contracts D0..D9

## Contratto D0 — Vocabolario geometrico unico e analizzatore puro

### Scopo

Introdurre un modello immutabile condiviso che descriva il “cosa” prima del “come”.

### Componenti previsti

- Nuovo `features.safety.traversal.ClimbableGeometry`:
  - classifica `WALL_MOUNTED`, `SCAFFOLDING` e `FREE_CLIMBABLE`;
  - risolve le facce valide senza assumere che ogni rampicante possieda `LadderBlock.FACING`;
  - mantiene `TraversalSafetyAnalyzer.isClimbable` come facciata compatibile oppure ne delega l’implementazione al nuovo classificatore.
- Nuovo record immutabile `features.safety.traversal.ClimbTraversal`:
  - direzione `UP` o `DOWN`;
  - tipo cinematico;
  - `entryPos`, prima e ultima cella della colonna;
  - `landingPos` sicura;
  - faccia di supporto opzionale;
  - botola opzionale;
  - `columnId` stabile.
- Nuovo `features.safety.traversal.ClimbTraversalAnalyzer`, puro e side-effect free.

### Precondizioni

- Livello leggibile, posizione del giocatore, bounding box e direzione candidata disponibili.
- Tutte le celle coinvolte entro limiti del mondo e raggio autorizzato.

### Postcondizioni

- Restituisce una transizione solo se ingresso, colonna e landing sono continui e compatibili con il tipo cinematico.
- Per una ladder, la direzione verso la parete deriva dall’orientamento reale del blocco; per una vine vengono valutate tutte le facce presenti; per scaffolding non viene inventata una parete.
- Un landing superiore richiede piedi e testa liberi e supporto calpestabile; un landing inferiore richiede supporto reale e spazio corporeo.
- Lava, fuoco, cactus, magma, powder snow non autorizzata e collisioni estranee invalidano la transizione.

### Invariante anti-regressione

`LadderBlock` resta passabile nel corridoio e non diventa `isStandable` come pavimento.

### Complessità

- Analisi locale A*: O(1) per vicino verticale, con cache per ricerca indicizzata da `columnId`.
- Risoluzione tattica dell’intera colonna: O(h), con `h` limitato dal raggio configurato, dal bordo del mondo e comunque non oltre 64 blocchi per singola scansione.

## Contratto D1 — Espansione A* con archi verticali atomici e costo fisico

### Modifiche previste

- Estendere `AutoWalkPathfinder.NeighborMove` con un tipo di movimento e metadati di scalata, preservando un costruttore compatibile per i test e gli usi storici.
- Da un landing inferiore valido generare `MOUNT_UP` verso la prima cella arrampicabile.
- Dentro la stessa colonna generare soltanto `CLIMB_UP` o `CLIMB_DOWN` di delta Y unitario.
- Dall’ultima cella generare `DISMOUNT_TOP` o `DISMOUNT_BOTTOM` soltanto verso il landing validato.
- Vietare diagonali verticali generiche, salti di colonna, cambio di faccia non validato e collegamenti tra colonne soltanto adiacenti.
- Estendere `isWithinBounds` a X, Y, Z e ai limiti minimi e massimi del livello.

### Costo g

- Definire costanti nominate di velocità, non coefficienti anonimi.
- Costo base orizzontale: tempo normalizzato per un metro a circa 4.3 m/s.
- Costo verticale iniziale: rapporto `4.3 / 2.35`, circa `1.83` per metro, come richiesto dalla strategia.
- Mount e dismount usano la loro distanza fisica e non ricevono bonus per “preferire” una scala.
- Porte e botole conservano una penalità soltanto se rappresentano un’interazione reale autorizzata.

### Euristica

- Sostituire il moltiplicatore verticale 2.5 con un limite inferiore ammissibile rispetto alle mosse effettivamente disponibili.
- Combinare distanza orizzontale octile e limite verticale tramite una formula che non sommi due costi già pagabili dalla stessa mossa diagonale; una forma ammessa è il massimo tra i due lower bound direzionali.
- Mantenere distinti `NO_PATH` e `SEARCH_BUDGET_EXHAUSTED`.
- Nessun incremento di `MAX_EXPLORED_NODES` fa parte di MC-26.22.

### Distanza e costo

- `PathResult` deve distinguere metri geometrici da costo di attraversamento.
- La narrazione “metri” usa esclusivamente la lunghezza geometrica, mai `gCost` con penalità o rapporti temporali.

### Postcondizione

Se esistono una rampa e una scala entrambe sicure, la scelta deriva dal costo fisico complessivo; se la scala è l’unica connessione, il grafo possiede finalmente gli archi necessari.

## Contratto D2 — Percorso tipizzato e RouteNavigator

### Modello

- Aggiungere un record immutabile `RouteSegment` con:
  - origine e destinazione;
  - tipo `WALK`, `STEP_UP`, `DROP_DOWN`, `SWIM`, `CLIMB`;
  - metadati `ClimbTraversal` soltanto per `CLIMB`.
- Conservare temporaneamente `PathResult.path()` come vista `List<BlockPos>` per compatibilità.
- Aggiungere l’elenco dei segmenti allineato agli archi: per N nodi devono esistere N meno 1 segmenti.
- Ricostruire nodi e segmenti dallo stesso chain di `PathNode`, senza inferenza postuma basata sul mondo mutabile.

### RouteNavigator

- Installare nodi e segmenti in modo atomico a ogni start o repath.
- Esporre `getCurrentSegment()` coerente con `currentPathIndex` e con la semantica radice-primo nodo già esistente.
- Conservare `firstSegmentPending`, `routeRevisionId`, target e goal.
- In caso di mismatch tra nodi e segmenti, rifiutare la rotta e restituire un esito sicuro, senza `IndexOutOfBoundsException`.

### Invariante anti-regressione

Le rotte storiche costruite solo con `BlockPos` nei test vengono adattate a segmenti ordinari e mantengono gli stessi indici.

## Contratto D3 — FSM cinematica Mount, Climb e Dismount

### Stati

- Aggiungere `CLIMBING_UP` e `CLIMBING_DOWN` a `AutoWalkMotor.State` e alla facciata legacy `AutoWalkController.State`.
- Includere entrambi in `isActive()`.
- Usare una sottofase interna `MOUNT`, `TRANSIT`, `DISMOUNT`; non creare un secondo motore concorrente.

### Mount

- Disattivare sprint e input non pertinenti.
- Per blocchi a parete, allineare yaw verso la parete di supporto e centrare lateralmente il giocatore prima di avanzare.
- Per scaffolding, centrare il giocatore nel volume prima di acquisire salto o sneak.
- Non iniziare il transito se la verifica runtime differisce dalla geometria pianificata.

### Transit

- Ladder e rampicanti a parete in salita: pressione avanti posseduta dal motore, orientamento mantenuto verso il supporto.
- Rampicanti liberi: comando definito dal tipo cinematico verificato, senza inventare un `FACING`.
- Scaffolding in salita: salto sintetico posseduto dal motore.
- Discesa ladder/vine: controllo verticale senza sneak artificiale salvo quanto richiesto dal tipo.
- Discesa scaffolding: richiesta di sneak al solo arbitro di sicurezza del Contratto D5.
- Avanzamento del segmento basato sulla quota continua e sull’identità della colonna, non soltanto sulla distanza orizzontale.

### Dismount

- Interrompere l’input verticale al raggiungimento della quota di uscita.
- Eseguire il solo passo orizzontale necessario verso il centro del landing validato.
- Dichiarare stabile soltanto se `player.onGround()` è vero e la distanza orizzontale dal centro è inferiore a 0.35 m.
- Solo dopo stabilità: avanzare il waypoint, chiudere la lease di discesa e consentire eventuale repath.

### Watchdog

- Progresso verticale minimo osservabile: 0.05 m entro una finestra di 15 tick.
- Prima finestra senza progresso: un solo tentativo di riallineamento e remount.
- Seconda finestra consecutiva senza progresso: arresto sicuro, rilascio di tutti gli input posseduti ed evento `stuck`.
- Nessun `Thread.sleep`; contatori e snapshot sono valutabili headless.

### Mutazioni e lifecycle

- Se un rung scompare, compare un ostacolo, il landing diventa pericoloso o la botola non si apre, allora arrestare o tornare all’ultimo landing sicuro senza continuare alla cieca.
- Morte, cambio dimensione, disconnessione e apertura GUI rilasciano input, lease e sessioni.
- Un target entità che si sposta durante il transito viene ricalcolato soltanto dopo il landing.

## Contratto D4 — Climb Assistant tattico e risoluzione della direzione

### Componenti

- Nuovo `features.autowalk.ClimbAssistantController`, responsabile solo di rilevamento contestuale, scelta sicura della direzione e avvio.
- Riuso di `MovementCoordinator`, `RouteNavigator` e `AutoWalkMotor` tramite una sessione tipizzata `TACTICAL_CLIMB`; nessun motore parallelo.
- Il bersaglio tattico è il landing della transizione, quindi non richiede un POI selezionato.

### Regole “Se... Allora”

- Se il giocatore è su un landing superiore calpestabile e davanti esiste una colonna completa con landing inferiore sicuro, allora scegliere `DOWN`.
- Altrimenti, se il giocatore è sul landing inferiore o davanti alla base di una colonna con landing superiore sicuro, allora scegliere `UP`.
- Altrimenti, se il giocatore è già dentro una colonna e una sola direzione termina in un landing sicuro, allora scegliere quella direzione.
- Altrimenti, se entrambe le direzioni sono valide o nessuna lo è, allora non muovere e narrare una sola indicazione di ambiguità o assenza landing.
- Se una sessione tattica è già attiva e lo stesso comando viene premuto di nuovo, allora cancellare la sessione e restituire il controllo.
- Se è attivo un AutoWalk globale, allora il comando tattico non sostituisce silenziosamente la rotta globale.

### Configurazione minima

- Aggiungere `Config.ClimbAssistant` con `enabled` e `contextualUseEnabled`.
- Non accoppiare l’abilitazione del comando tattico alla presenza di un target AutoWalk.

## Contratto D5 — Lease di discesa controllata e proprietà dello sneak

### Problema da prevenire

Il tick del rilevatore anticaduta può revocare una discesa quando non vede intento orizzontale; la discesa verticale assistita può invece essere valida anche con movimento X/Z nullo. Inoltre lo scaffolding richiede sneak, ma `AutoWalkMotor` non può diventare un secondo writer di `keyShift`.

### Soluzione

- Definire una porta stretta `ControlledDescentPort`, iniettata nel coordinatore del movimento.
- Il sottosistema anticaduta concede una lease soltanto per un `SafeDescentCandidate` o `ClimbTraversal` già validato e per il relativo `columnId`.
- La lease contiene proprietario, identità colonna, requisito eventuale di sneak sintetico e scadenza rinnovabile a tick.
- `SafetyMovementGuard` resta l’unico writer dello sneak effettivo e combina:
  - intenzione Shift fisica raw;
  - token di freno anticaduta;
  - token di discesa scaffolding validata.
- Una revoca del detector di prossimità non può cancellare la lease posseduta da AutoWalk o Climb Assistant.
- Uscita dalla colonna, takeover, errore, GUI, morte o timeout chiudono la lease in modo idempotente.
- Se il probe raw non è affidabile, non si apre una nuova discesa automatica pericolosa.

### Integrazione prevista

- Conservare un riferimento all’istanza facade `FallDetector` registrata in `MainClass`, oppure iniettare la porta al costruttore senza dipendere dal concreto `CentralFallSafetyManager`.
- Non accedere direttamente ai campi interni di `ProximityFallDetector` dal motore.

## Contratto D6 — Cooperazione botole senza duplicare DoorInteractionManager

### Correzione necessaria

Il semplice affidamento al ramo “manual auto-open” non basta durante AutoWalk, perché quel ramo è escluso quando AutoWalk è attivo.

### API cooperativa

- Esporre in `DoorInteractionManager` una richiesta idempotente di passaggio botola per una posizione canonica.
- La richiesta deve riusare cooldown, interazione, `TrapdoorPassageSession`, Passage Renewal, auto-close, narrazione configurata e clock testabile già esistenti.
- `AutoWalkMotor` chiede l’apertura e attende l’esito; non invoca direttamente `gameMode.useItemOn` e non crea una seconda mappa di sessioni.
- Il pathfinder ammette una botola chiusa nel solo arco verticale se:
  - è una botola interagibile, non di ferro;
  - `autoOpenDoors` e `includeGatesAndTrapdoors` sono abilitati nella policy di ricerca;
  - esistono spazio corporeo e landing sicuro dopo l’apertura.
- Se la botola è già aperta, la transizione non paga una seconda interazione.
- Se auto-open è disabilitato o l’apertura fallisce, la rotta non attraversa la botola e il motore si arresta senza forzarla.

### Compatibilità

La regola storica “botola chiusa non passabile nel fallback orizzontale” resta invariata. L’eccezione è tipizzata e limitata alla transizione verticale cooperativa.

## Contratto D7 — Arbitraggio input, tre canali e takeover raw

### `KEY_RBRACKET` e interazione contestuale

- Inserire in `MouseSimulation` un hook stretto sul fronte di pressione del tasto destro simulato.
- Se Shift fisico raw è premuto, allora bypassare sempre il Climb Assistant e preservare la simulazione del tasto destro per posa o uso oggetti.
- Se il controller accetta una scalata valida, allora consumare l’intero ciclo press/release corrente senza inviare il click al gioco.
- Se il controller rifiuta o è disabilitato, allora eseguire esattamente il comportamento storico di `MouseUtils.Key.RIGHT`.
- Una pressione tenuta non può avviare sessioni duplicate.

### `Alt+S`

- Registrare `other.auto_climb` con `InputConstants.KEY_S` e solo `KeyModifier.ALT`.
- Applicare obbligatoriamente `ModifierUtils.hasAltOnly()`.
- Usare un latch di attivazione fino al rilascio fisico di S, così il tasto che ha avviato la funzione non provoca takeover nello stesso gesto.
- Verificare in-game conflitti con vanilla e mod dell’istanza; in caso di conflitto il key mapping Kuma resta rimappabile.

### Access Menu

- Aggiungere `addon.accessmenu.AutoClimb`.
- Registrare `auto_climb` in `CoreAddon` senza assegnare un secondo tasto predefinito.

### Takeover

- Estendere il probing raw a W, A, S, D, salto e Shift, distinguendo gli input fisici da quelli posseduti dal motore o da `SafetyMovementGuard`.
- Dopo il latch di attivazione, qualunque input manuale di movimento affidabile cancella immediatamente la sessione.
- Il rilascio pulisce solo i tasti sintetici posseduti dal sistema; non annulla una pressione fisica ancora attiva.

## Contratto D8 — Eventi cognitivi, I18N e quiete sensoriale

### Eventi `SourceDomain.MOVEMENT`

- Aggiungere factory in `MovementCoordinator` per:
  - inizio salita;
  - inizio discesa;
  - avanzamento di un nodo verticale;
  - landing completato;
  - contesto ambiguo o landing non sicuro;
  - stallo o mutazione della colonna.
- Usare `StateSignature` con `columnId`, direzione e fase per deduplicare.
- I nodi intermedi emettono al massimo un cue sonoro throttled; nessuna voce per ogni rung.
- Arrivo del Climb Assistant significa landing stabile, non semplice raggiungimento della quota.

### Chiavi previste

- `access_menu_function.minecraft_access.auto_climb`
- `key.minecraft_access.other.auto_climb`
- `minecraft_access.climb_assistant.ambiguous`
- `minecraft_access.climb_assistant.cancelled`
- `minecraft_access.climb_assistant.disabled`
- `minecraft_access.climb_assistant.landing_unsafe`
- `minecraft_access.climb_assistant.started_down`
- `minecraft_access.climb_assistant.started_up`
- `minecraft_access.climb_assistant.stuck`
- `minecraft_access.climb_assistant.completed`

### Vincoli

- Nessuna stringa utente hardcoded.
- Aggiornare soltanto `it_it.json` ed `en_us.json`.
- Inserire ogni chiave nel rigoroso ordine alfabetico crescente globale del file.
- Conservare volumi posizionali tra 0.7f e 0.8f; nessun nuovo volume fuori fascia.

## Contratto D9 — Test seams, copertura deterministica e telemetria tecnica

### Seams obbligatori

- Analyzer geometrici puri con `BlockGetter` o adattatore equivalente.
- Snapshot cinematico immutabile in ingresso e comando motorio immutabile in uscita per testare la FSM senza client grafico.
- Clock o contatore tick iniettabile per watchdog, lease, cooldown botola e throttling eventi.
- Probe raw iniettabile per takeover e bypass Shift.
- Porte iniettabili per botole, sicurezza, output cognitivi e input virtuali.
- Reset idempotente di tutti i seam tra test.

### Suite automatica minima

1. `ClimbTraversalAnalyzerTest`:
   - ladder nei quattro orientamenti;
   - vine con una o più facce;
   - scaffolding;
   - tagged climbable privo di `FACING`;
   - top e bottom landing;
   - soffitto basso e collisione laterale;
   - colonna spezzata, hazard, bordo mondo e limite raggio;
   - botola aperta, lignea chiusa autorizzata, ferro chiusa rifiutata.
2. `AutoWalkPathfinderTest`:
   - rotta multi-piano con scalata in entrambe le direzioni;
   - archi verticali unitari e assenza di teletrasporto;
   - scelta costo-fisica tra rampa e scala;
   - euristica non sovrastimante su fixture note;
   - separazione metri/costo;
   - regressione `NO_PATH` contro `SEARCH_BUDGET_EXHAUSTED`;
   - invarianti porte, curve a L, step, drop, ladder passabile ma non standable.
3. `RouteNavigatorTest`:
   - allineamento N nodi/N meno 1 segmenti;
   - indice radice-primo segmento;
   - repath atomico e rotta malformata rifiutata.
4. `AutoWalkMotorTest`:
   - tutte le transizioni FSM Mount/Transit/Dismount;
   - salita e discesa per ciascun tipo cinematico;
   - landing stabile;
   - watchdog 15 più 15 tick a 0 ms;
   - mutazione colonna e cleanup input;
   - repath di entità rinviato al landing.
5. `ClimbAssistantControllerTest`:
   - precedenza deterministica UP/DOWN;
   - ambiguità fail closed;
   - nessun requisito POI;
   - seconda pressione cancella.
6. `SafetyMovementGuardTest` e `CentralFallSafetyManagerTest`:
   - lease valida non revocata dal detector ordinario;
   - lease scaduta o colonna diversa revocata;
   - sneak scaffolding e Shift fisico riconciliati senza doppio writer.
7. `DoorInteractionManagerTest`:
   - richiesta idempotente, cooldown, Passage Renewal, chiusura dopo uscita e timeout.
8. `MouseSimulationTest` o seam equivalente:
   - click consumato solo per scalata accettata;
   - fallback identico per contesto non valido;
   - bypass raw Shift;
   - pressione tenuta senza doppio avvio.
9. `MovementCoordinatorTest`:
   - eventi nel dominio MOVEMENT, deduplicazione, quiete intermedia e cleanup lifecycle.
10. Test I18N:
    - chiavi presenti in entrambe le lingue;
    - insiemi IT/EN coerenti;
    - ordinamento alfabetico crescente.

### Telemetria tecnica non vocale

- Loggare a livello debug: `columnId`, tipo, direzione, fase, quota corrente, progresso, esito landing, lease e botola.
- Non loggare ogni tick in modalità normale.
- Distinguere `INVALID_GEOMETRY`, `UNSAFE_LANDING`, `TRAPDOOR_BLOCKED`, `SAFETY_LEASE_DENIED`, `TAKEOVER`, `STALLED` e `WORLD_CHANGED`.

---

## 6. Sequenza implementativa proposta per la Sotto-Fase 1B

1. Implementare D0 e i relativi test geometrici puri.
2. Implementare D1 e D2, poi estendere i test del pathfinder e del navigatore.
3. Implementare D5 prima della discesa motorizzata, affinché lo sneak abbia già un unico proprietario.
4. Implementare D6 e i test cooperativi delle botole.
5. Implementare D3 e i test della FSM senza runtime grafico.
6. Implementare D4 e D7, quindi i tre canali di attivazione e i relativi test di arbitraggio.
7. Implementare D8 e completare I18N ordinata.
8. Eseguire l’intera suite D9 e soltanto dopo produrre il JAR.

Ogni passo deve lasciare compilabile il ramo. Se un contratto richiede una deviazione architetturale materiale, interrompere la 1B e riportare il piano in revisione 1A.

---

## 7. File previsti

### Nuovi file principali

- `src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/ClimbableGeometry.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/ClimbTraversal.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/ClimbTraversalAnalyzer.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/ControlledDescentPort.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/RouteSegment.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/ClimbAssistantController.java`
- `src/main/java/org/mcaccess/minecraftaccess/addon/accessmenu/AutoClimb.java`
- Test omologhi nei package `src/test/java/...`.

### File esistenti candidati a modifica

- `AutoWalkPathfinder.java`
- `RouteNavigator.java`
- `AutoWalkMotor.java`
- `MovementCoordinator.java`
- `AutoWalkController.java`
- `AutoWalkManager.java`
- `TraversalSafetyAnalyzer.java`
- `SafeDescentCandidate.java`, solo se necessario per l’adattamento al modello condiviso
- `SafetyMovementGuard.java`
- `ProximityFallDetector.java`
- `CentralFallSafetyManager.java`
- `FallDetector.java`
- `DoorInteractionManager.java`
- `MouseSimulation.java`
- `Config.java`
- `MainClass.java`
- `CoreAddon.java`
- `it_it.json`
- `en_us.json`

### Documentazione post-implementazione, non autorizzata ora

- Manuale tasti e feature utente per `Alt+S`, interazione contestuale e Access Menu.
- Report di sessione e aggiornamento RRU.
- Diario modifiche e AVF soltanto nelle fasi previste dalla governance.

---

## 8. Matrice di simulazione preventiva a tre livelli

## Livello 1 — Happy path

1. Scala a pioli verticale con landing liberi: AutoWalk sale e scende, sbarca al centro e arriva al target.
2. Stesso scenario invocato con `Alt+S`: nessun POI richiesto, arrivo al primo landing e controllo restituito.
3. Interazione contestuale su ladder valida: il click viene consumato e parte l’assistenza.
4. Interazione contestuale non pertinente: il normale tasto destro resta invariato.
5. Scaffolding: salto posseduto in salita, sneak centralizzato in discesa.
6. Vine e tagged climbable: nessun accesso illegale a una proprietà `FACING` assente.
7. Botola lignea autorizzata: apertura tramite `DoorInteractionManager`, rinnovo sessione e richiusura solo dopo uscita sicura.

## Livello 2 — Concorrenza e alternative

1. Rampa sicura e ladder entrambe disponibili: vince il costo fisico complessivo, non una preferenza hardcoded.
2. Target entità si sposta durante la scalata: completamento landing e poi repath.
3. AutoWalk globale attivo e pressione `Alt+S`: nessuna sostituzione silenziosa della rotta.
4. Detector anticaduta attivo durante discesa: nessun falso freno, ma allarmi critici estranei restano prioritari.
5. Shift fisico durante scalata: takeover immediato; Shift sintetico del sistema non causa autocancellazione.
6. Config botole disabilitata: nessuna apertura forzata e nessun passaggio attraverso botola chiusa.
7. Cognitive Coordinator abilitato o disabilitato: comportamento motorio identico e fallback vocale coerente.

## Livello 3 — Corner cases

1. Ladder interrotta, landing mancante, lava, fuoco o soffitto basso: nessuna partenza.
2. Blocco rimosso o aggiunto durante il transito: stop sicuro con cleanup.
3. Botola di ferro, apertura negata o timeout: nessun attraversamento.
4. Pressione tenuta di `KEY_RBRACKET` o `Alt+S`: un solo avvio.
5. Entrambe le direzioni valide da metà colonna: stato ambiguo, nessun movimento automatico.
6. Correzione di posizione del server, lag o progresso inferiore alla soglia: un remount, poi arresto.
7. Cambio dimensione, morte, disconnessione o GUI: zero tasti e lease residue.
8. Colonna al limite di altezza del mondo o oltre il raggio: nessuna lettura fuori limite.
9. Percorso malformato o segmenti disallineati: rotta rifiutata senza crash.
10. Inventario in mano e Shift fisico più `KEY_RBRACKET`: posa/uso vanilla preservato, Climb Assistant bypassato.

---

## 9. Validazione preventiva sui 7 Assi di Qualità

1. **Validità**: usa API e pattern già presenti nel ramo, mantiene Fabric/NeoForge, Java 25, Kuma, Balm e il dominio cognitivo.
2. **Efficacia**: aggiunge l’arco topologico verticale mancante e una modalità tattica che riusa lo stesso esecutore.
3. **Coerenza**: non duplica botole, anticaduta, narrazione o motore; introduce porte strette tra domini.
4. **Completezza**: copre geometria, path, cinematica, comandi, configurazione, I18N, lifecycle, telemetria e collaudo.
5. **Precisione**: separa costo e metri, mantiene metadati del segmento e verifica AABB/VoxelShape lungo ingresso, colonna e uscita.
6. **Prestazioni e affidabilità**: vicini verticali atomici, cache per singola ricerca, scansioni limitate, zero sleep e log non per-tick.
7. **Assenza di regressioni**: preserva rotte orizzontali, ladder non standable, fallback botole orizzontale, tasto destro, takeover e quiete AutoWalk.

Esito 1A: i sette assi sono coperti dal disegno e da criteri verificabili. Non sono ancora convalidati da codice, test automatici o runtime.

---

## 10. Audit avversariale Protocollo 12

1. **Cancello 1 — Rifiuto patching euristico**: superato nel piano. Vengono aggiunti archi e semantica; il budget A* non viene aumentato.
2. **Cancello 2 — Hardware grounding**: superato nel piano. Takeover e bypass usano input raw e distinguono input sintetici posseduti.
3. **Cancello 3 — Hitbox e clearance continua**: superato nel piano. Ingresso, transito e uscita verificano corpo, testa e collision shapes reali.
4. **Cancello 4 — Named Contracts**: superato nel piano con D0..D9, precondizioni, postcondizioni, complessità e invarianti.
5. **Cancello 5 — Determinismo headless**: superato nel piano con analyzer puri, snapshot, clock, tick e porte iniettabili a 0 ms.
6. **Cancello 6 — Anti-bloat**: superato nel piano tramite modello condiviso e puntatori; vietate copie di logica tra AutoWalk, safety e botole.

---

## 11. Verifiche previste dopo l’approvazione, non eseguite in 1A

### Sotto-Fase 1B

1. Compilazione sorgenti e test:
   - `.\gradlew.bat --no-daemon --no-watch-fs compileJava compileTestJava`
2. Suite automatica completa:
   - `.\gradlew.bat --no-daemon --no-watch-fs test`
3. Verifica XML dei test, non solo exit code o console.
4. Verifica ordinamento e parità chiavi I18N IT/EN.
5. Build del JAR:
   - `.\gradlew.bat --no-daemon --no-watch-fs shadowJar`

Una suite verde dimostra coerenza automatizzata, non comportamento in-game certificato.

### Fase 2 — Collaudo manuale NVDA

1. Deploy provvisorio soltanto dopo build verde.
2. Prova salita e discesa su ladder, vine, scaffolding e tagged climbable realmente presenti.
3. Prova con botola aperta, lignea chiusa e ferro chiusa.
4. Prova dei tre canali: `Alt+S`, `KEY_RBRACKET`, Access Menu.
5. Prova takeover su W, A, S, D, salto e Shift.
6. Conferma vocale di assenza chatter, messaggi doppi, falsi arrivi e falsi “metri”.
7. Triangolazione tra suite, `latest.log` e riscontro diretto di Luca in-game.

La Fase 2 è superata solo con conferma esplicita di Luca. Fino ad allora piano e strategia restano attivi.

---

## 12. Criteri di accettazione finali

- [ ] A* trova almeno una rotta verticale sicura in salita e in discesa senza introdurre archi non fisici.
- [ ] Il segmento `CLIMB` arriva tipizzato al motore dopo start e repath.
- [ ] Costo fisico e distanza narrata sono separati.
- [ ] Mount, transito e dismount terminano su landing stabile.
- [ ] Climb Assistant funziona senza target POI.
- [ ] `KEY_RBRACKET` conserva il click storico quando il contesto non è accettato o quando Shift fisico richiede bypass.
- [ ] `Alt+S` non confligge nel test runtime e resta rimappabile.
- [ ] Lo sneak sintetico ha un solo writer e la lease anticaduta è sempre ripulita.
- [ ] Le botole sono gestite esclusivamente tramite `DoorInteractionManager`.
- [ ] Nessun input resta premuto dopo arrivo, takeover, errore, GUI, morte o disconnessione.
- [ ] Test deterministici a 0 ms verdi e nessun `Thread.sleep`.
- [ ] `it_it.json` ed `en_us.json` completi, paritari e alfabeticamente ordinati.
- [ ] Suite completa e `shadowJar` verdi con `--no-daemon --no-watch-fs`.
- [ ] Collaudo manuale NVDA di Luca positivo su happy path, alternative e corner cases.

---

## 13. Decisione richiesta a Luca

Il piano è pronto per peer review. La sola decisione che abilita la Sotto-Fase 1B è un comando esplicito di Luca come “procedi”, “applica” o “esegui” riferito all’implementazione di questo piano. In assenza di tale comando resta valido lo stop obbligatorio.
