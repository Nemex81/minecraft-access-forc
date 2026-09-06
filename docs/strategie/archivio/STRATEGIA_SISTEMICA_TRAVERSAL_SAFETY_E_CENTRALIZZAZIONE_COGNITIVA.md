# Strategia sistemica — Traversal Safety, discesa assistita e integrazione cognitiva

**Ramo di riferimento:** `feat/cognitive-orchestrator`  
**Baseline tecnica:** `d60c234acab2126250a3997de84b0699cb99a01f` — Rev. MC-26.8  
**Stato:** `[DOCUMENTO DI INDIRIZZO — NON IMPLEMENTARE SENZA APPROVAZIONE ESPLICITA]`  
**Ambito:** sicurezza di attraversamento verticale, ownership dello sneak, feedback cognitivo di discesa.  

---

## 1. Decisione strategica

La discesa da scale, liane, impalcature, botole e atterraggi in acqua non deve essere trattata come un'eccezione al rilevatore di cadute. È un distinto caso di **attraversamento verticale intenzionale**, composto da:

1. percezione geometrica del mondo e della hitbox;
2. decisione fisica fail-safe sullo sneak;
3. assistenza cognitiva non invasiva;
4. arbitraggio centrale di voce e audio.

Il `CognitiveCoordinator` resta il centro dell'attenzione e dell'output. Non acquisisce input, non effettua raycast e non decide se un passo è sicuro. La sicurezza fisica resta locale al dominio `features.safety.traversal`; il coordinatore riceve solo osservazioni semantiche già validate.

Questa scelta evita sia un coordinatore onnisciente sia logiche di sicurezza duplicate nei vari rilevatori.

---

## 2. Evidenza consolidata e correzione del modello mentale

Il collaudo reale ha mostrato che una scala viene riconosciuta come discesa sicura, ma il giocatore può restare bloccato sul ciglio e riuscire a scendere solo saltando.

Nella scena misurata, la scala occupa il blocco `X = -60`, quindi il suo volume orizzontale è `[-60, -59]` e il centro è `-59,5`. Con il giocatore a `X = -59,28`, l'offset dal centro è circa 22 cm; la hitbox standard di 60 cm ha già una sovrapposizione laterale con la cella della scala. Il problema non è quindi, in primo luogo, una scala mancata di 72 cm.

Le cause da trattare sono quattro:

1. `SafetyMovementGuard` legge `keyShift.isDown()` come se fosse input fisico, ma lo stesso guard può avere impostato quel valore tramite `setDown(true)`.
2. L'autorizzazione alla discesa può rimanere memorizzata oltre il candidato che l'ha giustificata.
3. Il presidio statico del ciglio può riapplicare lo sneak mentre la discesa deve agganciarsi al volume della scala.
4. `TraversalSafetyAnalyzer` usa un campione rigido a 0,55 m e non modella ancora un corridoio di attraversamento né tutte le collisioni che rendono un varco realmente raggiungibile.

Il deadlock dello Shift è fortemente coerente con codice e collaudo, ma viene considerato definitivamente certificato solo dopo un test che osservi contemporaneamente input grezzo, token di sistema, stato effettivo di crouch, stato di transizione e `onClimbable()`.

---

## 3. Invarianti non negoziabili

1. **Fail-safe fisico:** una discesa può revocare l'override soltanto dentro un corridoio specifico, immediatamente raggiungibile e validato. Una scala vicina non rende sicuro il dirupo laterale.
2. **Ownership esplicita:** il mod può rimuovere solo il proprio override; non deve mai cancellare una pressione manuale dell'utente.
3. **CRITICAL prevale:** un pericolo reale continua ad attivare protezione fisica e feedback critico, senza dipendere dalla coda cognitiva.
4. **Nessuna eco:** ogni messaggio o cue di discesa è deduplicato per candidato e transizione; non deve ripetersi ad ogni tick.
5. **Nessuna dipendenza ciclica:** traversal non dipende da `ObstacleDetector`, mirino o coordinatore per decidere la geometria.
6. **Ciclo di vita pulito:** cambio mondo/dimensione, morte, disconnessione, GUI incompatibile, acqua e disabilitazione della funzione revocano token e stato transitorio.
7. **Fase 3B congelata:** nessuna modifica a `ObstacleDetector`, `NarrateCrosshair` o ad altri rilevatori durante questa strategia.

