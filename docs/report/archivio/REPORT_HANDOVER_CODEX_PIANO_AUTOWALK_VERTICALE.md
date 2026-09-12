# Handover MC-26.22 — Stato finale archiviato delle revisioni verticali

> **Stato archivio**: documento archiviato dopo conferma di Luca del collaudo positivo finale. Il sistema risulta funzionante in salita e discesa, sia con AutoWalk sia con Climb Assistant semi-automatico. Non è più un report operativo attivo.

## Aggiornamento Codex — 2026-09-12 — Piano correttivo post sesto collaudo

- **Stato storico intermedio**: il sesto collaudo aveva rilevato regressioni e aveva richiesto il piano D30-D41. La revisione è poi stata completata e collaudata con successo, come indicato nello stato archivio e nelle sezioni finali del documento.
- **Diagnosi integrata**: confermato lo spegnimento prematuro in salita; aggiunti stabilità errata sul valore grezzo -0.0784, recuperi riarmati dal salto, presa funzionale distinta dalla collisione e ingresso frenabile sulla sommità portante della ladder. Nel log esaminato: 36 recuperi della rotta 37, sette aborti DISMOUNT, nessun evento di ingresso discendente; per quest'ultimo resta una verifica geometrica preventiva.
- **Piano e direttive**: [piano tecnico completato, D30–D41](../../piani/completati/PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md), con motore condiviso, transizioni direzionali, stabilità da moto osservato, watchdog di fase, matrice integrata e bonifica obbligatoria dopo la sostituzione, prima della suite/build finali.
- **Registro e storia**: [ARCHIVIO_REVISIONI.md, MC-26.23](../ARCHIVIO_REVISIONI.md) contiene la voce breve conclusiva. I tre piani precedenti sono conservati in `docs/piani/superati/MC-26.22/`, mentre il piano finale è in `docs/piani/completati/`.

---

## Storico conservato — consegne e risultati precedenti

# Report di Handover Ingegneristico: Esito 4° Collaudo & Mandato per Nuova Strategia Correttiva Discesa Scale (Rev MC-26.22)
# Mittente: Antigravity (AI Pair Programmer Primario)
# Destinatario: GPT Codex / ChatGPT (Copilota Ausiliario e Peer Programmer)
# Autore & Committente: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA)
# Data: 2026-09-11 (Aggiornato post 4° Collaudo Live ore 18:42..18:43)
# Framework: ASTRALIS v3.0.4 — Protocolli 1, 4, 5 e 12 (Inner Codex Pattern)
# Riferimento Strategico Master archiviato: docs/strategie/archiviate/STRATEGIA_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md
# Piano Tecnico Corrente: docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md
# Registro Revisioni RRU: docs/report/REGISTRO_REVISIONI.md

---

## 🎯 MANDATO OPERATIVO PER GPT CODEX (ZERO MODIFICHE CODICE)

Caro Codex,
su direttiva categorica di Luca:
1. **DIVIETO ASSOLUTO DI MODIFICARE IL CODICE**: non toccare, creare o alterare classi Java o file di implementazione.
2. **ANALISI APPROFONDITA A VERIFICA DELLA CAUSA REALE**: esamina con massimo rigore logico e geometrico i log reali del 4° collaudo in-game (riportati sotto), il comportamento cinematico di discesa, l'interazione con `ProximityFallDetector` e `SafetyMovementGuard`, e la convergenza tra guida manuale (Climb Assistant) e guida automatica (AutoWalk).
3. **STRATEGIA CORRETTIVA & NUOVO PIANO TECNICO UNIFICATO**:
   - Individua la strategia correttiva più efficace, coerente, sinergica e completa.
   - Sia in guida manuale (`ClimbAssistantController` / `Alt+S`) sia in navigazione automatica (`AutoWalkMotor`), il sistema deve convergere verso la **stessa identica routine/motore** adibita alla gestione della scalata/discesa verticale, eliminando disparità o fallimenti di aggancio.
   - Redigi/aggiorna il Piano Tecnico Formale in `docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md` (senza applicarlo a codice), pronto per la convalida di Luca.

---

## 📍 1. EVIDENZE TELEMETRICHE REALI DEL 4° COLLAUDO (`latest.log` ore 18:42:17..18:43:01)

