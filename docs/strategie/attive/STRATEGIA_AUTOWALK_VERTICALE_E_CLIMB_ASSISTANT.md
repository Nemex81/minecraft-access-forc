# Strategia Logico-Cognitiva: AutoWalk Verticale & Assistente Tattico di Scalata (Climb Assistant) (Rev MC-26.22)
# Autori: Luca (Sviluppatore Senior Non Vedente) & Antigravity (AI Pair Programmer)
# Data: 2026-09-11
# Framework: ASTRALIS v3.0.4 — Protocollo 1 & Protocollo 12 (Inner Codex Pattern)
# Rete Documentale DRY:
#   - Report Handover Codex: docs/report/REPORT_HANDOVER_CODEX_PIANO_AUTOWALK_VERTICALE.md
#   - Registro Revisioni RRU: docs/report/REGISTRO_REVISIONI.md
#   - Piano Tecnico Corrente (1A, attesa convalida): docs/piani/attivi/PIANO_TECNICO_CORRETTIVO_TRANSIZIONI_VERTICALI_E_LANDING.md
#   - Baseline storica: docs/piani/superati/MC-26.22/PIANO_TECNICO_AUTOWALK_VERTICALE_E_CLIMB_ASSISTANT.md

---

## 1. VISIONE STRATEGICA & MODELLO MENTALE

Nel gameplay di Minecraft per un giocatore non vedente che interagisce al 100% tramite screen reader NVDA e feedback sonori, le transizioni verticali su scale a pioli, impalcature e piante rampicanti rappresentano uno dei punti di maggiore complessità e rischio:
1. **In salita**: l'utente non ha riscontro visivo del culmine della scala. Mantenere la pressione in avanti oltre la sommità può provocare cadute all'indietro o mancati agganci del pianerottolo.
2. **In discesa**: imboccare una scala a pioli dall'alto (specialmente in un pozzo 1x1 protetto da botola) richiede un allineamento millimetrico difficilissimo da eseguire senza vista, con elevato pericolo di caduta fatale nel vuoto.
3. **Macro-Navigazione (AutoWalk)**: attualmente l'algoritmo A* in `AutoWalkPathfinder` gestisce dislivelli solo fino a delta Y = +1 (salti/gradini) e discese fino a delta Y = -3 (salti giù controllati). I blocchi `LadderBlock` sono stati modellati come passabili orizzontalmente dal Contratto D6, ma esclusi come pavimentazione calpestabile (`isStandable = false`).

### La Soluzione Sinergica Duale:
Il sistema viene esteso introducendo due funzionalità perfettamente complementari che condividono lo stesso nucleo cinematico ed esecutivo:
- **Funzionalità A (AutoWalk Globale con Vertical Traversal)**: l'A* calcola rotte tridimensionali complete che possono comprendere segmenti di arrampicata verticale per raggiungere target posti a qualsiasi quota;
- **Funzionalità B (Climb Assistant / Semi-AutoWalk Tattico a Comando)**: una routine mirata richiamabile dall'utente quando si trova davanti o all'imbocco di una scala, che si occupa unicamente di agganciare la scala, salire o scendere fino al primo blocco sicuro calpestabile (`isStandable`), stabilizzare il personaggio e restituire all'istante il pieno controllo dei movimenti a Luca.

---

## 2. MODELLO GEOMETRICO VOXEL DEI BLOCCHI ARRAMPICABILI

Per garantire modularità ed estendibilità a qualsiasi elemento scalabile di Minecraft, il sistema adotta un'astrazione unificata:

1. **Blocchi a Parete con Orientamento (`WALL_MOUNTED`)**:
   - `LadderBlock` (scale a pioli): spessore 0.1875m, ancorato a un blocco solido di supporto. Possiede la proprietà `FACING` che definisce la direzione verso cui la scala è rivolta.
   - `VineBlock` (viti rampicanti): possono trovarsi su più facce dello stesso blocco.
   - *Regola di Ingaggio*: il giocatore deve guardare e avanzare verso la parete di supporto per salire.
