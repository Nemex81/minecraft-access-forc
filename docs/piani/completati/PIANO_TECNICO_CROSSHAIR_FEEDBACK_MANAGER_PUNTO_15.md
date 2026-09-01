# Piano Tecnico Integrale (Fase 1A / Fase 3): `CrosshairFeedbackManager` Modulare & Scalabile (Punto 15)
# Autore: Luca & Antigravity
# Data: 2026-09-01
# Ambito: Repository `minecraft-access`
# Stato: Completato con successo e collaudato in-game (Fase 3)

---

## 📌 1. Obiettivo e Quadro di Riferimento

Il presente Piano Tecnico definisce l'architettura, la logica di coordinamento e i dettagli implementativi del modulo **`CrosshairFeedbackManager`** (Punto 15 del Registro Revisioni), integrando il **modello a token informativi indipendenti** e l'**ordinamento modulare configurabile** concordati con Luca.

### Il Problema Risolto (Race Condition & Collisione Vocale)
In precedenza, la lettura del mirino ([`NarrateCrosshair.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/NarrateCrosshair.java)) e la lettura dell'orientamento della visuale ([`CameraControls.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/CameraControls.java) e [`NumpadControls.java`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/src/main/java/org/mcaccess/minecraftaccess/features/NumpadControls.java)) operavano come sottosistemi indipendenti e disaccoppiati.
Durante la rotazione della visuale o il centramento dell'orizzonte (es. tastierino numerico o tasti visuale):
1. La rotazione emetteva una narrazione dell'orientamento (es. *"Sud, 180 gradi, Dritto"* con `interrupt: true`).
2. Nello stesso tick o nel tick immediatamente successivo, il raycast del mirino rilevava il nuovo blocco ed emetteva la narrazione del target (es. *"Mattoni di pietra"* con `interrupt: true`).
3. **Risultato**: Uno dei due messaggi veniva brutalmente troncato a metà dallo screen reader NVDA, generando un'esperienza confusa, caotica e frammentata.

### La Soluzione Architetturale Implementata
Introdotto **`CrosshairFeedbackManager`** secondo il pattern **Single Source of Truth & Coordinator**:
- Accentra la gestione dello stato del puntamento e della visuale.
- Esegue il debouncing atomico tra rotazione ed eventi mirino.
- Assembla e vocalizza un'**unica frase atomica** strutturata secondo i 5 token informativi configurati dall'utente e l'ordine di lettura preferito, eliminando alla radice ogni corsa critica.

---

## 🏛️ 2. Architettura Tecnica e Modello Dati a Token

```
                        +----------------------------------------+
                        |         CrosshairFeedbackManager       |
                        |      (Single Source of Truth)          |
                        +----------------------------------------+
                                      ^            ^
                                     /              \
        (Raycast Target Change)     /                \  (Camera Turn / Snap / Center)
                                   /                  \
                    +--------------------+      +--------------------+
                    |  NarrateCrosshair  |      | CameraControls &   |
                    |                    |      | NumpadControls     |
                    +--------------------+      +--------------------+
```

### A. I 5 Token Informativi Indipendenti
Ciascuna informazione può essere attivata o disattivata indipendentemente tramite toggle dedicati in `Config.NarrateCrosshair`:

1. **`includeBlock`** (boolean, default: `true`): Nome del blocco o entità mirata (es. *"Mattoni di pietra"*).
2. **`includeDistance`** (boolean, default: `false`): Distanza euclidea dal bersaglio (es. *"a 3 blocchi"*).
3. **`includeCardinal`** (boolean, default: `true`): Direzione cardinale orizzontale (es. *"Sud"*, *"Nord-Est"*).
4. **`includeCompassDegrees`** (boolean, default: `true`): Gradi di bussola orizzontale $0^\circ..359^\circ$ (es. *"180 gradi"*).
5. **`includePitchAngle`** (boolean, default: `true`): Inclinazione verticale dello sguardo (es. *"Dritto"*, *"15 gradi Su"*, *"45 gradi Giù"*), fondamentale per stimare immediatamente a quale quota/altezza si trova il blocco mirato rispetto al piano degli occhi.

---

### B. Enum di Ordinamento: `CrosshairReadingOrder`
Per governare la sequenza di narrazione in modo modulare e scalabile:

```java
package org.mcaccess.minecraftaccess.features.crosshair;

public enum CrosshairReadingOrder {
    TARGET_FIRST,                  // Prima il bersaglio (blocco/distanza), poi l'orientamento
    ORIENTATION_FIRST,             // Prima l'orientamento (cardinale/gradi/pitch), poi il bersaglio
    TARGET_CARDINAL_INLINE         // Bersaglio con punto cardinale inline, poi il resto
}
```

#### Esempi di composizione semantica collaudati:
- **`TARGET_FIRST`**: *"Mattoni di pietra, a 3 blocchi, Sud, 180 gradi, 15 gradi Su"*
- **`ORIENTATION_FIRST`**: *"Sud, 180 gradi, 15 gradi Su: Mattoni di pietra, a 3 blocchi"*
- **`TARGET_CARDINAL_INLINE`**: *"Mattoni di pietra a Sud, a 3 blocchi, 180 gradi, 15 gradi Su"*
- **Degrado Automatico & Silenzio Selettivo**:
  - Se tutti i token di orientamento sono disabilitati: *"Mattoni di pietra, a 3 blocchi"*.
  - Se il blocco è disabilitato (`includeBlock = false`): *"Sud, 180 gradi, 15 gradi Su"*.
  - Se il raycast mira nel vuoto (`MISS` / Aria): la parte del target viene omessa in automatico, senza virgole orfane né spazi doppi.

