# 05 — Dominio 1: Regole Voxel, Posizionamento Torce & Comandi In-Game

## 1. Principi Fondamentali di Integrità Voxel

Nelle sessioni di gioco assistite e nella generazione di strutture voxel per giocatori non vedenti, l'assistente deve garantire la massima precisione geometrica e strutturale:

1. **Integrità Strutturale delle Pareti Solide**:
   - I blocchi di parete solida (`minecraft:stone_bricks`, `minecraft:oak_planks`, `minecraft:deepslate_bricks`, ecc.) non devono **MAI** essere distrutti, svuotati o sostituiti da torce o elementi decorativi.
2. **Posizionamento Torce a Muro (`minecraft:wall_torch`)**:
   - Devono essere collocate **esclusivamente nel blocco d'ARIA** adiacente al muro portante, impostando la proprietà `facing` che indica verso quale direzione la torcia si protende:
     - **Parete Nord** a quota $Z$: torcia posizionata nell'aria a $Z+1$ con blocco `minecraft:wall_torch[facing=south]`.
     - **Parete Sud** a quota $Z$: torcia posizionata nell'aria a $Z-1$ con blocco `minecraft:wall_torch[facing=north]`.
     - **Parete Ovest** a quota $X$: torcia posizionata nell'aria a $X+1$ con blocco `minecraft:wall_torch[facing=east]`.
     - **Parete Est** a quota $X$: torcia posizionata nell'aria a $X-1$ con blocco `minecraft:wall_torch[facing=west]`.
3. **Posizionamento Torce Ritte / Poggiate (`minecraft:torch`)**:
   - Vanno collocate a quota $Y+1$ **appoggiate sopra** un blocco solido, una staccionata (`oak_fence`) o un muretto (`stone_brick_wall`) a quota $Y$.
   - Verificare sempre che il blocco di supporto sottostante sia stato effettivamente posato prima della torcia.

---

## 2. Regole di Generazione Comandi In-Game (Anti-Ridondanza)

Durante le sessioni di gioco guidate, l'assistente prepara lotti di comandi Minecraft per Luca da incollare in chat. Per evitare falsi allarmi dello screen reader, applicare rigorosamente queste regole:

1. **Divieto di Comandi Ridondanti su Blocchi Già Posati**:
   - Se si esegue `/setblock X Y Z <blocco>` o `/fill` su coordinate dove è **già presente lo stesso identico blocco e blockstate**, il motore di Minecraft restituisce il messaggio ingannevole: `"Impossibile posizionare il blocco"`.
   - Questo messaggio genera confusione per un utente non vedente. L'assistente deve analizzare le dipendenze logiche e **omettere i comandi per blocchi già posati** da precedenti comandi `/fill` o `/setblock`.
2. **Posa di Punti Luce su Pilastri / Recinzioni**:
   - Se le recinzioni o i pilastri a quota $Y$ sono già stati generati da un comando `/fill` di perimetro, emettere **esclusivamente** il comando per la torcia o lanterna a quota $Y+1$ (es. `/setblock X 66 Z torch`), senza riemettere il comando ridondante per la quota $Y$.

---

## 3. Verifica Voxel Continua via NBT

- **Scansione File Regione (.mca)**: Analisi periodica della struttura NBT per verificare assenza di buchi nei pavimenti, corretta illuminazione, continuità dei percorsi di camminamento e assenza di dislivelli pericolosi senza ringhiera.

---

## 4. Regole Geometriche Voxel per Raycast, Look-Ahead & Rilevamento Pericoli

Per prevenire qualsiasi falso allarme vocale e garantire la massima precisione nella mobilità assistita di Minecraft Access:

1. **Corner Pinching Voxel nei Passi Diagonali ($45^\circ$)**:
   - Nelle transizioni diagonali $(\Delta X \neq 0, \Delta Z \neq 0)$, se uno dei due blocchi ortogonali intermedi $(X+\Delta X, Z)$ o $(X, Z+\Delta Z)$ è una barriera/muro, la hitbox fisica del giocatore ($0.6\text{ m}$) è bloccata.
   - Il raggio deve arrestarsi istantaneamente per impedire infiltrazioni attraverso i vertici di contatto a zero spessore tra pareti e finestre adiacenti.
