# Piano tecnico implementativo completo — Traversal Safety e integrazione cognitiva

**Ramo vincolato:** `feat/cognitive-orchestrator`  
**Baseline da verificare prima di iniziare:** `d60c234acab2126250a3997de84b0699cb99a01f`  
**Riferimento strategico:** `docs/strategie/STRATEGIA_SISTEMICA_TRAVERSAL_SAFETY_E_CENTRALIZZAZIONE_COGNITIVA.md`  
**Stato:** `[PIANO COMPLETO — IMPLEMENTAZIONE AUTORIZZABILE SOLO PER FASE A]`  
**Ambito:** Fase 3A, sicurezza di attraversamento verticale, ownership dello sneak e pubblicazione di osservazioni cognitive.  

---

## 0. Decisione, scopo e regola di avanzamento

Questo piano corregge il comportamento per cui una discesa validata può restare bloccata dallo sticky-sneak e definisce l'evoluzione verso una gestione riusabile delle osservazioni dei rilevatori.

Il piano descrive integralmente le Fasi A–D, ma non autorizza l'esecuzione automatica dell'intero programma. La sola Fase A può iniziare dopo approvazione esplicita; ciascuna fase successiva richiede i propri test superati, una revisione del diff e una nuova autorizzazione.

### Inclusi

- `FallDetector` e package `features.safety.traversal`;
- contratti di input/override dello sneak confinati al dominio di sicurezza;
- test unitari e di integrazione del percorso di caduta/discesa;
- eventi cognitivi e risorse I18n strettamente necessari alla discesa;
- documentazione di esito e collaudo.

### Esclusi

- modifiche a `ObstacleDetector`, `CrosshairFeedbackManager`, `NarrateCrosshair`, AutoWalk o altri rilevatori della Fase 3B;
- mixin, nuove GUI, comandi, protocollo, salvataggi mondi o configurazioni pubbliche non espressamente approvate;
- deploy automatico su PrismLauncher, copia di JAR o modifiche alle istanze di gioco;
- migrazione generalizzata delle chiamate a `MainClass.narrate`.

Build e deploy sono gate operativi distinti: una build valida non equivale a collaudo NVDA, e un deploy richiede autorizzazione separata.

---

## 1. Diagnosi tecnica da preservare

Nella scena di regressione il blocco scala è a `(-60, 84, -42)` e il giocatore è stato rilevato a `X=-59,28`. La scala occupa la fascia `X=[-60,-59]`, per cui la distanza dalla sua mezzeria è circa 22 cm, non 72 cm. Il precipizio laterale rimane reale e va protetto: l'esistenza di una scala vicina non autorizza in alcun caso un passo verso il vuoto laterale.

Il codice baseline mostra quattro problemi:

1. `SafetyMovementGuard.createDefault()` usa `keyShift.isDown()` come probe dell'input umano, ma il guard modifica quello stesso stato tramite `keyShift.setDown(...)`.
2. `currentAllowedDescentId` può rimanere impostato dopo che il candidato o l'intento non sono più validi.
3. Il presidio statico `isStandingOnDangerousEdge(...)` può riacquisire sneak in una transizione di aggancio non modellata.
4. `TraversalSafetyAnalyzer` usa una posizione fissa a 0,55 m e conserva geometria parzialmente sovrapposta al fallback di `FallDetector`.

Il deadlock dell'input è un'ipotesi principale, fortemente sostenuta da codice e collaudo, non una causa dichiarata definitivamente provata prima della telemetria e del test descritti nella Fase A.

---

## 2. Invarianti architetturali e di sicurezza

