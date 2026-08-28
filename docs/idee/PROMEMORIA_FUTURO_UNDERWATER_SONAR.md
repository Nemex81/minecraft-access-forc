# Promemoria di Progetto Futuro: Modulo UnderwaterSonar (Sonar Subacqueo ed Ecolocalizzazione 3D)

## 1. Visione e Obiettivi del Modulo

Nella navigazione di Minecraft, mentre il modulo `ObstacleDetector` gestisce in modo eccellente il movimento pedonale vincolato alla gravità su terraferma e l'approdo a riva, l'ambiente subacqueo profondo introduce una fisica completamente diversa basata su 6 gradi di libertà nello spazio 3D volumetrico.

L'obiettivo del futuro modulo **`UnderwaterSonar`** sarà quello di fornire al giocatore non vedente una suite completa di percezione acustica tridimensionale durante il nuoto e le immersioni.

---

## 2. Specifiche Tecniche e Funzionalità Previste

### 2.1 Ecolocalizzazione Sonora 3D Continua (Ping Sonar)
- **Frequenza e Pitch**:
  - Tono acuto direzionato in alto: segnala la presenza di scogli affioranti, barche o soffitti di caverne allagate.
  - Tono grave direzionato in basso: segnala la vicinanza al fondale marino (sabbia, ghiaia, blocchi di magma, sabbia delle anime).
  - Tono centrale stereo panoramico: segnala relitti sommersi (*shipwrecks*), monumenti oceanici o pareti rocciose frontali.
- **Intervallo di Ping Proporzionale**: La frequenza dei battiti acustici aumenta proporzionalmente all'avvicinarsi a un ostacolo (stile radar/sonar).

### 2.2 Bussola di Superficie e Gestione Ossigeno
- **Guida Verso la Superficie**: Comando rapido o segnale continuo che indica la direzione per risalire verso l'aria aperta prima che finisca l'ossigeno.
- **Avviso Riserva d'Aria**: Vocalizzazione e segnale sonoro ritmico quando le bolle di ossigeno scendono sotto le soglie critiche (5 bolle, 3 bolle, 1 bolla).

### 2.3 Rilevamento Colonne di Bolle
- Distinzione tra:
  - Colonne di bolle ascensionali (`soul_sand`): spinta verso l'alto e ricarica ossigeno.
  - Colonne di bolle discendenti (`magma_block`): risucchio verso il fondo e danno da calore.

---

## 3. Stato del Progetto
- **Stato**: `IN CODA / PIANIFICATO PER SESSIONI FUTURE`
- **Priorità**: Alta per espansioni marittime ed esplorazioni oceaniche.