### Prova 1: Target Scala a pioli (ore 18:42:17)
```text
[18:42:17] [AutoWalk Telemetry] Target: BlockPos{x=-60, y=83, z=-42}, Dist: 8.2m, Budget: 5000, Pass1: FOUND (86 nodes), Pass2: NOT_RUN (0 nodes), Fallback: DISABLED
[18:42:18] Narrating(interrupt:true)= Navigazione verso Scala a pioli, distanza 19 metri, 13 passi
[18:42:18] Narrating(interrupt:false)= Ancora 10 passi
[18:42:19] Narrating(interrupt:true)= Discesa scala avviata
[18:42:21] Narrating(interrupt:true)= Movimento bloccato sulla scala
[18:42:21] Narrating(interrupt:true)= Percorso ostruito, marcia arrestata
```

### Prova 2: Scala manuale / Climb Assistant (ore 18:42:37)
```text
[18:42:37] Narrating(interrupt:true)= Salita scala avviata
[18:42:37] Narrating(interrupt:true)= Navigazione verso Salita scala, distanza 4 metri, 4 passi
[18:42:39] Narrating(interrupt:true)= Raggiunto piano stabile
[18:42:39] Narrating(interrupt:true)= Arrivato a destinazione: Aria
```

### Prova 3: Target `casa torre belvedere` (ore 18:42:47)
```text
[18:42:47] [AutoWalk Telemetry] Target: BlockPos{x=-62, y=81, z=-40}, Dist: 4.9m, Budget: 5000, Pass1: NO_PATH (337 nodes), Pass2: FOUND (90 nodes), Fallback: ENABLED(5.0)
[18:42:48] Narrating(interrupt:true)= Discesa scala avviata
[18:42:48] Narrating(interrupt:true)= Navigazione verso casa torre belvedere, distanza 20 metri, 10 passi
[18:42:50] Narrating(interrupt:true)= Movimento bloccato sulla scala
[18:42:50] Narrating(interrupt:true)= Percorso ostruito, marcia arrestata
```

---

## 🔍 2. ANALISI TECNICA PRELIMINARE DI ANTIGRAVITY (POST 4° COLLAUDO)

Dall'ispezione combinata del codice eseguito nel 4° collaudo e dei log di gioco emergono tre fattori critici concatenati:

### Fattore A: Concorrenza distruttiva di `ProximityFallDetector.tick` (Linee 222-233)
In `ProximityFallDetector.java`:
1. `CentralFallSafetyManager` invoca `proximityDetector.tick(client, player, level, autoWalkActive, silenceFallVoice)` ad ogni tick del client.
2. In `ProximityFallDetector.tick`:
   ```java
   TraversalSafetyResult traversalResult = TraversalSafetyAnalyzer.analyzeTraversal(traversalContext);
   if (traversalResult.status() == TraversalSafetyStatus.SAFE_DESCENT_AVAILABLE && traversalResult.candidate() != null) {
       getMovementGuard().allowValidatedDescent(traversalResult.candidate().columnId());
       // ...
       return ProximityStatus.SAFE_DESCENT;
   }
   // CRITICO: se traversalResult non è SAFE_DESCENT_AVAILABLE (es. corridoio locale o moveDir non allineato):
   getMovementGuard().revokeValidatedDescent();
   DangerInfo danger = findDangerAhead(player, level, moveDir);
   if (danger != null) {
       return handleDangerDetected(player, danger.pos(), danger.depth(), ...); // -> ENGAGE FALL PROTECTION!
   }
   ```
3. Se `moveDir` è nullo (come accade in `GRAVITY_DESCENT`, dove non ci sono tasti orizzontali premuti):
   ```java
   if (moveDir == null) {
       getMovementGuard().revokeValidatedDescent();
       if (config.autoSneakOnEdge && isStandingOnDangerousEdge(player, level)) {
           getMovementGuard().engageFallProtection(); // Sneak forzato sul ciglio!
           return ProximityStatus.MECHANICAL_BRAKE_ZONE_2B;
       }
   }
   ```
4. **Evidenza**: `ProximityFallDetector` NON riconosce che `AutoWalkMotor` possiede una lease attiva (`hasActiveDescentLease()`). Di conseguenza, ad ogni tick sovrascrive lo stato con `revokeValidatedDescent()` o `engageFallProtection()`, reinserendo lo sneak di emergenza sul ciglio prima che il giocatore scivoli, bloccando la discesa gravitazionale!

### Fattore B: Dinamica dell'imbocco (Mount) e Gravità Vanilla
In `ClimbKinematics.java`:
- In discesa `TRANSIT`, la politica D10 prevede `keyUp = false`, `keyDown = false`, confidando nella sola gravità.
- Tuttavia, se il giocatore è a contatto con il bordo superiore o la parete e la velocità orizzontale/verticale è nulla (o frenata dallo sneak residuo), non cade lungo i pioli.
- Trascorsi 15 tick (0.75s) + 15 tick di retry (0.75s), il watchdog rileva $\Delta Y < 0.05\text{ m}$ ed emette `STUCK_ABORT` (esattamente i 2 secondi osservati nei log tra le 18:42:19 e le 18:42:21, e tra le 18:42:48 e le 18:42:50).

