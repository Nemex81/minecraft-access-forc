# Piano tecnico correttivo MC-26.22 — Transizioni verticali e landing

- **Tipologia**: correttivo strutturale, post sesto collaudo; Sotto-Fase 1A.
- **Autore / revisori**: Codex / Luca e Antigravity.
- **Data**: 2026-09-12, Europe/Rome.
- **Stato**: `[COMPLETATO E COLLAUDATO IN-GAME DA LUCA — CHIUSURA APPROVATA]`.
- **Ramo osservato**: `feat/dual-fall-safety-subsystem`.
- **Incremento Versione Target (AVF)**: correzione PATCH nella lavorazione MC-26.22; numero definitivo da determinare sul riferimento di versione verificato alla chiusura. Nessun incremento applicato in 1A.
- **Report**: [handover archiviato e stato finale](../../report/archivio/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md).
- **Registro**: [REGISTRO_REVISIONI.md, MC-26.22](../../report/REGISTRO_REVISIONI.md).
- **Strategie archiviate**: [approfondimento Antigravity](../../strategie/archiviate/STRATEGIA_CORRETTIVA_DISMOUNT_SALITA_E_DISCESA.md) e [architettura generale](../../strategie/archiviate/STRATEGIA_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md).

## 0. Autorità, perimetro e checkpoint riprendibili

Questo è il piano operativo unico della correzione. La convalida richiesta riguarda D30–D41 e le relative verifiche, compresa la bonifica circoscritta. L'autorizzazione attuale copre solo documentazione e archiviazione dei piani superati; implementazione, test eseguibili, build e deploy attendono la convalida di Luca.

- [x] P0 — Analisi read-only di codice, log, geometria persistita e identità del JAR; contraddizioni della diagnosi distinte dalle evidenze.
- [x] P1 — Contratti, matrice a tre livelli e audit preventivo redatti e riesaminati.
- [x] P2 — Convalida esplicita del piano da parte di Luca.
- [x] I1 — D30/D40: fissare baseline, fixture e riproduttori significativi che falliscono sul difetto attuale.
- [x] I2 — D31/D32: contatto funzionale e aggancio geometrico; recuperi limitati.
- [x] I3 — D33/D34: sbarco ascendente, landing e stabilità coerenti con il tick reale.
- [x] I4 — D35/D36: ingresso discendente e transito/uscita inferiore entro corridoio sicuro.
- [x] I5 — D37/D38/D39: progresso di fase, identità run, safety, equivalenza delle due origini e telemetria.
- [x] I6 — D40: matrice integrata e suite completa prima della bonifica.
- [x] I7 — D41: bonifica verificata di residui e tentativi sostituiti, con elenco delle rimozioni e prove di non utilizzo.
- [x] I8 — Suite post-bonifica, controlli mirati, build finale e deploy verificato con copia di ripristino.
- [x] V1 — Collaudo NVDA di Luca, con evidenze correlate al JAR finale e confronto col mondo.
- [x] V2 — Convalida funzionale esplicita di Luca e chiusura documentale.

Ogni fase è spuntata solo dopo l'evidenza prevista. Registrare sotto la fase data, file interessati, comando/esito, prove mancanti e prossimo passo. Una spunta automatica non certifica la fisica in-game. Se la sessione termina, ripartire dal primo checkpoint incompleto e verificare prima eventuali modifiche concorrenti.

### 0.1 Precedenza e conservazione della storia

I tre predecessori sono conservati in `docs/piani/superati/MC-26.22/`, non in `completati/`:

- [Piano originario](../superati/MC-26.22/PIANO_TECNICO_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md): baseline di comandi, I18N e architettura.
- [Correzione scalata](../superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_SCALATA_VERTICALE.md): storia di segmentazione, cursore e FSM.
- [Correzione discesa](../superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md): storia topologica, lease e D10–D29.

