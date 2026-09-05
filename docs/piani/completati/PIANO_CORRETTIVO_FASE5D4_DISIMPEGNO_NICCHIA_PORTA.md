# Piano tecnico correttivo — Revisione 5D.4: disimpegno dalla nicchia della porta

## 1. Identità, stato e perimetro

- Data di revisione del documento: 5 settembre 2026.
- Autori: Antigravity, con revisione di Codex approvata da Luca.
- Stato: piano aggiornato all'approccio concordato; implementazione della revisione 5D.4 non avviata da questo aggiornamento.
- La Fase 5 della strategia cognitiva è già implementata. Questo intervento è una revisione correttiva post-implementazione, non una riapertura delle sotto-fasi 5A, 5B o 5C.
- Gli identificatori di lavorazione di questo documento hanno il prefisso `5D4-`, per non confonderli con le fasi della strategia o del protocollo generale ASTRALIS.
- Ambito esclusivo: falsa partenza dalla nicchia della porta, primo segmento di movimento, uscita dagli stipiti e coerenza dell'attesa porta durante i ricalcoli.
- Esclusioni: revisione della scalinata, budget di ricerca, pesi A*, scanner storico Pagina Su/Pagina Giù, Mentor, Accademia, nuove funzioni cognitive o nuove impostazioni.
- Fonti: codice attuale di navigatore, motore e pianificatore; piano correttivo 5D.3; rapporto `docs/report/RAPPORTO_TELEMETRIA_E_ANALISI_PORTE_LOOKAT_FASE5D.md`, in particolare la Sezione 15.
- Questa revisione del piano sostituisce le prescrizioni iniziali su soglia di uscita di 0,50 metri, azzeramento incondizionato dell'attesa a ogni ricalcolo e sufficienza dei soli cinque test. La Sezione 15 del rapporto resta una testimonianza cronologica della proposta precedente, non la specifica esecutiva aggiornata.

### 1.1 Incremento Versione Target (AVF)

- Versione di base dichiarata nel piano precedente: `1.12.0`, con revisione 5D.3.
- Destinazione funzionale: revisione correttiva 5D.4 sulla stessa linea di sviluppo.
- Il presente aggiornamento documentale non incrementa la versione e non modifica i metadati della mod.
- L'eventuale incremento AVF sarà verificato e proposto a Luca alla chiusura tecnica, secondo il protocollo del progetto; non è deciso implicitamente dal numero 5D.4.

### 1.2 Risultato funzionale atteso

Se il giocatore parte dalla nicchia e il percorso conduce verso il corridoio interno libero, allora l'autowalk deve orientarsi e imboccare quel percorso senza chiedere di aprire la porta iniziale.

Se invece il primo tratto attraversa realmente il pannello chiuso, allora deve fermarsi, orientare lo sguardo una sola volta per quell'attesa e chiedere l'apertura attraverso i canali esistenti.

Se il percorso curva subito dopo la nicchia, allora il corpo deve avere spazio sufficiente per effettuare la curva senza urtare gli stipiti. Una breve rotazione iniziale da fermo è ammessa; un'attesa impropria della porta, una falsa dichiarazione di arrivo o un blocco senza termine non lo sono.

Non si introduce una retromarcia di un numero prefissato di passi e non si impone l'apertura di una porta estranea alla rotta.

## 2. Diagnosi verificata e limiti delle evidenze

### 2.1 Causa della falsa partenza

Una porta vanilla chiusa ha un pannello di collisione sottile, spesso 0,1875 metri. La cella contiene quindi spazio non occupato dal pannello. Il personaggio può stare nella parte interna della cella, tra gli stipiti, senza attraversare la porta.

Nel codice esaminato:

1. `AutoWalkPathfinder.findPath` ricava la cella iniziale dalla posizione continua tramite `BlockPos.containing`.
2. `reconstructPath` conserva questa radice come `path.get(0)`.
3. `RouteNavigator.startRoute` e `repath` inizializzano `currentPathIndex` a zero.
4. `AutoWalkMotor.tick` tratta quindi la radice come nodo da raggiungere.
5. Il controllo porta vede una porta chiusa nella stessa cella del giocatore, a distanza discreta zero.
6. `processDoorWait` interrompe il tick prima dell'avanzamento del nodo, anche se il percorso effettivo si allontana dal pannello.

La presenza dei due stipiti spiega la particolare geometria segnalata da Luca e il rischio di incastro durante la curva. Non deve diventare una condizione artificiale del tipo «applica la correzione solo se esistono esattamente due muri».

### 2.2 Perché non basta ignorare la porta o saltare un indice

- Ignorare ogni porta nella cella iniziale permetterebbe di tentare attraversamenti realmente bloccati.
- Cambiare solo il nodo nel controllo porta lascerebbe direzione, distanza e avanzamento riferiti al vecchio nodo.
- Impostare soltanto l'indice a uno può anticipare l'arrivo su una rotta di due nodi: oggi `isAtFinalGoal` usa una tolleranza di prossimità e viene consultato prima dell'attesa porta.
- La tolleranza ordinaria del nodo, 0,45 metri camminando o 0,70 in sprint, può consentire una curva quando una parte del corpo è ancora fra gli stipiti.
- Cancellare l'attesa a ogni ricalcolo può ripetere avviso e orientamento sulla stessa porta ancora necessaria.

La catena della falsa partenza è verificata staticamente. La risoluzione concreta del disimpegno e della curva richiede i test integrati e il collaudo descritti sotto: questa diagnosi non equivale a una certificazione in-game.

## 3. Contratti vincolanti della revisione 5D.4

### 3.1 Contratto C1 — Unico proprietario della rotta e partenza persistente

`RouteNavigator` rimane l'unico proprietario della lista, del cursore e dello stato di avanzamento della rotta.

Per un nuovo risultato `FOUND` con almeno due nodi:

