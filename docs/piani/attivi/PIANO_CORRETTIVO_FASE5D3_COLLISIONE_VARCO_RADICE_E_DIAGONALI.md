# Piano Correttivo Formale — Revisione 5D.3: Collisione Fisica del Varco Radice e Sicurezza delle Diagonali
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA), Antigravity e Codex / ChatGPT
# Framework: ASTRALIS v2.6.3 — Sotto-Fase 1A Correttiva (PRAPI)
# Repository: minecraft-access (Fabric / NeoForge, Java 25, Minecraft 26.2)
# Incremento Versione Target (AVF): invariato, v26.2-1.19.0

> [!IMPORTANT]
> **Gating Semantico — Regola 0**: questo è un piano correttivo, non un'autorizzazione a modificare il codice. Antigravity può iniziare la Sotto-Fase 1B soltanto dopo un comando esplicito di Luca, ad esempio: *"procedi con la revisione 5D.3"*.

---

## 1. Identità, perimetro e linguaggio non ambiguo

Questo documento disciplina esclusivamente la **Revisione Correttiva 5D.3** della Fase 5D, emersa dopo l'audit della Revisione 5D.2.

- Non riapre né modifica le Sotto-Fasi **5A** o **5B**, già concluse.
- Non introduce alcuna funzione della futura **5E**: niente auto-apertura porte, nuove opzioni, tasti o messaggi vocali.
- Non modifica `AutoWalkMotor`, `RouteNavigator`, `MovementCoordinator`, configurazioni, localizzazioni o contratti I18N.
- Interviene soltanto in `AutoWalkPathfinder` e nella relativa suite di test headless.
- Conserva la correzione 5D.2 già valida: la visuale resta libera durante l'attesa davanti a una porta chiusa.

Le espressioni **Passaggio 1** e **Passaggio 2** indicano soltanto i due passaggi A* già esistenti: percorso rigoroso senza attraversare varchi chiusi, poi percorso di ripiego con penalità se non esiste un'alternativa. Non sono fasi 5A o 5B.

---

## 2. Origine delle criticità residue

### 2.1 Varco chiuso nella cella radice

La revisione 5D.2 conserva la posizione continua iniziale (`Vec3 startVec`) solo fino alla conversione in `BlockPos`. La funzione `isCrossingClosedDoorFrom(...)` classifica poi l'uscita dalla cella iniziale in base alla proprietà `DoorBlock.FACING`.

Quella proprietà descrive l'orientamento del pannello, ma non basta da sola a stabilire se la traiettoria concreta del giocatore, con hitbox di 0.6 × 1.8 blocchi, intersechi davvero quel pannello. In particolare, una traiettoria parallela al pannello può essere classificata come attraversamento pur non avendo collisione fisica.

Se la classificazione è falsa, allora il Passaggio 1 può scartare una via libera o il Passaggio 2 può attribuirle una penalità non dovuta. Il risultato è una preferenza di rotta artificiale e potenzialmente una falsa partenza verso una porta chiusa.

### 2.2 Diagonali di ripiego e varchi intermedi

Nelle mosse diagonali, il controllo di anti-corner-pinch verifica i due corridoi ortogonali intermedi con la politica del passaggio corrente. Nel Passaggio 2, tale politica ammette le porte chiuse perché sono storicamente attraversabili con arresto sicuro.

Se una porta chiusa è in uno dei due corridoi intermedi, allora una diagonale può aggirare la semantica del varco. Il motore riceve come prossimo nodo la diagonale di arrivo, non il varco laterale: non può quindi fermarsi e chiedere l'apertura della porta che il percorso ha tagliato di sbieco.

Questo viola la regola ASTRALIS sul corner pinching: una diagonale è valida solo quando entrambi i corridoi ortogonali che la compongono sono realmente liberi per la hitbox del giocatore.

---

## 3. Obiettivi di accettazione

1. Il percorso classifica il primo movimento da una cella contenente un varco chiuso tramite la collisione voxel effettiva, usando la posizione continua reale del giocatore.
2. Nessuna decisione di percorso usa più `DoorBlock.FACING` come scorciatoia per dedurre concetti come “stanza”, “interno” o “lato libero”. La proprietà può rimanere esclusivamente un dato da cui Minecraft genera la sua forma di collisione.
3. Nel Passaggio 1, un primo movimento che incontra fisicamente il pannello chiuso viene escluso; un primo movimento che non lo incontra resta disponibile.
4. Nel Passaggio 2, il medesimo movimento fisicamente bloccato conserva una sola `CLOSED_DOOR_PENALTY`; un movimento libero non riceve la penalità di uscita dalla cella radice.
5. Ogni diagonale, in entrambi i passaggi, viene scartata se uno dei due corridoi ortogonali intermedi contiene una collisione a quota piedi, testa o culmine del salto. Una porta, un cancelletto o una botola chiusi sono quindi sempre barriera laterale invalicabile per una diagonale.
6. Il percorso di ripiego conserva il comportamento storico per un varco chiuso realmente inevitabile: lo raggiunge ortogonalmente, il motore lo riconosce come prossimo nodo e si arresta con il messaggio storico già esistente.

