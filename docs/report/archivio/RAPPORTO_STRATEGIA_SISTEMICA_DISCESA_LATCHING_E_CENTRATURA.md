# Rapporto Tecnico: Diagnosi Voxel su Salvataggio Reale e Strategia Sistemica per la Discesa Sicura (Latching State Machine, Swept-Volume & Centratura Cognitiva)

**Autori:** Luca & Antigravity  
**Destinatario Primario:** ChatGPT (Senior Architectural Review)  
**Ambito:** Perfezionamento Sistemico della Fase 3A — Dominio Sicurezza / `FallDetector` & `TraversalSafetyAnalyzer`  
**Riferimento Revisione:** Rev MC-26.8  
**Stato:** `[DOCUMENTO DI INDIRIZZO E PROPOSTA SISTEMICA — IN ATTESA DI REVISIONE DI CHATGPT]`  

---

## 0. Executive Summary & Obiettivo

Durante il collaudo manuale in-game della prima iterazione del `TraversalSafetyAnalyzer` (commit `d60c234a`), Luca ha riscontrato un comportamento peculiare:
- La discesa dalla scala a pioli verso terra ha successo **esclusivamente saltando (premendo Spazio)**;
- Se l'utente tenta di scendere **camminando in avanti verso la scala** oppure **voltandosi di 180° e camminando all'indietro**, il personaggio non riesce a scendere e viene bloccato sul ciglio, scatenando anche allarmi di burrone a profondità 17–19 blocchi.

Anziché implementare una correzione ad hoc o un "hack" tarato sulla specifica casa di Luca, il presente rapporto:
1. Riporta la **radiografia voxel oggettiva** estratta dai file di salvataggio del mondo reale (`.mca` e `.dat` NBT);
2. Isola le **cause radice fisiche e geometriche a monte** che determinano l'attrito tra la fisica Vanilla di Minecraft e l'algoritmo di sicurezza;
3. Risponde alla fattibilità voxel del prolungamento superiore della scala;
4. Propone una **Strategia Sistemica a 3 Pilastri** (Swept-Volume 3D, Latching State Machine con finestra di grazia, e Assistenza Cognitiva di Centratura) per rendere la transizione di discesa fluida, sicura e deterministica in qualunque contesto di gioco.

---

## 1. Evidenza Empirica: Telemetria Live e Dati dal Salvataggio Reale

### 1.1 La Telemetria Live del Client (`latest.log` ore 17:00 – 17:01)
I log in-game evidenziano che il nuovo componente `TraversalSafetyAnalyzer` riconosce la scala e produce l'evento semantico atteso:
```text
[17:00:12] Narrating: Discesa sicura
[17:00:21] Narrating: Discesa sicura
[17:00:48] Narrating: Discesa sicura
[17:00:51] Narrating: Discesa sicura
```
Tuttavia, quando il giocatore si muove all'indietro o cammina verso il bordo:
```text
[17:00:58] Narrating: Attenzione: burrone 1 blocchi indietro 2 blocchi in basso, profondità 19 blocchi
[17:00:58] Narrating: Attenzione: burrone 1 blocchi indietro 2 blocchi in basso, profondità 18 blocchi
[17:01:00] Narrating: Sul ciglio: burrone 2 blocchi in basso, profondità 17 blocchi
```
L'utente è rimasto bloccato sul bordo finché, premendo il tasto Salto, è riuscito a scendere.

### 1.2 La Radiografia Voxel da File di Salvataggio Reale (`scuola di sopravvivenza mondo 2 (1)`)
La decodifica diretta di `players/data/e48e6275-dac3-40de-8d53-17ec4b51515e.dat` e del chunk `r.-1.-1.mca` ha svelato la configurazione spaziale esatta dell'ambiente di test:

1. **Posizione Giocatore**:
   - `X = -59.28`, `Y = 85.00`, `Z = -41.49`, `Yaw = 0.0°` (Sguardo a Nord), `Pitch = 0.0°`.
   - Piano di calpestio solido: quota **`Y = 84`** (`stone_bricks` da `Z = -41` a `Z = -38`).