### Fattore C: Convergenza Necessaria tra Guida Manuale e Automatica
- Luca evidenzia la necessità di un'unica architettura unificata:
  Che si tratti di Climb Assistant (`Alt+S` / tasto use) o di navigazione A* (`AutoWalkMotor`), il transito lungo la colonna arrampicabile deve usare il **medesimo motore/routine**, con le stesse identiche garanzie di aggancio e discesa.

---

## 📋 3. CONSEGNE SPECIFICHE PER CODEX

1. **Riesame Critico e Approfondito**:
   Verifica l'interferenza tra `ProximityFallDetector` e la lease di `AutoWalkMotor`, e la reale dinamica di discesa su ladder in Minecraft 26.2 (gravità pura vs spinta `keyDown` controllata vs orientamento).
2. **Elaborazione della Strategia Correttiva Unificata**:
   Definisci la strategia più solida e coerente per far convergere Climb Assistant e AutoWalk verso la medesima routine di scalata/discesa.
3. **Redazione del Nuovo Piano Tecnico Correttivo Formale**:
   Aggiorna formalmente [`PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md) senza toccare codice.
4. **Relazione per Luca**:
   Fornisci a Luca un riepilogo lineare, sequenziale ad elenchi (stile NVDA), spiegando la strategia e attendendo la sua esplicita approvazione prima di procedere.

---

## 4. Appendice post-implementazione — Peer review, revisione e convalida Codex del 2026-09-12

### 4.1 Stato ricevuto

Antigravity aveva dichiarato completati D23..D29, 388 test verdi e deploy. Codex ha verificato che suite, build e hash installati erano reali, ma che l'implementazione non rispettava ancora la revisione normativa della Sezione 21 del piano:

- mancavano `ClimbContactProbe`, AABB precedente/swept e `VoxelShape` mondo;
- mancavano le fasi `ALIGN`, `APPROACH` e `CAPTURE_WAIT`;
- il fondo poteva essere riconosciuto dal solo `onGround`;
- il landing non richiedeva quattro campioni di supporto, velocità stabile e due tick consecutivi;
- i test Belvedere usavano ancora `(-60,81,-43)` invece del landing reale `(-60,81,-42)`.

Esito sul codice ricevuto: `NON CONVALIDATO — REVISIONE NECESSARIA`.

### 4.2 Attività Codex autorizzate da Luca

Codex ha applicato la revisione strutturale D23..D29, mantenendo un unico motore per AutoWalk e Climb Assistant. I dettagli normativi e implementativi sono registrati nelle Sezioni 21 e 22 del piano tecnico correttivo:

`docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md`

### 4.3 Evidenze finali

- 393 test effettivi nei report XML;
- 0 failure, 0 error, 0 skipped;
- build `shadowJar` riuscita;
- JAR distribuito in entrambe le istanze Minecraft Access;
- SHA-256 comune: `56284E5478719016DB25E9F4699D18038A3C98ECB8EDF5A3B5956D6D6B249217`;
- i nuovi probe, la cinematica e i relativi test hanno zero errori Checkstyle; il controllo globale resta non verde per violazioni storiche del repository, incluse 32 già presenti nella struttura di `AutoWalkMotor` e lasciate fuori da questa revisione mirata;
- JAR precedente conservato per rollback con SHA-256 `F05904D11D366FFF0D06A89A04F4950AC482A58FEEAD2CAED8512CEB81ED33AE`.

### 4.4 Direttive operative

1. Eseguire il sesto collaudo NVDA tetto della Torre Belvedere verso balconata.
2. Verificare che la telemetria riporti `ALIGN -> APPROACH -> CAPTURE_WAIT -> TRANSIT`, quindi `BOTTOM_CROSSING` e due campioni `SUPPORTED_LANDING` senza W sul landing.
3. Ripetere la stessa discesa con Climb Assistant `Alt+S`.
4. Verificare salita, botola e takeover manuale come regressioni complementari.
5. Non chiudere la revisione e non spuntare I8.7/I8.8 prima della conferma esplicita di Luca.

Stato: `CONVALIDATO TECNICAMENTE FINO A I8.6 — IN ATTESA DEL SESTO COLLAUDO IN-GAME`.

### 4.5 Nota operativa corrente

Codex ha completato gli aggiustamenti correttivi, la suite, la build e il deploy descritti nelle Sezioni 21–22 del piano:

`docs/piani/superati/MC-26.22/PIANO_TECNICO_CORRETTIVO_DISCESA_AUTOWALK.md`

Il `docs/report/REGISTRO_REVISIONI.md` è stato aggiornato con lo stato convalidato fino a I8.6 e con l'hash del JAR installato. Luca sta eseguendo il sesto collaudo in-game; I8.7 e I8.8 restano aperti fino all'esito della prova e alla sua convalida esplicita.

---

## 5. Esito del 6° Collaudo In-Game & Diagnosi Sintetica Antigravity (2026-09-12 ore 00:33..00:35)

### 5.1 Evidenze di Collaudo & Regressione Rilevata
Nel 6° collaudo condotto da Luca alla Torre Belvedere:
1. **Regressione Salita**: La scalata si avvia regolarmente ma si arresta esattamente prima del tetto con *"Movimento bloccato sulla scala"* e *"Percorso ostruito, marcia arrestata"*.
2. **Telemetria Reale (`latest.log` ore 00:35:23..00:35:25)**:
   - `Y=81.42`: `MOUNT -> TRANSIT` (aggancio avvenuto, salita in corso con `keyUp=true`).
   - `Y=83.88`: `TRANSIT -> DISMOUNT` (doppio cancello superato per quota e ultimo piolo, waypoint avanzato al tetto `Y=85`).
   - `Y=82.49` (15 tick dopo): `DISMOUNT -> DISMOUNT` emette `OUTSIDE_COLUMN_ABORT` con `STUCK_ABORT` (`velocityY=-0.225`, `keyUp=false`).

### 5.2 Causa Radice Isolata
- In `ClimbKinematics.java`, `evaluateDismount` è priva di consapevolezza direzionale (`snapshot.isAscent()`) ed è modellata solo per la discesa: assegna `keyUp = snapshot.landingResult().transferSafe()`.
- In `ClimbLandingProbe.java`, `transferSafe` esige che il supporto calpestabile sia alla quota corrente dei piedi entro 0.08 m (`Math.abs(world.maxY - feetY) <= 0.08`).
- In salita, i piedi sono ancora a `Y=83.88` mentre il pavimento del tetto è a `Y=85.0` (1.12 m più in alto): `transferSafe` fallisce, `keyUp` diventa `false`, il giocatore perde la spinta e scivola all'indietro per gravità fino al timeout del watchdog.
- I test unitari di Codex erano verdi perché simulavano il giocatore già a terra sul landing o fornivano un mock statico con `transferSafe=true`.

### 5.3 Valutazione Preventiva Discesa (AutoWalk, Climb Assistant & Manuale)
- **AutoWalk & Climb Assistant**: La sotto-fase `CAPTURE_WAIT` disattiva `keyUp` sul bordo del tetto prima dell'attraversamento verticale; l'attrito superficiale rischia di arrestare il personaggio sul ciglio causando stallo da watchdog.
- **Manuale (WASD)**: `TraversalSafetyAnalyzer` riconosce la scala come sicura con `W` premuto, ma il rilascio dei tasti sul bordo attiva il freno di emergenza `isStandingOnDangerousEdge` di `ProximityFallDetector`.

### 5.4 Documento di Approfondimento Strategico Master
Tutti i dettagli cinematici, i calcoli geometrici e la proposta correttiva sono formalizzati in:  
[`docs/strategie/archiviate/STRATEGIA_CORRETTIVA_DISMOUNT_SALITA_E_DISCESA.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/archiviate/STRATEGIA_CORRETTIVA_DISMOUNT_SALITA_E_DISCESA.md).

