# Piano Tecnico Implementativo — Fase 2: Configurazione Cloth Config & Facciata Retrocompatibile NarrationPriority

**Riferimento strategico:** `docs/strategie/STRATEGIA_COGNITIVE_COORDINATOR.md`  
**Riferimento operativo:** `docs/report/RAPPORTO_CHIUSURA_FASE1_E_INDIRIZZO_FASE2_COGNITIVE_COORDINATOR.md`  
**Stato:** `[FASE 2 — PIANO TECNICO REVISIONATO, DA CONVALIDARE]`  
**Ramo di lavoro:** `feat/cognitive-orchestrator`  
**Ambito:** Configurazione controllabile dall'utente, I18N accessibile Cloth Config, Facade `NarrationPriority` e binding deterministico.  
**Vincolo assoluto di non-regressione:** Nessuna modifica a sensori storici (`FallDetector`, `ObstacleDetector`, mirino, AutoWalk, POI, Mentore), mixin, o percorsi diretti esistenti. Zero migrazioni di produttori in questa fase.

---

## 0. Esito della revisione e correzioni vincolanti

La struttura del piano è valida, ma la sua prima stesura conteneva tre punti che avrebbero creato configurazioni senza effetto reale o test fragili:

1. `ambientSpeechDensity` e `criticalModAudioDucking` non sono ancora applicati dal nucleo corrente: non devono apparire nella UI finché un comportamento concreto e testabile non li usa. Sono quindi rinviati alla prima fase di migrazione che introduce rispettivamente filtraggio di eventi ambientali e cue secondari migrati.
2. `DirectInteractionShield` rappresenta esclusivamente input diretto di tastiera/GUI. Un toast o un pacchetto saliente che passa da `NarrationPriority` non è un'interazione diretta e non deve attivarlo.
3. I test di `NarrationPriority` non possono intercettare direttamente `MainClass.narrate` né simulare il tempo in modo affidabile senza un piccolo seam di test. Il piano lo prescrive esplicitamente, confinato alla facciata e ripristinato dopo ogni test.

Le sezioni seguenti incorporano tali correzioni. Nessuna di esse amplia il perimetro della Fase 2.

---

## 1. Analisi dei Componenti Esistenti & Confini Operativi

### Componenti Coinvolti nella Fase 2
1. `src/main/java/org/mcaccess/minecraftaccess/Config.java`:
   - Sistema di configurazione basato su AutoConfig / Cloth Config;
   - Espone categorie tramite classi interne statiche annotate con `@ConfigEntry.Category` e `@ConfigEntry.Gui.TransitiveObject`.
2. `src/main/java/org/mcaccess/minecraftaccess/MainClass.java`:
   - Modulo principale Balm e bootstrap del client;
   - Registra i moduli tramite `registrars.registerModule(...)` alle righe 141-174;
   - Inizializza la configurazione tramite `Config.init()`.
3. `src/main/java/org/mcaccess/minecraftaccess/utils/NarrationPriority.java`:
   - Gestore storico dello scudo vocale sincrono;
   - Chiamato attualmente da `ToastManagerMixin` e `ClientPacketListenerMixin`;
   - Invoca direttamente `NarrateCrosshair.suppressNarration`, `ObstacleDetector.suppressWarnings` e `MainClass.narrate`.
4. `src/main/resources/assets/minecraft_access/lang/it_it.json` e `en_us.json`:
   - File di localizzazione per Cloth Config e sintesi vocale NVDA;
   - Mantenimento del rigido ordinamento alfabetico crescente.

### Componenti Rigorosamente Esclusi (Invarianti Intoccabili)
- Nessun sensore (`FallDetector`, `ObstacleDetector`, `AutoWalkManager`, `NarrateCrosshair`, `POIEntities`, `ContextualMentor`) produrrà `CognitiveEvent`.
- Nessun Mixin verrà modificato.
- Nessun chiamante legacy verrà modificato: la facciata conserverà il percorso di produzione diretto verso `MainClass.narrate` e non genererà `CognitiveEvent`.
- Nessuna modifica a build di rilascio, deploy PrismLauncher o merge su `mymaster`.

---

## 2. Elenco Dettagliato File-per-File e Modifiche Previste

