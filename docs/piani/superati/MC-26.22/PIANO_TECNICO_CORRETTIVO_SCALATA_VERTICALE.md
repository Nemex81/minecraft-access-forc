# Piano Tecnico Correttivo Formale MC-26.22 — FSM di scalata verticale

> Documento storico superato, conservato il 2026-09-12 senza dichiarazione di collaudo concluso. Riferimento operativo corrente: `docs/piani/attivi/PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md`. Stati e checklist sottostanti descrivono la precedente iterazione; restano consultabili le specifiche architetturali non sostituite dal nuovo piano.

- **ID revisione**: MC-26.22, iterazione correttiva successiva al primo collaudo in-game
- **Tipologia**: correzione strutturale della cinematica AutoWalk Verticale e Climb Assistant
- **Autore del piano**: GPT Codex / ChatGPT, copilota ausiliario e senior reviewer
- **Committente**: Luca
- **Revisori previsti**: Luca e Antigravity
- **Data e ora**: 2026-09-11 12:09 CEST
- **Stato operativo**: `[SOTTO-FASE 1A CORRETTIVA REDATTA — IN ATTESA DI CONVALIDA DI LUCA]`
- **Ramo locale osservato**: `feat/dual-fall-safety-subsystem`
- **Strategia master**: `docs/strategie/attive/STRATEGIA_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md`
- **Piano tecnico precedente**: `docs/piani/superati/MC-26.22/PIANO_TECNICO_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md`
- **Handover e RCA**: `docs/report/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md`
- **Registro RRU**: `docs/report/REGISTRO_REVISIONI.md`, revisione MC-26.22

---

## 0. Gating, autorità del documento e stato della revisione

- [x] Analizzati in sola lettura il report di handover locale, la strategia, il piano precedente, l'implementazione corrente e il log reale dell'istanza di collaudo.
- [x] Verificata la causa primaria indicata da Antigravity e individuate le condizioni concorrenti che renderebbero incompleto un fix limitato alla soglia `exitY`.
- [x] Redatti i contratti correttivi D0..D9 e le matrici preventive sui tre livelli e sui sette assi di qualità.
- [x] Convalida formale del presente piano da parte di Luca.
- [x] Sotto-Fase 1B: implementazione dei contratti approvati.
- [x] Compilazione, suite automatica e build del JAR.
- [x] Deploy controllato e nuovo collaudo in-game con NVDA.
- [ ] Chiusura o archiviazione della revisione soltanto dopo conferma esplicita di Luca.

### Stop obbligatorio

La redazione di questo file autorizza esclusivamente la Sotto-Fase 1A correttiva. Non autorizza modifiche ai sorgenti, ai test, alle configurazioni, alle traduzioni, al registro RRU, alla strategia, al piano precedente, alla storia Git, ai JAR o alle istanze PrismLauncher.

Il piano precedente non viene dichiarato chiuso: il collaudo reale ha invalidato la sua accettazione cinematica. Il presente documento ne conserva l'architettura generale e sostituisce, in caso di conflitto, le sole specifiche operative relative a produzione dei segmenti `CLIMB`, cursore verticale, FSM Mount/Transit/Dismount, target tattico e test cinematici.

Il working tree contiene modifiche e file non tracciati appartenenti alla lavorazione in corso. Lo stato Git segnala inoltre errori di permesso su alcuni oggetti loose; il ramo locale è stato osservato, ma non viene certificata la relazione completa con remote o commit. Nessuna di tali modifiche viene alterata in questa Sotto-Fase 1A.

---

## 1. Esito dell'analisi mirata

### 1.1 Evidenze confermate

1. Il log reale `latest.log` dell'istanza `Minecraft 26.2 Access 1.12.0` conferma:
   - rotta globale trovata in Pass 2 verso `BlockPos{x=-59, y=85, z=-37}`;
   - gestione della porta immediatamente precedente alla scala;
   - evento vocale `Salita scala avviata` alle 11:32:35;
   - cancellazione manuale soltanto alle 11:32:49;
   - due ulteriori avvii del Climb Assistant, alle 11:32:53 e 11:33:04, entrambi seguiti da cancellazione manuale;
   - nessun evento di timeout o stallo automatico nel periodo osservato.
2. I dati del salvataggio riportati nell'handover collocano il giocatore a `Y = 81.0` e il target a `Y = 85`, in una struttura contenente ladder, botola di acacia e mattoni di pietra.
3. Il successo del pathfinding e dei trigger, ripetuto sia in modalità globale sia tattica, esclude come causa primaria la selezione del target, il riconoscimento della scala o il key binding.
4. Il difetto appartiene al nucleo cinematico condiviso: è quindi corretto intervenire una sola volta nel percorso tipizzato, nel navigatore e nella FSM comune.
5. I 361 test verdi dichiarati nel report dimostrano la coerenza della suite esistente, ma non una scalata reale: i test correnti del motore verificano l'esistenza degli stati e il reset della sottofase, non una sequenza multi-tick completa con movimento, cursore e input.

### 1.2 RCA primaria precisata

La diagnosi di Antigravity è valida, con una precisazione temporale: il collasso non avviene nello stesso ramo `switch` che accetta il Mount, ma nel primo tick utile successivo eseguito in `TRANSIT`.

