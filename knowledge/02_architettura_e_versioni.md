# 02 — Architettura, Runtime & Versioni

## 1. Stack Tecnologico & Runtime Congelato

Il progetto `minecraft-access` è sviluppato su uno stack multi-loader moderno per Minecraft Java Edition:

- **Versione Gioco**: Minecraft 26.2 (1.21.x Snapshot/Release).
- **Mod Loaders**: Architettura unificata per **Fabric** e **NeoForge**.
- **Build System**: Gradle 8.x con **Architectury Loom**.
- **Bytecode Injection**: **SpongePowered Mixin** (deoffuscamento tramite Yarn mappings).
- **JDK Richiesto**: **Java 25** (necessario per le nuove API di Minecraft 26.2 e Loom).
- **Nome Mod / Versione Attiva**: `minecraft-access-1.12.0.jar`.

---

## 2. Mappatura dei Percorsi di Sistema

Tutte le sessioni operative e gli script fanno riferimento ai seguenti percorsi fisici standardizzati:

| Ambito | Percorso Fisico | Note |
|---|---|---|
| **Repository Sorgenti (Git / Sviluppo)** | `c:\Users\nemex\OneDrive\Documenti\GitHub\minecraft-access` | Albero dei sorgenti Java, Mixin, assets I18N, configurazioni Gradle, schede `knowledge/` e documentazione in `docs/`. |
| **Cartella Master Archivio & Backup** | `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft archivio backup\` | Sede di archiviazione storica, progetti edilizi, prompt e canali ChatGPT. |
| **Cartella Backup Istanze & Mondi** | `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft archivio backup\minecraft backup\` | Sede esclusiva per backup `.zip` dei mondi e istanze pronte. |
| **Istanza Attiva PrismLauncher** | `c:\Users\nemex\AppData\Roaming\PrismLauncher\instances\Minecraft 26.2 Access 1.12.0\minecraft\` | Ambiente di gioco locale sul PC gaming. |
| **Mod Installata Attiva** | `.../minecraft/mods/minecraft-access-1.12.0.jar` | File JAR attivo eseguito da PrismLauncher. |
| **Log di Gioco a Runtime** | `.../minecraft/logs/latest.log` | File di log consultato per la diagnostica runtime. |
| **Configurazione Comandi In-Game** | `.../minecraft/options.txt` | Keybinding e preferenze utente di Minecraft. |

---

## 3. Struttura dei Package Sorgente (`src/`)

Il codice risiede nel package radice `org.mcaccess.minecraftaccess`:

- **`features/`**:
  - `inventory_controls/`: Navigazione a griglia e a gruppi (`GroupGenerator.java`, `InventoryControls.java`, `SlotsGroup.java`).
  - `camera_controls/`: Controllo visuale da tastiera e lettura orientamento.
  - `pointing/`: Identificazione e annuncio vocale di blocchi ed entità mirati.
- **`mixin/`**: Mixin SpongePowered iniettati nel client Minecraft (`AbstractRecipeBookScreen`, `HandledScreen`, ecc.).
- **`screen_reader/`**: Proxy di comunicazione verso NVDA e la sintesi vocale di sistema.
- **`config/`**: Serializzazione e deserializzazione delle impostazioni (`config/minecraft_access/config.json`).
- **`utils/`**: Funzioni pure di utilità e manipolazione dati.

---

## 4. Matrice Dinamica dei Prerequisiti & Pre-Flight Environment Check

Prima di avviare qualsiasi compilazione o test, l'ambiente locale viene verificato dinamicamente:

### A. Prerequisiti Hard (Mandatori per compilare ed eseguire):
1. **JDK 25 (Microsoft LTS / Epsilon Runtime)**:
   - Scansione dinamica in `$env:ProgramFiles\Microsoft\jdk-25*` e `$env:APPDATA\PrismLauncher\java\*`.
2. **Flag Gradle Anti-Daemon Lock**:
   - Compilazione obbligatoria con flag `--no-daemon` per prevenire il blocco file `Access is denied` su Windows/OneDrive.
3. **PrismLauncher & Istanze di Gioco**:
   - Rilevamento automatico in `$env:APPDATA\PrismLauncher\instances\` con pattern `*26.2*Access*`.

### B. Prerequisiti Soft (Qualità & Versioning):
1. **Git 2.40+** per gestione rami `mymaster`, `dev` e Conventional Commits.
2. **PowerShell 5.1+ / 7+** con policy di esecuzione script abilitata.

### C. Script di Pre-Flight Check Dinamico (PowerShell):
```powershell
# Pre-Flight Check: scansione dinamica dell'ambiente di compilazione
$jdk = Get-ChildItem "$env:ProgramFiles\Microsoft\jdk-25*", "$env:APPDATA\PrismLauncher\java\*" -Directory -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1

if ($jdk -and (Test-Path "$($jdk.FullName)\bin\javac.exe")) {
    $env:JAVA_HOME = $jdk.FullName
    Write-Host "✅ Pre-Flight OK: Java 25 individuato e impostato su $($jdk.FullName)"
} elseif (Get-Command javac -ErrorAction SilentlyContinue) {
    Write-Host "✅ Pre-Flight OK: Java di sistema disponibile: $((Get-Command javac).Source)"
} else {
    Write-Error "❌ Pre-Flight FAILED: Nessun JDK 25 trovato nei percorsi standard o in PrismLauncher!"
}
```