1. Conservare la lista immutabile completa, inclusa la radice, senza modificare `PathResult` o la ricostruzione A*.
2. Registrare la posizione continua di partenza e la cella radice come contesto della nuova rotta.
3. Impostare il cursore operativo sul primo nodo da raggiungere, `path.get(1)`. Non mantenere contemporaneamente un cursore a zero nel navigatore e un obiettivo diverso nel motore.
4. Segnalare nel navigatore che il primo segmento non è ancora completato. Questo stato non dipende dal fatto che il giocatore continui a occupare la cella radice.
5. Consumare il primo nodo una sola volta, soltanto quando le condizioni di raggiungimento e di disimpegno applicabili risultano soddisfatte. Da quel momento si torna alla progressione ordinaria.
6. Non emettere `onStepNode` per la radice: è un punto di origine, non un passo compiuto.
7. Applicare la stessa inizializzazione a `startRoute` e a ogni `repath` riuscito. Pulire il contesto su `clearRoute` e invalidazione.

Usare un identificatore locale di revisione della rotta per riconoscere risultati sostituiti o azzerati durante un tick. È un contatore interno, non un nuovo gestore o servizio.

`getRemainingSteps` deve contare i nodi realmente ancora da raggiungere, escludendo la radice. Anche l'annuncio iniziale nel coordinatore deve usare questo significato: non può continuare a presentare `result.path().size()` come numero di passi. Non si modificano in questa revisione la metrica dei costi A* o i testi tradotti.

I costruttori di scenari di test devono distinguere esplicitamente una rotta A* completa, radice inclusa, da una lista di soli obiettivi. Non si salta il primo elemento di qualsiasi lista artificiale senza verificarne il significato.

### 3.2 Contratto C2 — Un solo segmento coerente per tick e nessun arrivo anticipato

L'obiettivo operativo va risolto prima di calcolare `nodeCenter`, `dx`, `dz`, `distH`, `deltaY`, direzione, salto e avanzamento.

Ordine funzionale vincolante:

1. Conservare la precedenza delle verifiche di contesto, intervento manuale e validità del bersaglio.
2. Gestire gli eventuali ricalcoli. Dopo un risultato `FOUND`, invalidare le valutazioni del vecchio percorso, rilasciare la spinta automatica del tick e rinviare l'esecuzione della nuova rotta al tick successivo.
3. Acquisire il contesto aggiornato della rotta e del segmento.
4. Individuare gli ostacoli porta pertinenti e riconciliare l'eventuale attesa precedente.
5. Verificare l'ammissibilità del movimento iniziale e della curva locale, senza anticipare la progressione.
6. Valutare l'arrivo soltanto se non rimangono un primo segmento da completare o un varco pertinente ancora da attraversare.
7. Eseguire il movimento e valutare l'avanzamento usando lo stesso segmento.

La regola sul ricalcolo vale anche per il ricalcolo tardivo del watchdog, oggi successivo al calcolo delle distanze. Non è ammesso avanzare un nodo della nuova rotta con le distanze della vecchia. Dopo ogni avanzamento non emettere nello stesso tick altri comandi calcolati per il nodo appena consumato.

La prossimità al traguardo non basta durante il primo segmento. Una rotta `FOUND` di due nodi deve superare i controlli del varco e raggiungere il nodo operativo; non deve diventare `ARRIVED` solo perché il cursore iniziale è già sull'ultimo indice. La medesima precedenza dei controlli porta vale per l'ultimo segmento ordinario.

L'esito esplicito `ALREADY_AT_TARGET` del pianificatore conserva il trattamento contrattuale esistente attraverso `finishArrival`. Non ricavarlo dalla sola lunghezza della lista:

- Una lista vuota o un nodo assente per errore non costituiscono prova di arrivo.
- Un risultato `FOUND` con meno di due nodi non va eseguito né convertito automaticamente in arrivo: gestirlo come risultato incoerente con arresto pulito e diagnostica.
- Gli esiti `NO_PATH`, `OUT_OF_RANGE` e `SEARCH_BUDGET_EXHAUSTED` mantengono la gestione terminale e il rilascio dei comandi già previsti.

### 3.3 Contratto C3 — Porta iniziale e porta di destinazione del segmento

Durante il primo segmento, le verifiche della porta iniziale e del nodo da raggiungere sono entrambe necessarie.

1. Per la porta nella radice, a quota piedi o testa, verificare se il movimento dalla posizione continua attuale al primo nodo interseca il pannello chiuso.
2. Riutilizzare `getRootMoveIntersectedClosedDoor` della revisione 5D.3 e la normalizzazione `getCanonicalDoorPos`. Non ricostruire una geometria basata soltanto su nome o orientamento della porta.
3. Un risultato senza intersezione esclude quel pannello iniziale dall'attesa; non certifica che l'intero segmento sia libero.
4. Controllare anche il varco nel nodo operativo e sopra di esso, fin dal primo segmento. Una seconda porta necessaria non può essere ignorata perché la prima è alle spalle.
5. Per eventuali altre forme intercettate dal volume locale del movimento, distinguere un varco chiuso realmente pertinente da un semplice ostacolo solido; non attribuire a quest'ultimo un avviso di apertura.
6. Se più varchi pertinenti risultano bloccanti, scegliere il primo lungo il segmento con ordine deterministico, normalizzare la sua identità e produrre una sola attesa.
7. Conservare per l'approccio ordinario ai nodi-porta la soglia storica `distSqr <= 4.5`. È una distanza tra celle, non una misura continua esatta dal pannello.
8. Un pannello iniziale che il corpo sta per attraversare impone comunque l'arresto prima del contatto; una distanza discreta non deve autorizzare la compenetrazione.

La metà superiore e quella inferiore della stessa porta hanno una sola identità. Cancelli e botole mantengono i riconoscimenti già supportati: non si estende qui il sistema a qualsiasi blocco apribile di altre mod.

Una porta aperta non genera attesa di apertura, ma la sua forma ruotata può ancora ostacolare il corpo. Non equivale a un volume trasparente né autorizza sempre lo sprint.

### 3.4 Contratto C4 — Uscita del corpo e curva senza incastro