2. **Posizione della Scala a Pioli**:
   - Colonna a **`X = -60`**, **`Z = -42`**, `facing = north` (attaccata alla parete Sud a `Z = -41`).
   - Sviluppo verticale: da `Y = 82` a **`Y = 84`** (3 pioli).
   - **Piolo sommitale**: quota **`Y = 84`**, ovvero **1 blocco sotto la quota piedi del giocatore** (`Y = 85.0`).
   - Sotto la scala: a `Y = 80` è presente un tetto/terrazza solido (`stone_bricks`), ovvero atterraggio sicuro a 2 blocchi di distanza.
3. **L'Intorno Immediato a Destra della Scala (`X = -59`, dove si trova il giocatore)**:
   - A `X = -59, Z = -42`: **ARIA TOTALE** da `Y = 84` fino a `Y = 65`.
   - Esiste un precipizio reale di **19 blocchi di profondità** che costeggia il lato destro della scala!

---

## 2. Diagnosi Fisica e Geometrica a Monte

Dalla comparazione tra coordinate reali e codice emergono tre cause concorrenti:

### Causa 1: Disallineamento Laterale e Campionamento Unidimensionale
- Luca si trova a `X = -59.28`. La scala è centrata a `X = -60.0`.
- Esiste un offset laterale di circa **0.72 metri** (il giocatore è spostato a destra).
- Poiché l'analizzatore campiona un singolo raggio lineare centrale lungo l'intento:
  - Muovendosi a Nord (o indietreggiando da Sud), il raggio cade a `X = -59, Z = -42`.
  - In quel punto **non c'è la scala**, ma c'è il dirupo profondo 19 blocchi!
  - Il sistema classifica correttamente quel raggio come `DANGEROUS_DROP` (profondità 19) e interdice il movimento con l'auto-sneak, impedendo all'utente di precipitare.

### Causa 2: Il Vincolo Fisico dello Sneak di Minecraft Vanilla (A terra vs In volo)
- La scala finisce a quota `Y = 84`; il pavimento solido su cui poggia il giocatore è a quota `Y = 84` (piedi a quota 85).
- In Minecraft Vanilla (`Entity.collide`):
  - **A terra (onGround = true)**: quando `keyShift` è attivo, il motore fisico proibisce categoricamente alle suole del giocatore di oltrepassare il bordo del blocco solido a `Z = -41`. Il corpo non può sporgersi quei 15–20 cm necessari a far collidere la hitbox con il piolo a quota 84.
  - **In aria durante un salto (!onGround)**: il motore di Minecraft sospende il vincolo di bordo dello sneak. Il salto proietta la hitbox in avanti nel vuoto a `Z = -42`, il personaggio scende verso quota 84, collide con la scala e il motore di gioco lo aggancia ai pioli (`isClimbing = true`), consentendo la discesa.

### Causa 3: Isteresi del Presidio Statico da Fermo (`isStandingOnDangerousEdge`)
- Quando il giocatore tocca il bordo e rallenta prima dello step-off, la velocità orizzontale cala (`moveDir == null`).
- In quell'istante, `isStandingOnDangerousEdge` campiona gli 8 punti radiali; i campioni a destra intercettano il precipizio da 19 blocchi, riapplicando immediatamente `engageFallProtection()`.
- Lo sneak viene riapplicato proprio mentre il giocatore sta tentando di compiere il passo di discesa, creando un ciclo bloccante.

---

## 3. Analisi Voxel sul Prolungamento Superiore della Scala

Luca ha posto una domanda fondamentale:  
*«Se estendo la scala a pioli anche un blocco sopra la posizione attuale facendola sporgere verso l'alto nel tetto, la rendo più tracciabile e gestibile? È ammesso dalle regole di Minecraft?»*