2. **Riconoscimento Strutturale Verticale delle Scale (Scale & Sottoscala)**:
   - La misura di profondità di una colonna verticale d'aria non deve valutare solo il blocco sul fondo: se il blocco d'atterraggio è uno `StairBlock`/`SlabBlock` **oppure** se lungo la colonna verticale tra atterraggio e quota piedi sono presenti i gradini sovrastanti della scala, l'intera campata della rampa è considerata sicura (`drop = 0`).
3. **Arresto Immediato su Davanzali / Ostacoli Solidi ($\ge 1.0\text{ m}$)**:
   - Nei raycast di avanzamento, un blocco solido a quota piedi di altezza $\ge 1.0\text{ m}$ (o con ostacolo/vetro a quota testa) arresta istantaneamente il raggio (`break;`), impedendo lo scavalcamento errato del davanzale verso il vuoto esterno.
4. **Modello Voxel a 4 Pilastri per Elementi a Parete (`LadderBlock`)**:
   - Se un blocco a parete possiede una bounding box di collisione fisica parziale (es. i 3 pixel di spessore di una scala a pioli o un cartello):
     1. `isPassable = true`: la hitbox del giocatore ($0.60\text{ m}$) transita liberamente nei restanti $0.8125\text{ m}$ di spazio utile;
     2. `isClearHeadroom = true`: la testa del giocatore non subisce collisione dall'ingombro a parete;
     3. `isStandable = false`: divieto categorico di appoggio sui pioli nel vuoto (previene cadute orizzontali e salite improprie verso botole e tetti);
     4. `isSolid = false`: trasparenza per i raggi di discesa verticale e controlli di linea visiva.
5. **Clearance Volumetrica a Quota Occhi nel Rilevamento Cadute (`FallDetector`)**:
   - Nel presidio del ciglio (`isStandingOnDangerousEdge`) e nel look-ahead (`findDangerAhead`), non limitare la valutazione al solo piano dei piedi ($Y$ o $Y-1$);
   - Verificare sempre che la cella ad altezza occhi/testa ($Y+1$, `stepPos.above()`) sia libera;
   - Se la cella superiore è ostruita da blocchi solidi, soffitti bassi, muri o barriere, la caduta è fisicamente impossibile per la statura del giocatore ($1.80\text{ m}$) e la cella viene scartata a monte, azzerando i falsi allarmi nei vani scale, corridoi chiusi e passaggi bassi.

---

## 5. Sonda di Percorso: Inseguimento Suolo, Scansione Continua e Rilevamento Colture Non-Solide
1. **Tracciamento Continuo del Terreno Calpestabile**:
   - A ogni passo $d$, campionare il blocco solido di pavimento a quota $Y$ per identificare sempre il materiale (Erba, Pietra, Legno, Terra zappata).
2. **Campionamento Blocchi Pianta a Quota Piedi**:
   - Poiché le colture (`CropBlock`) hanno collisione vuota per consentire il passaggio, non devono essere trattate come aria vuota ma rilevate come risorsa raccoglibile.
3. **Scansione a Lungo Raggio per Modalità DETAILED**:
   - In modalità dettagliata, la scansione non si arresta al primo ostacolo (`break;`), ma prosegue fino al limite `scanRange` descrivendo la successione completa di segmenti, ostacoli, dislivelli e risorse.

---

## 6. Architettura a Due Zone per la Sicurezza Anticaduta, Auto-Sneak Vettoriale & Eccezioni di Discesa

