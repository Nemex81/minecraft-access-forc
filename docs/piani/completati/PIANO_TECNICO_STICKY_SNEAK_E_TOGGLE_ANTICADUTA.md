# Piano Tecnico di Revisione: Presidio Fisico del Ciglio (Sticky Sneak) & Toggle Rapido `Ctrl + Alt + F`

## 🎯 Obiettivo del Piano

Completare e blindare l'Auto-Sneak Edge Protection del modulo `FallDetector.java` affrontando i Punti 13 e 14 del Registro Revisioni:
1. **Presidio Fisico del Ciglio da Fermo (Sticky Sneak on Edge & Riaccovacciamento Istantaneo)**: Mantenere l'accovacciamento forzato anche a velocità zero ($v \approx 0$) e al rilascio di `W` finché il giocatore si trova sul bordo di un dislivello pericoloso ($\le 1.0\text{ m}$), impedendo ogni bypass da salto (`Spazio`) o micro-tap a raffica. Garantire il riaccovacciamento istantaneo nello stesso tick alla riattivazione da fermi sul bordo. Consentire il disimpegno immediato in retromarcia con `S` o allontanamento.
2. **Scorciatoia di Toggle Rapido in Tempo Reale (`Ctrl + Alt + F`)**: Offrire al giocatore la possibilità di attivare e disattivare al volo la protezione fisica con una combinazione di tasti comoda, audio-sonificata a due toni e vocalizzata, senza aprire la GUI delle impostazioni.

---

## 🛠️ Architettura Tecnica delle Modifiche

### 1. Presidio Fisico del Ciglio & Riaccovacciamento Istantaneo (`FallDetector.java`)
- **Problema**: All'azzeramento della velocità o rilascio di `W`, il sistema chiamava ciecamente `handleDangerCleared`, facendo alzare in piedi il personaggio a pochi centimetri dal baratro.
- **Logica di Presidio**:
  - In `checkLookAheadSafety`:
    - *Se* `moveDir != null` (il giocatore tenta di muoversi):
      - Esegue la scansione predittiva lungo `moveDir`.
      - *Se* rileva pericolo a $d \le 0.85\text{ m}$ -> attiva/mantiene `autoSneakActive = true`.
      - *Se* il tragitto è sicuro (es. retromarcia con `S` o cammino verso pavimento solido) -> chiama `handleDangerCleared(client, player)` e il personaggio si rialza all'istante.
    - *Se* `moveDir == null` (giocatore fermo o nessun tasto di movimento premuto):
      - *Se* `config.autoSneakOnEdge` è attivo:
        - Esegue un controllo di prossimità del bordo: se `autoSneakActive` è già attivo e `distSqr(lastWarnedDangerPos) <= 2.25` ($\le 1.5\text{ m}$), **oppure** se la scansione radiale immediata dei 4 lati della hitbox a quota piedi ($0.5\text{m} - 0.85\text{m}$) intercetta un dislivello $\ge \text{depth}$:
          - **Mantiene / imposta `autoSneakActive = true`** e forza `client.options.keyShift.setDown(true)` / `player.setShiftKeyDown(true)`.
          - Ritorna senza azzerare lo sneak.
      - *Altrimenti* (giocatore in mezzo a una stanza sicura o lontano da pericoli) -> chiama `handleDangerCleared(client, player)`.

---

### 2. Scorciatoia di Toggle Rapido (`fall_detector.toggle_auto_sneak`)
- **Keybinding Kuma**:
  - Tasto predefinito: `InputConstants.KEY_F` con modificatori `KeyModifiers.of(KeyModifier.CONTROL, KeyModifier.ALT)`.
  - Categoria: `KeyMappingCategories.OTHER`.
  - Handler di input: schermato con `ModifierUtils.hasControlAndAlt()`.
