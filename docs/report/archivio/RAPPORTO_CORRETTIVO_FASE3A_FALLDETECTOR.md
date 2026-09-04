# Rapporto correttivo — Fase 3A: FallDetector

**Destinatario:** Antigravity  
**Ramo:** `feat/cognitive-orchestrator`  
**Stato:** correzioni obbligatorie prima della convalida e del collaudo NVDA della 3A  
**Data:** 3 settembre 2026

---

## 1. Esito della verifica indipendente

L’implementazione della 3A è strutturalmente vicina al risultato richiesto:

- `FallDetector` costruisce eventi `SAFETY` critici per nuova caduta ed edge-bump;
- i quattro canali di output sono rappresentati nel contratto eventi;
- il fallback legacy è separato dal percorso cognitivo;
- il fast-path ora registra la deduplicazione anche per eventi `SOUND_ONLY`;
- `ObstacleDetector`, `CrosshairFeedbackManager` e `NarrateCrosshair` non presentano riferimenti al dominio cognitivo: la 3B è correttamente congelata.

La 3A **non è ancora convalidabile** per quattro correzioni di affidabilità e perimetro descritte qui sotto. La 3B resta bloccata.

---

## 2. Correzione P1 — eliminare fallback hardcoded e catch silenzioso

File: `src/main/java/org/mcaccess/minecraftaccess/features/FallDetector.java`  
Area: `buildFallEvent(...)`, circa righe 136–145.

Il mapper contiene un `try/catch (Exception ignored)` che, in caso di errore, costruisce i testi letterali:

```java
isEdgeBump ? "Sul ciglio" : "Attenzione caduta"
```

Questo contraddice il contratto I18N del sistema cognitivo e reintroduce il problema già corretto nella Fase 2: un errore reale può essere mascherato e l’utente può ricevere un testo non localizzato.

### Correzione richiesta

1. Costruire il messaggio localizzato nel punto produttivo, come avviene già in `handleDangerDetected(...)` per nuova caduta ed edge-bump.
2. Rendere il parametro `text` della factory/mapper non nullo e non vuoto. Se serve, usare `Objects.requireNonNull` e una precondizione esplicita; non rigenerare il messaggio nel mapper.
3. Rimuovere il blocco `try/catch`, entrambe le stringhe hardcoded e l’overload che passa `null` per il testo, salvo una motivazione tecnica dimostrabile.
4. I test headless devono passare sempre un testo localizzato fittizio ma non vuoto; non hanno bisogno di `NarrationUtils`, di `I18n` o di un fallback di produzione.

### Invariante

Un errore di localizzazione o di chiamata deve essere individuabile durante sviluppo/collaudo, non trasformato silenziosamente in un messaggio italiano incompleto.

---

## 3. Correzione P1 — riportare `NarrationUtils` fuori dalla 3A

File: `src/main/java/org/mcaccess/minecraftaccess/utils/NarrationUtils.java`.

La modifica di null-safety introdotta per facilitare i test headless è fuori dal perimetro approvato della 3A e non è più necessaria se il mapper riceve il testo già formato.

### Correzione richiesta

- ripristinare il comportamento precedente di `NarrationUtils` tramite un revert mirato della sola modifica 3A, senza reset distruttivi della working tree;
- non spostare la correzione in questo refactor e non aggiungere fallback nascosti;
- se in futuro emergerà un bug reale in `NarrationUtils`, aprire un piano/richiesta separato con casi d’uso e test dedicati.

### Invariante

La 3A deve limitarsi a `FallDetector`, contratti cognitivi e test direttamente necessari. Non deve modificare utilità trasversali per aggirare limiti dei test.

---

## 4. Correzione P2 — confinare i seam di test

File: `src/main/java/org/mcaccess/minecraftaccess/features/FallDetector.java`.

I delegate e i metodi introdotti per i test (`legacyNarrationConsumer`, `legacyAudioConsumer`, `cognitiveEventConsumer`, `fallSoundSupplier`, relativi setter, `buildFallEvent(...)` e `dispatchFallAlert(...)`) sono ora esposti come `public static`.

### Correzione richiesta

