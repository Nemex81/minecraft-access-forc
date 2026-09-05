# Cronologia di Revisione — Fase 5C: Micro-Correzione Sensoriale AutoWalk
# Framework: ASTRALIS v2.6.3 — Protocollo 2 (Validazione Preventiva)
# Stato: correzione implementata e convalidata con successo
# Data: 4 settembre 2026

## 1. Scopo e regola di lettura

Questo documento conserva in ordine temporale le valutazioni relative alla regressione sensoriale emersa dopo l'implementazione della Sotto-Fase 5C di AutoWalk.

È la fonte di continuità per Codex e Antigravity: chi interviene successivamente deve leggere tutte le sezioni, applicare esclusivamente la correzione approvata da Luca e non dichiarare il passaggio alla Fase 5D prima della nuova convalida.

Documenti collegati:

- `docs/report/RAPPORTO_CONVALIDA_ANTIGRAVITY_FASE5C.md`: rapporto di implementazione originario di Antigravity.
- `docs/piani/attivi/PIANO_TECNICO_FASE5_NAVIGATORE_E_AUTOWALK.md`: piano tecnico di Fase 5.

## 2. Rapporto iniziale Codex post-implementazione 5C

### 2.1 Esiti confermati

- L'integrazione architetturale è corretta: `AutoWalkManager` possiede una sola istanza di `MovementCoordinator`, registra tick e ciclo di vita; `AutoWalkController` resta una facciata pubblica retrocompatibile.
- Gli esiti principali della marcia, l'integrazione con il Cognitive Coordinator, le localizzazioni esistenti e il piano risultano coerenti.
- La compilazione completa, i test e la generazione del JAR sono riusciti: 254 test passati, nessun fallimento o errore.
- I file della 5C rilevanti sono in UTF-8 senza BOM e con terminazioni LF.

### 2.2 Regressione individuata

Se AutoWalk viene disabilitato dalla configurazione mentre una marcia è già attiva, il nuovo flusso:

1. arresta correttamente il motore;
2. annuncia correttamente `minecraft_access.autowalk.disabled` con interrupt;
3. non emette il tradizionale suono di arresto `NOTE_BLOCK_HAT`, pitch `0.5f`.

Il controller storico emetteva tale suono prima dell'annuncio vocale. La sua assenza riduce la conferma non verbale dell'arresto, rilevante per l'uso con NVDA.

Evidenza tecnica originaria:

- `MovementCoordinator.tick(...)` richiama `cancel(client, true, "minecraft_access.autowalk.disabled")` quando la configurazione viene disabilitata durante una marcia.
- In `MovementCoordinator.cancel(...)`, un `reasonKey` specifico emette direttamente la voce; il ramo che produce HAT era invece raggiungibile soltanto con `narrate = false`.
- Il test di disabilitazione in corsa controllava stato e voce, ma non il canale audio.

### 2.3 Esito della convalida iniziale

La 5C è risultata valida per struttura, integrazione, compilazione e test automatici, ma non pienamente convalidata per parità percettiva. Il passaggio alla Fase 5D resta sospeso fino alla micro-correzione e alla relativa verifica.

### 2.4 Riserva separata

`Config.saveConfig()` intercetta attualmente `Throwable` per tollerare l'ambiente di test headless. Il comportamento evita crash nei test, ma può nascondere un errore reale di persistenza in gioco. È una riserva non bloccante e non fa parte della micro-correzione sensoriale.

## 3. Risposta e proposta di Antigravity

Antigravity ha riconosciuto la diagnosi come corretta e, rispettando la Regola 0, ha presentato una proposta senza modificare file.

### 3.1 Correzione proposta

Per una marcia attiva:

- con annullamento generico e narrazione attiva, mantenere `createCancelledEvent(...)`, che contiene già voce e HAT;
- con un motivo specifico, in particolare `minecraft_access.autowalk.disabled`, emettere HAT a volume `config.audioCueVolume` e pitch `0.5f`, quindi pronunciare la motivazione specifica con interrupt;
- con `narrate = false`, emettere il solo HAT.

La proposta prevede inoltre che il percorso di emissione del suono possa usare il consumer audio di test, così da rendere verificabile il cue senza avviare Minecraft.

### 3.2 Test proposto