La sequenza è questa:

1. `processClimbMount` può accettare l'aggancio quando è vera una delle condizioni `distH < 0.35`, `player.onClimbable()` o `player.blockPosition().equals(climbPos)`.
2. L'uguaglianza del voxel o la sola vicinanza orizzontale non provano che la hitbox del giocatore sia realmente agganciata alla sottile collisione della ladder.
3. Al primo tick in `TRANSIT`, `exitY` viene ricavata dal nodo operativo corrente, che nella rotta osservata può essere ancora a quota base `Y = 81`.
4. La condizione `81.0 >= 81.0 - 0.15` è subito vera e porta a `DISMOUNT` senza progresso verticale.
5. `DISMOUNT` può quindi spegnere `keyUp` a terra oppure orientare `keyUp` verso il landing finale senza più produrre un comando di aggancio verticale.
6. Il watchdog non protegge questo caso perché opera in `TRANSIT`; una transizione prematura a `DISMOUNT` lo elude.

La soglia numerica non è la causa radice. La causa è l'uso del nodo locale come se fosse contemporaneamente soglia del piolo corrente e soglia terminale dell'intera colonna.

### 1.3 Difetti concorrenti rilevati nel ramo

1. `AutoWalkPathfinder` crea segmenti `CLIMB` globali con `climbData == null`. Il motore non dispone quindi, per AutoWalk globale, di `columnTopPos`, `columnBottomPos`, `landingPos`, `columnId`, tipo cinematico e faccia di supporto.
2. L'ingresso orizzontale in qualunque blocco arrampicabile viene marcato automaticamente `CLIMB`, anche quando è soltanto avvicinamento o attraversamento. Il tipo non distingue Mount, Transit e Dismount.
3. `ClimbAssistantController` può classificare come `CLIMB` il collegamento diretto tra il voxel del giocatore e l'inizio della colonna, anche se è orizzontale o più lungo di un arco atomico.
4. `ClimbAssistantController` duplica l'assemblaggio della rotta invece di usare un costruttore comune con il pathfinder.
5. `ClimbTraversalAnalyzer` memorizza oggi `originPos` come `entryPos`; se la ricerca trova la scala in una cella adiacente, l'entry registrata non coincide necessariamente con la prima cella arrampicabile reale.
6. L'uscita superiore viene selezionata tra candidati genericamente calpestabili, ma non è ancora contrattualizzata la raggiungibilità continua dalla faccia e dalla hitbox effettive della colonna.
7. `startTacticalRoute` installa `goalPos` nel `PathResult`, ma `RouteNavigator.installRoute` non assegna `targetObject`. Un repath successivo può quindi trovare un target nullo o stale.
8. Il contesto `ClimbTraversal` non viene trattenuto dalla FSM come identità della run: dopo l'avanzamento del cursore, la logica dipende dal segmento corrente, che può già essere il successivo segmento ordinario verso il landing.
9. `processClimbDismount` rilascia la lease di discesa all'ingresso della sottofase, prima che il landing sia stabile. Questa finestra è incoerente con il contratto di sicurezza originario.
10. La telemetria attuale prova l'avvio e la cancellazione, ma non registra transizioni di fase, indice, quota, stato di aggancio e comandi sintetici. Il collasso della FSM è quindi dimostrato dal flusso del codice e dai valori reali, non direttamente da una riga di log di fase.

---

## 2. Valutazione della proposta di Antigravity

### 2.1 Avanzamento verticale progressivo

**Decisione: accolta e rafforzata.**

L'avanzamento di un piolo alla volta è necessario, ma deve essere atomico, vincolato alla stessa run e protetto da `routeRevisionId`. Non basta chiamare `advanceWaypoint()` e poi osservare genericamente se il prossimo segmento è ancora `CLIMB`: occorre distinguere il ruolo del segmento e impedire doppi avanzamenti o passaggi a una colonna diversa.

### 2.2 Dismount vincolato a `columnTopPos` o `columnBottomPos`

**Decisione: accolta con doppio cancello.**

La quota reale della colonna è necessaria ma non sufficiente. `DISMOUNT` sarà ammesso soltanto quando:

1. è stato consumato l'ultimo segmento verticale della medesima run; e
2. la quota continua del giocatore ha attraversato la soglia terminale coerente con direzione e geometria; e
3. l'uscita e la botola risultano ancora attraversabili.

Questo impedisce sia il collasso al nodo di base sia un'uscita anticipata causata da oscillazioni della coordinata Y.

### 2.3 Impulso iniziale di salto

**Decisione: accolta come assistenza di Mount, non come correzione primaria.**

Il salto sarà un impulso breve, nominato, deterministico e posseduto dal motore, emesso soltanto per salita su elementi a parete quando il giocatore è a terra, allineato ma non ancora realmente agganciato. Non sarà usato in discesa, non resterà premuto indefinitamente e non sostituirà la verifica `onClimbable` più contatto geometrico/progresso reale.

Valore iniziale proposto per la Sotto-Fase 1B: `MOUNT_JUMP_PULSE_TICKS = 3`, senza sleep. L'efficacia del valore dovrà essere convalidata in-game; il contratto vincolante è la finitezza e la condizionalità dell'impulso.

