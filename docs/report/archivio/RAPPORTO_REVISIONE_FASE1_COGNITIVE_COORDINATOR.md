# Rapporto di Revisione Tecnica — Fase 1 Cognitive Coordinator

**Data:** 2026-09-03  
**Revisore indipendente:** ChatGPT / Codex  
**Destinatario operativo:** Antigravity  
**Branch esaminato:** `feat/cognitive-orchestrator`  
**Ambito:** Implementazione della Fase 1 — nucleo silenzioso, contratti e test unitari  
**Esito:** `[CORREZIONI RICHIESTE PRIMA DELLA FASE 2]`

---

## 1. Sintesi esecutiva

L'implementazione ha creato correttamente il package cognitivo, i contratti immutabili, il coordinatore, il fast-path critico, il buffer di fine tick, la deduplicazione, la coda breve, il token di interazione diretta e una prima suite di test.

La Fase 1 è quindi sostanzialmente avviata e il refactor resta isolato dal comportamento dei sensori storici. Tuttavia il nucleo non è ancora pienamente conforme alla strategia e al piano tecnico: quattro difetti funzionali possono produrre annunci indesiderati o informazione spaziale fuorviante quando i sensori verranno migrati.

**Regola di avanzamento:** non procedere alla Fase 2 né collegare un sensore esistente finché tutte le correzioni bloccanti e i relativi test non sono completati e contro-validati.

## 2. Elementi verificati e conformi

| Area | Esito | Evidenza |
|---|---|---|
| Contratti | Conforme | Presenti `CognitiveEvent`, `CognitivePriority`, `SourceDomain`, `StateSignature`, `SoundCue`. |
| Fast-path | Conforme con precisazione | Primo critico emesso con `interrupt=true`; secondo critico concorrente con `interrupt=false`. |
| Deduplicazione | Conforme | Chiave comprensiva di dominio, semantica, posizione, firma di stato e priorità. |
| Fallback | Conforme alla Fase 1 | Il flag del coordinatore mantiene l'uscita diretta per gli eventi che in futuro saranno migrati. |
| Token GUI | Conforme nel principio | Un critico bypassa `DirectInteractionShield`. |
| Isolamento | Conforme | Nessun sensore storico risulta migrato nel nucleo della Fase 1. |
| Test di base | Positivo | Esistono test unitari per priorità, doppio critico, TTL, deduplicazione, coda, shield, fallback e reset. |

## 3. Correzioni bloccanti

### BLOCCANTE 1 — Uno scudo critico non sopprime gli eventi operativi

**File:** `features/cognitive/CognitiveCoordinator.java`  
**Rilevamento:** Durante `flushTick`, lo scudo critico esclude soltanto eventi `PASSIVE` e `CONTEXTUAL`. Un evento `OPERATIONAL` già presente nel buffer può essere emesso immediatamente dopo un burrone, una lava o un danno.

**Perché è un problema:** La tabella delle priorità stabilisce che `CRITICAL` interrompe `OPERATIONAL`, `CONTEXTUAL` e `PASSIVE`. Un messaggio come “Destinazione raggiunta” o “Agganciato bersaglio” non deve competere con un allarme salvavita.

**Correzione richiesta:**

1. Durante `criticalShieldUntil`, sopprimere oppure rinviare ogni evento non critico.
2. Per gli eventi `OPERATIONAL`, preferire una coda breve con TTL invece dello scarto, purché l'evento sia ancora valido alla fine dello scudo.
3. Non modificare il comportamento dei critici: essi continuano il fast-path immediato.

**Test obbligatorio:**

- Inviare un `CRITICAL` e un `OPERATIONAL` nello stesso tick.
- Eseguire il flush durante lo scudo.
- Verificare che sia emesso solo il critico.
- Alla scadenza dello scudo, verificare che l'operativo sia emesso solo se il suo TTL non è scaduto.

### BLOCCANTE 2 — `SOUND_ONLY` può diventare voce nel percorso concatenato

**File:** `features/cognitive/CognitiveCoordinator.java`  
**Rilevamento:** Quando due eventi sono concatenabili, il coordinatore compone il testo e lo invia al narratore senza verificare `isVoiceEnabled()` su entrambi gli eventi.

**Perché è un problema:** L'utente può avere scelto un feedback esclusivamente sonoro. Pronunciarne il testo attraverso una concatenazione viola direttamente il principio “Filtro a Monte, Arbitraggio a Valle”.

**Correzione richiesta:**

1. Concatenare vocalmente soltanto due eventi entrambi abilitati alla voce.
2. Se uno dei due è `SOUND_ONLY`, emettere il cue consentito e pronunciare soltanto l'altro evento, se autorizzato.
3. `SILENT` non deve generare né testo né cue, anche in presenza di un evento compatibile.

**Test obbligatori:**

- `VOICE_ONLY` + `SOUND_ONLY`: solo testo del primo e cue del secondo.
- `SOUND_ONLY` + `SOUND_ONLY`: nessuna narrazione, entrambi i cue autorizzati.
- `SILENT` + `VOICE_ONLY`: soltanto la voce del secondo; nessun cue né testo dal primo.

### BLOCCANTE 3 — Concatenazione testuale senza template I18N

**File:** `features/cognitive/CognitiveCoordinator.java`, metodo `fuseTexts`  
**Rilevamento:** La fusione usa punto e spazio direttamente nel codice.