---

## 4. Architettura di destinazione

```text
Snapshot client/input/mondo
        │
        ▼
TraversalPerception (puro)
  ├─ pericolo/corridoio ordinario
  └─ DescentAffordance validata
        │
        ▼
TraversalStateMachine (pura)
  ├─ GuardDecision per SafetyMovementGuard
  └─ TraversalObservation per il confine cognitivo
        │                         │
        ▼                         ▼
SafetyMovementGuard      TraversalCognitivePublisher
  │                       └─ CognitiveEvent
  ▼                               │
SneakOverridePort                ▼
fisica Minecraft          CognitiveCoordinator
                                   │
                             voce e audio 3D
```

### 4.1 Componenti e responsabilità

| Componente | Responsabilità | Non deve fare |
|---|---|---|
| `TraversalPerception` | Analizzare il volume di attraversamento, collisioni, scala/liana/impalcatura/acqua e pericoli. | Scrivere input, narrare, mutare stato globale. |
| `DescentAffordance` | Descrivere un singolo varco: volume di ingresso/aggancio, direzione, supporto, atterraggio, anchor audio e offset laterale. | Decidere policy o scadenze. |
| `TraversalStateMachine` | Decidere transizioni, scadenze, revoche e ordini al guard. | Leggere direttamente Minecraft o creare audio. |
| `SafetyMovementGuard` | Applicare e revocare esclusivamente il token sintetico di crouch. | Analizzare voxel o attribuire priorità cognitive. |
| `RawCrouchIntentProvider` | Esporre input utente non contaminato dall'override. | Scrivere verso il client. |
| `SneakOverridePort` | Applicare l'effetto effettivo nel client, in un solo punto. | Stabilire se il mondo è sicuro. |
| `TraversalCognitivePublisher` | Tradurre osservazioni validate in `CognitiveEvent`. | Riaprire/chiudere il token di sicurezza. |
| `CognitiveCoordinator` | Deduplicare, dare priorità e produrre output. | Guidare il movimento o interpretare collisioni. |

`FallDetector` resta l'adattatore locale: costruisce lo snapshot, invoca percezione e macchina a stati, applica `GuardDecision` e pubblica le osservazioni. Il suo fallback storico rimane temporaneamente attivo, ma ogni regola geometrica duplicata deve avere un piano di rimozione.

---

## 5. Contratti dati modulari

I nomi sono proposti; l'implementazione deve aderire alle convenzioni Java esistenti.

### 5.1 Snapshot e percezione

```text
TraversalSnapshot
  playerAabb, playerFeet, onGround, onClimbable
  movementIntent, horizontalVelocity, rawCrouchHeld
  world/collision probe, danger threshold, clock

DescentAffordance
  candidateId, descentType, entryVolume, attachmentVolume
  approachVector, safeSupportVolume, landingVolume
  anchorPosition, lateralOffset, confidence/reason

TraversalAssessment
  hazard assessment, optional affordance, diagnostic reason
```

La percezione campiona il volume della hitbox traslato lungo il movimento previsto, non un solo punto. Una affordance è valida quando il volume spazzato può entrare nel volume di aggancio senza attraversare una collisione insormontabile e senza uscire dal corridoio sicuro.

L'acqua richiede una policy esplicita: volume effettivamente raggiungibile, assenza di lava, profondità/uscita accettabili e nessun ostacolo che renda l'atterraggio solo apparente. Una scala deve includere faccia di aggancio e supporto; la direzione di avvicinamento non può restare un dato inutilizzato.

### 5.2 Input e ownership

```text
RawCrouchIntentProvider  -> stato fisico o di binding non sintetico
SneakOverridePort        -> applicazione dell'override nel client
SystemCrouchToken        -> possesso esclusivo di SafetyMovementGuard
```

L'implementazione della tastiera può usare GLFW, ma GLFW non deve entrare nella logica di policy. L'adapter deve poter essere sostituito per controller, binding diversi e test. Alla revoca, il guard rimuove il proprio token; non deduce l'input umano da `KeyMapping.isDown()` dopo averlo alterato.

### 5.3 Macchina a stati

