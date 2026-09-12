# STRATEGIA: Bonifica e Pulizia Selettiva del Codice Morto e De-Risking Configurazione Legacy

## 📌 Metadati del Documento

- **ID Documento**: STRATEGIA-DEAD-CODE-PURGE-E-CONFIG-RESIDUES
- **Tipo Documento**: STRATEGIA
- **Esito Classificazione**: CONFERMATA
- **Intento Operativo**: REFACTORING
- **Ambito**: PROGETTO:minecraft-access
- **Stato Documento**: ARCHIVIATA
- **Stato Lavoro**: COMPLETATO
- **Gate Operativo**: CHIUSO POST-COLLAUDO
- **Evidenza del Gate**: Collaudo pratico superato con successo da Luca il 2026-09-12; Rev MC-26.24 registrata; versione v26.2-1.21.1 assegnata.
- **Partecipanti e Ruoli**:
  - **Responsabile Decisionale**: Luca
  - **Coordinatore Operativo**: Antigravity
  - **Esecutore Previsto**: Antigravity
  - **Revisori**: GPT / Codex
  - **Collaudatore Finale**: Luca
- **Modalità Operativa**: TRIADE
- **Documenti Correlati**:
  - archivio-piano: docs/piani/completati/PIANO_TECNICO_BONIFICA_CODICE_MORTO_E_CONFIG.md
  - archivio-report: docs/report/archivio/REPORT_HANDOVER_STRATEGIA_BONIFICA_CODICE_MORTO.md
- **Riferimenti Git**: test/bonifica-codice-morto-e-config -> feat/cognitive-orchestrator
- **Ultimo Aggiornamento**: 2026-09-12 12:58 Europe/Rome

---

## 🎯 1. Obiettivo & Modello Mentale

Definire la strategia architetturale per la rimozione controllata di codice morto staticamente certificato e il de-risking dei campi di configurazione legacy (ASTRALIS v3.0.4 — Canone 8, Protocollo 11 e Protocollo 12).

Il principio guida è la **doppia separazione prudente**:
1. **Perimetro 1 (Bonifica Certa & Minimale)**: elementi interni privi di riferimenti o impatti persistenti;
2. **Perimetro 2 (De-Risking Configurazione)**: messa in sicurezza della superficie GUI senza rottura della persistenza Gson e senza alterazione dei file di salvataggio reali.

---

## 🏛️ 2. Specifica dei Due Perimetri di Intervento

### Perimetro 1: Bonifica Certa & Minimale
- **Metodo Morto**:
  - `org.mcaccess.minecraftaccess.features.NumpadControls`: `private void narrateCrosshairTarget()`.
  - *Invariante di Tutela*: Non rimuovere gli import condivisi con `narrateTargetCoordinates()` (`BlockHitResult`, `WorldNarrator`, `PlayerUtils`, `NarrationUtils`).
- **Costante Morta**:
  - `org.mcaccess.minecraftaccess.features.crosshair.CrosshairFeedbackManager`: `private static final long DEBOUNCE_GRACE_PERIOD_MS = 80;`.