Antigravity ha proposto di estendere il test di disabilitazione durante la marcia affinché controlli:

1. HAT con sorgente `BLOCKS`, pitch `0.5f` e volume configurato;
2. voce `minecraft_access.autowalk.disabled` con interrupt;
3. stato finale `CANCELLED` e rilascio del movimento.

### 3.3 Proposta accessoria non inclusa

Antigravity ha proposto anche un `log.warn` nel blocco di protezione di `Config.saveConfig()`. È un miglioramento diagnostico separato, non necessario alla correzione della parità sensoriale.

## 4. Revisione Codex della proposta Antigravity

### 4.1 Giudizio

La strategia di recupero del feedback HAT è corretta, minimale e coerente con il comportamento storico. Può essere approvata dopo le tre precisazioni vincolanti seguenti.

### 4.2 Precisazione vincolante 1 — Un solo canale per il cue legacy

Estrarre un piccolo helper interno, ad esempio `emitLegacySound(SoundCue cue)`:

1. se è presente `legacyAudioConsumer`, inoltra il cue al consumer;
2. altrimenti richiama la riproduzione fisica legacy.

Sia il fallback di `postEvent(...)` sia il ramo di cancellazione specifica devono usare questo helper. In questo modo non si duplica la logica e il test headless può catturare il suono senza dipendere da un'istanza Minecraft reale.

### 4.3 Precisazione vincolante 2 — Fixture corretta per il test

Il test corrente di disabilitazione passa `client = null`. In tali condizioni il ramo di cancellazione non può costruire il cue, anche se la riproduzione viene resa intercettabile dal consumer di test.

Il test deve quindi fornire un client simulato con giocatore e livello simulati. Deve verificare con precisione:

1. un solo cue HAT;
2. `SoundSource.BLOCKS`;
3. pitch `0.5f`;
4. volume pari a `testConfig.audioCueVolume`;
5. una sola voce `autowalk.disabled` con `interrupt = true`;
6. assenza di eventi cognitivi aggiuntivi;
7. stato finale `CANCELLED`.

Il rilascio fisico dei tasti resta coperto dal test dedicato già esistente; non va duplicato artificialmente nel test della regressione sonora.

### 4.4 Precisazione vincolante 3 — Nessuna modifica a Config in questa correzione

Non modificare `Config.java` nel medesimo intervento. Il logging del salvataggio è utile ma amplia lo scope, rende meno isolabile la convalida e richiede una decisione autonoma sulla politica di eccezioni e log in produzione.

## 5. Piano esecutivo autorizzabile

Quando Luca fornirà un comando esplicito di applicazione, l'implementazione dovrà limitarsi a:

1. aggiungere l'helper unico per l'emissione audio legacy in `MovementCoordinator`;
2. ripristinare HAT prima della voce per una cancellazione attiva con motivo specifico;
3. estendere il test di disabilitazione in corsa con la fixture client corretta e le sette verifiche elencate;
4. eseguire test completi e generazione JAR con Gradle senza demone;
5. aggiornare questo rapporto con l'esito effettivo della nuova convalida, senza modificare lo stato della Fase 5D prima dell'approvazione di Luca.

## 6. Gating ASTRALIS originario

- Implementazione della micro-correzione: autorizzata da Luca in data 04/09/2026 ore 19:18.
- Convalida automatica successiva: completata con esito 100% verde (254 test su 254).
- Collaudo in-game della Fase 5D: non avviato (Stop Obbligatorio attivo).

## 7. Esito Effettivo della Nuova Convalida Post-Correzione

In data 4 settembre 2026, a seguito dell'autorizzazione esplicita di Luca ("ok, procedi con la correzione"), sono stati eseguiti con successo i 5 passi del piano esecutivo:

1. **Helper Unico `emitLegacySound`**: estratto metodo privato in `MovementCoordinator` che convoglia in modo trasparente l'audio su `legacyAudioConsumer` (in test) o su `client.level.playLocalSound` (in gioco), cablato sia nel fallback di `postEvent` sia nel ramo di cancellazione attiva con motivo specifico.
2. **Ripristino Parità Sensoriale Acustica**: in `MovementCoordinator.cancel(...)`, quando la marcia è attiva (`wasActive == true`) e l'arresto ha una motivazione specifica (es. `autowalk.disabled`), viene emesso il cue sonoro `NOTE_BLOCK_HAT` a pitch `0.5f`, sorgente `BLOCKS` e volume di configurazione, immediatamente prima della vocalizzazione con `interrupt = true`.
3. **Fixture Completa e Test 8 Stringente**: il test unitario 8 in `AutoWalkHarmonizationTest.java` (`testTickDisabledCancelsMovement`) è stato aggiornato con mock completo di `Minecraft` (`client.player` e `client.level`) e ha convalidato al 100% le 7 asserzioni vincolanti richieste da Codex.
4. **Assoluta Circoscrizione dello Scope**: `Config.java` non è stato modificato in questo ciclo, nel pieno rispetto della Precisazione Vincolante 3.
5. **Esiti di Compilazione e Test**:
   - `.\gradlew.bat --no-daemon --no-watch-fs test`: **254 test passati su 254 (0 errori, 0 fallimenti, 100% VERDE)**;
   - `.\gradlew.bat --no-daemon --no-watch-fs shadowJar`: **BUILD SUCCESSFUL in 41s**, artefatto `build/libs/minecraft-access-1.12.0-SNAPSHOT.jar` (7.4 MB) generato e pronto per il deploy.

## 8. Gating ASTRALIS Post-Correzione (STOP OBBLIGATORIO PRE-5D)

- Micro-correzione sensoriale: completata e verificata con successo.
- Suite di test automatica: convalidata al 100% verde (254/254).
- Artefatto di produzione: compilato con successo.
- **Passaggio alla Sotto-Fase 5D (Deploy Proattivo in PrismLauncher & Collaudo In-Game con NVDA)**: in attesa dell'esplicita autorizzazione di Luca (*"procedi con 5D"*).

## 9. Convalida Indipendente Codex Post-Implementazione

### 9.1 Verifica del codice e della copertura

Codex ha riesaminato in sola lettura l'implementazione successiva alla micro-correzione e ha confermato tutte le prescrizioni della Sezione 4:

1. `emitLegacySound(SoundCue)` è l'unico punto di inoltro dell'audio legacy: usa il consumer audio in test e la riproduzione fisica in gioco.
2. Il fallback di `postEvent(...)` e la cancellazione con motivo specifico usano lo stesso helper.
3. Quando una marcia attiva viene disabilitata dalla configurazione, il sistema riproduce prima un solo `NOTE_BLOCK_HAT`, sorgente `BLOCKS`, pitch `0.5f` e volume configurato; pronuncia poi la motivazione con `interrupt = true`.
4. Il test di regressione usa un client simulato completo e verifica il cue, la voce, l'assenza di eventi cognitivi duplicati e lo stato finale `CANCELLED`.
5. `Config.java` non ha ricevuto modifiche in questo ciclo; la riserva sul suo `catch (Throwable)` resta separata e non bloccante.

### 9.2 Verifica indipendente di compilazione

Codex ha eseguito nuovamente, in un'area di compilazione temporanea isolata, la suite completa e la generazione del JAR. L'isolamento è stato necessario solo perché la cartella `build` ordinaria era temporaneamente bloccata da un processo esterno; non è stata rilevata alcuna anomalia nel codice o nella compilazione.

Esito verificato:

- 44 file di risultati JUnit elaborati;
- 254 test passati;
- 0 fallimenti, 0 errori, 0 test ignorati;
- `AutoWalkHarmonizationTest`: 10 test passati;
- JAR di produzione generato con successo.

### 9.3 Verdetto Codex secondo Protocollo 2 ASTRALIS

- Validità: conforme; la regressione sensoriale è risolta.
- Efficacia e accessibilità: conformi; il riscontro acustico precede l'annuncio vocale come nel comportamento storico.
- Coerenza e coesione: conformi; non sono introdotti duplicati cognitivi né canali audio divergenti.
- Integrabilità e compatibilità: conformi; manager, facciata legacy e contratti pubblici restano invariati.
- Affidabilità: conforme entro la copertura automatica disponibile.

**Verdetto finale Codex: Sotto-Fase 5C pienamente convalidata.** Il deploy e il collaudo in-game della Sotto-Fase 5D restano soggetti al comando esplicito di Luca, in conformità alla Regola 0.