- **Azione `toggleAutoSneak()`**:
  - Inverte `config.autoSneakOnEdge = !config.autoSneakOnEdge`.
  - Salva la configurazione: `Config.save()`.
  - *Se disattivato (`OFF`)*:
    - Chiama `resetSafetyState()`, liberando immediatamente lo sneak per consentire il salto/tuffo.
    - Riproduce suono click grave ($0.8\text{ pitch}$, volume $0.8$).
    - Vocalizza: `"Auto-accovacciamento anticaduta: Disattivato"` (IT) / `"Fall auto-sneak protection: Disabled"` (EN).
  - *Se attivato (`ON`)*:
    - Esegue immediatamente `checkLookAheadSafety`: se il giocatore si trova sul ciglio da fermo, **lo riaccovaccia all'istante nello stesso tick**.
    - Riproduce suono click acuto ($1.2\text{ pitch}$, volume $0.8$).
    - Vocalizza: `"Auto-accovacciamento anticaduta: Attivato"` (IT) / `"Fall auto-sneak protection: Enabled"` (EN).

---

### 3. Localizzazione I18N (`it_it.json` & `en_us.json`)
Inserimento rigorosamente alfabetico delle chiavi:
- `key.minecraft_access.fall_detector.toggle_auto_sneak`
- `minecraft_access.fall_detector.auto_sneak_disabled`
- `minecraft_access.fall_detector.auto_sneak_enabled`

---

### 4. Neutralizzazione Mixin del Salto sul Ciglio (`LivingEntityMixin.java`)
- **Diagnosi della Corsa dei Tick**: In Minecraft, `LocalPlayer.aiStep()` elabora l'input della tastiera e invoca `LivingEntity.jumpFromGround()` *prima* che l'evento Balm `ClientPlayingTick.AFTER` possa intervenire. Modificare `keyJump.setDown(false)` nel tick successivo risulta inefficace poiché il giocatore è già decollato in aria.
- **Soluzione Mixin Chirurgica**:
  - In `LivingEntityMixin.java`, iniezione cancellabile all'ingresso di `jumpFromGround()`:
    ```java
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void cancelJumpWhenAutoSneakActive(CallbackInfo ci) {
        if (Objects.equals(Minecraft.getInstance().player, this) && FallDetector.isAutoSneakActive()) {
            ci.cancel();
        }
    }
    ```
  - **Effetto Fisico**: Quando `autoSneakActive == true`, qualsiasi impulso di salto viene annullato all'origine prima del calcolo della spinta verso l'alto. Il personaggio rimane saldamente a terra sul ciglio.
  - **Ripulitura Codice**: Rimozione dell'istruzione inefficace `keyJump.setDown(false)` da `FallDetector.java`.
  - **Salto Volontario**: Disattivando lo scudo con `Ctrl + Alt + F`, `isAutoSneakActive()` diventa `false` e `jumpFromGround()` torna pienamente eseguibile.

---

## 🧪 Piano di Verifica e Collaudo

1. **Test Automatici JUnit (JDK 25)**:
   - Verifica compilazione Java e conformità alfabetica JSON.
2. **Collaudo Manuale In-Game (Fase 2 di Luca)**:
   - **Test 1 (Sticky Sneak sul bordo)**: Camminare verso il baratro con `W`, fermarsi sul ciglio rilasciando `W` -> verificare che il personaggio rimanga accovacciato da fermo.
   - **Test 2 (Anti-Bypass da Salto e Tap)**: Provare a premere `Spazio` o picchiettare `W` a raffica da fermi sul bordo -> verificare che il personaggio non cada.
   - **Test 3 (Retromarcia fluida)**: Premere `S` dal ciglio -> verificare che il personaggio indietreggi e si rialzi istantaneamente appena allontanato.
   - **Test 4 (Toggle `Ctrl + Alt + F`)**:
     - Premere `Ctrl + Alt + F` sul ciglio -> verificare disattivazione e rialzamento in piedi per poter saltare.
     - Premere `Ctrl + Alt + F` di nuovo sul ciglio -> verificare riattivazione e **riaccovacciamento istantaneo sul posto**.
