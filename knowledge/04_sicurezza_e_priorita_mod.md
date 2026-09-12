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
├── $env:OneDrive\Documenti\GitHub\minecraft-access\             <-- CARTELLA OPERATIVA PRIMARIA & SVILUPPO
│   ├── src/ (Codice Java Mod Fabric/NeoForge)
│   ├── gemini.md (Hub Master di Contesto)
│   ├── knowledge/ (Schede Architetturali 00..12)
│   └── docs/ (Documentazione Tecnica e Organizzativa)
│       ├── piani/
│       │   ├── attivi/ (Piani in lavorazione/da collaudare)
│       │   └── completati/ (Piani collaudati e integrati)
│       ├── strategie/ (Documenti strategici e architetturali)
│       ├── report/ (Rapporti di collaudo e verifiche)
│       ├── idee/ (Promemoria e spunti futuri)
│       └── manuali/ (Manuali in-game)
│
└── $env:OneDrive\progetti dei frati\accessible games\minecraft archivio backup\ <-- CARTELLA ARCHIVIO, BACKUP & PROGETTI IN-GAME
    ├── archivio completati/ (Archivio storico)
    ├── progetto casa personale/ & mappa originale del server vecchio/
    ├── prompts/ (Prompt specifici per macchine - Privati)
    ├── CHATGPT.md, ANTIGRAVITY_SCRIVE_A_CHATGPT.md (Canali ChatGPT)
    │
    └── backup istanze\                                                   <-- NUOVA SUITE DI BACKUP AUTOMATIZZATO
```

---

## 3. NORME DI SICUREZZA PER LE MODIFICHE

### A. Repositori Git (`minecraft-access`)
- **Regola**: Tutte le modifiche al codice sorgente avvengono **esclusivamente** sul repository Git locale in `Documenti\GitHub\minecraft-access`.
- **Rami Attivi**:
  - `mymaster`: Branch master personale stabile del fork.
  - `dev` / `feat/*`: Branch di lavoro per nuove funzionalità e fix.
- **Divieto**: Nessun file di codice sorgente o progetto Java/Gradle deve essere modificato direttamente nelle cartelle di installazione di PrismLauncher o nei backup su OneDrive.

### B. Istanze PrismLauncher (`C:\Users\nemex\AppData\Roaming\PrismLauncher\instances\`)
- **Ruolo**: Ambiente di runtime per il gioco reale e per i collaudi pratici.
- **Operazioni Consentite**:
  - Sovrascrittura del file `.jar` della mod compilata nella cartella `mods/` (Deploy Proattivo).
  - Lettura dei log (`latest.log`, `crash-reports/`).
  - Ispezione e aggiornamento dei salvataggi mondo in `saves/`.
- **Divieto**: Non modificare mai le librerie Fabric/NeoForge o la struttura dell'istanza senza aver prima documentato le modifiche.

### C. Cartella Suite Backup (`backup istanze\`)
- **Ruolo**: Sede centrale per i backup automatizzati di mondi e istanze gestiti dalla suite PowerShell `MinecraftSync.ps1`.
- **Politica di Conservazione**:
  - I backup vengono generati e sincronizzati automaticamente con hashing SHA-256 e profilo macchina.
  - Conservare sempre almeno 3 snapshot recenti di ogni mondo attivo.
- **Procedura di Emergenza**: In caso di corruzione del mondo o dell'istanza durante una sessione di test:
  1. Verificare che l'ultimo backup del mondo sia presente in `backup istanze/`.
2. Conservare l'ultimo file `.jar` funzionante prima di sovrascriverlo con la nuova build Gradle.