2. **Blocchi Volumetrici Autoportanti (`SCAFFOLDING`)**:
   - `ScaffoldingBlock` (impalcature): attraversabile verticalmente al suo interno. Salita mediante mantenimento del salto (`keyJump`), discesa mediante abbassamento (`keySneak`). Possiede una superficie calpestabile in cima se approcciata dall'alto.
3. **Vegetazione Sospesa (`HANGING_VINES`)**:
   - `WeepingVinesBlock`, `TwistingVinesBlock`: colonne verticali scalabili tipiche del Nether.

### Contratti di Atterraggio (Landing Contracts):
- **Top Landing (Pianerottolo Superiore)**:
  - Definizione: il blocco calpestabile (`isStandable == true`) immediatamente adiacente alla sommità della colonna arrampicabile, oppure la quota $Y+1$ sopra l'ultimo blocco di scala se sfocia su un pavimento aperto.
  - Verifica di testa: assenza di ostruzioni solide per $0.6 \times 1.8\text{ m}$.
- **Bottom Landing (Base Inferiore)**:
  - Definizione: il blocco solido calpestabile (`onGround() == true`, collisione non vuota) su cui poggia o atterra la base della scala.

---

## 3. ESTENSIONE DEL PATHFINDING A* (`AutoWalkPathfinder`)

L'esplorazione dei vicini in `AutoWalkPathfinder.getValidNeighbors` viene estesa per supportare transizioni verticali multi-blocco (`ClimbMove`):

1. **Rilevamento Nodi Arrampicabili**:
   - Se il nodo corrente si trova adiacente a un blocco arrampicabile, oppure se il nodo corrente è già all'interno di una colonna arrampicabile;
   - L'algoritmo genera transizioni verticali verso l'alto ($Y+1$) e verso il basso ($Y-1$) lungo la colonna.
2. **Bilanciamento Euristico del Costo (Cancello 1 — Inner Codex)**:
   - Divieto assoluto di pesi fittizi arbitrari o forzature euristiche;
   - La velocità di salita su scala in Minecraft è di circa $2.35\text{ m/s}$ (rispetto ai $4.3\text{ m/s}$ della camminata piana). Il costo $gCost$ per blocco verticale deve riflettere questo rapporto fisico ($costo \approx 1.8 \times passo\_orizzontale$), facendo sì che l'A* scelga rampe o percorsi piani se disponibili, ma selezioni naturalmente la scala se rappresenta la via più rapida o l'unica via.
3. **Clearance Continua della Colonna (Cancello 3 — Inner Codex)**:
   - Ogni passo verticale della colonna deve verificare che lo spazio per il corpo e la testa sia privo di ostacoli solidi o pericoli (lava, fuoco).

---

## 4. CINEMATICA E MACCHINA A STATI DEL MOTORE (`AutoWalkMotor`)

Vengono aggiunti gli stati dedicati `CLIMBING_UP` e `CLIMBING_DOWN` alla FSM di `AutoWalkMotor`:

1. **Fase 1: Ingresso (Mount / Aggancio)**:
   - *In salita*: il motore calcola l'angolo verso la faccia della scala, orienta la visuale (`Yaw`) con sterzata progressiva e inietta `keyUp` per far entrare il giocatore nella hitbox della scala.
   - *In discesa*: il motore guida il giocatore verso l'orlo del pozzo, si coordina con `SafeDescentCandidate` per non essere bloccato dall'auto-sneak, e inserisce il giocatore nella colonna.
2. **Fase 2: Transito Continuo (Climb Traversal)**:
   - *In salita*: iniezione controllata di `keyUp` (e `keyJump` per scaffolding), mantenendo la direzione dello sguardo verso la parete.
   - *In discesa*: discesa gravitazionale controllata, con monitoraggio continuo della velocità $v_y$.
   - *Watchdog di Stallo*: se per 15 tick consecutivi la quota $Y$ non progredisce, interviene la procedura di sblocco o cancellazione sicura.