| ID | Invariante |
|---|---|
| I-1 | Solo `SafetyMovementGuard` può possedere o revocare l'override sintetico di crouch. |
| I-2 | La pressione fisica/manuale dell'utente non è mai inferita dal valore già scritto dal mod. |
| I-3 | Il rilascio del token del sistema non cancella mai uno Shift manuale noto. |
| I-4 | In presenza di input non distinguibile o di geometria incompleta, prevale la protezione. |
| I-5 | Un candidato di discesa abilita unicamente il proprio corridoio raggiungibile, non tutto l'intorno. |
| I-6 | Un pericolo `CRITICAL` continua a proteggere subito il player; il coordinatore non è un prerequisito dell'azione fisica. |
| I-7 | Il `CognitiveCoordinator` riceve osservazioni semantiche, non acquisisce input né analizza voxel. |
| I-8 | Eventi di discesa sono deduplicati per candidato/transizione e non vengono narrati ogni tick. |
| I-9 | Cambio mondo/dimensione, morte, disconnessione, GUI incompatibile, acqua e disabilitazione rimuovono stato e token. |
| I-10 | Fase 3B resta immutata durante tutte le fasi di questo piano. |

---

## 3. Architettura di destinazione e dipendenze consentite

```text
Client snapshot + input grezzo + collision query
                    │
                    ▼
           TraversalPerception  (pura)
                    │ TraversalAssessment / DescentAffordance
                    ▼
         TraversalStateMachine  (pura)
              ├─ GuardDecision ───────► SafetyMovementGuard ─► SneakOverridePort
              │
              └─ TraversalObservation ─► TraversalCognitivePublisher ─► CognitiveCoordinator
```

### 3.1 Responsabilità concrete

| Componente | Responsabilità | Dipendenze vietate |
|---|---|---|
| `TraversalPerception` | Riconosce pericoli e affordanze di discesa dal solo snapshot/world query. | `MainClass`, input, guard, audio. |
| `DescentAffordance` | Descrive un varco fisico identificabile e raggiungibile. | Policy temporale, scrittura stato. |
| `TraversalStateMachine` | Trasforma assessment e stato in una transizione e in `GuardDecision`. | Minecraft singleton, I18n, suoni. |
| `RawCrouchIntentProvider` | Esprime l'intento fisico/raw, incluso il grado di affidabilità. | Policy di sicurezza e scritture client. |
| `SneakOverridePort` | Applica un valore effettivo di crouch in un solo punto. | Geometria, priorità cognitive. |
| `SafetyMovementGuard` | Gestisce esclusivamente token e riconciliazione tra token/input raw/output. | Calcoli voxel, testi/localizzazioni. |
| `TraversalCognitivePublisher` | Mappa osservazioni validate in `CognitiveEvent`. | Crouch/input/collisioni. |
| `FallDetector` | Compone snapshot, percezione, macchina, guard e publisher; conserva fallback controllato. | Duplicare le policy specialistiche. |
| `CognitiveCoordinator` | Arbitra priorità, deduplica, voce e cue degli eventi ricevuti. | Comandi fisici di movimento. |

Le dipendenze scorrono in una sola direzione: `features.safety.traversal` può pubblicare verso `features.cognitive`, mentre `features.cognitive` non deve importare classi di traversal.

---

## 4. Contratti implementativi

I nomi sono intenzionali; minime variazioni sono ammesse solo se motivate nel report d'implementazione.

### 4.1 Input e ownership — Fase A

```java
interface RawCrouchIntentProvider {
    CrouchIntent readIntent();
}

record CrouchIntent(boolean pressed, boolean reliable) {}

interface SneakOverridePort {
    void applyEffectiveCrouch(boolean crouching);
}
```

- `pressed=true, reliable=true`: l'utente sta premendo fisicamente il binding conosciuto.
- `pressed=false, reliable=true`: non c'è pressione fisica rilevata.
- `reliable=false`: l'adapter non può distinguere con sicurezza il binding/dispositivo; il guard mantiene la protezione se il token è attivo.

`SafetyMovementGuard` conserva `systemCrouchToken` e applica ad ogni riconciliazione:

