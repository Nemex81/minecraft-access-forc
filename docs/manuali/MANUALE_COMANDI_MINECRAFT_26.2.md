# Manuale dei Comandi di Minecraft Java 26.2
## Prontuario Operativo, Tecnico e di Accessibilità per Minecraft Access 1.12.0 (Fork Locale)

Questo documento costituisce il manuale di riferimento e la guida pratica per il controllo del mondo di Minecraft, redatto specificamente per un giocatore non vedente e programmatore. Ogni comando, formula geometrica e combinazione di tasti è stato verificato e validato direttamente sul codice sorgente di **Minecraft Access 1.12.0**, sui registri e sui bytecode di **Minecraft Java Edition 26.2**.

---

## 🧭 Struttura del Sistema e Livelli di Gioco

Il sistema di controllo si articola su tre livelli distinti e indipendenti:

1. **Livello A: Comandi Vanilla di Minecraft Java 26.2**  
   Comandi nativi eseguiti dalla chat di gioco (`/`), disponibili in qualsiasi installazione di Minecraft Java con trucchi abilitati.
2. **Livello B: Controlli e Feedback di Minecraft Access 1.12.0**  
   Funzionalità di accessibilità della mod per la lettura vocale dell'ambiente, navigazione a griglia degli inventari e vocalizzazione delle coordinate.
3. **Livello C: Mod Aggiuntive di Gameplay (FallingTree)**  
   Modifiche meccaniche specifiche (es. abbattimento rapido degli alberi).

---

## ⚠️ Cosa NON fa Minecraft Access

- **Minecraft Access non altera la sintassi dei comandi vanilla**: I comandi `/tp`, `/fill`, `/tick`, `/time`, `/give`, `/locate` e `/gamerule` appartengono esclusivamente a Minecraft Java e seguono le regole sintattiche del motore di gioco.
- **Minecraft Access agisce come interfaccia di accessibilità**: Fornisce sintesi vocale (tramite Tolk/NVDA), segnali audio 3D (crosshair sonoro, tracker), navigazione assistita tramite tastiera e comandi dedicati per leggere lo stato del giocatore e delle GUI.

---

# PARTE 1: COMANDI VANILLA DI MINECRAFT JAVA 26.2 `[LIVELIO A]`

---

## 1. Regole Fondamentali della Sintassi e Chat

### Inserimento dei Comandi
- **Apertura Chat**: Premere **`T`** per aprire la casella di testo. NVDA vocalizza i caratteri digitati.
- **Autocompletamento con `Tab`**: Premendo `Tab`, Minecraft suggerisce e completa automaticamente comandi, selettori, identificatori di blocchi e coordinate del blocco mirato.
- **Cronologia Frecce**: Frecce `Su` e `Giù` permettono di richiamare i comandi inviati in precedenza.
- **Prefisso `/`**: Ogni comando deve obbligatoriamente iniziare con la barra `/`.

### Selettori di Destinazione (Target Selectors) `[VERIFICATO DA REGISTRI]`
I selettori stabiliscono il bersaglio del comando:
- **`@s` (Self / Sé stesso)**: Il giocatore che esegue il comando (è il selettore più sicuro ed esatto).
- **`@p` (Nearest Player)**: Il giocatore più vicino alla posizione di esecuzione.
- **`@a` (All Players)**: Tutti i giocatori presenti nel server o mondo.
- **`@e` (All Entities)**: Tutte le entità caricate nel mondo (mostri, animali, oggetti a terra).
  - *Filtri tra quadre*: Permettono di restringere il target, ad esempio `@e[type=minecraft:zombie]` seleziona solo gli zombie, mentre `@e[type=minecraft:item,distance=..10]` seleziona solo gli oggetti a terra entro 10 blocchi.

---

## 2. Coordinate Spaziali e Sistemi di Riferimento

Minecraft organizza lo spazio tridimensionale tramite una terna cartesiana `(X, Y, Z)`:
- **Asse X (Est / Ovest)**: Valori crescenti verso Est (+X), decrescenti verso Ovest (-X).
- **Asse Y (Quota / Verticalità)**: Da `-64` (fondo del mondo) a `320` (limite del cielo). Il livello del mare è tipicamente `Y = 62-64`.
- **Asse Z (Sud / Nord)**: Valori crescenti verso Sud (+Z), decrescenti verso Nord (-Z).

