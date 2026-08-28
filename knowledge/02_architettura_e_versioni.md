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
| **Repository Sorgenti (Git)** | `c:\Users\nemex\OneDrive\Documenti\GitHub\minecraft-access` | Albero dei sorgenti Java, Mixin, assets I18N e configurazioni Gradle. |
| **Cartella Operativa Documenti** | `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft\` | Unica sede di documentazione operativa, log, canali ChatGPT e registri. |
| **Cartella Backup & Istanze** | `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft\minecraft backup\` | Sede esclusiva per backup `.zip` dei mondi e istanze pronte. |
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
