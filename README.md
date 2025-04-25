# 🏢 Organigramma Aziendale

**Organigramma Aziendale** è un'applicazione Java sviluppata per la gestione dinamica di un **organigramma aziendale**, completa di interfaccia grafica Swing e progettata secondo il pattern **MVC** con l'utilizzo di diversi **design pattern** classici (Factory, Singleton, Strategy, Composite, Observer).

> ✅ Progetto realizzato con **Java 17**.

---

## 📦 Struttura del Progetto

Il progetto è suddiviso in package funzionali per garantire modularità e manutenibilità:

### 🔹 `controller`
- `OrgChartManager`: controller principale che implementa il **pattern Singleton**. Coordina l’intero sistema.

### 🔹 `model`
- `OrganizationalUnit`: classe astratta per tutte le unità organizzative.
- `Board`, `Department`, `Group`: classi concrete per i tre tipi di unità.
- `Role`: rappresenta i ruoli associabili alle unità.
- `Employee`: rappresenta i dipendenti.
- `OrgChartValidator`: valida le regole di business (gerarchia, ruoli, ecc.).

### 🔹 `view`
- `OrgChartGUI`: finestra principale Swing.
- `UnitPanel`, `RolePanel`, `EmployeePanel`: pannelli dedicati alla visualizzazione e modifica.
- `ButtonEditor`, `ButtonRenderer`: componenti UI personalizzati.

### 🔹 `persistence`
- Implementazione del **pattern Strategy** per il salvataggio/caricamento dati.
- Supporta i formati: **JSON**, **CSV**, **DBMS (SQLite)**.

### 🔹 `factory`
- `StorageFactory`: factory per la creazione delle strategie di persistenza.
- `UnitFactory`: factory per la creazione delle unità organizzative.

### 🔹 `util`
- `Logger`: per la gestione centralizzata dei log.
- `ErrorManager`: per la gestione globale degli errori.

---

## 🧩 Design Pattern Utilizzati

- **Singleton** – `OrgChartManager`
- **Observer** – Per aggiornamento automatico delle viste.
- **Strategy** – Per la gestione flessibile della persistenza.
- **Factory** – Per la creazione di unità e strategie.
- **Composite** – Per rappresentare la struttura gerarchica.
- **MVC** – Per l’architettura generale dell’applicazione.

---

## 🧠 Funzionalità

- Creazione e gestione di un **organigramma gerarchico**
- Aggiunta/rimozione di unità organizzative: **Board**, **Department**, **Group**
- Definizione e assegnazione dinamica dei **ruoli predefiniti**
- Gestione dei **dipendenti** con supporto a ruoli multipli in più unità
- Validazione delle operazioni secondo **regole di business** chiare
- Salvataggio e caricamento in più formati
- Visualizzazione ad **albero** della struttura
- Interfaccia utente responsive e interattiva con **Swing**
- Logging ed error handling centralizzati

---

## 🛠️ Requisiti

- **Java 17**
- IDE consigliato: **IntelliJ IDEA**
- File `.jar` di libreria presente in `/lib`

---

## ⚙️ Installazione

### ✅ Su IntelliJ IDEA

1. **Apri IntelliJ IDEA**
2. Seleziona `File > New > Project from Existing Sources`
3. Importa il progetto come **progetto Java semplice**
4. Aggiungi la libreria esterna:
    - Vai su `File > Project Structure > Modules`
    - Tab **Dependencies** → clicca su **+ JARs or directories**
    - Seleziona il file `.jar` presente nella cartella `lib`
    - Assicurati che sia impostato come **Compile scope**
5. Compila ed esegui il progetto (`Run > Run 'OrgChartGUI'`)

---

## 📤 Esportazione e Salvataggio

Il sistema permette di salvare e caricare l’organigramma in:
- **Formato JSON**: `.json`
- **Formato CSV**: `.csv`
- **Database SQLite**: `.db`

La strategia di persistenza può essere selezionata dinamicamente in fase di utilizzo.

---

## ⚠️ Note Importanti

- Le **unità organizzative** devono rispettare una gerarchia rigida: i **Dipartimenti possono contenere Gruppi**, ma **i Gruppi non possono contenere altri Dipartimenti o Gruppi**.
- I **nomi delle unità** devono essere **univoci all’interno dello stesso livello gerarchico**.
- L’**eliminazione di un’unità** con contenuti comporta la comparsa di un **dialogo di conferma** nella GUI, che mostra quanti dipendenti e sottounità verranno eliminati. Se l’unità è vuota, viene eliminata direttamente.

---

## 📚 Licensing & Autori

Questo progetto è parte di un esercizio accademico basato sulla gestione di organigrammi e strutture aziendali.  
Per ogni utilizzo esterno o aziendale, si raccomanda di adattare la logica secondo le politiche interne.

---