---

## 4. Contratto correttivo A — Uscita fisica dalla cella radice

### 4.1 Conservazione del dato necessario e identificazione della radice

`findPath(...)` deve trasmettere `startVec` intatto a `computeAStar(...)`, alla generazione dei vicini del solo nodo radice e al calcolo del costo del primo movimento.

- **Identificazione rigorosa del nodo radice**: la radice è determinata esclusivamente dalla condizione strutturale `current.parent == null` (e mai tramite confronto di coordinate come `from.equals(startFeet)`, per evitare falsi positivi su percorsi ciclici o che ritornano alla cella di avvio). L'informazione `boolean isRootNode = (current.parent == null)` viene propagata esplicitamente a `getValidNeighbors` e a `checkAndAddMoves`.
- I nodi successivi restano discreti (`BlockPos`): non serve introdurre coordinate continue nell'intero A*.
- La valutazione continua è limitata agli al più otto candidati del nodo radice. L'impatto prestazionale è trascurabile e non altera euristica, budget o ordine delle altre mosse.

### 4.2 Nuovo criterio fisico e restituzione identità canonica

Sostituire la decisione basata su `isCrossingClosedDoorFrom(...)` con un helper a responsabilità unica, dal significato e firma equivalente a:

`public static @Nullable BlockPos getRootMoveIntersectedClosedDoor(Level level, Vec3 startVec, BlockPos rootFeet, NeighborMove move)`

L'implementazione deve rispettare tutte le condizioni seguenti:

1. Individua i varchi chiusi fisicamente presenti nella cella piedi della radice e, se necessario, nella cella testa immediatamente superiore.
2. Per ogni varco trovato usa la sua effettiva `VoxelShape` di collisione e la sua reale `BlockPos` in coordinate di mondo;
3. Costruisce la traiettoria continua dal `startVec` reale al centro orizzontale del nodo candidato (`move.targetPos.getBottomCenter()`). Per salite e discese, la quota segue coerentemente il movimento verso il nodo di arrivo.
4. Valuta la traiettoria con l'ingombro reale del giocatore: larghezza 0.6 e altezza 1.8 blocchi. È ammesso un campionamento lineare con intervallo massimo di 0.10 blocchi, purché ogni campione verifichi l'intersezione tra hitbox reale (`AABB`) e le `VoxelShape` reali dei varchi chiusi presenti alla radice. Non usare un singolo raggio centrale che potrebbe passare dove la hitbox non passa.
5. Se viene rilevata un'intersezione fisica con un varco chiuso, restituisce l'identificatore canonico (`getCanonicalDoorPos`) del varco intersecato; restituisce `null` per allontanamento, scorrimento parallelo o assenza di collisione.

### 4.3 Applicazione nei due passaggi A*

Se il nodo corrente è la radice (`isRootNode` / `current.parent == null`):

- Nel **Passaggio 1**, se `getRootMoveIntersectedClosedDoor(...) != null`, la mossa viene esclusa prima dell'inserimento fra i vicini.
- Nel **Passaggio 2**, se l'helper restituisce un blocco non nullo (`exitedDoor != null`), la mossa resta disponibile e riceve `CLOSED_DOOR_PENALTY = 30.0` soltanto se il varco sorgente canonico è distinto dall'eventuale varco di arrivo canonico (`!exitedDoor.equals(enteredDoor)`).
- Se l'helper restituisce `null`:
  - Il Passaggio 1 lascia disponibile il movimento iniziale;
  - Il Passaggio 2 non aggiunge alcuna penalità di uscita dalla sorgente;
  - Resta invariata l'eventuale penalità storica per un varco chiuso effettivamente entrato come nodo di arrivo (`enteredDoor != null`).
- Resta invariata l'eventuale penalità storica per un varco chiuso effettivamente entrato come nodo di arrivo.

### 4.4 Regole di non regressione