### I Tre Sistemi di Coordinate `[VERIFICATO DA CODICE]`

1. **Coordinate Assolute (`X Y Z`)**:
   - Punti fissi e immutabili della mappa globale del mondo.
   - Esempio: `150 64 -300` indica in modo univoco il blocco a X=150, Y=64, Z=-300.
2. **Coordinate Relative all'Origine di Esecuzione (`~X ~Y ~Z`)**:
   - Definite tramite il simbolo tilde `~`, indicano uno scostamento relativo alla **posizione di origine del contesto o dell'entità che esegue il comando**.
   - Nel caso di un comando eseguito dal giocatore, l'origine spaziale corrisponde alle coordinate `[X, Y, Z]` del giocatore, dove la quota `Y` di riferimento è quella al livello di appoggio del piano calpestabile.
   - `~ ~ ~` = la coordinata di origine dell'esecutore.
   - `~ ~20 ~` = coordinate X e Z invariate, quota Y aumentata di 20 blocchi verso l'alto.
   - `~-5 ~ ~10` = 5 blocchi a Ovest (-X), quota invariata, 10 blocchi a Sud (+Z).
3. **Coordinate Locali della Visuale (`^sinistra ^alto ^avanti`)**:
   - Definite tramite il simbolo caret `^`, indicano uno scostamento calcolato in base alla **rotazione e all'inclinazione della testa del giocatore**.
   - `^ ^ ^5` = 5 blocchi esattamente di fronte allo sguardo.
   - `^ ^2 ^` = 2 blocchi sopra la visuale nell'angolo di orientamento corrente.

---

## 3. Controllo del Tempo e della Simulazione: `/time` vs `/tick`

In Minecraft Java 26.2 esistono due motori temporali distinti, ciascuno dotato di comandi specifici:

```text
+----------------------+---------------------------------------------------+----------------------------------------------------+
| Proprietà            | /time (WorldClock)                                | /tick (Simulation Engine)                          |
+----------------------+---------------------------------------------------+----------------------------------------------------+
| Sistema Controllato  | Orologio celeste e timeline del mondo             | Motore di simulazione del server (target 20 Hz)    |
| Sintassi Principale  | /time rate <valore> (o /time of <clock> rate ...) | /tick rate <valore>                                |
| Range Parametri      | Float da 0.00001 (1.0E-5) a 1000.0                | Float da 0.1 a 10000.0                             |
| Valore Standard      | 1.0 (1x)                                          | 20.0 (20 tick al secondo)                          |
| Cosa Modifica        | SOLO la velocità di transizione giorno/notte      | Frequenza del ciclo di simulazione del mondo       |
| Movimento Mob/Entità | Velocità normale                                  | Elaborazione AI, fisica e movimenti a target tick  |
| Movimento Giocatore  | Velocità normale                                  | Polling input e rotazione visuale fluidi a 60fps   |
| Scorrimento Fame     | Velocità normale                                  | Calcolato sui tick di simulazione                  |
| Circuiti Redstone    | Velocità normale                                  | Aggiornati alla frequenza di tick impostata        |
| Comandi di Blocco    | /time pause | /time resume                        | /tick freeze | /tick unfreeze                      |
+----------------------+---------------------------------------------------+----------------------------------------------------+
```

### Comandi `/time` `[VERIFICATO DA BYTECODE/REGISTRI]`
- `/time rate <valore>`: Modifica la velocità dell'orologio celeste. Impostando `/time rate 0.1`, la transizione giorno/notte dura 10 volte di più (circa 200 minuti reali invece di 20), lasciando la simulazione e la fisica del mondo a piena velocità (20 tick/s).
- `/time pause`: Ferma l'avanzamento dell'orologio celeste (il sole/luna restano fermi).
- `/time resume`: Riavvia l'avanzamento dell'orologio celeste.
- `/time set day` / `/time set 1000`: Imposta l'ora sul mattino (ore 07:00).
- `/time set noon` / `/time set 6000`: Imposta l'ora a mezzogiorno.
- `/time set night` / `/time set 13000`: Imposta l'ora sulla notte (ore 19:00).
- `/time set midnight` / `/time set 18000`: Imposta l'ora a mezzanotte.

