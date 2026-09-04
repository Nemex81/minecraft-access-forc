# Piano Tecnico Implementativo — Rev. MC-26.8: Traversal Safety Analyzer

**Ramo di lavoro:** `feat/cognitive-orchestrator`  
**Ambito:** affinamento strutturale della Fase 3A — dominio Sicurezza / `FallDetector`  
**Stato:** `[PIANO FORMALE — STOP OBBLIGATORIO REGOLA 0 — NON IMPLEMENTARE PRIMA DELL'APPROVAZIONE]`  
**Riferimenti:**
- `docs/report/RAPPORTO_INDIRIZZO_CORRETTIVO_TRAVERSAL_SAFETY_ANALYZER.md`
- `docs/piani/attivi/PIANO_TECNICO_FASE3_MIGRAZIONE_SICUREZZA.md`
- `docs/report/RAPPORTO_CORRETTIVO_FASE3A_FALLDETECTOR.md`

---

## 0. Decisione e perimetro

Il test NVDA sul tetto ha confermato un difetto funzionale: una scala a parete è riconosciuta da un sistema come elemento di mobilità, ma il `FallDetector` classifica il suo ingresso come burrone e blocca fisicamente il giocatore con lo sticky-sneak.

La correzione non consiste in un'eccezione locale né nella lettura del solo tasto `W`. Si introduce un sottocomponente del dominio Sicurezza, puro e riutilizzabile, denominato **`TraversalSafetyAnalyzer`**, insieme a un piccolo controllore di override, **`SafetyMovementGuard`**.

### Entro il perimetro

- `FallDetector`, stato del giocatore, geometria del mondo e tag vanilla/modded necessari;
- nuovo analizzatore e relativi DTO puri;
- guard di movimento limitato alla proprietà dell'auto-sneak del `FallDetector`;
- integrazione di un solo feedback semantico, deduplicato, con il `CognitiveCoordinator`;
- test unitari, di integrazione e collaudo NVDA.

### Fuori perimetro

- nessuna modifica a `ObstacleDetector`, `CrosshairFeedbackManager`, `NarrateCrosshair` o altri produttori della Fase 3B, che resta congelata;
- nessuna nuova GUI/configurazione, mixin, comando, modifica del protocollo o automazione di movimento;
- nessuna narrazione diretta nuova quando il coordinatore è attivo;
- nessuna manipolazione di account, mondi, istanze o configurazioni dell'utente.

---

## 1. Diagnosi tecnica consolidata

Nel `FallDetector` attuale:

1. `isStandingOnDangerousEdge(...)` tratta otto campioni radiali indipendenti come prova di pericolo. Un solo campione nel vuoto laterale attiva lo sneak, anche se il corridoio frontale conduce a una scala.
2. La catena di look-ahead può interpretare una scala come collisione/barriera prima che la logica degli arrampicabili venga consultata.
3. Il tentativo locale di discesa sicura usa la posizione sotto il candidato anche quando l'arrampicabile è nella cella superiore; non modella parete, direzione, aggancio o corridoio della hitbox.
4. `tick(...)` riapplica lo sneak a ogni ciclo se `autoSneakActive`; un rilascio puntuale non sarebbe stabile.
5. Il rilascio corrente dello Shift non distingue l'override del mod dall'input volontario dell'utente.
6. La semantica attuale dei fluidi è troppo ampia: un fluido non è automaticamente un atterraggio sicuro.

La causa radice è quindi l'assenza di una valutazione unitaria di **attraversamento verticale intenzionale**. Il valore numerico della profondità non deve essere alterato per far passare una scala: la discesa sicura deve diventare un esito distinto.

---

## 2. Architettura di destinazione

```text
FallDetector (orchestrazione locale di sicurezza)
        │ crea uno snapshot senza effetti collaterali
        ▼
TraversalSafetyAnalyzer (puro)
        │ TraversalSafetyResult
        ├───────────────┐
        ▼               ▼
SafetyMovementGuard   CognitiveCoordinator
protezione fisica     feedback semantico deduplicato
```

### 2.1 Responsabilità

| Componente | Responsabilità | Non deve fare |
|---|---|---|
| `TraversalSafetyAnalyzer` | Classificare il corridoio di attraversamento e validare candidati di discesa. | Scrivere tasti, narrare, suonare, modificare il mondo. |
| `SafetyMovementGuard` | Applicare/revocare il solo override di crouch posseduto dal mod. | Decidere geometria o generare eventi cognitivi. |
| `FallDetector` | Acquisire snapshot, delegare l'analisi, mantenere fallback e avvisi di caduta. | Duplicare la geometria specialistica o consultare direttamente `ObstacleDetector`. |
| `CognitiveCoordinator` | Arbitrare eventuale messaggio di discesa sicura con gli altri eventi. | Stabilire che una scala sia fisicamente sicura. |