```text
effectiveCrouch = systemCrouchToken || (rawIntent.reliable && rawIntent.pressed)
```

Quando `rawIntent.reliable=false` e il token viene rilasciato, l'implementazione deve adottare la policy conservativa documentata nel codice: non simulare di conoscere l'input. La decisione precisa dipende dall'API verificata al Gate A; non è ammesso usare `KeyMapping.isDown()` dopo che il mod lo ha scritto come prova dell'intenzione fisica.

L'implementazione client di default può usare GLFW **soltanto dietro `RawCrouchIntentProvider`** e dopo aver verificato il tipo di keybinding/API della versione corrente. Controller, mouse e binding non standard non sono dichiarati supportati finché non hanno adapter e collaudo dedicati.

### 4.2 Snapshot e percezione — Fasi B/C

```text
TraversalSnapshot
  playerAabb, playerPosition, playerFeet, onGround, onClimbable
  movementIntent, horizontalVelocity, dangerThreshold, timestamp
  BlockGetter/collision query

DescentAffordance
  candidateId, descentType, entryVolume, attachmentVolume
  approachVector, anchorPosition, landingPosition
  lateralOffset, support/face data, diagnostic reason

TraversalAssessment
  hazard status, optional affordance, diagnostic reason
```

`DescentAffordance` sostituisce progressivamente l'attuale `SafeDescentCandidate`, senza rimuoverlo finché Fase C non dimostra parità di comportamento.

### 4.3 Macchina a stati — Fase B

```text
SAFE
EDGE_PROTECTED
DESCENT_ARMED
DESCENT_LATCHING
CLIMBING
```

La macchina riceve snapshot, assessment, stato precedente e tempo monotono iniettato; restituisce:

```text
TraversalDecision
  nextState
  guardDecision: ENGAGE / RELEASE_SYSTEM_TOKEN / KEEP / CLEAR
  optional traversalObservation
  transitionReason
```

`DESCENT_LATCHING` richiede un candidato invariato, intento coerente e finestra breve. Nella Fase B il limite iniziale è massimo 8 tick **e** 400 ms, con una distanza massima dall'ingresso. In Fase C il limite spaziale usa `attachmentVolume`. Qualunque fallimento rivalidato a ogni tick produce `ENGAGE`, senza attendere la deadline.

### 4.4 Osservazioni cognitive — Fase D

| Chiave semantica | Dominio/priorità | Condizione |
|---|---|---|
| `guidance.descent.available` | `GUIDANCE / CONTEXTUAL` | Nuova affordance raggiungibile, una sola volta. |
| `guidance.descent.align_left` | `GUIDANCE / CONTEXTUAL` | Offset laterale significativo verso sinistra. |
| `guidance.descent.align_right` | `GUIDANCE / CONTEXTUAL` | Offset laterale significativo verso destra. |
| `safety.traversal.safe_descent` | `SAFETY / OPERATIONAL` | Solo se l'utente avvia un'azione/richiesta operativa esplicita; non come annuncio periodico. |
| pericolo caduta | `SAFETY / CRITICAL` | Flusso esistente, invariato nella precedenza. |

La soglia iniziale per la guida laterale è 25–30 cm, calcolata rispetto alla faccia utile/centro del candidato, non all'origine del blocco. Il cue 3D è posizionato sull'anchor del candidato e non aggira mai le preferenze audio/voce esistenti.

---

## 5. Gate preliminare A0 — verifica prima di modificare codice

**Obiettivo:** verificare le ipotesi API e congelare il comportamento baseline.

