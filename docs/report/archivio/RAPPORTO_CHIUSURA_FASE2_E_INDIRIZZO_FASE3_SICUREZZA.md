# Rapporto di chiusura Fase 2 e indirizzo per il piano tecnico della Fase 3

**Destinatario:** Antigravity  
**Ramo di lavoro:** `feat/cognitive-orchestrator`  
**Stato:** Fase 2 completata e convalidata; Fase 3 autorizzata esclusivamente per la pianificazione tecnica  
**Data:** 3 settembre 2026

---

## 1. Chiusura formale della Fase 2

La Fase 2 è **tecnicamente chiusa e convalidata**.

La contro-verifica ha confermato:

- categoria Cloth Config effettiva e accessibile, con soli valori già applicati dal nucleo;
- binding centralizzato e normalizzato per `cognitiveCoordinatorEnabled`, `chainedNarrationEnabled` e `deduplicationWindowMs`;
- registrazione del `CognitiveCoordinator` senza produttori migrati;
- facciata `NarrationPriority` retrocompatibile, con API storiche invariate;
- rimozione del catch-all che nascondeva errori degli scanner legacy;
- I18N IT/EN valide e ordinate;
- 22 test cognitivi (5 facciata, 3 configurazione, 14 nucleo) rieseguiti con esito positivo.

Non è autorizzato il merge in `mymaster`. Il ramo dedicato resta il solo perimetro consentito.

---

## 2. Mandato: redigere il piano tecnico della Fase 3

Il prossimo deliverable di Antigravity è il **piano tecnico implementativo completo della Fase 3 — migrazione pilota del dominio Sicurezza**. Non implementare alcun codice dopo averlo redatto: il piano dovrà ricevere revisione formale e consenso esplicito di Luca.

La Fase 3 deve dimostrare che il coordinatore può ricevere eventi reali senza alterare le protezioni esistenti, i controlli espliciti dell’utente o l’accessibilità NVDA. È una migrazione verticale, misurabile e reversibile: non un refactor globale dei sensori.

---

## 3. Strategia logica vincolante

### 3.1 Principio primario: conservare l’urgenza storica

Nel codice corrente `FallDetector.handleDangerDetected(...)` invia gli avvisi automatici con `MainClass.narrate(..., true)` sia nella zona di pre-allerta sia sul ciglio immediato. La prima migrazione non deve renderli meno tempestivi.

Di conseguenza, per il primo incremento:

- ogni **nuova** rilevazione automatica di caduta già annunciata storicamente come interrupt deve diventare un evento `SAFETY` con priorità `CRITICAL` e fast-path immediato;
- l’edge-bump ripetuto conserva il debounce storico di 1500 ms e resta critico fintanto che la sua voce storica usa interrupt;
- nessun evento critico viene concatenato, ritardato dallo shield di interazione o convertito in coda passiva;
- una futura distinzione fra pre-allerta contestuale e pericolo critico richiederà dati di collaudo NVDA e un piano separato: non appartiene alla Fase 3 pilota.

### 3.2 Fase 3 in due sotto-blocchi con gate interno

Il piano deve separare chiaramente due sotto-blocchi.

1. **Fase 3A — `FallDetector`, soli avvisi automatici.** È il primo verticale sicuro perché il rilevatore conosce già posizione, profondità, distanza, stato, debounce, testo localizzato e cue sonoro.
2. **Fase 3B — `ObstacleDetector`, soli avvisi automatici.** Può iniziare solo dopo test automatici e collaudo NVDA positivo della 3A. `ObstacleDetector` passa oggi da `CrosshairFeedbackManager.onObstacleDetected(...)`; il piano deve quindi spiegare come evitare doppie emissioni e come preservare l’arbitro locale finché il mirino non sarà migrato nella Fase 4.

Il completamento della 3A non autorizza automaticamente la 3B: costituisce un checkpoint tecnico interno da sottoporre a Luca.

### 3.3 Flussi esclusi: risposta diretta dell’utente invariata

Restano fuori dalla migrazione tutti i comandi espliciti, perché devono conservare latenza zero e controllo diretto:

- ispezione cadute `Alt+F`, inclusi “buca trovata” e “nessuna buca vicina”;
- toggle auto-sneak e relative conferme;
- ispezione panoramica ostacoli e orientamento visuale (`Alt+V`), inclusa l’eventuale rotazione della visuale;
- GUI, tastiera, Numpad, chat e altri comandi manuali.

Questi flussi restano su `MainClass.narrate` e sui cue storici. Non devono produrre `CognitiveEvent` in Fase 3.

### 3.4 Disciplina del produttore e configurazioni esistenti

`FallDetector` e, in 3B, `ObstacleDetector` mantengono per intero il filtro a monte. Il coordinatore non decide se un rilevatore sia abilitato, se la voce sia voluta o quale volume sia preferito.

Per ciascun punto automatico migrato, il piano deve stabilire una tabella esplicita:

| Configurazione del rilevatore | Output dell’evento | Risultato obbligatorio |
|---|---|---|
| Voce + cue attivi | `VOICE_AND_SOUND` | Testo e suono storici, volume invariato. |
| Solo voce | `VOICE_ONLY` | Nessun cue. |
| Solo cue | `SOUND_ONLY` | Nessuna narrazione vuota o fantasma. |
| Voce e cue disattivi / modulo disattivo | Nessun evento | Nessun output e nessun costo aggiuntivo. |

Non usare senza verifica le factory correnti di `CognitiveEvent` per il caso “solo cue”: esse non esprimono direttamente l’`OutputType` scelto dal produttore. Il piano deve indicare una factory o un costruttore dedicato che rappresenti fedelmente i quattro casi, senza stringhe vuote semantiche.

