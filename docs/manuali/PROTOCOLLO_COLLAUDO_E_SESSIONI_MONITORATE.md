# Protocollo Operativo: Collaudo Manuale In-Game & Sessioni Monitorate

Questo manuale stabilisce le linee guida operative per l'esecuzione della **Fase 2 (Collaudo Manuale In-Game)** del ciclo di sviluppo di `minecraft-access`, come sancito nella Regola 8 di `GEMINI.md` e nella scheda `knowledge/00_consuetudini_operative_e_sinergia_assistente.md`.

L'obiettivo è testare, diagnosticare e validare le build modificate direttamente in ambiente live di gioco con screen reader NVDA e audio 3D, garantendo l'assoluta stabilità e usabilità prima del rilascio definitivo.

---

## 1. Ruoli e Sinergia Operativa in Sessione

Il collaudo in-game è un'attività coordinata a due vie:

### Ruolo di Luca (Sviluppatore & Collaudatore Esperto)
- Esegue i movimenti, i percorsi e le azioni in Minecraft (Survival / Creative).
- Valuta la reattività dei comandi da tastiera e l'ergonomia dei tasti rapidi (Numpad, `C`, `È`, `U`, `X`, `V`).
- Verifica l'accessibilità vocale (chiarezza dei messaggi dello Screen Reader, assenza di sovrapposizioni o troncamenti vocali).
- Testa il feedback acustico posizionale 3D e il volume dei suoni di navigazione.

### Ruolo di Antigravity (Senior AI Pair Programmer & Diagnostico Live)
- Monitora in tempo reale il file di log del client:
  `C:\Users\nemex\AppData\Roaming\PrismLauncher\instances\Minecraft 26.2 Access 1.12.0\minecraft\logs\latest.log`
- Isola eventuali eccezioni silenti, avvisi Mixin o conflitti di binding.
- Verifica la corretta esecuzione dei vettori di raycast voxel e dei nodi del pathfinder.
- Fornisce assistenza e spiegazioni immediate su coordinate, altezze o comportamenti anomali del mondo.

---

## 2. Checklist di Collaudo Modulo per Modulo

Durante la sessione di test, verificare sistematicamente i seguenti moduli:

### A. Navigazione & AutoWalk (`features/autowalk/`)
- [ ] **Rotazione Continua**: La visuale vira fluidamente verso la direzione del target ($20^\circ$/tick) senza oscillazioni o scatti bruschi.
- [ ] **Isteresi Anti-Chattering**: Nessun tremolio FOV o continuo attacca-stacca dello sprint durante e dopo le curve.
- [ ] **Arrivo a Destinazione**: Riproduzione nitida del suono di arrivo (campana `NOTE_BLOCK_BELL` a volume `0.8f`) e pronuncia vocale completa *"Arrivato a destinazione: [Nome]"* senza interruzioni premature da parte del crosshair o dell'obstacle detector.
- [ ] **Sicurezza Vuoti**: Arresto immediato della camminata prima di precipitare in burroni, scarpate o pozze di lava.

### B. Waypoints & Punti di Interesse (`features/point_of_interest/waypoints/`)
- [ ] **Salvataggio Rapido**: Apertura schermo `SaveWaypointScreen`, digitazione nome e salvataggio senza blocchi.
- [ ] **Gestione Lista**: Apertura `ManageWaypointsScreen`, navigazione a celle tra i waypoint, visualizzazione distanza in metri e coordinate XYZ.
- [ ] **Puntamento & Locking**: Puntamento con tasto dedicato e riproduzione audio posizionale.
- [ ] **Persistenza File**: Salvataggio coerente in `minecraft/config/minecraft-access/waypoints/singleplayer_<mondo>.json`.

### C. Controlli Tastierino Numerico (`features/NumpadControls.java` - Zero Shift)
- [ ] **Sfera Visiva Layer 0**: Numpad `8`, `2`, `4`, `6` e diagonali `7`, `9`, `1`, `3` per rotazione visuale a tocchi e continua con bussola acustica.
- [ ] **Centratura Orizzonte & Stato Layer 0**: Tasto `5` (orizzonte piatto con rintocco sonoro e mirino) e tasto `.` (lettura istantanea Salute e Fame).
- [ ] **Azioni Layer 0**: Tasto `0` (Attacco/Scavo sinistro) e tasto `Invio` (Uso/Piazzamento/Cibo destro).
- [ ] **Bussola & Radar Layer 1 (`Ctrl + Numpad`)**: `Ctrl+8,6,2,4` per snap cardinali, `Ctrl+5` per coordinate assolute, `Ctrl+Enter` per puntare POI.
- [ ] **Diagnostica & Mobilità Layer 2 (`Alt + Numpad`)**: `Alt+8/2` per mano principale/secondaria, `Alt+6` durabilità, `Alt+1/3` Nadir/Zenith, `Alt+0` Auto-Walk.

### D. Rilevatore Ostacoli & Cadute (`features/ObstacleDetector.java` & `FallDetector.java`)
- [ ] **Auto-Step Silenzioso**: Nessun falso avviso ostacolo quando si cammina su sentieri battuti, lastre o gradini ($\Delta Y \le 0.60\text{ m}$).
- [ ] **Dislivelli Saltabili**: Annuncio vocale/acustico corretto per salti da $0.60 < \Delta Y \le 1.20\text{ m}$.
- [ ] **Muri e Barriere**: Segnalazione tempestiva per ostacoli $\Delta Y > 1.20\text{ m}$.
- [ ] **Corner Pinching**: Arresto del passaggio e segnalazione corretta nelle diagonali a $45^\circ$ se uno dei lati ortogonali è ostruito.

### E. Controlli Inventario & Ricettario (`features/inventory_controls/` & `mixin/RecipeButtonMixin.java`)
- [ ] **Navigazione a Gruppi**: Tasto `C` e `Shift+C` per passare tra Barra Rapida, Inventario, Griglia Crafting e Output.
- [ ] **Spostamento Rapido**: `Shift+È` per trasferire istantaneamente stack di oggetti verso casse o fornaci.
- [ ] **Info Ricetta & Focus Lock**: Tasto `X` per leggere ingredienti e disponibilità senza perdere il focus della cella del ricettario o bloccare la navigazione.

---

## 3. Diagnostica e Analisi Live dai Log (`latest.log`)

Quando si riscontra un comportamento anomalo:
1. **Eventi di Narrazione**: Isolare le righe `[Render thread/INFO]: Narrating(interrupt:...)` per verificare cosa viene inviato a Tolk/NVDA e se ci sono chiamate concorrenti che provocano troncamenti.
2. **Conflitti di Keybinding**: Verificare in `options.txt` che nessun tasto sia mappato su `key.keyboard.unknown` o in conflitto con comandi vanilla.
3. **Avvisi di Mixin & Injection**: Controllare che non vi siano avvisi di injection mancata o mismatch di descrittori bytecode su Minecraft 26.2.

---

## 4. Criteri di Certificazione Finale

Il collaudo si considera **ufficialmente superato** quando:
1. Tutte le funzionalità del piano tecnico sono state testate con successo in-game.
2. Zero crash, zero eccezioni non gestite nei log e zero conflitti sonori.
3. Luca conferma formalmente l'esito positivo del test.

Al superamento, Antigravity esegue la **Fase 3 (Aggiornamento Backup JAR + Archiviazione Piano)** e attiva immediatamente la **Fase 4 (Auto-Apprendimento Automatico)**.