1. Confermare branch, commit di partenza e assenza di modifiche sorgente estranee nel worktree.
2. Individuare l'API reale per leggere un keybind fisico nella versione Minecraft/Balm corrente, verificando tipo di `keyShift`, tastiera e implicazioni del loop input client.
3. Verificare se l'applicazione via `keyShift.setDown(...)` deve essere riconciliata ogni tick e in quale momento del tick non confligge col client.
4. Confermare gli hook già disponibili per reset: mondo/dimensione, morte/respawn, disconnessione, GUI e acqua.
5. Aggiungere o progettare telemetria `debug` non narrata che includa: raw input/riliability, token, effective crouch, candidato, stato, ragione e `onClimbable`.
6. Scrivere test rossi che dimostrino il deadlock baseline in termini di contratto, senza dipendere dal singleton Minecraft reale.

**Stop:** se il raw input non è distinguibile con affidabilità, non introdurre una scorciatoia che rilasci lo sneak; aggiornare il piano e mantenere fail-safe.

---

## 6. Fase A — ownership dello sneak e revoca dei permessi residui

### A.1 Obiettivo

Rimuovere il deadlock tra override sintetico e presunto input manuale senza cambiare il modello geometrico né la Fase 3B.

### A.2 File previsti

| File | Intervento |
|---|---|
| `features/safety/traversal/CrouchIntentProbe.java` | Evolvere o sostituire con contratto raw affidabile, mantenendo compatibilità testabile. |
| `features/safety/traversal/RawCrouchIntentProvider.java` | Nuovo adapter contrattuale, se non ottenibile evolvendo il tipo esistente senza ambiguità. |
| `features/safety/traversal/SneakOverridePort.java` | Nuovo confine dell'effetto verso Minecraft. |
| `features/safety/traversal/SafetyMovementGuard.java` | Token esplicito, riconciliazione centralizzata, pulizia candidato autorizzato. |
| `features/FallDetector.java` | Rimuovere gate persistenti quando l'intento/candidato decade; invocare riconciliazione/reset corretti. |
| test traversal e `FallDetector` | Nuove prove di ownership, revoca e reset. |
| `docs/report/...` | Resoconto con API verificata, risultati e limiti input. |

Nessuna modifica a risorse, configurazione pubblica, `ObstacleDetector` o build/deploy in questa fase.

### A.3 Algoritmo

1. All'ingresso in protezione il guard acquisisce `systemCrouchToken` e riconcilia l'output effettivo.
2. Se una discesa già validata viene concessa, il guard rilascia soltanto il token del sistema, registra `candidateId` e riconcilia senza scrivere una falsa intenzione fisica.
3. Prima del presidio statico, `FallDetector` invalida l'autorizzazione se non esiste più intento o se l'assessment non conferma il medesimo candidato.
4. Disabilitazione detector, GUI, acqua/nuoto, cambio ciclo di vita o candidato diverso eseguono `CLEAR` e cancellano l'autorizzazione.
5. Il presidio statico può riacquisire il token per un bordo reale quando nessun candidato ancora valido lo copre.
6. Ogni tick attivo riconcilia port/input/token in un solo punto; non devono esistere altre scritture a `keyShift` nel perimetro modificato.

### A.4 Test obbligatori

- token acquisito con raw input rilasciato → effective crouch attivo;
- discesa valida con raw input rilasciato e affidabile → token rilasciato/effective crouch inattivo;
- discesa valida con raw input premuto → token rilasciato ma effective crouch mantenuto;
- input non affidabile → comportamento conservativo documentato;
- arresto, cambio candidato, GUI, acqua e disabilitazione → autorizzazione cancellata;
- regressione: un precedente candidato non sopprime la protezione su un nuovo ciglio;
- test del `FallDetector` con seam del guard: ordine di revoca prima del presidio statico;
- nessuna doppia narrazione o modifica ai flussi `Alt+F`/toggle auto-sneak.

### A.5 Criteri di uscita e rollback

La Fase A è superata solo se test automatici e telemetria dimostrano che token e input fisico non vengono confusi. È vietato dichiarare risolta la discesa completa prima del collaudo NVDA.

Commit atomico proposto: `fix(safety): separate system crouch ownership from raw intent`.