Restano invarianti la distinzione `NO_PATH`/`SEARCH_BUDGET_EXHAUSTED`, gli archi topologici validati, i segmenti tipizzati, il motore condiviso, il takeover fisico e l'assenza di manipolazioni dirette di posizione/velocità. D31 sostituisce la semantica incompleta D23; D32/D35 rettificano D24; D37 sostituisce D25; D33/D34/D36 rettificano D26/D27; D38/D39 precisano D28/D29. Le precedenti certificazioni cinematiche non valgono come accettazione della nuova revisione.

## 1. Evidenze e limiti della diagnosi

- JAR del sesto collaudo e del build osservato: SHA-256 `56284E5478719016DB25E9F4699D18038A3C98ECB8EDF5A3B5956D6D6B249217`. I 393 test verdi sono baseline storica; non sono stati rieseguiti nell'analisi.
- `latest.log` dell'istanza `Minecraft 26.2 Access 1.12.0`: rotta 44, righe 415/418/419, salita da 81.42 a 83.88, poi discesa a 82.49 e aborto; sette aborti DISMOUNT nella sessione. La rotta 37 presenta 36 `REMOUNT_RETRY`.
- Nessun evento delle fasi d'ingresso discendente nel log del sesto collaudo: il rischio CAPTURE_WAIT è dimostrato come controesempio geometrico, non come esito live già osservato in questa sessione.
- Mondo operativo identificato dalla persistenza aggiornata: `%APPDATA%/PrismLauncher/instances/Minecraft 26.2 Access 1.12.0/minecraft/saves/scuola di sopravvivenza mondo 2 (1)`. Il nome visualizzato nel log non distingue la copia senza suffisso, più vecchia.
- Colonna verificata: ladder `facing=north` a X=-60, Z=-42, Y=82..84; supporto SOUTH, balconata Y=81, tetto Y=85 con landing superiore (-60,85,-41). Yaw 0 indica SOUTH, non NORTH. La posa giocatore del report è storica: il salvataggio corrente è successivo e non ricostruisce da solo la traiettoria del collaudo.
- Il salvataggio corrente e i log con `onGround=true` mostrano `Motion.y=-0.0784000015`; il motore gira dopo il tick mondo. La componente di velocità destinata alla prossima integrazione non coincide con lo spostamento verticale effettivamente realizzato.

### 1.1 Cause accertate e rettifiche alla proposta Antigravity

1. `evaluateDismount` richiede supporto complanare ai piedi prima di alimentare W, anche in salita; a 83.88 manca ancora 1.12 m al tetto. Il watchdog aumenta comunque, senza progresso di fase.
2. `ClimbLandingProbe.stable()` impone `abs(velocityY)<=0.03`; il ramo appoggiato azzera il watchdog anche se la stabilità non arriva. Può quindi lasciare la run aperta indefinitamente. Lo stesso ramo precede l'aborto per revisione rotta diversa.
3. `evaluateMount` accetta il solo aumento di quota come aggancio e azzera i recuperi: un salto fuori colonna riapre il ciclo. Il problema non si risolve con una distanza radiale più permissiva.
4. `ClimbContactProbe` confonde intersezione della lamina, presa funzionale e volume colonna. Nei log `OUTSIDE` coesiste con `onClimbable=true`; la collisione può mantenere il corpo tangente alla lamina. Inoltre bottom crossing e BELOW_COLUMN non certificano da soli appartenenza XZ.
5. A Z=-41.45, per corpo largo 0.6, maxZ=-41.15; la ladder NORTH inizia a Z=-41.1875. Restano circa 0.0375 m di sovrapposizione alla sua sommità portante. Lo spegnimento anticipato può dipendere dall'inerzia per completare l'ingresso. Questi numeri sono fixture, mai costanti operative.
6. Il trasferimento predetto di 0.08 m non usa velocità effettiva, attrito o percorso continuo. Il solo supporto futuro del landing non autorizza W senza verificare come raggiungerlo.
7. `SafetyMovementGuard` già protegge la lease dal detector generico; WASD usa anche il vettore d'inerzia dopo il rilascio. Non introdurre una seconda lease o rimuovere il presidio manuale per correggere l'automatismo.