### 2.4 `targetObject` esplicito nel Climb Assistant

**Decisione: accolta.**

Il landing deve essere installato atomicamente sia come goal della rotta sia come `targetObject`. L'installazione deve inoltre eliminare un eventuale target precedente. Durante Transit il repath resta sospeso; dopo landing stabile può usare il target tattico corretto.

---

## 3. Obiettivo verificabile della correzione

Fare in modo che una singola run di scalata venga eseguita come unità cinematica coerente:

1. ingresso e aggancio fisico verificato;
2. avanzamento verticale di tutti e soli i pioli pianificati;
3. uscita consentita soltanto al confine finale della colonna;
4. stabilizzazione sul landing validato;
5. rilascio degli input e delle lease soltanto a completamento, takeover o arresto sicuro.

La stessa run deve essere prodotta e consumata allo stesso modo da AutoWalk globale e Climb Assistant. Nessuna parte della correzione deve aumentare il budget A*, introdurre attese artificiali, duplicare botole o sicurezza anticaduta, oppure affidarsi a un caso speciale della Torre Belvedere.

---

## 4. Invarianti trasversali

1. **Una run, un'identità**: tutti i segmenti di una scalata condividono `columnId`, direzione e snapshot geometrico.
2. **Nessun `CLIMB` anonimo**: un segmento di scalata pubblicato al navigatore non può avere metadati nulli.
3. **Ruolo esplicito**: ingresso, transito verticale e uscita non sono inferiti dal solo delta Y durante il tick.
4. **Aggancio reale**: essere nello stesso `BlockPos` della ladder non equivale a essere agganciati.
5. **Cursore monotono**: ogni arco è consumato al massimo una volta e soltanto dopo la propria condizione fisica.
6. **Fine run geometrica**: la quota del waypoint intermedio non può terminare l'intera scalata.
7. **Landing stabile**: arrivo significa `onGround`, prossimità orizzontale, clearance corporea e geometria ancora valida.
8. **Input con proprietario**: avanti e salto appartengono al motore; lo sneak sintetico resta di competenza del sistema di sicurezza tramite lease.
9. **Repath differito**: nessuna sostituzione della rotta mentre il giocatore è impegnato nella colonna.
10. **Fail closed**: mismatch di segmenti, mutazione del mondo, botola bloccata o perdita di identità fermano il sistema con cleanup idempotente.
11. **Compatibilità orizzontale**: rotte senza run di scalata conservano comportamento, progressione e semantica precedenti.
12. **Quiete NVDA**: nessuna narrazione per tick o per piolo; la diagnostica tecnica resta non vocale e limitata alle transizioni.

### Transizioni di stato ammesse

1. `WALKING -> CLIMBING_UP/DOWN + MOUNT`: soltanto su una run D0 già validata.
2. `MOUNT -> TRANSIT`: soltanto dopo aggancio o progresso fisico verificato.
3. `MOUNT/TRANSIT -> MOUNT`: un solo retry causato dalla prima finestra watchdog senza progresso.
4. `TRANSIT -> TRANSIT`: a ogni piolo intermedio della medesima run, senza rilascio di `keyUp`.
5. `TRANSIT -> DISMOUNT`: soltanto dopo il doppio cancello fine-run più quota terminale e la verifica dell'uscita.
6. `DISMOUNT -> WALKING` oppure `ARRIVED`: soltanto sul landing stabile.
7. Qualunque sottofase può passare a `CANCELLED` o arresto sicuro per takeover, seconda finestra watchdog, mutazione o lifecycle; nessuna altra scorciatoia è ammessa.

---

## 5. Named Contracts correttivi D0..D9

## Contratto D0 — Modello formale della Climb Run

### Modifiche previste

1. Estendere `RouteSegment` con un ruolo di scalata esplicito, ad esempio `ClimbLeg`:
   - `MOUNT` per l'arco di ingresso validato nella colonna;
   - `TRANSIT` per un arco verticale atomico;
   - `DISMOUNT` per uno o più archi atomici di uscita verso il landing.
2. Conservare `SegmentType.CLIMB` come categoria compatibile, ma imporre:
   - se `type == CLIMB`, allora `climbLeg` e `climbData` sono non nulli;
   - se `type != CLIMB`, allora `climbLeg` e `climbData` sono nulli.
3. Rendere `ClimbTraversal.entryPos` la cella di confine realmente usata per entrare nella run: normalmente `columnBottomPos` in salita e `columnTopPos` in discesa, mai la posizione originaria della query. Un ingresso a metà colonna è ammesso soltanto se rappresentato da limiti di run espliciti e validati.
4. Aggiungere alla vista di run, direttamente o tramite un piccolo record immutabile dedicato, gli indici iniziale/finale, la prima e ultima gamba `TRANSIT` e l'eventuale sequenza `DISMOUNT`.
5. Tutti i segmenti della run devono condividere lo stesso `ClimbTraversal` o una copia immutabile semanticamente identica.

### Validazioni strutturali