Rollback: `git revert` del solo commit di Fase A; nessun `reset --hard`.

**Gate A1:** revisione del diff, build/test autorizzati e collaudo manuale della scena reale. Solo dopo esito positivo si chiede approvazione per Fase B.

---

## 7. Fase B — macchina a stati formale

### B.1 Obiettivo

Eliminare la volatilità dei rami tick-by-tick e rendere dimostrabile ogni transizione senza ancora sostituire l'intera geometria.

### B.2 File previsti

| File | Intervento |
|---|---|
| `features/safety/traversal/TraversalState.java` | Nuovo enum dei cinque stati. |
| `features/safety/traversal/TraversalStateMachine.java` | Nuova policy pura e senza singleton client. |
| `features/safety/traversal/TraversalDecision.java` | DTO immutabile: stato, comando guard, osservazione, ragione. |
| `features/safety/traversal/TraversalTransitionInput.java` | DTO immutabile con tempo, snapshot minimo e assessment. |
| `SafetyMovementGuard.java` | Consuma `GuardDecision`, senza calcolare la transizione. |
| `FallDetector.java` | Costruisce input e delega alla macchina. |
| test traversal/FallDetector | Copertura delle transizioni e delle uscite. |

### B.3 Tabella di transizione essenziale

| Stato corrente | Condizione | Stato/azione successiva |
|---|---|---|
| `SAFE` | Bordo reale senza affordance | `EDGE_PROTECTED` + `ENGAGE` |
| `SAFE` o `EDGE_PROTECTED` | Affordance vista ma non pronta | `DESCENT_ARMED` + `KEEP/ENGAGE` |
| `DESCENT_ARMED` | Intento compatibile e candidato raggiungibile | `DESCENT_LATCHING` + `RELEASE_SYSTEM_TOKEN` |
| `DESCENT_LATCHING` | `onClimbable()` | `CLIMBING` + `CLEAR` |
| `DESCENT_LATCHING` | Stop, deviazione, candidato mutato, deadline o pericolo | `EDGE_PROTECTED` + `ENGAGE` |
| `CLIMBING` | Uscita su supporto sicuro | `SAFE` + `CLEAR` |
| `CLIMBING` | Uscita presso bordo rischioso | nuova valutazione + `EDGE_PROTECTED` se necessaria |
| qualunque | reset ciclo di vita | `SAFE` + `CLEAR` |

La macchina non interpreta `onClimbable()` come prova che il terreno finale sia sicuro: al termine della salita/discesa una nuova valutazione è obbligatoria.

### B.4 Test obbligatori

- tutte le transizioni della tabella, comprese le transizioni impossibili;
- limite con otto tick e con 400 ms, usando clock deterministico;
- stop e deviazione prima/dopo rilascio del token;
- aggiornamento del candidato e reset mondo;
- ingresso/uscita da `CLIMBING` su supporto sicuro e bordo rischioso;
- un pericolo critico forza `ENGAGE` senza dipendere dall'output vocale.

**Gate B1:** nessun token/permesso residuo in test; collaudo NVDA limitato al tetto e a una scala ordinaria. Richiede nuova autorizzazione prima della Fase C.

---

## 8. Fase C — percezione volumetrica e fonte geometrica unica

### C.1 Obiettivo

Sostituire il campione statico a 0,55 m con una valutazione del volume reale e ridurre gradualmente la duplicazione con `FallDetector`.

### C.2 Nuovi/modificati contratti

| Tipo | Contenuto minimo |
|---|---|
| `DescentAffordance` | ID stabile, tipo, `entryVolume`, `attachmentVolume`, direzione/faccia, supporto, landing, anchor, offset e ragione. |
| `TraversalPerception` | API pura: snapshot → assessment; ordina candidati per raggiungibilità prima della vicinanza. |
| `CollisionQuery` o adapter equivalente | Accesso testabile a collisioni, fluidi, block state e tag. |
| `TraversalAssessment` | Pericolo, affordance opzionale e diagnostica; non decide l'output. |