### Comandi `/tick` `[VERIFICATO DA BYTECODE/REGISTRI]`
- `/tick rate <valore>`: Modifica la frequenza target del ciclo di simulazione (in tick al secondo).
  - `/tick rate 2`: Esegue la simulazione a 2 tick al secondo (10% della velocità standard di 20 tick/s), rallentando la frequenza di aggiornamento della fisica, dell'IA delle entità e degli eventi ambientali.
  - `/tick rate 20`: Ripristina il valore di simulazione predefinito di 20 tick/s.
- `/tick freeze`: Blocca l'esecuzione dei tick di simulazione senza aprire menu GUI.
- `/tick unfreeze`: Riprende la normale esecuzione del ciclo di simulazione.
- `/tick step <tempo>`: Fa avanzare la simulazione di un intervallo di tempo prestabilito durante il freeze (es. `/tick step 5s` o `/tick step 20t`).

---

## 4. Teletrasporto e Movimento (`/tp`)

```text
/tp @s <coordinate>
```

### Comandi di Spostamento e Soccorso `[VERIFICATO DA CODICE]`
1. **Spostamento Verticale Relativo**:
   ```text
   /tp @s ~ ~20 ~
   ```
   - *Effetto*: Sposta il giocatore di 20 blocchi verso l'alto rispetto alla posizione corrente.
   - *Uso di emergenza*: Uscire da burroni, cavità o buche profonde.
2. **Spostamento Orizzontale Relativo**:
   ```text
   /tp @s ~10 ~ ~-30
   ```
   - *Effetto*: Sposta il giocatore di 10 blocchi a Est (+X) e 30 blocchi a Nord (-Z).
3. **Teletrasporto a Coordinate Assolute**:
   ```text
   /tp @s 150 72 -320
   ```
   - *Effetto*: Posiziona il giocatore esattamente alle coordinate globali specificate.

---

## 5. Esplorazione e Individuazione Strutture (`/locate`)

In Minecraft 26.2 le strutture vengono individuate tramite identificatori registrati o tag di categoria preceduti dal cancelletto `#`:

### Ricerca Strutture `[VERIFICATO DA REGISTRI]`
- **Qualsiasi villaggio più vicino**:
  ```text
  /locate structure #minecraft:village
  ```
- **Villaggio di pianura specifico**:
  ```text
  /locate structure minecraft:village_plains
  ```
- **Miniera abbandonata (Mineshaft)**:
  ```text
  /locate structure #minecraft:mineshaft
  ```
- **Antica città sotterranea (Ancient City)**:
  ```text
  /locate structure minecraft:ancient_city
  ```

### Procedura di Teletrasporto Sicuro dopo `/locate`
Quando `/locate` restituisce le coordinate (es. `[X: 240, Z: -380]`), la quota `Y` non è nota a priori:
1. Non teletrasportarsi ciecamente a `Y = ~` o a una quota arbitraria bassa, per evitare di generarsi all'interno di pareti solide o sottoterra.
2. **Procedura prudente consigliata**:
   - Applicare l'effetto di caduta lenta: `/effect give @s minecraft:slow_falling 60 0 true`
   - Teletrasportarsi a una quota di sicurezza elevata: `/tp @s 240 120 -380`
   - Il giocatore atterrerà dolcemente sul terreno della struttura senza subire danni.
   - *In alternativa*: Utilizzare le coordinate X e Z come riferimento per camminare, verificando la direzione con `Alt + X` e `Alt + Z` di Minecraft Access.

---

## 6. Risorse e Inventario (`/give`)

```text
/give @s minecraft:<oggetto> [quantità]
```