1. **Il Principio di Disaccoppiamento tra Pre-Allerta e Barriera Posturale**:
   - Nei sistemi anticaduta voxel predittivi, non applicare mai l'accovacciamento fisico (`sneak`) alla stessa distanza dell'avviso di pre-allerta.
   - **Zona 1 — Pre-Allerta & Rallentamento Corsa ($0.85\text{ m} < d \le \text{slowdownDistance}$)**:
     - Se il baratro è a 2-3 blocchi di distanza, interrompe lo sprint (`autoSlowdown`) e riproduce l'avviso vocale/sonoro preventivo.
     - L'auto-sneak rimane disattivato: la marcia procede alla velocità naturale senza costringere a terra il giocatore.
   - **Zona 2 — Bordo Fisico Immediato / Ciglio ($d \le 0.85\text{ m}$)**:
     - Attivazione dell'Auto-Sneak forzato (`autoSneakActive = true`) solo sull'ultimo passo prima del baratro.
     - La fisica nativa di Minecraft blocca fisicamente l'uscita dalla piattaforma, rendendo impossibile la caduta accidentale anche tenendo premuto `W`.
2. **Calcolo Vettoriale dell'Intenzione di Movimento (`W`, `S`, `A`, `D`)**:
   - Determinare la direzione di avanzamento reale dai tasti premuti (`W` = $0^\circ$, `S` = $180^\circ$, `A` = $-90^\circ$, `D` = $+90^\circ$, diagonali $\pm 45^\circ / \pm 135^\circ$).
   - Se il giocatore preme `S` per indietreggiare verso il terreno solido, il pericolo svanisce all'istante: l'auto-sneak viene rilasciato immediatamente (`handleDangerCleared`) consentendo la retromarcia fluida senza impuntamenti.
   - A velocità nulla ($v \approx 0$) e senza tasti premuti, azzerare il pericolo per non bloccare posture da fermo.
3. **Matrice delle Eccezioni di Discesa Verticale Sicura**:
   - I blocchi arrampicabili (`BlockTags.CLIMBABLE`, `LadderBlock`, `VineBlock`, `ScaffoldingBlock`) e gli smorzatori di caduta (Acqua, `Blocks.COBWEB`, Fieno, Miele, Slime, Neve polverosa) non hanno collisione piena ma rappresentano vie di discesa intenzionali.
   - Eseguire una scansione discendente lungo la colonna verticale: se gli elementi arrampicabili o i fluidi collegano la piattaforma al suolo (o con salto finale $\le 3$ blocchi), il dislivello è valutato sicuro (`depth = 0`), permettendo di scendere liberamente da tetti e scale a pioli.
4. **Feedback di Barriera Attiva sul Ciglio (Edge Bump)**:
   - Se l'utente insiste premendo `W` contro il vuoto mentre è protetto dall'auto-sneak, non sopprimere l'output vocale: emettere il rintocco sonoro 3D posizionale e l'avviso vocale dedicato (*"Sul ciglio: burrone avanti, N blocchi"*) con un debouncing temporale a 30 tick ($1.5\text{ secondi}$), configurabile tramite l'Enum `EdgeBumpFeedbackMode`.
5. **Presidio Fisico del Ciglio da Fermo (Sticky Sneak on Edge)**:
   - Quando il giocatore rilascia i tasti di movimento (`moveDir == null`), una scansione radiale a 8 punti perimetrali attorno alla hitbox (raggio $0.45\text{ m} - 0.70\text{ m}$) verifica se i piedi confinano direttamente con un dislivello pericoloso.
   - *Se* il pericolo persiste, *allora* mantiene forzatamente `autoSneakActive = true`: il personaggio rimane accovacciato da fermo sul bordo impedendo che micro-tap di ripartenza o scivolamenti lo facciano precipitare.
   - Il disimpegno e ritorno in piedi avvengono istantaneamente non appena il giocatore indietreggia con `S` o cammina verso terreno solido.
6. **Neutralizzazione Mixin del Vettore di Salto (`LivingEntity.jumpFromGround`)**:
   - Poiché la fisica di blocco del ciglio di Minecraft opera **esclusivamente quando i piedi toccano terra (`onGround == true`)**, la pressione di `Spazio` sul bordo annullava la protezione permettendo al personaggio di veleggiare nel vuoto.
   - L'iniezione Mixin all'ingresso di `LivingEntity.jumpFromGround()` annulla l'impulso fisico (`ci.cancel()`) quando `FallDetector.isAutoSneakActive()` è attivo, rendendo il distacco da terra fisicamente impossibile sul ciglio.
