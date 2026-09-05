# Piano Tecnico Correttivo — Revisione 5D.5: Integrità della Discesa su Scale, Headroom di Step-Off e Pre-Clearance di Virata (ASTRALIS v2.6.3)

## 1. Identità, Stato e Perimetro

- **Data di Redazione**: 5 settembre 2026.
- **Autore**: Antigravity (Senior AI Pair Programmer).
- **Destinatari**: Luca (Senior Developer & Utente Finale) e GPT Codex (Copilota Ausiliario).
- **Framework Operativo**: ASTRALIS v2.6.3 — Specializzazione Minecraft Access.
- **Stato**: **COMPLETATO E CONVALIDATO A PIENI VOTI** — Tutte le sotto-fasi completate, test in-game di Luca superato e telemetria live verificata.
- **Incremento Versione Target (AVF)**: Rientra nella linea di sviluppo `1.12.0` (Fase 5D, revisione 5D.5). L'eventuale proposta di incremento AVF avverrà alla chiusura tecnica post-convalida.
- **Ambito Esclusivo**:
  1. Risoluzione dell'anomalia a metà rampa di scale: virata improvvisa verso la parete laterale/corridoio inferiore con incaglio frontale;
  2. Integrazione del controllo di headroom a quota testa al momento del passo di discesa in `AutoWalkPathfinder.isSafeDescent`;
  3. Definizione del vincolo di percorrenza continua su rampa di scale (`Stair Flight Constraint`), impedendo salti o cadute laterali nel vuoto dal bordo della scala;
  4. Validazione volumetrica pre-virata nel motore (`AutoWalkMotor`) prima di eseguire sterzate brusche contro pareti o stipiti a contatto;
  5. Armonizzazione di `FallDetector.isSafeWalkableStaircase` per evitare falsi allarmi di burrone sul pianerottolo finale in fondo alla rampa.
- **Esclusioni Tassative**:
  - Nessuna alterazione ai pesi o all'euristica di base dell'A*;
  - Nessuna modifica alle traduzioni I18N (`it_it.json`, `en_us.json`);
  - Nessuna modifica alle funzionalità della GUI o di configurazione Cloth Config.

---

## 2. Diagnosi Geometrica e Correlazione Telemetrica

### 2.1 Evidenze di Telemetria e Stato Reale del Salvataggio
Dall'analisi congiunta di `latest.log` (ore `11:40:41` - `11:40:44`) e dei dati NBT estratti dal mondo reale (`scuola di sopravvivenza mondo 2 (1)`):
- **Posizione di stallo**: `X = -58.502`, `Y = 68.000`, `Z = -36.700`, `Yaw = -179.82°` (orientato esattamente a Nord verso `-Z`).
- **Blocco di calpestio**: gradino superiore di `stone_brick_stairs[facing=west, half=bottom]` a quota `Y = 67`.
- **Rampa di scale**: si estende lungo l'asse X da Ovest (`X = -63`, primo piano, `Y = 70`) verso Est (`X = -57`, piano terra, `Y = 65`) sulle corsie `Z = -37` e `Z = -36`.
- **Parete Sud (`Z = -35`)**: muro continuo e pieno di `stone_bricks` da `Y = 65` a `Y = 71`.
- **Parete Nord / Corridoio Inferiore (`Z = -38`)**: a quota `X = -59`, sotto la rampa scorre il corridoio per la rimessa, con pavimento a quota `Y = 65` e soffitto pieno in `stone_bricks` a quota `Y = 69`.

### 2.2 I 4 Meccanismi del Difetto
1. **Omissione Headroom in `isSafeDescent`**:
   - `AutoWalkPathfinder.isSafeDescent` verifica solo che la colonna verticale dal punto di discesa verso il basso (`columnAir.below(y)`) sia libera.
   - Non controlla MAI `columnAir.above()`.
   - Di conseguenza, da `from = (-59, 68, -37)`, A* ha considerato transitabile una mossa di discesa con `drop = 2` verso `dropLanding = (-59, 66, -38)`.
   - Tuttavia, a `(-59, 69, -38)` il soffitto è in `stone_bricks` pieno: il giocatore, compiendo il passo orizzontale a quota piedi `Y = 68`, urta violentemente la testa a quota `Y = 69`.
2. **Scorciatoia Errata Scelta da A\***:
   - Poiché il varco fasullo a quota `Y = 68` veniva considerato aperto, A* ha preferito saltare dal fianco della scala a metà rampa per imboccare direttamente il corridoio della rimessa, anziché scendere l'intera scala fino al pianerottolo (`X = -57`).