## 2. Architettura e responsabilità

Conservare il flusso esistente: selezione/ricerca, assemblaggio di segmenti e metadati, raccolta runtime nel motore, decisione pura, arbitraggio input, avanzamento del navigatore. AutoWalk e Climb Assistant devono produrre le medesime decisioni a parità di snapshot.

- `ClimbTraversalAnalyzer`, `ClimbRouteAssembler`, `ClimbEntryTransition`, `ClimbTraversal`: geometria pianificata e identità della run; estendere i dati solo dove manca un contratto verificabile. Congelare le BlockPos ricevute, comprese quelle mutabili.
- `ClimbContactProbe`: osservazioni su presa, colonna e attraversamenti; nessuna scrittura di input.
- `ClimbLandingProbe`: distinguere regione sicura di destinazione, appoggio attuale e trasferimento ammissibile; nessuna FSM autonoma.
- `ClimbKinematics`: unica decisione deterministica, specializzata per direzione, fase e tipo arrampicabile.
- `AutoWalkMotor`: raccoglie AABB, stato fisico e mondo una volta per tick; applica decisioni e conserva la sessione. Non aggiungere un secondo controller.
- `SafetyMovementGuard`/`ControlledDescentPort`, `DoorInteractionManager`, `RouteNavigator`: restano proprietari rispettivamente di Sneak/lease, botole e cursore rotta.

La previsione geometrica di movimento deve riusare helper compatibili esistenti. Se manca un helper adatto, ammettere un componente puro locale al dominio traversal con input immutabili e test condivisi; evitare infrastrutture generiche, nuove dipendenze o copie della fisica vanilla. I probe non devono dipendere da singleton del client; un adattatore runtime fornisce collisioni e dati necessari.

## 3. Contratti formali D30–D41

### D30 — Baseline e identità immutabile della sessione

Precondizione: rotta tipizzata valida e mondo caricato. Identità minima: revisione rotta, dimensione, columnId, direzione, landing/uscita. Conservare la geometria attiva senza sostituirla silenziosamente a ogni segmento. Verificare l'identità prima di input, rinnovo lease, ramo appoggiato e avanzamento waypoint, non solo dentro il completamento. In caso di invalidazione: fermare gli input posseduti, rilasciare la propria lease e chiudere con motivo topologico. Nessun progresso di una run precedente è trasferito alla nuova. Stato e controlli O(1).

### D31 — Presa funzionale e geometria di contatto

Esporre separatamente: appartenenza alla colonna attiva, presa arrampicabile corrente, contatto/tangenza alla shape, attraversamento percorso, sostegno al bordo, uscita inferiore. La lettura vanilla `onClimbable` va associata alla cella/colonna effettiva e alle regole di botola; non basta una ladder qualsiasi nelle vicinanze. L'intersezione della sagoma sottile non è necessaria né sufficiente da sola a certificare la presa.

Usare AABB reale e precedente, celle arrampicabili incontrate alla quota del corpo e shape effettive. L'unione AABB dei due estremi è soltanto una broad phase: nei movimenti diagonali non certifica che il percorso abbia toccato la colonna. Il commit e il bottom crossing richiedono attraversamento firmato e permanenza nel corridoio XZ della stessa run. Teletrasporto/discontinuità invalida il campione precedente. Valutare un insieme locale limitato di celle, non tutta la colonna a ogni tick.

### D32 — Aggancio ascendente e recuperi finiti

Prima di salire, allineare posizione laterale e orientamento con una posa d'ingresso raggiungibile; la sola direzione verso il muro non corregge un errore sull'altro asse. Il percorso d'approccio deve avere supporto e clearance. Il salto iniziale è ammesso soltanto nel volume d'ingresso e con spazio testa libero.

