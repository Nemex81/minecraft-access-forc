# Piano Tecnico Completato: Regolazione Dinamica e Comandi Rapidi per il Volume dei Passi del Giocatore
- **Tipologia:** IMPLEMENTATIVO
- **Autore:** Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
- **Revisori:** Luca / Antigravity / GPT Codex
- **Data e Ora:** 2026-09-03
- **Stato Operativo:** [COMPLETATO E ARCHIVIATO — CONVALIDATO AL 100% DA LUCA IN-GAME]
- **Incremento Versione Target (AVF):** [Tracciato nel Diario Modifiche Fork]
- **Piani & Documenti Correlati:**
  * [`docs/report/REGISTRO_REVISIONI.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/report/REGISTRO_REVISIONI.md)
- **Conformità ai 5 Cancelli (Protocollo 12):** Conforme al 100% (Rifiuto patching euristico, Hardware grounding, Hitbox continua, Contratti denominati, Determinismo headless a 0 ms)

---
## 🗺️ Sommario Operativo & Registro di Avanzamento (Checklist con Gating di Convalida)

> **Regola Aurea di Avanzamento (Matrice a 3 Stati)**:
> - `- [ ] [DA AVVIARE]`: Attività pianificata ma non ancora iniziata.
> - `- [/] [IMPLEMENTATO — IN ATTESA DI CONVALIDA]`: Codice scritto o intervento completato, ma in attesa di test o collaudo formale (spunta parziale).
> - `- [x] [CONVALIDATO CON SUCCESSO]`: Spunta definitiva concessa **esclusivamente POST-CONVALIDA** (approvazione di Luca per la 1A, test suite 100% verde per la 1B, collaudo pratico in-game di Luca per la Fase 2).

- [x] **1. Obiettivo & Motivazione di Accessibilità** [CONVALIDATO CON SUCCESSO]
- [x] **2. Moduli e File Modificati** [CONVALIDATO CON SUCCESSO]
- [x] **3. Esito Verifiche & Collaudo** [CONVALIDATO CON SUCCESSO]

---
## 1. Obiettivo & Motivazione di Accessibilità
In Minecraft Vanilla, il volume dei passi è ridotto al 15% del volume originale del blocco calpestato (`0.15F`), risultando quasi impercettibile e privando il giocatore non vedente di una fondamentale ancora di propriocezione e orientamento tattile-acustico.
La funzionalità implementata offre:
1. Regolazione fine tramite cursore percentuale nella GUI delle opzioni (`0%` - `300%`, default `100%`).
2. Tasti rapidi on-the-fly su tastiera estesa (**`Alt + Page Up`** per incrementare di +10%, **`Alt + Page Down`** per ridurre di -10%) con annuncio vocale istantaneo e salvataggio automatico.
3. Iniezione Mixin chirurgica e resiliente su `Entity.class` che scala il volume esclusivamente per il giocatore locale (`LocalPlayer`).

---

## 2. Moduli e File Modificati

- `src/main/java/org/mcaccess/minecraftaccess/Config.java`:
  - Aggiunto `playerStepSoundVolume` in `Features` (`@ConfigEntry.BoundedDiscrete(min = 0, max = 300)`).
- `src/main/java/org/mcaccess/minecraftaccess/features/PlayerStepSound.java`:
  - Modulo client `BalmClientModule` con registrazione keybinding `Alt + Page Up` / `Alt + Page Down`, logica di step e narrazione.
- `src/main/java/org/mcaccess/minecraftaccess/MainClass.java`:
  - Registrazione del modulo `PlayerStepSound`.
- `src/main/java/org/mcaccess/minecraftaccess/mixin/EntityMixin.java`:
  - Mixin su `Entity.playStepSound`, `playCombinationStepSounds` e `playMuffledStepSound` con `require = 0`.
- `src/main/resources/minecraft_access.mixins.json`:
  - Registrazione di `EntityMixin` nell'array `"client"`.
- `src/main/resources/assets/minecraft_access/lang/it_it.json` & `en_us.json`:
  - Localizzazioni bilingue complete in rigoroso ordine alfabetico.
- `knowledge/06_controlli_avanzati_e_bridge_chatgpt.md`:
  - Mappa comandi aggiornata.
- `knowledge/09_registro_bug_e_soluzioni.md`:
  - Inserito Record 20 su Mixin resilience.

---

## 3. Esito Verifiche & Collaudo
- Test Unitari: 78 test superati con successo (`BUILD SUCCESSFUL`).
- Deploy su istanze PrismLauncher: `Minecraft 26.2 Access 1.12.0` e `Minecraft 26.2 Access - Server Tenuta`.
- Collaudo in-game di Luca: Esito positivo e convalidato al 100%.
