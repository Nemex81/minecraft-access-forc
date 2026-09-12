# PIANO-TECNICO: Bonifica Codice Morto Certificato e De-Risking Configurazione Legacy

## 📌 Metadati del Documento

- **ID Documento**: PIANO-TECNICO-BONIFICA-CODICE-MORTO-E-CONFIG
- **Tipo Documento**: PIANO-TECNICO
- **Esito Classificazione**: CONFERMATA
- **Intento Operativo**: REFACTORING
- **Ambito**: PROGETTO:minecraft-access
- **Stato Documento**: REVISIONATO
- **Stato Lavoro**: IMPLEMENTATO — IN ATTESA DI CONVALIDA
- **Gate Operativo**: STOP OBBLIGATORIO POST-1B
- **Evidenza del Gate**: Autorizzazione esplicita di Luca del 2026-09-12 10:47; Sotto-Fase 1B implementata con successo sul branch `test/bonifica-codice-morto-e-config`; suite completa con 401 test superati senza fallimenti o errori; Checkstyle differenziale con `UnusedImports = 0`, zero violazioni nel nuovo test e nessun incremento rispetto alle 1444 diagnostiche preesistenti non correlate.
- **Partecipanti e Ruoli**:
  - **Responsabile Decisionale**: Luca
  - **Coordinatore Operativo**: Antigravity
  - **Esecutore Previsto**: Antigravity
  - **Revisori**: GPT / Codex
  - **Collaudatore Finale**: Luca
- **Modalità Operativa**: TRIADE
- **Documenti Correlati**:
  - deriva-da: docs/strategie/attive/STRATEGIA_BONIFICA_CODICE_MORTO_E_CONFIG.md
  - allegato-di: docs/report/REPORT_HANDOVER_STRATEGIA_BONIFICA_CODICE_MORTO.md
- **Riferimenti Git**: test/bonifica-codice-morto-e-config (originato da HEAD `d4c23b0ce...`)
- **Ultimo Aggiornamento**: 2026-09-12 11:52 Europe/Rome

---

## 📊 0. Registro di Convergenza & Checklist Operativa (3 Stati)

- `[x]` **Pre-Flight Attuabile**: verifica assenza modifiche preesistenti nei soli target `src/main/` e `src/test/`; ammessi i 3 documenti ASTRALIS non tracciati.
- `[x]` **Contratto D0**: rimozione metodo morto `narrateCrosshairTarget()` in `NumpadControls.java` con tutela import condivisi.
- `[x]` **Contratto D1**: rimozione costante orfana `DEBOUNCE_GRACE_PERIOD_MS` in `CrosshairFeedbackManager.java`.
- `[x]` **Contratto D2**: rimozione chirurgica dei 30 import inutilizzati Checkstyle (inclusa delimitazione selettiva in `FallDetector`, `ObstacleDetector`, `LongRangeFallDetector`).
- `[x]` **Contratto D3**: aggiunta annotazione `@ConfigEntry.Gui.Excluded` su `range`, `depth`, `delay` in `Config.java`.
- `[x]` **Contratto D4**: tutela integrale chiavi di traduzione multilingua nei file `assets/minecraft_access/lang/*.json`.
- `[x]` **Contratto D5**: congelamento invariato del campo `criticalModAudioDucking` in `CognitiveCoordinator.java`.
- `[x]` **Contratto S1**: esecuzione delle 5 barriere del Protocollo 11 (quarantena di backup e procedura di rollback non distruttiva).
- `[x]` **Contratto S2**: implementazione del test seam unitario `LegacyConfigSerializationTest` con configurazione Gson produttiva (tramite `IdentifierAdapter` condiviso reso pubblico) e fixture A, B, C, D su directory temporanea isolata.
- `[x]` **Contratto S3**: verifica Checkstyle differenziale (violazioni `UnusedImports` a 0 nel report XML, nessun incremento di altri errori, conformità test).
- `[x]` **Contratto S4**: esecuzione suite JUnit completa superata al 100% senza regressioni (401 test passati).
- `[x]` **Revisione Diff Lineare**: ispezione finale `git diff` limitata ai 23 file sorgente tracciati (+4 righe, -48 righe) oltre al nuovo file di test `LegacyConfigSerializationTest.java`.
- `[/]` **Stop Obbligatorio & Gating**: arresto completo post-1B in attesa della convalida di Luca prima di commit o deploy.