- `MOUNT` termina sull'entry della colonna ed è composto soltanto da archi adiacenti e con clearance valida.
- `TRANSIT` conserva X/Z, ha `abs(deltaY) == 1`, segue la direzione dichiarata e non cambia `columnId`.
- `DISMOUNT` è una sequenza atomica continua che termina esattamente sul `landingPos` validato.
- Una run contiene almeno una gamba `TRANSIT`.
- Non sono ammessi buchi, inversioni di direzione, diagonali verticali o salto tra colonne.
- Per N nodi continuano a esistere N meno 1 segmenti.

### Geometria del landing

`ClimbTraversalAnalyzer` deve ordinare e validare i landing in relazione alla faccia reale della ladder o delle vine, alla continuità dell'uscita, alla botola e al prisma corporeo. Un blocco genericamente `isStandable` ma non raggiungibile dalla sommità non è un landing valido.

## Contratto D1 — Produzione AutoWalk di segmenti completi

1. `AutoWalkPathfinder` deve allegare `ClimbTraversal` non nullo alle mosse verticali già durante l'espansione, usando una cache locale alla singola ricerca indicizzata almeno da colonna, direzione e politica botole.
2. La regola corrente “entrare orizzontalmente in un climbable significa sempre `CLIMB`” deve essere rimossa.
3. Un attraversamento orizzontale senza successivo transito verticale resta `WALK`.
4. Un ingresso diventa `MOUNT` soltanto quando appartiene a una transizione verticale validata.
5. L'uscita dalla colonna può essere pubblicata soltanto verso il landing scelto e validato dalla stessa transizione; gli archi ordinari non possono creare un dismount implicito verso aria o supporti incompatibili.
6. `PathNode`, `NeighborMove` e ricostruzione devono conservare ruolo e metadati dalla ricerca fino a `PathResult`; è vietato ricostruire la semantica soltanto interrogando il mondo dopo la pubblicazione della rotta.
7. Se il modello unitario richiede di distinguere due stati con lo stesso `BlockPos` ma run o direzioni diverse, la chiave di visita deve includere il contesto di scalata. È vietato lasciare che `gScore` o `closedSet` fondano UP e DOWN nello stesso stato logico.
8. La normalizzazione finale deve rifiutare una rotta con `CLIMB` nullo o malformato prima di consegnarla al motore.

### Vincoli di perimetro

- Nessuna modifica a `MAX_EXPLORED_NODES`.
- Nessuna correzione tramite peso o timeout artificiale.
- Il costo fisico e l'euristica non vengono ritoccati per mascherare il difetto cinematico.
- `NO_PATH` e `SEARCH_BUDGET_EXHAUSTED` restano distinti.

## Contratto D2 — Assemblaggio condiviso del Climb Assistant

1. Introdurre un costruttore condiviso e testabile, ad esempio `ClimbRouteAssembler`, usato dal Climb Assistant e dalla normalizzazione dei segmenti AutoWalk.
2. `ClimbAssistantController` conserva soltanto rilevamento, risoluzione della direzione, policy di avvio e narrazione.
3. Il collegamento dal giocatore all'entry:
   - se è già sulla colonna, può iniziare dal primo `TRANSIT` ma la FSM esegue comunque il Mount fisico;
   - se è adiacente, viene prodotto un `MOUNT` atomico;
   - se è più lontano, viene costruito un breve approccio con normali archi `WALK` validati oppure l'avvio viene rifiutato in sicurezza;
   - non è mai prodotto un singolo arco lungo o diagonale marcato `CLIMB`.
4. La colonna viene emessa come sequenza ordinata di `TRANSIT` unitari.
5. L'uscita viene emessa come sequenza `DISMOUNT` fino al landing, senza aggiungere un generico `WALK` che faccia perdere prematuramente il contesto della run.
6. Le rotte globali e tattiche devono superare lo stesso validatore D0.
7. La direzione non viene scelta dal pitch se entrambe le direzioni sono geometricamente valide: in quel caso resta valido il fail closed per ambiguità previsto dal piano precedente.

## Contratto D3 — Installazione atomica, cursore e target

1. `RouteNavigator.installRoute` deve ricevere e installare atomicamente:
   - nodi;
   - segmenti;
   - goal;
   - `targetObject`;
   - revisione della rotta.
2. `startTacticalRoute` passa `landingPos` sia come goal sia come target e cancella ogni target precedente.
3. Il navigatore espone una vista immutabile della run corrente e un'operazione di avanzamento protetta da indice atteso e `routeRevisionId`.
4. L'avanzamento di un piolo:
   - verifica che il segmento corrente sia il `TRANSIT` atteso;
   - incrementa il cursore una sola volta;
   - invoca `onStepNode()` una sola volta;
   - restituisce il ruolo successivo senza resettare la FSM.
5. Il motore trattiene un `activeClimbTraversal` e l'identità della run fino a landing stabile o abort. Il passaggio del cursore su un segmento diverso non cancella tale contesto.
6. Repath, ObjectTracker e target mobili sono sospesi durante `MOUNT`, `TRANSIT` e `DISMOUNT`; riprendono soltanto dopo il landing.
7. Un mismatch di revisione o un target tattico nullo produce stop sicuro, non fallback a un target precedente.

