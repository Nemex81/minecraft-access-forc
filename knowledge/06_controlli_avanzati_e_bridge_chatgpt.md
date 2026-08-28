# 06 — Dominio 2: Controlli In-Game & Bridge Bidirezionale con ChatGPT

## 1. Mappa dei Controlli di Accessibilità da Tastiera

La mod implementa una mappatura completa per eliminare qualsiasi uso del mouse:

| Tasto | Azione di Accessibilità | Modulo Java Responsabile |
|---|---|---|
| **`C`** | Gruppo Logico Successivo (es. Barra Rapida -> Inventario -> Ricette -> Output) | `features/inventory_controls/GroupGenerator.java` |
| **`Shift + C`** | Gruppo Logico Precedente | `features/inventory_controls/GroupGenerator.java` |
| **`I` / `K`** | Cursore Su / Giù tra le celle del gruppo attivo | `features/inventory_controls/InventoryControls.java` |
| **`J` / `L`** | Cursore Sinistra / Destra tra le celle del gruppo attivo | `features/inventory_controls/InventoryControls.java` |
| **`È`** | Selezione / Presa / Deposito stack oggetto | `features/inventory_controls/InventoryControls.java` |
| **`Shift + È`** | Spostamento Rapido (*Quick Move*) dello stack verso altro contenitore | `features/inventory_controls/InventoryControls.java` |
| **`U`** | Stato Carburante & Cottura (tempo residuo fornace, ampolle supporto pozioni) | `features/inventory_controls/InventoryControls.java` |
| **`X`** | Lettura Prerequisiti & Ingredienti Ricetta (con `.ignoreScreenFocus()`) | `features/inventory_controls/InventoryControls.java` |
| **`V` / `Shift + V`** | Scorrimento Schede del Ricettario | `features/inventory_controls/InventoryControls.java` |

---

## 2. Protocollo del Canale Persistente con ChatGPT

Per scambiare dati tecnici, specifiche di codice e coordinamento senza ingombrare la chat di Luca, l'ambiente adotta due file di canale situati esclusivamente nella cartella operativa:  
📁 `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft archivio backup\`

### A. `ANTIGRAVITY_SCRIVE_A_CHATGPT.md`
- **Mittente**: Antigravity (tu).
- **Contenuto**: Report di build, esiti di compilazione Gradle, diagnostica NBT, log di errore o quesiti tecnici.
- **Formato**: Aggiungere sempre data e ora (Timestamp `YYYY-MM-DD HH:MM`) in testa ad ogni nuovo messaggio.

### B. `CHATGPT_SCRIVE_AD_ANTIGRAVITY.md`
- **Mittente**: ChatGPT.
- **Contenuto**: Specifiche logiche, indicazioni per nuovi moduli o istruzioni per Antigravity.
- **Azione di Antigravity**: Consultare sempre l'ultimo record di questo file all'inizio di una sessione.