---

## 🎯 1. Obiettivo & Origine del Requisito

### 1.1 Origine
Il presente piano operativo nasce dall'analisi esplorativa del repository e dalla successiva dialettica ingegneristica convalidata da GPT/Codex (Protocollo 12) nella Strategia di Fase 0 [`STRATEGIA_BONIFICA_CODICE_MORTO_E_CONFIG.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/strategie/attive/STRATEGIA_BONIFICA_CODICE_MORTO_E_CONFIG.md).

### 1.2 Obiettivo
Eseguire una pulizia chirurgica e sicura su due perimetri rigorosamente disaccoppiati:
1. **Perimetro 1**: Eliminazione del codice morto staticamente dimostrato privo di chiamanti (1 metodo, 1 costante, 30 import inutilizzati certificati da Checkstyle);
2. **Perimetro 2**: Messa in sicurezza della GUI Cloth Config tramite `@ConfigEntry.Gui.Excluded` su 3 campi deprecati (`range`, `depth`, `delay`), tutelando la persistenza reversibile Gson nei profili reali e garantendo l'assoluta stabilità dei dati di configurazione tramite test seam dedicato di round-trip produttivo.

---

## 🚫 2. Scopo ed Esclusioni Tassative (Delimitazione Negativa)

1. **Divieto di Rimozione dei Campi in Config**: I campi `range`, `depth`, `delay` in `Config.FallDetector` non devono essere rimossi, rinominati o marcati `transient`. Devono rimanere serializzabili da Gson.
2. **Divieto di Modifica Traduzioni**: Nessuna chiave di traduzione nei file `src/main/resources/assets/minecraft_access/lang/*.json` deve essere toccata o rimossa.
3. **Divieto di Tocco a `criticalModAudioDucking`**: Il campo in `CognitiveCoordinator.java` non deve essere modificato, esposto o rimosso.
4. **Delimitazione Selettiva su Rilevatori e Facciate**:
   - In `FallDetector.java`, `ObstacleDetector.java` e `LongRangeFallDetector.java` è autorizzata *esclusivamente* la rimozione degli specifici import inutilizzati censiti nell'inventario del contratto D2.
   - È fatto divieto assoluto di toccare qualsiasi riga di logica, algoritmi, firme o campi di tali classi.
   - È fatto divieto assoluto di toccare `AutoWalkController.java`, `ProximityFallDetector.java` o `CentralFallSafetyManager.java`.
5. **Divieto di Accesso ai File Reali**: Divieto assoluto di leggere, scrivere o sovrascrivere file `config.json` reali delle istanze PrismLauncher durante i test.
6. **Divieto di Deploy**: Nessun file `.jar` deve essere copiato nelle istanze attive per questa bonifica.
7. **Divieto di Comandi Distruttivi**: Divieto di `git reset --hard`, `git checkout --` o `git restore` distruttivo non circoscritto.

---

## ⚙️ 3. Precondizioni & Pre-Flight Check Attuabile

Prima di qualsiasi modifica in Sotto-Fase 1B:
- [ ] **Verifica Modifiche Preesistenti**: Il comando `git status --porcelain` deve certificare l'assenza di modifiche non committate nei soli percorsi sorgente e test (`src/main/` e `src/test/`).
- [ ] **Documenti Autorizzati Ammessi**: I tre file di documentazione e governance generati nelle sessioni di coordinamento (`docs/strategie/attive/...`, `docs/report/...`, `docs/piani/attivi/...`) sono esplicitamente ammessi come untracked e non bloccano il pre-flight. Se un file target in `src/` contiene modifiche estranee, l'esecuzione si arresta all'istante.
- [ ] **Hash di Pre-Flight**: Coincidente con `d4c23b0ce1a213b73c6b86f6a70648f27b84b124`.
- [ ] **Quarantena Temporanea**: Creazione della directory temporanea `$env:TEMP/minecraft_access_quarantine_bonifica/` con copia di sicurezza dei file target prima di aprirli in scrittura.

