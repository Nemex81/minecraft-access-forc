# 07 — Sincronizzazione Macchine, Build & Pipeline di Deploy

## 1. Procedura di Compilazione Locale

La compilazione del progetto avviene tramite Gradle Wrapper e richiede Java 25:

```powershell
# Eseguire dalla radice del repository minecraft-access
.\gradlew.bat shadowJar
```

- **Output generato**: `build\libs\minecraft-access-1.12.0.jar`
- **Verifica preliminare**: Prima della compilazione, Gradle esegue i task di linting e Checkstyle.

---

## 2. Auto-Rilevamento Macchina Corrente ($env:COMPUTERNAME)

Per evitare qualsiasi errore umano nel routing dei backup e dei salvataggi, l'assistente interroga dinamicamente la variabile di sistema `$env:COMPUTERNAME`:

| Hostname di Sistema (`$env:COMPUTERNAME`) | Macchina Identificata | Cartella Backup OneDrive di Riferimento |
|---|---|---|
| **`MSI`** | **PC Portatile** | `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft\minecraft backup\Minecraft 26.2 Access 1.12.0 pc portatile\` |
| **`NEMEXMASTER`** | **PC Fisso Salotto** | `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft\minecraft backup\Minecraft 26.2 Access 1.12.0 pc fisso Salotto\` |

### Protocollo di Apprendimento Dinamico (Auto-Discover):
- Se `$env:COMPUTERNAME` corrisponde a `MSI`, l'assistente opera automaticamente con i percorsi del **PC Portatile** (`Minecraft 26.2 Access 1.12.0`).
- Se `$env:COMPUTERNAME` corrisponde a `NEMEXMASTER`, l'assistente opera automaticamente con i percorsi del **PC Fisso Salotto** (`Minecraft_26.2_Access_1.12.0`).

---

## 3. Pipeline di Deploy & Sequenza di Validazione a Due Fasi

Una volta generato il file `.jar`, il deploy e la messa in sicurezza seguono una sequenza rigorosa a due fasi:

### Fase 1: Deploy Immediato nelle Istanze di Test
Copia simultanea del file `.jar` compilato in tutte le istanze operative locali di PrismLauncher per permettere a Luca di eseguire i test in gioco:
- Su PC Portatile (`MSI`):
  - `c:\Users\nemex\AppData\Roaming\PrismLauncher\instances\Minecraft 26.2 Access 1.12.0\minecraft\mods\minecraft-access-1.12.0.jar`
  - `c:\Users\nemex\AppData\Roaming\PrismLauncher\instances\Minecraft 26.2 Access - Server Tenuta\minecraft\mods\minecraft-access-1.12.0.jar`
- Su PC Fisso Salotto (`NEMEXMASTER`):
  - `c:\Users\nemex\AppData\Roaming\PrismLauncher\instances\Minecraft_26.2_Access_1.12.0\minecraft\mods\minecraft-access-1.12.0.jar`

### Fase 2: Backup Ufficiale OneDrive & Chiusura Piano (Solo Post-Convalida Utente)
**TASSATIVO**: L'aggiornamento della cartella backup su OneDrive e la spunta delle voci nel Piano Tecnico avvengono **esclusivamente DOPO che Luca ha effettuato il test in-game e ha convalidato e confermato con successo le modifiche**:
- Su **PC Portatile** (`MSI`): `.../minecraft backup/Minecraft 26.2 Access 1.12.0 pc portatile/minecraft/mods/`
- Su **PC Fisso Salotto** (`NEMEXMASTER`): `.../minecraft backup/Minecraft 26.2 Access 1.12.0 pc fisso Salotto/minecraft/mods/`
- Aggiornamento contestuale delle caselle di verifica `[x]` nel file del piano tecnico su OneDrive.
3. **Allineamento Configurazione Comandi & Codifica Tassativa `UTF-8 No-BOM` (`options.txt`)**:
   - **Regola di Codifica**: Qualsiasi file `.txt` o `.properties` modificato via script (in particolare `options.txt`) deve essere scritto rigorosamente in **UTF-8 puro senza BOM** (`[System.Text.UTF8Encoding]($false)`). La presenza del BOM (`\uFEFF`) corrompe la lettura del token `version:` da parte del DataFixerUpper di Minecraft provocando il reset integrale delle opzioni.
   - **Verifica Keybinding**: Assicurarsi che i comandi di accessibilità e le mod grafiche (Iris shader toggle su **`F7`**) siano mappati senza conflitti:
   ```properties
   key_key.minecraft_access.inventory_controls.recipe_info:key.keyboard.x
   key_key.iris.toggleShaders:key.keyboard.f7
   key_key.iris.shaderPackSelection:key.keyboard.unknown
   ```

---

## 4. Sincronizzazione Multi-Postazione & Adattività Hardware GPU

Per garantire la perfetta parità e massime prestazioni tra il PC fisso e il portatile di Luca:
- **Rilevamento Adattivo GPU**: Su qualsiasi macchina host (PC Portatile `MSI` o PC Fisso Salotto), analizzare le schede video disponibili e configurare `instance.cfg` per utilizzare l'adattatore grafico più prestante (es. GPU discreta / dedicata):
  ```properties
  UseDiscreteGpu=true
  LaunchMaximized=true
  OverrideWindow=true
  ```
- **Parità Configurazioni**: Le configurazioni collaudate dell'istanza e i file mod vengono sincronizzati nelle rispettive sottocartelle di `minecraft backup\`.
- Prima di avviare una sessione sul PC opposto, verificare la presenza del `.jar` aggiornato e sincronizzare i salvataggi `.zip`.

---

## 5. Regola di Sincronizzazione Salvataggi & Waypoint POI

- **Dualità dei Dati**: In Minecraft Access, i blocchi e il terreno risiedono in `minecraft/saves/<mondo>/`, mentre i **Punti di Interesse (Waypoint)** sono serializzati in `minecraft/config/minecraft-access/waypoints/singleplayer_<mondo>.json`.
- **Regola di Migrazione**: Quando si trasferisce o sincronizza una partita tra PC Portatile e PC Salotto, è **tassativo copiare congiuntamente**:
  1. La cartella del mondo: `minecraft/saves/<mondo>/`
  2. Il file dei waypoints: `minecraft/config/minecraft-access/waypoints/singleplayer_<mondo>.json`
- **Prevenzione Reset POI**: Se il file JSON dei waypoints non viene copiato insieme al salvataggio, il modulo `WaypointManager` inizializza la lista vuota e il primo evento di morte (`autoSaveDeathPoint`) sovrascriverà la lista cancellando i POI storici.
