# Piano Tecnico (Fase 1A): Memoria Visuale a Doppio Livello (Buffer Dinamico + Bussola Persistente)

## 📌 1. Obiettivo e Quadro di Riferimento
Separare e armonizzare la gestione della memoria dello sguardo in **due livelli distinti e complementari**:
1. **Livello 1 — Buffer Dinamico Volatile (Undo Sguardo / Breve Termine)**:
   - Aggiornato automaticamente da ostacoli (`Alt+V`), tracciamento risorse/alberi (`End`), punti di interesse (`Home`), mira entità (`X`), scatti cardinali e rotazioni manuali (`I, J, K, L` / tastierino).
   - Richiamabile in qualsiasi momento con **`Backspace`** (o `Ctrl + Numpad 5`) con funzione di A/B Toggle.
2. **Livello 2 — Bussola Persistente di Riferimento (Lungo Termine / Segnalibro Fisso)**:
   - Modificabile **esclusivamente** tramite comando esplicito **`Ctrl + Backspace`** (*"Fissa rotta di riferimento"*). Non viene MAI sovrascritta dai normali movimenti o esplorazioni.
   - Richiamabile in qualsiasi momento con **`Alt + Backspace`** (*"Allinea a rotta di riferimento"*).
   - Prima di ruotare verso la rotta fissa, salva la visuale attuale nel Livello 1, consentendo di tornare indietro con un semplice `Backspace`.

---

## 🛠️ 2. Mappa delle Modifiche Tecniche

### A. Modifiche a `LookHistoryManager.java`
- Introduzione campi dedicati per il Livello 2:
  ```java
  private static float bookmarkYaw = 0.0f;
  private static float bookmarkPitch = 0.0f;
  private static boolean hasBookmark = false;
  ```
- Metodo `syncReferenceLook(Minecraft client)`:
  - Salva `bookmarkYaw` e `bookmarkPitch` con la posizione attuale.
  - Imposta `hasBookmark = true`.
  - Sincronizza anche il Livello 1 (`saveCurrentLook`).
  - Vocalizza: `minecraft_access.camera_controls.reference_look_set` (*"Rotta di riferimento fissata a %s"*).
- Metodo `alignToReferenceLook(Minecraft client)`:
  - Se `!hasBookmark`: vocalizza `minecraft_access.camera_controls.no_reference_look` (*"Nessuna rotta di riferimento fissata"*).
  - Se presente: salva lo sguardo attuale in Livello 1 (`saveCurrentLook(currentYaw, currentPitch)`), imposta `player.setYRot(bookmarkYaw)` e `player.setXRot(bookmarkPitch)`, e vocalizza `minecraft_access.camera_controls.aligned_to_reference_look` (*"Orientato a rotta di riferimento: %s"*).

---

### B. Registrazione Keybinding in `CameraControls.java`
- Aggiunta del comando `camera_controls.align_to_reference_look`:
  ```java
  Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(MainClass.MOD_ID, "camera_controls.align_to_reference_look"))
          .withDefault(InputBinding.key(InputConstants.KEY_BACKSPACE, KeyModifiers.of(KeyModifier.ALT)))
          .overrideCategory(KeyMappingCategories.CAMERA_CONTROLS)
          .handleWorldInput(_ -> LookHistoryManager.alignToReferenceLook(Minecraft.getInstance()))
          .build();
  ```

---

### C. Localizzazioni I18N (`it_it.json` & `en_us.json`)
- Inserimento chiavi in rigoroso ordine alfabetico:
  - `"key.minecraft_access.camera_controls.align_to_reference_look"`: `"Allinea a rotta di riferimento"` / `"Align to reference look"`
  - `"minecraft_access.camera_controls.aligned_to_reference_look"`: `"Orientato a rotta di riferimento: %s"` / `"Aligned to reference look: %s"`
  - `"minecraft_access.camera_controls.no_reference_look"`: `"Nessuna rotta di riferimento fissata"` / `"No reference look set"`

---

## 🧪 3. Piano di Verifica e Collaudo

### Test Unitari Automatizzati
- Aggiornamento di `LookHistoryManagerTest.java`:
  - Test persistenza del Livello 2 anche a seguito di rotazioni manuali e salvataggi dinamici del Livello 1.
  - Test ripristino Livello 1 post-allineamento a Livello 2.
  - Test presenza chiavi I18N in italiano ed inglese.

### Collaudo Manuale In-Game
1. **Fissaggio Bussola**: Posizionarsi verso Nord-Est (45°), premere `Ctrl + Backspace`.
2. **Esplorazione Libera**: Camminare, girarsi a scatti con `L` o `J`, puntare un albero con `End`, usare `Backspace` per verificare che l'undo dinamico funzioni normalmente.
3. **Richiamo Bussola**: Premere `Alt + Backspace` e verificare che la visuale torni all'istante a 45° con la vocalizzazione *"Orientato a rotta di riferimento: Nord-est, 45 gradi, Dritto"*.
4. **Ritorno Post-Richiamo**: Premere `Backspace` da solo e verificare il ritorno all'orientamento precedente al richiamo della bussola.
