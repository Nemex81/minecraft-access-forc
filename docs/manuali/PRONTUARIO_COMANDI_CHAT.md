# Prontuario Operativo dei Comandi da Chat di Minecraft (Java 26.2)

**Autore:** Antigravity & Luca  
**Target:** Sviluppatore e Giocatore Non Vedente con Screen Reader NVDA  
**Contesto:** Minecraft Java Edition 26.2 / Minecraft Access 1.12.0  
**Collocazione:** `docs/manuali/PRONTUARIO_COMANDI_CHAT.md`  

---

## 🧭 1. Istruzioni Rapide per l'Inserimento da Tastiera (Zero Mouse)

- **Apertura Chat:** Premere **`T`** (oppure direttamente **`/`**). NVDA vocalizza l'apertura e i caratteri digitati.
- **Incolla Rapido:** Copiare il comando desiderato da questo manuale (`Ctrl + C`), aprire la chat in gioco con `T`, incollare con **`Ctrl + V`** e premere **`Invio`**.
- **Autocompletamento:** Premere il tasto **`Tab`** per completare automaticamente nomi di comandi, blocchi, coordinate o selettori.
- **Cronologia Comandi:** Usare le frecce **`Freccia Su`** e **`Freccia Giù`** all'interno della chat aperta per scorrere e riutilizzare i comandi inviati in precedenza.
- **Selettore Sicuro `@s`:** Si raccomanda di usare sempre `@s` (sé stesso): garantisce che l'effetto sia applicato unicamente al proprio personaggio.

---

## 🧱 2. Comandi per Risorse e Materiali di Costruzione (`/give`)

### Il Comando Specifico per i Mattoni di Pietra (Stone Bricks)
```text
/give @s minecraft:stone_bricks 50
```
- **Scopo:** Assegna istantaneamente 50 blocchi di mattoni di pietra nel tuo inventario.
- **Funzionamento:** Se hai spazio negli slot liberi della hotbar o dell'inventario, i 50 mattoni vengono depositati direttamente. Se l'inventario è pieno, cadono a terra ai tuoi piedi pronti per essere raccolti.

### Altri Materiali Utili per il Tuo Cantiere
- **50 Scalini di Mattoni di Pietra:**
  ```text
  /give @s minecraft:stone_brick_stairs 50
  ```
  - *Scopo:* Gradini per rampe e scale interne o perimetrali.
- **16 Scale a Pioli:**
  ```text
  /give @s minecraft:ladder 16
  ```
  - *Scopo:* Scale verticali per condotti, salite su tetti e torri.
- **4 Botole di Acacia:**
  ```text
  /give @s minecraft:acacia_trapdoor 4
  ```
  - *Scopo:* Botole per chiusura sicura di botole sul tetto o pavimenti.
- **32 Muretti di Mattoni di Pietra (Parapetti):**
  ```text
  /give @s minecraft:stone_brick_wall 32
  ```
  - *Scopo:* Ringhiere e parapetti per terrazze e tetti anticaduta.
- **64 Torce Luminose:**
  ```text
  /give @s minecraft:torch 64
  ```
  - *Scopo:* Punti luce per pareti e pavimenti.
- **64 Assi di Quercia:**
  ```text
  /give @s minecraft:oak_planks 64
  ```
  - *Scopo:* Legname lavorato per pavimenti, porte e mobili.
- **32 Pagnotte di Pane (Cibo):**
  ```text
  /give @s minecraft:bread 32
  ```
  - *Scopo:* Ricarica immediata della barra della fame.

---

## 🚀 3. Teletrasporto, Soccorso e Coordinate (`/tp`)

- **Salita di Emergenza da Buca o Burrone (+20 blocchi in alto):**
  ```text
  /tp @s ~ ~20 ~
  ```
  - *Scopo:* Se cadi in un burrone o in una buca profonda senza via d'uscita, ti solleva verticalmente di 20 blocchi mettendoti in salvo.
- **Spostamento Orizzontale Relativo (es. 5 blocchi a Est):**
  ```text
  /tp @s ~5 ~ ~
  ```
  - *Scopo:* Ti sposta di 5 blocchi lungo l'asse X senza variare la quota.
