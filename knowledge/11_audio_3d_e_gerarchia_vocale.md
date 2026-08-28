# 11 — Audio Posizionale 3D, Gerarchia Vocale & Anti-Sovrapposizione

Questa scheda stabilisce gli standard acustici e di sintesi vocale per `minecraft-access`, garantendo che l'interfaccia sonora e lo Screen Reader (NVDA / SAPI tramite Tolk) funzionino in perfetta armonia senza troncamenti vocali o affaticamento uditivo per il giocatore non vedente.

---

## 1. Gerarchia di Priorità della Sintesi Vocale (`MainClass.narrate`)

Per evitare che eventi concorrenti tronchino prematuramente messaggi vocali critici, ogni chiamata di narrazione deve rispettare questa gerarchia a 3 livelli:

### Livello 1: Pericolo Critico & Sicurezza Immediata (`interrupt: true`)
- **Eventi**: Baratri imprevisti (`FallDetector`), pozze di lava, ricezione danno vitale, ostacoli insormontabili ad alta velocità.
- **Comportamento**: Interrompe immediatamente qualsiasi narrazione in corso per trasmettere l'allarme vitale.

### Livello 2: Navigazione, GUI & Azioni Guidate (`interrupt: false` / Accodamento)
- **Eventi**: Notifica di arrivo a destinazione (`"Arrivato a destinazione: [Nome]"`), apertura schermate (Waypoints, Inventario, Ricettario), conferma di salvataggio o crafting.
- **Comportamento**: Viene accodato o protetto: il crosshair e i detector ambientali NON devono interrompere questo messaggio prima del suo completamento.

### Livello 3: Esplorazione Spontanea & Crosshair (`debounced`)
- **Eventi**: Puntatore del mouse/sguardo su blocchi ordinari, aria o entità passive distanti.
- **Comportamento**: Narrazione a bassa priorità con debouncing (almeno 150-300ms) per evitare lo spam di parole quando si ruota rapidamente la testa.

---

## 2. Standard di Volume & Protezione Acustica (Anti-Sovrastamento)

La sintesi vocale di NVDA è il canale informativo primario. I suoni sintetizzati o posizionali generati dalla mod non devono mai coprire la voce:

1. **Tetto Massimo Decibel / Volume**:
   - I suoni personalizzati della mod (es. campanella di arrivo `NOTE_BLOCK_BELL`, sonar di dislivello, click di navigazione) devono avere un volume massimo di **`0.7f - 0.8f`** (default consigliato: `0.6f`).
2. **Spazializzazione 3D Posizionale**:
   - I suoni di puntamento verso POI o entità devono sfruttare il motore audio 3D di Minecraft (OpenAL) per trasmettere azimut (stereo sinistro/destro), elevazione e distanza (attenuazione logaritmica).

---

## 3. Debouncing Sonoro, Pitch Shifting & Anti-Affaticamento

1. **Anti-Mitragliatrice Sonora**:
   - Qualsiasi segnale acustico ripetuto (es. sonar di prossimità o battito cardiaco di pericolo) deve avere un intervallo di isteresi temporale (minimo 250ms tra un impulso e il successivo).
2. **Codifica Logica del Pitch (Altezza Tono)**:
   - **Salita / Quota Superiore**: Pitch acuto ($> 1.0\text{f}$, es. $1.2\text{f} - 1.5\text{f}$) per dislivelli positivi o salti.
   - **Discesa / Quota Inferiore**: Pitch grave ($< 1.0\text{f}$, es. $0.5\text{f} - 0.8\text{f}$) per buche, gradini a scendere o discese.
   - **Conferma / Successo**: Tono armonico puro ($1.0\text{f}$).
   - **Errore / Blocco**: Tono grave e secco ($0.5\text{f}$).