L'aumento di Y causato da un salto non promuove da solo a TRANSIT. Richiedere D31 o una transizione fisica coerente dalla posa validata. Conservare il conteggio recuperi attraverso MOUNT/TRANSIT; al massimo un recupero per episodio di mancata presa. Riarmarlo solo dopo avanzamento autentico nella colonna verso un nuovo piolo, mai per salto, oscillazione o cambio di etichetta della fase. Esaurimento: arresto esplicito, non ciclo infinito.

### D33 — Sbarco ascendente completo e direzionale

Separare internamente DISMOUNT in sollevamento residuo, trasferimento e stabilizzazione, con stato esplicito e senza una seconda FSM concorrente. Consumo dell'ultimo piolo e raggiungimento del tetto non sono equivalenti.

- Nel sollevamento, mantenere i comandi che producono salita sul tipo corrente finché resta presa valida e l'uscita è praticabile: per ladder/vine direzione verso supporto; per scaffolding politica Jump già prevista. Non introdurre W incondizionato fino a onGround.
- Non ruotare prematuramente verso un landing laterale se ciò fa perdere il supporto necessario alla salita. La quota di trasferimento deriva dalla superficie reale, dall'ingombro corporeo e dalle possibilità vanilla di superamento del bordo.
- Trasferire verso una regione interna sicura soltanto con percorso corporeo libero, appoggio finale verificato e sostegno lungo la transizione fornito dalla scala o dal pavimento. Collisione tangente al supporto è lecita; compenetrazione e urti testa non lo sono.
- Botola: usare lo stato effettivamente osservato dopo `DoorInteractionManager`; una richiesta di apertura non equivale a passaggio libero. Ostacolo nuovo, perdita irreversibile di presa o mancato progresso: arresto motivato.
- Stabilizzare secondo D34; avanzare dal landing solo dopo completamento. Non usare il timeout per compensare una soglia di uscita errata.

### D34 — Regione di landing e stabilità osservata

Separare tre risultati: destinazione praticabile alla sua quota, appoggio presente sotto il corpo, trasferimento raggiungibile. La certificazione della destinazione non rende il giocatore già arrivato.

La regione di landing è una superficie connessa raggiungibile dall'uscita, associata alla run; non l'intero intorno 3x3 indiscriminato. Usare altezze reali delle collisioni anche per lastre/scale. I quattro punti inset sono controlli necessari; shape disgiunte, fori e impronte parziali richiedono verifica della copertura della regione portante, non un falso pieno tra quattro campioni.

Completamento dopo almeno due osservazioni consecutive della stessa run con onGround, appoggio nella regione autorizzata, quota compatibile, corpo libero e variazione effettiva dei piedi stabile. Misurare lo spostamento tra tick confrontabili nello stesso hook; non esigere velocità grezza nulla né alzare arbitrariamente l'epsilon per accettare -0.0784. La velocità grezza resta diagnostica e parte della previsione, distinta dal moto realizzato. Verificare anche moto orizzontale residuo e permanenza prevista sulla superficie prima del ritorno a WALKING/ARRIVED.

Supporto senza stabilità non azzera indefinitamente il watchdog: usare D37 con limite esplicito alla stabilizzazione. Revisione diversa viene intercettata da D30 anche se il corpo è appoggiato. Postcondizione: input di scalata rilasciati, lease propria rilasciata, unico annuncio e avanzamento atomico.

### D35 — Ingresso discendente senza dipendenza dall'inerzia

Conservare ALIGN/APPROACH/CAPTURE_WAIT, rettificandone i cancelli. Il solo centro nella banda 0.45 non decide l'arresto. Il probe deve osservare supporto effettivo dell'impronta, inclusi bordo superiore della ladder e botola, traiettoria verso la presa e limite esterno sicuro.