- **Teletrasporto a Coordinate Assolute:**
  ```text
  /tp @s <X> <Y> <Z>
  ```
  - *Esempio per tornare alla base:*
    ```text
    /tp @s -62 79 -35
    ```
  - *Scopo:* Ti posiziona esattamente alle coordinate specificate.

---

## 🏗️ 4. Costruzione e Modellazione Voxel Rapida (`/fill` e `/setblock`)

- **Piazzare un Blocco Solido Esattamente Sotto i Piedi (`~-1`):**
  ```text
  /setblock ~ ~-1 ~ minecraft:stone_bricks
  ```
  - *Scopo:* Crea un punto d'appoggio solido al suolo senza rischiare cadute.
- **Piazzare una Torcia ai Tuoi Piedi:**
  ```text
  /setblock ~ ~ ~ minecraft:torch
  ```
  - *Scopo:* Posiziona una torcia nello spazio d'aria della quota piedi, poggiata sul blocco sottostante.
- **Liberarsi da Soffocamento o Sabbia/Ghiaia Crollata:**
  ```text
  /setblock ~ ~1 ~ minecraft:air
  ```
  - *Scopo:* Svuota istantaneamente il blocco all'altezza della testa se vieni compenetrato da blocchi solidi.
- **Creare una Stanza Rifugio Vuota (11 × 5 × 11 blocchi):**
  ```text
  /fill ~-5 ~-1 ~-5 ~5 ~3 ~5 minecraft:stone_bricks hollow
  ```
  - *Scopo:* Costruisce un guscio esterno in mattoni di pietra lasciando l'interno completamente vuoto e calpestabile (pavimento a `~-1`, altezza interna 3 blocchi).
- **Chiudere una Buca nel Pavimento (3 × 3 blocchi):**
  ```text
  /fill ~-1 ~-1 ~-1 ~1 ~-1 ~1 minecraft:stone_bricks
  ```
  - *Scopo:* Ripara buchi e dislivelli sul pavimento attorno al giocatore.

---

## ⏱️ 5. Controllo del Tempo e della Simulazione (`/time` e `/tick`)

- **Impostare Mattino (Luce Solare Piena):**
  ```text
  /time set day
  ```
  - *Scopo:* Porta istantaneamente l'orologio di gioco alle 07:00 del mattino.
- **Rallentare il Ciclo Giorno/Notte a 1/10 (Giornate 10 volte più lunghe):**
  ```text
  /time rate 0.1
  ```
  - *Scopo:* Fa durare il giorno circa 200 minuti reali invece di 20, mantenendo la fisica di gioco a velocità normale (20 tick/s).
- **Fermare / Riavviare il Sole (WorldClock):**
  - Ferma: `/time pause`
  - Riavvia: `/time resume`
- **Rallentare la Fisica di Gioco al 10% (Ideale per Apprendimento o Pericolo):**
  ```text
  /tick rate 2
  ```
  - *Scopo:* Rallenta il movimento di mostri, acqua e fisica a 2 tick al secondo (consente di muoversi e pensare con calma senza panico).
- **Ripristinare la Velocità Fisica Standard (20 tick/s):**
  ```text
  /tick rate 20
  ```
- **Congelamento Totale delle Entità (Freeze):**
  - Congela: `/tick freeze` (i mostri e la fisica si fermano, tu puoi camminare e orientarti).
  - Sblocca: `/tick unfreeze` (riprende il gioco normale).

---

## 🛡️ 6. Regole di Gioco per Accessibilità e Sicurezza (`/gamerule`)

- **Conservare gli Oggetti dopo la Morte (No Drop):**
  ```text
  /gamerule keep_inventory true
  ```
  - *Scopo:* Se muori, mantieni tutto l'inventario e tutti i livelli di esperienza.
- **Impedire ai Creeper di Distruggere i Blocchi (No Griefing):**
  ```text
  /gamerule mob_griefing false
  ```
  - *Scopo:* Le esplosioni infliggono danno al giocatore ma non distruggono pareti, porte o torce di casa.
