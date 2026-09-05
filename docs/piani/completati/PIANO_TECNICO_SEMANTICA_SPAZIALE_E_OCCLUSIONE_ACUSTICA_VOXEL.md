# Piano Tecnico [COMPLETATO]: Ottimizzazione Semantica delle Indicazioni Spaziali & Sistema di Occlusione Acustica Voxel a 5 Livelli
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

- [x] **🎯 1. OBIETTIVI RAGGIUNTI** [CONVALIDATO CON SUCCESSO]
- [x] **🧪 2. VERIFICA & COLLAUDO IN-GAME** [CONVALIDATO CON SUCCESSO]

---
## 🎯 1. OBIETTIVI RAGGIUNTI

1. **Riformulazione Semantica Vettoriale Naturale**:
   - Risolte le formule telegrafiche e frammentate in lingua italiana (`it_it.json`):
     - `position_difference_away`: `"%s blocchi avanti"`
     - `position_difference_behind`: `"%s blocchi indietro"`
     - `position_difference_down`: `"%s blocchi in basso"`
     - `position_difference_left`: `"%s blocchi a sinistra"`
     - `position_difference_right`: `"%s blocchi a destra"`
     - `position_difference_up`: `"%s blocchi in alto"`
2. **Sistema di Occlusione Acustica Voxel a 5 Livelli (`AcousticOcclusion.java`)**:
   - Implementato raycast 3D ad alte prestazioni tra `playerEyes` e `targetPos` con scala di densità:
     - *Livello 1 (Porte, Botole, Lastre, Staccionate, Vetri, Foglie)*: $-10\%$ (`0.10f`)
     - *Livello 2 (Assi / Planks, Scale, Casse, Terra, Lana)*: $-18\%$ (`0.18f`)
     - *Livello 3 (Tronchi massicci, Ceppi / Logs)*: $-28\%$ (`0.28f`)
     - *Livello 4 (Pietra, Mattoni, Cobblestone, Rame, Ferro)*: $-38\%$ (`0.38f`)
     - *Livello 5 (Deepslate, Ossidiana, Bedrock)*: $-50\%$ (`0.50f`)
   - **Soglia Minima Garantita (Floor)**: **$1\%$ (`0.01f`)**, garantendo una dinamica acustica del $99\%$ senza mai perdere il tracciamento continuo in cuffia.
3. **Qualifica Semantica Vocale ("Oltre Parete")**:
   - Quando `totalOcclusion >= 20%`, la narrazione vocale (tasto `Home` e mirino) aggiunge automaticamente ` (oltre parete)`.
4. **Configurazione GUI (Cloth Config)**:
   - Opzione `wallOcclusionFeedback` in `Config.POI`: `SOUND_AND_VOICE` (default), `SOUND_ONLY`, `VOICE_ONLY`, `OFF`.

---

## 🧪 2. VERIFICA & COLLAUDO IN-GAME

- **Test Automatici**: `AcousticOcclusionTest.java` superato al $100\%$ con OpenJDK 25.0.1 LTS (`BUILD SUCCESSFUL`).
- **Collaudo In-Game**: Verificato in "scuola di sopravvivenza mondo 2":
  `[18:44:05] Narrating: Lama di mercante (oltre parete) 24 blocchi indietro  2 blocchi in basso  16 blocchi a sinistra`
- **Deploy Eseguito**:
  - `Minecraft 26.2 Access - Server Tenuta` ✅
  - `Minecraft 26.2 Access 1.12.0` ✅
- **Backup OneDrive Post-Convalida**: Sincronizzato con successo per PC Portatile e PC Fisso Salotto.