APPROACH alimenta W solo verso la cattura validata con lease attiva. CAPTURE_WAIT a W spento è ammesso quando il corpo ha superato l'appoggio impeditivo dentro il corridoio e può essere preso dalla colonna, oppure la presa è già effettiva. Se resta appoggiato, consentire solo l'avanzamento necessario certificato dal volume previsto; se non certificabile, fermarsi sul supporto sicuro e segnalare la causa.

Previsione: derivare l'inviluppo del prossimo movimento e del residuo dopo rilascio da velocità, input, stato al suolo e proprietà fisiche pertinenti. Usare il percorso continuo tra pose, non uno spostamento costante di 0.08 m o una sola AABB finale. Validare contro geometria corrente; condizioni fisiche non modellate devono produrre esito non certificato, senza abbassare la sicurezza. Il commit firmato dentro colonna rilascia atomicamente W/Jump; nessun riuso della spinta d'ingresso dopo il commit. Vietati setPos/setDeltaMovement e impulsi S per simulare discesa verticale.

### D36 — Transito discendente, fondo e uscita laterale

Mantenere discesa gravitazionale su ladder/vine e politica Sneak specifica per scaffolding. Contatto, fondo e landing precedono il watchdog; il solo BELOW_COLUMN non autorizza uno sbarco se XZ è fuori corridoio.

Se la balconata sostiene già il corpo, W resta spento e si applica D34. Se occorre trasferimento laterale, riusare il percorso validato D33/D35 con sostegno scala/pavimento continuo: non pretendere appoggio complanare già esistente quando il corpo è ancora in transizione, né autorizzare vuoto non protetto. Dopo perdita del corridoio azzerare gli input nocivi e dichiarare interruzione; lo Sneak non arresta una caduta già aerea. Recupero post-commit ammesso solo se esiste una traiettoria verificata verso la stessa presa; altrimenti arresto. Nessun ritorno all'approccio del tetto.

### D37 — Progresso per fase e terminazione

Misurare progresso verso l'obiettivo attuale: allineamento alla posa, attraversamento d'ingresso, salita/discesa firmata, sollevamento residuo, trasferimento, stabilizzazione. Confrontare con il miglior progresso già raggiunto nella fase per evitare che oscillazioni resettino il timer. Sommare piccoli avanzamenti autentici; il conteggio non riparte per rinomina della fase o retry.

Conservare la finestra di mancato progresso esistente come baseline, senza estenderla per nascondere errori. Distinguerla dal limite dei recuperi. Azzerarla solo su progresso certificato; ingresso in una nuova fase valida inizializza il suo riferimento. Per stabilizzazione richiedere D34 entro la finestra oppure chiudere con motivo specifico. Ogni cammino decisionale deve terminare o dimostrare progresso; nessuna attesa perpetua, inclusi ALIGN e appoggio instabile.

### D38 — Integrazione safety, botole e origini

AutoWalk e Alt+S condividono snapshot, decisione, controllo geometrico e istanza guard. L'assembler tattico non deve inventare un ingresso CLEAR solo dalla vicinanza dei BlockPos: risolvere anche per esso passaggio e botola con gli stessi controlli usati dal pathfinder. Il pathfinder conserva budget, pesi e distinzione degli esiti.

Acquisire/verificare la lease prima degli input di ingresso discendente; rinnovarla solo con identità e geometria autorizzate. Il detector generico non la revoca per assenza di input orizzontale. Pericolo estraneo al corridoio, invalidazione del mondo o perdita della run usano il percorso di revoca esplicita esistente; verificarne i chiamanti e collegarlo dove manca, senza duplicare il guard. Priorità a Shift fisico, takeover e lifecycle; input hardware non disponibile resta fail-closed.

La guida WASD conserva il proprio intento e il vettore d'inerzia: non avviare automaticamente la FSM tattica né concedere lease persistenti per il solo rilascio di W. Dimensione, morte, disconnessione, target invalidato e annullamento rilasciano tutte le risorse della run. Le pose di botola vengono rivalidate dopo ogni cambio osservato.