---

## 🛠️ 3. Dettaglio Componenti e Modifiche

### 1. Nuovo Modulo: `CrosshairFeedbackManager.java`
**Percorso**: `src/main/java/org/mcaccess/minecraftaccess/features/crosshair/CrosshairFeedbackManager.java`

**Responsabilità**:
- **Formatore Atomico Modulare (`formatFeedback`)**:
  Metodo statico puro e testabile al 100% che riceve:
  `(String targetName, @Nullable Double distance, String cardinal, int compassDegrees, @Nullable String pitchDirection, CrosshairReadingOrder order, boolean includeBlock, boolean includeDistance, boolean includeCardinal, boolean includeDegrees, boolean includePitch)`
  e compone la stringa pulita secondo le regole linguistiche.
- **Gestione Evento Rotazione (`onCameraRotated` / `onLookCentered`)**:
  - All'atto della rotazione o centramento orizzonte, campiona l'orientamento corrente ed esegue il raycast istantaneo del mirino.
  - Costruisce la frase unificata ed emette una singola chiamata `MainClass.narrate(atomicText, true)`.
  - Applica la sincronizzazione di stato su `NarrateCrosshair` (aggiornando `previousTarget`/`previousNarration` e impostando una breve soppressione di 100ms) per impedire duplicazioni nello stesso tick.
- **Gestione Evento Bersaglio Mirino (`onCrosshairTargetChanged`)**:
  - Invocato da `NarrateCrosshair.tick(...)` quando il giocatore cambia blocco mirato (es. camminando in avanti o muovendo il cursore).
  - Costruisce la frase unificata rispettando i token attivi e l'ordine configurato, emettendo `MainClass.narrate(atomicText, true)`.

---

### 2. Modifica a `NarrateCrosshair.java`
**Percorso**: `src/main/java/org/mcaccess/minecraftaccess/features/NarrateCrosshair.java`

- Conserva il raycast continuo e il calcolo del sound cue 3D posizionale.
- Sostituisce la chiamata diretta `MainClass.narrate(narration, true)` con l'inoltro a `CrosshairFeedbackManager.onCrosshairTargetChanged(rayCast, narration)`.
- Fornisce metodi di coordinamento per sincronizzare `previousTarget` e `previousNarration` con le emissioni generate dalla rotazione telecamera.

---

### 3. Modifiche a `CameraControls.java` e `NumpadControls.java`
**Percorsi**:
- `src/main/java/org/mcaccess/minecraftaccess/features/CameraControls.java`
- `src/main/java/org/mcaccess/minecraftaccess/features/NumpadControls.java`

- **In `CameraControls.java`**:
  - Nei metodi di rotazione (`rotateCameraBy`, `rotateCameraTo`, `rotateCameraToPitch`, `centerCamera`), instrada la narrazione attraverso `CrosshairFeedbackManager.onCameraRotated(...)`.
  - I tasti dedicati alla sola lettura dell'orientamento (`H`, `Alt+H`) continuano a leggere la sola direzione richiesta invocando il manager o `PlayerPositionUtils`.
- **In `NumpadControls.java`**:
  - In `rotateCameraBy(...)`, `rotateCameraTo(...)` e al termine della rotazione continua in `tick()`, delega la narrazione dell'evento a `CrosshairFeedbackManager`.
  - In `centerCameraHorizon()` (Tasto Numpad 5), unifica la riproduzione del suono di snap, l'annuncio vocale e il bersaglio del mirino in una singola transazione coordinata, eliminando la tripla emissione concorrente.

---

### 4. Configurazione Estesa in `Config.java`
**Percorso**: `src/main/java/org/mcaccess/minecraftaccess/Config.java`

All'interno della classe statica `Config.NarrateCrosshair`:
```java
@ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
public CrosshairReadingOrder readingOrder = CrosshairReadingOrder.TARGET_FIRST;

public boolean includeBlock = true;
public boolean includeDistance = false;
public boolean includeCardinal = true;
public boolean includeCompassDegrees = true;
public boolean includePitchAngle = true;
```

---

### 5. Localizzazioni I18N (`it_it.json` ed `en_us.json`)
**Percorsi**:
- `src/main/resources/assets/minecraft_access/lang/it_it.json`
- `src/main/resources/assets/minecraft_access/lang/en_us.json`

Aggiunta in **ordine alfabetico JSON rigoroso** delle relative chiavi di configurazione e composizione.

---

## 🧪 4. Esito Test e Collaudo

### Verifica Automatica JUnit (Fase 1B)
File di test dedicato:
`src/test/java/org/mcaccess/minecraftaccess/features/crosshair/CrosshairFeedbackManagerTest.java`
- **100% test superati** su tutti i 3 ordini di lettura, i 5 token indipendenti, target nulli/aria e conformità JSON CI.

### Collaudo Manuale In-Game di Luca (Fase 2)
- **Esito Positivo**: Tutte le rotazioni, i centramenti orizzonte (Numpad 5) e le modifiche in Cloth Config hanno prodotto narrazioni atomiche impeccabili nei log di telemetria, senza alcun troncamento con NVDA.