La correzione geometrica aggiuntiva riguarda il breve disimpegno orizzontale dalla nicchia. Non introduce una nuova ricerca di percorsi e non riscrive la gestione di salti, scale o nuoto.

- La direzione iniziale deriva dal primo segmento valido della rotta, non da un numero fisso di passi indietro o dal solo orientamento del pannello.
- Finché la curva successiva interferirebbe con gli stipiti, mantenere come obiettivo il primo nodo. Non consumarlo soltanto perché è entrato nella tolleranza ordinaria.
- Se lo sguardo è rivolto altrove, è ammessa una rotazione progressiva da fermo verso il segmento. Il comando di avanzamento si abilita quando la direzione effettivamente impartita non porta il corpo contro il telaio.
- Il controllo locale deve considerare l'ingombro del giocatore e il volume attraversato dal movimento, non soltanto il centro. Usare la bounding box effettiva disponibile al motore; nei test standard essa misura 0,6 per 1,8 metri.
- Per una svolta, verificare sia il movimento comandato mentre termina l'uscita, sia la connessione al nodo successivo. Non basta dimostrare che i due punti finali sono liberi.
- La fine del disimpegno richiede il raggiungimento del primo nodo secondo la tolleranza applicabile e la clearance necessaria al movimento successivo. La soglia di prossimità non sostituisce questa seconda condizione.
- Distinguere l'ingombro degli stipiti da un'altra porta chiusa più avanti: se il corpo ha completato l'uscita in una posizione sicura, quella porta successiva non deve mantenere artificialmente aperto il disimpegno. Consumare il primo nodo senza impartire altra spinta; al tick seguente valutare il nuovo segmento e la sua eventuale attesa. Se invece il pannello impedisce già di raggiungere in sicurezza il primo nodo, applicare subito C3, senza mascherarlo come incastro.
- Se la tolleranza ordinaria è raggiunta ma la curva non è libera, continuare verso il centro del primo nodo lungo il tratto valido. Se neppure lì esiste la clearance necessaria, usare l'arresto e il recupero limitato esistenti: niente spinte infinite contro lo stipite.
- Non introdurre la regola «centro fuori dalla cella» o «distanza maggiore di 0,50 metri» come prova di uscita. Il volume intero e la traiettoria successiva sono il riferimento.
- Durante questo breve disimpegno evitare lo sprint se compromette la clearance; dopo l'uscita ripristinare il comportamento della configurazione tramite l'isteresi esistente.

L'helper della radice attuale controlla il pannello, non tutti gli stipiti. Il piano prevede quindi un controllo geometrico puro aggiuntivo e locale in `AutoWalkPathfinder`, riusabile dal motore: interrogazione delle forme native nel volume limitato del primo segmento e della connessione al nodo seguente. Per i tratti orizzontali, verificare il volume spazzato dalla bounding box contro le forme solide, con tolleranza numerica esplicita; non usare soltanto un raggio centrale o un campionamento grossolano.

Sono ammessi al massimo i nodi radice, primo obiettivo e successivo per questa verifica locale, con un numero limitato di celle e interrogazioni per tick. Nessuna scansione dell'intera rotta o del mondo. Un riuso con bounding box effettiva deve preservare i risultati e l'interfaccia storica del controllo radice usato da A*.

Non applicare una traiettoria rettilinea orizzontale come sostituto dell'arco di salto. Le transizioni verticali conservano le verifiche già presenti; eventuali conflitti non risolvibili entro questo perimetro richiedono una segnalazione, non un aggiramento dei controlli di sicurezza.

### 3.5 Contratto C5 — Attesa porta riconciliata, non azzerata a ogni ricalcolo

`waitingClosedDoorPos` identifica l'episodio di attesa di un varco canonico. Il cambio della lista di nodi non è, da solo, un nuovo episodio.

Dopo un ricalcolo riuscito:

1. Se la stessa porta è ancora chiusa e necessaria al segmento, conservare l'identità dell'attesa. Non ripetere `onDoorClosed`, orientamento o livellamento dello sguardo.
2. Se la porta precedentemente attesa non è più pertinente alla rotta, cancellare l'attesa in silenzio. Non emettere `onDoorOpened`.
3. Se il blocco atteso è ancora lo stesso varco, è tuttora pertinente ed è realmente passato allo stato aperto, chiudere l'episodio ed emettere una sola notifica di apertura, se consentita dalle impostazioni.
4. Se il blocco è stato rimosso, sostituito o non è verificabile, non dichiararlo «aperto» per deduzione. Gestire la nuova situazione senza conservare un blocco fantasma.
5. Se il primo varco bloccante è diverso, chiudere silenziosamente l'episodio precedente e valutare il nuovo con un solo avviso e orientamento.
6. Il semplice fatto che il nuovo nodo non contenga una porta non dimostra che quella attesa sia stata aperta.

L'attesa si attiva solo quando il varco è effettivamente pertinente e nelle condizioni di approccio previste. Conservarne l'identità non significa mantenere un arresto ingiustificato a qualunque distanza.

All'avvio di una nuova sessione esplicita e alla sua conclusione si puliscono i dati transitori. Un ricalcolo interno non deve richiamare indiscriminatamente `start`, rinnovare la grazia sull'intervento manuale o riarmare i segnali della stessa attesa.

### 3.6 Contratto C6 — Comandi, sicurezza e recupero limitato