L'analizzatore è estendibile a future osservazioni standardizzate di ostacoli, ma questa revisione non chiama né modifica l'`ObstacleDetector`. Nessuna dipendenza ciclica è ammessa.

---

## 3. Contratti dati proposti

Creare nel package di sicurezza scelto dopo verifica delle convenzioni del repository (preferenza: `features.safety.traversal`, oppure package interno a `features` se la struttura attuale lo impone):

1. `TraversalSafetyStatus`:
   - `DANGEROUS_DROP`
   - `SAFE_DESCENT_AVAILABLE`
   - `AMBIGUOUS_OR_UNSAFE_DESCENT`
   - `NOT_APPLICABLE`

2. `TraversalSafetyContext` immutabile, costruito dal bordo client:
   - posizione e bounding box del giocatore;
   - quota piedi, vettore di intento/moto orizzontale normalizzato, presenza di intento;
   - soglia di caduta configurata;
   - facciata read-only del mondo per blocchi e fluidi;
   - limite verticale configurabile internamente e finito (non scansioni illimitate).

3. `SafeDescentCandidate` immutabile:
   - posizione di ingresso, posizione della colonna, tipo (`LADDER`, `VINE`, `SCAFFOLDING`, `TAGGED_CLIMBABLE`, `WATER_DESCENT` se validato);
   - direzione di ingresso/parete, posizione di atterraggio e identità stabile della colonna;
   - nessun riferimento mutabile al client o al mondo.

4. `TraversalSafetyResult` immutabile:
   - stato;
   - candidato opzionale solo per `SAFE_DESCENT_AVAILABLE`;
   - pericolo opzionale con posizione/profondità reale per `DANGEROUS_DROP`;
   - ragione diagnostica solo per test/log di sviluppo, senza testo destinato all'utente.

**Invariante:** `SAFE_DESCENT_AVAILABLE` non equivale a `drop = 0`; il pericolo resta misurabile e viene soppresso solo lungo il corridoio validato.

---

## 4. Algoritmo del TraversalSafetyAnalyzer

### 4.1 Acquisizione e selezione del corridoio

1. Se non esiste intenzione/vettore di moto, l'analizzatore non concede una discesa: restituisce `NOT_APPLICABLE`. Il presidio statico del ciglio resta conservativo.
2. Costruire un corridoio corto nella direzione dell'intento dal volume reale del giocatore: cella corrente, bordo di uscita, cella d'ingresso, celle inferiore/superiore e blocchi adiacenti alla parete. Non usare soltanto punti radiali.
3. Individuare i candidati arrampicabili nel corridoio prima della classificazione generica come barriera o vuoto.
4. Ordinare i candidati per raggiungibilità fisica e allineamento con l'intento; non per sola prossimità di blocco.

### 4.2 Validazione di un candidato

Un candidato è valido solo se tutte le condizioni seguenti sono vere:

1. È `BlockTags.CLIMBABLE` o una forma vanilla esplicitamente supportata; il tag è il percorso preferenziale per mod compatibili.
2. La hitbox può entrare/agganciarsi al candidato dal corridoio attuale, senza attraversare una barriera solida.
3. Per una scala, sono coerenti faccia, parete di supporto e direzione d'ingresso. Una scala dietro il giocatore o su una parete non raggiungibile non è un candidato.
4. La colonna prosegue fino a un atterraggio verificato oppure a una transizione d'acqua validata. Una colonna interrotta con caduta oltre la soglia configurata restituisce `AMBIGUOUS_OR_UNSAFE_DESCENT` o `DANGEROUS_DROP`.
5. Una botola viene trattata come geometria di accesso esclusivamente se è aperta e collegata al candidato già valido. Non è mai una whitelist autonoma.
6. L'acqua viene valutata esplicitamente come acqua e con volume/profondità sufficienti; lava e fluidi non riconosciuti non diventano sicuri per la sola presenza del fluido.

### 4.3 Esito e principio fail-safe

- Candidato valido e intento diretto verso l'ingresso → `SAFE_DESCENT_AVAILABLE`.
- Nessun candidato ma caduta oltre soglia → `DANGEROUS_DROP`.
- Candidato incompleto, ambiguo, irraggiungibile o percorso non dimostrabile → `AMBIGUOUS_OR_UNSAFE_DESCENT` e protezione mantenuta.
- Nessun elemento di transito pertinente → `NOT_APPLICABLE`; il `FallDetector` applica il proprio flusso storico.

In caso di errore o informazione incompleta dell'ambiente, l'analizzatore non autorizza mai il rilascio dell'auto-sneak.

---

## 5. SafetyMovementGuard e proprietà dell'input