## Contratto D4 — Mount fisico verificato e impulso controllato

### Regole comuni

1. Il target di Mount è l'entry reale della run, non il nodo verticale successivo.
2. La sola uguaglianza `player.blockPosition().equals(climbPos)` non può promuovere a `TRANSIT`.
3. Il Mount mantiene sprint disattivato, sterzata verso la faccia di supporto e centraggio coerente con il tipo cinematico.

### Ladder, vine e climbable a parete

1. `keyUp` resta acquisito verso il supporto.
2. L'aggancio richiede una combinazione verificabile di:
   - `player.onClimbable()`;
   - contatto/intersezione del bounding box con il volume o la faccia della colonna;
   - identità della colonna coerente con la run.
3. Se il giocatore è a terra, centrato e non ancora agganciato, il motore emette un solo impulso iniziale di `keyJump` per un massimo di 3 tick.
4. Il salto viene rilasciato appena è osservato l'aggancio o il progresso verticale; resta comunque soggetto al limite massimo.

### Scaffolding

Il Mount verifica l'inclusione corporea nella colonna e poi trasferisce il controllo alla policy di salita o alla lease di discesa già previste. Non usa l'impulso specifico delle ladder.

### Condizione terminale

`TRANSIT` inizia soltanto dopo aggancio fisico o progresso verticale coerente. Se ciò non avviene entro la prima finestra watchdog, è permesso un solo riallineamento con nuovo Mount; alla seconda finestra avviene arresto sicuro.

## Contratto D5 — Transit progressivo e monotono

1. Le tolleranze di quota diventano costanti nominate. I valori iniziali possono conservare `0.15 m` in salita e `0.25 m` in discesa; una loro variazione richiede evidenza separata e non costituisce il fix del difetto.
2. Per la salita, un nodo `TRANSIT` è consumato quando la quota continua supera la sua soglia in direzione positiva.
3. Per la discesa, il criterio è speculare in direzione negativa.
4. Dopo il consumo:
   - se la gamba successiva è `TRANSIT` della stessa run, il motore resta in `TRANSIT`, mantiene gli input e aggiorna il target;
   - se sono state attraversate più soglie tra due snapshot, può consumare in un ciclo limitato tutti e soli i nodi già fisicamente superati;
   - se il ruolo successivo è `DISMOUNT`, applica il doppio cancello del Contratto D6;
   - se il segmento successivo è incompatibile, arresta la sessione.
5. Il watchdog misura il delta Y reale rispetto all'ultimo progresso osservato, non il cambio di fase o l'incremento dell'indice.
6. La prima finestra di 15 tick senza almeno 0.05 m consente un solo remount; la seconda finestra consecutiva termina con cleanup e notifica di stallo.
7. `keyUp` non viene spento tra due pioli della stessa run. `keyJump` resta riservato al Mount wall-mounted o alla salita su scaffolding secondo il tipo.
8. La transizione a `DISMOUNT` è vietata mentre il cursore indica il `MOUNT` o un `TRANSIT` intermedio, anche se la coordinata Y coincide con quel nodo.

## Contratto D6 — Dismount e landing stabili

### Cancello di ingresso

`DISMOUNT` può iniziare soltanto se sono vere tutte le condizioni seguenti:

1. ultimo `TRANSIT` della run consumato;
2. in salita, quota coerente con `columnTopPos`; in discesa, quota coerente con `columnBottomPos`;
3. `columnId` e `routeRevisionId` ancora validi;
4. botola e corridoio d'uscita attraversabili dal prisma del giocatore;
5. sequenza `DISMOUNT` presente e coerente con `landingPos`, oppure landing coincidente esplicitamente con il termine del Transit.

### Esecuzione

1. Il motore conserva il contesto della run anche quando il segmento corrente diventa `DISMOUNT`.
2. L'input verticale specifico viene rilasciato soltanto quando non è più necessario; il comando avanti verso il landing resta attivo finché il passo d'uscita non è completato.
3. Ogni gamba atomica di uscita viene consumata una sola volta.
4. In discesa, la lease resta acquisita e rinnovata per tutta la sottofase. Viene rilasciata dopo landing stabile o nel cleanup di abort, mai all'ingresso di `DISMOUNT`.
5. Il landing è completo soltanto se:
   - `player.onGround()` è vero;
   - distanza orizzontale dal centro non superiore a 0.35 m;
   - piedi, testa e AABB sono liberi;
   - il supporto è ancora calpestabile e non pericoloso;
   - la botola non interseca il corpo.
6. Solo dopo tali verifiche vengono emessi `onClimbLanding`, completamento/ritorno a `WALKING`, rilascio degli input e riabilitazione del repath.
7. Il completamento del Climb Assistant coincide con il landing stabile, non con il raggiungimento della quota superiore o inferiore.

## Contratto D7 — Integrazione con input, botole, safety e lifecycle