Quando `cognitiveCoordinatorEnabled` è `false`, ciascun produttore migrato deve eseguire **il proprio percorso storico diretto**, non un’approssimazione generica del fallback del coordinatore. Questo è particolarmente importante per `ObstacleDetector`, il cui percorso automatico oggi passa attraverso `CrosshairFeedbackManager`.

### 3.5 Contratto semantico per la caduta

Il piano 3A deve definire esattamente, senza deduzioni testuali:

- `SourceDomain.SAFETY`;
- chiavi semantiche stabili e non localizzate per nuova caduta ed edge-bump;
- `targetPos` uguale alla posizione del pericolo;
- `distance` uguale alla distanza già calcolata;
- `StateSignature` con bucket distanza, profondità/gravità e identificatore semantico quando necessario;
- `SpatialDirection` derivata dall’informazione direzionale già disponibile oppure impostata in modo conservativo senza abilitare concatenazioni;
- TTL coerente con il debounce attuale e `canChain=false` per tutti gli avvisi automatici critici;
- `SoundCue` con `SoundEvents.ANVIL_HIT`, sorgente, posizione, volume e pitch storici quando i cue sono abilitati.

L’uscita da un pericolo non deve introdurre una nuova frase “Percorso libero” in questo pilota: oggi `handleDangerCleared(...)` ripristina solo lo stato motorio. Ogni nuovo messaggio di recupero sarebbe una modifica percettiva e necessita di un requisito e una validazione separati.

### 3.6 Strategia speciale per gli ostacoli

Il piano 3B deve trattare `CrosshairFeedbackManager` come dipendenza di compatibilità, non come dettaglio eliminabile.

- Prima mappare con precisione cosa fa `onObstacleDetected(...)`, compresa l’eventuale fusione con il mirino.
- Con coordinatore attivo, l’avviso automatico dell’ostacolo deve avere **un solo produttore vocale**: o il nuovo evento cognitivo oppure l’arbitro locale, mai entrambi.
- Con coordinatore disattivo, eseguire il percorso storico completo, incluso `CrosshairFeedbackManager`.
- Mantenere invariati calcolo geometrico, movimento, `delay`, `detectionRange`, `NarrationStyle`, direzionalità, `voiceWarning`, `playAudioCues` e volume.
- La distinzione tra ostacolo superabile e bloccante deve essere rappresentata dalla firma di stato; non dedotta dal testo localizzato.

Non migrare `NarrateCrosshair` in 3B. La sua migrazione e la concatenazione ostacolo + mirino appartengono alla Fase 4.

### 3.7 Reset e assenza di stati fantasma

La Fase 3 non deve aggiungere hook globali di cambio mondo, morte o respawn: tale estensione richiede un piano dedicato se non è già disponibile nel nucleo. Il piano deve però garantire che i produttori non inviino eventi con riferimenti nulli e che un evento con TTL scaduto non venga emesso dopo un cambio di stato.

Eventuali notifiche di safety-clear devono limitarsi all’aggiornamento di memoria già supportato e solo se non producono nuovo parlato; altrimenti sono fuori perimetro.

---

## 4. Requisiti del piano tecnico da consegnare

Antigravity deve produrre un piano file-per-file che includa:

1. i punti esatti di emissione automatica in `FallDetector` e `ObstacleDetector`, con elenco separato dei flussi manuali esclusi;
2. la matrice evento/configurazione/priorità/output per ogni punto migrato;
3. la soluzione tecnica per preservare il fallback storico quando il coordinatore è disattivato;
4. il disegno dell’integrazione con `CrosshairFeedbackManager`, inclusa la prova dell’assenza di doppia voce;
5. i file Java, test e localizzazioni realmente necessari, con motivazione e invarianti;
6. un piano di test unitari con clock/delegate controllabili, senza avviare Minecraft;
7. scenari NVDA in-game, criteri di accettazione, checkpoint 3A→3B, rollback e commit piccoli;
8. un audit finale che dimostri che non sono stati migrati altri domini, mixin, GUI o comandi diretti.

Il piano non deve proporre nuove opzioni Cloth Config, refactor globali o modifiche a `mymaster`.

---

## 5. Matrice minima di verifica richiesta

### Test automatici

- nuova caduta: fast-path critico, testo e cue corretti;
- due critici concorrenti: primo interrupt, secondo accodato senza troncamento;
- stessa caduta invariata: deduplicazione; cambiamento di distanza/profondità: escalation annunciata;
- edge-bump: debounce storico preservato;
- tutte le combinazioni voce/cue/disabilitazione del rilevatore;
- `cognitiveCoordinatorEnabled=false`: percorso legacy verificato, senza duplicati;
- 3B: ostacolo superabile e bloccante, cambio stato/posizione, ritardo, direzione e assenza di doppia emissione con `CrosshairFeedbackManager`;
- TTL e buffer vuoti dopo invalidazione dello stato.

### Collaudo manuale NVDA

- avvicinamento e permanenza sul ciglio, con e senza auto-sneak e slowdown;
- caduta profonda, scalini, buche vicine e ritorno a terreno sicuro;
- ostacolo frontale, laterale, superabile e bloccante durante movimento;
- mirino attivo durante ostacolo, verificando che non compaiano doppie frasi;
- disabilitazione del coordinatore e confronto diretto con il comportamento storico.

---

## 6. Checkpoint

Antigravity deve ora redigere il piano tecnico della Fase 3 e fermarsi. L’implementazione potrà iniziare soltanto dopo revisione positiva e comando esplicito di Luca, ad esempio:

> **“Approvo il piano della Fase 3: implementa prima la 3A sul ramo dedicato.”**