- **Inventario Congelato dei 30 Import Inutilizzati (Certificati Checkstyle)**:
  1. `org.mcaccess.minecraftaccess.features.academy.MissionRegistry`: `net.minecraft.world.phys.BlockHitResult`
  2. `org.mcaccess.minecraftaccess.features.autowalk.ClimbAssistantController`: `java.util.ArrayList`
  3. `org.mcaccess.minecraftaccess.features.autowalk.ClimbAssistantController`: `java.util.List`
  4. `org.mcaccess.minecraftaccess.features.autowalk.ClimbAssistantController`: `net.minecraft.world.level.block.state.BlockState`
  5. `org.mcaccess.minecraftaccess.features.door.DoorInteractionHelper`: `org.mcaccess.minecraftaccess.features.autowalk.AutoWalkPathfinder`
  6. `org.mcaccess.minecraftaccess.features.door.DoorInteractionManager`: `net.minecraft.world.level.block.DoorBlock`
  7. `org.mcaccess.minecraftaccess.features.FallDetector`: `java.util.Objects`
  8. `org.mcaccess.minecraftaccess.features.FallDetector`: `org.mcaccess.minecraftaccess.MainClass`
  9. `org.mcaccess.minecraftaccess.features.inventory_controls.GroupGenerator`: `net.minecraft.client.gui.screens.inventory.CraftingScreen`
  10. `org.mcaccess.minecraftaccess.features.inventory_controls.InventoryControls`: `net.minecraft.client.gui.screens.inventory.CraftingScreen`
  11. `org.mcaccess.minecraftaccess.features.inventory_controls.InventoryControls`: `net.minecraft.client.gui.screens.inventory.InventoryScreen`
  12. `org.mcaccess.minecraftaccess.features.NumpadControls`: `org.mcaccess.minecraftaccess.utils.NarrationPriority`
  13. `org.mcaccess.minecraftaccess.features.ObstacleDetector`: `net.minecraft.sounds.SoundEvents`
  14. `org.mcaccess.minecraftaccess.features.ObstacleDetector`: `net.minecraft.sounds.SoundSource`
  15. `org.mcaccess.minecraftaccess.features.point_of_interest.ObjectTracker`: `java.util.stream.Stream`
  16. `org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.POIWaypoints`: `lombok.Getter`
  17. `org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.WaypointManager`: `net.blay09.mods.balm.Balm`
  18. `org.mcaccess.minecraftaccess.features.point_of_interest.waypoints.WaypointUtils`: `net.minecraft.client.Minecraft`
  19. `org.mcaccess.minecraftaccess.features.safety.fall.LongRangeFallDetector`: `org.jetbrains.annotations.Nullable`
  20. `org.mcaccess.minecraftaccess.features.safety.fall.LongRangeFallDetector`: `org.mcaccess.minecraftaccess.features.cognitive.SourceDomain`
  21. `org.mcaccess.minecraftaccess.features.safety.traversal.ClimbTraversalAnalyzer`: `java.util.List`
  22. `org.mcaccess.minecraftaccess.features.safety.traversal.TraversalSafetyAnalyzer`: `net.minecraft.tags.BlockTags`
  23. `org.mcaccess.minecraftaccess.features.survival_tracker.SurvivalScanner`: `java.util.Comparator`
  24. `org.mcaccess.minecraftaccess.features.survival_tracker.SurvivalScanner`: `net.minecraft.client.Minecraft`
  25. `org.mcaccess.minecraftaccess.features.survival_tracker.SurvivalScanner`: `net.minecraft.tags.ItemTags`
  26. `org.mcaccess.minecraftaccess.features.survival_tracker.SurvivalScanner`: `org.jetbrains.annotations.NotNull`
  27. `org.mcaccess.minecraftaccess.features.survival_tracker.SurvivalScanner`: `org.mcaccess.minecraftaccess.Config`
  28. `org.mcaccess.minecraftaccess.mixin.ClientPacketListenerMixin`: `org.mcaccess.minecraftaccess.MainClass`
  29. `org.mcaccess.minecraftaccess.mixin.ToastManagerMixin`: `org.mcaccess.minecraftaccess.MainClass`
  30. `org.mcaccess.minecraftaccess.utils.position.Orientation`: `net.minecraft.client.Minecraft`

### Perimetro 2: De-Risking Configurazione Legacy
- **Campi Deprecati in `Config.FallDetector`**:
  - `public int range = 6;`
  - `public int depth = 4;`
  - `public int delay = 2500;`
  - *Intervento*: Aggiunta dell'annotazione `@ConfigEntry.Gui.Excluded` su ciascuno dei 3 campi.
  - *Contratto Semantico*: I campi non sono rimossi né rinominati. Rimangono accessibili a reflection e Gson, ma vengono esclusi dalla costruzione dell'albero UI di Cloth Config.
  - *Regola di Cautela Gson*: La rimozione immediata dei campi non è autorizzata perché non garantirebbe la conservazione reversibile dei dati legacy durante il round-trip (lettura, riscrittura e rilettura).
  - *Tutela File Reali*: Divieto assoluto di toccare i file `config.json` reali delle istanze PrismLauncher. Ogni verifica avverrà su fixture isolate in cartelle temporanee.

