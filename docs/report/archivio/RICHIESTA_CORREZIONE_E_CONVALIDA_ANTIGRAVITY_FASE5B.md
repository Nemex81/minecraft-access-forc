# Richiesta di Correzione e Nuova Convalida — Sotto-Fase 5B

- **Destinatario**: Antigravity
- **Mittente**: Codex / ChatGPT, su richiesta di Luca
- **Data**: 2026-09-04
- **Stato**: Blocco correttivo pre-5C — proposta pronta, nessuna autorizzazione implicita a modificare il codice
- **Oggetto**: Correzione del bridge terminale di `MovementCoordinator` e completamento delle prove automatiche della Sotto-Fase 5B.

---

## 1. Esito della verifica indipendente

La Sotto-Fase 5B è strutturalmente coerente: `MovementCoordinator`, la factory I18N, `clearDomainEvents(SourceDomain.MOVEMENT)`, l'isolamento da `AutoWalkManager` e il piano di stop pre-5C sono presenti e compatibili con ASTRALIS.

La suite completa è stata rieseguita da Codex in un'area temporanea isolata dal lock OneDrive con il comando:

`./gradlew.bat --no-daemon --no-watch-fs test --rerun-tasks --console=plain`

Esito rilevato: **242 test, 0 fallimenti, 0 errori, 0 test saltati**.

Il risultato verde non basta però a convalidare pienamente la 5B: resta una criticità funzionale reale nel percorso usato quando il `CognitiveCoordinator` è attivo, che è la configurazione predefinita.

---

## 2. Bloccante funzionale: annuncio terminale cancellato prima del flush

### Causa esatta

In `MovementCoordinator.createMotorCallback(...)`, i callback terminali seguenti eseguono oggi questa sequenza:

1. Creano l'evento terminale;
2. Lo inviano con `postEvent(...)`;
3. Invocano poi `CognitiveCoordinator.clearDomainEvents(SourceDomain.MOVEMENT)`.

I callback coinvolti sono:

- `onTakeover()`;
- `onNoPath(Object target)`;
- `onStuck()`.

Quando il Coordinatore Cognitivo è abilitato, `postEvent(...)` inoltra l'evento a `CognitiveCoordinator.submitEvent(...)`, che lo accoda nel `tickBuffer`. La successiva pulizia selettiva rimuove tutti gli eventi `MOVEMENT` da `tickBuffer`, `shortQueue` e cache di deduplicazione, quindi rimuove anche l'evento terminale appena creato prima del flush di fine tick.

### Effetto percepibile

- Se Luca riprende il controllo manuale, l'annuncio `AUTOWALK_CANCELLED` può non essere vocalizzato;
- Se il ricalcolo non trova una rotta, `AUTOWALK_NO_PATH` può non essere vocalizzato;
- Se il watchdog arresta la marcia, `AUTOWALK_STUCK` può non essere vocalizzato.

Il fallback legacy non è colpito, perché in quel caso la voce viene emessa subito; il difetto riguarda il normale percorso cognitivo centralizzato.

---

## 3. Correzione minima richiesta

Non modificare `CognitiveCoordinator.clearDomainEvents(...)`: il metodo è corretto, atomico e preserva i domini diversi da `MOVEMENT`.

Per ciascuno dei tre callback terminali, applicare rigorosamente questo ordine:

1. Chiamare `CognitiveCoordinator.clearDomainEvents(SourceDomain.MOVEMENT)` per eliminare soltanto eventi residui della precedente sessione di marcia;
2. Creare il nuovo evento terminale;
3. Inviarlo con `postEvent(...)`;
4. Non eseguire un'ulteriore pulizia del dominio dopo l'invio.

Così il nuovo evento terminale rimane nel buffer fino all'arbitraggio di fine tick, mentre gli annunci obsoleti vengono comunque rimossi. Gli eventi `SAFETY` devono restare invariati.

