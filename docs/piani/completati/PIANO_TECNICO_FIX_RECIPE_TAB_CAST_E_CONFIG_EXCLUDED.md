# Piano Tecnico Formale (Fase 1A): Risoluzione ClassCastException Ricettario e Annotazione @Excluded Config

## 📌 0. Stato del Piano
# Stato: Completato con successo e collaudato in-game (Fase 3)

## 📌 1. Quadro di Riferimento & Diagnostica

Il presente piano tecnico risponde alle anomalie riscontrate durante la sessione di collaudo e telemetria in-game del 01/09/2026, documentate in:
- [`docs/report/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REPORT_SESSIONE_TELEMETRIA_E_ANOMALIE_2026-09-01.md)
- [`knowledge/09_registro_bug_e_soluzioni.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/09_registro_bug_e_soluzioni.md) (Record 25)

### Obiettivi Specifici:
1. **Fix ClassCastException Ricettario (`InventoryControls.java`)**:
   - **Sintomo**: Alla pressione del tasto `V` o `Shift+V` per scorrere le schede del ricettario/crafting, l'handler di input crashava con `ClassCastException: RecipeBookCategory cannot be cast to SearchRecipeBookCategory`.
   - **Causa**: In Minecraft 26.2, `recipeBookComponentAccessor.getSelectedTab().getCategory()` restituisce un tipo compatibile con `ExtendedRecipeBookCategory` o `RecipeBookCategory`, ma non `SearchRecipeBookCategory`. Il cast a riga 837 in `log.debug(...)` generava l'eccezione a runtime bloccando la navigazione.
2. **Fix Avviso GUI Mancante per Singleton (`Config.java`)**:
   - **Sintomo**: Nei log di avvio e apertura opzioni compariva `[ERROR]: No GUI provider registered for field 'Config.instance'`.
   - **Causa**: La libreria AutoConfig (Cloth Config) analizza i campi per riflessione; non avendo `@ConfigEntry.Gui.Excluded`, tentava di creare un provider di editing GUI per il campo statico singleton `instance`.

---

## 🛡️ 2. Validazione Preventiva sui 7 Assi

1. **Validità (Formale & Architetturale)**:
   - Utilizzo delle annotazioni native di Cloth Config (`@ConfigEntry.Gui.Excluded`) già presenti nel classpath del progetto.
   - Trattamento polimorfico sicuro dell'interfaccia `ExtendedRecipeBookCategory` tramite `.toString()` o `name()` protetto senza downcast forzati a classi concrete non garantite in Minecraft 26.2.
2. **Efficacia**:
   - Eliminazione istantanea del crash al cambio tab con `V` / `Shift+V` nel ricettario.
   - Azzeramento dell'errore di riflessione AutoConfig nei log.
3. **Coerenza**:
   - Rispetto integrale della struttura e dello stile di logging (`log.debug(...)`) e delle annotazioni di configurazione adottate in `Config.java`.
4. **Completezza**:
   - Rimozione dell'import orfano `SearchRecipeBookCategory` in `InventoryControls.java`.
   - Inserimento di guard checks difensivi (verifica null su `selectedTab` e `category`) e blocco `try-catch` difensivo sull'istruzione di debug per evitare che qualsiasi futura anomalia di logging interferisca con il cambio tab effettivo.
5. **Precisione**:
   - Interventi strettamente mirati e puntuali sulle righe 31 e 836-838 di `InventoryControls.java` e sulla riga 24 di `Config.java`.
6. **Affidabilità & Prestazioni**:
   - Zero allocazioni extra o overhead nel tick di gioco.
   - Garanzia di continuità operativa dell'handler tastiera in-game.
7. **Assenza di Regressioni & Prevenzione Anomalie**:
   - Nessun impatto sulle restanti funzionalità di `InventoryControls` (navigazione a celle, gruppi o lettura ricette `X`).
   - Nessuna alterazione della serializzazione/deserializzazione della configurazione JSON su disco.

---

## 🛠️ 3. Dettaglio delle Modifiche Previste (Sotto-Fase 1B)

### A. Componente `InventoryControls`
- **File**: [`src/main/java/org/mcaccess/minecraftaccess/features/inventory_controls/InventoryControls.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/inventory_controls/InventoryControls.java)
- **Modifiche**:
  1. Rimozione dell'import inutilizzato:
     ```java
     // Rimuovere riga 31:
     import net.minecraft.client.gui.screens.recipebook.SearchRecipeBookCategory;
     ```
  2. Revisione difensiva del metodo `changeRecipeTab`:
     ```java
     // Righe 836-838 attuali:
     ExtendedRecipeBookCategory category = recipeBookComponentAccessor.getSelectedTab().getCategory();
     log.debug("Change tab to {}", ((SearchRecipeBookCategory) category).name());

     // Nuova implementazione protetta:
     try {
         var selectedTab = recipeBookComponentAccessor.getSelectedTab();
         if (selectedTab != null) {
             ExtendedRecipeBookCategory category = selectedTab.getCategory();
             log.debug("Change tab to {}", category != null ? category.toString() : "null");
         }
     } catch (Exception e) {
         log.debug("Could not log recipe tab change", e);
     }
     ```

### B. Componente `Config`
- **File**: [`src/main/java/org/mcaccess/minecraftaccess/Config.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/Config.java)
- **Modifiche**:
  1. Aggiunta dell'annotazione `@ConfigEntry.Gui.Excluded` sul campo singleton `instance`:
     ```java
     // Righe 23-25:
     @Getter
     @ConfigEntry.Gui.Excluded
     private static Config instance;
     ```

---

## 🧪 4. Piano di Verifica e Collaudo

### Sotto-Fase 1B (Verifica Automatica & Compilazione):
- Esecuzione compilazione e packaging Gradle:
  ```powershell
  .\gradlew.bat --no-daemon shadowJar
  ```
- Verifica assenza di errori o warning di compilazione Java 25.

### Fase 2 (Deploy & Collaudo Manuale In-Game):
1. **Test Ricettario (`V` / `Shift+V`)**:
   - Aprire l'inventario o il tavolo da lavoro in-game.
   - Premere `V` per avanzare tra le schede e `Shift+V` per retrocedere.
   - Verificare che il cambio scheda avvenga istantaneamente senza blocchi e che lo screen reader NVDA vocalizzi correttamente gli slot e le ricette aggiornate.
2. **Test Configurazione & Log**:
   - Aprire il menu di configurazione mod o avviare il client.
   - Ispezionare `latest.log` per verificare la totale assenza dell'avviso `No GUI provider registered for field 'Config.instance'`.
