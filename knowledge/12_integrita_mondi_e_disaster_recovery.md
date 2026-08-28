# 12 — Integrità dei Mondi, Snapshot Preventivi & Disaster Recovery

Questa scheda definisce le procedure di sicurezza per tutelare i salvataggi, le costruzioni e le proprietà in-game di Luca da corruzioni di dati, crash imprevisti o errori di esecuzione comandi di costruzione massiva.

---

## 1. Regola dello Snapshot Preventivo

Prima di qualsiasi operazione a potenziale impatto distruttivo o strutturale, è obbligatorio creare uno **Snapshot di Sicurezza** del mondo di gioco.

### Operazioni che Richiedono Snapshot Obbligatorio:
1. **Comandi di Riempimento Massivo**: Esecuzione di `/fill` con volume superiore a 50 blocchi.
2. **Comandi di Duplicazione / Spostamento**: Esecuzione di `/clone` su edifici o stanze intere.
3. **Comandi di Cancellazione / Sostituzione**: Uso di `/fill ... replace air` su strutture esistenti.
4. **Migrazioni o Upgrade di Versione**: Passaggio a nuove versioni di Minecraft o aggiornamenti maggiori di Fabric API.

### Comando Rapido di Snapshot (PowerShell):
```powershell
# Esempio di snapshot istantaneo del mondo attivo
$worldName = "Scuola di sopravvivenza mondo 2"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$src = "C:\Users\nemex\AppData\Roaming\PrismLauncher\instances\Minecraft 26.2 Access 1.12.0\minecraft\saves\$worldName"
$dest = "C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft archivio backup\minecraft backup\${worldName}_snapshot_${timestamp}.zip"

Compress-Archive -Path "$src\*" -DestinationPath $dest -Force
```

---

## 2. Anatomia dei File di Salvataggio

Un salvataggio di Minecraft è composto da componenti critici:
- **`level.dat` / `level.dat_old`**: Contiene lo stato del giocatore (inventario, coordinate XYZ, salute), gamerule e seed.
- **`region/*.mca`**: Contiene la griglia dei voxel compressi (formato Anvil).
- **`poi/` & `entities/`**: Contiene i villici, animali, mostri e nodi POI vanilla.
- **`minecraft/config/minecraft-access/waypoints/singleplayer_*.json`**: Contiene il database dei Waypoint personali della mod.

---

## 3. Procedura di Disaster Recovery (Ripristino di Emergenza)

In caso di corruzione del mondo, blocco del personaggio nel vuoto o errore grave di `/fill`:

1. **Arresto Immediato del Gioco**: Chiudere Minecraft per rilasciare i lock sui file `.mca` e `.dat`.
2. **Isolamento del Mondo Corrotto**:
   - Rinominare la cartella del mondo in `saves/` aggiungendo il suffisso `_corrotto_backup`.
3. **Estrazione del Backup Stabile**:
   - Decomprimere l'ultimo archivio `.zip` integro presente in:  
     `C:\Users\nemex\OneDrive\progetti dei frati\accessible games\minecraft archivio backup\minecraft backup\`
4. **Preservazione dei Waypoints**:
   - Verificare che il file `singleplayer_<nome_mondo>.json` in `config/minecraft-access/waypoints/` sia preservato e non sovrascritto da dati vecchi.
5. **Riavvio & Validazione In-Game**:
   - Avviare Minecraft, caricare il mondo ripristinato e verificare con il tasto Numpad `0` che le coordinate del giocatore siano corrette.