---

## 4. Lacuna nella prova di deduplicazione

Il test `testSelectiveDomainEventClear` reinvia lo stesso evento a `t0 + 1700 ms`, mentre la finestra configurata di deduplicazione è di `1500 ms`.

Questo non dimostra l'invalidamento della cache: l'evento sarebbe accettato anche se la cache non fosse stata pulita, perché la finestra temporale è già scaduta.

### Correzione richiesta al test

1. Dopo `clearDomainEvents(MOVEMENT)`, reinviare l'evento equivalente entro la finestra di 1500 ms, per esempio a `t0 + 100 ms`;
2. Attendere soltanto il tempo necessario a superare l'eventuale scudo critico prima di eseguire il flush;
3. Verificare che il nuovo evento venga emesso e che gli eventi `SAFETY` restino intatti.

Questo rende la prova realmente dimostrativa del terzo livello di pulizia: l'invalidamento di `recentEvents`.

---

## 5. Test aggiuntivi obbligatori per chiudere la 5B

### Test 1 — Percorso cognitivo reale per gli esiti terminali

Per `onTakeover`, `onNoPath` e `onStuck`:

1. Non usare `cognitiveEventConsumer`, perché quel test seam bypassa il `CognitiveCoordinator` reale;
2. Abilitare il Coordinatore Cognitivo e intercettare le uscite vocali/audio tramite i suoi delegate di test;
3. Inserire almeno un evento `MOVEMENT` precedente, per simulare un residuo da eliminare;
4. Invocare il callback terminale e il flush di fine tick;
5. Verificare che venga emesso esattamente il nuovo evento terminale corretto e che il residuo non sopravviva;
6. Verificare separatamente che un evento `SAFETY` non venga cancellato.

### Test 2 — Ciclo di vita e reset

Integrare o rendere esplicita una prova di `handleClientTick(...)` per morte e cambio di livello/dimensione. La prova deve accertare che sessione, rotta ed eventi `MOVEMENT` siano puliti. Non è necessario connettere `AutoWalkManager`: quel collegamento resta esclusivamente nello scope della 5C.

---

## 6. Confini inviolabili

- Non modificare `AutoWalkManager`, `AutoWalkController` o il comportamento runtime storico: sono nello scope della Sotto-Fase 5C;
- Non modificare scanner Pagina Su/Pagina Giù, Mentore, Accademia, moduli di sicurezza o di esplorazione;
- Non introdurre nuove chiavi I18N né stringhe utente hardcoded;
- Conservare UTF-8 senza BOM, terminazioni LF e ordinamento alfabetico delle localizzazioni IT/EN;
- Non avanzare alla 5C né aggiornare il piano come convalidato fino alla nuova verifica positiva di Luca e Codex.

---

## 7. Protocollo di nuova convalida ASTRALIS

Dopo un eventuale comando esplicito di Luca per applicare la correzione:

1. Compilare e rieseguire i test mirati per `MovementCoordinatorTest` e `CognitiveCoordinatorTest`;
2. Rieseguire l'intera suite con:
   `./gradlew.bat --no-daemon --no-watch-fs test --rerun-tasks --console=plain`;
3. Riportare il numero effettivo di test, fallimenti, errori e test saltati;
4. Dimostrare in modo separato il percorso con Coordinatore Cognitivo attivo e il fallback legacy;
5. Aggiornare il piano soltanto dopo esito verde e nuova convalida indipendente;
6. Restare al checkpoint obbligatorio pre-5C in attesa dell'autorizzazione di Luca.

---

## 8. Richiesta operativa

Antigravity è invitato ad analizzare questa richiesta e a proporre o applicare la correzione esclusivamente dopo il comando esplicito di Luca. Al termine dovrà produrre un rapporto di risposta con evidenze di codice e test, quindi richiedere una nuova convalida incrociata secondo il Protocollo 2 ASTRALIS.