### Risposta Tecnica e Normativa Voxel:
1. **Regola di Supporto Vanilla**: Una scala a pioli (`LadderBlock`) non può fluttuare nel vuoto; deve essere posizionata contro la faccia di un blocco solido. Se il tetto termina piatto a quota 84, non è possibile piazzare una scala a quota 85 a meno di creare un supporto verticale (es. pilastro, muretto, staccionata o colonna a quota 85).
2. **Impatto sul Gameplay e sull'Accessibilità**:
   - Se la scala viene estesa a quota piedi (`Y = 85`), **l'accessibilità migliora drasticamente al 100%**:
     - Il giocatore cammina sul tetto ed entra nel volume della scala **prima** che i piedi lascino il pavimento solido;
     - La hitbox aggancia la scala istantaneamente sul piano orizzontale;
     - Non è necessario scavalcare il ciglio né saltare: il motore di Minecraft pone `isClimbing = true` sul posto e la discesa avviene con la massima naturalezza.
   - Soluzioni equivalenti perfettamente legali in Vanilla sono:
     - **Botola aperta** a quota calpestio sopra la scala;
     - **Impalcatura (`ScaffoldingBlock`)**, che non necessita di parete posteriore e consente il transito verticale continuo sia a salire che a scendere.

---

## 4. La Proposta Architetturale Sistemica a 3 Pilastri

Per garantire che il mod funzioni a monte in **qualsiasi architettura** (anche con tetti piatti senza scala sporgente e con dirupi laterali), proponiamo una soluzione strutturata su tre pilastri:

```text
[ Input & Movimento ]
        │
        ▼
[ PILASTRO 1: Swept-Volume Hitbox ] ──→ Valuta l'intero volume del corpo (0.6m), non un punto
        │
        ├─→ Se disallineato (> 0.25m) ──→ [ PILASTRO 3: Guida Cognitiva di Centratura ]
        │                                  ("Scala a sinistra / destra" + Audio 3D)
        │
        ▼ Se allineato al corridoio
[ PILASTRO 2: Latching State Machine ] ──→ Entra in stato DESCENT_TRANSITION (600-800 ms)
        │                                  - Sopprime lo sticky sneak isterico
        │                                  - Rilascia l'override per consentire lo step-off
        │                                  - Fail-safe istantaneo se l'utente sterza nel vuoto
        ▼
[ Aggancio Fisico alla Scala & Discesa Naturale ]
```

---

### Pilastro 1 — Hitbox Swept-Volume (Dalla Linea al Volume Reale)
- **Concetto**: Sostituire il raycast unidimensionale con un test di intersezione volumetrica della hitbox del giocatore ($0.6 \times 1.8 \times 0.6\text{ m}$) traslata lungo il vettore di moto (`Minkowski sum` discreta).
- **Meccanica**:
  - Proiettare 3 raggi paralleli: mezzeria, bordo sinistro (`-0.25 m`) e bordo destro (`+0.25 m`);
  - Se almeno una parte significativa della hitbox interseca il volume di aggancio della scala (`ladderAABB`), il corridoio è classificato come **transito verso scala**;
  - Il vuoto adiacente non deve scatenare un allarme bloccante se l'intento proietta il corpo verso la scala e non verso il precipizio laterale.

### Pilastro 2 — Latching State Machine (Finestra di Grazia per lo Step-Off)
- **Concetto**: Eliminare la volatilità tick-by-tick introducendo una macchina a stati finiti con finestra temporale controllata.
- **Stati della Macchina**:
  1. `IDLE_OR_SAFE`: camminata ordinaria su piano solido;
  2. `FALL_PROTECTION_ENGAGED`: ciglio pericoloso senza discesa (auto-sneak attivo);
  3. `DESCENT_TRANSITION`: l'utente si muove attivamente verso una scala convalidata;
     - Durata: finestra di grazia di 600–800 ms;
     - Comportamento: `SafetyMovementGuard` revoca l'auto-sneak anche se il giocatore tocca il bordo a velocità ridotta;
     - Uscita: se il giocatore si aggancia alla scala (`player.onClimbable()`), lo stato si chiude con successo;
     - **Fail-Safe**: se il vettore di movimento devia di oltre 45° rispetto alla scala, se l'utente rilascia i tasti di moto o se arretra verso il vuoto, lo stato transitorio decade all'istante e `engageFallProtection()` viene ripristinato in tempo zero.

