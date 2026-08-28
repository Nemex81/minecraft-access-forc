# 04 — Sicurezza, Priorità Mod & Separazione Cartelle

## 1. Gestione del Mod Loader & Priorità di Esecuzione

Il file `.jar` di `minecraft-access` viene iniettato nel runtime di Minecraft come mod client-side primaria:

- **Nome File Standard**: `minecraft-access-1.12.0.jar`
- **Isolamento dei Test**: Prima di distribuire nuove build o aggiornamenti critici nell'istanza di gioco principale di Luca, testare le modifiche su profili separati di PrismLauncher con mondi di prova dedicati.
- **Integrità dei Salvataggi**: Non avviare mai salvataggi del mondo principale con versioni sperimentali non compilate con successo e convalidate da Checkstyle.

---

## 2. Regola Tassativa: Gerarchia e Ruoli delle Cartelle

La gestione dei file rispetta una separazione netta e ordinata tra sviluppo operativo, documentazione e backup:

```
├── C:\Users\nemex\OneDrive\Documenti\GitHub\minecraft-access\             <-- CARTELLA OPERATIVA PRIMARIA & SVILUPPO
│   ├── src/ (Codice Java Mod Fabric/NeoForge)
│   ├── gemini.md (Hub Master di Contesto)
│   ├── knowledge/ (Schede Architetturali 01..10)
│   └── docs/ (Documentazione Tecnica e Organizzativa)
│       ├── piani/
│       │   ├── attivi/ (Piani in lavorazione/da collaudare)
│       │   └── completati/ (Piani collaudati e integrati)
│       ├── strategie/ (Documenti strategici e architetturali)
│       ├── report/ (Rapporti di collaudo e verifiche)
│       ├── idee/ (Promemoria e spunti futuri)
│       └── manuali/ (Manuali in-game)
│
└── C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft\ <-- CARTELLA ARCHIVIO, BACKUP & PROGETTI IN-GAME
    ├── gemini.md & knowledge/ (Copia di sicurezza sincronizzata)
    ├── archivio completati/ (Archivio storico)
    ├── progetto casa personale/ & mappa originale del server vecchio/
    ├── prompts/ (Prompt specifici per macchine)
    ├── CHATGPT.md, ANTIGRAVITY_SCRIVE_A_CHATGPT.md (Canali ChatGPT)
    │
    └── minecraft backup\                                                  <-- CARTELLA BACKUP ESCLUSIVA
        ├── Minecraft 26.2 Access 1.12.0 pc fisso Salotto\
        ├── Minecraft 26.2 Access 1.12.0 pc portatile\
        └── scuola_di_sopravvivenza_mondo_2_backup_*.zip
```

### A. Cartella Operativa Primaria (`Documenti\GitHub\minecraft-access\`)
- **Scopo**: È la **SEDE PRIMARIA DI SVILUPPO**: codice sorgente, build Gradle, rami Git (`mymaster`, `dev`), piani e documentazione tecnica strutturata in `docs/`.
- **Regola**: Qualsiasi nuovo piano tecnico attivo va creato in `docs\piani\attivi\`.

### B. Cartella Master Archivio & Progetti In-Game (`accessible games\minecraft\`)
- **Scopo**: È la sede per i progetti edilizi/planimetrie in gioco, archivio storico, prompt di configurazione e canali di dialogo persistenti con ChatGPT.
- **Sincronizzazione**: Mantiene una copia specchiata di sicurezza di `knowledge/` e `gemini.md`.

### C. Cartella Backup Esclusiva (`minecraft backup\`)
- **Scopo**: Riservata **esclusivamente** ai backup compressi dei mondi di gioco (`.zip`), agli archivi delle istanze PrismLauncher e ai `.jar` di rilascio stabili.
- **Divieto Assoluto**: **NON creare, NON modificare e NON duplicare documenti operativi o note di lavoro all'interno di questa cartella**.

---

## 3. Protocollo di Protezione Pre-Deploy

Prima di qualsiasi modifica massiva al codice o all'ambiente di gioco:
1. Verificare che l'ultimo backup del mondo sia presente in `minecraft backup/`.
2. Conservare l'ultimo file `.jar` funzionante prima di sovrascriverlo con la nuova build Gradle.