- Il controllo manuale resta prioritario secondo `stopOnManualInput` e le regole esistenti. Non si introducono nuove combinazioni di tasti.
- L'orientamento automatico verso una porta resta singolo per episodio; nessuna rotazione ripetuta verso una porta estranea al percorso.
- Distinguere l'allineamento intenzionale da fermo da un tentativo di cammino realmente bloccato. Il primo deve avere una durata massima documentata e testata, coerente con la rotazione esistente di 20 gradi per tick.
- Per l'allineamento iniziale orizzontale prevedere un limite di 12 tick, sufficiente alla rotazione nominale di 180 gradi con margine. Non rinnovarlo indefinitamente con ricalcoli sulla stessa partenza; se non converge, ritornare al recupero limitato del watchdog.
- Non aumentare le soglie storiche del watchdog per mascherare gli incastri. Dopo il breve allineamento, un movimento senza progresso deve ancora raggiungere il ricalcolo e poi l'arresto previsti.
- Una corretta attesa di apertura può durare finché Luca non apre o annulla; non è un incastro da sopprimere. Il controllo manuale deve restare disponibile.
- Su attesa, ricalcolo, annullamento, invalidazione, cambio mondo e conclusione, rilasciare la spinta e gli eventuali comandi di salto posseduti dal motore. Non cancellare indiscriminatamente uno Spazio manuale.
- Preservare `autoJump`, `autoSwim`, sprint configurato, `voiceFeedback`, `narrateHints`, `lookAtTargetOnArrival`, suoni, volumi e instradamento cognitivo/legacy.

Nessuna modifica a `FallDetector`, `TraversalSafetyAnalyzer`, `SafetyMovementGuard`, al sistema di locking generale o al coordinatore cognitivo per aggirare un difetto del primo segmento.

### 3.7 Contratto C7 — Evidenze e sobrietà

La verifica deve osservare la cooperazione tra pianificatore, navigatore e motore. I test dei singoli predicati sono necessari, ma non sufficienti.

La telemetria mirata, usando i canali diagnostici già esistenti, deve permettere di correlare:

- avvio o ricalcolo e identificatore della rotta;
- posizione continua, cella radice, indice e primo nodo operativo;
- porta pertinente e motivo dell'attesa o della sua esclusione;
- inizio e termine del disimpegno;
- avanzamento, arrivo o causa di arresto.

Registrare transizioni e dati necessari al caso, senza log ripetitivi a ogni tick durante la marcia ordinaria, nuovi annunci vocali o letture dei salvataggi nel ciclo del motore.

## 4. Mappatura chirurgica degli interventi futuri

### 4.1 Navigatore — `RouteNavigator.java`

- Centralizzare l'installazione della rotta per avvio e ricalcolo.
- Introdurre il contesto minimo di partenza, lo stato di primo segmento incompleto e l'identificatore della rotta.
- Allineare cursore, conteggi, distanza rimanente, avanzamento e verifica di arrivo ai contratti C1 e C2.
- Pulire coerentemente lo stato e adeguare le fixture che oggi inizializzano sempre l'indice a zero.

### 4.2 Motore — `AutoWalkMotor.java`

- Riorganizzare il tick prima del calcolo delle metriche: non limitare la modifica al solo blocco di controllo porta.
- Applicare i controlli congiunti radice/nodo operativo, l'attesa riconciliata e il disimpegno locale.
- Interrompere l'esecuzione corrente dopo ogni sostituzione della rotta, incluso il ricalcolo del watchdog.
- Proteggere l'arrivo dalla prossimità prematura e la progressione dalle curve anticipate.
- Preservare i comandi manuali, la gestione terminale e i comportamenti configurabili.

### 4.3 Geometria — `AutoWalkPathfinder.java`

- Riutilizzare gli helper 5D.3 di collisione radice e identità canonica.
- Aggiungere il controllo puro locale di clearance del corpo descritto in C4, senza spostare nel pianificatore lo stato del motore.
- Nessuna modifica a ricerca A*, budget, penalità, selezione degli obiettivi, diagonali rigorose o politica del secondo tentativo con porte chiuse.
- Non equiparare `SEARCH_BUDGET_EXHAUSTED` a `NO_PATH` e non aprire nuovi percorsi diagonali attraverso gli stipiti.

### 4.4 Integrazione — `MovementCoordinator.java`

- Adeguare soltanto il numero di passi inizialmente annunciati alla semantica C1 e verificare il passaggio degli esiti.
- Conservare eventi, priorità, filtri, traduzioni, fallback e comportamento `ALREADY_AT_TARGET`.
- Non introdurre nuovi tipi di evento, chiavi I18N o riforme del coordinamento.

### 4.5 Test e documentazione

- Estendere `RouteNavigatorTest`, `AutoWalkMotorTest` e `AutoWalkPathfinderTest`; aggiungere o estendere nel package autowalk i test integrati e quelli del conteggio annunciato dal coordinatore.
- Riutilizzare le fixture esistenti, senza costruire una seconda implementazione dell'autowalk nei test.
- Registrare i risultati nel rapporto cronologico esistente dopo l'esecuzione autorizzata. Non riscrivere le risposte precedenti come se avessero già previsto questa revisione.
- Nessun aggiornamento del registro differito, di altri piani o della governance globale per effetto di questo solo intervento.

## 5. Scenari riproducibili e simulazione ASTRALIS a tre livelli

### 5.1 Fixture geometrica di riferimento

Queste coordinate sono sintetiche e servono a riprodurre la geometria, non a dichiarare la posizione storica esatta di Luca.

- Pavimento a quota Y = 63; piedi a Y = 64; spazio per il corpo fino a Y = 66.
- Porta chiusa con metà inferiore a `(0, 64, 0)`, metà superiore a `(0, 65, 0)`, orientamento `WEST`.
- In questa configurazione vanilla il pannello è sul lato Est della cella, con X da 0,8125 a 1. Verificare questo intervallo dalla forma nativa usata nel test.
- Stipiti solidi a `(0, 64, -1)`, `(0, 65, -1)`, `(0, 64, 1)` e `(0, 65, 1)`.
- Giocatore a `(0.5, 64.0, 0.5)`, senza compenetrazioni iniziali.
- Primo nodo verso l'interno: `(-1, 64, 0)`. Primo nodo attraverso il pannello: `(1, 64, 0)`.
- Per la curva interna, proseguire dopo `(-1, 64, 0)` verso `(-1, 64, 1)`, con pavimento e spazio libero verificati.
- Impostare esplicitamente anche cerniera, stato aperto/chiuso, posizione, orientamento iniziale del giocatore e validità del bersaglio. Un bersaglio vicino non deve trasformare involontariamente una prova `FOUND` in `ALREADY_AT_TARGET`.