3. **Fase 3: Sbarco e Stabilizzazione (Dismount / Landing)**:
   - Quando la quota $Y$ del giocatore raggiunge il Top Landing o il Bottom Landing, il motore interrompe l'input verticale e inietta un passo orizzontale di completamento verso il centro del blocco calpestabile (`isStandable`).
   - Verifica di stabilità: `player.onGround() == true` e distanza orizzontale dal centro $< 0.35\text{ m}$.
   - Arresto pulito di tutti i comandi virtuali e notifica di arrivo.

---

## 5. SINERGIA CON BOTOLE E SISTEMA DI SICUREZZA

1. **Sinergia con `DoorInteractionManager` (Zero Duplicazioni)**:
   - `DoorInteractionManager` gestisce già attivamente `activeTrapdoorSessions` e `TrapdoorPassageSession` con la logica di **Passage Renewal** lungo la colonna della scala (Rev MC-26.13).
   - Durante la salita, all'avvicinarsi della botola in cima alla scala, `DoorInteractionManager` intercetta la presenza del giocatore ed esegue l'apertura automatica; all'uscita sul pianerottolo, esegue la chiusura automatica. Nessuna logica concorrente viene duplicata.
2. **Sinergia con `CentralFallSafetyManager` & `SafeDescentCandidate`**:
   - La discesa da un pozzo non deve innescare l'allarme acustico o l'auto-freno di caduta. La presenza verificata della scala registra un'istanza transitoria di `SafeDescentCandidate` che informa il sistema di sicurezza che la discesa è controllata.
3. **Human Takeover Immediato (Cancello 2 — Inner Codex)**:
   - Come per l'AutoWalk ordinario, se Luca tocca manualmente un tasto di movimento (`W`, `A`, `S`, `D`, `Sneak`), il motore rilascia all'istante ogni controllo e ripristina la guida manuale al 100%.

---

## 6. MECCANISMO DI ATTIVAZIONE E MAPPATURA CONTROLLI

L'assistente di scalata tattico (Semi-AutoWalk) supporta tre canali di attivazione coordinati:

1. **Canale Primario: Tasto Interazione Contestuale (`keyUse` / Tasto Destro / `KEY_RBRACKET`)**:
   - **Se** il giocatore preme il tasto interazione mirando a una scala o trovandosi sul ciglio di una scala discendente;
   - **E** non è accovacciato (lo `Sneak` manuale fa da bypass per consentire di piazzare torce/blocchi senza salire);
   - **Allora** avvia immediatamente la routine di arrampicata/discesa.
2. **Canale Secondario Dedicato: Tasto `Alt+S` (S = Scala / Salita-Scendi)**:
   - *Verifica Conflitti*: i tasti `Alt+X`, `Alt+C`, `Alt+Z` sono occupati da `PositionNarrator` per la lettura delle coordinate X, Y, Z. Il tasto `Alt+S` risulta **completamente libero** e semanticamente perfetto per "Scala / Salita-Scendi".
   - Dotato delle consuete guardie esclusive `ModifierUtils.hasAltOnly()`.
3. **Canale Access Menu**:
   - Voce dedicata in `org.mcaccess.minecraftaccess.addon.accessmenu.AutoClimb` (es. *"Sali/Scendi Scala"*).

---

## 7. I 6 CANCELLI DEL PROTOCOLLO 12 (INNER CODEX CHECK)

1. **Cancello 1 (Rifiuto Patching Euristico)**: Costo A* calcolato sui tempi fisici di risalita voxel, zero modifiche al budget o pesi magici.
2. **Cancello 2 (Hardware Grounding)**: Human takeover basato su `isManualMovementKeyPressed` disaccoppiato dal motore.
3. **Cancello 3 (Hitbox & Clearance Continua)**: Verifica continua $0.6 \times 1.8\text{ m}$ lungo tutta la colonna verticale.
4. **Cancello 4 (Named Contracts D0..DN)**: Scomposizione atomica nei contratti da redigere nel Piano Tecnico da parte di GPT Codex.
5. **Cancello 5 (Determinismo Headless)**: Test seams package-private deterministici a 0 ms senza `Thread.sleep`.
6. **Cancello 6 (Budget Token & Anti-Bloat Normativo)**: Rete documentale snella, zero duplicazioni, conformità ASTRALIS.
