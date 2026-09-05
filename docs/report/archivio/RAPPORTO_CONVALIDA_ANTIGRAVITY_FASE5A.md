# Rapporto di Convalida Indipendente (Protocollo 2 ASTRALIS) — Sotto-Fase 5A Navigatore e Auto-Walk

- **Destinatario**: Luca
- **Autore**: Antigravity (Senior AI Pair Programmer & Software Engineer)
- **Oggetto**: Analisi, verifica tecnica e convalida indipendente dell'implementazione della Sotto-Fase 5A (`RouteNavigator` e `AutoWalkMotor`), integrata con le correzioni del report `docs/report/RICHIESTA_CONVALIDA_ANTIGRAVITY_FASE5A.md`.
- **Data**: 2026-09-04
- **Stato operativo**: Convalida Indipendente Superata (Protocollo 2 ASTRALIS)

---

## 1. Esito Conclusivo della Convalida

- **Verdetto**: **CONVALIDA PIENA DELLA SOTTO-FASE 5A**.
- **Stato del Dominion Core**: I componenti `RouteNavigator.java` e `AutoWalkMotor.java` sono completi, architetturalmente isolati, dotati di immutabilità strutturale, parità cinematica 1:1 con il codice storico e coperti dai 20 test unitari mirati del dominio.
- **Isolamento e Sicurezza Runtime**: Il controller storico (`AutoWalkController`) e il manager (`AutoWalkManager`) non sono stati modificati; la marcia in-game reale resta quindi affidata al controller collaudato, garantendo zero rischi di regressione prima dell'integrazione pianificata per la Sotto-Fase 5C.

---

## 2. Valutazione sui 7 Assi di Qualità ASTRALIS

### Asse 1 — Validità Architetturale e Tecnologica
- Piena compatibilità con Java 25, Architectury Loom, Fabric + NeoForge, Balm e API Minecraft 26.2.
- Rigorosa tipizzazione, assenza di dipendenze cicliche e corretta collocazione nel package `org.mcaccess.minecraftaccess.features.autowalk`.

### Asse 2 — Efficacia Funzionale
- La netta separazione tra `RouteNavigator` (stato geometrico e avanzamento rotta) e `AutoWalkMotor` (corpo esecutivo, FSM e comandi client) previene nodi orfani e ambiguità decisionali.
- Trattamento esplicito di tutti i quattro esiti di `PathResult` (`FOUND`, `ALREADY_AT_TARGET`, `NO_PATH`, `OUT_OF_RANGE`), eliminando loop su ricalcoli o transizioni spurie.
- Azzeramento esplicito dei tasti virtuali in `resetMovement()` e tracciamento puntuale del possesso del salto (`motorHoldingJump`), evitando il rilascio di comandi non di proprietà del motore.

### Asse 3 — Coerenza con lo Storico e con ASTRALIS
- Le regole matematiche e le soglie storiche di `AutoWalkController` sono state fedelmente conservate.
- L'interfaccia di callback semantica (`MotorCallback`) disaccoppia il motore dagli eventi audio e di narrazione, predisponendo un contratto pulito per il futuro coordinatore.

### Asse 4 — Completezza Implementativa
- Supporto completo dei bersagli dinamici (entità mobili) e statici (`BlockPos`, `BlockPos3d`, `Waypoint`).
- Preservazione preventiva di `targetBefore` prima di `clearRoute()`, garantendo la disponibilità del nome del bersaglio per i messaggi vocali I18N anche su esiti `NO_PATH` o `OUT_OF_RANGE`.
- Gestione integrata di porte e cancelli chiusi con orientamento dello sguardo, arresto della marcia e ripartenza automatica post-apertura.