---

## 🚫 3. Delimitazione Negativa Tassativa (Cosa NON Fare)

1. **Divieto di Rimozione Campi Config**: I campi `range`, `depth`, `delay` NON devono essere cancellati né dichiarati `transient`.
2. **Divieto di Modifica Traduzioni**: Nessuna chiave di traduzione in `assets/minecraft_access/lang/*.json` (italiano, inglese, tedesco, francese, portoghese, ucraino, cinese) deve essere cancellata o alterata.
3. **Divieto di Modifica su `criticalModAudioDucking`**: Il campo in `CognitiveCoordinator.java` NON deve essere rimosso, esposto in `Config`, tradotto né attivato parzialmente in questa sessione. La sua valutazione è rinviata alla fase audio dedicata.
4. **Delimitazione Rilevatori e Facciate Operative**:
   - In `FallDetector.java`, `ObstacleDetector.java` e `LongRangeFallDetector.java` è autorizzata **esclusivamente** la rimozione degli specifici import inutilizzati nominati nell'inventario del Perimetro 1 (`java.util.Objects`, `MainClass`, `SoundEvents`, `SoundSource`, `Nullable`, `SourceDomain`).
   - È fatto **divieto assoluto** di modificare qualsiasi linea di codice applicativo, logica, firme di metodi, campi, flussi o formattazione non necessaria in queste o altre classi del gameplay.
   - Nessun tocco di alcun tipo ad `AutoWalkController.java`, `ProximityFallDetector.java` o `CentralFallSafetyManager.java`.
5. **Divieto di Deploy**: Nessun file `.jar` deve essere copiato nelle istanze PrismLauncher per questa bonifica puramente statica.
6. **Divieto di Comandi Distruttivi**: Divieto tassativo di `git reset --hard`, `git checkout --` o `git restore` distruttivo non circoscritto.

---

## 🛡️ 4. Materializzazione del Protocollo 11 (Le 5 Barriere di Pulizia)

1. **Barriera 1 — Audit a Doppia Chiave**:
   - Incrocio tra codice Java, configurazioni mixin e risorse JSON per certificare che i 32 elementi rimossi non abbiano risoluzioni dinamiche o bindings occulti.
2. **Barriera 2 — Dry-Run Lineare NVDA**:
   - Presentazione del diff testuale compatto ed esplicito prima di qualunque modifica persistente su disco.
3. **Barriera 3 — Quarantena di Sicurezza**:
   - Creazione di una copia di backup temporanea e isolata dei file interessati prima della modifica.
4. **Barriera 4 — Compilazione & Verifica Automatica**:
   - Esecuzione di `compileJava`, `compileTestJava`, `checkstyleMain` (con verifica abbattimento a 0 degli `UnusedImports`) e suite test JUnit.
5. **Barriera 5 — Procedura di Rollback Sicura e Non Distruttiva**:
   - *Verifica preventiva*: ispezione obbligatoria con `git status` e diff lineare.
   - *Ripristino pre-commit*: patch inversa o ripristino mirato limitato esclusivamente alle sole righe modificate dalla bonifica, con divieto assoluto di comandi distruttivi (`git checkout --`, `git restore` indiscriminato o `git reset --hard`).
   - *Ripristino post-commit*: in caso di regressioni post-commit, esecuzione di `git revert` del solo commit atomico dedicato, previa esplicita autorizzazione di Luca.

---

## 🧪 5. Contratto di Prova di Round-Trip Gson (Test Seam Dedicato)

