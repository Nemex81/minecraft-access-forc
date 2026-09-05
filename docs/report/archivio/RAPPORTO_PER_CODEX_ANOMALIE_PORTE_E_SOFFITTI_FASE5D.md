# Rapporto Tecnico per GPT Codex — Anomalie Porte Chiuse e Soffitti Bassi (Fase 5D)

**Destinatario:** GPT Codex / ChatGPT (Copilota Ausiliario e Peer Programmer)  
**Mittente:** Antigravity (Senior AI Pair Programmer) & Luca (Sviluppatore Senior Non Vedente)  
**Framework di Riferimento:** ASTRALIS v2.6.3  
**Data:** 4 Settembre 2026  
**Stato:** Consultivo — Richiesta di Analisi, Verifica e Convalida della Soluzione Sistemica  

---

## 1. Scopo del Documento

Questo rapporto ha lo scopo di sottoporre a GPT Codex una revisione tecnica indipendente su un comportamento anomalo emerso durante il collaudo in-game della **Sotto-Fase 5D** della mod **Minecraft Access** (Minecraft 26.2, Fabric/NeoForge, Java 25).

In particolare, il documento illustra:
1. Il problema percettivo iniziale (pitch inclinato verso il pavimento davanti alle porte) e la micro-correzione applicata;
2. I nuovi comportamenti anomali e bloccanti (punti residuali) rilevati da Luca tramite telemetria in-game e analisi del mondo salvato;
3. La diagnosi tecnica delle cause radice nei componenti [`AutoWalkMotor`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java) e [`AutoWalkPathfinder`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkPathfinder.java);
4. La proposta di soluzione architetturale formulata da Antigravity, articolata su tre assi correttivi;
5. La richiesta formale a GPT Codex di valutare se tale soluzione sia la più efficace, strutturale, sistemica, coerente, compatibile e completa.

---

## 2. Il Problema Iniziale e la Risoluzione Applicata

### 2.1 Descrizione del Problema Iniziale
Durante la prima sessione di collaudo della Sotto-Fase 5D, Luca ha notato che all'arrivo a destinazione o quando il navigatore si arrestava davanti a un ostacolo interattivo come una porta chiusa, lo sguardo del personaggio rimaneva inclinato verso il basso (pitch compreso tra 38° e 56° verso il pavimento).  
Nonostante la sintesi vocale annunciasse correttamente la presenza della porta di fronte, il mirino puntava verso terra. Per un giocatore non vedente che fa affidamento sul raycast al centro del mirino, questo disallineamento impediva l'aggancio diretto e l'interazione immediata con il blocco porta (tasto destro per aprire) senza dover prima livellare manualmente la testa con i comandi ausiliari (tasto `M` o `5` del tastierino).