### D39 — Telemetria utile e limitata

Integrare il logger esistente. Eventi su cambi di fase, presa/supporto, motivo di attesa/aborto e completamento; deduplicare gli stati identici. Un reason code non nullo ripetuto non autorizza log a ogni tick. Campi: origine, revisione/colonna, fase direzionale, piedi precedenti/correnti, delta reale, velocità grezza, cella di presa, supporto residuo, esito corridoio, landing, input richiesti/applicati, lease effettiva, progresso/watchdog/recuperi.

Distinguere almeno mancata presa, uscita impedita, assenza di progresso, landing instabile, cambio rotta, corridoio perso e pericolo; non etichettare ogni timeout OUTSIDE_COLUMN_ABORT. Nessuna nuova dipendenza di logging. Narrazioni tramite le chiavi esistenti; eventuali nuove chiavi IT/EN solo se necessarie e alfabeticamente ordinate.

### D40 — Prove con oracoli indipendenti

I test devono combinare shape reali con sequenze temporali significative e verificare gli input applicati, non solo record con booleani precostituiti. Riutilizzare JUnit/Mockito e i seam presenti; 0 sleep, nessun mondo reale modificato. Predisporre fixture vanilla per i quattro facing e i tipi supportati. I numeri del Belvedere servono alla riproduzione, non alla logica produttiva.

Minimo: probe geometrici; sequenze della cinematica con stato riportato al tick successivo; integrazione `AutoWalkMotor.processClimbTick` con mondo configurato, guard, input e navigatore; confronto AutoWalk/Alt+S. Includere il valore grezzo -0.0784 con quota stabile e un vero moto discendente che deve invece fallire. Verificare una transizione che dura oltre 15 tick con progresso, e uno stallo che termina; 36 retry non devono essere possibili.

L'evidenza headless valida i contratti. La fisica completa, collisioni moddate e feedback NVDA richiedono V1; una sequenza di snapshot costruiti non equivale a simulazione completa di Minecraft.

### D41 — Bonifica finale mirata, reversibile e verificata

Mandato di Luca: rimuovere, dopo la sostituzione e prima della build finale, codice morto/obsoleto dei tentativi correttivi, senza eliminare funzionalità ancora necessarie. La convalida del piano autorizza questa bonifica limitata; elementi di utilizzo incerto restano bloccati e segnalati fino a verifica.

1. Inventariare candidati e riferimenti in codice, test, risorse, entry point, Mixin/reflection e configurazioni. Per ogni rimozione indicare responsabilità sostitutiva e prova di non utilizzo.
2. Candidati da verificare: banda CAPTURE_DEPTH come criterio di verità; passo predetto fisso; fallback di aggancio da solo delta Y; ramo dismount complanare unico; reset recuperi non qualificati; costanti senza consumatori; costruttori di snapshot legacy che nascondono dati mancanti; REACQUIRE se privo di ingressi reali; motivi telemetrici fuorvianti e duplicazioni geometriche.
3. Non cancellare automaticamente l'intero probe o lo stato REACQUIRE: prima decidere se D31–D38 ne assegnano una funzione necessaria e coperta. Migrare i chiamanti prima di rimuovere API; preservare gli scenari dei test eliminando gli oracoli falsi.
4. Conservare fuori dai sorgenti compilati una copia/diff delle sole rimozioni, con hash e provenienza; nessuna perdita delle modifiche concorrenti. Non usare reset Git o pulizie globali. Nessun vecchio algoritmo disattivato lasciato come secondo percorso operativo.
5. Rieseguire dopo la bonifica i test interessati e la suite completa; cercare riferimenti orfani, compilare e controllare risorse/I18N se coinvolte. Registrare l'esito e l'inventario nel report in forma compatta. Se un candidato resta irrisolto, I7 non è completato.

## 4. Matrice di simulazione e accettazione

### Livello 1 — Percorsi nominali