- Eliminare la logica che usa `facing.getOpposite()` per decidere l'attraversamento della radice. Non lasciare fallback direzionali in produzione.
- Applicare lo stesso criterio fisico a porte, cancelli e botole quando costituiscono il varco sorgente della radice; non inventare per nessuno di essi una nozione di lato interno.
- Non modificare le regole già consolidate per varchi chiusi incontrati più avanti nella rotta: l'arresto rimane responsabilità di `AutoWalkMotor`.
- Non aggiungere messaggi, suoni, chiavi di traduzione o opzioni di configurazione.

---

## 5. Contratto correttivo B — Inviolabilità fisica delle diagonali

### 5.1 Helper di clearance intermedia

Introdurre un helper leggibile, dal significato equivalente a:

`hasStrictDiagonalIntermediateClearance(Level level, BlockPos ortho1, BlockPos ortho2, boolean isStepUp)`.

Per ciascuno dei due corridoi ortogonali intermedi:

1. La cella a quota piedi deve essere passabile con politica rigorosa (`allowClosedDoors = false`).
2. La cella a quota testa deve essere passabile con politica rigorosa (`allowClosedDoors = false`).
3. Se la diagonale è una salita, anche il volume al culmine del salto deve essere libero da collisioni e pericoli.

La clearance intermedia è indipendente dal Passaggio 1 o 2: un varco chiuso in uno dei due corridoi intermedi invalida sempre la diagonale.

### 5.2 Inserimento nella generazione dei vicini

La generazione delle mosse diagonali deve chiamare l'helper prima di `checkAndAddMoves(...)`.

- Se l'helper restituisce `false`, la mossa diagonale non viene generata.
- Se restituisce `true`, restano attivi tutti i controlli già esistenti su destinazione, dislivello, acqua, pericoli e arco di salto.
- Un varco chiuso può quindi essere usato dal Passaggio 2 soltanto con una mossa ortogonale esplicita: il nodo successivo coinciderà con il varco e il motore potrà fermare Luca in modo sicuro.

---

## 6. Suite di test vincolante

Tutti i test devono essere headless, deterministici e costruiti con blocchi reali. Non sono validi test che controllano solo una variabile `Direction` senza verificare la forma di collisione.

1. **Radice, movimento verso pannello**: una porta chiusa nella cella radice e una traiettoria che ne interseca la `VoxelShape` devono essere escluse nel Passaggio 1 e penalizzate una sola volta nel Passaggio 2.
2. **Radice, movimento parallelo al pannello**: con la stessa porta e la stessa cella radice, una traiettoria parallela priva di intersezione deve restare valida e non ricevere la penalità di uscita. Questo è il test che impedisce di reintrodurre la scorciatoia basata solo sul `FACING`.
3. **Specularità fisica**: ripetere i due test precedenti con pannello orientato specularmente. Le aspettative devono derivare dal risultato fisico della forma di collisione, non da una convenzione “est/ovest = interno/esterno”.
4. **Normalizzazione a due blocchi**: una porta con metà inferiore e superiore conserva una sola penalità, anche quando la collisione è rilevata su una delle due metà.
5. **Fixture Tenuta, alternativa aperta**: la topologia con varco chiuso vicino e porta doppia aperta alternativa deve scegliere il varco aperto quando la traiettoria iniziale diretta interseca davvero il pannello chiuso.
6. **Varco inevitabile (Dominio Pathfinder)**: senza alternativa libera, il Passaggio 2 deve produrre una rotta (status `FOUND`) che raggiunge ortogonalmente il varco chiuso come nodo esplicito del percorso. La verifica cinematica dell'arresto e della visuale libera resta certificata dal test dedicato di `AutoWalkMotorTest`.
7. **Diagonale, barriera intermedia**: una porta chiusa, un cancelletto chiuso e una botola chiusa, ciascuno collocato a turno in uno dei due corridoi intermedi, devono invalidare la diagonale sia nel Passaggio 1 sia nel Passaggio 2.
8. **Diagonale, corridoi realmente liberi**: senza collisioni laterali, la diagonale piatta deve restare valida; con una salita, deve restare valida soltanto quando anche i due volumi di culmine sono liberi.

### Gate Esterno di Regressione Completa (Pipeline Gradle/CI)
- **Certificazione della Suite**: non un metodo unitario artificioso dentro `AutoWalkPathfinderTest`, bensì l'esecuzione dell'intera suite di progetto (`.\gradlew.bat --no-daemon --no-watch-fs test`) a valle degli 8 test mirati, a garanzia del 100% verde sui 269+ test totali.

---

