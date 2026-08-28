# Roadmap Implementazione Supporto Tastierino Numerico (Numpad Controls)

Documento di riferimento strategico e tecnico per l'integrazione del supporto completo al tastierino numerico (Numpad) in **Minecraft Access**, progettato come consolle di controllo tattile per giocatori non vedenti.

---

## 1. Obiettivo e Visione

Il tastierino numerico (Numpad) offre un vantaggio ergonomico e cognitivo fondamentale:
1. **Griglia fisica 3x3 naturale**: con il tasto `5` come perno tattile centrale di riferimento (grazie al rilievo fisico).
2. **Tasti perimetrali grandi e facilmente distinguibili**: `+`, `Enter`, `-`, `0`, `.`, `/`, `*`.
3. **Controllo a una sola mano**: combinato con i modificatori standard (`Shift`, `Ctrl`, `Alt`), permette di avere 4 livelli operativi logici distinti senza mai spostare la mano dal tastierino, lasciando l'altra mano libera per il movimento (WASD) o per la pressione dei modificatori.
4. **Coesistenza e Indipendenza**: Il modulo si aggiunge come sistema parallelo ai controlli da tastiera esistenti (`I, J, K, L`, `Page Up/Down`, `[`, `]`, `\`), lasciandoli attivi e pienamente funzionanti.

---

## 2. Schema a Livelli Funzionali (Layer Architecture)

* **Livello 0 (Numpad liscio - Diretto)**: Telecamera, Sguardo relativo & Interazione/Mouse (Combattimento / Scavo / Uso).
* **Livello 1 (`Shift` + Numpad)**: Scansione POI, Navigazione bersagli (Object Tracker) & Lock-on.
* **Livello 2 (`Ctrl` + Numpad)**: Orientamento Assoluto (Punti Cardinali, Zenith, Nadir, Snap e Coordinate).
* **Livello 3 (`Alt` + Numpad)**: Ispezione Ambiente, Stato Giocatore, HUD & Utility Rapide.

---

## 3. Mappatura Dettagliata dei Tasti (Preset Standard / Destrorso)

### 🔹 Livello 0: Numpad Diretto (Telecamera & Azioni di Base)

### 2.1 Livello 0: Numpad Diretto (Navigazione & Interazione Base)
*Nessun modificatore premuto (Tastierino liscio)*

* **Rotazione Sguardo a Croce (Dual-Mode: Scatto discreto su tocco rapido / Rotazione continua su tenuta prolungata dopo 200ms)**:
  - `Numpad 8`: Guarda in Alto (Pitch Up / +15° o +45°) / Rotazione continua su hold
  - `Numpad 2`: Guarda in Basso (Pitch Down / -15° o -45°) / Rotazione continua su hold
  - `Numpad 4`: Ruota a Sinistra (Yaw Left / -15° o -45°) / Rotazione continua su hold
  - `Numpad 6`: Ruota a Destra (Yaw Right / +15° o +45°) / Rotazione continua su hold
* **Diagonali e Livellamento Rapido**:
  - `Numpad 7`: Inclinazione rapida verso l'alto (+45°) / Rotazione continua su hold
  - `Numpad 9`: Inclinazione rapida verso il basso (-45°) / Rotazione continua su hold
  - `Numpad 1`: Guarda dritto verso il basso (Nadir / Piedi a 90°)
  - `Numpad 3`: Guarda dritto verso l'alto (Zenith / Cielo a 90°)
  - `Numpad 5`: **Centra Orizzonte e Leggi Mirino** (Pitch a 0° e lettura blocco/entità puntato)
  - `Numpad 0`: **Leggi Direzione Sguardo** (annuncia direzione orizzontale e inclinazione)
  - `Numpad .`: **Snap Cardinale Rapido** (allinea al punto cardinale orizzontale più vicino con audio cue) |
| **Numpad +** | Tasto Sinistro Mouse | Attacca / Rompi blocco (supporta pressione e mantenimento) |
| **Numpad Enter** | Tasto Destro Mouse | Usa oggetto / Piazza blocco / Interagisci (supporta hold) |
| **Numpad -** | Sblocca mira (Unlock) | Sblocca il lock-on corrente |
| **Numpad /** | Hotbar Indietro | Simula rotella mouse in su (slot precedente) |
| **Numpad \*** | Hotbar Avanti | Simula rotella mouse in giù (slot successivo) |

---

### 🔹 Livello 1: `Shift` + Numpad (Scansione, POI & Object Tracker)

| Combinazione | Funzione Proposta | Descrizione / Azione |
| :--- | :--- | :--- |
| **Shift + 8** | Elemento Precedente | Naviga all'oggetto/entità precedente nel gruppo (`Page Up`) |
| **Shift + 2** | Elemento Successivo | Naviga all'oggetto/entità successivo nel gruppo (`Page Down`) |
| **Shift + 4** | Categoria Precedente | Passa al gruppo POI precedente (Ostili, Passivi, Blocchi, Waypoints) |
| **Shift + 6** | Categoria Successiva | Passa al gruppo POI successivo |
| **Shift + 5** | Guarda Bersaglio POI | Ruota la telecamera direttamente verso l'oggetto selezionato (`Look at current object`) |
| **Shift + 0** | Bersaglio più vicino (Qualsiasi) | Punta istantaneamente all'oggetto o mob più vicino (`Target nearest any`) |
| **Shift + 1** | Entità più vicina | Punta all'entità più vicina (`Target nearest entity`) |
| **Shift + 3** | Blocco più vicino | Punta al blocco più vicino (`Target nearest block`) |
| **Shift + Enter** | Lock-on (Blocca visuale) | Aggancia la telecamera al bersaglio selezionato (`Y` - Lock on target) |
| **Shift + .** | Contrassegna (Mark Target) | Imposta un marker sul blocco/entità nel mirino |
| **Shift + -** | Rimuovi Contrassegno | Rimuove il marker attivo |
| **Shift + /** | Waypoint Precedente | Cicla direttamente tra i waypoint salvati |
| **Shift + \*** | Waypoint Successivo | Cicla direttamente tra i waypoint salvati |

---

### 🔹 Livello 2: `Ctrl` + Numpad (Orientamento Assoluto & Punti Cardinali)

| Combinazione | Funzione Proposta | Descrizione |
| :--- | :--- | :--- |
| **Ctrl + 8** | Gira a NORD | Snap istantaneo della telecamera esattamente a Nord |
| **Ctrl + 6** | Gira a EST | Snap istantaneo della telecamera esattamente a Est |
| **Ctrl + 2** | Gira a SUD | Snap istantaneo della telecamera esattamente a Sud |
| **Ctrl + 4** | Gira a OVEST | Snap istantaneo della telecamera esattamente a Ovest |
| **Ctrl + 7** | Gira a Nord-Ovest | Allineamento diagonale |
| **Ctrl + 9** | Gira a Nord-Est | Allineamento diagonale |
| **Ctrl + 1** | Gira a Sud-Ovest | Allineamento diagonale |
| **Ctrl + 3** | Gira a Sud-Est | Allineamento diagonale |
| **Ctrl + 5** | Guarda Indietro (180°) | Gira istantaneamente lo sguardo alle proprie spalle |
| **Ctrl + 0** | Coordinate X, Y, Z | Lettura vocale delle coordinate spaziali correnti del giocatore |
| **Ctrl + .** | Coordinate Bersaglio | Lettura coordinate del blocco/entità puntato |
| **Ctrl + Enter** | Tasto Centrale Mouse | Simula il tasto centrale (Pick block) |

---

### 🔹 Livello 3: `Alt` + Numpad (Stato Giocatore, Ambiente & HUD)

| Combinazione | Funzione Proposta | Descrizione |
| :--- | :--- | :--- |
| **Alt + 5** | Stato Giocatore Completo | Annuncia Salute, Fame, Armatura, Aria, Freddo |
| **Alt + 8** | Oggetto Mano Principale | Annuncia nome, quantità e dettagli dell'oggetto impugnato a destra |
| **Alt + 2** | Oggetto Mano Secondaria | Annuncia cosa c'è nella mano sinistra (scudo, torce, ecc.) |
| **Alt + 4** | Effetti e Pozioni | Annuncia gli effetti attivi e la durata residua |
| **Alt + 6** | Durabilità | Annuncia la durabilità residua dell'arma/attrezzo in mano |
| **Alt + 7** | Bioma & Meteo | Annuncia bioma attuale e se piove/nevica/sereno |
| **Alt + 9** | Luce & Ora | Annuncia il livello di luce del blocco e l'ora del giorno |
| **Alt + Enter** | Access Menu (F4) | Apre/Chiude l'Access Menu rapido |
| **Alt + +** | Bossbar Successiva | Legge la bossbar successiva |
| **Alt + -** | Bossbar Precedente | Legge la bossbar precedente |

---

## 4. Personalizzazione, Rimappatura & Preset

### 4.1 Categoria Dedicata nel Menu Assegnazione Tasti
Tutti i comandi del Numpad sono registrati sotto una categoria Kuma dedicata nel menu standard di Minecraft (`Opzioni...` -> `Controlli...` -> `Assegnazione tasti...`):
> **`Minecraft Access: Numpad Controls`**

Caratteristiche:
* **Rimappabilità totale**: Ogni singolo comando (sia liscio che con `Shift`, `Ctrl`, `Alt`) può essere riassegnato a piacere dall'utente.
* **Pulsante "Ripristina" nativo**: Consente di ripristinare in qualsiasi momento la configurazione predefinita consigliata.
* **Piena accessibilità vocale**: Il menu dei tasti è completamente leggibile dalla sintesi vocale del mod.

### 4.2 Preset Destrorso / Mancino in Configurazione
Il modulo offre nel menu di configurazione Cloth Config la possibilità di selezionare il preset di base:
1. **Destrorso (Standard)**: Layout predefinito con `Numpad +` (attacco) ed `Enter` (uso) a destra.
2. **Mancino (Invertito)**: Layout ottimizzato per chi usa la mano sinistra sul tastierino, invertendo i comandi di azione o i laterali per un'ergonomia naturale.

---

## 5. Variabili e Opzioni di Configurazione (`Config.java`)

Nel menu `Opzioni...` -> `Minecraft Access`, la sezione **"Numpad Controls"** include:

* **Interruttore Generale (Master Switch)**:
  - `enabled` (booleano, default: `true`): Abilita o disabilita interamente il modulo Numpad.
* **Preset Ergonomico**:
  - `preset` (enum: `RIGHT_HANDED`, `LEFT_HANDED`): Selezione rapida tra configurazione standard o mancina.
* **Sguardo e Telecamera**:
  - `normalRotatingAngle` (decimale, default: `15.0°`, range: `1.0°` - `90.0°`): Angolo di rotazione per tocco singolo.
  - `modifiedRotatingAngle` (decimale, default: `45.0°`, range: `5.0°` - `180.0°`): Angolo di rotazione rapida.
  - `continuousRotation` (booleano, default: `true`): Rotazione continua tenendo premuto il tasto.
  - `continuousRotationSpeed` (decimale, default: `1.0x`, range: `0.5x` - `5.0x`): Velocità di rotazione continua.
  - `invertYAxis` (booleano, default: `false`): Inversione asse verticale.
  - `narrateFacingOnChange` (booleano, default: `true`): Annuncio vocale della direzione dopo la rotazione.
* **Simulazione Mouse ed Azioni**:
  - `enableContinuousHold` (booleano, default: `true`): Pressione continua per scavo (`+`) e piazzamento/uso (`Enter`).
  - `scrollDelayMilliseconds` (intero, default: `150ms`, range: `50ms` - `500ms`): Ritardo cambio slot hotbar.
* **Radar e Tracciamento POI**:
  - `narrateDistanceOnSelect` (booleano, default: `true`): Annuncio vocale distanza dal bersaglio.
  - `autoLookOnLock` (booleano, default: `true`): Allineamento automatico dello sguardo al lock-on.
* **Feedback Sonoro**:
  - `playCardinalSnapSound` (booleano, default: `true`): Audio cue distintivi per gli snap cardinali e snap orizzonte.
  - `audioCueVolume` (decimale, default: `1.0`, range: `0.0` - `1.0`): Volume dei suoni del modulo.

---

## 6. Roadmap di Lavoro

- [x] **Fase 1: Definizione Struttura, Categoria e Configurazione Base**
  - [x] Creazione costante `NUMPAD_CONTROLS` in `KeyMappingCategories.java`
  - [x] Aggiunta classe interna `@ConfigEntry.Gui.CollapsibleObject NumpadControls` in `Config.java` con Master Switch, preset destrorso/mancino e tutti i parametri
  - [x] Registrazione modulo in `MainClass.java`
  - [x] Aggiunta stringhe di localizzazione in `en_us.json` e `it_it.json`

- [x] **Fase 2: Implementazione Livello 0 (Telecamera & Mouse)**
  - [x] Mappatura tasti Numpad 8, 2, 4, 6, 7, 9, 1, 3, 5, 0, . per controllo sguardo
  - [x] Mappatura Numpad +, Enter, -, /, * per simulazione mouse (sinistro, destro, sblocco, scroll hotbar)
  - [x] Gestione `ClientTickCallback` per la tenuta continua (hold per scavare/attaccare con `+` ed `Enter`)
  - [x] Test di reattività, pressione singola e tenuta

- [x] **Fase 3: Implementazione Livello 2 (Orientamento Assoluto con Ctrl)**
  - [x] Mappatura Ctrl + Numpad 8, 6, 2, 4, 7, 9, 1, 3 (Punti cardinali e diagonali)
  - [x] Mappatura Ctrl + Numpad 5 (Guarda indietro 180°)
  - [x] Mappatura Ctrl + Numpad 0, . (Lettura coordinate X, Y, Z e bersaglio)
  - [x] Mappatura Ctrl + Numpad Enter (Tasto centrale / Pick block)
  - [x] Aggiunta audio feedback distinti per snap cardinali

- [x] **Fase 4: Implementazione Livello 1 (Scansione, POI & Lock con Shift)**
  - [x] Integrazione con `ObjectTracker`: Shift + Numpad 8/2 (elementi) e 4/6 (gruppi)
  - [x] Mappatura Shift + Numpad 5 (guarda bersaglio POI)
  - [x] Mappatura Shift + Numpad 0, 1, 3 (puntamento bersaglio più vicino: all, mob, blocco)
  - [x] Mappatura Shift + Numpad Enter e - (Lock-on e sblocco)
  - [x] Mappatura Shift + Numpad /, * (Navigazione rapida Waypoints)

- [x] **Fase 5: Implementazione Livello 3 (Stato, HUD & Ambiente con Alt)**
  - [x] Mappatura Alt + Numpad 5 (Stato giocatore completo)
  - [x] Mappatura Alt + Numpad 8, 2 (Mano principale e secondaria)
  - [x] Mappatura Alt + Numpad 4, 6 (Effetti attivi e durabilità)
  - [x] Mappatura Alt + Numpad 7, 9 (Bioma/Meteo e Luce/Ora)
  - [x] Mappatura Alt + Numpad Enter (Access Menu) e + / - (Bossbars)

- [x] **Fase 6: Rifinitura, Collaudo & Documentazione**
  - [x] Verifica del menu "Assegnazione tasti" e rimappabilità completa sotto `Minecraft Access: Numpad Controls`
  - [x] Test di commutazione preset Destrorso / Mancino
  - [x] Aggiornamento documentazione e localizzazione completa (Italiano / Inglese)
  - [x] Compilazione ed assemblaggio JAR completati con successo con Gradle