**Perché è un problema:** Il piano impone template localizzati IT/EN. La punteggiatura, l'ordine delle informazioni e i prefissi non sono universalmente equivalenti fra lingue né fra coppie cognitive.

**Correzione richiesta:**

1. Sostituire la concatenazione generica con un risolutore di template basato sulla coppia di domini e sul tipo semantico.
2. Introdurre prima i template minimi IT/EN, ordinati alfabeticamente:
   - ostacolo/sicurezza + mirino/esplorazione;
   - movimento + ostacolo o mirino, se tale combinazione resta autorizzata.
3. Se non esiste un template per la coppia, non fondere gli eventi: usare la coda breve.

**Test obbligatori:**

- Verificare la chiave/template italiano e inglese per sicurezza + esplorazione.
- Verificare che una coppia senza template non produca una frase costruita manualmente.

### BLOCCANTE 4 — Compatibilità spaziale assente dalla decisione di fusione

**File:** `features/cognitive/CognitiveCoordinator.java`, metodo `canChainEvents`  
**Rilevamento:** Il metodo controlla essenzialmente domini e flag `canChain`; non valuta posizione relativa, direzione, distanza o opposizione spaziale.

**Perché è un problema:** Può fondere, ad esempio, un nemico a sinistra con una risorsa a destra. Per un giocatore non vedente ciò crea una rappresentazione spaziale falsa e aumenta il carico cognitivo.

**Correzione richiesta:**

1. Aggiungere al contratto o alla policy dati sufficienti per verificare la compatibilità spaziale: posizione, vettore/direzione relativa o categoria direzionale e distanza.
2. Consentire la fusione solo quando entrambi gli eventi sono frontali oppure appartengono alla stessa zona spaziale compatibile.
3. Per eventi privi di posizione, autorizzare la fusione solo con una regola esplicita e un template dedicato.

**Test obbligatori:**

- ostacolo e mirino frontali: fusione autorizzata;
- minaccia a sinistra e risorsa a destra: fusione negata, evento secondario in coda o scartato per TTL;
- evento senza posizione: fusione ammessa esclusivamente se coperta da regola dichiarata.

## 4. Miglioramenti richiesti prima della migrazione di sensori

Questi punti non impediscono di rifinire il nucleo, ma devono essere risolti prima di Fase 3.

| Priorità | Osservazione | Azione richiesta |
|---|---|---|
| Alta | `DirectInteractionShield` scarta eventi contestuali/passivi invece di differirli. | Conservare al più una voce valida con TTL e ripresentarla dopo la scadenza del token. |
| Alta | `interruptible` è presente in `CognitiveEvent` ma non influenza ancora l'arbitraggio. | Applicarne la semantica oppure rimuoverlo dal contratto fino a quando serve davvero. |
| Media | `criticalModAudioDucking` è dichiarato ma non usato. | Rimandarlo alla Fase 2 o implementare una policy che agisca solo sui cue della mod. |
| Media | Lo shield usa un timestamp `volatile`, ma l'estensione non è atomicamente protetta. | Usare sincronizzazione o aggiornamento atomico se l'API può essere invocata da più contesti. |
| Media | `clearAllBuffers()` esiste senza hook di ciclo vita. | Progettare hook concreti per disconnessione, cambio mondo/dimensione, morte e respawn. |
| Bassa | I consumer fittizi dei test non vengono ripristinati dopo ogni test. | Aggiungere una API di reset o ripristino dei consumer di produzione nel teardown. |

## 5. Vincoli di non regressione

- Non modificare ancora `FallDetector`, `ObstacleDetector`, `NarrateCrosshair`, AutoWalk, Mentore, Accademia o mixin chiamanti.
- Non registrare output cognitivo nei percorsi GUI/input diretto senza il token esplicito.
- Non rimuovere `NarrationPriority`; la facciata resta necessaria per toast e pacchetti non migrati.
- Non forzare volumi, modalità vocali o cue disabilitati dalle configurazioni esistenti.
- Non avviare merge verso `mymaster` né deploy PrismLauncher in questa revisione del nucleo.

## 6. Sequenza operativa richiesta ad Antigravity

1. Correggere i quattro blocchi nel solo package `features.cognitive`.
2. Aggiungere i test obbligatori descritti nel presente rapporto.
3. Eseguire il test mirato `CognitiveCoordinatorTest` e l'intera suite Gradle con `--no-daemon` e Java 25.
4. Consegnare elenco dei file modificati, esiti test e diff della sola correzione.
5. Attendere contro-validazione indipendente prima di iniziare Fase 2.

## 7. Criterio di uscita dalla revisione

La Fase 1 può essere dichiarata conforme soltanto quando:

- tutti e quattro i difetti bloccanti sono coperti da test passanti;
- il coordinatore rispetta tutti gli `OutputType` in ogni percorso;
- nessuna concatenazione avviene senza template I18N e compatibilità spaziale;
- uno scudo critico non lascia filtrare eventi operativi prematuri;
- la suite completa non segnala regressioni;
- la contro-validazione conferma il rispetto del piano tecnico e della strategia ASTRALIS.

---

**Checkpoint:** Questo rapporto non autorizza Fase 2, migrazione sensori, build di rilascio, deploy o merge. Richiede esclusivamente le correzioni isolate della Fase 1 e una nuova revisione indipendente.