7. **Toggle Rapido Bimodale in Tempo Reale (`Ctrl + Alt + F`)**:
   - Commutazione istantanea dello scudo senza accedere alla GUI:
     - *Disattivazione (`OFF`)*: Rilascia all'istante l'accovacciamento e sblocca il salto per consentire tuffi e balzi volontari.
     - *Riattivazione (`ON`)*: Se il giocatore si trova fermo sul ciglio, esegue il check radiale e lo riaccovaccia all'istante nello stesso tick.

---

## 7. Geometria Voxel della Hitbox Giocatore per il Salto Automatico del Navigatore

1. **Il Paradosso della Distanza Euclidea dal Centro Voxel**:
   - I blocchi voxel di Minecraft hanno dimensione $1.0 \times 1.0\text{ m}$ con centro a $(X+0.5, Z+0.5)$.
   - La hitbox del giocatore ha larghezza $0.6\text{ m}$ (raggio $0.3\text{ m}$).
   - Quando il giocatore tocca la parete frontale di un gradino solido, la fisica di Minecraft lo arresta a una distanza orizzontale minima pari a:
     $$d_{\text{min}} = 0.5\text{ (parete blocco)} + 0.3\text{ (hitbox giocatore)} = 0.80\text{ metri (fino a 1.0 m su approcci diagonali)}$$
2. **Regola di Calibrazione del Salto Automatico (`AutoWalkController`)**:
   - **Divieto di Soglie Sottodimensionate**: Non esigere mai $\text{distH} < 0.65\text{ m}$ per azionare il salto, poiché esigerebbe una compenetrazione fisica impossibile dentro il solido prima di premere Spazio.
   - **Finestra di Approccio Naturale**: Attivare il salto con dislivello $0.30 < \Delta Y \le 1.25$ quando $\text{distH} \le 1.25\text{ m}$ oppure in presenza di collisione fisica (`player.horizontalCollision == true`) con appoggio al suolo (`onGround == true`).
   - **Spinta Verticale Stabile**: Mantenere la pressione di `keyJump` per 4 tick ($200\text{ ms}$) per garantire l'impulso completo e l'atterraggio a quota $+1$.

---

## 8. Micro-Voxel Raymarch per Lamine Sottili & Armonizzazione Orizzontale $XZ$ (Rev MC-29.5 - MC-29.6)

1. **Il Paradosso del Pavimento a Coordinate Negative per Lamine Sottili**:
# 05 — Dominio 1: Regole Voxel, Posizionamento Torce & Comandi In-Game

## 1. Principi Fondamentali di Integrità Voxel

Nelle sessioni di gioco assistite e nella generazione di strutture voxel per giocatori non vedenti, l'assistente deve garantire la massima precisione geometrica e strutturale:

1. **Integrità Strutturale delle Pareti Solide**:
   - I blocchi di parete solida (`minecraft:stone_bricks`, `minecraft:oak_planks`, `minecraft:deepslate_bricks`, ecc.) non devono **MAI** essere distrutti, svuotati o sostituiti da torce o elementi decorativi.
2. **Posizionamento Torce a Muro (`minecraft:wall_torch`)**:
   - Devono essere collocate **esclusivamente nel blocco d'ARIA** adiacente al muro portante, impostando la proprietà `facing` che indica verso quale direzione la torcia si protende:
     - **Parete Nord** a quota $Z$: torcia posizionata nell'aria a $Z+1$ con blocco `minecraft:wall_torch[facing=south]`.
     - **Parete Sud** a quota $Z$: torcia posizionata nell'aria a $Z-1$ con blocco `minecraft:wall_torch[facing=north]`.
     - **Parete Ovest** a quota $X$: torcia posizionata nell'aria a $X+1$ con blocco `minecraft:wall_torch[facing=east]`.
     - **Parete Est** a quota $X$: torcia posizionata nell'aria a $X-1$ con blocco `minecraft:wall_torch[facing=west]`.