- rendere campi, setter, reset, mapper e dispatcher **package-private**;
- collocare i test nel package `org.mcaccess.minecraftaccess.features`, così possono usarli senza aprire nuove API pubbliche;
- lasciare pubblica solo l’API di produzione preesistente strettamente necessaria al mod;
- mantenere `resetTestSeams()` in `@AfterEach` per evitare contaminazione tra test.

### Motivazione

I seam esistono solo per test deterministici. Renderli API pubbliche permette a codice esterno di reindirizzare narrazione, cue o eventi di sicurezza durante il gioco, aumentando la superficie di regressione senza alcun vantaggio funzionale.

---

## 5. Correzione P2 — rendere reale la verifica del cue sonoro

File: `src/test/java/.../SafetyEventFactoryTest.java` e `FallDetectorCognitiveDispatchTest.java`.

I test impostano `fallSoundSupplier` a un supplier che restituisce `null`. In tal modo il test conta un `SoundCue`, ma il cue non contiene un `SoundEvent` realmente riproducibile. Non viene quindi verificata la conservazione di `SoundEvents.ANVIL_HIT` richiesta dalla 3A.

### Correzione richiesta

1. Nei test fornire un `SoundEvent` valido e non nullo, oppure verificare esplicitamente il supplier di produzione che restituisce `SoundEvents.ANVIL_HIT`.
2. Nei casi `VOICE_AND_SOUND` e `SOUND_ONLY` asserire:
   - `soundCue != null`;
   - `soundCue.soundEvent() != null`;
   - evento sonoro previsto: `ANVIL_HIT`;
   - `SoundSource.BLOCKS`, posizione, volume e pitch storico `1.0f`.
3. Conservare il caso `VOICE_ONLY` senza cue e il caso voce+cue disattivati senza evento.

### Invariante

Un test di “solo suono” deve dimostrare un suono effettivamente riproducibile, non soltanto un contenitore di dati presente in memoria.

---

## 6. Verifiche dopo le correzioni

1. Rieseguire almeno le suite coinvolte:

   ```powershell
   .\gradlew.bat --no-daemon test --tests "org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinatorTest" --tests "org.mcaccess.minecraftaccess.features.SafetyEventFactoryTest" --tests "org.mcaccess.minecraftaccess.features.FallDetectorCognitiveDispatchTest"
   ```

   Adeguare i nomi package/classi al nuovo package `features` se necessario.

2. Rieseguire l’intera suite:

   ```powershell
   .\gradlew.bat --no-daemon test
   ```

3. Eseguire un audit del diff e dichiarare esplicitamente:

   - nessuna modifica a `ObstacleDetector`, `CrosshairFeedbackManager`, `NarrateCrosshair`, mixin, GUI o comandi manuali;
   - `NarrationUtils` riportato allo stato precedente;
   - nessun testo hardcoded e nessun catch silenzioso nel mapper della caduta;
   - nessun seam di test pubblico introdotto.

4. Creare un commit correttivo atomico sul ramo dedicato e fornire hash, file modificati ed esito dei test.

---

## 7. Regole di collaudo e deploy

Il JAR della 3A è stato già distribuito alle istanze PrismLauncher, ma quel deploy non costituisce convalida. Dopo il commit correttivo:

- ricreare l’artefatto solo dopo test verdi;
- aggiornare l’istanza di prova secondo il flusso previsto;
- eseguire il collaudo NVDA della sola 3A: caduta nuova, edge-bump, solo voce, solo cue, comandi `Alt+F` / `Ctrl+Alt+F` e fallback con coordinatore disattivato;
- non iniziare la 3B fino al rapporto di collaudo positivo e a un nuovo via libera esplicito di Luca.

---

## 8. Criterio di convalida della 3A

La 3A sarà convalidabile quando tutte queste condizioni sono soddisfatte:

- gli eventi automatici di caduta restano critici, localizzati e a fast-path;
- il fallback legacy è osservabilmente identico per voce e cue;
- il debounce include `SOUND_ONLY` con un `SoundEvent` reale;
- i test sono isolati senza introdurre API pubbliche di manipolazione;
- non sono presenti fallback testuali hardcoded o catch silenziosi;
- non restano modifiche trasversali non motivate, incluso `NarrationUtils`;
- test automatici e collaudo NVDA della 3A hanno esito positivo;
- la 3B è ancora immutata e in attesa di autorizzazione.

