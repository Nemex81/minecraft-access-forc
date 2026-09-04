# Strategia: Tracciatore Risorse Base di Sopravvivenza (Legno, Pietra, Cibo)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA)
# Co-Autore: Antigravity (Senior AI Pair Programmer & Software Engineer)
# Progetto: Minecraft Access
# Data: 2026-08-30

---

## 🎯 1. Obiettivo & Visione Strategica

Fornire al giocatore non vedente un sistema di orientamento e rilevamento essenziale a **basso carico cognitivo**, completamente **autonomo e indipendente** dal POI Radar standard (`ObjectTracker`).

La "triade di sopravvivenza" di Minecraft Vanilla (Legno, Pietra, Cibo) viene scansionata all'istante tramite un comando rapido su tastiera/numpad o periodicamente in background, restituendo una descrizione spaziale 3D unificata e coerente con la semantica del navigatore.

---

## 🧭 2. Criteri di Rilevamento dei Bersagli

### 2.1. Categoria Legno (`WOOD`)
- **Tronchi Naturali**: Tutti i blocchi contrassegnati dal tag `#minecraft:logs` (quercia, betulla, abete, acacia, giungla, ciliegio, mangrovia, pale oak, steli crimson/warped).
- **Assi Piazzate**: Blocchi del tag `#minecraft:planks`.
- **Banchi da Lavoro**: `Blocks.CRAFTING_TABLE` posizionati nel mondo.

### 2.2. Categoria Pietra (`STONE`)
- **Tipi di Pietra**: Pietra naturale (`Blocks.STONE`), Ciottoli (`Blocks.COBBLESTONE`), Deepslate naturale e ciottoloso (`Blocks.DEEPSLATE`, `Blocks.COBBLED_DEEPSLATE`), Andesite, Diorite, Granito, Arenaria (`Blocks.SANDSTONE`, `Blocks.RED_SANDSTONE`).
- **Regola Tassativa di Esposizione all'Aria (Zero Pietra Cieca/Sepolta)**:
  - Il blocco di pietra è considerato valido **solo se ha almeno una faccia adiacente esposta all'aria** (`level.getBlockState(neighbor).isAir()`):
    * Faccia superiore `pos.above()` (pavimento / roccia affiorante su collina).
    * Faccia inferiore `pos.below()` (soffitto grotta / sporgenza).
    * Facce laterali `pos.north()`, `pos.south()`, `pos.east()`, `pos.west()` (parete verticale, scarpata, gola, montagna a vista).
  - Sono rigorosamente esclusi i blocchi ciechi incassati sotto il terreno o all'interno della massa solida delle montagne.

### 2.3. Categoria Cibo Commestibile (`FOOD`)
- **Animali Passivi Commestibili**: Mucche, maiali, pecore, polli, conigli, pesci.
- **Colture Mature (100%)**: Grano, carote, patate, barbabietole (con verifica `CropBlock.isMaxAge() == true`).
- **Frutti e Cespugli**: Cespugli di bacche dolci mature (`SweetBerryBushBlock` con `age >= 2`), blocchi di melone maturo (`Blocks.MELON`).
- **Cibo a Terra**: `ItemEntity` con stack contenente `DataComponents.FOOD`.

---

## 🗣️ 3. Standard di Sintassi e Semantica Vocale

Ogni risorsa rilevata viene descritta componendo 4 parametri lineari:
`[Nome Risorsa] [Distanza e Vettore Relativo], [Cardinale e Gradi Bussola], [Dislivello Verticale]`

1. **Vettore Relativo al Giocatore**:
   - `X blocchi avanti`
   - `X blocchi avanti a destra` / `avanti a sinistra`
   - `X blocchi a destra` / `a sinistra`
   - `X blocchi indietro a destra` / `indietro a sinistra`
   - `X blocchi indietro`
2. **Cardinale e Gradi Bussola ($0^\circ \dots 359^\circ$)**:
   - `nord a X gradi`, `nord-est a X gradi`, `est a X gradi`, `sud-est a X gradi`, `sud a X gradi`, `sud-ovest a X gradi`, `ovest a X gradi`, `nord-ovest a X gradi`.
3. **Dislivello Verticale ($\Delta Y$)**:
   - $\Delta Y > 0$: `in alto X blocchi` (o `in alto 1 blocco`)
   - $\Delta Y < 0$: `in basso X blocchi` (o `in basso 1 blocco`)
   - $\Delta Y == 0$: `stesso livello`
4. **Risorsa Assente**:
   - `[Nome Risorsa] non trovato` / `non trovata`

### Esempio Vocale Finale:
> *"Risorse: Legno 8 blocchi avanti, nord a 10 gradi, in alto 2 blocchi; Pietra 14 blocchi a destra, est a 90 gradi, in basso 3 blocchi; Cibo 6 blocchi indietro a sinistra, sud-ovest a 220 gradi, stesso livello."*

---

## 🕹️ 4. Controlli & Modalità Operative

- **Comando Manuale (On-Demand)**:
  - Numpad: **`Alt + Numpad 7`** (Layer 2 Diagnostica, ergonomico e libero).
  - Tastiera Estesa: **`Alt + B`** (estremamente comodo per la sola mano sinistra, privo di conflitti con chat o comandi vanilla).
- **Scansione Periodica in Background (Opzionale)**:
  - Toggle disattivato di default, attivabile in `Config.java`.
  - Intervallo regolabile (default 30s).
  - Smart Debounce: silenziosa se il giocatore è fermo e non ci sono nuove risorse.
