indirizzo pe rconnessione al server personale di minecraft
192.168.1.201 (oppure 192.168.1.201:25565) e premi Invio!

# Piano Operativo: Aggiornamento e Conversione Server a Minecraft 26.2 (Fabric)

Questo documento definisce la strategia completa, le specifiche architetturali e le fasi esecutive per convertire e aggiornare il server Minecraft ([`ilfenomeno-gif/server-minecraft`](https://github.com/ilfenomeno-gif/server-minecraft)) dal vecchio motore **1.20.1 Arclight/Forge** alla nuova architettura nativa **Fabric 26.2**.

La destinazione operativa e repository del server è:
`C:\Users\nemex\OneDrive\Documenti\GitHub\server-minecraft`  
(accessibile anche tramite il collegamento `C:\Users\nemex\Desktop\cartelle preferite\GitHub - progetti di Nemex.lnk`).

Il piano garantisce la contemporanea e perfetta convivenza di:
* **Giocatori non vedenti** (es. Luca), con accesso a sintesi vocale, audio direzionale 3D e navigazione inventario a griglia/gruppi tramite `minecraft-access-1.12.0.jar`.
* **Giocatori normovedenti** (es. Sebastian e amici), con grafica standard, interfaccia visiva nativa (o arricchita da mod HUD/luci dinamiche), senza alcuna sintesi vocale invasiva o alterazione dei comandi visivi.

---

## 1. Obiettivi Chiave del Progetto

1. **Architettura Pulita e Moderna (Fabric 26.2):**
   * Server dedicato basato su **Fabric Loader 0.19.3+** per Minecraft **26.2**.
   * Piena compatibilità con l'istanza client Prism Launcher di Luca (`Minecraft_26.2_Access_1.12.0`) e con i client standard Fabric dei giocatori normovedenti.
2. **Separazione Rigorosa dei Livelli Mod (Server vs Client Non Vedente vs Client Normovedente):**
   * Le mod di puro contenuto (blocchi, navi, armi, cucina, animali) risiedono sia sul server sia sui client.
   * La mod vocale `minecraft-access` risiede **esclusivamente** sul client non vedente.
   * Le mod visive (es. `LambDynamicLights`, `ToroHealth`, minimappa) risiedono **esclusivamente** sui client normovedenti.
3. **Ottimizzazione delle Prestazioni Server (TPS, RAM e Rete):**
   * Inclusione della suite di ottimizzazione nativa Fabric: **`Lithium`**, **`FerriteCore`** e **`Krypton`**.
4. **Connettività P2P Protetta (`e4mc` + Whitelist):**
   * Connessione istantanea per tutti i partecipanti tramite il tunnel **`e4mc (Fabric)`** senza apertura porte sul router.
   * Modalità `online-mode=false` per flessibilità massima di accesso, protetta da `white-list=true` per consentire l'ingresso solo a Luca, Sebastian e amici autorizzati.
5. **Predisposizione Mappa Mondo Reale (Earth Map):**
   * Script automatizzato `aggiorna_mappa.bat` pronto all'uso con i flag `--forceUpgrade --eraseCache` per convertire la mappa al formato NBT/MCA 26.2 non appena verrà importata.
6. **Runtime Java Moderno con Tuning G1GC:**
   * Script `avvia.bat` configurato per Java 21+ con 6 GB di RAM e flag Aikar.

---

## 2. Matrice di Distribuzione e Conversione Mod

### A. Mod di Contenuto e Fisica (Obbligatorie su Server + Tutti i Client)
Tutti i giocatori (non vedenti e normovedenti) e il server condividono queste mod per avere gli stessi blocchi, entità e meccaniche di gioco:

| Mod Originale (Forge 1.20.1) | Nuova Mod (Fabric 26.2) | Categoria / Funzionalità | Server | Client Luca | Client Sebastian |
| :--- | :--- | :--- | :---: | :---: | :---: |
| `FarmersDelight-1.20.1.jar` | **`Farmer's Delight Refabricated`** | Coltivazione avanzata, ricette, tagliere, coltelli | ✅ | ✅ | ✅ |
| `mcw-bridges` | **`Macaw's Bridges (Fabric)`** | Ponti in legno, corda e pietra | ✅ | ✅ | ✅ |
| `mcw-doors` | **`Macaw's Doors (Fabric)`** | Porte, portoni e botole decorate | ✅ | ✅ | ✅ |
| `mcw-furniture` | **`Macaw's Furniture (Fabric)`** | Mobili, tavoli, cassettiere, sedie | ✅ | ✅ | ✅ |
| `mcw-mcwfences` | **`Macaw's Fences (Fabric)`** | Recinzioni moderne e steccati | ✅ | ✅ | ✅ |
| `mcw-mcwwindows` | **`Macaw's Windows (Fabric)`** | Finestre, persiane e vetrate | ✅ | ✅ | ✅ |
| `valkyrienskies` | **`Valkyrien Skies 2 (Fabric)`** | Motore fisico corpi rigidi e navi | ✅ | ✅ | ✅ |
| `eureka` | **`Eureka Ships (Fabric)`** | Costruzione e pilotaggio navi/dirigibili | ✅ | ✅ | ✅ |
| `cookingforblockheads` | **`Cooking for Blockheads (Fabric)`** | Cucina componibile, frigo, lavello, forno | ✅ | ✅ | ✅ |
| `treeharvester` / `FallingTree` | **`FallingTree (Fabric)`** | Abbattimento rapido alberi interi | ✅ | ✅ | ✅ |
| `oreharvester` | **`Ore Harvester (Fabric)`** | Scavo istantaneo vene minerali | ✅ | ✅ | ✅ |
| `sophisticatedbackpacks` | **`Traveler's Backpack (Fabric)`** | Zaini espandibili indossabili con serbatoi | ✅ | ✅ | ✅ |
| `tacz` (Guns mod) | **`Vic's Point Blank (Fabric)`** | Armi da fuoco 3D, accessori e munizioni | ✅ | ✅ | ✅ |
| `alexsmobs` + `citadel` | **`Naturalist (Fabric)`** + **`Ecologics`** | Fauna selvatica dettagliata e nuovi biomi | ✅ | ✅ | ✅ |
| `player-animation-lib` | **`Player Animation Lib (Fabric)`** | Libreria sincronizzazione animazioni | ✅ | ✅ | ✅ |

### B. Librerie di Sistema e Dipendenze (Server + Tutti i Client)
| Libreria Fabric | Scopo | Server | Client Luca | Client Sebastian |
| :--- | :--- | :---: | :---: | :---: |
| **`fabric-api`** | Interfaccia essenziale per le mod Fabric | ✅ | ✅ | ✅ |
| **`architectury`** | Interfaccia multipiattaforma | ✅ | ✅ | ✅ |
| **`cloth-config`** | Schermate e gestione configurazioni | ✅ | ✅ | ✅ |
| **`collective`** | Libreria base Serilum (per Ore Harvester) | ✅ | ✅ | ✅ |
| **`balm`** | Libreria base Waystones / Cooking for Blockheads | ✅ | ✅ | ✅ |
| **`fabric-language-kotlin`**| Runtime Kotlin per mod moderne | ✅ | ✅ | ✅ |

### C. Mod Specifiche per Tipologia di Giocatore e Server
| Mod | Funzione | Server | Client Luca (Non Vedente) | Client Sebastian (TLauncher Vedente) |
| :--- | :--- | :---: | :---: | :---: |
| **`e4mc (Fabric)`** | Condivisione LAN/WAN P2P client-side | ❌ *(Rimosso dal server per prevenire crash comandi)* | ✅ *(PrismLauncher)* | ✅ **SÌ (TLauncher)** |
| **`Lithium (Fabric)`** | Ottimizzazione fisica tick e chunk | ✅ | ✅ | ✅ |
| **`FerriteCore (Fabric)`**| Riduzione consumo memoria RAM | ✅ | ✅ | ✅ |
| **`FallingTree (Fabric)`**| Abbattimento rapido alberi | ✅ | ✅ | ✅ |
| **`sodium (Fabric)`** | Ottimizzazione grafica per giocatori vedenti | ❌ *(Server headless)* | ❌ *(Audio puro)* | ✅ **SÌ (TLauncher RTX 5060)** |
| **`tl_skin_cape`** | Skin e mantelli TLauncher | ❌ | ❌ | ✅ **SÌ (TLauncher)** |
| **`minecraft-access-1.12.0`** | Sintesi vocale, audio 3D, griglie inventario | ❌ *(NO)* | ✅ **SÌ (PrismLauncher)** | ❌ **NO (Nessuna voce)** |

---

## 3. Fasi di Implementazione Operativa

### Fase 1: Creazione e Struttura della Cartella Server
1. Creazione della directory del server in:
   `C:\Users\nemex\OneDrive\Documenti\GitHub\server-minecraft`
2. Download e installazione dei file di avvio **Fabric 26.2** (`fabric-server-launch.jar` + Vanilla `server.jar` 26.2).
3. Creazione del file `eula.txt` con `eula=true`.
4. Configurazione ottimizzata di `server.properties`:
   - `server-port=25565`
   - `online-mode=false`
   - `white-list=true`
   - `enforce-whitelist=true`
   - `difficulty=normal`
   - `motd=Server Minecraft 26.2 Fabric - Tenuta di Luca e Sebastian`
   - `view-distance=10`
   - `simulation-distance=8`
   - `spawn-protection=0`
5. Inserimento dei nickname di Luca e Sebastian in `whitelist.json`.

### Fase 2: Installazione Mod Server-Side e Condivisione Rete
1. Inserimento nella cartella `mods/` del server delle sole mod di contenuto condivise e delle librerie di sistema.
2. Inserimento delle mod di performance server-side (**`Lithium`**, **`FerriteCore`**, **`Krypton`**).
3. Inserimento e configurazione di **`e4mc (Fabric)`** per generare il codice di connessione a ogni avvio.
4. **Verifica anti-crash**: Controllo che nessuna mod grafica client-side (`minecraft-access`, `LambDynamicLights`, `ToroHealth`) sia presente nel server.

### Fase 3: Scripting e Ottimizzazione Memoria
1. **`avvia.bat` (Avvio Standard a 6 GB di RAM con G1GC):**
   ```bat
   @echo off
   title Server Minecraft 26.2 Fabric (Luca e Sebastian)
   java -Xms4G -Xmx6G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -jar fabric-server-launch.jar nogui
   pause
   ```
2. **`aggiorna_mappa.bat` (Script per upgrade Mappa Mondo Reale):**
   ```bat
   @echo off
   title Aggiornamento Mappa Mondo Reale a 26.2
   echo Esecuzione conversione forzata dei chunk con DataFixerUpper...
   java -jar fabric-server-launch.jar --forceUpgrade --eraseCache nogui
   echo Conversione completata con successo!
   pause
   ```
3. **`leggi_codice_e4mc.bat` (Utility rapida per Luca):**
   * Script batch che legge l'ultima riga contenente l'indirizzo `.e4mc.link` dal file `logs/latest.log` e la mostra a schermo/copia negli appunti, facilitando la lettura con lo screen reader.

### Fase 4: Procedura per l'Importazione Futura della Mappa
Non appena la mappa del Mondo Reale sarà disponibile:
1. Copiare la cartella della mappa dentro la root del server rinominandola `world`.
2. Eseguire una copia di sicurezza preventiva `world_backup`.
3. Avviare `aggiorna_mappa.bat` per convertire tutti i file di regione `.mca` e gli NBT al formato 26.2.
4. Avviare il server normalmente con `avvia.bat`.

---

## 4. Stato e Convalida

* [x] **Validità Tecnica**: Piena compatibilità con Fabric Loader 0.19.3+ su MC 26.2 e Java 21+.
* [x] **Coerenza Architetturale**: Eliminazione delle ridondanze e armonizzazione delle dipendenze di tutte le mod.
* [x] **Compatibilità Multi-Client**: Separazione netta tra esperienza non vedente (screen reader solo su client Luca) ed esperienza normovedente (grafica e HUD solo su client Sebastian).
* [x] **Efficacia & Performance**: Integrazione di `Lithium`, `FerriteCore` e `Krypton` per garantire TPS stabili con navi volanti e fisica avanzata.
* [x] **Sicurezza**: Protezione della sessione pubblica `e4mc` tramite whitelist.