- T1: salita Belvedere da balconata a ultimo piolo 83.88, sollevamento e appoggio sul tetto 85; nessuna interruzione della propulsione necessaria, completamento unico dopo stabilità.
- T2: discesa dal tetto alla balconata 81; superamento sommità ladder, commit senza spinta successiva verso parapetto, arresto stabile e ripresa della rotta orizzontale solo dopo D34.
- T3: stessa geometria e stesso stato iniziale per AutoWalk e Alt+S producono gli stessi comandi e controlli; salita/discesa delle impalcature mantengono la politica specifica.

### Livello 2 — Alternative e interazioni

- T4: quattro facing, landing laterale, colonne di altezze diverse, vines e scaffolding; niente coordinate o comandi ladder applicati indiscriminatamente.
- T5: botola assente, aperta, chiusa apribile, ferro/non apribile e chiusura durante la run; richiesta di apertura senza conferma non abilita avanzamento.
- T6: Shift/WASD fisici, rilascio tasti con e senza inerzia, alternanza detector/motore nello stesso ciclo, stop Alt+S, dati hardware indisponibili: nessun override del takeover e nessuna lease orfana.
- T7: cambio rotta/colonna/dimensione, target invalidato, morte e disconnect, anche mentre supported=true: invalidazione immediata prima dei comandi e del completamento.

### Livello 3 — Limiti e regressioni

- T8: salto fuori colonna e approccio decentrato; vietato falso aggancio, recuperi finiti e correzione laterale solo su percorso sicuro.
- T9: sommità ladder ancora portante per 3.75 cm, velocità bassa/alta e piedi ancora a Y=85; nessuna dipendenza da inerzia casuale e nessuna spinta oltre corridoio.
- T10: onGround con velocità grezza -0.0784 e piedi stabili; completamento valido. Caduta reale, appoggio intermittente, scivolamento laterale o oscillazione: nessun falso completamento.
- T11: ultimo piolo attraversato tra tick, contatto tangente, ladder vicina estranea, swept diagonale che racchiude ma non attraversa la colonna, posa discontinua: nessun commit/crossing falso.
- T12: lastre, gradini, fori tra supporti, parapetto, soffitto basso, landing distrutto e pericolo esterno: clearance/supporto/corridoio respingono la manovra non sicura.
- T13: progresso lento ma autentico oltre la finestra nominale, stallo reale, oscillazione e supported mai stabile: watchdog riconosce solo progresso utile e garantisce terminazione.
- T14: replay degli scenari dopo bonifica e audit riferimenti: stessi comportamenti attesi, nessun ramo alternativo obsoleto, chiamante orfano o duplicazione di writer.

Per ogni T registrare PASS/FAIL/NON ESEGUITO e tipo di evidenza. T1/T2/T3/T5/T6 richiedono anche collaudo NVDA; le varianti non riproducibili in-game devono essere dichiarate tali, senza estendere la convalida oltre l'evidenza.

## 5. Verifica preventiva: sette assi e sei cancelli

Esito: **piano tecnicamente convalidabile**, con verifiche esecutive vincolanti; codice attuale non convalidato funzionalmente dopo il sesto collaudo.

1. Validità: D31/D34 modellano presa vanilla e campionamento dopo il tick; D33 distingue supporto e direzione. La previsione D35 deve essere confrontata con runtime e attributi reali prima di certificarla.
2. Efficacia: D32 elimina i falsi agganci, D33 mantiene la salita necessaria, D34 rimuove il falso stallo a terra e D35 affronta la sommità portante. Obbligatorie prove integrate, non sole verifiche dei singoli booleani.
3. Coerenza: D30/D38 preservano navigatore, lease, botole, takeover e motore unico. WASD rimane guida manuale con safety condivisa.
4. Completezza: T1–T14 coprono ingressi, transiti, uscite, concorrenza, recuperi, cleanup e lifecycle. I tipi non modellati non ottengono certificazioni per analogia.
5. Precisione: contatto funzionale, appoggio target/attuale e moto reale sono distinti; niente soglie Belvedere o blanket tolerance. Curve di attrito e collisioni vanno validate, non indovinate.
6. Prestazioni: raccolta locale una volta per tick, broad phase seguita da verifica sui soli volumi candidati, log deduplicato. Misurare numero di query/allocazioni su scala lunga e idle; nessuna scansione O(altezza) nel tick. Cache solo per dati stabili, invalidata su cambi osservati di geometria/run.
7. Assenza di regressioni: oracoli indipendenti, equivalenza delle origini, D41 e suite post-bonifica. Il precedente verde non sostituisce le prove mancanti né il nuovo collaudo.