3. **Posizionamento Torce Ritte / Poggiate (`minecraft:torch`)**:
   - Vanno collocate a quota $Y+1$ **appoggiate sopra** un blocco solido, una staccionata (`oak_fence`) o un muretto (`stone_brick_wall`) a quota $Y$.
   - Verificare sempre che il blocco di supporto sottostante sia stato effettivamente posato prima della torcia.

---

## 2. Regole di Generazione Comandi In-Game (Anti-Ridondanza)

Durante le sessioni di gioco guidate, l'assistente prepara lotti di comandi Minecraft per Luca da incollare in chat. Per evitare falsi allarmi dello screen reader, applicare rigorosamente queste regole:

1. **Divieto di Comandi Ridondanti su Blocchi Già Posati**:
   - Se si esegue `/setblock X Y Z <blocco>` o `/fill` su coordinate dove è **già presente lo stesso identico blocco e blockstate**, il motore di Minecraft restituisce il messaggio ingannevole: `"Impossibile posizionare il blocco"`.
   - Questo messaggio genera confusione per un utente non vedente. L'assistente deve analizzare le dipendenze logiche e **omettere i comandi per blocchi già posati** da precedenti comandi `/fill` o `/setblock`.
2. **Posa di Punti Luce su Pilastri / Recinzioni**:
   - Se le recinzioni o i pilastri a quota $Y$ sono già stati generati da un comando `/fill` di perimetro, emettere **esclusivamente** il comando per la torcia o lanterna a quota $Y+1$ (es. `/setblock X 66 Z torch`), senza riemettere il comando ridondante per la quota $Y$.

---

## 3. Verifica Voxel Continua via NBT

- **Scansione File Regione (.mca)**: Analisi periodica della struttura NBT per verificare assenza di buchi nei pavimenti, corretta illuminazione, continuità dei percorsi di camminamento e assenza di dislivelli pericolosi senza ringhiera.

---

## 4. Regole Geometriche Voxel per Raycast, Look-Ahead & Rilevamento Pericoli

Per prevenire qualsiasi falso allarme vocale e garantire la massima precisione nella mobilità assistita di Minecraft Access:

1. **Corner Pinching Voxel nei Passi Diagonali ($45^\circ$)**:
   - Nelle transizioni diagonali $(\Delta X \neq 0, \Delta Z \neq 0)$, se uno dei due blocchi ortogonali intermedi $(X+\Delta X, Z)$ o $(X, Z+\Delta Z)$ è una barriera/muro, la hitbox fisica del giocatore ($0.6\text{ m}$) è bloccata.
   - Il raggio deve arrestarsi istantaneamente per impedire infiltrazioni attraverso i vertici di contatto a zero spessore tra pareti e finestre adiacenti.
2. **Riconoscimento Strutturale Verticale delle Scale (Scale & Sottoscala)**:
   - La misura di profondità di una colonna verticale d'aria non deve valutare solo il blocco sul fondo: se il blocco d'atterraggio è uno `StairBlock`/`SlabBlock` **oppure** se lungo la colonna verticale tra atterraggio e quota piedi sono presenti i gradini sovrastanti della scala, l'intera campata della rampa è considerata sicura (`drop = 0`).
3. **Arresto Immediato su Davanzali / Ostacoli Solidi ($\ge 1.0\text{ m}$)**:
   - Nei raycast di avanzamento, un blocco solido a quota piedi di altezza $\ge 1.0\text{ m}$ (o con ostacolo/vetro a quota testa) arresta istantaneamente il raggio (`break;`), impedendo lo scavalcamento errato del davanzale verso il vuoto esterno.

---

## 5. Sonda di Percorso: Inseguimento Suolo, Scansione Continua e Rilevamento Colture Non-Solide
1. **Tracciamento Continuo del Terreno Calpestabile**:
   - A ogni passo $d$, campionare il blocco solido di pavimento a quota $Y$ per identificare sempre il materiale (Erba, Pietra, Legno, Terra zappata).