La Sotto-Fase 1B dovrà implementare un test unitario dedicato (`LegacyConfigSerializationTest`) in `src/test/java` che verifichi:
1. **Fixture A (Valori Personalizzati Reali)**: JSON contenente `range = 6`, `depth = 5`, `delay = 2500`.
2. **Fixture B (Valori Personalizzati Alternativi)**: JSON contenente `range = 7`, `depth = 3`, `delay = 2500`.
3. **Fixture C (Valori Limite)**: JSON con valori a `0` per escludere clamping o riscritture involontarie sui campi legacy.
4. **Ciclo di Round-Trip**: Deserializzazione -> Risalvataggio su file temporaneo -> Ricaricamento -> Assert di uguaglianza semantica dei 3 campi legacy e dei nuovi campi duali.
5. **Verifica Riflessiva GUI Excluded**: Verifica che i 3 campi abbiano l'annotazione `@ConfigEntry.Gui.Excluded` e non siano marcati `transient`.

---

## ⚖️ 6. Valutazione Sintetica sui 7 Assi di Qualità ASTRALIS

- **Validità**: Piena separazione tra bonifica certa e de-risking GUI senza alterare lo schema Gson.
- **Efficacia**: Azzeramento di 1 metodo morto, 1 costante orfana, 30 import inutilizzati e pulizia visiva della GUI Cloth Config.
- **Coerenza**: Piena tutela degli import condivisi in `NumpadControls`, delimitazione selettiva degli import su `FallDetector`/`ObstacleDetector` e congelamento negativo di `criticalModAudioDucking`.
- **Completezza**: Inventario nominale congelato dei 30 import, 5 barriere del Protocollo 11, test di round-trip Gson e delimitazione negativa esplicita.
- **Precisione**: Identificazione univoca tramite classe, firma e nome completo dei simboli Java. Rischio qualificato come staticamente minimo e subordinato alle verifiche.
- **Prestazioni**: Lavoro a runtime nullo; rimozione di elementi inerti e aggiunta di metadati di reflection solo in fase di setup GUI.
- **Assenza di Regressioni**: Non dichiarata come promessa a priori in Fase 0, ma contrattualizzata come subordinata alle verifiche empiriche della Sotto-Fase 1B: round-trip Gson su fixture temporanee, Checkstyle con zero errori `UnusedImports`, suite completa dei test superata e revisione del diff ristretto.

---

## 🔬 7. Matrice di Simulazione a 3 Livelli

- **Livello 1 (Happy Path)**: Rimozione elementi inerti, aggiunta annotazioni, compilazione pulita, test passati e zero violazioni `UnusedImports`.
- **Livello 2 (Configurazioni Reali & Round-Trip)**: Conservazione fedele di valori personalizzati (`depth = 3` o `5`) attraverso salvataggio e ricaricamento su fixture temporanea. File originali dell'utente intatti.
- **Livello 3 (Casi Limite & Concorrenza)**: Campi legacy mancanti nel JSON (fallback a default), localizzazioni storiche non-IT/EN preservate, zero modifiche al coordinatore cognitivo o all'audio.

---

## 📊 8. Registro di Convergenza Fase 0 (Checklist a 3 Stati)

- `[/]` **Invariante 1**: Separazione rigida nei 2 Perimetri di bonifica (In attesa di convalida GPT).
- `[/]` **Invariante 2**: Conservazione invariata dello schema persistente Gson e divieto di cancellazione campi (In attesa di convalida GPT).
- `[/]` **Invariante 3**: Conservazione integrale delle chiavi nei file `lang/*.json` (In attesa di convalida GPT).
- `[/]` **Invariante 4**: Tutela degli import condivisi in `NumpadControls` (In attesa di convalida GPT).
- `[/]` **Invariante 5**: Delimitazione negativa su `criticalModAudioDucking` e facciate (In attesa di convalida GPT).
- `[/]` **Invariante 6**: Attuazione delle 5 barriere del Protocollo 11 e test round-trip Gson (In attesa di convalida GPT).
- `[ ]` **Gating Sotto-Fase 1A**: Approvazione formale di Luca per la stesura del Piano Tecnico (NON AUTORIZZATO — Gate CONSULTIVO).
