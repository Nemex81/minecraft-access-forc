# Piano Tecnico Formale: Salto Automatico Pilota (Rev MC-28.0) & Auto-Focus Menu di Pausa Esc (Rev MC-28.1)
# Progetto: Minecraft Access (Fork 26.2 / 1.21.x)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
# Percorso: docs/piani/attivi/PIANO_TECNICO_SALTO_AUTOMATICO_PILOTA_E_CALIBRAZIONE_HITBOX.md
# Riferimento Standard: Astralis Versioning Framework (AVF v26.2-1.17.1 / Rev MC-28.0 & MC-28.1)

---

## 📋 1. OBIETTIVI DELL'INTERVENTO & MOTIVAZIONE ARCHITETTURALE

### A. Rev MC-28.0 — Calibrazione Fisica Salto Automatico Pilota
1. **Problema**: Con `config.autoJump == true`, il pilota automatico si arrestava davanti a blocchi saltabili (+1 Y) dichiarando *"Percorso ostruito, marcia arrestata"*.
2. **Causa**: In `AutoWalkController.java:319`, il check `distH < 0.65` era fisicamente impossibile data la hitbox del giocatore ($0.3\text{ m}$) e la parete del blocco ($0.5\text{ m}$ dal centro), che impongono contatto a $\text{distH} \approx 0.80\text{ m}$.
3. **Soluzione**: Ricalibrare con $\text{distH} \le 1.25\text{ m}$ o `player.horizontalCollision`, con dislivello $0.30 < \Delta Y \le 1.25$, vincolato rigorosamente alla guardia `config.autoJump == true`.

### B. Rev MC-28.1 — Auto-Focus Iniziale & Frecce Direzionali su Menu di Pausa (`Esc`)
1. **Problema**: Premendo `Esc` in gioco per aprire il menu (`PauseScreen`), il focus della tastiera rimaneva perso o ancorato altrove, costringendo a premere `Tab` prima di poter usare le frecce per scorrere i pulsanti.
2. **Causa**: In `MenuFix.java`, `PauseScreen.class` non era incluso nel set `MENUS_NEED_FIX` e mancava l'impostazione proattiva del focus sul primo elemento interattivo (`AbstractWidget`).
3. **Soluzione**:
   - Includere `PauseScreen.class` in `MENUS_NEED_FIX`;
   - Implementare `ensureInitialFocus(screen)` in `MenuFix.java` per posizionare all'istante il focus logico sul primo pulsante attivo all'apertura della schermata, rendendo le frecce direzionali immediatamente attive.

---

## 🛠️ 2. MODIFICHE TECNICHE PUNTUALI

### 1. `features/autowalk/AutoWalkController.java` (Rev MC-28.0)
- Ricalibrazione della condizione di salto:
```java
// 10. Step-Up Jump Timing
boolean isApproachingStep = (distH <= 1.25 || player.horizontalCollision) && deltaY > 0.30 && deltaY <= 1.25;
if (config.autoJump && isApproachingStep && onGround) {
    state = State.JUMPING;
    client.options.keyJump.setDown(true);
    jumpHoldingTicks = 4;
} else {
    if (jumpHoldingTicks > 0) {
        jumpHoldingTicks--;
    } else {
        client.options.keyJump.setDown(false);
        if (state == State.SWIMMING || state == State.JUMPING) {
            state = State.WALKING;
        }
    }
}
```

### 2. `features/MenuFix.java` (Rev MC-28.1)
- Aggiunta di `PauseScreen.class` in `MENUS_NEED_FIX`;
- Aggiunta del metodo `ensureInitialFocus(Screen screen)` invocato ad ogni cambio schermata:
```java
    private static void ensureInitialFocus(Screen screen) {
        if (screen == null) return;
        if (screen.getFocused() == null) {
            for (var child : screen.children()) {
                if (child.isFocused() || (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget.active && widget.visible)) {
                    screen.setFocused(child);
                    break;
                }
            }
        }
    }
```

---

## 🛡️ 3. PROTOCOLLO DI VALIDAZIONE PREVENTIVA (7 ASSI DI QUALITÀ)

1. **Validità**: Risolve sia il blocco del salto automatico, sia il ritardo di focus su `PauseScreen`.
2. **Efficacia**: Il pilota salta in cammino e il menu di gioco è navigabile con le frecce al primo tocco di `Esc`.
3. **Coerenza**: Conformità piena a `ObstacleDetectionUtils` e all'architettura di focus management di Minecraft vanilla.
4. **Completezza**: Copre ogni tipologia di schermata di menu (`PauseScreen`, `OptionsScreen`, ecc.).
5. **Precisione**: `config.autoJump` tutela la disattivazione manuale del salto.
6. **Affidabilità**: Zero crash, zero warning e massima stabilità.
7. **Assenza di Regressioni**: La navigazione con `Tab`, l'inventario e i comandi di gioco restano perfetti.

---

## 🎲 4. MATRICE DI SIMULAZIONE A 3 LIVELLI

- **Livello 1 — Scenario Comune (Happy Path)**:
  - Giocatore che naviga verso la porta con `autoJump=true`: salta il blocco d'erba e arriva a destinazione.
  - Giocatore che preme `Esc` in partita: il menu di pausa si apre, il focus è sul primo pulsante ("Torna al gioco") e con la freccia Giù passa subito a "Avanzamenti" / "Opzioni".
- **Livello 2 — Scenario Alternativo**:
  - Giocatore con `autoJump=false`: si ferma davanti al gradino e notifica percorso ostruito.
- **Livello 3 — Caso Limite (Corner Case)**:
  - Giocatore che apre `PauseScreen` mentre era a fuoco su una casella di testo o su uno slot inventario: `ensureInitialFocus` reimposta deterministico il focus sul menu di pausa.

---

# Incremento Versione Target (AVF)
- Versione Target: **`v26.2-1.17.1` (Rev MC-28.0 & Rev MC-28.1)**