2. **Campionamento Blocchi Pianta a Quota Piedi**:
   - Poiché le colture (`CropBlock`) hanno collisione vuota per consentire il passaggio, non devono essere trattate come aria vuota ma rilevate come risorsa raccoglibile.
3. **Scansione a Lungo Raggio per Modalità DETAILED**:
   - In modalità dettagliata, la scansione non si arresta al primo ostacolo (`break;`), ma prosegue fino al limite `scanRange` descrivendo la successione completa di segmenti, ostacoli, dislivelli e risorse.

---

## 6. Architettura a Due Zone per la Sicurezza Anticaduta, Auto-Sneak Vettoriale & Eccezioni di Discesa

1. **Il Principio di Disaccoppiamento tra Pre-Allerta e Barriera Posturale**:
   - Nei sistemi anticaduta voxel predittivi, non applicare mai l'accovacciamento fisico (`sneak`) alla stessa distanza dell'avviso di pre-allerta.
   - **Zona 1 — Pre-Allerta & Rallentamento Corsa ($0.85\text{ m} < d \le \text{slowdownDistance}$)**:
     - Se il baratro è a 2-3 blocchi di distanza, interrompe lo sprint (`autoSlowdown`) e riproduce l'avviso vocale/sonoro preventivo.
     - L'auto-sneak rimane disattivato: la marcia procede alla velocità naturale senza costringere a terra il giocatore.
   - **Zona 2 — Bordo Fisico Immediato / Ciglio ($d \le 0.85\text{ m}$)**:
     - Attivazione dell'Auto-Sneak forzato (`autoSneakActive = true`) solo sull'ultimo passo prima del baratro.
     - La fisica nativa di Minecraft blocca fisicamente l'uscita dalla piattaforma, rendendo impossibile la caduta accidentale anche tenendo premuto `W`.
2. **Calcolo Vettoriale dell'Intenzione di Movimento (`W`, `S`, `A`, `D`)**:
   - Determinare la direzione di avanzamento reale dai tasti premuti (`W` = $0^\circ$, `S` = $180^\circ$, `A` = $-90^\circ$, `D` = $+90^\circ$, diagonali $\pm 45^\circ / \pm 135^\circ$).
   - Se il giocatore preme `S` per indietreggiare verso il terreno solido, il pericolo svanisce all'istante: l'auto-sneak viene rilasciato immediatamente (`handleDangerCleared`) consentendo la retromarcia fluida senza impuntamenti.
   - A velocità nulla ($v \approx 0$) e senza tasti premuti, azzerare il pericolo per non bloccare posture da fermo.
3. **Matrice delle Eccezioni di Discesa Verticale Sicura**:
   - I blocchi arrampicabili (`BlockTags.CLIMBABLE`, `LadderBlock`, `VineBlock`, `ScaffoldingBlock`) e gli smorzatori di caduta (Acqua, `Blocks.COBWEB`, Fieno, Miele, Slime, Neve polverosa) non hanno collisione piena ma rappresentano vie di discesa intenzionali.
   - Eseguire una scansione discendente lungo la colonna verticale: se gli elementi arrampicabili o i fluidi collegano la piattaforma al suolo (o con salto finale $\le 3$ blocchi), il dislivello è valutato sicuro (`depth = 0`), permettendo di scendere liberamente da tetti e scale a pioli.
4. **Feedback di Barriera Attiva sul Ciglio (Edge Bump)**:
   - Se l'utente insiste premendo `W` contro il vuoto mentre è protetto dall'auto-sneak, non sopprimere l'output vocale: emettere il rintocco sonoro 3D posizionale e l'avviso vocale dedicato (*"Sul ciglio: burrone avanti, N blocchi"*) con un debouncing temporale a 30 tick ($1.5\text{ secondi}$), configurabile tramite l'Enum `EdgeBumpFeedbackMode`.