### Pilastro 3 — Assistenza Cognitiva di Centratura & Audio 3D Posizionale
- **Concetto**: Un giocatore non vedente non può dedurre visivamente di trovarsi 70 cm a destra rispetto a una scala larga 1 metro, soprattutto quando a destra c'è un baratro di 19 blocchi.
- **Meccanica**:
  - Quando il `TraversalSafetyAnalyzer` individua una scala nel raggio di 1.5 metri, calcola l'offset laterale perpendicolare all'asse di discesa:
    $$\Delta_{\text{lateral}} = (\vec{P}_{\text{player}} - \vec{P}_{\text{ladder}}) \cdot \vec{u}_{\text{lateral}}$$
  - Se $|\Delta_{\text{lateral}}| > 0.25\text{ m}$, il sistema non dichiara semplicemente *"Discesa sicura"*, ma fornisce assistenza attiva:
    - Esempio: *"Scala per scendere, 1 passo a sinistra"*;
  - Il cue sonoro (`SoundEvents.LADDER_STEP`) viene posizionato alle coordinate esatte del piolo sommitale (`-60.0, 84.0, -42.0`), consentendo all'utente di centrare la scala ad orecchio prima di iniziare la discesa.

---

## 5. Analisi Comparativa: Punti di Forza vs Punti di Attenzione

### Punti di Forza dell'Approccio Sistemico
1. **Universalità Indipendente dalla Mappa**: Funziona su tetti piatti, miniere, scale su pareti a sbalzo, botole o impalcature, senza costringere a modificare le costruzioni in gioco.
2. **Accessibilità Cognitiva Reale (Zero Mouse)**: La combinazione di audio 3D posizionale e indicazione di centratura ("a sinistra/destra") permette al non vedente di affrontare burroni adiacenti con totale serenità.
3. **Fisica Naturale di Discesa**: Elimina la necessità antinaturale di saltare dal tetto per agganciare una scala.
4. **Disaccoppiamento e Manutenibilità**: Il codice risiede interamente in `features.safety.traversal`, lasciando pulito `FallDetector` e congelata al 100% la Fase 3B.

### Punti di Attenzione e Rischi da Presidiare (Fail-Safe Audit)
1. **Rischio di "Falso Varco" su Dirupi Laterali**: Se l'utente si trova a 70 cm dalla scala e cammina dritto verso il vuoto da 19 blocchi, la transizione di discesa **NON DEVE APRIRSI**. La tolleranza laterale deve consentire la discesa SOLO se il vettore punta verso il volume della scala, mai se punta verso il baratro adiacente.
2. **Scadenza Rigorosa del Timeout (Watchdog)**: Se la finestra di grazia dura 800 ms e il giocatore non si aggancia alla scala entro quel tempo (es. resta incastrato su un davanzale), il guard deve riapplicare istantaneamente lo sneak per prevenire cadute nel vuoto.
3. **Priorità Assoluta di un Pericolo Reale**: Un allarme `CRITICAL` di caduta deve poter interrompere istantaneamente qualsiasi stato di grazia se la distanza dalla colonna di sicurezza aumenta.
4. **Preservazione dello Shift Manuale**: Se Luca tiene premuto fisicamente Shift per cautela, il guard non deve rilasciarlo forzatamente.

---

## 6. Domande Guida per la Revisione di ChatGPT

Chiediamo a ChatGPT di analizzare questa proposta architetturale rispondendo ai seguenti punti:
1. La suddivisione in 3 Pilastri (Swept-Volume, Latching State Machine, Assistenza di Centratura) è considerata ottimale, completa e priva di effetti collaterali per il dominio Sicurezza?
2. La durata e le condizioni di guardia della finestra di grazia (Pilastro 2) rispettano rigorosamente il principio fail-safe?
3. Come raccomanda ChatGPT di calcolare l'offset laterale del Pilastro 3 senza appesantire il tick client?
4. Possiamo procedere alla formalizzazione di questa revisione all'interno del piano tecnico della Rev MC-26.8?