---

## 6. Implementazione Ingegneristica Contratti D30–D40 (2026-09-12 ore 02:35)

### 6.1 Sintesi delle Modifiche Applicate
1. **Contratto D31 & D32 (Aggancio Ascendente Robusto & Recuperi Finiti)**:
   - `ClimbKinematics.java`: `evaluateMount` esige aderenza fisica effettiva (`playerOnClimbable` o contatto geometrico verificato all'interno del corridoio XZ `inColumnCorridor`). Un salto a vuoto non promuove più a `TRANSIT`.
   - Conservazione dei `recoveryAttempts` tra `MOUNT` e `TRANSIT` (massimo 1 `REMOUNT_RETRY`); il reset dei tentativi avviene unicamente su superamento autentico di un piolo (`rungPassed`).
2. **Contratto D33 & D34 (Sbarco Ascendente Biforcato & Regione di Landing)**:
   - `ClimbKinematics.java`: `evaluateDismount` disaccoppia salita e discesa tramite `snapshot.isAscent()`.
   - In salita, distingue **Sollevamento Residuo** (`playerY < landingY - 0.20`: orientamento verso il supporto della scala, `keyUp=true` continuo, monitoraggio progresso verticale watchdog) e **Trasferimento** (`playerY >= landingY - 0.20`: orientamento verso il blocco calpestabile di sbarco `targetYaw`, avanzamento con `keyUp=true` se `destinationPracticable` o `transferSafe`).
   - Stabilizzazione: completamento `Outcome.COMPLETED` solo dopo 2 tick consecutivi stabili a terra sul landing (`landingResult.stable()`).
3. **Contratto D31 & D34 (`ClimbContactProbe` & `ClimbLandingProbe`)**:
   - `ClimbContactProbe`: delimitazione di `bottomCrossing` e `belowColumn` strettamente all'interno di `inColumnCorridor`, prevenendo falsi positivi da posizioni esterne alla colonna.
   - `ClimbLandingProbe`: aggiunto campo `destinationPracticable` con fallback sicuro (`false` nei costruttori di compatibilità), tolleranza velocità verticale `VELOCITY_EPSILON = 0.085` per gestire la fisica vanilla `Motion.y = -0.0784` a terra.
4. **Contratto D40 (Suite Test Headless)**:
   - 396 test automatici passati con successo (0 fallimenti, 0 errori, 0 skipped).

### 6.2 Dati di Compilazione e Deploy
- Build: `.\gradlew.bat --no-daemon --no-watch-fs shadowJar` completata con successo.
- JAR generato: `minecraft-access-26.2-1.19.0.SNAPSHOT.jar`
- SHA-256: `5F3479D81F05869EC80849748D1005634D889030F6805A6F079AD5930999BA01`
- Deploy eseguito in:
  1. `C:\Users\nemex\AppData\Roaming\PrismLauncher\instances\Minecraft 26.2 Access - Server Tenuta\minecraft\mods`
  2. `C:\Users\nemex\AppData\Roaming\PrismLauncher\instances\Minecraft 26.2 Access 1.12.0\minecraft\mods`

---

## 7. Rifinitura Atterraggio a Terra, Bonifica D41 & Chiusura Definitiva (2026-09-12 ore 03:00)

### 7.1 Esito del 7° Collaudo Live di Luca alla Torre Belvedere
- **Salita scala (Rotte 5)**: 100% successo.
  - Sequenza: `MOUNT -> TRANSIT -> DISMOUNT` con sollevamento residuo e transfer verso il tetto a $Y=85.00$.
  - Atterraggio stabile e annuncio vocale: *"Raggiunto piano stabile"*, *"Arrivato a destinazione: casa tetto torre belvedere"*.
- **Discesa scala (Rotta 7)**: 100% successo funzionale.
  - Sequenza d'ingresso: `ALIGN -> APPROACH -> CAPTURE_WAIT -> TRANSIT` via `SWEPT_CROSSING` senza alcun attrito o blocco sul bordo.
  - Transito verticale controllato fino al suolo del terrazzino/balconata a quota $Y=82.50$ con `onGround=true`.
  - Micro-anomalia terminale risolta: all'atterraggio a terra, `evaluateTransit` attendeva un dismount per corridoio stretto; con l'integrazione di `snapshot.playerOnGround()` sull'ultimo piolo in discesa (`isLastTransitRung()`), la transizione a `DISMOUNT` con `SUPPORTED_LANDING` scatta all'istante al contatto con il suolo.

### 7.2 Esecuzione Bonifica Codice Obsoleto (Contratto D41)
1. **Rimozione stato orfano `REACQUIRE`**:
   - Eliminato `REACQUIRE` dall'enum `AutoWalkMotor.ClimbSubPhase`.
   - Eliminato il ramo morto e non referenziato `evaluateReacquire()` in `ClimbKinematics.java`.
2. **Archiviazione Documentale**:
   - Spuntati al 100% tutti i checkpoint da P0 a V2 in [`PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md`](../piani/completati/PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md).
   - Piano tecnico spostato da `docs/piani/attivi/` a `docs/piani/completati/`.
   - Tre piani predecessori storici conservati in `docs/piani/superati/MC-26.22/`.
3. **Validazione Finale**:
   - Test suite: 100% verde (`BUILD SUCCESSFUL`).
   - JAR finale compilato e deployato: SHA-256 `B40CC3F3F87B94B14173B8DF89A138E3EA7879E47C16506ACB8523705D627823`.