### Materiali Base Essenziali `[VERIFICATO DA REGISTRI]`
- Assi di quercia (x64): `/give @s minecraft:oak_planks 64`
- Tronchi di quercia (x32): `/give @s minecraft:oak_log 32`
- Torce (x64): `/give @s minecraft:torch 64`
- Pietrisco (x64): `/give @s minecraft:cobblestone 64`
- Lingotti di ferro (x16): `/give @s minecraft:iron_ingot 16`
- Carbone (x32): `/give @s minecraft:coal 32`
- Cibo pronto (x32): `/give @s minecraft:bread 32`
- Strumenti in ferro: `/give @s minecraft:iron_pickaxe 1`, `/give @s minecraft:iron_axe 1`, `/give @s minecraft:iron_sword 1`

- *Uso didattico*: Utile per testare combinazioni di crafting e studiare il comportamento dei materiali prima di produrli in modalità Survival pura.

---

## 7. Costruzione Rapida Geometrica (`/fill`, `/setblock`, `/clone`)

### A. Il Comando `/fill` `[VERIFICATO DA FORMULA MATEMATICA E REGISTRI]`
Riempie il volume tridimensionale compreso tra due punti estremi opposti `(X1 Y1 Z1)` e `(X2 Y2 Z2)`:
```text
/fill <X1 Y1 Z1> <X2 Y2 Z2> minecraft:<blocco> [modalità]
```

#### Modalità di `/fill`
- **`replace` (senza filtro)**: Sostituisce incondizionatamente qualsiasi blocco all'interno del volume con il nuovo blocco specificato (è il comportamento predefinito di `/fill`).
- **`replace <blocco_filtro>`**: Sostituisce all'interno del volume **esclusivamente** i blocchi che corrispondono al blocco o al tag di filtro specificato, lasciando inalterati tutti gli altri blocchi presenti.
- **`hollow`**: Genera il guscio esterno con il blocco scelto e riempie l'intero volume interno con `minecraft:air`.
- **`outline`**: Sostituisce solo il guscio esterno, lasciando inalterato ciò che si trova già all'interno.
- **`keep`**: Piazza il blocco solo negli spazi attualmente occupati dall'aria (non sovrascrive blocchi esistenti).
- **`destroy`**: Riempie il volume distruggendo i blocchi precedenti e droppandone gli oggetti a terra come se fossero stati scavati.

---

### Formule Geometriche Verificate con Coordinate Relative `~`

Tutte le formule partono dalla quota di riferimento dell'origine del giocatore `(~)`:

#### 1. Casa / Stanza Abitabile Sicura (11 × 5 × 11 blocchi esterni)
```text
/fill ~-5 ~-1 ~-5 ~5 ~3 ~5 minecraft:oak_planks hollow
```
- **Verifica Geometrica**:
  - Pavimento solido: quota `Y = ~-1` (sotto i piedi).
  - Spazio d'aria interno: quota `Y = ~`, `~+1`, `~+2` (3 blocchi d'aria libera; altezza interna 3 blocchi).
  - Soffitto solido: quota `Y = ~+3` (sopra la testa).
  - Dimensioni interne calpestabili: 9 × 3 × 9 blocchi.
  - **Sicurezza**: Il giocatore si trova al centro della stanza, in piedi sul pavimento, senza compenetrare alcun blocco solido.

#### 2. Pavimento Pulito (11 × 11 blocchi)
```text
/fill ~-5 ~-1 ~-5 ~5 ~-1 ~5 minecraft:oak_planks
```
- Piazza una superficie solida di spessore 1 blocco sotto i piedi.

#### 3. Parete Protettiva a Nord (11 blocchi di larghezza, 3 di altezza)
```text
/fill ~-5 ~ ~-5 ~5 ~2 ~-5 minecraft:stone_bricks
```
- Piazza una parete solida 5 blocchi a Nord (-Z) dal giocatore.

#### 4. Soffitto / Copertura Protettiva (11 × 11 blocchi)
```text
/fill ~-5 ~3 ~-5 ~5 ~3 ~5 minecraft:oak_planks
```
- Piazza una copertura orizzontale a quota `Y = ~+3` (sopra la testa).

#### 5. Galleria / Tunnel Orizzontale a Sud (+Z)
```text
/fill ~ ~ ~1 ~ ~1 ~15 minecraft:air
```
- Svuota un corridoio d'aria largo 1, alto 2 (`~` e `~+1`) e profondo 15 blocchi dritto davanti a te.