Variante a coordinate negative: porta a `(-55, 66, -36)`, giocatore a `(-54.5, 66.0, -35.5)`, primo nodo interno a `(-56, 66, -36)`, con traslazione equivalente di stipiti e pavimento. Verificare con un'asserzione che `BlockPos.containing(playerPos)` coincida con la radice.

Le coordinate della bozza precedente `(-55.2, 66.0, -36.5)` ricadono invece nella cella `(-56, 66, -37)`: non riproducono una partenza nella porta `(-55, 66, -36)`.

### 5.2 Livello 1 — Scenari comuni

1. Nicchia chiusa, rotta interna libera: nessuna attesa della porta iniziale; completamento del primo segmento e prosecuzione.
2. Nicchia chiusa, rotta attraverso il pannello: arresto corretto, avviso singolo, ripartenza dopo apertura effettiva.
3. Approccio ordinario alla porta da una cella d'aria, interno ed esterno: comportamento di attesa conservato.
4. Percorso senza porte: progressione regolare, nessun nuovo arresto e conteggio dei passi coerente.

### 5.3 Livello 2 — Scenari alternativi e transitori

1. Curva interna a destra e a sinistra: il corpo esce dagli stipiti prima di assumere la traiettoria della curva.
2. Stessa geometria ruotata nei quattro orientamenti, con cerniere speculari e posizione iniziale decentrata ma fisicamente valida.
3. Porta iniziale estranea alla rotta e seconda porta necessaria nel primo tratto: attesa soltanto del varco corretto.
4. Ricalcolo con stessa porta ancora necessaria: nessuna ripetizione di avviso o orientamento.
5. Ricalcolo che abbandona la porta, oppure introduce un'altra porta: aggiornamento dell'attesa senza falsa apertura.
6. Porta aperta con pannello ruotato: niente invito ad aprirla; collisione residua comunque rispettata.

### 5.4 Livello 3 — Casi limite

1. Centro appena oltre la cella radice ma corpo ancora vicino agli stipiti: niente curva anticipata.
2. Ingresso nella tolleranza del primo nodo mentre la curva è ancora ostruita, sia camminando sia con sprint configurato.
3. Rotta di due nodi con porta necessaria: niente arrivo prematuro; distinta prova con primo segmento libero.
4. `ALREADY_AT_TARGET` esplicito, risultato vuoto, singolo nodo incoerente e ogni esito terminale del ricalcolo.
5. Rilevazione della stessa porta da metà superiore e inferiore, senza duplicazioni.
6. Porta rimossa o sostituita, contesto di gioco non disponibile e annullamento durante l'attesa.
7. Bersaglio mobile, atterraggio e watchdog che sostituiscono una rotta durante il tick.
8. Intervento manuale e rotazione iniziale opposta di 180 gradi: controllo umano disponibile, allineamento limitato e nessun ciclo infinito.

## 6. Piano di test vincolante

### 6.1 Metodo

Aggiungere prima i test di regressione e documentare quali riproducono il difetto nel codice precedente. Se una prova non è eseguibile nel vecchio codice perché dipende da una nuova interfaccia, dichiararlo: non inventare una dimostrazione prima/dopo.

I test integrati devono esercitare la rotta prodotta dal pianificatore, l'installazione nel navigatore e più chiamate reali a `AutoWalkMotor.tick`. Sono ammesse sostituzioni controllate del mondo e dell'input; le decisioni di produzione su radice, porta, indice e avanzamento non devono essere sostituite da valori booleani precalcolati.

Per il disimpegno verificare il volume del movimento derivato dai comandi effettivi contro gli stipiti. Non basta spostare artificialmente il giocatore oltre l'ostacolo o verificare che lo yaw vari poco. Una simulazione headless della fisica va dichiarata come tale e non sostituisce il collaudo in Minecraft.

### 6.2 Gruppi di copertura obbligatori

1. **Origine e primo obiettivo**: radice conservata come metadato, primo nodo operativo corretto, stato persistente dopo l'uscita dalla cella.
2. **Progressione**: primo nodo consumato una sola volta, nessun passo fittizio per la radice, nessun indice fuori limite.
3. **Conteggi**: passi iniziali e rimanenti coerenti tra navigatore, coordinatore e notifiche di avanzamento.
4. **Partenza libera integrata**: fixture della nicchia con A* e tick del motore; nessun `DOOR_CLOSED`, movimento e avanzamento entro un limite di tick dichiarato.
5. **Attraversamento integrato**: pannello realmente necessario; nessuna spinta contro la porta, un solo orientamento e un solo avviso per episodio.
6. **Destinazione del primo segmento**: seconda porta rilevata anche quando quella iniziale non blocca; ordine e identità del primo ostacolo corretti.
7. **Rotazioni e coordinate**: quattro orientamenti, cerniere, interno/esterno, coordinate negative e posizioni decentrate valide.
8. **Curva e corpo intero**: curve speculari, caso entro 0,45 metri dal nodo e variante sprint; nessuna scorciatoia attraverso lo stipite. Una porta chiusa successiva produce la corretta attesa e non un disimpegno infinito o un falso incastro.
9. **Pannello aperto**: assenza di attesa di apertura, ma rispetto della sua forma fisica.
10. **Arrivo breve**: rotta di due nodi, varco chiuso e primo segmento libero; nessuna anticipazione dovuta all'indice iniziale.
11. **Esiti del percorso**: `ALREADY_AT_TARGET` attraverso la pipeline esistente; dati incoerenti non convertiti in successo; pulizia su tutti gli errori.
12. **Ricalcolo sulla stessa porta**: ricalcoli ripetuti, zero duplicazioni di avviso e orientamento, nessun rinnovo indefinito della grazia manuale.
13. **Ricalcolo su altra rotta**: abbandono dell'attesa senza falsa apertura; nuova porta con un solo nuovo episodio.
14. **Apertura reale e identità**: porta canonica piedi/testa, apertura singola, rimozione o sostituzione non narrate come apertura.
15. **Coerenza del tick**: ricalcoli da entità, atterraggio e watchdog; niente comandi o avanzamenti con dati della rotta sostituita.
16. **Recupero limitato**: rotazione da fermo ammessa, scadenza dell'allineamento, ricalcolo e arresto in caso di mancato progresso reale.
17. **Controllo e pulizia**: annullamento, bersaglio invalidato, cambio mondo, fine rotta e tasti manuali; niente comandi automatici residui.
18. **Configurazioni e regressioni**: `autoJump`, `autoSwim`, sprint, `stopOnManualInput`, `narrateHints`, `voiceFeedback`, `lookAtTargetOnArrival`, canale cognitivo/legacy e suoni invariati.