1. `AutoWalkMotor` resta l'unico motore e l'unico proprietario di `keyUp` e dell'eventuale `keyJump` di Mount.
2. La logica verticale va estratta in un componente focalizzato e testabile, ad esempio `ClimbKinematics`, senza registrare un secondo tick loop e senza accesso globale autonomo agli input.
3. `SafetyMovementGuard` resta l'unico writer dello sneak sintetico; `ControlledDescentPort` conserva acquire, renew e release per `columnId`.
4. `DoorInteractionManager` resta l'unico proprietario della sessione botola. La FSM può richiedere e rinnovare il passaggio, ma non replica apertura, chiusura o cooldown.
5. Prima del confine superiore la geometria della botola viene rivalutata. Una botola lignea chiusa ma autorizzata mantiene la run in attesa controllata; ferro, rifiuto o timeout causano stop sicuro.
6. La botola può richiudersi soltanto quando il corpo è interamente fuori dal varco e il landing è stabile, secondo la sessione esistente.
7. Il takeover raw W/A/S/D/Salto/Shift conserva precedenza immediata dopo il latch del gesto di attivazione.
8. Cleanup per takeover, stallo, mutazione, morte, dimensione, disconnessione o GUI:
   - rilascia soltanto gli input sintetici posseduti;
   - rilascia la lease se presente;
   - chiude il contesto della run in modo idempotente;
   - non altera un input fisico ancora premuto.
9. L'estrazione della cinematica deve ridurre il bloat di `AutoWalkMotor`, non creare logiche duplicate tra motore, assistente e safety.

## Contratto D8 — Test deterministici e telemetria diagnostica

### Seam cinematico obbligatorio

Introdurre una funzione o classe package-private che riceva uno snapshot immutabile e restituisca una decisione immutabile. Lo snapshot include almeno fase, run, indice, quota Y, posizione/AABB, `onGround`, `onClimbable`, contatto con colonna, stato botola, progressione watchdog e lease. La decisione include fase successiva, comandi avanti/salto, yaw desiderato, numero di waypoint da consumare, azione lease e outcome.

Il seam non usa sleep, clock reale, client grafico o key mapping globali.

### Test minimi obbligatori

1. Riproduttore Belvedere: giocatore a `Y = 81`, primo arco alla stessa quota, colonna fino a `Y = 85`; il primo tick utile non può entrare in `DISMOUNT`.
2. Mount wall-mounted non agganciato: stesso voxel e distanza inferiore a 0.35 non bastano; output `keyUp = true`, impulso salto finito, fase ancora `MOUNT`.
3. Aggancio osservato: passaggio unico a `TRANSIT` e rilascio del pulse.
4. Salita su almeno quattro nodi: indice 1, 2, 3, 4 consumato una sola volta; nessun dismount intermedio.
5. Discesa speculare con lease rinnovata fino al landing.
6. Salto di più soglie in uno snapshot: catch-up limitato senza doppio callback e senza oltrepassare la run.
7. Fine colonna: doppio cancello segmento più quota; un solo criterio vero non basta.
8. Dismount con botola aperta, lignea chiusa autorizzata, ferro o blocco non attraversabile.
9. Landing non ancora `onGround`: avanti mantenuto e run attiva.
10. Landing stabile: callback e cleanup esattamente una volta.
11. Watchdog 15 più 15 tick a 0 ms; il collasso in Dismount non può più eluderlo.
12. Mutazione di rung, landing o `routeRevisionId`: fail closed e cleanup.
13. RouteNavigator rifiuta `CLIMB` con ruolo o metadati nulli, run spezzata, direzione inversa e mismatch N/N meno 1.
14. AutoWalkPathfinder produce una run completa sia in salita sia in discesa e non classifica come scalata un attraversamento orizzontale di una ladder.
15. Climb Assistant produce approccio `WALK`, run tipizzata e target landing esplicito; nessun arco lungo fittizio.
16. Repath di target mobile differito fino al landing.
17. Takeover manuale durante ogni sottofase e cleanup degli input sintetici.
18. Rotte interamente orizzontali, porte, step, drop, nuoto e scala a L invariati.

### Telemetria tecnica

Loggare a livello debug soltanto su ingresso run, cambio fase, avanzamento indice, retry watchdog, abort e landing:

- `routeRevisionId`;
- `columnId` e direzione;
- ruolo e indice corrente;
- fase precedente e successiva;
- quota corrente e soglia;
- `onGround`, `onClimbable` e contatto colonna;
- stato sintetico di avanti, salto e lease;
- motivo terminale.

Nessun log per ogni tick in modalità normale e nessuna nuova vocalizzazione per piolo.

## Contratto D9 — Validazione, deploy controllato e chiusura

### Verifica automatica successiva all'autorizzazione

1. Compilazione Java e test con flag Windows anti-lock.
2. Suite completa forzata con `--rerun-tasks test`, per evitare che risultati da cache vengano scambiati per nuova evidenza.
3. Ispezione dei report XML per conteggio, errori, failure e skipped.
4. Build `shadowJar` soltanto dopo suite verde.
5. La suite dovrà contenere più dei 361 test baseline; il numero esatto dipenderà dai casi implementati e non viene pre-certificato dal piano.

### Deploy e prova in-game

