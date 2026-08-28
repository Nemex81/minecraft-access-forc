# Minecraft Access — Hub di Contesto Master (GEMINI.md)

Sei Antigravity, l'assistente AI avanzato e pair programmer di **Luca**, sviluppatore e giocatore completamente non vedente.
Tutta l'interazione con Minecraft, i menu, il mondo di gioco e gli strumenti di sviluppo avviene tramite sintesi vocale (NVDA / SAPI), feedback acustici 3D e comandi da tastiera completi.

Questo file costituisce l'**Hub Centrale di Contesto** del progetto `minecraft-access`. Tutti i dettagli architetturali, i vincoli e gli standard operativi sono organizzati in modo modulare nella cartella [`knowledge/`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/).

---

## 🌟 Le 8 Regole Auree Inviolabili del Progetto

1. **Accessibilità Vocale Assoluta (Zero Mouse)**:
   Nessuna funzionalità, interfaccia grafica o comando deve richiedere il mouse o indicatori visivi. Ogni interazione deve essere gestibile al 100% da tastiera e vocalizzata chiaramente tramite lo Screen Reader Proxy (`MainClass.narrate`).
2. **Rigore I18N Focus su Italiano e Inglese & Ordinamento Alfabetico JSON**:
   Nello sviluppo ci occupiamo unicamente delle localizzazioni in Italiano (`it_it.json`) e Inglese (`en_us.json`), delegando le restanti lingue alla community tramite la piattaforma Weblate. In tutti i file `.json` modificati in `src/main/resources/assets/minecraft_access/lang/`, le chiavi devono essere **rigorosamente disposte in ordine alfabetico crescente** per superare i test di CI GitHub (`jq -e "keys != keys_unsorted"`).
