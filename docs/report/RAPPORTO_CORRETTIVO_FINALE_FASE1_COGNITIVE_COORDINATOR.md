# Rapporto Correttivo Finale — Chiusura Fase 1 Cognitive Coordinator

**Data:** 2026-09-03  
**Destinatario operativo:** Antigravity  
**Branch:** `feat/cognitive-orchestrator`  
**Riferimento:** `RAPPORTO_REVISIONE_FASE1_COGNITIVE_COORDINATOR.md`  
**Esito attuale:** `[MINI-REVISIONE RICHIESTA — NON AVVIARE FASE 2]`

---

## 1. Obiettivo della mini-revisione

Le quattro correzioni bloccanti del rapporto precedente sono state realizzate in modo sostanzialmente corretto. Questo documento richiede gli ultimi interventi necessari per certificare la Fase 1, oltre a due vincoli che devono essere rispettati prima della futura migrazione del dominio Sicurezza.

L'intervento resta confinato al package `features.cognitive`, ai suoi test e, se indispensabile, alle localizzazioni già introdotte. Non modificare sensori storici, mixin, `Config.java`, `NarrationPriority`, `MainClass`, build, deploy o `mymaster`.

## 2. Correzioni obbligatorie per chiudere la Fase 1

### CORREZIONE F1-1 — Nessun fallback di concatenazione testuale hardcoded

**Problema:** Il risolutore di template I18N esegue ancora una fusione generica con punto e spazio se la chiave non viene trovata o il sistema I18N non è disponibile. Ciò viola il contratto: senza un template localizzato, gli eventi non devono essere fusi.

**Comportamento richiesto:**

1. `getJoinTemplateKey` rimane l'unica autorizzazione a proporre una fusione.
2. Il risolutore deve restituire un risultato esplicitamente non valido quando la chiave manca, non un testo costruito a mano.
3. Se il template non è disponibile, il coordinatore deve emettere il primario secondo il suo `OutputType` e inserire il secondario valido nella `shortQueue`, rispettandone TTL e priorità.
4. Nessuna coppia di eventi può produrre una frase combinata senza chiave I18N effettivamente disponibile.

**Casi di test obbligatori:**

- Resolver che segnala template assente per una coppia normalmente concatenabile: il primario viene emesso da solo, il secondario non viene fuso.
- Coppia di domini priva di chiave: stessa politica di coda, senza punteggiatura hardcoded.
- Resolver IT/EN valido: la frase combinata proviene esclusivamente dal template.

### CORREZIONE F1-2 — Correggere il resoconto e completare la copertura test

**Problema:** Il resoconto dichiara 10 test, ma la classe contiene 8 metodi `@Test`.

**Comportamento richiesto:**

1. Aggiornare il resoconto tecnico con il numero reale dei test eseguiti, oppure aggiungere i test mancanti e dichiararne l'esito verificato.
2. Aggiungere un test esplicito per il comportamento F1-1.
3. Aggiungere un test con `DirectInteractionShield` attivo e un evento `CRITICAL`: l'evento deve percorrere il fast-path immediato, senza essere differito o silenziato.
4. Il teardown deve continuare a ripristinare narratore, audio e resolver di template di produzione.

**Criterio di accettazione:** Il numero riportato nel rapporto deve corrispondere esattamente al conteggio dei metodi `@Test` eseguiti dal task mirato.

### CORREZIONE F1-3 — Rendere effettivo oppure rimuovere `interruptible`

**Problema:** `CognitiveEvent` espone il campo `interruptible`, ma l'arbitraggio non lo consulta. Il contratto suggerisce una semantica che il codice non applica.

**Scelta richiesta:** adottare una sola delle due alternative e documentarla nel Javadoc.

**Alternativa A — Applicazione della semantica:**

- Un evento non interrompibile non viene rimpiazzato da un evento di pari priorità nel medesimo ciclo; un evento di priorità superiore conserva comunque precedenza assoluta.
- Il comportamento deve valere sia nel buffer sia nella `shortQueue`.
- Aggiungere test per pari priorità e per priorità superiore.

**Alternativa B — Semplificazione del contratto:**

- Rimuovere `interruptible` da `CognitiveEvent`, dalle factory e dai test.
- Rinviare l'introduzione della semantica a una revisione futura, quando esisterà un caso d'uso concreto.

**Raccomandazione:** Alternativa B. Nella Fase 1 attuale non esiste ancora una coda vocale gestita direttamente dal coordinatore che giustifichi questa complessità; la semplificazione riduce ambiguità e rischio di sovraingegnerizzazione.

## 3. Vincoli da implementare prima della migrazione di Sicurezza

Questi punti non impediscono la sola Fase 2 configurativa, ma devono essere soddisfatti prima di collegare `FallDetector`, `ObstacleDetector` o la sentinella minacce.

### S1 — Debounce audio per critici duplicati

Un evento critico duplicato sopprime oggi la voce ma può ancora riprodurre il cue sonoro a ogni submit. Sul ciglio di un burrone ciò può diventare un rumore continuo.

**Requisito:** distinguere deduplicazione vocale e cooldown audio. Per stessa chiave semantica, posizione e firma di stato, il cue critico può ripetersi al massimo una volta per finestra configurata o stabilita dalla policy; una escalation di gravità o distanza può superare il cooldown.

**Test richiesto:** tre submit identici nel periodo di cooldown producono un solo cue; un quarto con `StateSignature` più grave ne produce uno nuovo.

### S2 — Semantica del `DirectInteractionShield` per passivi e contestuali

Il token rinvia correttamente gli operativi ma attualmente scarta i contestuali e passivi. Prima della migrazione, scegliere e documentare una politica definitiva:

- i passivi possono essere scartati perché rapidamente obsoleti;
- i contestuali possono conservare al massimo una voce recente, con TTL molto breve, se resta utile dopo la GUI;
- i critici non sono mai influenzati;
- gli operativi restano nella `shortQueue` finché validi.

La scelta consigliata è: passivi scartati, un solo contestuale recente differito, operativi differiti. Questa politica evita accumulo senza perdere avvisi utili come buio o ostacolo appena rilevato.

## 4. Test e validazione richiesti

1. Eseguire il test mirato `CognitiveCoordinatorTest` con Java 25 e `--no-daemon`.
2. Eseguire l'intera suite `test` con Java 25 e `--no-daemon`.
3. Riportare numero esatto dei test mirati, suite completa e file modificati.
4. Allegare una breve matrice: test → requisito F1-1, F1-2, F1-3, S1 o S2.
5. Attendere nuova contro-validazione indipendente prima della Fase 2.

## 5. Criteri di chiusura della Fase 1

La Fase 1 può essere certificata soltanto quando:

- non esiste alcuna concatenazione hardcoded di fallback;
- la copertura test dichiarata e reale coincide;
- `interruptible` è applicato e testato oppure rimosso completamente;
- i quattro blocchi del rapporto di revisione originario restano coperti;
- la suite completa continua a passare;
- nessun sensore storico risulta modificato o migrato;
- il codice e i test ricevono contro-validazione indipendente positiva.

---

**Checkpoint ASTRALIS:** Questo rapporto autorizza esclusivamente la mini-revisione isolata della Fase 1. Non autorizza Fase 2, migrazione dei sensori, build di rilascio, deploy PrismLauncher, merge o aggiornamenti `knowledge/`.