5. **Presidio Fisico del Ciglio da Fermo (Sticky Sneak on Edge)**:
   - Quando il giocatore rilascia i tasti di movimento (`moveDir == null`), una scansione radiale a 8 punti perimetrali attorno alla hitbox (raggio $0.45\text{ m} - 0.70\text{ m}$) verifica se i piedi confinano direttamente con un dislivello pericoloso.
   - *Se* il pericolo persiste, *allora* mantiene forzatamente `autoSneakActive = true`: il personaggio rimane accovacciato da fermo sul bordo impedendo che micro-tap di ripartenza o scivolamenti lo facciano precipitare.
   - Il disimpegno e ritorno in piedi avvengono istantaneamente non appena il giocatore indietreggia con `S` o cammina verso terreno solido.
6. **Neutralizzazione Mixin del Vettore di Salto (`LivingEntity.jumpFromGround`)**:
   - Poiché la fisica di blocco del ciglio di Minecraft opera **esclusivamente quando i piedi toccano terra (`onGround == true`)**, la pressione di `Spazio` sul bordo annullava la protezione permettendo al personaggio di veleggiare nel vuoto.
   - L'iniezione Mixin all'ingresso di `LivingEntity.jumpFromGround()` annulla l'impulso fisico (`ci.cancel()`) quando `FallDetector.isAutoSneakActive()` è attivo, rendendo il distacco da terra fisicamente impossibile sul ciglio.
7. **Toggle Rapido Bimodale in Tempo Reale (`Ctrl + Alt + F`)**:
   - Commutazione istantanea dello scudo senza accedere alla GUI:
     - *Disattivazione (`OFF`)*: Rilascia all'istante l'accovacciamento e sblocca il salto per consentire tuffi e balzi volontari.
     - *Riattivazione (`ON`)*: Se il giocatore si trova fermo sul ciglio, esegue il check radiale e lo riaccovaccia all'istante nello stesso tick.

---

## 7. Geometria Voxel della Hitbox Giocatore per il Salto Automatico del Navigatore

1. **Il Paradosso della Distanza Euclidea dal Centro Voxel**:
   - I blocchi voxel di Minecraft hanno dimensione $1.0 \times 1.0\text{ m}$ con centro a $(X+0.5, Z+0.5)$.
   - La hitbox del giocatore ha larghezza $0.6\text{ m}$ (raggio $0.3\text{ m}$).
   - Quando il giocatore tocca la parete frontale di un gradino solido, la fisica di Minecraft lo arresta a una distanza orizzontale minima pari a:
     $$d_{\text{min}} = 0.5\text{ (parete blocco)} + 0.3\text{ (hitbox giocatore)} = 0.80\text{ metri (fino a 1.0 m su approcci diagonali)}$$
2. **Regola di Calibrazione del Salto Automatico (`AutoWalkController`)**:
   - **Divieto di Soglie Sottodimensionate**: Non esigere mai $\text{distH} < 0.65\text{ m}$ per azionare il salto, poiché esigerebbe una compenetrazione fisica impossibile dentro il solido prima di premere Spazio.
   - **Finestra di Approccio Naturale**: Attivare il salto con dislivello $0.30 < \Delta Y \le 1.25$ quando $\text{distH} \le 1.25\text{ m}$ oppure in presenza di collisione fisica (`player.horizontalCollision == true`) con appoggio al suolo (`onGround == true`).
   - **Spinta Verticale Stabile**: Mantenere la pressione di `keyJump` per 4 tick ($200\text{ ms}$) per garantire l'impulso completo e l'atterraggio a quota $+1$.

---

## 8. Micro-Voxel Raymarch per Lamine Sottili & Armonizzazione Orizzontale $XZ$ (Rev MC-29.5 - MC-29.6)