Protocollo 12: superato nella progettazione, da attestare in esecuzione per ciascun cancello:

- C1: niente aumento budget A*, pesi o timeout per compensare geometria errata.
- C2: input fisico distinto dagli input virtuali e dai freni reattivi.
- C3: AABB reale, shape parziali, clearance continua e regione portante.
- C4: D30–D41 esplicitano responsabilità, condizioni, esiti e test.
- C5: sequenze deterministiche senza sleep, collaudo fisico separato.
- C6: nessun nuovo router monolitico; router entro 250 righe ove creati/modificati, dettagli nel dominio; nessun refactoring globale dei file legacy solo per raggiungere una misura numerica.

### 5.1 Riesame avversariale del piano

Respinte cinque scorciatoie: W sempre acceso fino a terra; validità del landing usata come appoggio presente; velocità grezza accettata alzando epsilon; cattura dedotta dalla sola AABB inviluppo; limite recuperi azzerato da un salto. D33–D37 impongono guardie e oracoli che rendono queste regressioni verificabili. La cancellazione della legacy è successiva alla migrazione dei chiamanti e seguita da nuova suite: la bonifica non può mascherare una funzione mancante eliminandone il test.

## 6. Esecuzione successiva alla convalida

1. Acquisire stato ramo/worktree e hash dei file coinvolti; evitare scritture concorrenti. Fissare dipendenze/runtime realmente disponibili senza aggiornamenti del toolchain estranei.
2. Eseguire I1–I5, con test mirati per ogni contratto. Annotare ogni scostamento sostanziale dal piano prima di ampliarne il perimetro.
3. I6: suite completa con `.\gradlew.bat --no-daemon --no-watch-fs --rerun-tasks test`, conteggio XML e motivi di eventuali skipped; non prescrivere un numero totale futuro come obiettivo.
4. I7: bonifica D41. I8: ripetere suite sul codice bonificato, compilazione e `.\gradlew.bat --no-daemon --no-watch-fs shadowJar`. Checkstyle: zero nuove violazioni nei file toccati, baseline globale dichiarata separatamente; nessuna riscrittura estranea.
5. Deploy della build finale nelle istanze di collaudo concordate a gioco chiuso; identificare i destinatari reali, conservare JAR precedente fuori da mods e verificare SHA-256 build/installato. Mai promuovere un artefatto pre-bonifica.
6. V1: Luca prova salita e discesa Belvedere, AutoWalk e Alt+S, botole e interruzioni. Correlare log e persistenza al mondo corretto e al JAR verificato. Mancata copertura di una variante rimane esplicita.
7. V2: solo dopo conferma Luca chiudere MC-26.22, definire AVF e archiviare il piano in completati. Commit/push/merge e diffusione della build stabile restano operazioni di chiusura da concordare, non conseguenze della sola redazione.

## 7. Stato della presente consegna

Prodotti: nuovo piano, nota sintetica nel report, stato corrente del registro, predecessori conservati come superati e puntatori riallineati. Nessuna modifica a sorgenti/test/configurazioni/JAR e nessuna nuova esecuzione della suite in 1A. La bonifica richiesta è parte obbligatoria dell'implementazione futura, non risulta già eseguita.