#### 6. Chiusura e Riempimento di una Buca sotto i Piedi
```text
/fill ~-1 ~-10 ~-1 ~1 ~-1 ~1 minecraft:dirt
```
- Riempie di terra solida una cavità 3×10×3 sottostante il giocatore fino al livello del pavimento (`~-1`).

#### 7. Livellamento del Terreno
```text
/fill ~-10 ~-1 ~-10 ~10 ~-1 ~10 minecraft:stone replace minecraft:grass_block
```
- Sostituisce solo i blocchi di erba superficiale con pietra liscia in un raggio di 10 blocchi.

---

### B. Il Comando `/setblock` `[VERIFICATO DA CODICE]`
```text
/setblock <X Y Z> minecraft:<blocco> [destroy|keep|replace|strict]
```
- **Blocco solido sotto i piedi**: `/setblock ~ ~-1 ~ minecraft:oak_planks`  
  *(Piazza un blocco di supporto al livello del pavimento)*
- **Piazzare una torcia a terra**: `/setblock ~ ~ ~ minecraft:torch`  
  *(Piazza una torcia nello spazio d'aria al livello di riferimento `~`, poggiandola sul blocco solido sottostante a `~-1`)*
- **Liberarsi dal soffocamento**: `/setblock ~ ~1 ~ minecraft:air`  
  *(Svuota il blocco all'altezza del busto/testa se si rimane bloccati da sabbia o ghiaia)*

---

### C. Il Comando `/clone` `[VERIFICATO DA CODICE]`
```text
/clone <X1 Y1 Z1> <X2 Y2 Z2> <X_dest Y_dest Z_dest> [filtered|masked|replace] [force|move|normal]
```
- Permette di copiare una stanza o struttura modulare precedentemente collaudata e duplicarla in un'altra area del mondo.

---

## 8. Modalità di Gioco (`/gamemode`)

```text
/gamemode <survival|creative|adventure|spectator>
```

- **`survival`**: Modalità principale di gioco. Gestione salute, fame, raccolta risorse e combattimento.
- **`creative`**: Risorse illimitate, volo (doppio spazio), invulnerabilità totale. Ideale per progettare strutture geometriche.
- **`adventure`**: Impedisce totalmente di rompere o piazzare blocchi nel mondo (a meno di strumenti dotati di componenti specifici come `can_break`).  
  *Utilità per il giocatore non vedente*: Permette di esplorare percorsi, villaggi o la propria casa senza il rischio di distruggere inavvertitamente blocchi o coltivazioni premendo i tasti di attacco per sbaglio.
- **`spectator`**: Volo libero e passaggio attraverso i blocchi senza collisioni fisiche.

---

## 9. Gamerule per Accessibilità, Sicurezza e Apprendimento `[VERIFICATO DA REGISTRI]`

In Minecraft 26.2 tutte le regole del mondo usano la nomenclatura snake_case:

```text
/gamerule <regola> <true|false>
```

### Livello 1: Sicurezza Base e Prevenzione Perdite (Consigliato per iniziare)
- `/gamerule keep_inventory true`: Mantiene oggetti ed exp nell'inventario dopo la morte.
- `/gamerule mob_griefing false`: I Creeper infliggono danno da esplosione ma **non distruggono blocchi o pareti**; gli Endermen non spostano blocchi.

### Livello 2: Controllo Ambientale
- `/gamerule advance_time false`: Ferma il ciclo giorno/notte (sole fisso). *(Sostituisce doDaylightCycle)*
- `/gamerule advance_weather false`: Ferma le variazioni meteorologiche (pioggia/temporale). *(Sostituisce doWeatherCycle)*
- `/gamerule fall_damage false`: Disattiva i danni derivanti dalle cadute da altezze o burroni.

### Livello 3: Protezione da Minacce Fisiche
- `/gamerule fire_damage false`: Disattiva i danni da fuoco e lava.
- `/gamerule drowning_damage false`: Disattiva i danni da annegamento sott'acqua.
- `/gamerule freeze_damage false`: Disattiva i danni da congelamento nella neve polverosa.
- `/gamerule spawn_monsters false`: Disattiva la comparsa di mostri ostili (mantenendo gli animali amichevoli).

---

## 10. Effetti di Stato (`/effect`) `[VERIFICATO DA CODICE]`

```text
/effect give @s minecraft:<effetto> [infinite|<secondi>] [amplificatore] [nascondi_particelle]
```

### Effetti di Supporto Utili
- **Saturazione Infinita (Elimina la gestione della fame)**:
  ```text
  /effect give @s minecraft:saturation infinite 0 true
  ```
- **Rigenerazione Continua della Salute**:
  ```text
  /effect give @s minecraft:regeneration infinite 1 true
  ```
- **Resistenza ai Danni**:
  ```text
  /effect give @s minecraft:resistance infinite 1 true
  ```
- **Respirazione Subacquea**:
  ```text
  /effect give @s minecraft:water_breathing infinite 0 true
  ```
- **Caduta Lenta (Floating dolce senza impatto)**:
  ```text
  /effect give @s minecraft:slow_falling infinite 0 true
  ```
- **Rimuovere tutti gli effetti attivi**:
  ```text
  /effect clear @s
  ```

---

## 11. Punto di Rinascita (`/spawnpoint` vs `/setworldspawn`) `[VERIFICATO DA CODICE]`

- **Punto di Respawn Personale**:
  ```text
  /spawnpoint @s
  ```
  - Fissa il punto di rinascita personale del giocatore esattamente alle sue coordinate attuali.
- **Punto di Spawn Globale del Mondo**:
  ```text
  /setworldspawn
  ```
  - Fissa il punto di spawn iniziale per l'intero mondo di gioco.

---

# PARTE 2: CONTROLLI E FEEDBACK DI MINECRAFT ACCESS 1.12.0 `[LIVELLO B]`

---

## 12. Mappa Completa dei Keybind di Accessibilità `[VERIFICATO DA CODICE]`

Tutti i comandi seguenti sono configurabili dal menu Controlli di Minecraft tramite Kuma:

| Funzione | Tasto di Default | Categoria Kuma | Contesto e Condizioni | Note Upstream vs Fork |
| :--- | :--- | :--- | :--- | :--- |
| **Vocalizza Posizione Completa (X, Y, Z)** | **`V`** | `PLAYER_POSITION` | Mondo aperto | Upstream 1.12.0 |
| **Vocalizza Solo Coordinata X** | **`Alt + X`** | `PLAYER_POSITION` | Mondo aperto | Upstream 1.12.0 |
| **Vocalizza Solo Quota Y** | **`Alt + C`** | `PLAYER_POSITION` | Mondo aperto | Upstream 1.12.0 *(posizionato su C per continuità fisica)* |
| **Vocalizza Solo Coordinata Z** | **`Alt + Z`** | `PLAYER_POSITION` | Mondo aperto | Upstream 1.12.0 |
| **Stato Giocatore (Cuori/Fame/Aria)** | **`H`** | `PLAYER_STATUS` | Mondo aperto | Upstream 1.12.0 |
| **Stato Giocatore Critico** | **`Shift + H`** | `PLAYER_STATUS` | Mondo aperto | Upstream 1.12.0 |
| **Vocalizza Effetti Attivi** | **`Ctrl + H`** | `PLAYER_STATUS` | Mondo aperto | Upstream 1.12.0 |
| **Navigazione Griglia Slot** | **`I, K, J, L`** | `INVENTORY_CONTROLS` | All'interno di qualsiasi GUI | Upstream 1.12.0 (`I`=Su, `K`=Giù, `J`=Sinistra, `L`=Destra) |
| **Cambio Gruppo Slot** | **`C`** / **`Shift + C`** | `INVENTORY_CONTROLS` | All'interno di qualsiasi GUI | Upstream 1.12.0 (`C`=Avanti, `Shift+C`=Indietro) |
| **Stato Carburante e Cottura** | **`U`** | `INVENTORY_CONTROLS` | GUI Fornace / Alambicco | Upstream 1.12.0 (vocalizza % cottura e carburante) |
| **Lettura Requisiti Ricetta (Recipe Info)** | **`X`** | `INVENTORY_CONTROLS` | **GUI con Ricettario e gruppo `Ricette` attivo** | **Funzionalità Specifica del Nostro Fork Locale** |
| **Schermata Avanzamenti (Advancements)** | **`X`** | Vanilla Controls | **Nel mondo aperto fuori dalle GUI** | Vanilla / Upstream 1.12.0 |
| **Salta alla Casella di Ricerca** | **`T`** | `INVENTORY_CONTROLS` | All'interno di GUI con ricerca | Upstream 1.12.0 |
| **Filtro Ricette Realizzabili/Tutte** | **`R`** | `INVENTORY_CONTROLS` | GUI con Ricettario | Upstream 1.12.0 |
| **Sonda Terreno / Ispezione Burroni** | **`Alt + F`** | `OTHER` | Mondo aperto | **Funzionalità Specifica del Nostro Fork Locale** (vocalizza burrone vicino o "Nessuna buca") |
| **Ripeti Ultimo Parlato** | **`Alt + G`** | `OTHER` | Globale / Ovunque | **Funzionalità Specifica del Nostro Fork Locale** (riascolto vocale immediato con mano sinistra) |
| **Ispeziona Ostacolo Frontale (Look-At)** | **`Alt + V`** | `OTHER` | Mondo aperto | **Funzionalità Specifica del Nostro Fork Locale** (vocalizza e mira all'ostacolo/dislivello più vicino) |
| **Vocalizza POI Tracciato Attuale** | **`Home`** | `OBJECT_TRACKER` | Mondo aperto | Upstream 1.12.0 (vocalizza nome, distanza e direzione del POI) |
| **Orienta Sguardo su POI Tracciato (Look-At)** | **`Ctrl + Home`** | `OBJECT_TRACKER` | Mondo aperto | **Funzionalità Specifica del Nostro Fork Locale** (mira al POI senza lock continuo e senza sneak) |
| **Vocalizza Coordinate del POI** | **`Alt + Home`** | `OBJECT_TRACKER` | Mondo aperto | Upstream 1.12.0 (vocalizza coordinate XYZ del POI) |
| **Punta Bersaglio su Qualsiasi POI Vicino** | **`End`** | `OBJECT_TRACKER` | Mondo aperto | Upstream 1.12.0 |
| **Punta Bersaglio su Blocco Vicino** | **`Shift + End`** | `OBJECT_TRACKER` | Mondo aperto | Upstream 1.12.0 |
| **Punta Bersaglio su Entità Vicina** | **`Ctrl + End`** | `OBJECT_TRACKER` | Mondo aperto | Upstream 1.12.0 |

> [!IMPORTANT]
> **Comportamento Contestuale del Tasto `X`**:
> Nel nostro fork locale, `X` è stato potenziato con il metodo `.ignoreScreenFocus()` in Kuma:
> - **Quando sei all'interno dell'Inventario, Banco da Lavoro o Fornace sul gruppo `Ricette`**: Premendo **`X`**, la sintesi vocale legge istantaneamente il nome del prodotto e la lista degli ingredienti necessari con relative quantità (es. *"4 Torcia: richiede 1 Carbone, 1 Bastone"*).
> - **Quando sei nel mondo aperto fuori dalle GUI**: Premendo **`X`**, si apre la schermata degli Avanzamenti vanilla.

---

# PARTE 3: MOD AGGIUNTIVE DI GAMEPLAY `[LIVELLO C]`

---

## 13. Mod FallingTree `[FALLINGTREE]`

- **Funzionamento**: Rompendo il blocco basale di un tronco con un'ascia, l'intero albero cade istantaneamente trasformandosi in blocchi di legna raccolti automaticamente.
- **Vantaggio per il Survival**: Consente di raccogliere legna rapidamente nel flusso naturale del gioco Survival senza dover ricorrere al comando `/give`.

---

# PARTE 4: TABELLA RAPIDA DI RIFERIMENTO E PRONTO SOCCORSO

---

## 14. Pronto Soccorso per Situazioni Critiche

| Situazione Critica | Soluzione Operativa | Spiegazione Tecnica |
| :--- | :--- | :--- |
| **Sei caduto in un burrone profondo** | `/tp @s ~ ~20 ~` | Sposta il giocatore di 20 blocchi verso l'alto rispetto alla posizione corrente. |
| **Senti mostri e vuoi analizzare la zona con calma** | `/tick freeze` | Congela all'istante tutte le entità per consentire l'orientamento audio. |
| **Vuoi riprendere la simulazione dopo il freeze** | `/tick unfreeze` | Riavvia il motore di simulazione. |
| **Costruire un rifugio chiuso istantaneo** | `/fill ~-5 ~-1 ~-5 ~5 ~3 ~5 minecraft:oak_planks hollow` | Crea una stanza 11×5×11 con pavimento a `~-1` e interno vuoto calpestabile. |
| **Piazzare una torcia ai piedi** | `/setblock ~ ~ ~ minecraft:torch` | Posiziona una torcia sul pavimento alla quota corrente. |
| **Fissare la tua casa come punto di rinascita** | `/spawnpoint @s` | Imposta il punto di respawn personale alle coordinate correnti. |
| **Eliminare la fame durante l'apprendimento** | `/effect give @s minecraft:saturation infinite 0 true` | Mantiene la barra della fame piena permanentemente. |

---

## 15. Cheat Sheet dei Comandi Più Usati

```text
==================================================================================================
COMANDO                                             | EFFETTO OPERATIVO
==================================================================================================
/tp @s ~ ~20 ~                                      | Spostamento di emergenza in alto (+20 blocchi)
/tp @s <X> <Y> <Z>                                  | Teletrasporto a coordinate globali assolute
/time rate 0.1                                      | Rallenta il ciclo giorno/notte a 1/10 (WorldClock)
/time pause  |  /time resume                        | Ferma / riattiva il ciclo giorno/notte
/time set day                                       | Imposta l'ora sul mattino (ore 07:00)
/tick rate 2                                        | Rallenta la fisica del mondo al 10% (Simulazione)
/tick rate 20                                       | Ripristina la velocità fisica standard
/tick freeze  |  /tick unfreeze                     | Congela / sblocca tutte le entità del mondo
/gamerule advance_time false                        | Blocca il sole/luna (regola perenne)
/gamerule keep_inventory true                       | Mantiene oggetti ed exp alla morte
/gamerule mob_griefing false                        | Impedisce distruzione blocchi da Creeper
/gamerule fall_damage false                         | Disattiva i danni da caduta
/locate structure #minecraft:village                | Localizza il villaggio più vicino
/give @s minecraft:torch 64                         | Assegna 64 torce all'inventario
/fill ~-5 ~-1 ~-5 ~5 ~3 ~5 oak_planks hollow        | Genera stanza vuota 11x5x11 attorno al giocatore
/effect give @s saturation infinite 0 true          | Fame sempre piena
/spawnpoint @s                                      | Imposta punto di rinascita personale qui
==================================================================================================
```

---

## 16. Metodologia di Verifica e Fonti Tecniche

Le informazioni contenute in questo documento sono state verificate tramite le seguenti fonti:

1. **Minecraft Java Edition 26.2**:
   - **Bytecode e Registri dei Comandi**: Decompilazione e ispezione dei comandi `TimeCommand`, `TickCommand`, `TeleportCommand`, `FillCommand`, `SetBlockCommand`, `CloneCommands`, `LocateCommand`, `EffectCommands`, `GameModeCommand`, `SpawnpointCommand` (`minecraft-merged-deobf-26.2.jar`).
   - **Registri delle GameRules**: Ispezione statica della classe `net.minecraft.world.level.gamerules.GameRules`.
2. **Minecraft Access 1.12.0 (Fork Locale)**:
   - **Codice Sorgente**: Ispezione delle classi `PositionNarrator.java`, `InventoryControls.java`, `PlayerStatus.java`, `CameraControls.java`, `MouseSimulation.java`.
   - **Gestione Key Mapping**: Ispezione delle chiamate a Kuma API e verifica del flag `.ignoreScreenFocus()` in `inventory_controls.recipe_info`.
3. **Mod Aggiuntive**:
   - Configurazione e test di **FallingTree Fabric 26.2**.
