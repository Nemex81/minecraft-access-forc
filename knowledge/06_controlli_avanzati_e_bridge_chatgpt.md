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
| **`B`** | **Lettura Istantanea Mirino (Mano Sinistra)**: Vocalizzazione atomica a richiesta di blocco/entità, distanza e orientamento secondo Cloth Config | `features/crosshair/CrosshairFeedbackManager.java` |
| **`Alt + B`** | **Tracciatore Risorse Base** (Scansione 3D Legno, Pietra esposta, Cibo con la mano sinistra) | `features/survival_tracker/SurvivalResourceTracker.java` |
| **`Alt + Page Up`** | **Aumenta Volume Passi Giocatore** (+10%, fino a 300%) | `features/PlayerStepSound.java` |
| **`Alt + Page Down`** | **Riduci Volume Passi Giocatore** (-10%, fino a 0%) | `features/PlayerStepSound.java` |

---

## 2. Mappa Architetturale dei 3 Layer del Tastierino Numerico (Numpad Controls - Zero Shift)

Il modulo `NumpadControls.java` organizza l'intera griglia del tastierino numerico su 3 layer funzionali privi di `Shift` (per prevenire ogni accovacciamento o rallentamento involontario):

### A. Layer 0 (Pressione Diretta — Movimento Visuale, Azioni, Stato & Hotbar)
- **`8` / `2`**: Guarda in Alto / Guarda in Basso (tap $15^\circ$, hold continuo).
- **`4` / `6`**: Guarda a Sinistra / Guarda a Destra (rotazione a scatti dinamici con annuncio di settore e gradi bussola reali $0^\circ \dots 359^\circ$ configurabile con `rotationFeedbackMode`, e rintocco acustico modulato a 3 frequenze).
- **`7` / `9`**: Guarda in Alto-Sinistra / Guarda in Alto-Destra (diagonali 2D combinate sia a scatti sia continue).
- **`1` / `3`**: Guarda in Basso-Sinistra / Guarda in Basso-Destra (diagonali 2D combinate sia a scatti sia continue).
- **`5`**: **Centra Orizzonte** (azzera solo il Pitch a $0^\circ$, rintocco sonoro `playSnapSound` e voce opzionale `centerHorizonFeedbackMode`) e legge il blocco nel mirino.
- **`0`**: **Azione Primaria** (Attacca / Rompi blocco / Scava con click sinistro simulato).
- **`Invio`**: **Azione Secondaria** (Usa oggetto / Piazza blocco / Mangia cibo con click destro simulato).
- **`.`**: **Stato Istantaneo Giocatore** (Salute, Fame e Livello a 1 tocco rapido).
- **`+`**: **Seleziona Blocco nel Mirino** (Pick Block / Tasto Centrale Mouse).
- **`-`**: **Sblocca Puntamento / Bersaglio** (Unlock lock-on).
- **`/` e `*`**: Scorrimento Barra Rapida Slot Precedente / Successivo.

### B. Layer 1 (`Ctrl + Numpad` — Bussola Assoluta, Snap Magnetici & Radar POI)
- **`Ctrl + 8` / `Ctrl + 6` / `Ctrl + 2` / `Ctrl + 4`**: Allinea istantaneamente lo sguardo a Nord, Est, Sud, Ovest (con rintocco acustico ed annuncio).
- **`Ctrl + 7` / `Ctrl + 9` / `Ctrl + 1` / `Ctrl + 3`**: Allinea istantaneamente a Nord-Ovest, Nord-Est, Sud-Ovest, Sud-Est.
- **`Ctrl + 5`**: Vocalizza le coordinate assolute $X, Y, Z$ del giocatore.
- **`Ctrl + .`**: **Vocalizza le coordinate assolute del Blocco/Entità nel Mirino**.
- **`Ctrl + 0`**: Ruota lo sguardo di $180^\circ$ alle spalle (*Look Behind*).
- **`Ctrl + /` e `Ctrl + *`**: Categoria POI Precedente / Successiva (Blocchi, Mob, Giocatori).
- **`Ctrl + -` e `Ctrl + +`**: Oggetto POI Precedente / Successivo nella categoria.
- **`Ctrl + Invio`**: **Guarda l'Oggetto Puntato dal Radar POI** (*Look at Current Object*).

### C. Layer 2 (`Alt + Numpad` — Diagnostica, Equipaggiamento, Vertici & Mobilità)
- **`Alt + 8` / `Alt + 2`**: Oggetto in Mano Principale / Mano Secondaria.
- **`Alt + 4`**: Effetti di Stato Attivi (Pozioni, Veleno, Buff, Debuff).
- **`Alt + 6`**: Durabilità residua dell'oggetto impugnato.
- **`Alt + 5`**: **Leggi Direzione & Inclinazione Sguardo** (annuncia direzione orizzontale e gradi di pitch verticale).
- **`Alt + 7`**: **Tracciatore Risorse Base** (Scansione 3D Legno, Pietra esposta all'aria, Cibo commestibile).
- **`Alt + 1`**: Guarda dritto ai piedi (**Nadir**, Pitch $+90^\circ$ diretto senza singolarità).
- **`Alt + 3`**: Guarda dritto in cielo (**Zenith**, Pitch $-90^\circ$ diretto senza singolarità).
- **`Alt + 0`**: Avvia / Ferma marcia automatica (**Auto-Walk**).
- **`Alt + .`**: Alterna corsa/camminata nel navigatore automatico.
- **`Alt + Invio`**: Apri **Access Menu `F4`**.

### D. Layer 3 (`Ctrl + Alt + Numpad` e `Ctrl + Alt + Frecce` — Sonda Direzionale di Percorso)
Il modulo `DirectionalPathScanner.java` implementa una sonda virtuale di cammino per esplorare in anticipo fino a 32 blocchi:
- **`Ctrl + Alt + 8` / `2` / `4` / `6`**: Scansione direttrice **Nord, Sud, Ovest, Est** (Cardinali).
- **`Ctrl + Alt + 7` / `9` / `1` / `3`**: Scansione direttrice **Nord-Ovest, Nord-Est, Sud-Ovest, Sud-Est** (Diagonali).
- **`Ctrl + Alt + 5`**: Scansione **Avanti** (direzione dello sguardo corrente del giocatore).
- **`Ctrl + Alt + 0`**: Scansione **Dietro** (alle spalle del giocatore).
- **`Ctrl + Alt + Freccia Su / Giù / Sinistra / Destra`**: Scansione per computer portatili o tastiere senza tastierino (Avanti, Dietro, Sinistra, Destra) con zero conflitti con mod esterne o shader.

---


## 3. Protocollo del Canale Persistente con ChatGPT

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
