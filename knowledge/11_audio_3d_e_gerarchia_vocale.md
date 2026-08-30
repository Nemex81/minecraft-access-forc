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

---

## 4. Architettura Narration Shield (Finestra Protetta per Notifiche Salienti)

Per garantire che eventi salienti non vengano troncati dal campionamento continuo del mirino (`NarrateCrosshair`) o dagli allarmi ostacoli (`ObstacleDetector`):

1. **Finestra Protetta (`NarrationPriority`)**:
   - All'emissione di una notifica saliente (raccolta oggetto, sblocco ricette, avanzamenti, arrivo a destinazione), si attiva una finestra di soppressione temporanea di **$1500\text{ ms}$** per i sensori ambientali passivi.
2. **Zittimento del Sottofondo & Accodamento Protetto**:
   - Il primo evento saliente zittisce il mirino pregresso (`interrupt: true`).
   - Eventi salienti concorrenti generati nello stesso tick o durante lo Shield (es. *Oggetto Raccolto* + *Nuova Ricetta Sbloccata*) vengono inviati con `interrupt: false` (accodati), estendendo la durata dello Shield. In questo modo NVDA pronuncia entrambe le frasi per intero in sequenza.

---

## 5. Principio di Separazione Acustico/Vocale nei Moti Rapidi (Bussola Acustica Tattile)

Durante movimenti o rotazioni continue ad alta velocità (es. rotazione della visuale a $90^\circ/\text{s}$):

1. **Divieto di Troncamento Vocale ad Alta Frequenza**:
   - Una parola parlata richiede $600\text{--}800\text{ ms}$ per essere completata; l'attraversamento di settori a $45^\circ$ avviene ogni $400\text{ ms}$. È **vietato inviare notifiche vocali continue ad ogni settore**, poiché genererebbero troncamenti e sillabe smozzicate.
2. **Bussola Acustica Tattile**:
   - Il moto continuo in tempo reale è affidato a un **click acustico discreto** (`NOTE_BLOCK_HAT` a volume $0.35\text{f}$ su `SoundSource.PLAYERS`) con pitch differenziato:
     - **Punti Cardinali Principali (Nord, Est, Sud, Ovest)**: Pitch alto $1.2\text{f}$ (*TOCK*).
     - **Punti Diagonali Intermedi (NE, NO, SE, SO)**: Pitch morbido $0.9\text{f}$ (*tick*).
3. **Annuncio Vocale Finale all'Arresto**:
   - All'arresto del movimento (rilascio del tasto), il sistema vocalizza immediatamente e con priorità la direzione finale esatta raggiunta, prima che il mirino riprenda la scansione ordinaria.

---

## 6. Regola Geometrica di Manipolazione Monoassiale della Visuale

Quando si modifica un singolo asse della visuale (es. azzeramento Pitch all'orizzonte o puntamento Zenith/Nadir a $\pm 90^\circ$):
- **Divieto Assoluto di `player.lookAt` con Vettori Verticali**: `lookAt` ricalcola entrambi gli angoli introducendo la singolarità $\text{atan2}(0,0)-90^\circ = -90^\circ$ (forzando lo Yaw a Est) o forzando snap a griglia dello Yaw continuo.
- **Applicazione Diretta**: Le modifiche di pitch devono avvenire esclusivamente tramite `player.setXRot(pitchDegrees)` e `player.xRotO = pitchDegrees`.

---

## 7. Scudo Vocale Didattico per Onboarding & Tutorial (`HelpNarrator`)

Durante le spiegazioni del Mentore, le missioni dell'Accademia o i wizard di primo avvio:

1. **Finestra Protetta Temporale Dinamica**:
   - La durata dello scudo viene calcolata proporzionalmente alla lunghezza del testo:
     $$\text{Durata (ms)} = (\text{Numero Parole} \times 280\text{ ms}) + 600\text{ ms}$$
2. **Soppressione Ambientale Totale**:
   - Durante lo scudo, lo scanner continuo del mirino (`NarrateCrosshair`) e i rilevatori di ostacoli non vocali vengono completamente silenziati per evitare qualsiasi interruzione o frammentazione cognitiva.
3. **Firme Acustiche Distintive**:
   - **Suggerimento Didattico**: Rintocco armonico `NOTE_BLOCK_BELL` a volume $0.6\text{f}$.
   - **Obiettivo Raggiunto / Successo**: Rintocco percussivo di completamento `EXPERIENCE_ORB_PICKUP` a volume $0.7\text{f}$.

---

## 8. Gerarchia Sonora per Rotazione Discreta a 360° (`RotationFeedbackMode`)

Nelle modalità con feedback sonoro a ogni scatto di rotazione della visuale (tasti `4` e `6`):
- **Punti Cardinali Puri ($0^\circ, 90^\circ, 180^\circ, 270^\circ$)**: Tono alto e limpido (Pitch $1.2\text{f}$).
- **Punti Diagonali Intercardinali ($45^\circ, 135^\circ, 225^\circ, 315^\circ$)**: Tono medio (Pitch $1.0\text{f}$).
- **Angoli Intermedi ($15^\circ, 30^\circ, 60^\circ, 75^\circ\dots$)**: Tono morbido (Pitch $0.85\text{f}$).