### 2.1 `src/main/java/org/mcaccess/minecraftaccess/Config.java`
- **Responsabilità:** Definizione della categoria persistita `CognitiveCoordinator` e binding centralizzato.
- **Modifiche:**
  1. Aggiunta del campo categoria:
     ```java
     @ConfigEntry.Category("cognitiveCoordinator")
     @ConfigEntry.Gui.TransitiveObject
     public CognitiveSettings cognitiveCoordinator = new CognitiveSettings();
     ```
  2. Aggiunta della classe interna statica:
     ```java
     public static final class CognitiveSettings {
         public boolean cognitiveCoordinatorEnabled = true;
         public boolean chainedNarrationEnabled = true;

         @ConfigEntry.BoundedDiscrete(min = 500, max = 5000)
         public int deduplicationWindowMs = 1500;

         public CognitiveSettings() {
         }
     }
     ```
  3. Metodo statico di binding:
     ```java
     public static void applyCognitiveConfig() {
         if (instance != null) {
             applyCognitiveSettings(instance.cognitiveCoordinator);
         }
     }

     static void applyCognitiveSettings(CognitiveSettings cfg) {
         if (cfg != null) {
             int normalizedWindow = Math.clamp(cfg.deduplicationWindowMs, 500, 5000);
             org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator.setCoordinatorEnabled(cfg.cognitiveCoordinatorEnabled);
             org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator.setChainedNarrationEnabled(cfg.chainedNarrationEnabled);
             org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator.setDeduplicationWindowMs(normalizedWindow);
         }
     }
     ```
  4. `validatePostLoad()` e `saveConfig()` normalizzano il valore persistito prima del salvataggio; `applyCognitiveConfig()` viene invocato al termine di `init()` e dopo il salvataggio riuscito. `applyCognitiveSettings(CognitiveSettings)` resta package-private per il test unitario puro, senza bootstrap AutoConfig.
  5. **Rinvio esplicito:** `ambientSpeechDensity` e `criticalModAudioDucking` non sono aggiunti in Fase 2. Il primo non ha ancora un filtro di eventi migrati cui applicarsi; il secondo non ha ancora cue secondari migrati da attenuare. La UI non deve offrire opzioni inattive.

### 2.2 `src/main/java/org/mcaccess/minecraftaccess/MainClass.java`
- **Responsabilità:** Registrazione del modulo `CognitiveCoordinator` nel ciclo di vita Balm del client.
- **Modifiche:**
  - Registrazione una sola volta, dopo `Config.init()` e `Config.applyCognitiveConfig()`:
    ```java
    registrars.registerModule(new org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator());
    ```
  - La registrazione attiva unicamente il ciclo di vita a fine tick del nucleo già verificato. Senza produttori migrati il buffer resta vuoto: non deve introdurre narrazioni, scansioni o hook aggiuntivi.

### 2.3 `src/main/java/org/mcaccess/minecraftaccess/utils/NarrationPriority.java`
- **Responsabilità:** Facciata/Adapter retrocompatibile.
- **Modifiche & Contratto di Compatibilità:**
  1. `public static void suppressBackgroundScanners(long durationMillis)`:
     - Aggiorna lo scudo locale storico `shieldUntil = max(shieldUntil, now + durationMillis)`.
     - Invoca `NarrateCrosshair.suppressNarration(durationMillis)`.
     - Invoca `ObstacleDetector.suppressWarnings(durationMillis)`.
     - Non attiva `DirectInteractionShield`: toast e pacchetti salienti non sono input diretto. Lo scudo locale resta l'unica protezione effettiva per gli scanner legacy, finché essi non saranno migrati.
  2. `public static boolean isShieldActive()`:
     - Restituisce `System.currentTimeMillis() < shieldUntil`.
  3. `public static void narrateSalient(String text, long protectionMillis)`:
     - Invoca `suppressBackgroundScanners(protectionMillis)`.
     - Invoca direttamente `MainClass.narrate(text, true)`.
     - **Invariante:** Zero doppie emissioni; nessuna emissione via coordinatore per chiamate legacy.
  4. `public static void narrateSalientQueued(String text, long protectionMillis)`:
     - Invoca `suppressBackgroundScanners(protectionMillis)`.
     - Invoca direttamente `MainClass.narrate(text, false)`.
     - **Invariante:** Zero doppie emissioni.

### 2.4 `src/main/resources/assets/minecraft_access/lang/it_it.json` e `en_us.json`
- **Responsabilità:** Localizzazione accessibile con screen reader NVDA per la nuova categoria Cloth Config.
- **Chiavi da inserire in ordine alfabetico rigoroso:**
  - Categoria: `text.autoconfig.minecraft-access.category.cognitiveCoordinator`
  - Opzioni e descrizioni:
    - `text.autoconfig.minecraft-access.option.cognitiveCoordinator.cognitiveCoordinatorEnabled`
    - `text.autoconfig.minecraft-access.option.cognitiveCoordinator.cognitiveCoordinatorEnabled.@Tooltip`
    - `text.autoconfig.minecraft-access.option.cognitiveCoordinator.chainedNarrationEnabled`
    - `text.autoconfig.minecraft-access.option.cognitiveCoordinator.chainedNarrationEnabled.@Tooltip`
    - `text.autoconfig.minecraft-access.option.cognitiveCoordinator.deduplicationWindowMs`
    - `text.autoconfig.minecraft-access.option.cognitiveCoordinator.deduplicationWindowMs.@Tooltip`
  - Non inserire chiavi per densità ambientale o ducking: le impostazioni corrispondenti sono rinviate assieme alla loro prima semantica applicabile.

---

## 3. Schema del Binding Config $\rightarrow$ Coordinator