| Stato | Significato | Uscite principali |
|---|---|---|
| `SAFE` | Nessun rischio o supporto sufficiente. | `EDGE_PROTECTED`, `DESCENT_ARMED`, `CLIMBING` |
| `EDGE_PROTECTED` | Cigli pericolosi: token di sistema attivo. | `SAFE`, `DESCENT_ARMED` |
| `DESCENT_ARMED` | Affordance rilevata, ma non ancora pronta al rilascio. | `EDGE_PROTECTED`, `DESCENT_LATCHING` |
| `DESCENT_LATCHING` | Token rilasciato soltanto nel corridoio di aggancio. | `CLIMBING`, `EDGE_PROTECTED`, `SAFE` |
| `CLIMBING` | Il client conferma `onClimbable()`. | `SAFE` o nuova valutazione di bordo alla fuoriuscita |

`DESCENT_LATCHING` non è un lasciapassare temporale. Deve superare, ogni tick:

- intenzione ancora compatibile con il corridoio;
- hitbox attuale e prevista ancora dentro il budget spaziale dell'affordance;
- nessun nuovo pericolo critico incompatibile;
- candidato invariato e raggiungibile;
- deadline breve, inizialmente massimo 8 tick **e** 400 ms, oltre a una distanza massima configurata internamente.

Al primo fallimento la macchina emette `REENGAGE_PROTECTION`. Non attende la scadenza. Il timeout impedisce stati appesi; la validazione spaziale impedisce di concedere metri di movimento nel vuoto.

---

## 6. Integrazione con il sistema cognitivo centralizzato

La sicurezza fisica e la priorità comunicativa sono due decisioni distinte.

| Osservazione | Dominio/priority | Effetto fisico | Output cognitivo |
|---|---|---|---|
| Caduta reale | `SAFETY / CRITICAL` | Protezione immediata. | Voce/audio critici, fast path. |
| Discesa validata | `GUIDANCE / CONTEXTUAL` o `SAFETY / OPERATIONAL` solo su richiesta esplicita. | Eventuale ingresso in `DESCENT_LATCHING`. | Una sola conferma deduplicata e cue 3D. |
| Allineamento da correggere | `GUIDANCE / CONTEXTUAL` | Nessun rilascio del token. | "Scala leggermente a sinistra/destra" solo oltre soglia. |
| Discesa agganciata | Normalmente silente. | Stato `CLIMBING`. | Nessuna ripetizione automatica. |
| Transizione revocata | Normalmente silente. | Riattivazione token. | Solo il pericolo critico, se necessario. |

L'offset laterale viene calcolato rispetto al centro e alla faccia utile della scala, non rispetto all'origine intera del blocco. La soglia iniziale di guida è 25–30 cm, da validare nel collaudo NVDA. Il cue `LADDER_STEP` deve essere ancorato al piolo/candidato; è contestuale, deduplicato e interrompibile da un evento `CRITICAL`.

`DirectInteractionShield` può filtrare la guida contestuale, ma non deve ritardare un allarme critico né alterare il guard.

---

## 7. Piano incrementale e reversibile

### Fase A — Correzione immediata dell'ownership

**Obiettivo:** rimuovere il deadlock e le autorizzazioni residue senza cambiare ancora la geometria.

- Introdurre adapter di input grezzo e porta di override.
- Far possedere il token esclusivamente a `SafetyMovementGuard`.
- Revocare l'autorizzazione di discesa a fermo, cambio candidato, reset di ciclo di vita e uscita dal corridoio.
- Centralizzare le scritture verso sneak in un unico punto.
- Aggiungere telemetria `debug` non narrata: input grezzo, token, stato effettivo, candidato, stato e motivo della transizione.

**Criterio di uscita:** una scala già validata rilascia il solo override sintetico; uno Shift umano rimane invariato.

### Fase B — Macchina a stati con l'affordance attuale

**Obiettivo:** eliminare l'isteresi tick-by-tick del presidio statico.

- Introdurre stati, eventi di transizione e `GuardDecision` puri.
- Usare temporaneamente l'attuale `SafeDescentCandidate` come affordance minima.
- Definire tutte le uscite da `CLIMBING`, reset e fallback.

**Criterio di uscita:** arresto, deviazione o timeout non lasciano token/permessi residui; il candidato corretto arriva a `onClimbable()` senza salto quando la fisica vanilla lo consente.

### Fase C — Corridoio volumetrico e fonte di verità unica

**Obiettivo:** sostituire il punto fisso a 0,55 m e ridurre geometria duplicata.