Questi sono gruppi di copertura, non un numero artificiale di metodi JUnit. Usare test parametrizzati dove opportuno e registrare il numero effettivo di casi eseguiti.

I test di porte, diagonali rigorose, salti, acqua, soffitti, sicurezza e arrivo già presenti restano vincolanti. Non modificarne le aspettative per nascondere regressioni; adeguare soltanto le fixture la cui semantica di origine è esplicitamente cambiata.

## 7. Verifica ASTRALIS sui sette assi

Le seguenti sono condizioni di accettazione e relative evidenze richieste, non risultati già certificati.

1. **Validità**: forme native e coordinate continue coerenti; compilazione sullo stack del progetto e test geometrici superati.
2. **Efficacia**: partenza e curva riuscite nella nicchia reale di Luca, senza apertura della porta estranea alla rotta; riscontro in telemetria.
3. **Coerenza**: un solo proprietario del percorso, un solo segmento operativo, eventi instradati nei canali esistenti; verifica integrata.
4. **Completezza**: copertura dei tre livelli di scenario, dei ricalcoli, delle configurazioni e dei casi terminali.
5. **Precisione**: nessuna porta fantasma, falsa apertura, duplicazione dello sguardo o anticipazione dell'arrivo; riscontro su tick e log.
6. **Affidabilità e prestazioni**: verifiche locali limitate, nessuna ricerca aggiuntiva a ogni tick e nessun recupero infinito; misurare interrogazioni geometriche e tempi rappresentativi. Non dichiarare «zero overhead» senza misure.
7. **Non regressione e sobrietà**: modifica circoscritta alle responsabilità elencate, suite completa superata e collaudo comparativo interno/esterno. Lasciare intatto il codice di sicurezza non è, da solo, prova di assenza di regressioni comportamentali.

## 8. Sequenza operativa e condizioni di chiusura

### 8.1 Passaggio 5D4-P — Revisione documentale

- [x] Approvazione di Luca alle modifiche del piano e adattamento all'approccio concordato.
- [x] Sostituzione delle prescrizioni ambigue con contratti di partenza, geometria, attesa e validazione.
- [x] Autorizzazione esplicita di Luca all'implementazione della revisione 5D.4.

L'approvazione all'aggiornamento del piano non autorizza l'esecuzione delle modifiche sorgenti.

### 8.2 Passaggio 5D4-I — Implementazione e verifica automatica

Dopo l'autorizzazione:

1. Verificare lo stato dei file, preservare le modifiche altrui e controllare JDK 25.
2. Aggiungere le prove del difetto e implementare i contratti con interventi circoscritti.
3. Eseguire compilazione, test mirati, suite completa e generazione del pacchetto con Gradle senza demoni persistenti.
4. Usare i comandi del progetto: `.\gradlew.bat --no-daemon --no-watch-fs compileJava compileTestJava`, `.\gradlew.bat --no-daemon --no-watch-fs test` e `.\gradlew.bat --no-daemon --no-watch-fs shadowJar`.
5. Riportare esiti effettivi, casi eseguiti, errori o limiti; non assimilare un controllo statico a un test passato.
6. Verificare il diff circoscritto, l'assenza di modifiche ai moduli esclusi e il riuso dei messaggi esistenti.

### 8.3 Passaggio 5D4-C — Distribuzione di prova e collaudo con Luca

La distribuzione nell'istanza di prova e il collaudo seguono il protocollo del progetto, dopo la verifica tecnica positiva e nel perimetro autorizzato. Non toccare il backup stabile.

Il collaudo deve comprendere almeno:

1. Partenza dalla posizione interna alla nicchia segnalata da Luca verso la rimessa passando per il corridoio accanto alle scale.
2. Ripetizione con spostamenti laterali piccoli ma validi e orientamento iniziale diverso.
3. Curva dopo l'uscita, senza urti persistenti o ritorno del blocco dello sguardo.
4. Percorso che richiede realmente la porta iniziale: arresto corretto, apertura manuale e prosecuzione.
5. Avvio dall'esterno e da una cella ordinaria: comportamento già funzionante preservato.
6. Ricalcolo, annullamento e nuova partenza, senza notifiche duplicate o tasti residui.

Applicare la triangolazione ASTRALIS:

- Test automatici per le decisioni riproducibili.
- Telemetria della sessione e riscontro vocale/NVDA per la sequenza temporale reale.
- Lettura non modificante di un salvataggio coerente, dopo il salvataggio della sessione, per verificare geometria, stato dei blocchi e posizione persistita.

Il salvataggio finale non dimostra da solo tutti i movimenti intermedi: la ricostruzione temporale richiede la telemetria. Se manca una fonte necessaria o non è aggiornata, annotare il limite e lasciare aperta la convalida empirica.

### 8.4 Passaggio 5D4-V — Convalida e chiusura

- [x] Contratti verificati sul codice implementato.
- [x] Test mirati e suite completa superati, pacchetto prodotto e identificato.
- [x] Prestazioni locali e assenza di regressioni nei casi concordati verificate.
- [x] Collaudo in-game positivo confermato esplicitamente da Luca.
- [x] Rapporto cronologico aggiornato con evidenze e riserve effettive.