---

## 🏛️ 4. Named Contracts (D0..D5 e S1..S4)

### Contratti Decisionali (D0..D5)

#### Contratto D0 — Rimozione Metodo Morto Numpad
- **Target**: `org.mcaccess.minecraftaccess.features.NumpadControls`.
- **Firma**: `private void narrateCrosshairTarget()`.
- **Azione**: Rimozione completa del metodo (dichiarazione e corpo).
- **Invariante**: Conservare gli import `BlockHitResult`, `WorldNarrator`, `PlayerUtils`, `NarrationUtils` utilizzati dal metodo contiguo `narrateTargetCoordinates()`.

#### Contratto D1 — Rimozione Costante Morta Crosshair
- **Target**: `org.mcaccess.minecraftaccess.features.crosshair.CrosshairFeedbackManager`.
- **Firma**: `private static final long DEBOUNCE_GRACE_PERIOD_MS = 80;`.
- **Azione**: Rimozione della riga di dichiarazione della costante.
- **Invariante**: Nessun'altra costante, campo o metodo della classe viene alterato.

#### Contratto D2 — Rimozione Chirurgica dei 30 Import Checkstyle
- **Target**: I 30 import inutilizzati formalmente censiti dal report XML di Checkstyle (`[UnusedImports]`):
  1. `features.academy.MissionRegistry`: `import net.minecraft.world.phys.BlockHitResult;`
  2. `features.autowalk.ClimbAssistantController`: `import java.util.ArrayList;`
  3. `features.autowalk.ClimbAssistantController`: `import java.util.List;`
  4. `features.autowalk.ClimbAssistantController`: `import net.minecraft.world.level.block.state.BlockState;`
  5. `features.door.DoorInteractionHelper`: `import org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder;`
  6. `features.door.DoorInteractionManager`: `import net.minecraft.world.level.block.DoorBlock;`
  7. `features.FallDetector`: `import java.util.Objects;`
  8. `features.FallDetector`: `import org.mcaccess.minecraftaccess.MainClass;`
  9. `features.inventory_controls.GroupGenerator`: `import net.minecraft.client.gui.screens.inventory.CraftingScreen;`
  10. `features.inventory_controls.InventoryControls`: `import net.minecraft.client.gui.screens.inventory.CraftingScreen;`
  11. `features.inventory_controls.InventoryControls`: `import net.minecraft.client.gui.screens.inventory.InventoryScreen;`
  12. `features.NumpadControls`: `import org.mcaccess.minecraftaccess.utils.NarrationPriority;`
  13. `features.ObstacleDetector`: `import net.minecraft.sounds.SoundEvents;`
  14. `features.ObstacleDetector`: `import net.minecraft.sounds.SoundSource;`
  15. `features.point_of_interest.ObjectTracker`: `import java.util.stream.Stream;`
  16. `features.point_of_interest.waypoints.POIWaypoints`: `import lombok.Getter;`
  17. `features.point_of_interest.waypoints.WaypointManager`: `import net.blay09.mods.balm.Balm;`
  18. `features.point_of_interest.waypoints.WaypointUtils`: `import net.minecraft.client.Minecraft;`
  19. `features.safety.fall.LongRangeFallDetector`: `import org.jetbrains.annotations.Nullable;`
  20. `features.safety.fall.LongRangeFallDetector`: `import org.mcaccess.minecraftaccess.features.cognitive.SourceDomain;`
  21. `features.safety.traversal.ClimbTraversalAnalyzer`: `import java.util.List;`
  22. `features.safety.traversal.TraversalSafetyAnalyzer`: `import net.minecraft.tags.BlockTags;`
  23. `features.survival_tracker.SurvivalScanner`: `import java.util.Comparator;`
  24. `features.survival_tracker.SurvivalScanner`: `import net.minecraft.client.Minecraft;`
  25. `features.survival_tracker.SurvivalScanner`: `import net.minecraft.tags.ItemTags;`
  26. `features.survival_tracker.SurvivalScanner`: `import org.jetbrains.annotations.NotNull;`
  27. `features.survival_tracker.SurvivalScanner`: `import org.mcaccess.minecraftaccess.Config;`
  28. `mixin.ClientPacketListenerMixin`: `import org.mcaccess.minecraftaccess.MainClass;`
  29. `mixin.ToastManagerMixin`: `import org.mcaccess.minecraftaccess.MainClass;`
  30. `utils.position.Orientation`: `import net.minecraft.client.Minecraft;`
