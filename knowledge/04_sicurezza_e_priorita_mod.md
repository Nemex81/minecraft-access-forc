# 04 — Sicurezza, Priorità Mod & Separazione Cartelle

## 1. Gestione del Mod Loader & Priorità di Esecuzione

Il file `.jar` di `minecraft-access` viene iniettato nel runtime di Minecraft come mod client-side primaria:

- **Nome File Standard**: `minecraft-access-1.12.0.jar`
- **Isolamento dei Test**: Prima di distribuire nuove build o aggiornamenti critici nell'istanza di gioco principale di Luca, testare le modifiche su profili separati di PrismLauncher con mondi di prova dedicati.
- **Integrità dei Salvataggi**: Non avviare mai salvataggi del mondo principale con versioni sperimentali non compilate con successo e convalidate da Checkstyle.

---

## 2. Regola Tassativa: Separazione tra Cartella Operativa e Cartella Backup

La gestione dei file rispetta una separazione netta e inviolabile tra i documenti di lavoro e i dati binari di backup:

```
├── C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft\   <-- CARTELLA OPERATIVA UNICA
│   ├── GEMINI.md
│   ├── knowledge/
│   ├── Export_Progressi.txt
│   ├── MANUALE_COMANDI_MINECRAFT_26.2.md
│   ├── ANTIGRAVITY_SCRIVE_A_CHATGPT.md
│   ├── CHATGPT_SCRIVE_AD_ANTIGRAVITY.md
│   │
│   └── minecraft backup\                                                   <-- CARTELLA BACKUP ESCLUSIVA
│       ├── Minecraft 26.2 Access 1.12.0 pc fisso Salotto\
│       ├── Minecraft 26.2 Access 1.12.0 pc portatile\
│       └── scuola_di_sopravvivenza_mondo_2_backup_*.zip
```

### A. Cartella Operativa Ufficiale (`minecraft\`)
- **Scopo**: È l'**UNICA SEDE UFFICIALE** per tutti i file di lavoro correnti: istruzioni, report di verifica, registri progressi, manuali e canali di comunicazione persistenti con ChatGPT.
- **Regola**: Prima di creare o modificare qualsiasi documento di testo, verificare sempre che il percorso sia `...\accessible games\minecraft\`.

### B. Cartella Backup (`minecraft\minecraft backup\`)
- **Scopo**: È riservata **esclusivamente** ai backup compressi dei mondi di gioco (`.zip`), agli archivi delle istanze PrismLauncher e ai `.jar` di rilascio.
- **Divieto Assoluto**: **NON creare, NON modificare e NON duplicare documenti operativi o note di lavoro all'interno di questa cartella**.

---

## 3. Protocollo di Protezione Pre-Deploy

Prima di qualsiasi modifica massiva al codice o all'ambiente di gioco:
1. Verificare che l'ultimo backup del mondo sia presente in `minecraft backup/`.
2. Conservare l'ultimo file `.jar` funzionante prima di sovrascriverlo con la nuova build Gradle.
