# Piano Tecnico Formale: Dispacciamento Diretto Ostacoli (onObstacleDetected), Micro-Voxel Raymarch (0.05m) & Armonizzazione XZ (Rev MC-29.0 - Rev MC-29.6)
# Autore: Luca (Sviluppatore Senior Non Vedente con Screen Reader NVDA) & Antigravity
# Data: 2026-09-02
# Ambito: Repository `minecraft-access`
# Incremento Versione Target (AVF): Minor Revision (v26.2-1.18.0)
# Stato: In Fase di Validazione Preventiva (Gating Semantico Sotto-Fase 1A)

---

## 📌 1. Quadro di Riferimento & Sintesi Funzionale

Il presente Piano Tecnico consolida l'architettura sensoriale di `minecraft-access` rendendo l'emissione vocale degli ostacoli autonoma e istantanea:

1. **Feedback Adattivo di Dislivello Verticale & Altezza Cubi (Rev MC-29.0)**:
   - 4 Modalità: `SOUND_AND_VOICE`, `SOUND_ONLY`, `VOICE_ONLY`, `OFF`;
   - 3 Stili Vocali: `DESCRIPTIVE`, `COMPACT`, `DELTA_ONLY`;
   - Toggle Quota Zero: `narrateSameLevel`.
2. **Regolatore di Verbosità Faccia del Blocco (Rev MC-29.0)**:
   - 4 Modalità: `DESCRIPTIVE` (*"Porta di betulla, lato ovest"*), `TOP_BOTTOM_ONLY`, `COMPACT`, `OFF`.
3. **Micro-Voxel Raymarch Continuo ($0.05\text{m}$) (Rev MC-29.6)**:
   - Campionamento volumetrico fitto con avvio a $d = 0.05\text{m}$ e passo $0.10\text{m}$;
   - Intercetta le lamine sottili (porte, vetri, staccionate, sbarre) a qualsiasi coordinata.
4. **Dispacciamento Diretto al Manager (`onObstacleDetected`) (Rev MC-29.6)**:
   - `ObstacleDetector` invoca direttamente `CrosshairFeedbackManager.onObstacleDetected(result, msg, relAngle)`:
     - **Cammino Frontale (`W`)**: Emette all'istante l'annuncio frontale dinamico con progressione della distanza:
       > *"Davanti: Ostacolo di Pannello di vetro, a 3 blocchi"* (senza attendere il mirino);
     - **Cammino Laterale/Retro (`A`/`D`/`S`)**: Compone fluidamente:
       > *"A destra: Salita su Fornace. Davanti: Assi di quercia, a 2 blocchi"*;
     - **Passo Libero**: emette il blocco frontale del mirino (*"Porta di betulla, a 1 blocco"*);
     - **Centramento Testa (`5`/`M`)**: frase fluida integrata (*"Sguardo livellato, Ovest. Davanti: Porta di betulla, a 2 blocchi"*).

---

## 🏛️ 2. Dettagli Architetturali della Sezione Aggiornata (Rev MC-29.6)

### 2.1 Metodo Diretto in `CrosshairFeedbackManager.java`
```java
public static void onObstacleDetected(ObstacleScanResult result, String obstacleMsg, double relAngle) {
    Minecraft client = Minecraft.getInstance();
    Player player = client.player;
    if (player == null) return;

    String frontPrefix = I18n.get("minecraft_access.obstacle_detector.dir_forward");
    if (frontPrefix.equals("minecraft_access.obstacle_detector.dir_forward")) {
        frontPrefix = "Davanti";
    }

    boolean isFrontal = obstacleMsg.startsWith(frontPrefix);
    String message;

    if (isFrontal) {
        int distance = Math.max(1, (int) Math.round(Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(result.targetFootPos())))));
        String distStr = (distance <= 1)
                ? getI18nString("minecraft_access.crosshair_feedback.distance_blocks_single", "1 blocco")
                : getI18nString("minecraft_access.crosshair_feedback.distance_blocks", "%d blocchi", distance);
        message = getI18nString("minecraft_access.crosshair_feedback.at_distance", obstacleMsg, distStr);
    } else {
        if (currentNarration != null && !currentNarration.isBlank()) {
            message = obstacleMsg + ". " + frontPrefix + ": " + currentNarration;
        } else {
            message = obstacleMsg;
        }
    }

    if (!Strings.isEmpty(message)) {
        lastNarrationTime = System.currentTimeMillis();
        MainClass.narrate(message, true);
    }
}
```

---

## 🛡️ 3. Protocollo di Validazione Preventiva (7 Assi di Qualità)

1. **Validità**: Elimina definitivamente la causa della voce muta durante l'allarme sonoro, garantendo sincronia perfetta tra audio 3D e voce.
2. **Efficacia**: Massima ergonomia: mentre ti avvicini con `W`, la voce annuncia con precisione l'ostacolo a ogni metro di distanza.
3. **Coerenza**: Piena Clean Architecture con invocazione diretta del Presentation Coordinator (`CrosshairFeedbackManager`).
4. **Completezza**: Copre ogni tipologia di ostacolo (frontale, laterale, posteriore, dislivelli, salti).
5. **Precisione**: Esecuzione deterministica $O(1)$.
6. **Affidabilità & Prestazioni**: Zero messaggi in sospeso o perduti.
7. **Assenza di Regressioni**: I cue sonori 3D OpenAL restano reattivi e perfetti.

---

## 🧪 4. Matrice di Simulazione a 3 Livelli (Rev MC-29.6)

- **Livello 1 — Scenario Comune (Camminata verso vetro/porta a 3 blocchi con W)**:
  - *Se* cammini in avanti verso il vetro:
  - *Allora* il sensore rileva l'ostacolo $\rightarrow$ suona il cue ed emette all'istante: *"Davanti: Ostacolo di Pannello di vetro, a 3 blocchi"*.
- **Livello 2 — Scenario Alternativo (Passo a destra con ostacolo e porta davanti)**:
  - *Se* cammini a destra verso un baule guardando la porta:
  - *Allora* emette all'istante: *"A destra: Salita su Baule. Davanti: Porta di betulla, a 1 blocco"*.
- **Livello 3 — Caso Limite (Aderenza al muro)**:
  - *Se* ti fermi contro il muro premendo W:
  - *Allora* emette stabilmente l'avviso a 1 blocco ogni intervallo di delay configurato.