- **Invariante**: Eliminare esclusivamente le righe di import sopra elencate. Nessuna riga di codice esecutivo deve essere toccata.

#### Contratto D3 — De-Risking GUI Campi Deprecati Config
- **Target**: `org.mcaccess.minecraftaccess.Config.FallDetector`.
- **Campi**:
  ```java
  @ConfigEntry.Gui.Excluded
  @Deprecated
  public int range = 6;

  @ConfigEntry.Gui.Excluded
  @Deprecated
  public int depth = 4;

  @ConfigEntry.Gui.Excluded
  @Deprecated
  public int delay = 2500;
  ```
- **Azione**: Aggiungere l'annotazione `@ConfigEntry.Gui.Excluded` (classe `me.shedaniel.autoconfig.annotation.ConfigEntry`) sopra a ciascun campo deprecato.
- **Invariante**: I campi rimangono `public int`, non `transient`, e conservano i valori predefiniti.

#### Contratto D4 — Tutela Integrale delle Traduzioni
- **Target**: File `.json` in `src/main/resources/assets/minecraft_access/lang/`.
- **Azione**: Nessuna modifica. Le chiavi legacy vengono preservate intatte per garantire compatibilità retroattiva.

#### Contratto D5 — Congelamento `criticalModAudioDucking`
- **Target**: `org.mcaccess.minecraftaccess.features.cognitive.CognitiveCoordinator`.
- **Azione**: Nessuna modifica. Rimane invariato in attesa della fase audio dedicata.

---

### Contratti di Sicurezza e Verifica (S1..S4)

#### Contratto S1 — Le 5 Barriere del Protocollo 11
- **Barriera 1 (Audit)**: Incrocio statico preliminare tra codice Java e risorse completato in Fase 0.
- **Barriera 2 (Dry-Run)**: Presentazione del diff unificato prima del commit.
- **Barriera 3 (Quarantena)**: Snapshot di backup in `$env:TEMP/minecraft_access_quarantine_bonifica/` prima della modifica.
- **Barriera 4 (Verifiche)**: Compilazione, Checkstyle differenziale e suite test JUnit.
- **Barriera 5 (Rollback)**: Procedura non distruttiva (ispezione `git status`/diff, patch inversa o ripristino mirato pre-commit; `git revert` atomico post-commit previa autorizzazione).

#### Contratto S2 — Test Seam Deterministico Round-Trip Gson Produttivo
- **Nuovo File di Test**: `src/test/java/org/mcaccess/minecraftaccess/config/LegacyConfigSerializationTest.java`.
- **Configurazione Serializer**: Istanziazione del serializer Gson esattamente con la configurazione produttiva di `ConfigExtension.serializer(...)` (utilizzando `GsonBuilder` con `IdentifierAdapter` e formattazione indentata a 4 spazi).
- **Isolamento Storage**: Operazioni eseguite esclusivamente su cartella temporanea gestita da JUnit (`@TempDir Path tempDir`).
- **Verifiche da eseguire**:
  1. **Test Round-Trip Profilo A (Personalizzato Reale)**: JSON con `range=6, depth=5, delay=2500` -> Deserializzazione -> Serializzazione su `@TempDir` -> Ricaricamento da file -> `assertEquals` sui 3 valori.
  2. **Test Round-Trip Profilo B (Personalizzato Alternativo)**: JSON con `range=7, depth=3, delay=2500` -> Deserializzazione -> Serializzazione su `@TempDir` -> Ricaricamento da file -> `assertEquals` sui 3 valori.
  3. **Test Round-Trip Profilo C (Valori Limite Zero)**: JSON con `range=0, depth=0, delay=0` -> Deserializzazione -> Serializzazione su `@TempDir` -> Ricaricamento da file -> `assertEquals` sui 3 valori per verificare rigorosamente la preservazione degli zero dopo riscrittura completa su disco.
  4. **Test Round-Trip Profilo D (Assenza Campi Legacy & Campi Duali)**: JSON senza chiavi legacy `range`, `depth`, `delay`, ma con valori custom per campi duali (`proximityMaxRange=4, longRangeScanInterval=4000`) -> Deserializzazione iniziale con assert dei default di classe (`6`, `4`, `2500`) -> Serializzazione su `@TempDir` -> Ricaricamento da file -> Verifica congiunta della persistenza dei default legacy e della persistenza dei campi duali personalizzati dopo il salvataggio.
  5. **Test Riflessivo `@ConfigEntry.Gui.Excluded`**: Verifica tramite reflection che i campi `range`, `depth`, `delay` abbiano l'annotazione `@ConfigEntry.Gui.Excluded` e che nessuno di essi sia marcato `Modifier.isTransient`.