3. **Falso Allarme `FallDetector`**:
   - Il dislivello di 3 blocchi verso il corridoio inferiore (e successivamente verso il pianerottolo) è stato classificato come burrone mortale (`profondità 3 blocchi`), innestando l'auto-sneak forzato e paralizzando l'avanzamento.
4. **Sterzata Cieca del Motore nel Muro**:
   - Il motore `AutoWalkMotor` ha iniziato la rotazione dello Yaw verso Nord (`-179.82°`), premendo `keyUp` contro il muro/soffitto senza verificare prima la clearance volumetrica della traiettoria di virata.

---

## 3. I 4 Contratti Vincolanti della Revisione 5D.5

### Contratto S1 — Controllo Obbligatorio di Headroom in Discesa (`isSafeDescent`)
- **Classe**: `AutoWalkPathfinder.java`
- **Specifica**:
  - Nel metodo `isSafeDescent(Level level, BlockPos from, BlockPos columnAir, BlockPos dropLanding, int dropDepth, boolean allowClosedDoors)`:
  - *Se* `dropDepth >= 1`, *allora* verificare prima di qualsiasi iterazione:
    ```java
    if (!isPassable(level, columnAir.above(), allowClosedDoors)) {
        return false;
    }
    ```
  - *Effetto*: garantisce che al momento dello scavalcamento orizzontale dal blocco di partenza `from` alla colonna di discesa `columnAir`, lo spazio a quota testa del giocatore (`columnAir.above()`, corrispondente a quota `from.getY() + 1`) sia completamente passabile e privo di soffitti bassi, architravi o blocchi solidi.

### Contratto S2 — Vincolo di Transito su Rampa di Scale (`Stair Flight Constraint`)
- **Classe**: `AutoWalkPathfinder.java`
- **Specifica**:
  - In `getValidNeighbors`:
  - Rilevare se il nodo di partenza poggia su un gradino di scale:
    ```java
    BlockState belowFromState = level.getBlockState(from.below());
    boolean isOnStairs = belowFromState.getBlock() instanceof StairBlock;
    ```
  - *Se* `isOnStairs == true`:
    1. Determinare la direzione longitudinale della rampa (es. `facing` dello `StairBlock`);
    2. Per tutte le mosse ortogonali e diagonali che comportano un dislivello verso il basso (`drop >= 1`), consentire la discesa **esclusivamente** se avviene nella direzione longitudinale di discesa della rampa (o verso il pianerottolo frontale);
    3. Scartare categorized tutte le mosse con `drop >= 1` perpendicolari all'asse della scala (salti laterali dal ciglio della scala nel vuoto).
  - *Effetto*: il giocatore scende le scale in modo rigorosamente continuo, naturale e ordinato fino a raggiungere il suolo pianeggiante in fondo alla rampa, esattamente come prescritto dalla direttiva di Luca.

### Contratto S3 — Validazione Volumetrica Pre-Virata nel Motore (`AutoWalkMotor`)
- **Classe**: `AutoWalkMotor.java`
- **Specifica**:
  - Prima di iniziare a ruotare lo Yaw verso un nuovo nodo target:
  - Calcolare la variazione angolare $|\Delta \text{yaw}|$ rispetto all'orientamento attuale.
  - *Se* $|\Delta \text{yaw}| > 45.0^\circ$:
    - Eseguire una scansione di clearance locale volumetrica `AutoWalkPathfinder.checkLocalClearance(level, player.position(), nodeCenter, player.getBoundingBox())`;
    - *Se* lo stato è `BLOCKED_BY_SOLID_JAMB` o collisione solida con una parete adiacente a quota testa/spalla:
      - Rilasciare la spinta in avanti (`keyUp.setDown(false)`);
      - Consentire l'allineamento sul posto fino a completamento o richiedere ricalcolo se l'avanzamento fisico è impossibile, evitando di spingere il giocatore contro il muro.

### Contratto S4 — Armonizzazione Pianerottolo e Soppressione Burrone (`FallDetector`)
- **Classe**: `FallDetector.java`
- **Specifica**:
  - Nel metodo `isSafeWalkableStaircase(Level level, BlockPos landingPos, int playerBaseY)`:
  - *Se* `landingPos` è un blocco solido normale (pavimento del pianerottolo), verificare se:
    1. Un blocco adiacente orizzontale a quota $Y+1$ è uno `StairBlock` orientato in discesa verso `landingPos`;
    2. Oppure la colonna verticale del giocatore si trova su un gradino di scale e il dislivello corrisponde alla naturale conclusione della rampa.
  - In tali condizioni, restituire `true` (discesa su scala sicura), azzerando la profondità di pericolo a 0.
  - *Effetto*: eliminazione totale dei falsi allarmi `"Attenzione: burrone 3 blocchi in basso"` quando si percorre l'ultimo tratto della rampa di scale verso il pavimento del pianerottolo.

