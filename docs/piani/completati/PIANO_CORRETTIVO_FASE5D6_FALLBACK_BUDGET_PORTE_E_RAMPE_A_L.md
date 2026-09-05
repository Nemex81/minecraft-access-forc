# Piano Tecnico Correttivo — Revisione 5D.6: Riforma Fallback A* su Budget Esaurito, Rampe a L e Disostruzione Nodi Torre-Solaio (ASTRALIS v2.6.3)

## 1. Identità, Stato e Perimetro

- **Data di Redazione**: 5 settembre 2026.
- **Autore**: Antigravity (Senior AI Pair Programmer).
- **Destinatari**: Luca (Senior Developer & Utente Finale) e GPT Codex (Copilota Ausiliario).
- **Stato**: ASSORBITO E COMPLETATO NELLA REVISIONE 5D.7 (CONVALIDATO AL 100% DA LUCA IN-GAME — ARCHIVIATO).
- **Ambito Esclusivo**:
  1. Risoluzione del fallimento di navigazione da Torre Belvedere verso qualsiasi altro punto di interesse della tenuta (`casa padronale porta d'ingresso`, `casa porta rimessa attrezzi`, `casa porta primo piano`, `cas ingresso solaio`);
  2. Riforma della politica di gestione di `SEARCH_BUDGET_EXHAUSTED` nel Two-Pass Pathfinding (`AutoWalkPathfinder.findPath`), autorizzando il Passaggio 2 (Fallback con `allowClosedDoors = true`) quando la meta si trova oltre porte chiuse entro il raggio operativo;
  3. Armonizzazione geometrica per rampe di scale a **L** con pianerottolo intermedio ad angolo e presenza di scala a pioli a parete (`ladder`) priva di collisione;
  4. Ottimizzazione del bilanciamento euristico per la discesa multi-piano evitando la dispersione dei nodi sulle terrazze e coperture superiori;
  5. Test unitari e di integrazione dedicati alla navigazione multi-piano con scale a L e varchi chiusi.
- **Esclusioni Tassative**:
  - Nessuna alterazione al sistema di movimento continuo o alla velocità del motore (`AutoWalkMotor`);
  - Nessuna modifica alle traduzioni I18N (`it_it.json`, `en_us.json`);
  - Nessuna modifica alla GUI dell'Access Menu o alle opzioni Cloth Config.

---

## 2. Diagnosi Geometrica e Correlazione Telemetrica

