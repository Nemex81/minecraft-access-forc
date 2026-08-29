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

## 2. Mappa Architetturale dei 4 Layer del Tastierino Numerico (Numpad Controls)

Il modulo `NumpadControls.java` organizza l'intera griglia 3x3 del tastierino numerico su 4 layer funzionali:

### A. Layer 0 (Pressione Diretta — Movimento Visuale & Azioni)
- **`8` / `2`**: Guarda in Alto / Guarda in Basso (tap $15^\circ$, hold continuo).
- **`4` / `6`**: Guarda a Sinistra / Guarda a Destra (tap $15^\circ$, hold continuo con **Bussola Acustica Tattile** e lettura direzione finale al rilascio).
- **`7` / `9`**: Guarda in Alto-Sinistra / Guarda in Alto-Destra (diagonali 2D combinate sia a scatti sia continue).
- **`1` / `3`**: Guarda in Basso-Sinistra / Guarda in Basso-Destra (diagonali 2D combinate sia a scatti sia continue).
- **`5`**: Centra Orizzonte (azzera solo il Pitch a $0^\circ$ preservando lo Yaw orizzontale reale) e legge il blocco nel mirino.
- **`0`**: Tasto Azione Primaria (Attacca / Scava).
- **`.`**: Tasto Azione Secondaria (Usa / Piazza).
- **`+` / `-`**: Scorrimento Slot Hotbar Successivo / Precedente.
- **`*`**: Tasto Centrale Mouse (Seleziona Blocco).
- **`/`**: Sblocca Puntamento / Bersaglio.

### B. Layer 1 (Shift + Numpad — Radar POI & Waypoints)
- **`Shift + 7` / `Shift + 9`**: Bersaglio POI Precedente / Successivo.
- **`Shift + 4` / `Shift + 6`**: Categoria POI Precedente / Successiva.
- **`Shift + 5`**: Guarda verso l'oggetto/blocco puntato dal POI attivo.
- **`Shift + 1` / `Shift + 2` / `Shift + 3`**: Bersaglio Rapido Entità / Blocco / Qualsiasi più vicino.
- **`Shift + 8`**: Blocca/Sblocca puntamento (Lock-on).
- **`Shift + 0`**: Contrassegna / Rimuovi contrassegno PDI.

### C. Layer 2 (Ctrl + Numpad — Snap Assoluti & Coordinate)
- **`Ctrl + 8` / `Ctrl + 6` / `Ctrl + 2` / `Ctrl + 4`**: Allinea istantaneamente lo sguardo a Nord, Est, Sud, Ovest.
- **`Ctrl + 7` / `Ctrl + 9` / `Ctrl + 1` / `Ctrl + 3`**: Allinea istantaneamente lo sguardo a Nord-Ovest, Nord-Est, Sud-Ovest, Sud-Est.
- **`Ctrl + 5`**: Vocalizza le coordinate assolute $X, Y, Z$ del giocatore.
- **`Ctrl + 0`**: Ruota lo sguardo di $180^\circ$ alle spalle (*Look Behind*).

### D. Layer 3 (Alt + Numpad — Status, Mobilità & Vertici)
- **`Alt + 8`**: Stato Completo Giocatore (Salute, Fame, Livello).
- **`Alt + 4` / `Alt + 6`**: Oggetto in Mano Principale / Mano Secondaria.
- **`Alt + 5`**: Effetti di Stato Attivi.
- **`Alt + 2`**: Durabilità dell'oggetto impugnato.
- **`Alt + 1`**: Guarda dritto ai piedi (**Nadir**, Pitch $+90^\circ$ diretto senza singolarità).
- **`Alt + 3`**: Guarda dritto in cielo (**Zenith**, Pitch $-90^\circ$ diretto senza singolarità).
- **`Alt + 0`**: Avvia marcia automatica (*Auto-Walk*) verso il bersaglio agganciato.
- **`Alt + .`**: Alterna corsa/camminata nel navigatore automatico.

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
