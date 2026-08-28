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
