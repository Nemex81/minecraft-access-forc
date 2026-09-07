# Idea di Progetto Futuro: Combat Assistant & Tactical Lock-On (Combattimento Accessibile a Bassa Latenza)

## 1. Visione Cognitiva & Obiettivi di Accessibilità

Nel gameplay di sopravvivenza in Minecraft, il combattimento corpo a corpo e a distanza contro entità ostili (zombie, scheletri, ragni, creeper) rappresenta la sfida a più alta intensità sensoriale e motoria:
- Un giocatore vedente percepisce istantaneamente la posizione, i movimenti laterali, la distanza d'attacco e le animazioni dei mob tramite la vista in tempo reale a 60 fps.
- Per un giocatore non vedente, affidarsi alla sola sintesi vocale (NVDA) durante un combattimento ravvicinato genera un divario insormontabile di latenza: mentre la voce pronuncia la frase che descrive la posizione del nemico, il mob ha già percorso 2 o 3 blocchi e sferrato molteplici attacchi letali.

L'obiettivo di questa idea è creare un'esperienza di combattimento equa, reattiva e gratificante, basata non sulla descrizione verbale differita, ma sulla **percezione acustica istantanea** e sul **supporto motorio coerente** con le dinamiche di gioco (ispirato ai sistemi di lock-on dei migliori giochi d'azione accessibili).

---

## 2. Il Modello Mentale del Giocatore in Combattimento

Il combattimento deve operare secondo un ciclo cognitivo fluido a tre stadi:

1. **Ingaggio Intuitivo (Lock-On)**:
   - Il giocatore rileva una minaccia tramite l'audio posizionale 3D o l'allerta di prossimità.
   - Con un singolo comando immediato, il giocatore "aggancia" il nemico più minaccioso o vicino.
   - Da quel momento, l'orientamento dello sguardo e del busto segue costantemente il bersaglio nei suoi movimenti, consentendo al giocatore di concentrarsi sul ritmo del combattimento (attacco, difesa, arretramento) senza dover riallineare manualmente la visuale a ogni secondo.

2. **Ritmo Acustico di Attacco (Hitbox & Timing)**:
   - Il giocatore riceve un segnale acustico distintivo a zero latenza nel momento esatto in cui il bersaglio entra nella portata effettiva di colpo dell'arma impugnata.
   - La combinazione tra il rintocco di ricarica dell'attacco (arma pronta al 100%) e il suono di nemico a portata di colpo crea una partitura ritmica chiara: quando entrambi i suoni sono attivi, sferrare il colpo garantisce la massima efficacia.

3. **Consapevolezza Difensiva & Parata**:
   - Gli attacchi in arrivo o la vicinanza critica del mob vengono comunicati con un preavviso acustico inequivocabile, permettendo al giocatore di sollevare tempestivamente lo scudo o arretrare.
   - In caso di mob multipli, il sistema segnala con discrezione la presenza di minacce laterali o alle spalle senza interrompere l'ingaggio corrente.

---

## 3. I 4 Pilastri Sistemici dell'Idea

### Pilastro 1 — Tactical Lock-On (Inseguimento Dinamico del Bersaglio)
- **Ingaggio Rapido a Scelta Unica**: Un'azione di comando che seleziona il nemico ostile prioritario entro un raggio di sicurezza e attiva la modalità bersaglio.
- **Centraggio Continuo Fluido**: La telecamera segue il movimento dell'entità agganciata mantenendola al centro del campo visivo, compensando salti, scatti o aggiramenti.
- **Sgancio Intenzionale & di Emergenza**: Il lock-on si disattiva istantaneamente se il nemico muore, se il giocatore decide di fuggire arretrando con decisione, oppure tramite un comando esplicito di sblocco.

### Pilastro 2 — Feedback Acustico di Portata (Hitbox Range Cue)
- **Segnale Sonoro di Raggio Utile**: Un impulso sonoro chiaro e non invasivo che scatta esclusivamente quando il mirino interseca l'area di impatto del bersaglio ed è entro la distanza massima d'attacco dell'arma impugnata (es. 3 metri per spada, mani o strumenti).
- **Conferma Sonora del Colpo a Segno**: Un feedback acustico di impatto riuscito che varia in tonalità in base alla salute residua del bersaglio, comunicando intuitivamente se il mob è prossimo alla sconfitta o ancora a piena salute.

### Pilastro 3 — Difesa Reattiva & Gestione dello Scudo
- **Segnale di Minaccia Imminente**: Un avviso acustico breve e cupo emesso nell'istante in cui un mob ravvicinato avvia l'intenzione di attacco, offrendo una finestra temporale coerente per alzare lo scudo.
- **Opzione di Auto-Guardia Contestuale**: Possibilità di mantenere automaticamente lo scudo alzato tra un fendente e l'altro quando si è in modalità lock-on, proteggendo il giocatore da attacchi a sorpresa o frecce da cecchini.

### Pilastro 4 — Igiene Vocale & Anti-Mascheramento Acustico
- Durante il combattimento attivo, i messaggi parlati lunghi vengono sospesi o ridotti al minimo essenziale (es. solo avviso di cuori critici sotto una certa soglia).
- Tutta l'informazione tattica viaggia su frequenze sonore nitide tra 0.7f e 0.8f, lasciando la concentrazione del giocatore libera dal carico verbale.

---

## 4. Coesistenza Armonica & Simmetria con i Moduli Esistenti

- **Sinergia con `LockingHandler`**: L'idea estende e potenzia la logica di lock-on già presente (usata per blocchi, waypoint e arco), creando un ramo specializzato per il combattimento corpo a corpo che non richiede la selezione preventiva dal radar POI.
- **Sinergia con `HUDStatus` e Indicatori di Gioco**: Integrazione armoniosa con l'indicatore di forza d'attacco esistente per favorire colpi ritmici al 100% di danno.
- **Sinergia con la Filosofia Zero Mouse**: Tutte le manovre di ingaggio, colpo, parata e sgancio rimangono accessibili da tastiera e tastierino numerico con singole pressioni o combinazioni naturali.

---

## 5. Stato dell'Idea

- **Stato**: `IN CODA / PRE-STRATEGIA (CONCEZIONE LOGICO-COGNITIVA)`
- **Collocazione**: Idea concettuale preliminare (Fase Pre-Strategica).
- **Prossimo Passo (quando si deciderà di affrontarla)**: Redazione della Strategia Cognitiva UPCS in `docs/strategie/attive/` e definizione delle invarianti matematiche/architetturali.