### Asse 5 — Precisione Geometrica e Temporale
- Tutte le costanti storiche sono conformi:
  - Rotazione Yaw progressiva con clamp a 20.0 gradi per tick;
  - Frenata in curva se deviazione > 55.0 gradi e distanza orizzontale > 0.6 metri;
  - Isteresi dello sprint con cooldown di 20 tick per deviazioni > 15.0 gradi;
  - Avanzamento nodi a 0.45 metri (0.70 metri in sprint) con vincolo verticale `Math.abs(deltaY) < 1.0`;
  - Riconoscimento arrivo finale con distanza quadratica <= 2.0;
  - Soglia ricalcolo dinamico entità a distanza quadratica > 4.0;
  - Watchdog anti-blocco: ricalcolo a 12 tick di immobilità, cancellazione a 24 tick;
  - Salto assistito su gradino (step-up): distanza <= 1.25 metri o collisione orizzontale, dislivello tra 0.30 e 1.25 metri, con 4 tick di tenuta su terreno.

### Asse 6 — Affidabilità e Prestazioni
- Immutabilità strutturale garantita dall'esposizione di `currentPath` tramite `List.copyOf(...)`.
- Calcoli matematici leggeri e assenza di allocazioni superflue nel ciclo di tick; ricalcoli innescati unicamente su eventi scatenanti discreti.

### Asse 7 — Assenza di Regressioni
- Il nuovo codice è completamente disaccoppiato e inattivo durante il gameplay corrente; scanner Pagina Su / Pagina Giù, modulo di sicurezza della traversabilità (Rev MC-26.8), Mentore, suoni e tasti non subiscono alcuna modifica.

---

## 3. Matrice di Simulazione Lineare a Scenari (Formato Lineare NVDA)

### Scenario 1 — Percorso Ordinario, Avanzamento Nodi e Arrivo
- **Se** il giocatore avanza lungo la sequenza di nodi:
  - **Allora** il motore orienta progressivamente la vista a 20 gradi per tick e inietta il comando avanti (`keyUp = true`).
  - **Se** la distanza orizzontale dal nodo scende sotto 0.45 metri (0.70 in sprint) e il dislivello verticale è entro 1.0 metro:
    - **Allora** il nodo avanza con `advanceWaypoint()`, viene emesso `onStepNode()` e, ogni 5 passi residui, `onProgression(remainingSteps)`.
  - **Se** viene superato l'ultimo nodo o la distanza quadratica dalla meta scende a <= 2.0:
    - **Allora** lo stato passa ad `ARRIVED`, tutti i comandi vengono rilasciati tramite `resetMovement()`, lo sguardo punta alla meta se configurato, la rotta viene azzerata e viene emesso `onArrival(target)`.

### Scenario 2 — Entità Bersaglio che si Sposta (Dynamic Entity Tracking)
- **Se** l'entità bersaglio si sposta di oltre 2 blocchi lineari (distanza quadratica > 4.0):
  - **Allora** `shouldRepathForEntity()` restituisce vero e il motore richiede il ricalcolo al navigatore.
  - **Se** l'esito è `FOUND`:
    - **Allora** la nuova rotta sostituisce la precedente, viene emesso `onRepathRequested()` e la marcia prosegue regolarmente.
  - **Se** l'esito è `ALREADY_AT_TARGET`:
    - **Allora** viene invocato immediatamente `finishArrival(...)`, lo stato diventa `ARRIVED` e il tick termina.

### Scenario 3 — Ricalcolo Senza Percorso o Fuori Raggio (`NO_PATH` / `OUT_OF_RANGE`)
- **Se** durante l'inseguimento, il post-atterraggio o il watchdog il pathfinder fallisce il calcolo:
  - **Allora** il motore cattura il bersaglio precedente (`targetBefore`), passa allo stato `CANCELLED`, azzera i tasti, svuota la rotta con `clearRoute()` e notifica `onNoPath(targetBefore)`.
  - **Allora** non rimane alcun nodo fantasma né stato orfano in memoria.

### Scenario 4 — Atterraggio Post-Dislivello
- **Se** il giocatore era in aria (`wasInAir = true`) e tocca terra (`onGround = true`):
  - **Se** il dislivello di atterraggio supera 0.4 metri oppure lo stato era `JUMPING`:
    - **Allora** il motore esegue il ricalcolo di assestamento per confermare la traiettoria da terra.

