# Piano Tecnico (Fase 1A): Revisione Post-Collaudo Sguardo & Auto-Sneak Edge Protection

## 📌 1. Obiettivo e Quadro di Riferimento
Implementare la suite di revisione emersa dalla sessione di collaudo empirico su Server Tenuta:
1. **Punto 7 — Notifica Vocale Completa su `Alt + V` (Zero Troncamento)**: Integrare in un unico messaggio coerente il report panoramico e l'avvenuta rotazione dello sguardo con indicazione di punto cardinale, gradi e inclinazione.
2. **Punto 8 — Standard Sistemico Globale di Notifica Rotazione Assistita**: Allineare `ObjectTracker` (POI, alberi con `End`, risorse) alla formula universale `[Target]: [Direzione, Gradi, Inclinazione]`.
3. **Punto 9 — Protezione Anticaduta con Auto-Sneak Edge Protection**:
   - Attivazione dell'accovacciamento forzato (`shiftKeyDown = true` via `KeyboardInputMixin`) sul ciglio di baratri/dirupi $\ge$ alla soglia configurata.
   - Sfruttamento della fisica nativa di Minecraft (`isStayingOnGround()`) per impedire al personaggio di precipitare anche tenendo premuto `W`.
   - Sonificazione 3D posizionale sul bordo del baratro e avviso vocale direzionale.
   - 4 opzioni di configurazione dedicate nella scheda *Rilevatore di Caduta* di Cloth Config (`Ctrl + O`).

---

## 🛠️ 2. Mappa Dettagliata delle Modifiche Tecniche

### A. `ObstacleDetector.java` (Punto 7)
- In `inspectObstacle()`:
  - Se `config.lookAtObstacleOnInspection` e `closestObs.result().lookAtPos() != null`:
    - Esegue `LookHistoryManager.saveCurrentLook(...)`.
    - Esegue `client.player.lookAt(...)`.
    - Compone un unico messaggio:
      ```java
      String panoMsg = ObstacleDetectionUtils.getPanoramicNarrationMessage(pano, config.narrationStyle);
      String facing = PlayerPositionUtils.getFullFacingInWords(true);
      String lookedAtMsg = I18n.get("minecraft_access.obstacle_detector.looked_at_obstacle", facing);
      MainClass.narrate(panoMsg + ". " + lookedAtMsg, true);
      ```
    - Riproduce il segnale sonoro 3D.
  - Se la rotazione non è attiva: invia normalmente solo `panoMsg`.

---

### B. `ObjectTracker.java` (Punto 8)
- In `lookAtCurrentObject()`:
  - Aggiornare i messaggi di puntamento per entità, blocchi e waypoint includendo `PlayerPositionUtils.getFullFacingInWords(true)`.
  - Risultato: *"Visuale orientata verso Tronco di quercia: Ovest, 270 gradi, Dritto"*.

---

### C. `FallDetector.java`, `Config.java` & `KeyboardInputMixin.java` (Punto 9)
1. **`Config.java`**:
   - In `Config.FallDetector`:
     - `public boolean autoSneakOnEdge = true;`
     - `public boolean playAudioCues = true;`
2. **`FallDetector.java`**:
   - Variabile di stato statica:
     ```java
     private static boolean autoSneakActive = false;
     public static boolean isAutoSneakActive() { return autoSneakActive; }
     ```
   - In `handleDangerDetected`:
     - Se `config.autoSneakOnEdge`: imposta `autoSneakActive = true;`.
     - Se `config.playAudioCues`: riproduce `SoundEvents.ANVIL_LAND` o audio 3D posizionale alle coordinate di `dangerPos`.
     - Se `config.voiceWarning`: invia avviso vocale direzionale con profondità.
   - In `handleDangerCleared` e `resetSafetyState`: imposta `autoSneakActive = false;`.
3. **`KeyboardInputMixin.java` [NEW MIXIN]**:
   - Creazione di `KeyboardInputMixin` per `net.minecraft.client.player.KeyboardInput`:
     ```java
     @Inject(method = "tick", at = @At("TAIL"))
     private void onTickTail(boolean slowDown, float sneakModifier, CallbackInfo ci) {
         if (FallDetector.isAutoSneakActive()) {
             this.shiftKeyDown = true;
         }
     }
     ```
   - Registrazione del mixin in `minecraft-access.mixins.json`.

---

### D. Localizzazioni I18N (`it_it.json` & `en_us.json`)
- Inserimento delle chiavi in rigoroso ordine alfabetico crescente:
  - `"minecraft_access.obstacle_detector.looked_at_obstacle"`
  - `"text.autoconfig.minecraft-access.option.fallDetector.autoSneakOnEdge"`
  - `"text.autoconfig.minecraft-access.option.fallDetector.autoSneakOnEdge.@Tooltip"`
  - `"text.autoconfig.minecraft-access.option.fallDetector.playAudioCues"`
  - `"text.autoconfig.minecraft-access.option.fallDetector.playAudioCues.@Tooltip"`

---

## 🧪 3. Piano di Verifica e Collaudo

### Test Unitari Automatizzati
- Aggiornamento / creazione di test in `src/test/java/`:
  - Test per `FallDetector.isAutoSneakActive()` (attivazione su pericolo, reset su safe).
  - Test per stringhe I18N complete e ordinate in `it_it.json` ed `en_us.json`.

### Collaudo Manuale In-Game (Fase 2)
1. **Ispezione Ostacoli (`Alt + V`)**:
   - Posizionarsi davanti a una staccionata a 45°, premere `Alt + V`.
   - Verificare che il messaggio vocale legga sia la panoramica sia l'orientamento finale senza tagli di voce.
2. **Puntamento Oggetti (`End`)**:
   - Puntare un albero con `End` e verificare l'annuncio con punto cardinale e gradi.
3. **Auto-Sneak Edge Protection**:
   - Avvicinarsi al ciglio di un baratro/buco di 4 blocchi tenendo premuto `W`.
   - Verificare che il personaggio si accovacci da solo e si fermi fisicamente sul bordo del blocco senza cadere.
   - Rilasciare `W` o girarsi e verificare che si rimetta in piedi.
