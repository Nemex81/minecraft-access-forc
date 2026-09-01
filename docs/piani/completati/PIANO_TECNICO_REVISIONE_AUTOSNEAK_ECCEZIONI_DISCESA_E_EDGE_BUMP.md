# Piano Tecnico di Revisione: Auto-Sneak Vettoriale, Eccezioni di Discesa Sicura & Feedback sul Ciglio

## 🎯 Obiettivo del Piano

Risolvere le due criticità emerse durante il collaudo in-game dell'Auto-Sneak Edge Protection:
1. **Feedback di Barriera Attiva sul Ciglio (Edge Bump)**: Garantire un riscontro vocale e sonoro 3D chiaro e configurabile quando il giocatore preme attivamente i tasti di marcia (`W`, `A`, `D`) contro il ciglio del dislivello, e consentire l'indietreggiamento istantaneo senza attrito con `S`.
2. **Eccezioni di Discesa Sicura & Strutture Verticali (Scale a Pioli & Arrampicabili)**: Permettere al giocatore di scendere da tetti, soppalchi e piattaforme elevate imboccando scale a pioli a parete (`LadderBlock`), liane (`VineBlock`), impalcature (`ScaffoldingBlock`) e atterraggi morbidi/smorzati, senza che l'auto-sneak intervenga a bloccare la discesa intenzionale.

---

## 🛠️ Architettura Tecnica delle Modifiche

### 1. Calcolo Vettoriale dell'Intenzione di Movimento (`FallDetector.java`)
- **Problema**: A velocità zero ($v \approx 0$), il raggio scansionava solo l'angolo della testa (`yaw` frontale), ignorando i tasti premuti (`S`, `A`, `D`).
- **Logica Vettoriale**:
  - Intercettare lo stato di `client.options.keyUp`, `keyDown`, `keyLeft`, `keyRight`.
  - Calcolare l'angolo di marcia inteso:
    * `W` (Avanti): $\theta = 0^\circ$
    * `S` (Indietro): $\theta = 180^\circ$
    * `A` (Sinistra): $\theta = -90^\circ$
    * `D` (Destra): $\theta = +90^\circ$
    * Diagonali (`W+A`, `W+D`, `S+A`, `S+D`): $\theta = \pm 45^\circ, \pm 135^\circ$
  - *Se* `S` viene premuto per indietreggiare verso una superficie solida, *allora* il look-ahead alle spalle non rileva baratri: `handleDangerCleared()` scatta all'istante, rilasciando l'auto-sneak per una retromarcia fluida.
  - *Se* nessun tasto di movimento è premuto e il giocatore è fermo ($v \approx 0$), non proiettare falsi allarmi frontali continuativi.

---

### 2. Matrice delle Eccezioni di Discesa Sicura (`FallDetector.java`)
- **Problema**: Le scale a pioli (`LadderBlock`), liane e impalcature hanno collisione vuota, quindi l'algoritmo considerava la colonna come vuoto d'aria pericoloso.
- **Riconoscimento Discese Controllate**:
  1. **Blocchi Arrampicabili (`BlockTags.CLIMBABLE`)**:
     - *Se* `stepPos`, `checkGround` o i blocchi lungo la colonna verticale verso cui ci si muove contengono elementi con tag `BlockTags.CLIMBABLE` (`LadderBlock`, `VineBlock`, `ScaffoldingBlock`, `WeepingVinesBlock`, `TwistingVinesBlock`, `CaveVinesBlock`), e la scala prosegue fino a terra (o con atterraggio $\le 3$ blocchi dal fondo), *allora* il dislivello è valutato **sicuro** (`depth = 0`).
     - L'auto-sneak non interviene e il giocatore scende normalmente dal tetto.
  2. **Smorzatori di Caduta & Superfici Morbide**:
     - *Acqua / Colonne di Bolle* (`Fluids.WATER` / `isWaterlogged`): caduta sicura (`depth = 0`).
     - *Ragnatele* (`CobwebBlock`): caduta sicura (`depth = 0`).
     - *Ballette di Fieno* (`HayBlock`), *Blocchi di Miele* (`HoneyBlock`), *Slime* (`SlimeBlock`), *Neve Polverosa* (`PowderSnowBlock`): caduta assorbita/sicura.

---