### 5.1 Contratto

Il guard espone operazioni intenzionali, non accesso sparso a `keyShift.setDown(...)`:

- `engageFallProtection(...)`
- `allowValidatedDescent(...)`
- `clearSystemOverride(...)`
- `isSystemOverrideActive()`

Il guard è l'unico punto della revisione autorizzato a scrivere l'override di crouch. `FallDetector` non deve più impostare o azzerare Shift in più rami indipendenti.

### 5.2 Intenzione manuale

Prima dell'implementazione Antigravity deve verificare l'API di input della versione Minecraft/Balm corrente. Il piano richiede un adattatore testabile, ad esempio `CrouchIntentProbe`, con implementazione client che usa il keybind effettivamente configurato e non una costante di tastiera.

- Se il probe identifica in modo affidabile un input fisico/manuale, il guard non deve rilasciarlo.
- Se il probe non può identificare con affidabilità un binding o dispositivo, prevale il fail-safe: il guard conserva la protezione e non finge di sapere che lo Shift sia manuale.
- È vietato dichiarare supporto controller/mouse senza verifica concreta dell'API e test manuale; la compatibilità deve essere esplicitamente documentata nel resoconto d'implementazione.

`allowValidatedDescent(...)` deve revocare soltanto l'override del mod, solo mentre il candidato rimane valido e l'intento punta verso di esso. Un tick successivo non deve riapplicare lo sneak per lo stesso corridoio validato.

---

## 6. Integrazione in FallDetector

1. Conservare `checkLookAheadSafety(...)` quale punto di orchestration, ma estrarre la costruzione dell'intento già esistente in un dato riusabile.
2. Prima di `findDangerAhead(...)` e prima del presidio statico che possa coinvolgere il corridoio d'ingresso, interrogare l'analizzatore solo quando esiste intento. L'analisi non deve raddoppiare scansioni del mondo non necessarie.
3. Per `SAFE_DESCENT_AVAILABLE`:
   - invocare il guard per consentire il passaggio;
   - evitare `handleDangerDetected(...)` esclusivamente per quel corridoio;
   - mantenere attivi gli avvisi per ogni altro bordo o vettore non validato.
4. Per `DANGEROUS_DROP` e `AMBIGUOUS_OR_UNSAFE_DESCENT`, mantenere o attivare il percorso di protezione del FallDetector.
5. Sostituire gradualmente i blocchi locali di riconoscimento arrampicabili in `calculateDangerousDrop(...)` solo dopo che i nuovi test coprono le stesse situazioni. Non lasciare due policy geometriche concorrenti.
6. Non cambiare i flussi manuali `Alt+F`, toggle auto-sneak, scansione ambientale e le loro narrazioni storiche.

---

## 7. Feedback cognitivo e compatibilità

La disponibilità della discesa può produrre un solo evento quando cambia lo stato o l'identità della colonna.

- Factory: usare `CognitiveEvent.createSafetyAlert(...)` esistente, salvo evidenza di una lacuna reale.
- Chiave semantica proposta: `safety.traversal.safe_descent`; chiave I18n distinta e localizzata in italiano/inglese.
- Dominio: `SAFETY`; priorità: `OPERATIONAL` (mai `CRITICAL`), perché informa un passaggio sicuro e non deve interrompere un allarme reale.
- Firma: identità stabile della colonna/tipo/direzione; non testo localizzato né coordinate volatili non necessarie.
- Output: rispettare preferenze vocali/audio esistenti. Nessuna nuova voce se entrambi sono disabilitati.
- Deduplicazione: transizione/cambio candidato, nessuna emissione per tick.
- Con coordinatore disabilitato: fallback legacy localizzato e con stesso anti-spam; non introdurre doppie emissioni.

Prima di emettere il feedback, l'implementazione deve verificare che un evento `OPERATIONAL` non riduca, ritardi o concateni un imminente evento `CRITICAL` di caduta.

---

## 8. File previsti e file vietati

### Previsti

- Nuovi file sotto il package selezionato per analizzatore, DTO e guard/adattatore.
- `FallDetector.java` per orchestration e rimozione della policy duplicata.
- `CognitiveEvent.java` solo se una factory esistente non è sufficiente, con motivazione nel commit.
- Risorse I18n italiano/inglese per il solo feedback approvato.
- Nuovi test unitari e d'integrazione della sicurezza.
- Report di implementazione e di collaudo.

### Vietati

- `ObstacleDetector.java`, `CrosshairFeedbackManager.java`, `NarrateCrosshair.java`;
- mixin, GUI, comandi e configurazioni pubbliche, salvo nuova autorizzazione scritta;
- modifiche opportunistiche, formattazioni globali o rifattorizzazioni estranee.

