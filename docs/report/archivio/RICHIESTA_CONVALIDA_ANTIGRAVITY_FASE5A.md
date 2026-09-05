# Richiesta di Convalida Indipendente — Sotto-Fase 5A Navigatore e Auto-Walk

**Destinatario:** Antigravity

**Richiedente:** Luca

**Stato:** In attesa di convalida indipendente. Nessuna ulteriore modifica al codice è autorizzata da questo documento.

## 1. Scopo della richiesta

Questa richiesta chiede ad Antigravity di analizzare, verificare e convalidare in modo indipendente l'implementazione della Sotto-Fase 5A del piano `PIANO_TECNICO_FASE5_NAVIGATORE_E_AUTOWALK.md`.

La verifica deve stabilire se il disaccoppiamento fra `RouteNavigator` e `AutoWalkMotor` è corretto, completo, coerente con il comportamento storico e pronto per il checkpoint formale precedente alla Sotto-Fase 5B.

Si applica integralmente il Protocollo 2 di validazione ASTRALIS. Antigravity deve restare in modalità consultiva: non deve modificare file, avviare la 5B o integrare i nuovi componenti in `AutoWalkManager` senza un nuovo comando esplicito di Luca.

## 2. Modifiche realizzate nella Sotto-Fase 5A

Sono stati creati i componenti di dominio seguenti:

- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/RouteNavigator.java`;
- `src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java`;
- `src/test/java/org/mcaccess/minecraftaccess/features/autowalk/RouteNavigatorTest.java`;
- `src/test/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotorTest.java`.

Il controller storico `AutoWalkController`, `AutoWalkManager`, il navigatore direzionale indipendente, i moduli di sicurezza, i moduli di esplorazione e il coordinatore cognitivo non sono stati modificati in questa sotto-fase.

Di conseguenza la marcia in-game resta ancora affidata al controller storico. Il nuovo dominio non è attivo nel gioco prima dell'integrazione pianificata per la 5C.

## 3. Correzioni di chiusura applicate

### 3.1 Risultati completi del ricalcolo

`RouteNavigator.repath(...)` ora tratta tutti gli esiti del pathfinder:

- `FOUND`: sostituisce percorso, meta e indice;
- `ALREADY_AT_TARGET`: memorizza il nuovo stato e permette al motore di completare l'arrivo;
- `NO_PATH` e `OUT_OF_RANGE`: elimina la rotta obsoleta.

`AutoWalkMotor` riceve il `PathResult` completo nei tre punti di ricalcolo:

- inseguimento di un'entità che si è spostata oltre la soglia quadratica storica `distSqr > 4.0`;
- atterraggio dopo dislivello;
- primo intervento del watchdog al dodicesimo tick di immobilità.

Il metodo `handleRepathResult(...)` applica le transizioni seguenti:

- `FOUND`: la marcia prosegue e il callback segnala il ricalcolo;
- `ALREADY_AT_TARGET`: arrivo immediato, rilascio comandi e stato `ARRIVED`;
- `NO_PATH` o `OUT_OF_RANGE`: rilascio comandi, stato `CANCELLED`, pulizia rotta e callback `onNoPath(targetPrecedente)`.

Il bersaglio viene catturato prima della pulizia della rotta, così la futura 5B potrà costruire il messaggio I18N corretto senza perdere il nome della destinazione.

### 3.2 Immutabilità del percorso

`RouteNavigator` usa ora `List.copyOf(...)` quando riceve un percorso dal pathfinder o dai test. Il getter espone quindi una lista non modificabile e il navigatore rimane proprietario esclusivo del proprio stato.

### 3.3 Proprietà e rilascio del tasto salto

`AutoWalkMotor` traccia `motorHoldingJump`, cioè il possesso del tasto salto da parte del motore.

- Se `autoSwim` è attivo in acqua o liquido, il motore preme il salto, ne registra il possesso e azzera il contatore residuo dello step-up.
- Se `autoSwim` viene disabilitato in acqua, il motore rilascia esclusivamente un salto di sua proprietà.
- All'uscita dall'acqua, il motore rilascia il salto automatico di nuoto.
- Il salto su gradino conserva invece i suoi quattro tick storici.
- `start()`, `stop()` e `resetMovement()` azzerano sia il possesso sia il contatore del salto, prevenendo stati residui fra due sessioni.

## 4. Verifica automatica disponibile

La suite completa è stata eseguita con Java 25, `--no-daemon` e `--no-watch-fs`.

Il normale output di build sotto OneDrive era bloccato da un reparse point nella cartella `build/classes/java/test`. Per non cancellare né alterare tale output, la verifica è stata rieseguita con una cartella temporanea esterna a OneDrive, poi rimossa integralmente.

Esito della verifica isolata:

- compilazione Java completata;
- packaging `shadowJar` completato come dipendenza della suite;
- 228 test eseguiti;
- 0 fallimenti;
- 0 errori;
- 0 test saltati;
- `AutoWalkMotorTest`: 13 test;
- `RouteNavigatorTest`: 7 test;
- build conclusa con `BUILD SUCCESSFUL`.

Gli avvisi osservati non sono introdotti dalla 5A:

- contatori prestazionali Windows e avviso di repository collocato in OneDrive;
- API deprecata in `AcousticOcclusion`;
- versione Balm non conforme al formato semver;
- 31 avvisi relativi a `EnvType.CLIENT` durante la compilazione dei test;
- avviso noto di JOML relativo a `sun.misc.Unsafe`.

## 5. Protocollo ASTRALIS da applicare

Antigravity deve eseguire e riportare il Protocollo 2 completo.

1. **Validità:** confermare la compatibilità con Java 25, Fabric, NeoForge, Balm, Architectury Loom e API Minecraft 26.2.
2. **Efficacia:** verificare che il nuovo dominio risolva nodi obsoleti, arrivo dopo ricalcolo e tasti virtuali incastrati.
3. **Coerenza:** confrontare le nuove transizioni con `AutoWalkController` e con il piano attivo, senza anticipare l'integrazione della 5B o della 5C.
4. **Completezza:** verificare gli esiti `FOUND`, `ALREADY_AT_TARGET`, `NO_PATH`, `OUT_OF_RANGE`, il bersaglio nullo o non valido, il watchdog e l'uscita dall'acqua.
5. **Precisione:** verificare che rimangano invariate le soglie storiche: 20 gradi per tick, 55 gradi per frenata, 15 gradi e 20 tick per isteresi sprint, 0.45/0.70 per i nodi, `Math.abs(deltaY) < 1.0`, soglia finale quadratica 2.0 e watchdog 12/24 tick.
6. **Affidabilità e prestazioni:** verificare che il ricalcolo non lasci rotta, target, tasti o callback fantasma e che i controlli siano limitati ai tick necessari.
7. **Assenza di regressioni:** confermare che scanner Pagina Su/Pagina Giù, sicurezza, esplorazione, Mentore, Accademia, tasti storici e controller in-game restino invariati.

Antigravity deve inoltre simulare in modo lineare:

- percorso ordinario, avanzamento nodi e arrivo;
- entità che si sposta, ricalcolo riuscito e bersaglio già raggiunto;
- ricalcolo senza percorso o fuori raggio, con cancellazione e nessun nodo fantasma;
- atterraggio dopo dislivello e ricalcolo;
- watchdog al dodicesimo e ventiquattresimo tick;
- ingresso e uscita dall'acqua con `autoSwim` attivo e disattivato;
- takeover manuale, porta chiusa e riapertura, verificando che i contratti storici restino disponibili per la futura integrazione.

## 6. Esito richiesto ad Antigravity

Il rapporto di risposta deve contenere un solo esito conclusivo:

- convalida piena della 5A;
- convalida con riserve, indicando il blocco esatto;
- mancata convalida, indicando il difetto e una proposta correttiva senza applicarla.

La convalida della 5A non chiude la Fase 5 completa. Restano obbligatorie la 5B, la 5C, la 5D e il collaudo in-game con NVDA prima della validazione empirica finale ASTRALIS.