1. Usare il JAR prodotto dal ramo operativo effettivamente installato nell'istanza live concordata, con verifica di nome, timestamp e hash.
2. Conservare una copia recuperabile del JAR precedente prima della sostituzione.
3. Ripetere sul Belvedere, nelle stesse condizioni:
   - AutoWalk globale verso il tetto;
   - Climb Assistant da `Alt+S`;
   - trigger contestuale;
   - takeover manuale con `S`.
4. Acquisire il log debug delle transizioni e la conferma NVDA di Luca.
5. Estendere il collaudo almeno a discesa, botola aperta/chiusa, scaffolding e una seconda ladder con orientamento diverso.
6. Una build verde non autorizza la chiusura. La revisione termina soltanto con movimento reale, landing sicuro e conferma esplicita di Luca.

### Rollback

Se il nuovo JAR peggiora movimento orizzontale, takeover, botole o sicurezza, ripristinare il JAR precedente verificato e mantenere MC-26.22 aperta. Non effettuare reset distruttivi del working tree.

---

## 6. Sequenza implementativa vincolante della Sotto-Fase 1B

1. Implementare D0 e i test degli invarianti di modello.
2. Correggere `ClimbTraversalAnalyzer` e introdurre l'assembler condiviso D1-D2.
3. Correggere i produttori AutoWalk e Climb Assistant; nessuna rotta malformata deve raggiungere il motore.
4. Rendere atomici target, run e cursore in `RouteNavigator` secondo D3.
5. Estrarre il seam cinematico e implementare Mount, Transit e Dismount D4-D6.
6. Integrare input, botole, lease e cleanup D7.
7. Aggiungere prima i riproduttori del difetto, poi la matrice D8.
8. Eseguire compilazione e suite completa; correggere soltanto difetti entro il perimetro approvato.
9. Fermarsi e presentare evidenze prima di build/deploy se emerge una deviazione architetturale materiale.
10. Eseguire build, deploy e collaudo D9 soltanto con il gating concordato da Luca.

Ogni passo deve lasciare il ramo compilabile. Il test che riproduce `Y = 81` deve fallire prima della correzione e passare dopo di essa, oppure deve essere accompagnato da un seam equivalente che dimostri in modo deterministico la regressione e la sua eliminazione.

---

## 7. File candidati

### Nuovi file proposti

- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/ClimbRouteAssembler.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/ClimbKinematics.java`
- `src/test/java/org/mcaccess/minecraftaccess/features/autowalk/ClimbRouteAssemblerTest.java`
- `src/test/java/org/mcaccess/minecraftaccess/features/autowalk/ClimbKinematicsTest.java`
- `src/test/java/org/mcaccess/minecraftaccess/features/autowalk/ClimbAssistantControllerTest.java`

I nomi possono essere adattati in 1B senza cambiare responsabilità o contratti. `ClimbKinematics` non è un secondo motore e non registra tick autonomi.

### File esistenti candidati a modifica

- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/RouteSegment.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkPathfinder.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/RouteNavigator.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/ClimbAssistantController.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/ClimbTraversal.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/safety/traversal/ClimbTraversalAnalyzer.java`
- test omologhi di pathfinder, navigator, motor, coordinator e analyzer.

### File non previsti salvo nuova evidenza e autorizzazione

- Configurazione utente e key binding.
- Localizzazioni `it_it.json` ed `en_us.json`, perché la correzione non richiede nuove stringhe vocali.
- Volumi audio.
- Budget A*.
- Registro RRU, strategia, piani precedenti e documentazione di chiusura.
- Sistemi generali di porte e anticaduta al di fuori delle strette porte di integrazione già esistenti.

---

## 8. Matrice preventiva di simulazione a tre livelli

## Livello 1 — Happy path

1. Belvedere, salita globale da `Y = 81` a `Y = 85`: Mount reale, quattro progressioni, botola attraversata, landing stabile.
2. Stessa geometria tramite `Alt+S` e interazione contestuale: stessa run e stessa FSM.
3. Ladder a parete nei quattro orientamenti: yaw corretto e impulso limitato.
4. Discesa ladder: ingresso controllato, lease attiva, avanzamento monotono, base stabile.
5. Scaffolding in salita e discesa: salto e sneak assegnati ai proprietari corretti.
6. Vine e climbable senza proprietà `FACING`: nessun cast illegale, run completata secondo geometria disponibile.

## Livello 2 — Concorrenza e alternative

1. Target entità si sposta durante Transit: nessun repath nella colonna; aggiornamento soltanto dopo landing.
2. AutoWalk globale attivo e comando tattico: cancellazione o policy già prevista, nessuna sovrascrittura silenziosa del target.
3. Botola lignea chiusa: sessione rinnovata senza perdere indice o fase.
4. Safety anticaduta attiva: discesa autorizzata non viene frenata, allarmi estranei restano prioritari.
5. Input fisico durante Mount, Transit e Dismount: takeover immediato e nessun tasto sintetico bloccato.
6. Basso frame rate o più nodi superati tra snapshot: catch-up limitato e callback univoci.
7. Rotta che attraversa orizzontalmente una cella ladder senza salire: resta cammino ordinario.

## Livello 3 — Corner case