---

## 9. Piano di test obbligatorio

### 9.1 Test puri del classificatore

1. Caso di regressione: tetto, scala a parete, vuoto laterale, atterraggio quattro blocchi sotto; esito `SAFE_DESCENT_AVAILABLE` con intento verso la scala.
2. Stesso tetto senza scala; esito `DANGEROUS_DROP`.
3. Scala interrotta o non raggiungibile; nessuna autorizzazione alla discesa.
4. Movimento diagonale/parallelo o in direzione opposta; nessuna autorizzazione.
5. Scala con orientamento/parete incompatibile; nessuna autorizzazione.
6. Liana, impalcatura e blocco modded taggato `CLIMBABLE`.
7. Botola chiusa, botola aperta senza scala e botola aperta con percorso valido.
8. Acqua validata, lava e fluido non classificato.
9. Atterraggio solido, atterraggio non sicuro e soglia di caduta configurabile.

### 9.2 Test del guard

1. Attivazione e rilascio dell'override posseduto dal sistema.
2. Rilascio autorizzato su discesa valida e assenza di riapplicazione al tick successivo.
3. Preservazione dell'intento manuale quando il probe lo può dimostrare.
4. Fallback fail-safe quando il probe non fornisce una risposta affidabile.
5. Reset su detector disabilitato, GUI aperta, acqua/nuoto e uscita effettiva dal pericolo.

### 9.3 Test di integrazione

1. `FallDetector` domanda il classificatore prima della policy di bordo e conserva gli avvisi critici per il bordo non validato.
2. Evento cognitivo di discesa emesso una volta per colonna e correttamente deduplicato.
3. Evento `CRITICAL` di caduta conserva fast-path, ordine audio/voce e fallback legacy della Fase 3A.
4. Coordinatore disattivato: un solo feedback legacy, nessun doppio output.
5. Verifica statica che i file della Fase 3B non siano modificati.

### 9.4 Collaudo NVDA obbligatorio

- scenario reale del tetto con scala;
- scala in miniera, liana, impalcatura e botola;
- bordo senza percorso sicuro;
- scala deliberatamente interrotta;
- movimento frontale, laterale e indietro;
- Shift manuale durante e dopo una protezione;
- alternanza rapida tra corridoio sicuro e ciglio reale;
- voce/cue attivi, solo voce, solo cue e entrambi disattivi.

Il collaudo deve registrare data, istanza, scenario, esito atteso, esito osservato e regressioni percepite. Il deploy non costituisce accettazione senza tale verifica.

---

## 10. Sequenza di implementazione e gate

1. **Gate A — revisione del piano:** Antigravity conferma che package, API di input e proprietà Minecraft sono effettivamente disponibili. Se manca una capacità, aggiorna il piano senza aggirarla.
2. **Passo 1:** aggiungere DTO e test puri del classificatore, tutti rossi/verdi in modo deterministico.
3. **Passo 2:** implementare classificatore puro e copertura completa dei casi geometrici.
4. **Passo 3:** introdurre guard e probe con test ownership/fail-safe.
5. **Passo 4:** integrare nel `FallDetector`, eliminando la policy duplicata soltanto dopo equivalenza di test.
6. **Passo 5:** aggiungere feedback cognitivo, risorse I18n e test di deduplicazione/fallback.
7. **Passo 6:** test mirati, suite completa, revisione diff e verifica che Fase 3B sia immutata.
8. **Gate B — deploy controllato e collaudo NVDA:** nessuna chiusura della revisione senza esito manuale positivo.

Ogni passo deve avere commit atomico e reversibile. Un eventuale rollback usa `git revert` del commit specifico, non `reset --hard`.

---

## 11. Criteri di accettazione finali

- Il giocatore può intenzionalmente agganciare e scendere dalla scala del test reale senza disabilitare la protezione globale.
- Lo stesso bordo, privo di discesa valida o con percorso interrotto, resta protetto e segnala il pericolo con la semantica Fase 3A invariata.
- Nessun falso varco su movimento laterale, diagonale o su scala non raggiungibile.
- Il mod non annulla lo Shift volontario dell'utente; in caso di incertezza input, sceglie il fail-safe.
- Nessuna lava/fluidità generica è trattata come atterraggio sicuro.
- Il feedback di discesa è localizzato, non invasivo, deduplicato e subordinato a un pericolo critico.
- Fase 3B è intatta; suite automatica e collaudo NVDA sono positivi.

---

## 12. Stop obbligatorio

Questo documento autorizza esclusivamente la pianificazione. Antigravity deve fermarsi qui, verificare la fattibilità delle API proposte e attendere l'approvazione esplicita di Luca e la revisione finale di ChatGPT prima di modificare qualsiasi sorgente.