- **Disattivare i Danni da Caduta (No Fall Damage):**
  ```text
  /gamerule fall_damage false
  ```
  - *Scopo:* Elimina completamente il rischio di morire cadendo da altezze o tetti.
- **Bloccare il Ciclo Giorno/Notte in Modo Permanente:**
  ```text
  /gamerule advance_time false
  ```
- **Bloccare Pioggia e Temporali (Sempre Sereno):**
  ```text
  /gamerule advance_weather false
  ```
- **Disattivare la Comparsa dei Mostri Ostili:**
  ```text
  /gamerule spawn_monsters false
  ```
  - *Scopo:* Rimuove zombie, scheletri e creeper mantenendo animali amichevoli (mucche, pecore, cavalli).

---

## 🧪 7. Effetti di Stato di Supporto (`/effect`)

- **Fame Infinita (Barra del Cibo Sempre al 100%):**
  ```text
  /effect give @s minecraft:saturation infinite 0 true
  ```
  - *Scopo:* Elimina totalmente la necessità di mangiare; la salute si rigenera continuamente.
- **Rigenerazione Rapida della Salute:**
  ```text
  /effect give @s minecraft:regeneration infinite 1 true
  ```
  - *Scopo:* Recupero automatico e costante dei cuori.
- **Caduta Lenta (Discesa Dolce come una Piuma):**
  ```text
  /effect give @s minecraft:slow_falling infinite 0 true
  ```
  - *Scopo:* Rende impossibile cadere bruscamente: si galleggia dolcemente verso il basso.
- **Respirazione Subacquea Infinita:**
  ```text
  /effect give @s minecraft:water_breathing infinite 0 true
  ```
- **Rimuovere Tutti gli Effetti Attivi:**
  ```text
  /effect clear @s
  ```

---

## 📍 8. Punti di Rinascita e Localizzazione (`/spawnpoint` e `/locate`)

- **Fissare la Casa come Tuo Punto di Rinascita Personale:**
  ```text
  /spawnpoint @s
  ```
  - *Scopo:* Se muori o rientri, rinascerai esattamente nel blocco in cui hai eseguito il comando.
- **Cercare il Villaggio più Vicino:**
  ```text
  /locate structure #minecraft:village
  ```
  - *Scopo:* Trova le coordinate del villaggio più vicino vocalizzandole in chat.
- **Cercare una Miniera Abbandonata:**
  ```text
  /locate structure #minecraft:mineshaft
  ```

---

## 🔄 9. Modalità di Gioco (`/gamemode`)

- **Modalità Sopravvivenza:**
  ```text
  /gamemode survival
  ```
- **Modalità Creativa (Volo, Invulnerabilità, Blocchi Infiniti):**
  ```text
  /gamemode creative
  ```
- **Modalità Avventura (Impossibile rompere blocchi per sbaglio):**
  ```text
  /gamemode adventure
  ```
  - *Scopo:* Utile per visitare percorsi e case senza rischiare di colpire muri o colture.

---

## 📋 10. Cheat-Sheet Tascabile per Copia-Incolla Diretto

```text
/give @s minecraft:stone_bricks 50             -> 50 mattoni di pietra
/give @s minecraft:stone_brick_stairs 50      -> 50 scalini di mattoni
/give @s minecraft:ladder 16                  -> 16 scale a pioli
/give @s minecraft:acacia_trapdoor 4          -> 4 botole di acacia
/give @s minecraft:stone_brick_wall 32        -> 32 muretti parapetto
/give @s minecraft:torch 64                   -> 64 torce
/give @s minecraft:bread 32                   -> 32 pagnotte
/tp @s ~ ~20 ~                                -> Soccorso in alto (+20)
/time set day                                 -> Fai giorno
/gamerule keep_inventory true                 -> Tieni oggetti alla morte
/gamerule fall_damage false                   -> Nessun danno da caduta
/effect give @s saturation infinite 0 true    -> Fame sempre piena
/spawnpoint @s                                -> Salva punto di rinascita qui
```