## 7. Collaudo manuale e telemetria

Il test automatico non sostituisce il collaudo NVDA. Dopo build e deploy provvisorio, Luca esegue in-game i casi seguenti.

1. Davanti a una porta chiusa con una porta aperta alternativa raggiungibile: `Alt+W` sceglie la rotta aperta senza falsa partenza verso la chiusa.
2. Davanti a una porta chiusa inevitabile: il personaggio avanza solo fino alla distanza storica di sicurezza, pronuncia una sola richiesta di apertura e Luca può ruotare liberamente la visuale durante l'attesa.
3. Da entrambi gli orientamenti pratici di una porta: l'esito dipende dalla collisione reale e dalla rotta disponibile, non dall'etichetta di orientamento della porta.
4. In diagonale davanti a un varco chiuso laterale: il personaggio non taglia l'angolo; sceglie una via libera oppure raggiunge il varco con una mossa ortogonale e vi si ferma.
5. Sotto soffitto basso durante una salita diagonale: nessun tentativo di salto impossibile.

Antigravity registra nel rapporto telemetrico la rotta selezionata, l'eventuale nodo-varco, l'unicità dell'avviso vocale e l'assenza di orientamenti automatici ripetuti. Nessun dato di salvataggio viene modificato dal test.

---

## 8. Verifica ASTRALIS sui sette assi

1. **Validità**: usa le `VoxelShape` native e la hitbox Minecraft, senza modello geometrico inventato.
2. **Efficacia**: rimuove il falso costo iniziale e impedisce la diagonale che elude il punto di arresto.
3. **Coerenza**: mantiene i ruoli: pathfinder calcola collisioni e rotta, motore esegue l'arresto e il feedback storico.
4. **Completezza**: include porte, cancelli, botole, metà porta, salita, discesa, alternativa aperta e varco inevitabile.
5. **Precisione**: la decisione dipende dalla geometria continua della collisione e non da una sola coordinata o direzione discreta.
6. **Affidabilità e prestazioni**: il calcolo continuo è limitato alla radice, per al più otto mosse; nessuna scansione viene inserita nel ciclo di marcia a 20 tick.
7. **Assenza di regressioni e sobrietà**: non modifica FSM, voce, I18N, configurazione, tasti o funzioni 5E; la diagonale diventa soltanto più sicura e aderente alla hitbox.

---

## 9. Procedura obbligatoria di esecuzione dopo il via libera di Luca

1. Antigravity recepisce questo piano e dichiara in risposta quali metodi esistenti verranno sostituiti o ridotti, senza introdurre fallback basati su `DoorBlock.FACING`.
2. Implementa in modo chirurgico i contratti delle Sezioni 4 e 5 e tutti i test della Sezione 6.
3. Esegue `.\gradlew.bat --no-daemon --no-watch-fs test` e `.\gradlew.bat --no-daemon --no-watch-fs shadowJar`.
4. Riporta numero di test, fallimenti, errori, JAR prodotto e istanze di deploy; non dichiara la correzione conclusa prima del collaudo NVDA di Luca.
5. Esegue il deploy soltanto dopo build verde e aggiorna il rapporto telemetrico con esiti osservabili, non con sole intenzioni progettuali.
6. Codex svolge l'audit post-implementazione su codice, test, piano e rapporto. Solo la convergenza fra test automatici, telemetria e collaudo manuale consente di chiudere 5D.3.

---

## 10. Stato di avanzamento: Sotto-Fase 1B e Fase 2 Completate

- **Autorizzazione**: Ricevuta da Luca (*"ok, procedi con l'implementazione"*).
- **Implementazione**: Completata in `AutoWalkPathfinder.java` con contratti fisici continui `VoxelShape`/`AABB` (`getRootMoveIntersectedClosedDoor`) e diagonali rigorose (`hasStrictDiagonalIntermediateClearance`).
- **Test mirati**: 8/8 test vincolanti implementati in `AutoWalkPathfinderTest.java` (16 test totali, 0 fallimenti).
- **Gate non-regressione**: `.\gradlew.bat --no-daemon --no-watch-fs test` -> BUILD SUCCESSFUL (100% verde).
- **Deploy proattivo**: Artefatto `minecraft-access-1.12.0-SNAPSHOT.jar` (7.422.312 byte) compilato con `shadowJar` e deployato nelle istanze `Minecraft 26.2 Access 1.12.0` e `Minecraft 26.2 Access - Server Tenuta`.
- **Fase attuale**: Fase 2 (Telemetria Live e Collaudo Manuale In-Game di Luca).