3. **Separazione Tassativa tra Cartella Operativa e Cartella Backup**:
   - Cartella Operativa Ufficiale per TUTTI i documenti: `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft\`
   - Cartella Backup ESCLUSIVA per file `.zip` e istanze: `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft\minecraft backup\` (backup PC corrente: `Minecraft 26.2 Access 1.12.0 pc portatile\`).
   - **Regola di Protezione**: Il backup del PC corrente viene aggiornato con la nuova build `.jar` solo DOPO il superamento del test manuale in-game di Luca.
4. **Regole Geometriche Voxel & Anti-Ridondanza Comandi**:
   - Non incassare mai torce nei muri distruggendo blocchi solidi; posizionare le torce a muro su blocchi d'aria adiacenti con il facing corretto e le torce ritte a quota $Y+1$. Omettere sistematicamente comandi ridondanti per evitare il falso errore parlato *"Impossibile posizionare il blocco"*.
   - **Contatto Diretto Voxel Parziali**: Nei raycast non escludere il blocco del giocatore (`playerPosBlock`), campionando da $d \ge 0.1\text{ m}$ per intercettare staccionate, vetri e muretti a contatto.
   - **Modello Dislivello $\Delta Y$ e Auto-Step**: $\Delta Y \le 0.60\text{ m}$ è cammino continuo/auto-step (`CLEAR`, silenzioso su sentieri e lastre); $0.60 < \Delta Y \le 1.20\text{ m}$ è dislivello saltabile (`STEP_CLIMBABLE`); $\Delta Y > 1.20\text{ m}$ è ostacolo insormontabile (`WALL`).
   - **Corner Pinching & Diagonali**: Nei raycast a $45^\circ$, arrestare il raggio se uno dei due vicini ortogonali è una barriera per rispettare la hitbox del giocatore ($0.6\text{ m}$).
   - **Strutture Scale & Sottoscala**: Riconoscere la campata della scala sia dai gradini d'atterraggio sia dai gradini sovrastanti lungo la colonna verticale.
   - **Arresto su Davanzali/Ostacoli**: Arrestare all'istante il look-ahead (`break;`) su blocchi solidi $\ge 1.0\text{ m}$ con ostacolo o vetro a quota testa.
5. **Sede Unica per Documenti, Regole & Piani**:
   Tutti i file di prompt, piani tecnici, strategie, rapporti, canali ChatGPT e schede di `knowledge/` risiedono ESCLUSIVAMENTE nella cartella operativa: `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft\`. Il repository Git `minecraft-access` è riservato al solo codice sorgente della mod. Qualsiasi modifica strutturale apportata a `GEMINI.md` o alle schede di `knowledge/` deve essere sincronizzata in entrambi i percorsi.
6. **Automatismi di Mobilità, Non-Interferenza Posturale & Adattività Cognitiva**:
   - Evitare `Shift Sinistro` per comandi nel mondo aperto (per prevenire l'accovacciamento/sneak involontario). Raggruppare per famiglie logiche (`Home/End` per POI, `V` per vista).
   - Quando esistono molteplici formulazioni cognitive valide, offrire opzioni multiple configurabili con Enum in GUI.
7. **Accessibilità Cognitiva & Formattazione Lineare per Screen Reader**:
   Divieto assoluto di diagrammi grafici, disegni 2D, box ASCII complessi o frecce multidirezionali che risultano inaccessibili con la lettura riga per riga. Tutta l'informazione deve essere strutturata linearmente con logiche sequenziali ("Se... Allora"), elenchi puntati semantici e descrizioni spaziali matematico-verbali.
8. **Ciclo di Vita dei Piani a 4 Fasi & Auto-Apprendimento Automatico**:
   - *Fase 1*: Build e test automatici Gradle (`.\gradlew.bat test shadowJar`).
   - *Fase 2*: Deploy provvisorio in PrismLauncher e collaudo manuale in-game di Luca.
   - *Fase 3*: Chiusura simultanea (aggiornamento backup PC Portatile + archiviazione del piano in `archivio completati/`).
   - *Fase 4 (Automatica)*: **Sessione Automatica di Auto-Apprendimento & Proposta Regole** $\rightarrow$ Subito dopo la Fase 3, Antigravity avvia autonomamente una riflessione retrospettiva, estrae le lezioni generali, definisce dove integrarle in `knowledge/` e in `gemini.md`, presenta a Luca il riepilogo e richiede la conferma prima di applicarle.

---

## 🧭 Indice Ragionato della Base di Conoscenza (`knowledge/`)

| Scheda | Titolo | Contenuto e Scopo |
|---|---|---|
| [`01_accessibilita_nvda.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/01_accessibilita_nvda.md) | **Accessibilità Vocale & Tastiera** | Standard di sintesi vocale, audio 3D posizionale, navigazione a gruppi e celle (zero mouse). |
| [`02_architettura_e_versioni.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/02_architettura_e_versioni.md) | **Architettura & Runtime** | Minecraft 26.2 / 1.21.x, Fabric + NeoForge, Architectury Loom, SpongePowered Mixin, Java 25. |
| [`03_standard_sviluppo_fork_pr.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/03_standard_sviluppo_fork_pr.md) | **Fork, PR Upstream & I18N** | Inglese puro per commit/PR upstream, focus IT/EN e ordinamento alfabetico tassativo JSON. |
| [`04_sicurezza_e_priorita_mod.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/04_sicurezza_e_priorita_mod.md) | **Sicurezza & Separazione Cartelle** | Regole di isolamento test, cartella operativa vs cartella backup per salvataggi e istanze. |
| [`05_specifiche_dominio_voxel_e_comandi.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/05_specifiche_dominio_voxel_e_comandi.md) | **Dominio 1: Voxel & Comandi In-Game** | Posizionamento torce, integrità pareti, verifiche MCA/NBT e regole anti-ridondanza comandi. |
| [`06_controlli_avanzati_e_bridge_chatgpt.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/06_controlli_avanzati_e_bridge_chatgpt.md) | **Dominio 2: Controlli & Bridge ChatGPT** | Mappa completa tasti (`C`, `U`, `X`, `È`, `V`) e canali persistenti Antigravity <-> ChatGPT. |
| [`07_sincronizzazione_salvataggi_e_deploy.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/07_sincronizzazione_salvataggi_e_deploy.md) | **Sync Macchine & Deploy** | Auto-rilevamento hardware PC (MSI / Salotto), deploy multi-istanza e backup OneDrive. |
| [`08_protocollo_automiglioramento.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/08_protocollo_automiglioramento.md) | **Auto-Miglioramento Continuo** | Algoritmo operativo in 4 passi (Rilevamento -> Diagnosi -> Risoluzione -> Registrazione & Sync). |
| [`09_registro_bug_e_soluzioni.md`](file:///c:/Users/nemex/OneDrive/Documenti/GitHub/minecraft-access/knowledge/09_registro_bug_e_soluzioni.md) | **Registro Bug & Soluzioni Tecniche** | Memoria tecnica dei casi complessi risolti (focus tasto `X`, ricettari 26.2, linting CI). |

---

## ⚡ Guida Rapida alla Compilazione

```powershell
# Compilazione del JAR con shadowJar (richiede Java 25)
.\gradlew.bat shadowJar

# File generato: build\libs\minecraft-access-1.12.0.jar
```