- Introdurre `DescentAffordance` e percezione swept-volume.
- Validare collisioni, parete della scala, direzione, ingresso e atterraggio.
- Confrontare in test il nuovo esito col fallback storico.
- Eliminare gradualmente i rami legacy solo dopo parità dimostrata.

**Criterio di uscita:** scala raggiungibile non viene bloccata; scala falsa o dirupo laterale non viene promosso a corridoio sicuro.

### Fase D — Guida cognitiva e adozione progressiva

**Obiettivo:** offrire centratura utile, senza chatter né accoppiamento.

- Aggiungere `TraversalCognitivePublisher` e chiavi semantiche/i18n.
- Introdurre cue 3D e guida laterale sopra soglia.
- Verificare deduplica, schermatura da `CRITICAL` e `DirectInteractionShield`.
- Documentare il contratto affinché futuri rilevatori possano pubblicare osservazioni standardizzate.

**Criterio di uscita:** il coordinatore riceve dati semantici coerenti; `ObstacleDetector` resta invariato.

---

## 8. Matrice obbligatoria di test

### Unitari puri

- hitbox sovrapposta alla scala nel caso reale; 
- scala accessibile, scala sul lato errato, scala interrotta e liana/impalcatura;
- varco con dirupo laterale: approccio verso scala consentito, approccio verso vuoto negato;
- acqua sicura, acqua insufficiente, lava e falso atterraggio;
- transizioni complete, timeout temporale/spaziale e tutte le uscite da `CLIMBING`;
- deduplica dell'osservazione e priorità `CRITICAL`.

### Integrazione controllata

- Shift fisico assente/presente con token sintetico attivo;
- arresto sul ciglio, deviazione, cambio candidato e reset ciclo di vita;
- ordine del tick: percezione → decisione → guard → pubblicazione;
- evento critico che interrompe una guida senza ritardare la protezione;
- nessuna narrazione diretta quando il coordinatore è attivo.

### Collaudo NVDA nel mondo reale

- scena del tetto con scala `(-60, 84, -42)` e dirupo laterale;
- avvicinamento frontale, all'indietro e laterale;
- con e senza Shift manuale;
- fermo al bordo, salto volontario, aggancio riuscito e abbandono della scala;
- ascolto del cue 3D e verifica che non vi siano istruzioni grossolane o ripetitive.

Ogni difetto riscontrato deve essere annotato con stato della macchina, input grezzo, token, affordance, posizione e motivo della transizione; non è accettabile correggere solo la scena della tenuta senza una regola generalizzabile.

---

## 9. Rischi e decisioni esplicite

| Rischio | Contromisura |
|---|---|
| Falso varco verso un burrone laterale | Corridoio/volume raggiungibile, non sola vicinanza della scala. |
| Sneak che non si rilascia | Token distinto dall'input grezzo; test di ownership. |
| Shift manuale annullato | Il guard revoca solo il token del sistema. |
| Stato appeso | Deadline in tick e tempo, budget spaziale, reset di ciclo di vita. |
| Regressione da logiche duplicate | Confronto temporaneo col fallback, rimozione soltanto dopo test di parità. |
| Chatter cognitivo | Eventi semantici deduplicati, guida contestuale, silenzio sulle transizioni normali. |
| Estensione prematura ad altri rilevatori | Congelamento Fase 3B e contratto riusabile prima della migrazione. |

---

## 10. Definition of Done

L'intervento è concluso soltanto quando:

1. il player può agganciare una discesa valida senza dover saltare, quando la fisica vanilla lo permette;
2. la protezione non apre un varco verso un dirupo laterale;
3. il token sintetico non viene confuso con Shift fisico;
4. tutte le transizioni hanno uscita e reset verificati;
5. il coordinatore riceve esclusivamente eventi semantici e mantiene la precedenza `CRITICAL`;
6. i test unitari, di integrazione e NVDA descritti sopra sono superati;
7. Fase 3B resta invariata;
8. ogni rimozione di fallback è motivata da una parità di comportamento dimostrata.

---

## 11. Decisione operativa successiva

Il primo cambiamento ammissibile è la **Fase A**. Non è autorizzata alcuna migrazione di altri rilevatori prima della sua verifica. Dopo la Fase A, si rivalutano telemetria e collaudo reale prima di scegliere se procedere con Fase B.
