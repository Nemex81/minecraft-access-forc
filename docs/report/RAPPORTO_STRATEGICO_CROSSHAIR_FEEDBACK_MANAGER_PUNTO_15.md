# 🎯 Rapporto Strategico & Dossier di Handover: `CrosshairFeedbackManager` (Punto 15)
# Autore: Luca & Antigravity
# Data: 2026-09-01
# Ambito: Repository `minecraft-access`
# Destinazione: Bootstrap Sessione Dedicata in Nuova Chat

---

## 📌 1. Obiettivo & Contesto di Bootstrap

Questo documento costituisce il **Dossier Tecnico Completo** per trasferire e avviare immediatamente i lavori sul **Punto 15 del Registro Revisioni** in una nuova chat pulita, preservando il 100% dei dettagli architetturali, delle analisi e delle decisioni progettuali concordate con Luca.

### Informazioni di Sistema & Ambiente
- **Repository Locale**: `c:\Users\nemex\OneDrive\Documenti\GitHub\minecraft-access`
- **Branch Git di Sviluppo**: `mymaster` (o `dev`)
- **JDK per Compilazione**: Java 25 (`$env:JAVA_HOME = 'C:\Users\nemex\AppData\Roaming\PrismLauncher\java\java-runtime-epsilon'`)
- **Target di Gioco**: Minecraft 26.2 / 1.21.x su Fabric + Balm
- **Sviluppatore**: Luca (Senior Developer Non Vedente, Screen Reader NVDA, Zero Mouse, Regola 0 permanente)

---

## 🔍 2. Diagnosi Approfondita del Problema Attuale

### Il Fenomeno Riscontrato nel Gameplay
Durante l'esplorazione e la rotazione della visuale, il giocatore non vedente sperimenta una **corsa critica (race condition)** tra due eventi vocali indipendenti:
1. La narrazione del blocco o entità puntata dal mirino (`NarrateCrosshair.java`).
2. La narrazione della direzione cardinale e dei gradi di pitch/yaw dello sguardo (`CameraControls.java` / `NumpadControls.java`).

### Causa Radice Architetturale
- Attualmente, `NarrateCrosshair` e `CameraControls` sono moduli completamente disaccoppiati.
- Quando il giocatore ruota la visuale (ad es. usando il tastierino numerico `NumpadControls` o i comandi `CameraControls`), entrambi i sottosistemi rilevano un cambiamento nello stesso tick o in tick ravvicinati.
- Ciascun modulo invoca autonomamente `MainClass.narrate(..., interrupt: true)`.
- **Conseguenza**: Il secondo evento interrompe e tronca a metà il primo. L'ordine di lettura risulta caotico e imprevedibile (a volte viene letto prima l'angolo e poi troncato dal blocco, altre volte viene letto il blocco e troncato dall'angolo).

---

## 🏛️ 3. La Soluzione Architetturale: `CrosshairFeedbackManager`

L'idea fondamentale (proposta da Luca) è **superare il modello a eventi isolati** introducendo un modulo coordinatore centrale con pattern **Single Source of Truth & Coordinator**:

### Schema Concettuale del Flusso
1. **Sottosistema Raycast Mirino (`NarrateCrosshair`)**: Campiona il blocco/entità/aria e notifica i dati grezzi al manager (`onCrosshairTargetChanged`).
2. **Sottosistema Orientamento Visuale (`CameraControls` / `NumpadControls`)**: Campiona direzione, yaw e pitch e notifica i dati grezzi al manager (`onFacingChanged`).
3. **Manager Centrale (`CrosshairFeedbackManager`)**:
   - Mantiene lo stato integrato di puntamento e visuale.
   - Applica un debouncing atomico per evitare doppie emissioni nello stesso tick.
   - Assembla un'**unica frase atomica** strutturata secondo le preferenze configurate dall'utente.
   - Emette una singola chiamata `MainClass.narrate(atomicMessage, interrupt: true)`.

---

## ⚙️ 4. Configurazione Utente in Cloth Config (`CrosshairFeedbackMode`)

Per offrire massima personalizzazione e rispetto del carico cognitivo, il manager sarà governato da un Enum dedicato:

```java
public enum CrosshairFeedbackMode {
    BLOCK_ONLY,                 // Legge solo il blocco/entità (es. "Mattoni di pietra")
    BLOCK_THEN_FACING,          // Legge blocco e poi orientamento (es. "Mattoni di pietra, Sud 180 gradi, Dritto")
    FACING_THEN_BLOCK,          // Legge prima l'orientamento e poi il blocco (es. "Sud 180 gradi, Dritto: Mattoni di pietra")
    BLOCK_AND_CARDINAL_ONLY,    // Legge il blocco con il solo punto cardinale sintetico (es. "Mattoni di pietra a Sud")
    FACING_ONLY                 // Legge solo la direzione/inclinazione (es. "Sud, 180 gradi, Dritto")
}
```

### Parametri Aggiuntivi di Configurazione:
- `crosshairFeedbackMode`: Enum (default: `BLOCK_THEN_FACING` o `BLOCK_ONLY`).
- `includeDistanceInCrosshair`: booleano per includere i metri di distanza dal blocco.
- `includePitchInFacing`: booleano per includere i gradi di inclinazione (Dritto, Su, Giù).

---

## 📁 5. Mappa dei File Coinvolti

1. **`src/main/java/org/mcaccess/minecraftaccess/features/crosshair/CrosshairFeedbackManager.java`** `[NUOVO]`:
   - Singleton / modulo Balm che accentra la composizione della stringa di puntamento.
2. **`src/main/java/org/mcaccess/minecraftaccess/features/NarrateCrosshair.java`** `[MODIFICA]`:
   - Esegue il raycast e inoltra il target a `CrosshairFeedbackManager` anziché chiamare direttamente `MainClass.narrate`.
3. **`src/main/java/org/mcaccess/minecraftaccess/features/CameraControls.java` & `NumpadControls.java`** `[MODIFICA]`:
   - Quando la rotazione della visuale richiede la lettura dell'orientamento, la richiesta viene instradata tramite il manager.
4. **`src/main/java/org/mcaccess/minecraftaccess/Config.java`** `[MODIFICA]`:
   - Registrazione del campo `crosshairFeedbackMode` nella sezione di configurazione del mirino.
5. **`src/main/resources/assets/minecraft_access/lang/it_it.json` & `en_us.json`** `[MODIFICA]`:
   - Aggiunta in ordine alfabetico rigoroso di tutte le stringhe di localizzazione dell'Enum e dei menu GUI.

---

## 🚀 6. Prompt di Avvio Rapido per la Nuova Chat

Copia e incolla il seguente prompt nella nuova conversazione per avviare subito la sessione:

```text
Ciao Antigravity! Riprendiamo i lavori sul repository minecraft-access.
Ho creato il rapporto di passaggio consegne per il Punto 15 del Registro Revisioni:
docs/report/RAPPORTO_STRATEGICO_CROSSHAIR_FEEDBACK_MANAGER_PUNTO_15.md

Leggi attentamente il rapporto, analizza il modulo CrosshairFeedbackManager che dobbiamo creare per unificare la lettura del mirino e della direzione/orientamento, e formula la proposta tecnica per la Sotto-Fase 1A (Piano Tecnico Formale). Non modificare alcun file finché non ti do la conferma esplicita (Regola 0).
```

---
*Dossier archiviato con successo nella Living Documentation di Minecraft Access.*