---

## 4. Matrice di Simulazione a 3 Livelli (Protocollo 2 ASTRALIS)

### Livello 1 — Scenari Comuni (Happy Path)
1. **Discesa ordinaria da scala chiusa o aperta**:
   - Da primo piano (`X = -64`) verso piano terra / rimessa (`X = -77`): la rotta segue gradino per gradino fino al pianerottolo (`X = -57`), esce sul piano terra e imbocca il corridoio senza alcuna virata precoce.
2. **Salita ordinaria da piano terra a primo piano**:
   - Il vincolo di discesa non interferisce con la salita continua sui gradini (auto-step `0.60 m` e `isClimbableStep`).

### Livello 2 — Scenari Alternativi & Concorrenza
1. **Scale a chiocciola o con pianerottolo intermedio a L**:
   - Sul pianerottolo intermedio il blocco sottostante non è uno `StairBlock` ma un blocco pieno (`stone_bricks` / `planks` flat): il vincolo longitudinale si disattiva e la virata sul pianerottolo è pienamente consentita.
2. **Presenza di botola aperta a testa di rampa**:
   - La clearance a quota testa `columnAir.above()` riconosce la botola aperta come passabile (`isPassable == true`), consentendo la discesa senza blocchi.

### Livello 3 — Casi Limite (Corner Cases & Limiti Geometrici)
1. **Scala sospesa nel vuoto (ponte/rampa aerea senza ringhiere)**:
   - Il vincolo S2 impedisce ad A* di pianificare cadute letali dai lati del ponte di scale, costringendo la rotta a rimanere al centro della rampa fino all'estremità opposta.
2. **Architrave basso su rampa di scale (soffitto degradante $\le 1.8\text{ m}$)**:
   - Il Contratto S1 intercetta tempestivamente qualsiasi tentativo di uscire lateralmente o scendere sotto architravi a collisione garantita, scartando i rami non transitabili.

---

## 5. Piano dei Test Automatici e Verifiche

1. **Test Pathfinder (Unitari & Geometrici)**:
   - `AutoWalkPathfinderTest.testSafeDescentRequiresHeadroomClearance`: verifica che `isSafeDescent` scarti mosse con `columnAir.above()` solido;
   - `AutoWalkPathfinderTest.testStairFlightDisallowsLateralDrop`: verifica che da una cella su `StairBlock` non vengano generate mosse di discesa laterale perpendicolari;
   - `AutoWalkPathfinderTest.testStaircaseDescentToRimessaFullRoute`: simulazione completa del percorso dal primo piano alla rimessa, verificando che tutti i nodi scendano fino al pianerottolo prima di curvare.
2. **Test FallDetector**:
   - `FallDetectorTest.testStairLandingNotReportedAsCliff`: verifica che il pavimento del pianerottolo in fondo alla scala non attivi l'allarme burrone né l'auto-sneak.
3. **Test Motore & Pre-Clearance**:
   - `AutoWalkMotorTest.testTurnClearancePreventsWallCollision`: verifica che virate $> 45^\circ$ contro pareti solide a contatto arrestino la spinta prima della collisione.
4. **Esecuzione Suite Completa**:
   - `.\gradlew.bat --no-daemon --no-watch-fs test` (target 100% verdi, $\ge 282$ test).

---

## 6. Sequenza Operativa

- [x] Sotto-Fase 1A: Redazione del Piano Tecnico Formale e deposito in `docs/piani/attivi/`.
- [x] **STOP OBBLIGATORIO (Regola 0 ASTRALIS)**: Attesa autorizzazione esplicita di Luca.
- [x] Sotto-Fase 1B: Implementazione Contratti S1, S2, S3, S4 nei rispettivi file sorgente Java.
- [x] Sotto-Fase 1B: Esecuzione suite di test automatica e compilazione `shadowJar`.
- [x] Fase 2: Deploy automatico nelle istanze PrismLauncher di Luca per collaudo manuale.
- [x] Fase 3: Convalida positiva in-game di Luca e chiusura.

---

## 7. Punto di Arresto — Regola 0 (Default Consultivo Permanente)

Questo piano costituisce la proposta tecnica per la Sotto-Fase 1A. In osservanza della **Regola 0**, nessun file sorgente, classe Java o file di configurazione è stato modificato o verrà modificato senza l'esplicito comando di Luca (*"procedi"*, *"applica"*, *"esegui"*).