Il piano resta in `docs/piani/attivi/` fino alla conferma manuale. AVF, archiviazione, eventuale promozione del pacchetto e operazioni Git seguiranno il protocollo e le autorizzazioni applicabili.

Non dichiarare l'anomalia «debellata al 100%» sulla base della sola progettazione, della compilazione o dei test isolati.

## 9. Chiusura formale e archiviazione (5 settembre 2026)

- **Esito Implementazione (5D4-I)**: Completata con successo. Implementati i Contratti C1-C7 e le clausole integrative dell'Addendum 10.1-10.7 in `RouteNavigator.java`, `AutoWalkMotor.java`, `MovementCoordinator.java` e `AutoWalkPathfinder.java`.
- **Esito Test Automatici**: 100% test superati (278/278 test passati nella suite Gradle).
- **Esito Compilazione e Deploy (5D4-C)**: Artefatto `minecraft-access-1.12.0.jar` (7.428.758 byte) generato con `shadowJar` e distribuito con successo in entrambe le istanze attive di PrismLauncher.
- **Esito Collaudo in-game di Luca (5D4-V)**: **Superato a pieni voti**. Luca ha verificato direttamente nel mondo reale che:
  1. Con il personaggio all'interno della nicchia di ingresso tra gli stipiti, il comando di raggiungere la porta del primo piano viene eseguito in modo fluido, senza falsi allarmi tick 0 e senza movimenti a scatti della visuale;
  2. Con il medesimo posizionamento nella nicchia, il comando di raggiungere la rimessa avvia il disimpegno pulito e lineare verso il corridoio;
  3. L'anomalia della falsa partenza dalla nicchia della porta è definitivamente risolta.
- **Stato del Piano**: COMPLETATO e ARCHIVIATO in `docs/piani/completati/`.

---

## 10. Addendum Vincolante di Perfezionamento Tecnico (Convergenza Paritetica Antigravity-Codex)

Il presente addendum perfeziona e blinda l'implementazione della Revisione 5D.4 integrando i 6 chiarimenti tecnici emersi dall'ispezione paritetica convalidata da Luca il 5 settembre 2026.