```text
[Avvio Gioco o Caricamento Config]
             ↓
        Config.init()
             ↓
   Config.applyCognitiveConfig()
             ↓
   CognitiveCoordinator.setCoordinatorEnabled(cfg.cognitiveCoordinatorEnabled)
   CognitiveCoordinator.setChainedNarrationEnabled(cfg.chainedNarrationEnabled)
   CognitiveCoordinator.setDeduplicationWindowMs(cfg.deduplicationWindowMs)
             ↓
[Modifica Utente in Schermata Cloth Config]
             ↓
      Config.saveConfig()
             ↓
   normalizzazione → persistenza → Config.applyCognitiveConfig()
   (Sincronizzazione a runtime dopo il salvataggio riuscito)
```

- **Isolamento Test:** Nessuna lettura sparsa di `Config` all'interno del `CognitiveCoordinator`. I test unitari possono continuare a usare setter espliciti senza richiedere il bootstrap di Minecraft o di AutoConfig.

---

## 4. Matrice di Test Previsti per la Fase 2

Verranno create due nuove classi di test JUnit:

### 4.1 `src/test/java/org/mcaccess/minecraftaccess/utils/NarrationPriorityFacadeTest.java`
La facciata mantiene la chiamata di produzione a `MainClass.narrate`, ma espone un delegate di narrazione e una sorgente temporale **package-private**, impiegati solo dai test e sempre ripristinati in `@AfterEach`. Non introdurre un'API pubblica né una dipendenza da Minecraft nei test.

1. `testLegacySuppressScannersKeepsLocalShieldWithCoordinatorEnabled`: Verifica che lo scudo locale funzioni anche a coordinatore attivo e che **non** attivi `DirectInteractionShield`.
2. `testLegacySuppressScannersKeepsLocalShieldWhenCoordinatorDisabled`: Verifica che a coordinatore disabilitato lo scudo locale funzioni inalterato.
3. `testNarrateSalientEmitsDirectlyWithoutDoubleSpeech`: Verifica che `narrateSalient` emetta direttamente via narratore esattamente 1 sola volta (nessun duplicato via coordinatore).
4. `testNarrateSalientQueuedEmitsDirectlyWithFalseInterrupt`: Verifica che `narrateSalientQueued` emetta direttamente con `interrupt=false`.
5. `testIsShieldActiveMatchesExpiry`: Verifica deterministica dell'indicatore di scudo attivo tramite sorgente temporale controllata.

### 4.2 `src/test/java/org/mcaccess/minecraftaccess/ConfigCognitiveSettingsTest.java`
1. `testBindingAppliesSupportedConfigParameters`: Verifica che `Config.applyCognitiveSettings(...)` trasferisca correttamente i tre parametri effettivamente supportati al `CognitiveCoordinator`.
2. `testDeduplicationWindowIsNormalized`: Verifica min 500ms, max 5000ms, default 1500ms e normalizzazione anche di valori esterni non validi.
3. `testI18nKeysAlphabeticalAndComplete`: Test automatizzato di conformità I18N: le chiavi previste esistono sia in `it_it.json` sia in `en_us.json`; la loro sequenza nel testo JSON è alfabetica. Il test non deve dipendere dall'ordine di una mappa JSON già deserializzata.

---

## 5. Vincoli di Compilazione & Criteri di Accettazione della Fase 2

### Vincoli di Build
- Compilazione con Java 25 e flag `--no-daemon`: `.\gradlew.bat --no-daemon test`.
- Nessun deploy verso istanze PrismLauncher e nessun merge su `mymaster`.
- Il branch operativo rimane `feat/cognitive-orchestrator`.

### Criteri di Accettazione per Dichiarare Chiusa la Fase 2
1. La nuova categoria `cognitiveCoordinator` appare regolarmente in Cloth Config ed è navigabile da tastiera con NVDA.
2. Tutte le etichette e tooltip effettivamente esposti hanno traduzione accessibile in italiano e inglese.
3. Tutte le chiamate esistenti a `NarrationPriority` in `ToastManagerMixin` e `ClientPacketListenerMixin` continuano a funzionare identicamente allo stato storico.
4. I test unitari di Fase 1 (14 test) e i nuovi test di Fase 2 passano al 100%.
5. I soli tre valori supportati producono effetto misurabile a runtime; nessuna opzione UI è decorativa o inattiva.
6. **Dichiarazione verificabile:** Nessun sensore (`FallDetector`, `ObstacleDetector`, mirino, AutoWalk, POI, didattica) è stato migrato o produce `CognitiveEvent`.

---

## 6. Procedura di Rollback della Fase 2

In caso di qualsiasi anomalia durante la Fase 2:
- creare un checkpoint commit della Fase 1 prima di iniziare e mantenere i commit della Fase 2 piccoli e tematici;
- interrompere l'implementazione e identificare il commit Fase 2 responsabile tramite revisione del diff e test;
- proporre un revert mirato del solo commit Fase 2, previa autorizzazione di Luca; non usare ripristini distruttivi della working tree;
- la Fase 1 rimane intatta e certificata nel commit precedente sul ramo dedicato.

---

## 🛑 7. Checkpoint di chiusura della pianificazione della Fase 2

Questo documento conclude la redazione del Piano Tecnico della Fase 2.  
**Stop Obbligatorio (Regola 0):** Nessun file di codice sorgente Java o file di configurazione è stato ancora modificato.  
L'implementazione inizierà esclusivamente dopo la revisione formale e l'esplicito comando di Luca.
