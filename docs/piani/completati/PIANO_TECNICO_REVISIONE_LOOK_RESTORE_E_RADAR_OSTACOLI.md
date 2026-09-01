# Piano Tecnico Correttivo: Look Restore, Sincronizzazione Rotta & Traduttore Enum

## 📌 1. Obiettivo e Quadro di Riferimento
Questo piano consolida e formalizza il pacchetto completo delle 4 rifiniture del sistema:
1. **Look Restore con "Ancora di Partenza" per Rotazioni Manuali**: Quando il giocatore ruota a scatti con `I, J, K, L` o con le frecce del tastierino, il sistema registra l'**angolo iniziale di partenza** all'inizio della sequenza (con finestra di inattività di 1500 ms). Premendo `Backspace` o `Ctrl + Numpad 5`, la visuale torna all'istante alla rotta iniziale prima dell'esplorazione.
2. **Mappatura Look Restore su `Backspace` su Tastiera Estesa**: Assegnare il tasto `InputConstants.KEY_BACKSPACE` (`Backspace`) al comando `camera_controls.restore_previous_look`.
3. **Sincronizzazione Manuale Rotta di Riferimento (`Ctrl + Backspace`)**: Comando dedicato `camera_controls.sync_reference_look` (`Ctrl + Backspace`, compatibile con Ctrl sinistro e destro) per fissare l'orientamento attuale del personaggio come nuova rotta di riferimento o resettare lo stato di Look History. Feedback vocale: *"Rotta di riferimento fissata a %s"*.
4. **Traduttore Automatico Universale Enum in `ConfigExtension`**: Registrare un `AnnotationTransformer` per `ConfigEntry.Gui.EnumHandler` in `ConfigExtension.apply(GuiRegistry)` per iniettare un `EnumNameProvider` translatable che risolve dinamicamente le traduzioni italiane di tutti i bottoni enum dell'intera mod (`DIRECT`, `SLOPE`, `EIGHT_DIRECTIONS`, ecc.).

---

## 🛠️ 2. Dettaglio Tecnico delle Modifiche

### A. Modifiche a `LookHistoryManager.java`
- Aggiunta campi e metodi per la gestione dell'ancora manuale e della sincronizzazione:
  ```java
  private static long lastManualRotationTime = 0L;
  private static final long MANUAL_ROTATION_WINDOW_MS = 1500L;

  public static void recordManualRotation(float currentYaw, float currentPitch) {
      long now = System.currentTimeMillis();
      if (now - lastManualRotationTime > MANUAL_ROTATION_WINDOW_MS) {
          saveCurrentLook(currentYaw, currentPitch);
      }
      lastManualRotationTime = now;
  }

  public static boolean syncReferenceLook(Minecraft client) {
      if (client.player == null) return false;
      saveCurrentLook(client.player.getYRot(), client.player.getXRot());
      lastManualRotationTime = System.currentTimeMillis();
      String facingWords = PlayerPositionUtils.getFacingDirectionInWords();
      MainClass.narrate(I18n.get("minecraft_access.camera_controls.reference_look_set", facingWords), true);
      return true;
  }
  ```

---

### B. Modifiche a `CameraControls.java` & `NumpadControls.java`
- **Rotazioni manuali**:
  - `CameraControls.java` (`rotateCameraBy`): invocare `LookHistoryManager.recordManualRotation(...)`.
  - `NumpadControls.java` (`rotateCamera`): invocare `LookHistoryManager.recordManualRotation(...)`.
- **Keybinding Look Restore**:
  - `camera_controls.restore_previous_look` registrato con predefinito `InputBinding.key(InputConstants.KEY_BACKSPACE)` (senza modificatori).
- **Keybinding Sincronizzazione Rotta**:
  - `camera_controls.sync_reference_look` registrato con predefinito `InputBinding.key(InputConstants.KEY_BACKSPACE, KeyModifiers.of(KeyModifier.CONTROL))` (gestisce sia Ctrl sinistro che destro).

---

### C. Traduttore Dinamico Enum in `ConfigExtension.java`
- Registrazione del trasformatore in `ConfigExtension.apply(GuiRegistry registry)` per iniettare l'I18N translatable su tutte le opzioni Enum dell'intera mod.

---

### D. File di Localizzazione (`it_it.json` & `en_us.json`)
- Aggiunta stringhe per la sincronizzazione:
  - IT: `"key.minecraft_access.camera_controls.sync_reference_look": "Fissa rotta di riferimento"`
  - IT: `"minecraft_access.camera_controls.reference_look_set": "Rotta di riferimento fissata a %s"`
  - EN: `"key.minecraft_access.camera_controls.sync_reference_look": "Set reference look"`
  - EN: `"minecraft_access.camera_controls.reference_look_set": "Reference look set to %s"`
- Mantenimento dell'ordinamento alfabetico rigoroso.

---

## 🧪 3. Piano di Verifica
1. Suite automatizzata JUnit con JDK 25 (`.\gradlew.bat --no-daemon :test`).
2. Build e Deploy automatico ShadowJar nelle istanze PrismLauncher.
3. Test in-game:
   - Rotazione manuale `L, L, L` -> `Backspace` -> ritorno alla rotta di partenza.
   - Pressione `Ctrl + Backspace` -> conferma parlata *"Rotta di riferimento fissata a..."*.
   - Apertura Cloth Config `Ctrl + O` -> verifica traduzioni italiane complete sui pulsanti.