### C.3 Algoritmo

1. Costruire l'AABB del giocatore attuale e il suo sweep breve lungo l'intento/velocità.
2. Cercare candidati nella cella corrente, al bordo di uscita, nella cella di ingresso e sotto/sopra il volume; non limitarsi al centro geometrico.
3. Per ciascun candidato, verificare collisioni insormontabili, faccia e blocco di supporto della scala, volume di aggancio, direzione utile e continuità della colonna.
4. Validare scala, liana, impalcatura e tag `CLIMBABLE`; botola solo come accesso aperto a un candidato già valido.
5. Per acqua: richiedere acqua non-lava, volume raggiungibile e atterraggio conforme alla policy definita nei test. Un fluido generico non diventa sicuro.
6. Costruire un `attachmentVolume` ristretto. Solo l'intersezione hitbox/sweep con questo volume autorizza `DESCENT_LATCHING`.
7. Se non esiste affordance valida, applicare la classificazione di caduta; se l'informazione è ambigua, non rilasciare la protezione.
8. Mantenere il fallback storico sotto confronto finché i test non dimostrano parità; poi rimuovere rami duplicati con commit dedicato e motivazione.

### C.4 Test obbligatori

- scena reale con scala e precipizio laterale: approccio alla scala consentito, traiettoria verso il precipizio negata;
- ostacolo insormontabile davanti a un vuoto: nessun falso allarme di varco raggiungibile;
- scale con lato/faccia incompatibili, colonne interrotte e percorsi modded `CLIMBABLE`;
- liane, impalcature, botole aperte/chiuse;
- acqua sicura/insufficiente, lava e falso atterraggio;
- diagonali, retro-marcia e hitbox che sfiora ma non può agganciare;
- equivalenza del fallback storico nei casi non di discesa.

**Gate C1:** test di parità, audit delle regole rimosse e collaudo NVDA in almeno quattro geometrie. Richiede autorizzazione prima della Fase D.

---

## 9. Fase D — pubblicazione cognitiva e guida non invasiva

### D.1 Obiettivo

Integrare l'osservazione di discesa nel sistema cognitivo senza trasformarlo in un decisore fisico e senza introdurre chatter.

### D.2 File previsti

| File | Intervento |
|---|---|
| `features/safety/traversal/TraversalCognitivePublisher.java` | Nuovo adapter da osservazioni validate a `CognitiveEvent`. |
| `features/cognitive/CognitiveEvent.java` | Modifica solo se nessuna factory esistente rende canale/TTL/chain corretti; motivazione obbligatoria. |
| `features/cognitive/CognitiveCoordinator.java` | Modifica solo se necessaria per rispettare priorità/deduplica già contrattuali. |
| `FallDetector.java` | Pubblica osservazioni al confine, senza I18n sparso. |
| `lang/it_it.json`, `lang/en_us.json` | Nuove chiavi in ordine coerente. |
| test cognitive/traversal | Priorità, deduplica, fallback e cue. |

### D.3 Regole di emissione

1. Nuova affordance raggiungibile → massimo un evento contestuale deduplicato per `candidateId`.
2. Offset oltre soglia → guida laterale contestuale, mai istruzione "un passo" se la misura non la giustifica.
3. Cue `LADDER_STEP`/equivalente → `SoundCue` 3D sull'anchor candidato, rispettando impostazioni audio esistenti.
4. Transizioni riuscite, revocate o ripetute → silenziose per default.
5. Pericolo di caduta → `SAFETY / CRITICAL`, fast-path e protezione fisica; può sopprimere guida e cue.
6. Coordinatore disabilitato → un solo fallback legacy, con la stessa deduplica locale; nessun doppio output.
7. `DirectInteractionShield` può differire guida contestuale, mai un pericolo critico o una decisione del guard.