1. Player nello stesso voxel della ladder ma non `onClimbable`: nessun falso Transit.
2. Primo nodo alla stessa quota del player: nessun falso Dismount.
3. Colonna di un solo blocco: Mount, unico Transit e Dismount ordinati.
4. Colonna spezzata o rung rimosso durante la scalata: stop sicuro.
5. Landing diventa ostruito o pericoloso: nessuna dichiarazione di arrivo.
6. Botola di ferro o sessione negata: nessun attraversamento.
7. Cambio di `routeRevisionId` durante la run: nessun avanzamento su rotta stale.
8. Perdita di `climbData`, ruolo nullo o direzione incoerente: rotta rifiutata prima del movimento.
9. Fine Transit raggiunta ma quota terminale non raggiunta, e caso inverso: nessun Dismount.
10. Dismount non a terra: input d'uscita mantenuto e lease non rilasciata.
11. Watchdog dopo un Mount fallito: un solo retry, poi stop con cleanup.
12. Morte, cambio dimensione, pausa o disconnessione in ogni fase: cleanup idempotente.

---

## 9. Validazione preventiva sui sette assi di qualità

1. **Validità**: la correzione discende dai valori reali `Y = 81/85`, dal log e dal flusso effettivo dei metodi; distingue il primo tick di Transit dal tick di Mount.
2. **Efficacia**: elimina il confronto tra quota di base e confine finale, impone avanzamento dei pioli e aggiunge l'aggancio fisico mancante.
3. **Coerenza**: conserva un solo motore, un solo gestore botole, un solo writer dello sneak e un unico modello geometrico.
4. **Completezza**: copre entrambe le modalità, salita/discesa, ingresso, transito, uscita, target, repath, input, watchdog e lifecycle.
5. **Precisione**: separa nodo, gamba, run, quota terminale e landing; nessuna condizione usa un concetto al posto di un altro.
6. **Prestazioni**: metadata calcolati durante la ricerca con cache locale; tick cinematico O(1), salvo catch-up limitato ai pochi nodi realmente superati; nessun polling o log per tick.
7. **Assenza di regressioni**: i segmenti non verticali restano invariati, le integrazioni esistenti sono usate tramite porte strette e la matrice include takeover, porte, safety e navigazione orizzontale.

Esito preventivo: i sette assi sono coperti dal disegno. Non sono ancora convalidati da implementazione, suite automatica, build o runtime.

---

## 10. Audit avversariale Protocollo 12

1. **Cancello 1 — Rifiuto patching euristico**: superato; nessun aumento di budget, peso o timeout sostituisce la semantica mancante.
2. **Cancello 2 — Hardware grounding**: superato; takeover raw e input sintetici posseduti restano distinti.
3. **Cancello 3 — Hitbox e clearance continua**: superato; Mount e landing richiedono AABB, contatto reale e corridoio libero.
4. **Cancello 4 — Named Contracts**: superato nel piano mediante D0..D9.
5. **Cancello 5 — Determinismo headless**: superato nel disegno tramite snapshot, decisione pura e tick discreti senza sleep.
6. **Cancello 6 — Anti-bloat**: superato nel perimetro correttivo tramite estrazione della cinematica e assembler condiviso; non si dichiara risolto l'eventuale debito storico di dimensione degli altri router.

---

## 11. Criteri di accettazione

- [ ] Nessun segmento `CLIMB` installato possiede ruolo o metadati nulli.
- [ ] Un ingresso orizzontale non viene confuso con un piolo verticale.
- [ ] A `Y = 81` il primo nodo alla stessa quota non causa `DISMOUNT`.
- [ ] Il Mount wall-mounted richiede aggancio reale e usa un pulse di salto finito.
- [ ] Tutti i nodi verticali vengono consumati una sola volta e il cursore resta sincronizzato.
- [ ] `DISMOUNT` richiede insieme fine run, quota terminale e uscita valida.
- [ ] La lease di discesa resta attiva fino al landing o all'abort.
- [ ] Il landing tattico è sia goal sia `targetObject`, senza target stale.
- [ ] Botola aperta, lignea chiusa autorizzata e ferro bloccata producono gli esiti previsti.
- [ ] Watchdog, takeover e lifecycle rilasciano ogni input sintetico posseduto.
- [ ] I riproduttori headless coprono l'intera sequenza Mount/Transit/Dismount.
- [ ] Suite completa forzata verde, senza errori, failure o skipped inattesi.
- [ ] Build verde con i flag anti-lock prescritti.
- [ ] AutoWalk globale sale realmente sulla ladder del Belvedere e raggiunge il tetto.
- [ ] Climb Assistant sale realmente sulla stessa ladder e restituisce il controllo sul landing.
- [ ] Discesa reale con NVDA completata senza caduta o falso freno.
- [ ] Nessuna regressione osservata su cammino piano, porte, scale a L, step, drop, nuoto e takeover.
- [ ] Conferma finale esplicita di Luca prima della chiusura di MC-26.22.

---

## 12. Decisione richiesta a Luca

Il piano correttivo è pronto per peer review. Una sua convalida approva il disegno; l'avvio della Sotto-Fase 1B richiede comunque un comando esplicito di Luca riferito all'implementazione, come `procedi`, `applica` o `esegui`.

Fino a tale comando restano vietati codice, test, build, deploy e modifiche documentali ulteriori.