### Scenario 5 — Watchdog Anti-Blocco
- **Se** il giocatore si muove meno di 0.04 metri su terreno solido per tick consecutivi:
  - **Se** il contatore raggiunge il 12° tick:
    - **Allora** `evaluateStuck` segnala `REPATH` e il motore tenta una rotta alternativa.
  - **Se** il blocco persiste fino al 24° tick:
    - **Allora** `evaluateStuck` segnala `ABORT`, lo stato passa a `CANCELLED`, i comandi vengono rilasciati e viene emesso `onStuck()`.

### Scenario 6 — Interazione con Acqua, Liquidi e Proprietà Salto (`motorHoldingJump`)
- **Se** il giocatore entra in acqua con `autoSwim = true`:
  - **Allora** il motore attiva il salto continuo (`keyJump = true`), registra `motorHoldingJump = true` e azzera il contatore di step-up.
- **Se** durante il nuoto `autoSwim` viene disattivato da opzioni:
  - **Allora** `shouldReleaseMotorJump` rileva che il salto era del motore e lo rilascia (`keyJump = false`).
- **Se** il giocatore esce dall'acqua:
  - **Allora** il motore rilascia il salto automatico di nuoto se era di sua proprietà (`motorHoldingJump = true`).
  - **Se** `motorHoldingJump = false`:
    - **Allora** il motore evita di rilasciare un salto che non risulta di sua proprietà.
- **Se** si verifica un salto assistito su gradino (step-up):
  - **Allora** nel normale movimento su terreno solido il salto conserva i quattro tick storici di tenuta.
  - **Se** entrando in acqua la configurazione prevede `autoSwim = false`:
    - **Allora** il salto automatico viene intenzionalmente rilasciato per rispettare la configurazione dell'utente.

### Scenario 7 — Human Takeover e Gestione Porte
- **Se** nei primi 10 tick di avvio viene premuto un tasto direzionale:
  - **Allora** i tick di grazia ignorano l'input per prevenire interruzioni accidentali.
- **Se** dopo i tick di grazia vengono premuti i tasti S, A, D o Shift:
  - **Allora** il takeover si attiva: i tasti virtuali vengono azzerati, lo stato diventa `CANCELLED` e viene emesso `onTakeover()`.
- **Se** il percorso incontra una porta o un cancello chiuso entro 2.1 metri:
  - **Allora** il motore azzera la spinta avanti (`keyUp = false`), orienta lo sguardo all'ostacolo e notifica `onDoorClosed()`.
  - **Se** la porta viene aperta dal giocatore:
    - **Allora** il motore emette `onDoorOpened(target)` e riprende la navigazione.

---

## 4. Risultati della Suite di Test Eseguita in Locale

- **Comando eseguito**: `.\gradlew.bat --no-daemon test --tests "org.mcaccess.minecraftaccess.features.autowalk.*"`
- **Esito**: **BUILD SUCCESSFUL** (durata 49s).
- **Verifica mirata Sotto-Fase 5A**: 20 test dedicati eseguiti con successo, 0 fallimenti, 0 errori:
  - `RouteNavigatorTest`: 7 test verdi;
  - `AutoWalkMotorTest`: 13 test verdi (inclusa la verifica di isolamento e rilascio del salto in acqua `testAutoSwimJumpOwnershipCondition`).
- **Suite di regressione globale del progetto**: Il dato di 228 test complessivi eseguiti con successo appartiene all'intera suite di progetto, precedentemente verificata in ambiente isolato.

---

## 5. Stato del Checkpoint e Gating Semantico

La **Sotto-Fase 5A** è formalmente conclusa, convalidata e sigillata con le correzioni documentali recepite.
Il passaggio alla **Sotto-Fase 5B** resta subordinato all'esplicita autorizzazione di Luca.
