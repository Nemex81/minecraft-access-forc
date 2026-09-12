# Strategia Logico-Cognitiva: Quiete Sensoriale AutoWalk, Verbosità di Progressione & Silenziamento Mirino (ASTRALIS v3.0.4)

- **Autori**: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity (Senior AI Pair Programmer)
- **Ramo Git di Riferimento**: `feat/cognitive-orchestrator`
- **Revisione di Riferimento**: Rev MC-26.19
- **Framework di Governance**: ASTRALIS v3.0.4 (Fase 0 — UPCS Unified Progressive Cognitive Strategy)
- **Stato**: `[STRATEGIA COMPLETATA E ARCHIVIATA — ESITO POSITIVO AL 100%]`
- **Data di Redazione**: 2026-09-08
- **Documenti Correlati**:
  * [`docs/piani/completati/PIANO_TECNICO_REV_MC-26.19_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/piani/completati/PIANO_TECNICO_REV_MC-26.19_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md)
  * [`docs/report/archivio/REPORT_SESSIONE_REV_MC-26.19_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/archivio/REPORT_SESSIONE_REV_MC-26.19_QUIETE_SENSORIALE_AUTOWALK_E_NAVIGATORE.md)
  * [`docs/report/ARCHIVIO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/ARCHIVIO_REVISIONI.md)
  * [`src/main/java/org/mcaccess/minecraftaccess/Config.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/Config.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/AutoWalkMotor.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/autowalk/MovementCoordinator.java)
  * [`src/main/java/org/mcaccess/minecraftaccess/features/NarrateCrosshair.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/NarrateCrosshair.java)

---

## 🧭 1. Visione Cognitiva & Obiettivo di Accessibilità (Screen Reader NVDA)

L'esperienza consolidata con la Rev MC-26.18 sul rilevatore di cadute duale (`CentralFallSafetyManager`) ha dimostrato empiricamente che l'AutoWalk raggiunge la sua massima efficacia quando il giocatore è immerso in un ambiente acustico pulito, ordinato e concentrato unicamente sul ritmo dei passi e sulle informazioni strettamente necessarie.

Tuttavia, l'analisi del comportamento in marcia evidenzia la presenza di **tre sorgenti residue di disturbo sensoriale e accoppiamento improprio**:
1. **Accoppiamento del Conteggio Passi con i Suggerimenti Globali**:
   - In `AutoWalkMotor`, il conteggio dei passi mancanti a traguardi di 5 (`callback.onProgression(remainingSteps)`) è vincolato alla variabile globale `narrateHints` (suggerimenti generali della mod).
   - Se Luca o un utente disattivano i suggerimenti per non udire la guida didattica, perdono involontariamente il conteggio dei passi, che costituisce invece una telemetria di rotta preziosa e rassicurante.
2. **Arpeggio Residuo del Mirino (`playRelativePositionSoundCue`)**:
   - In `NarrateCrosshair`, il suono Note Block Harp legato alla quota del blocco puntato scatta direttamente nel tick del client, prima di passare per `CrosshairFeedbackManager`.
   - Quando l'AutoWalk sterza fino a 20°/tick per seguire le curve della rotta, il mirino interseca decine di blocchi a quote differenti, generando un arpeggio continuo di note arpa non zittito.
3. **Gerarchia tra Interruttore Master e Filtri di Marcia**:
   - Necessità di ribadire formalmente che gli interruttori generali di modulo (`enabled` in `FallDetector`, `ObstacleDetector`, `AutoWalk`) governano l'esistenza stessa del modulo a runtime (Livello 0) e non devono mai essere confusi, indeboliti o scavalcati dai filtri contestuali di quiete in marcia (`silence*DuringWalk`, Livello 1).

---

## 🏛️ 2. I Quattro Pilastri Logico-Cognitivi della Soluzione

### 2.1 Pilastro 1 — Verbosità di Progressione Regolabile (`ProgressionFeedbackMode`)
- **Modello Mentale**: L'avanzamento lungo la rotta deve essere personalizzabile secondo il profilo sensoriale dell'utente, con impostazione predefinita ricca e informativa:
  - `SOUND_AND_VOICE` *(Default)*: Emette il click leggero a ogni nodo (`NOTE_BLOCK_HAT` a pitch 1.8f con debouncing 200 ms) **E** pronuncia l'annuncio vocale a intervalli di 5 passi (*"Mancano 15 passi"*);
  - `SOUND_ONLY`: Emette unicamente il click dei nodi a ogni passo, per chi desidera il totale silenzio della voce fino alla meta;
  - `VOICE_ONLY`: Pronuncia l'annuncio vocale ogni 5 passi senza emettere i click intermedi dei nodi;
  - `OFF`: Marcia completamente silenziosa sui nodi intermedi (emette solo avvio, arrivo ed emergenze).
- **Disaccoppiamento**: Questa opzione risiede nativamente nella scheda `autoWalk` di Cloth Config, svincolandosi al 100% da `speechSettings.narrateHints`.

### 2.2 Pilastro 2 — Silenziamento dell'Arpa di Quota del Mirino
- **Regola Funzionale**: In `NarrateCrosshair`, l'emissione del suono `playRelativePositionSoundCue` viene soppressa se l'AutoWalk è attivo (`MovementCoordinator.isAutoWalkActive()`) e l'opzione `silenceCrosshairDuringWalk` è abilitata.
- **Sovranità Manuale (DirectInteractionShield)**: Se il giocatore preme volontariamente il tasto `B` per interrogare il blocco o l'entità mirata, il mirino risponde all'istante a latenza 0 ms, pronunciando il nome e l'elevazione con `interrupt = true`.

### 2.3 Pilastro 3 — Sovranità dell'Interruttore Master di Modulo
- **Livello 0 — Interruttore Generale (`config.enabled`)**:
  - `config.fallDetector.enabled`: se disattivato, il sottosistema cadute (prossimità, lungo raggio e fisica) è spento alla radice;
  - `config.obstacleDetector.enabled`: se disattivato, il rilevatore ostacoli è spento alla radice;
  - `config.autoWalk.enabled`: se disattivato, il motore di navigazione rifiuta l'avvio della rotta.
- **Livello 1 — Filtri Contestuali di Marcia (`silence*DuringWalk`)**:
  - Intervengono **esclusivamente** se il modulo è abilitato (`enabled == true`) e l'AutoWalk è in marcia (`isAutoWalkActive == true`).

### 2.4 Pilastro 4 — Gestione Ostacoli in Navigazione
- **Ostacoli Ordinari e Scalabili**: I dislivelli fino a 1.20 m superati dall'auto-jump (`STEP_CLIMBABLE`) e le pareti costeggiate lungo il tragitto tacciono al 100%, evitando falsi allarmi;
- **Blocchi Imprevisti o Sbarramenti**: Se un ostacolo imprevisto arresta l'avanzamento fisico, interviene il Watchdog cinematico con ricalcolo o aborto (`autowalk:stuck`);
- **Ispezione Manuale Sovrana**: La pressione manuale del tasto `U` (`inspectObstacle`) risponde all'istante vocalizzando e orientando la visuale sull'ostacolo.

### 2.5 Pilastro 5 — Silenziamento del Cue Acustico Anticaduta (Incudine) in Marcia
- **Modello Mentale & Razionale**: Il pathfinding A* calcola a monte percorsi geometricamente stabili e privi di dirupi mortali. Quando la traiettoria costeggia un ciglio, attraversa un ponte o scende un pendio, il giocatore entra naturalmente nella fascia $1.0 - 1.5\text{ m}$ (Zona 2A/2B).
- **Abbattimento del Falso Allarme Acustico**: L'incudine (`ANVIL_LAND`), pensata per bloccare il giocatore in marcia manuale, costituisce in AutoWalk un falso allarme traumatico per chi usa NVDA. Se `silenceFallWarningsDuringWalk` è attivo, viene silenziata sia la voce sia il boato dell'incudine (`audioSilencedInAutoWalk = autoWalkActive && silenceFallWarningsDuringWalk`).
- **Resilienza e Ripristino Istantaneo**: Se l'AutoWalk si interrompe per stallo (Watchdog) o se il giocatore interviene manualmente (Human Takeover `keySneak` / WASD), la modalità di marcia decade istantaneamente (`autoWalkActive = false`) e l'allarme acustico dell'incudine torna operativo al 100% a latenza zero.

---

## 🛡️ 3. Le 5 Invarianti Inviolabili (Inner Codex Pattern)

1. **Invariante 1 — Sovranità dell'Interruttore Master**: Nessun filtro contestuale di marcia può alterare, disattivare o aggirare l'interruttore generale `enabled` di alcun modulo;
2. **Invariante 2 — Tutela del Modello di Marcia di Luca**: Il default di progressione è congelato su `SOUND_AND_VOICE`, garantendo a Luca la continuità 1:1 della lettura dei passi mancanti;
3. **Invariante 3 — Sovranità dei Comandi Manuali da Tastiera**: I tasti `B` (mirino), `U` (ispezione ostacolo), `5`/`M` (livellamento) e `X` (radar POI) bypassano qualsiasi silenziamento;
4. **Invariante 4 — Fail-Safe Fisico e Protezione Manuale Inviolabile**: L'auto-sneak e la protezione fisica del movimento non vengono disattivati; l'incudine acustica `ANVIL_LAND` è silenziata esclusivamente a rotta A* attiva; in caso di aborto navigazione, stallo o controllo manuale torna attiva all'istante a priorità `CRITICAL`;
5. **Invariante 5 — Transizione Istantanea Post-Marcia**: Al momento dell'arrivo (`ARRIVED`) o dell'arresto (`CANCELLED`), tutti i filtri di marcia decadono all'istante, ripristinando il regime ordinario per l'esplorazione a piedi.

---

## 🧪 4. Validazione Preventiva ASTRALIS sui 7 Assi di Qualità

- **1. Validità**: Pienamente conforme all'architettura Fabric/NeoForge, Balm, Cloth Config e Java 25.
- **2. Efficacia**: Abbattimento completo dell'arpeggio spurio del mirino in curva e totale libertà per l'utente di calibrare il parlato dei passi.
- **3. Coerenza**: Perfetta simmetria con il pattern a 3 zone già adottato nel sottosistema cadute.
- **4. Completezza**: Copre progressione nodi, mirino sonoro, ostacoli e gerarchia master degli switch.
- **5. Precisione**: Distinzione chirurgica tra feed passivi (silenziati) ed emergenze/comandi manuali (sempre attivi).
- **6. Affidabilità e Prestazioni**: Filtri a costo zero eseguiti a monte senza allocazioni heap superflue.
- **7. Assenza di Regressioni**: La navigazione manuale ordinaria (WASD) resta inalterata; retrocompatibilità totale garantita.

---

## 🔬 5. Matrice di Simulazione a 3 Livelli

### Livello 1 — Scenari Comuni (Happy Path)
- **Scenario 1.1: Marcia regolare con impostazione SOUND_AND_VOICE (Default Luca)**:
  - *Condizione*: AutoWalk attivo verso un waypoint a 30 blocchi con impostazione predefinita.
  - *Esito*: Il giocatore ascolta i passi e il click dei nodi a ogni passo; ogni 5 passi la sintesi vocale annuncia *"Mancano 25 passi"*, *"Mancano 20 passi"*, ecc. Nessun arpeggio di mirino nelle curve.
- **Scenario 1.2: Marcia in modalità SOUND_ONLY**:
  - *Condizione*: L'utente imposta la verbosità su `SOUND_ONLY`.
  - *Esito*: Nessuna parola pronunciata durante il tragitto; solo il click ritmico dei nodi fino alla meta, dove suona la campana d'arrivo.

### Livello 2 — Scenari Meno Comuni & Concorrenza (Alternative Paths)
- **Scenario 2.1: Interrogazione manuale del blocco mirato con tasto B in corsa**:
  - *Condizione*: Mentre il bot cammina e l'arpa è zittita, Luca preme `B`.
  - *Esito*: Il `DirectInteractionShield` risponde vocalmente pronunciando il blocco mirato a latenza zero (`interrupt = true`). Subito dopo, il mirino torna silenzioso.
- **Scenario 2.2: Interruttore generale FallDetector disattivato in configurazione**:
  - *Condizione*: L'utente disabilita `fallDetector.enabled = false` e avvia un AutoWalk.
  - *Esito*: Il modulo cadute è totalmente inattivo; l'AutoWalk naviga secondo il suo pathfinder senza invocare la fisica del modulo cadute disabilitato.

### Livello 3 — Casi Limite & Corner Cases (Boundary, Zero, Null, Error)
- **Scenario 3.1: Percorso breve (< 5 passi)**:
  - *Condizione*: Bersaglio a 3 passi.
  - *Esito*: Il click dei nodi accompagna i 3 passi; nessun annuncio di progressione a 5 passi; arrivo immediato a meta con campana.
- **Scenario 3.2: Cambio configurazione a caldo durante la marcia**:
  - *Condizione*: L'utente cambia la modalità di verbosità dal menu mentre la marcia è in corso.
  - *Esito*: Al ritorno in gioco, il motore applica immediatamente la nuova modalità senza eccezioni o desincronizzazioni.

---

## 🏁 6. Esito della Validazione di Fase 0
- **Esito Formale**: `SUPERATA AL 100% (7/7 Assi Verificati, Matrice a 3 Livelli Conforme)`.
- **Autorizzazione alla Sotto-Fase 1A**: Convalidata la stesura del Piano Tecnico Formale.