### 2.1 Evidenze di Telemetria e Dati NBT Reali
Dall'analisi congiunta di `latest.log` (ore `12:17:35` - `12:18:05`), delle coordinate NBT del giocatore (`e48e6275-dac3-40de-8d53-17ec4b51515e.dat`) e della scansione della regione voxel (`r.-1.-1.mca`):
- **Posizione del giocatore**: $X = -61.485$, $Y = 81.000$, $Z = -38.627$, Yaw = $180^\circ$ (Sud), `BlockPos(-62, 81, -39)`.
- **Waypoint salvati**:
  - `casa torre belvedere`: $X = -62, Y = 81, Z = -40$ (all'interno della stanza della torre).
  - `cas ingresso solaio`: $X = -64, Y = 75, Z = -36$ (esattamente sul blocco della porta chiusa in quercia scura).
  - `casa porta primo piano`: $X = -64, Y = 70, Z = -36$.
  - `casa padronale porta d'ingresso`: $X = -53, Y = 65, Z = -36$.
  - `casa porta rimessa attrezzi`: $X = -77, Y = 64, Z = -43$.
- **Comportamento Rilevato**:
  - Per qualsiasi waypoint tracciato dalla torre, il tentativo di avviare l'AutoWalk restituisce immediatamente l'evento `AUTOWALK_NO_PATH`: *"Nessun percorso sicuro trovato per %s"*.

### 2.2 I 2 Meccanismi del Difetto

1. **Il Cortocircuito di Passaggio 1 su Budget Esaurito (Causa Primaria)**:
   - In `AutoWalkPathfinder.findPath`:
     - Linea 131: esecuzione del Passaggio 1 con `allowClosedDoors = false`.
     - Tutte le mete indicate (`casa padronale porta d'ingresso`, `casa porta rimessa attrezzi`, `casa porta primo piano`) si trovano dietro porte chiuse.
     - L'A* del Passaggio 1 non può attraversare alcuna porta chiusa. Tuttavia, la torre non è un vano stagno isolato dal mondo: verso Nord e verso Est si apre sulla terrazza della torre e sulle coperture della villa.
     - L'algoritmo espande nodi all'esterno cercando una via alternativa aperta che non esiste, consumando l'intero budget di sicurezza di $2500$ nodi (`MAX_EXPLORED_NODES`).
     - Al raggiungimento di 2500 nodi, `computeAStar` restituisce `SEARCH_BUDGET_EXHAUSTED`.
     - Le linee 135–138 stabiliscono:
       ```java
       if (strictResult.status() == PathStatus.SEARCH_BUDGET_EXHAUSTED) {
           return strictResult;
       }
       ```
     - Poiché il metodo ritorna subito `strictResult`, il **Passaggio 2 (Fallback con `allowClosedDoors = true`) NON VIENE MAI ESEGUITO**.
     - Di conseguenza, qualsiasi meta protetta da porte chiuse situata in una struttura articolata viene dichiarata fallita al 100%.

2. **La Rampa a L e l'Allontanamento Euristico Temporaneo per `cas ingresso solaio` (Causa Secondaria)**:
   - Per raggiungere `cas ingresso solaio` ($X = -64, Y = 75, Z = -36$):
     - Dalla Torre ($X = -62, Y = 81, Z = -39$), la scala scende a Sud (Rampa 1) fino a $Z = -35$ ($Y = 79$).
     - La cella d'angolo $(-62, 79, -35)$ ha pavimento solido in mattoni di pietra e una scala a pioli (`ladder`) sulla parete Ovest.
     - Per proseguire la discesa verso il solaio, il percorso deve svoltare a Est a $90^\circ$ sulla Rampa 2 ($X = -61 \to -57$).
     - Questa rampa si muove verso Est (da $X = -61$ a $X = -57$), allontanandosi temporaneamente dalla coordinata $X = -64$ del solaio.
     - L'euristica euclidea 3D dell'A* penalizza i nodi che si muovono verso Est perché si allontanano da $X = -64$, preferendo espandere nodi che rimangono a quota $Y = 81$ sulla terrazza più vicini in pianta al solaio.
     - Questa dispersione sulla terrazza satura rapidamente il budget, innescando nuovamente il cortocircuito di linea 135.

---

## 3. I 4 Contratti Vincolanti della Revisione 5D.6

### Contratto B1 — Riforma dell'Accesso al Passaggio 2 su Esaurimento Budget (`Budget Fallback Authorization`)
- **Classe**: `AutoWalkPathfinder.java`
- **Specifica**:
  - In `findPath(Level level, Vec3 startVec, Object rawTarget, int maxRange, int maxExploredNodes)`:
  - Sostituire il blocco di aborto incondizionato alle linee 135–138 con una valutazione di idoneità al fallback:
    - *Se* `strictResult.status() == PathStatus.SEARCH_BUDGET_EXHAUSTED`:
      - Verificare se il raggio diretto `directDist <= maxRange`.
      - Verificare se la meta (`rawTargetPos`) o le celle adiacenti di accesso al varco contengono una porta, un cancello o una botola chiusa (`isDoorOrGateClosed`).
      - *Se* la meta è potenzialmente dietro una porta chiusa (oppure la distanza diretta è compatibile con una struttura interna), **non abortire**: procedere al Passaggio 2 (`allowClosedDoors = true`).
      - *Se* invece la meta si trova in aperta campagna (senza alcuna porta nel raggio e a distanza elevata), mantenere il ritorno di `SEARCH_BUDGET_EXHAUSTED` per evitare esplorazioni computazionalmente insostenibili.
  - *Effetto*: garantisce che quando Luca seleziona punti all'interno della residenza o della tenuta, l'esaurimento del budget in modalità stretta consenta al Passaggio 2 di calcolare la rotta ponderata attraverso le porte chiuse.

### Contratto B2 — Armonizzazione Pianerottolo ad Angolo con Scala a Pioli (`L-Stair & Ladder Landing`)
- **Classe**: `AutoWalkPathfinder.java`
- **Specifica**:
  - Nel metodo `isLateralStairDrop(Level level, BlockPos from, BlockPos to, int drop)`:
    - Verificare che il vincolo longitudinale si applichi unicamente quando il blocco di appoggio è effettivamente uno `StairBlock`.
    - Sul pianerottolo intermedio d'angolo `(-62, 79, -35)`, il blocco inferiore è `stone_bricks` (blocco piano) e la cella contiene una `ladder` a parete: verificare esplicitamente che la mossa ortogonale di svolta a $90^\circ$ verso Est (`to = (-61, 79, -35)`) sia classificata come movimento flat orizzontale valido (`drop = 0`) senza alcuna interferenza della scala a pioli.
  - In `isPassable` e `isSafeDescent`:
    - Riconfermare che la `ladder` (che in Minecraft ha forma di collisione vuota) sia considerata pienamente passabile e che non generi rigetti di discesa sul pianerottolo.

### Contratto B3 — Bilanciamento dell'Euristica 3D per Rampe a L (`3D Heuristic Vertical Balance`)
- **Classe**: `AutoWalkPathfinder.java`
- **Specifica**:
  - Nel metodo `calculateHeuristic(BlockPos a, BlockPos b)`:
    - Attualmente: `dy = (a.getY() - b.getY()) * 1.5`.
    - Calibrare il peso del dislivello verticale in modo che i passi che scendono effettivamente verso la quota del bersaglio ($Y_{dest} < Y_{cur}$) ricevano un incentivo euristico consistente rispetto ai nodi che vagano in piano alla stessa quota ($Y = 81$) lontano dalle scale.
    - Introdurre un fattore di convergenza di quota: quando esiste un dislivello verticale significativo ($|\Delta Y| \ge 4$), l'euristica premia i nodi che riducono il delta verticale, favorendo l'imbocco della rampa a L anche se il primo tratto gira temporaneamente in direzione ortogonale.

### Contratto B4 — Suite di Test e Verifiche Multi-Piano Headless
- **Classe**: `AutoWalkPathfinderTest.java`
- **Specifica**:
  - Aggiungere i seguenti scenari di collaudo automatico:
    1. `testBudgetExhaustionAllowsFallbackWhenDoorBlocked`: verifica che un'area aperta vasta con porta chiusa finale passi al Passaggio 2 anziché abortire su esaurimento budget;
    2. `testLShapedStaircaseWithLadderLanding`: verifica la discesa ordinata lungo una scala a L con pianerottolo d'angolo provvisto di scala a pioli;
    3. `testBelvedereToSolaioMansionRoute`: test di regressione end-to-end con la geometria reale della Torre Belvedere fino al Solaio, convalidando il percorso a 17 nodi.

---

## 4. Matrice di Simulazione a 3 Livelli (Protocollo 2 ASTRALIS)

### Livello 1 — Scenari Comuni (Happy Path)
1. **Navigazione da Torre Belvedere a Solaio**:
   - Dalla quota $Y=81$, A* segue la Rampa 1 verso Sud ($Y=79$), attraversa il pianerottolo d'angolo a $90^\circ$, scende la Rampa 2 verso Est fino a $Y=75$, percorre il corridoio a Ovest e raggiunge la porta del solaio a $(-64, 75, -36)$.
2. **Navigazione da Torre Belvedere a Piano Terra / Rimessa**:
   - Il Passaggio 1 esaurisce il budget cercando vie aperte; scatta il Passaggio 2 autorizzato dal Contratto B1; la rotta seleziona la porta chiusa ottimale con penalità $+30.0$ e guida il giocatore direttamente a destinazione.

### Livello 2 — Scenari Alternativi & Concorrenza
1. **Porta del Solaio già Aperta vs Chiusa**:
   - *Se* la porta del solaio è aperta: il Passaggio 1 la attraversa al costo base ($1.0$) senza consumare il budget.
   - *Se* la porta è chiusa: il Passaggio 1 seleziona la cella di stazionamento antistante nel corridoio come meta valida; in caso di esaurimento budget, il Passaggio 2 trova la rotta continua.
2. **Presenza di Giocatore sul Bordo Scala**:
   - Nessun falso allarme del FallDetector e nessun incaglio laterale grazie ai contratti S1-S4 di Fase 5D.5 già collaudati con successo.

### Livello 3 — Casi Limite (Corner Cases & Limiti Geometrici)
1. **Meta Veramente Impossibile (Isola Sospesa a 100 blocchi nel vuoto senza porte)**:
   - Poiché non vi sono porte chiuse associate alla meta e la distanza eccede lo spazio indoor, il pathfinder restituisce correttamente `SEARCH_BUDGET_EXHAUSTED` o `NO_PATH` senza sprecare cicli di CPU in un inutile Passaggio 2.
2. **Scala a Pioli che Interseca la Visuale**:
   - La presenza della scala a pioli sulla parete del pianerottolo d'angolo non interferisce con la bounding box del giocatore né arresta l'avanzamento motorio.

---

## 5. Piano dei Test Automatici e Verifiche

1. **Test Pathfinder (Unitari & Geometrici)**:
   - Esecuzione dei nuovi test del Contratto B4 in `AutoWalkPathfinderTest.java`.
2. **Verifica Non-Regressione**:
   - Riconferma dei 22 test della suite `AutoWalkPathfinderTest` (inclusi i test 5D.5 per la discesa da Primo Piano a Rimessa).
3. **Esecuzione Suite Completa**:
   - Comando: `.\gradlew.bat --no-daemon test`.
   - Requisito: 100% test superati senza fallimenti.
4. **Compilazione Artefatto**:
   - Comando: `.\gradlew.bat --no-daemon shadowJar`.

---

## 6. Sequenza Operativa

- [x] Sotto-Fase 1A: Redazione del Piano Tecnico Formale e deposito in `docs/piani/attivi/`.
- [x] **STOP OBBLIGATORIO (Regola 0 ASTRALIS)**: Ricevuta autorizzazione esplicita di Luca.
- [x] Sotto-Fase 1B: Implementazione Contratti B1, B2, B3 in `AutoWalkPathfinder.java`.
- [x] Sotto-Fase 1B: Implementazione test Contratto B4 in `AutoWalkPathfinderTest.java`.
- [x] Sotto-Fase 1B: Esecuzione suite di test automatica (100% superata, 282+ test) e compilazione `shadowJar`.
- [x] Fase 2: Deploy automatico nelle istanze PrismLauncher di Luca (`Minecraft 26.2 Access 1.12.0` e `Server Tenuta`).
- [ ] Fase 3: Convalida positiva in-game di Luca e chiusura tecnica.

---

## 7. Stato Attuale — Pronto per il Collaudo in Gioco (Fase 2 Attiva)

L'artefatto compilato è stato deployato con successo nelle due istanze attive di PrismLauncher di Luca. Il sistema è in modalità **Telemetria Live** pronto per il collaudo empirico in-game di Luca dalla Torre Belvedere.
