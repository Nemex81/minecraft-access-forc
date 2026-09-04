# Rapporto correttivo — Fase 2 Cognitive Coordinator

**Destinatario:** Antigravity  
**Ramo:** `feat/cognitive-orchestrator`  
**Stato:** una micro-correzione obbligatoria prima della convalida finale della Fase 2  
**Data:** 3 settembre 2026

---

## 1. Esito della contro-verifica

La contro-verifica indipendente della Fase 2 è positiva per configurazione, binding, bootstrap, I18N, test deterministici e assenza di migrazioni nei produttori verificati.

Sono confermati:

- categoria Cloth Config `cognitiveCoordinator` con i soli tre valori già effettivi (`cognitiveCoordinatorEnabled`, `chainedNarrationEnabled`, `deduplicationWindowMs`);
- normalizzazione della finestra di deduplicazione tra 500 e 5000 ms;
- binding centralizzato `Config → CognitiveCoordinator` all’avvio e dopo il salvataggio;
- registrazione del coordinatore senza produttori migrati;
- API pubbliche di `NarrationPriority` preservate;
- localizzazioni IT/EN JSON valide e ordinate alfabeticamente;
- test Fase 1 e Fase 2 rieseguiti con build riuscita.

La Fase 2 non può tuttavia essere dichiarata chiusa finché non è applicata la correzione seguente.

---

## 2. Difetto bloccante residuo

File coinvolto:

`src/main/java/org/mcaccess/minecraftaccess/utils/NarrationPriority.java`

Nel metodo privato `defaultSuppressScanners(long durationMillis)` sono stati introdotti due blocchi `try/catch (Throwable ignored)` attorno a:

```java
NarrateCrosshair.suppressNarration(durationMillis);
ObstacleDetector.suppressWarnings(durationMillis);
```

Questa scelta è incompatibile con il contratto di retrocompatibilità della facciata. Lo scudo storico chiamava direttamente entrambi gli scanner; eventuali errori venivano quindi resi visibili e diagnosticabili. Catturare `Throwable` in produzione:

- può nascondere errori reali, inclusi `Error` JVM e problemi di inizializzazione;
- può lasciare uno scanner attivo senza alcuna traccia, proprio mentre il sistema ritiene di averlo soppresso;
- trasforma una regressione in chatter vocale intermittente difficile da riprodurre e diagnosticare;
- non è necessario ai test headless, perché questi impostano già `scannerSuppressor` su un delegate controllato prima di invocare la facciata.

---

## 3. Strategia correttiva

Applicare la correzione più piccola possibile, senza cambiare API, test seam o comportamento funzionale.

### Modifica richiesta

Sostituire l’intero corpo di `defaultSuppressScanners(long durationMillis)` con chiamate dirette:

```java
private static void defaultSuppressScanners(long durationMillis) {
    NarrateCrosshair.suppressNarration(durationMillis);
    ObstacleDetector.suppressWarnings(durationMillis);
}
```

Non aggiungere logging sostitutivo, fallback silenziosi, catch selettivi o nuove politiche di recupero. In questa fase la facciata deve preservare esattamente il comportamento storico; una futura gestione esplicita degli errori potrà essere progettata solo con una policy e test dedicati.

### Invarianti da preservare

- `scannerSuppressor` resta un seam package-private per i test; il valore predefinito punta a `defaultSuppressScanners`.
- `narrationConsumer` e `timeSupplier` restano package-private e vengono ripristinati in `resetTestSeams()`.
- Nessuna attivazione di `DirectInteractionShield` da `NarrationPriority`.
- Nessuna produzione di `CognitiveEvent`, nessuna modifica a mixin o sensori.
- Le quattro firme pubbliche legacy rimangono immutate.

---

## 4. Verifiche richieste dopo la correzione

1. Rieseguire i test mirati:

   ```powershell
   .\gradlew.bat --no-daemon test --tests "org.mcaccess.minecraftaccess.utils.NarrationPriorityFacadeTest" --tests "org.mcaccess.minecraftaccess.ConfigCognitiveSettingsTest" --tests "org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinatorTest"
   ```

2. Rieseguire la suite completa:

   ```powershell
   .\gradlew.bat --no-daemon test
   ```

3. Verificare nel diff che la correzione tocchi solo `NarrationPriority.java` e, se strettamente necessario, un test correlato. Non modificare file dei produttori.

4. Consegnare un resoconto con esito dei test, file modificati e hash del commit correttivo sul ramo dedicato.

---

## 5. Criterio di chiusura

La Fase 2 sarà convalidabile e dichiarabile chiusa quando:

- il catch generalizzato è rimosso;
- lo scudo legacy torna a propagare gli errori come nello stato storico;
- i test Fase 1, Fase 2 e l’intera suite risultano verdi;
- non sono introdotte migrazioni di sensori né altre variazioni fuori perimetro.

Al termine, il sistema sarà pronto per la sola pianificazione della Fase 3: migrazione pilota del dominio Sicurezza. Nessuna implementazione della Fase 3 è autorizzata automaticamente.