1. **Il Paradosso del Pavimento a Coordinate Negative per Lamine Sottili**:
   - A coordinate negative (es. $X = -64.8$), il giocatore si trova nel voxel $X = -65$ (`Math.floor(-64.8) = -65`).
   - Se il campionamento volumetrico parte da $d = 0.25\text{ m}$ verso Ovest, $-64.8 - 0.25 = -65.05 \implies \text{floor} = -66$, scavalcando interamente il voxel $X = -65$ e rendendo invisibili porte e pannelli di vetro a filo parete.
   - **Regola Micro-Voxel Continuo**: Il campionamento volumetrico lungo la linea di vista per lamine sottili (`DoorBlock`, `CrossCollisionBlock`, `FenceBlock`, `IronBarsBlock`, `FenceGateBlock`, `TrapDoorBlock`) deve partire da $d = 0.05\text{ m}$ con passo fisso di $0.10\text{ m}$, intercettando con precisione millimetrica ogni elemento sin dal voxel a contatto.
2. **Armonizzazione Orizzontale Colonna Unica $XZ$ (Sguardo & Piedi)**:
   - Quando ci si muove in avanti (`W`) verso una barriera, i sensori dei piedi e dello sguardo inquadrano la **stessa colonna orizzontale** $(X, Z)$ ma a quote verticali diverse ($Y_{\text{piedi}}$ vs $Y_{\text{occhi}}$).
   - **Regola Colonna Unica**: Non confrontare le coordinate 3D rigide $XYZ$ per decidere se fondere i messaggi; se $X_{\text{target}} == X_{\text{ostacolo}} \land Z_{\text{target}} == Z_{\text{ostacolo}}$ oppure se il movimento è frontale, emettere un unico annuncio compatto (*"Davanti: Ostacolo di Pannello di vetro, a 3 blocchi"*), azzerando la duplicazione ridondante *"Davanti: ... Davanti: ..."*.
3. **Podometro Ritmico di Cadenza Parete**:
   - Muovendosi lungo una parete uniforme a blocchi adiacenti (es. assi di quercia), non sopprimere l'annuncio su cambio coordinata: l'annuncio metro per metro funge da sonar di cadenza e velocità fondamentale per il non vedente.

---

## 9. Continuità Voxel dei Condotti Verticali $1 \times 1$ (Scale a Pioli & Botole di Copertura)

1. **Il Vincolo di Continuità Assoluta della Scala a Pioli**:
   - In Minecraft, l'arrampicata fisica (`isClimbing()`) opera unicamente quando la hitbox del giocatore interseca il volume di un blocco di scala (`LadderBlock`).
   - Nei condotti verticali $1 \times 1$ (es. verso tetti o torri), la sequenza di scale a pioli dal pavimento ($Y_{\text{start}}$) alla quota d'uscita ($Y_{\text{end}}$) non deve presentare **alcun blocco mancante**.
   - Se anche un solo blocco lungo la colonna è aria (es. scala a quote $79, 80, 81, 82$, aria a quota $83$ e botola a quota $84$), il personaggio si arresta alla quota dell'ultima scala, non riuscendo a spingersi attraverso il varco d'uscita.
2. **Geometria delle Botole di Copertura (`TrapDoorBlock`)**:
   - Una botola posizionata con `half: top` su un condotto verticale a filo pavimento/tetto deve essere impostata su `open: true` per liberare il passaggio.
   - Con la botola aperta, lo spazio calpestabile si apre e il modulo `FallDetector` riconosce la scala sottostante consentendo sia la salita continua sia la `Discesa sicura` assistita senza falsi allarmi di burrone.

---

## 10. Prontuario Rapido Comandi da Chat (Anti-Spreco Token & Indice Diretto)

- Per qualsiasi richiesta utente riguardante la ricerca o l'utilizzo di comandi da chat (`/give`, `/tp`, `/fill`, `/setblock`, `/time`, `/tick`, `/gamerule`, `/effect`, `/spawnpoint`), consultare primariamente l'indice dedicato:
  📁 [`docs/manuali/PRONTUARIO_COMANDI_CHAT.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/docs/manuali/PRONTUARIO_COMANDI_CHAT.md)
- Questo documento contiene le formule e i comandi pronti per il copia-incolla diretto, evitando ricerche a tappeto nel codebase e azzerando il consumo ridondante di token.