#### Contratto S3 — Verifica Checkstyle Differenziale
- **Verifica Sorgenti Principali**:
  - Esecuzione del comando: `.\gradlew.bat --no-daemon --no-watch-fs checkstyleMain`.
  - Criterio 1: Il conteggio delle violazioni della regola `UnusedImports` nel file `build/reports/checkstyle/main.xml` deve essere esattamente pari a `0`.
  - Criterio 2: Il numero totale delle altre diagnostiche preesistenti non deve mostrare alcun incremento rispetto al baseline iniziale.
- **Verifica Deterministica Nuovo Test**:
  - Esecuzione del comando: `.\gradlew.bat --no-daemon --no-watch-fs checkstyleTest`.
  - Criterio 3: Ispezione del relativo report `build/reports/checkstyle/test.xml` per accertare che il nuovo file `LegacyConfigSerializationTest.java` contenga esattamente 0 errori di stile o import non utilizzati.

#### Contratto S4 — Assenza di Regressioni
- Esecuzione suite completa: `.\gradlew.bat --no-daemon --no-watch-fs test`.
- **Criterio di Successo**: 100% dei test passati (zero fallimenti e zero errori di regressione).

---

## 📁 5. Componenti Coinvolti

### File da Modificare (Chirurgicamente)
1. `src/main/java/org/mcaccess/minecraftaccess/Config.java` (aggiunta 3 annotazioni GUI sui campi legacy)
2. `src/main/java/org/mcaccess/minecraftaccess/features/NumpadControls.java` (rimozione metodo riga 825 e 1 import)
3. `src/main/java/org/mcaccess/minecraftaccess/features/crosshair/CrosshairFeedbackManager.java` (rimozione costante riga 37)
4. `src/main/java/org/mcaccess/minecraftaccess/utils/config/IdentifierAdapter.java` (modificatore reso `public` a supporto condiviso del test seam S2)
5. I 19 file contenenti i rimanenti 29 import inutilizzati censiti in D2.
*(Totale file sorgente tracciati modificati: 23)*

### File Nuovi da Creare (Separati e non ancora tracciati)
1. `src/test/java/org/mcaccess/minecraftaccess/config/LegacyConfigSerializationTest.java` (test seam round-trip Gson produttivo)

### File Blindati (Divieto Assoluto di Tocco)
- Tutte le classi di logica/gameplay (`CentralFallSafetyManager`, `ProximityFallDetector`, `LongRangeFallDetector`, `ObstacleDetector`, `AutoWalk*`, ecc. eccetto la rimozione degli import censiti).
- Tutti i file `.json` in `src/main/resources/assets/minecraft_access/lang/`.
- Tutti i file `config.json` delle istanze reali di PrismLauncher.

---

## 📋 6. Passi Esecutivi Dettagliati (Sotto-Fase 1B post-approvazione)