### 2.2 Come è Stato Risolto
L'intervento è stato applicato chirurgicamente in [`AutoWalkMotor.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java):
1. Nel blocco di intercettazione porta chiusa (linee 243–245), l'orientamento dello sguardo verso il blocco della porta è stato disaccoppiato in altezza: il punto di ancoraggio dello sguardo utilizza `player.getEyeY()` e forza esplicitamente `player.setXRot(0.0f)` (Sguardo Livellato / Dritto sull'orizzonte), mantenendo il calcolo dello Yaw per centrare orizzontalmente la porta.
2. Lo stesso principio di pitch livellato a 0.0° è stato applicato nel metodo ausiliario `lookAtTarget` (linee 404–421) per la visuale orientata verso `BlockPos`, `BlockPos3d` e `Waypoint`.
3. È stato aggiunto il test di regressione `testLookAtTargetLevelsPitchOnWaypointsAndBlocks` in [`AutoWalkMotorTest.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/test/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotorTest.java).
4. La suite completa di test automatici è passata a **255/255 test verdi**.
5. Il JAR compilato (`minecraft-access-1.12.0-SNAPSHOT.jar`) è stato distribuito nell'istanza attiva di PrismLauncher.

### 2.3 Esito del Collaudo sul Pitch
Il collaudo sul campo ha confermato che il problema del pitch è completamente risolto: in tutti i punti di sosta, NVDA vocalizza regolarmente `Dritto` (pitch a 0° esatti), consentendo al giocatore di trovarsi immediatamente con il mirino allineato sulla porta. Luca ha confermato: *"il grosso sembra risolto"*.

---

## 3. I Punti Residuali Emersi dal Collaudo In-Game

Durante la prosecuzione del collaudo in-game (sessione registrata nei log tra le 20:00 e le 20:05), sono emersi due comportamenti anomali e invalidanti per l'esperienza di gioco.

### 3.1 Punto Residuo 1: Deadlock a Tick 0 su Porta Chiusa all'Avvio dell'AutoWalk
- **Scenario Operativo**:
  - Luca si trovava all'esterno, fermo a circa 1 metro di fronte alla porta d'ingresso chiusa di `casa padronale porta d'ingresso` (coordinate waypoint: $X = -53, Y = 65, Z = -36$).
  - Dal menu POI/Waypoint, Luca seleziona come destinazione `casa porta rimessa attrezzi` (coordinate waypoint: $X = -77, Y = 64, Z = -43$), distante 27 metri (26 passi) alle sue spalle, sul lato opposto della tenuta.
  - Luca attiva l'AutoWalk.
- **Comportamento Riscontrato nei Log (`latest.log`)**:
  ```text
  [20:01:22] Navigazione verso casa porta rimessa attrezzi, distanza 27 metri, 26 passi
  [20:01:22] Porta chiusa davanti a te. Premi Tasto Destro per aprire
  ...
  [20:01:32] Navigazione verso casa porta rimessa attrezzi, distanza 27 metri, 26 passi
  [20:01:32] Porta chiusa davanti a te. Premi Tasto Destro per aprire
  ...
  [20:01:55] Navigazione verso casa porta rimessa attrezzi, distanza 27 metri, 26 passi
  [20:01:55] Porta chiusa davanti a te. Premi Tasto Destro per aprire
  ...
  [20:02:35] Navigazione verso casa porta rimessa attrezzi, distanza 27 metri, 26 passi
  [20:02:35] Porta chiusa davanti a te. Premi Tasto Destro per aprire
  ...
  [20:02:50] Navigazione verso casa porta rimessa attrezzi, distanza 28 metri, 27 passi
  [20:02:50] Porta chiusa davanti a te. Premi Tasto Destro per aprire
  ```
- **Sintomo**:
  Il bot si arresta **istantaneamente al tick 0**, gira la testa del giocatore verso la porta chiusa e si rifiuta di iniziare la marcia, intimando all'utente di aprire la porta.
  Luca si ritrova intrappolato in un loop senza uscita: non desidera entrare nella casa padronale (vuole andare all'aperto verso la rimessa), ma il bot impedisce qualsiasi movimento se non viene aperta quella specifica porta.

### 3.2 Punto Residuo 2: Collisione e Incaglio su Scale a Soffitto Basso durante il Disimpegno Manuale
- **Scenario Operativo**:
  - Per aggirare l'impasse della porta bloccata, Luca si disimpegna muovendosi manualmente con i comandi di tastiera verso una rampa di scale in mattoni di pietra (`Scalini di mattoni di pietra`).
  - Una volta raggiunta la rampa, Luca riattiva l'AutoWalk verso la rimessa.
- **Comportamento Riscontrato nei Log (`latest.log`)**:
  ```text
  [20:03:17] Navigazione verso casa porta rimessa attrezzi, distanza 25 metri, 25 passi
  [20:03:19] Ancora 20 passi
  [20:03:24] Davanti: Salita su Scalini di mattoni di pietra, soffitto basso, a 2 blocchi
  [20:03:25] Davanti: Ostacolo in alto di Mattoni di pietra, a 1 blocco
  [20:03:25] Navigazione automatica annullata
  ```
- **Sintomo**:
  La marcia inizia regolarmente ma, salendo la rampa di scale, il giocatore sbatte la testa contro i mattoni del soffitto perché lo spazio verticale non consente il salto, provocando l'incaglio fisico e costringendo Luca ad annullare la navigazione.

---

## 4. Analisi Tecnica delle Cause Radice

### 4.1 Causa Radice del Deadlock sulla Porta Chiusa (Punto 1)
L'indagine nel codice sorgente ha isolato due fattori concorrenti:

1. **Assenza di Penalità di Costo per le Porte Chiuse in A***:
   In [`AutoWalkPathfinder.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkPathfinder.java#L352-L364):
   ```java
   public static boolean isPassable(Level level, BlockPos pos) {
       if (level == null || isHazard(level, pos)) return false;
       BlockState state = level.getBlockState(pos);
       if (isDoorOrGate(state)) return true;
       return state.getCollisionShape(level, pos).isEmpty();
   }
   ```
   Tutte le porte (aperte o chiuse) sono considerate `isPassable`. Nel metodo `calculateStepCost` (linee 434–453), il passaggio attraverso una porta chiusa ha il costo standard di un blocco vuoto (`1.0` o `1.414`).  
   Poiché la rimessa attrezzi si trova a ovest dell'edificio e la porta d'ingresso a est, tagliare in linea retta attraverso le stanze interne della casa padronale (passando per la porta chiusa d'ingresso) richiede solo **26 passi**, mentre circumnavigare l'intero perimetro esterno dell'edificio richiederebbe 40–50 passi.  
   L'A* seleziona quindi come percorso ottimale quello che attraversa l'interno dell'edificio, e assegna come primissimo nodo della rotta proprio il blocco della porta chiusa.

2. **Controllo Bloccante Rigido al Tick 0 in AutoWalkMotor**:
   In [`AutoWalkMotor.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java#L238-L254):
   ```java
   if (isDoorOrGateClosed(level, doorCheckPos)) {
       double distToDoorSq = player.blockPosition().distSqr(doorCheckPos);
       if (distToDoorSq <= 4.5) { // Entro 2.1 blocchi da porta chiusa
           client.options.keyUp.setDown(false);
           player.setSprinting(false);
           Vec3 doorCenter = Vec3.atCenterOf(doorCheckPos);
           player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(doorCenter.x, player.getEyeY(), doorCenter.z));
           player.setXRot(0.0f);
           ...
           return; // Attende che la porta venga aperta
       }
   }
   ```
   Trovandosi a meno di 2.1 blocchi dal primo nodo (`targetNodePos`), il motore arresta la marcia al primo tick. Il sistema non offre alcuna via di uscita automatica: se l'utente non desidera aprire quella porta, l'AutoWalk non può partire.

---

### 4.2 Causa Radice dell'Incaglio su Scale a Soffitto Basso (Punto 2)
In [`AutoWalkPathfinder.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkPathfinder.java#L366-L380):
```java
public static boolean isClimbableStep(Level level, BlockPos from, BlockPos stepFoot, BlockPos targetStep) {
    if (level == null || isHazard(level, stepFoot) || isHazard(level, targetStep) || isHazard(level, targetStep.above())) {
        return false;
    }

    // Headroom directly above player (from.above(2)) must be completely clear to jump
    if (isSolid(level, from.above(2))) {
        return false;
    }

    // Clearance at the step landing: targetStep (feet) and targetStep.above() (head) and targetStep.above(2) (jump peak headroom)
    if (!isPassable(level, targetStep) || !isPassable(level, targetStep.above())) {
        return false;
    }
```
Nonostante il commento dichiari la necessità di verificare `targetStep.above(2)` per il picco della traiettoria di salto, la condizione reale controlla unicamente `targetStep` (piedi) e `targetStep.above()` (testa).  
Quando un gradino è sovrastato da un soffitto ribassato a quota $Y+2$ rispetto al gradino di atterraggio, il giocatore che tenta di saltare sbatte la testa e non completa lo step-up. L'A* non scarta questi gradini, provocando l'incaglio.

---

## 5. La Soluzione Sistemica Proposta da Antigravity

Per eliminare questi problemi alla radice senza compromettere la capacità dell'AutoWalk di navigare all'interno degli edifici quando espressamente richiesto, Antigravity propone una soluzione strutturata su **tre assi complementari**:

### Asse 1: Penalità Differenziale per Porte Chiuse in A* (`CLOSED_DOOR_PENALTY`)
- **Meccanismo**: Nel metodo `calculateStepCost`, verificare se il nodo di arrivo del passo contiene una porta/cancello/botola chiusa (`AutoWalkMotor.isDoorOrGateClosed(level, move.targetPos)`).
- **Valore di Penalità Proposto**: Assegnare una penalità di costo elevata (ad esempio `+25.0` o `+30.0` passi equivalenti).
- **Comportamento Atteso**:
  - *Se esiste un percorso alternativo all'aperto*: L'A* preferirà percorrere 30–40 passi all'aperto piuttosto che 26 passi che richiedono di attraversare una o più porte chiuse. Il bot circumnavigherà la casa senza impantanarsi sulla porta d'ingresso.
  - *Se la destinazione è interna all'edificio (o unica via possibile)*: La penalità non impedirà all'A* di trovare la rotta attraverso la porta chiusa se non esistono alternative a costo inferiore. L'accesso a stanze chiuse rimane garantito al 100%.

### Asse 2: Verifica Rigorosa dell'Headroom di Salto (`isClimbableStep`)
- **Meccanismo**: Nel metodo `isClimbableStep` di `AutoWalkPathfinder`, estendere i controlli di passabilità:
  1. `isPassable(level, targetStep.above(2))` deve essere verificato per garantire che l'atterraggio sul gradino disponga di 2 blocchi d'aria completi sopra la pedata;
  2. `isPassable(level, from.above(2))` deve essere verificato per consentire lo stacco del salto senza colpire il soffitto.
- **Comportamento Atteso**:
  - Scale o gradini incassati in cunicoli alti solo 2 blocchi totali (soffitto basso) vengono scartati dall'A*. Il bot utilizzerà solo scale e varchi con altezza utile di salto ($\ge 3$ blocchi dal blocco di partenza).

### Asse 3: Gestione Resiliente delle Porte Chiuse in Partenza (`AutoWalkMotor`)
- **Meccanismo**:
  - Se all'avvio della navigazione (tick 0) il giocatore si trova già entro la distanza di arresto (`distToDoorSq <= 4.5`) da una porta chiusa che coincide con il primo nodo della rotta:
    - Verificare se la porta è orientata nella direzione verso cui il giocatore intende allontanarsi o se la rotta può ricalcolare escludendo temporaneamente il blocco della porta chiusa (`repathExcluding(doorPos)`).
    - Se l'utente impartisce il comando di annullamento e poi rilancia la navigazione dalla stessa posizione, oppure se il bersaglio finale si trova all'esterno, privilegiare la rotta che non attraversa porte adiacenti chiuse.

---

## 6. Domande e Richieste di Verifica per GPT Codex

Chiediamo a GPT Codex di analizzare formalmente la diagnosi e la soluzione proposta, fornendo il proprio verdetto tecnico sui seguenti punti chiave:

1. **Efficacia e Architettura del Costo A* (Asse 1)**:
   - Ritenete che l'aggiunta di una costante `CLOSED_DOOR_PENALTY` (es. `25.0`) in `calculateStepCost` sia la modalità più pulita e canonica per scoraggiare l'attraversamento di porte chiuse senza degradare le performance di esplorazione dell'open set?
   - Quale valore numerico di penalità considerate ottimale per bilanciare il costo di circumnavigazione di un edificio tipico (20–40 blocchi) rispetto all'attraversamento di porte interne?
   - È opportuno verificare sia il blocco piedi (`targetPos`) sia il blocco testa (`targetPos.above()`) per l'eventuale presenza della parte superiore della porta?

2. **Accuratezza Geometrica dell'Headroom per Scale (Asse 2)**:
   - L'aggiunta di `!isPassable(level, targetStep.above(2))` in `isClimbableStep` previene completamente il fenomeno del "soffitto basso" riscontrato da Luca?
   - Esistono casi limite in Minecraft (es. scalini con lastre/slab superiori, scale a chiocciola o botole aperte) in cui richiedere clearance completa a quota `above(2)` potrebbe scartare falsi positivi (scale che il giocatore normovedente o con auto-jump salirebbe regolarmente)?

3. **Prevenzione del Deadlock all'Avvio (Asse 3)**:
   - Qual è il pattern raccomandato per gestire l'avvio della navigazione quando il giocatore si trova a ridosso di una porta chiusa?
   - È preferibile che la penalità A* (Asse 1) da sola guidi il percorso lontano dalla porta, oppure è necessaria una logica esplicita in `AutoWalkMotor` che eviti lo stop immediato al tick 0 se il vettore di moto iniziale si allontana dalla porta?

4. **Verdetto Complessivo sui 7 Assi di Qualità ASTRALIS**:
   - Viene concessa la convalida per procedere alla stesura del piano formale e alla successiva implementazione?

---

*In attesa dell'analisi e delle indicazioni di GPT Codex prima di procedere con qualsiasi modifica al codice sorgente.*