### 3. Feedback Vocale e Sonoro di Riscontro sul Ciglio (`EdgeBumpFeedbackMode`)
- **Nuovo Enum in `Config.java`**:
  ```java
  public enum EdgeBumpFeedbackMode {
      SOUND_AND_VOICE,
      SOUND_ONLY,
      VOICE_ONLY,
      OFF
  }
  ```
- **Comportamento a Bordo Baratro**:
  - *Se* il giocatore tenta di avanzare con `W` contro il ciglio protetto dall'auto-sneak:
  - Il sistema emette il riscontro sonoro 3D al bordo e/o la narrazione:
    *"Sul ciglio: burrone avanti, 4 blocchi"* (o *"Sul ciglio del dislivello, avanzamento protetto"*).
  - Debouncing temporale a 30 tick (1.5 secondi) per evitare sovrapposizioni vocali a raffica.
- **Configurabilità in Cloth Config (`Ctrl + O`)**:
  - Scheda *Rilevatore di cadute* -> Opzione *Feedback sul ciglio del baratro* con i 4 livelli selezionabili.

---

### 4. Disaccoppiamento tra Pre-Allerta a Distanza e Auto-Sneak al Ciglio Immediato (`FallDetector.java`)
- **Problema Riscontrato nel Collaudo**: `findDangerAhead` scansionava fino a `config.slowdownDistance` (2.5-3.0 blocchi) e quando intercettava un dislivello attivava contemporaneamente sia l'avviso a distanza sia `autoSneakActive = true`. Di conseguenza, il giocatore si accovacciava e procedeva a passo d'uomo frenato per 3 interi blocchi prima del bordo reale.
- **Architettura a Due Zone di Raggio**:
  1. **Zona 1 — Pre-Allerta Informativa & Rallentamento Corsa ($0.85\text{ m} < d \le \text{slowdownDistance}$)**:
     - *Se* il dislivello si trova a distanza $> 0.85\text{ m}$:
       - *Allora*: attiva `autoSlowdown` (interrompe lo sprint), emette l'avviso vocale e sonoro preventivo (*"Attenzione: burrone davanti, N blocchi"*).
       - **Auto-Sneak DISATTIVATO (`autoSneakActive = false`)**: il giocatore cammina a velocità normale verso il baratro senza alcuna frenata posturale.
  2. **Zona 2 — Bordo Fisico Immediato / Ciglio ($d \le 0.85\text{ m}$)**:
     - *Se* il dislivello si trova nel blocco immediatamente adiacente ($\le 0.85\text{ m}$, ultimo passo prima del vuoto):
       - *Allora*: attiva `autoSneakActive = true` (`setShiftKeyDown(true)` e `keyShift.setDown(true)`).
       - Minecraft blocca fisicamente l'uscita dalla piattaforma impedendo la caduta.
       - *Se* si insiste con `W` contro il vuoto: scatta il feedback debounced dell'Edge Bump (*"Sul ciglio: burrone davanti, N blocchi"*).
  3. **Zona 3 — Allontanamento o Rotazione Sicura**:
     - *Se* il giocatore indietreggia (`S`), ruota la visuale o rilascia i tasti:
       - La distanza esce dalla Zona 2, `autoSneakActive` viene rilasciato istantaneamente e il personaggio si rialza in piedi senza alcun attrito.

---

### 5. Localizzazione I18N (`it_it.json` & `en_us.json`)
- Inserimento delle chiavi `text.autoconfig.minecraft-access.enum.EdgeBumpFeedbackMode.*` e `minecraft_access.fall_detector.edge_bump` in **rigoroso ordine alfabetico crescente**.

---

## 🧪 Piano di Verifica e Collaudo

1. **Test Automatici JUnit (JDK 25)**:
   - Validazione delle chiavi I18N in italiano e inglese.
   - Test unitario del calcolo vettoriale e della matrice eccezioni discesa (scale a pioli, liane, acqua).
2. **Collaudo Manuale In-Game (Fase 2 di Luca)**:
   - Salire sul tetto con la scala a pioli e riscendere: verificare che la scala venga imboccata senza blocchi.
   - Camminare verso un baratro con `W`: verificare che l'auto-sneak trattenga il personaggio e vocalizzi il riscontro sul ciglio.
   - Premere `S`: verificare che il personaggio indietreggi immediatamente senza impuntamenti.
   - Configurare in `Ctrl + O` il feedback su *Solo Suono*, *Solo Voce*, *Entrambi*, *Disattivato*.