1. **Passo 1 (Pre-Flight & Quarantena)**: Verifica assenza modifiche nei target sorgente/test e backup in `$env:TEMP/minecraft_access_quarantine_bonifica/`.
2. **Passo 2 (Modifica Perimetro 2 - Config.java)**: Apposizione di `@ConfigEntry.Gui.Excluded` sui tre campi in `Config.java`.
3. **Passo 3 (Implementazione Test Seam Gson)**: Creazione di `LegacyConfigSerializationTest.java` ed esecuzione con validazione round-trip profili A, B, C, D e check riflessivo.
4. **Passo 4 (Modifica Perimetro 1 - Metodo e Costante)**: Rimozione di `narrateCrosshairTarget()` in `NumpadControls.java` e `DEBOUNCE_GRACE_PERIOD_MS` in `CrosshairFeedbackManager.java`.
5. **Passo 5 (Modifica Perimetro 1 - Rimozione 30 Import)**: Rimozione chirurgica dei soli 30 import inutilizzati censiti.
6. **Passo 6 (Verifica Statica Differenziale)**: Esecuzione di `checkstyleMain` con parsing XML per verificare `UnusedImports = 0` e nessun incremento degli errori preesistenti.
7. **Passo 7 (Verifica Automatica Suite)**: Esecuzione di `test` (`.\gradlew.bat --no-daemon --no-watch-fs test`).
8. **Passo 8 (Revisione Diff Lineare)**: Ispezione visiva con `git diff` per verificare l'assoluta conformità al perimetro concordato.

---

## 🔬 7. Matrice di Simulazione a Tre Livelli

- **Livello 1 — Scenario Comune (Happy Path)**:
  - Compilazione pulita, test passati al 100%, Checkstyle differenziale con 0 violazioni `UnusedImports`, i 3 campi legacy non compaiono più nella GUI Cloth Config ma rimangono salvati e riletti correttamente.
- **Livello 2 — Configurazioni Personalizzate Reali**:
  - Un'istanza con `depth = 5` o `depth = 3` mantiene fedelmente tali valori attraverso cicli completi di caricamento e risalvataggio via Gson senza reset a default.
- **Livello 3 — Casi Limite & Corner Cases**:
  - JSON con valori a `0` gestiti senza anomalie;
  - JSON privo dei campi legacy riempito con i default stabili senza errori di parsing e senza alterare i valori personalizzati dei campi duali;
  - Tutte le traduzioni storiche (tedesco, francese, portoghese, ucraino, cinese) rimangono intatte nel bundle JAR senza chiavi mancanti.

---

## ⚖️ 8. Audit sui Sette Assi di Qualità ASTRALIS

- **Validità**: Piena. Disaccoppiamento completo tra pulizia certa interna e messa in sicurezza della superficie persistente.
- **Efficacia**: Azzeramento rumore visivo in Cloth Config, azzeramento codice orfano e azzeramento 30 import Checkstyle.
- **Coerenza**: Pieno rispetto delle invarianti di tutela su import condivisi, traduzioni multilingua e congelamento `criticalModAudioDucking`.
- **Completezza**: 100% degli elementi mappati con contratti formali D0..D5, S1..S4, checklist Sezione 0 e test seam dedicato.
- **Precisione**: Criteri differenziali per Checkstyle e pre-flight attuabile; nessun automatismo cieco.
- **Prestazioni**: Nessun overhead introdotto a runtime; reflection invocata solo alla costruzione GUI una tantum.
- **Assenza di Regressioni**: Non postulata a priori, ma empiricamente vincolata al passaggio formale del test seam Gson, della suite JUnit completa e del diff lineare.

---

## 🔢 9. Impatto Versione AVF

- **Tipo Intervento**: `MAINTENANCE / REFACTORING` (Bonifica interna e pulizia statica).
- **Classificazione AVF**: Nessun incremento di versione applicativa per gli utenti finali (il JAR distribuibile non muta semantica funzionale).
- **Annotazione**: L'intervento sarà registrato nel diario modifiche interno come bonifica di igiene del codice pre-migrazione domini cognitivi.

---

## 🛑 10. Punto di Stop Obbligatorio (Gating Semantico)

> [!IMPORTANT]
> **STOP OBBLIGATORIO (Sotto-Fase 1A Conclusa)**:
> Il presente piano definisce con precisione millimetrica l'intero perimetro dell'intervento.
> **Nessun file di codice sorgente o di configurazione è stato modificato**.
> L'esecuzione della Sotto-Fase 1B è subordinata all'approvazione formale ed esplicita di Luca (*"procedi con la bonifica 1B"*).