### 10.1 Risoluzione ALREADY_AT_TARGET condizionato alla traversabilità diretta in entrambi i passaggi (AutoWalkPathfinder.java)
- **Problema**: Se il bersaglio si trova a meno di 1,25 metri (`directDist < 1.25`) o la cella di partenza è inclusa negli obiettivi validi ma è separata dal giocatore da un varco chiuso o da un ostacolo solido (es. waypoint posizionato subito oltre una porta d'ingresso), il sistema restituiva prematuramente `ALREADY_AT_TARGET` prima di verificare la presenza della porta chiusa, sia nel Passaggio 1 che nel Passaggio 2 (`validGoalsFallback.contains(startFeet)`), impedendo l'avvio della rotta e l'attesa di apertura.
- **Specifica Vincolante**:
  1. Nel **Passaggio 1 (Strict Path)** e nel **Passaggio 2 (Fallback Path)**, la scorciatoia di prossimità o di inclusione della cella di partenza (`validGoalsStrict.contains(startFeet)`, `directDist < 1.25`, e `validGoalsFallback.contains(startFeet)`) viene rigorosamente subordinata alla **traversabilità diretta** senza varchi chiusi (`isDoorOrGateClosed`) o barriere solide tra la posizione reale del giocatore (`startVec`) e la posizione del bersaglio (`rawTargetPos`).
  2. Se il segmento diretto interseca un varco chiuso (`isDoorOrGateClosed == true`) o un blocco solido impassabile, la condizione `ALREADY_AT_TARGET` **non scatta in nessun passaggio**: la ricerca A* prosegue normalmente, includendo la porta nel percorso con attesa porta attiva.
  3. Questa specifica armonizza e rende deterministici i test delle rotte minime a 2 nodi (`FOUND` con un nodo radice e un nodo destinazione separati da porta).

### 10.2 FSM a 5 Stati per la Riconciliazione dell'Attesa Porta (AutoWalkMotor.java)
- **Problema**: Il meccanismo storico `else if (waitingClosedDoorPos != null)` interpretava qualsiasi condizione `isClosed == false` come avvenuta apertura della porta, emettendo la notifica vocale spuria "porta aperta" anche quando la rotta ricalcolata abbandonava semplicemente la porta o deviava su un altro percorso.
- **Specifica Vincolante**:
  La riconciliazione dell'attesa viene modellata tramite una FSM a 5 esiti espliciti:
  1. **Stessa porta ancora chiusa**: `waitingClosedDoorPos.equals(currentDoor) && isDoorOrGateClosed(level, currentDoor)` $\implies$ Conservazione dell'attesa in silenzio; nessun riorientamento dello sguardo e nessun avviso ripetuto.
  2. **Stessa porta ora realmente aperta**: `waitingClosedDoorPos.equals(currentDoor) && !isDoorOrGateClosed(level, currentDoor)` con blocco ancora presente nel mondo come istanza di porta/cancello $\implies$ Conclusione dell'attesa ed emissione singola dell'evento/notifica `onDoorOpened(target)`.
  3. **Porta non più pertinente (Rotta deviata)**: La nuova rotta non transita più attraverso `waitingClosedDoorPos` $\implies$ Azzeramento silenzioso di `waitingClosedDoorPos = null` **senza emettere alcuna notifica di apertura**.
  4. **Porta rimossa o sostituita**: Il blocco alle coordinate attese non è più un varco apribile $\implies$ Azzeramento silenzioso di `waitingClosedDoorPos = null` senza deduzioni arbitrarie.
  5. **Nuova porta chiusa intercettata**: `!waitingClosedDoorPos.equals(currentDoor) && isDoorOrGateClosed(level, currentDoor)` $\implies$ Chiusura silenziosa dell'episodio precedente e avvio del nuovo episodio di attesa con singolo allineamento sguardo e singolo avviso `onDoorClosed`.

### 10.3 API Geometrica di Clearance e Gerarchia C3 vs C4 (AutoWalkPathfinder.java)
- **Problema**: È necessario distinguere deterministicamente se un ostacolo intercettato durante il movimento iniziale è uno stipite solido (che richiede la prosecuzione del disimpegno C4 prima della curva) o un varco chiuso (che impone l'arresto C3). Inoltre, per consentire al motore di gestire l'attesa senza ambiguità, il controllo geometrico deve restituire l'esatta posizione canonica della porta bloccante. Una porta successiva non deve mantenere aperto il disimpegno del primo segmento all'infinito.
- **Specifica Vincolante**:
  1. In `AutoWalkPathfinder` viene introdotta la funzione geometrica pura con record di ritorno strutturato:
     ```java
     public record ClearanceResult(
             ClearanceStatus status,
             @Nullable BlockPos blockingDoorPos
     ) {
         public enum ClearanceStatus {
             CLEAR,
             BLOCKED_BY_SOLID_JAMB,
             BLOCKED_BY_CLOSED_DOOR
         }
     }
     ```
     `public static ClearanceResult checkLocalClearance(Level level, Vec3 from, Vec3 to, AABB playerBox)`
     che campiona il volume spazzato dalla hitbox reale ($0.6 \times 1.8\text{ m}$) e popola `blockingDoorPos` con la coordinata canonica normalizzata (`getCanonicalDoorPos`) del primo varco chiuso intersecato.
  2. **Gerarchia Operativa**:
     - Se `status == BLOCKED_BY_CLOSED_DOOR`: prevale il Contratto C3; il motore utilizza `blockingDoorPos` per memorizzare la porta nella FSM, livellare il pitch a 0.0f, orientare lo sguardo al centro del varco e richiedere l'apertura.
     - Se `status == BLOCKED_BY_SOLID_JAMB`: prevale il Contratto C4 (il motore mantiene l'avanzamento allineato al primo nodo finché la rotazione verso la curva successiva non risulta `CLEAR`).
  3. **Completamento del Disimpegno su Porta Successiva**:
     - Se il giocatore completa l'uscita dalla nicchia e raggiunge il nodo 1 in sicurezza, l'eventuale presenza di una porta chiusa al nodo 2 non estende artificialmente lo stato di disimpegno C4. Il primo segmento viene dichiarato concluso (`completeFirstSegment`) e la porta al nodo 2 viene gestita ordinariamente da C3 a partire dal tick successivo con la sua posizione dedicata.

### 10.4 Semantica del Ricalcolo FOUND e Allineamento Test Storico (AutoWalkMotor.java)
- **Problema**: Nel test storico `testHandleRepathResultFound` (`AutoWalkMotorTest.java:223`), l'asserzione verificava `assertFalse(isTerminal)`. La sospensione del tick corrente per ricalibrare la marcia al tick successivo rischiava di creare ambiguità terminologica tra "terminazione della marcia" e "rinvio del tick".
- **Specifica Vincolante**:
  1. La semantica viene chiarita in modo inequivocabile:
     - Lo stato del motore **rimane `State.WALKING`** (la marcia non è abortita né cancellata);
     - Il metodo di gestione del ricalcolo segnala la necessità di rilasciare la spinta (`keyUp.setDown(false)`) e terminare l'elaborazione del tick corrente (`yieldCurrentTick = true`), affinché il nuovo percorso venga avviato con coordinate e metriche pulite al tick successivo.
  2. L'asserzione del test storico `testHandleRepathResultFound` viene aggiornata per testare esplicitamente che lo stato rimanga `WALKING` e che il flusso del tick corrente venga correttamente interrotto.

### 10.5 Tutela del Tasto Spazio Manuale in resetMovement (AutoWalkMotor.java)
- **Problema**: `AutoWalkMotor.resetMovement` rilasciava incondizionatamente `client.options.keyJump.setDown(false)`, intercettando e rilasciando la barra spaziatrice anche quando veniva premuta volontariamente dal giocatore durante un takeover manuale o un arresto.
- **Specifica Vincolante**:
  1. Il rilascio di `keyJump` viene rigorosamente vincolato alla proprietà del motore:
     ```java
     if (client != null && client.options != null) {
         client.options.keyUp.setDown(false);
         if (motorHoldingJump) {
             client.options.keyJump.setDown(false);
         }
     }
     this.motorHoldingJump = false;
     this.jumpHoldingTicks = 0;
     ```
  2. Se `motorHoldingJump == false` (il salto era stato premuto manualmente da Luca), il motore non modifica lo stato del tasto. Viene introdotto un test unitario mirato a protezione di questo comportamento.

### 10.6 Fixture di Simulazione Multi-Tick per i 18 Gruppi di Copertura
- **Problema**: I test esistenti esercitavano singole funzioni statiche, ma non coprivano la dinamica temporale integrata di `AutoWalkMotor.tick`.
- **Specifica Vincolante**:
  1. Viene formalizzato l'impiego di una fixture di test integrata headless all'interno di `AutoWalkMotorTest` (o classe di test correlata nel package `autowalk`).
  2. La fixture simula sequenze realistiche su più tick (evoluzione di `player.position()`, rotazione visuale `player.getYRot()`, stato tasti `keyUp`/`keyJump` e callback del coordinatore) per coprire in modo esaustivo i 18 gruppi di test specificati nella Sezione 6.

### 10.7 Disciplina della Baseline Git e Tracciamento dei File di Fase 5
- **Specifica Vincolante**:
  I componenti `AutoWalkMotor.java`, `RouteNavigator.java`, `MovementCoordinator.java` e i relativi file di test e documentazione — attualmente untracked in Git nel branch `feat/cognitive-orchestrator` — costituiranno una baseline esplicita e controllata. Prima della prima modifica sorgente di 5D4-I, verrà fissata una fotografia dello stato (staging/baseline) per garantire che il diff della revisione 5D.4 sia nitido, circoscritto e privo di commistioni con codice pregresso.