### D.4 Test obbligatori

- affordance invariata su più tick → una sola emissione;
- candidateId mutato → una nuova emissione lecita;
- `CRITICAL` nello stesso tick → guida soppressa e protezione invariata;
- audio, voce, solo audio, sola voce e canali entrambi disabilitati;
- template I18n IT/EN presente e fallback quando il coordinatore è spento;
- nessun accesso del coordinatore a input, guard o collisioni.

**Gate D1:** suite completa positiva e collaudo NVDA che confermi utilità del cue, assenza di chatter e precedenza del pericolo. Solo allora l'intervento può essere chiuso e il contratto proposto per futuri rilevatori.

---

## 10. Sequenza commit, revisione e rollback

| Fase | Commit atomico indicativo | Condizione |
|---|---|---|
| A | `fix(safety): separate system crouch ownership from raw intent` | Test ownership e collaudo prima di B. |
| B | `feat(safety): add deterministic traversal transition state machine` | Nessun token residuo. |
| C-1 | `refactor(safety): model reachable descent affordances` | Test sweep/parità. |
| C-2 | `refactor(safety): retire duplicated traversal checks` | Solo dopo audit equivalenza. |
| D | `feat(cognitive): publish deduplicated traversal guidance` | I18n, priorità e NVDA. |

Ogni commit deve includere test pertinenti e non modificare file vietati. Un rollback usa `git revert` del commit atomico interessato. È vietato usare `git reset --hard`, riscrivere la storia o cancellare lavoro non correlato.

Prima di ogni commit: diff ristretto, test della fase, verifica esplicita dei file modificati e aggiornamento del report di implementazione. Il nome/versione di release resta fuori piano finché non viene approvato separatamente.

---

## 11. Matrice di accettazione e collaudo NVDA

| Scenario | Atteso |
|---|---|
| Tetto reale, approccio alla scala | Discesa agganciabile senza salto quando la fisica vanilla lo consente. |
| Stessa scena, moto verso il vuoto laterale | Sneak/protezione attivi; nessun falso corridoio. |
| Stop sul ciglio | Nessun permesso residuo; ritorno immediato a protezione se necessario. |
| Shift manuale | Mai forzato a falso. |
| Token sintetico | Rilasciato soltanto per candidato/corridoio valido. |
| Scala interrotta o lato errato | Nessuna discesa autorizzata. |
| Liana, impalcatura, botola, acqua | Solo percorsi validati dalla policy; lava mai sicura. |
| Evento critico durante guida | Protezione e feedback `CRITICAL` prevalgono. |
| Coordinatore spento | Un fallback legacy, non doppio. |
| Cambio mondo/GUI/acqua/disabilitazione | Stato e token puliti. |

Per ogni prova NVDA annotare: data, istanza, commit, scenario, input raw, token, stato macchina, affordance/candidato, esito atteso, esito osservato e regressione eventuale. Un deploy non è accettazione se questa tabella non è completata.

---

## 12. Definition of Done e stop finale

L'intervento è concluso quando tutte le condizioni seguenti sono vere:

1. una discesa valida può essere agganciata senza salto quando la fisica vanilla lo permette;
2. un precipizio laterale resta protetto anche con una scala vicina;
3. input fisico e token sintetico non sono confusi;
4. ogni stato e autorizzazione ha una via di uscita verificata;
5. il corridoio volumetrico è la fonte geometrica di verità per le decisioni migrate;
6. la guida cognitiva è deduplicata, localizzata e subordinata a `CRITICAL`;
7. tutti i test automatici e NVDA del piano sono positivi;
8. Fase 3B non ha subito modifiche;
9. report, diff e commit atomici consentono rollback sicuro.

**Stop obbligatorio:** questo documento non autorizza build, deploy, installazione della mod o modifiche alle istanze PrismLauncher. Dopo ciascun gate, attendere una nuova decisione esplicita dell'utente.
