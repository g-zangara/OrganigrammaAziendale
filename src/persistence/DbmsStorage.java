package persistence;

import model.*;
import java.sql.*;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Implementation of StorageStrategy for database storage using SQLite
 */
public class DbmsStorage implements StorageStrategy {
    private String dbPath;

    /**
     * Constructor
     * @param dbPath Path to the SQLite database file
     */
    public DbmsStorage(String dbPath) {
        this.dbPath = dbPath;
    }

    @Override
    public boolean save(OrganizationalUnit rootUnit, String filePath) {
        try {
            // Determina il percorso del file del database
            String actualFilePath = (filePath != null) ? filePath : dbPath;
            File dbFile = new File(actualFilePath);

            // Elimina il file esistente se presente
            if (dbFile.exists()) {
                try {
                    // Crea prima una copia di backup
                    File backupFile = new File(dbFile.getAbsolutePath() + ".bak");
                    if (backupFile.exists()) {
                        backupFile.delete(); // Rimuovi backup precedente se esiste
                    }

                    // Copia il file originale
                    java.nio.file.Files.copy(dbFile.toPath(), backupFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Creato backup del database in: " + backupFile.getAbsolutePath());

                    // Ora è sicuro eliminare il file originale
                    if (!dbFile.delete()) {
                        System.err.println("AVVISO: Impossibile eliminare il file DB esistente.");
                        // Usa un nome file alternativo
                        dbFile = new File(dbFile.getAbsolutePath() + ".new");
                        System.out.println("Usando un nome file alternativo: " + dbFile.getAbsolutePath());

                        // Se anche il file alternativo esiste, eliminalo
                        if (dbFile.exists()) {
                            dbFile.delete();
                        }
                    } else {
                        System.out.println("File DB esistente eliminato correttamente.");
                    }
                } catch (Exception e) {
                    System.err.println("Errore durante l'eliminazione del file DB: " + e.getMessage());
                    // Continuiamo comunque, ma potrebbe causare errori
                    System.out.println("Proseguiamo comunque con la creazione/aggiornamento...");
                }
            }

            // Assicurati che la directory esista
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    System.err.println("AVVISO: Impossibile creare la directory per il database.");
                    return false;
                }
            }

            // Carica il driver SQLite JDBC
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                System.err.println("Driver SQLite JDBC non trovato: " + e.getMessage());
                e.printStackTrace();
                return false;
            }

            // Verifica che ci siano dipendenti da salvare
            List<Employee> allEmployees = new ArrayList<>();
            countEmployees(rootUnit, allEmployees);
            System.out.println("*** Aggiornamento tabella employees ***");
            System.out.println("Numero di dipendenti trovati nell'unità: " + allEmployees.size());

            if (allEmployees.isEmpty()) {
                System.out.println("ATTENZIONE: Nessun dipendente trovato da salvare!");
            }

            for (Employee emp : allEmployees) {
                System.out.println("Dipendente trovato: " + emp.getName() + " [" + emp.getUniqueId() + "]");
                for (Role role : emp.getRoles()) {
                    System.out.println(" - Ruolo: " + role.getName());
                }
            }

            // Crea un nuovo database fresco
            String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            System.out.println("Connessione al database: " + jdbcUrl);

            try {
                // Usare una nuova connessione per ogni fase principale
                // per assicurarci che le operazioni siano complete

                // 1. Creazione delle tabelle
                try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
                    // Imposta il timeout più lungo per operazioni
                    conn.setNetworkTimeout(null, 30000); // 30 secondi

                    // Attiva le foreign key constraints
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("PRAGMA foreign_keys = ON");
                    }

                    // Crea le tabelle (schermo pulito)
                    createTables(conn);
                    System.out.println("Tabelle del database create con successo.");
                } catch (SQLException e) {
                    System.err.println("Database error during creation: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }

                // 2. Salvataggio delle unità organizzative e ruoli
                try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
                    conn.setNetworkTimeout(null, 30000); // 30 secondi

                    // Salva le unità organizzative e ruoli
                    saveUnit(conn, rootUnit, null);
                    System.out.println("Unità e ruoli salvati con successo.");
                } catch (SQLException e) {
                    System.err.println("Database error during save: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }

                // 3. Verifica finale
                try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
                    // Verifica il numero di dipendenti salvati
                    int employeeCount = 0;
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employees")) {
                        if (rs.next()) {
                            employeeCount = rs.getInt(1);
                            System.out.println("Numero totale di dipendenti salvati: " + employeeCount);
                        }
                    }

                    // Verifica il numero di associazioni ruolo-dipendente
                    int roleAssignmentCount = 0;
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employee_roles")) {
                        if (rs.next()) {
                            roleAssignmentCount = rs.getInt(1);
                            System.out.println("Numero di associazioni ruolo-dipendente salvate: " + roleAssignmentCount);
                        }
                    }

                    // Verifica se i dati sono stati salvati correttamente
                    if (employeeCount == 0 || employeeCount != allEmployees.size()) {
                        System.err.println(String.format("ATTENZIONE: Previsti %d dipendenti, ma ne sono stati salvati %d",
                                allEmployees.size(), employeeCount));
                    }

                    // Controllo finale di integrità del database
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("PRAGMA integrity_check")) {
                        if (rs.next()) {
                            String result = rs.getString(1);
                            if ("ok".equalsIgnoreCase(result)) {
                                System.out.println("Verifica integrità database: OK");
                            } else {
                                System.err.println("Verifica integrità database: FALLITA - " + result);
                            }
                        }
                    }

                    System.out.println("Database salvato con successo in: " + dbFile.getAbsolutePath());
                    return true;
                } catch (SQLException e) {
                    System.err.println("Errore nella verifica finale del database: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Database error during save operation: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            System.err.println("Errore imprevisto durante il salvataggio: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Metodo per contare i dipendenti nell'albero organizzativo
    private void countEmployees(OrganizationalUnit unit, List<Employee> allEmployees) {
        // Cerca in tutti i ruoli di questa unità
        for (Role role : unit.getRoles()) {
            for (Employee emp : role.getEmployees()) {
                if (!allEmployees.contains(emp)) {
                    allEmployees.add(emp);
                }
            }
        }

        // Cerca anche nelle sottounità
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            countEmployees(subUnit, allEmployees);
        }
    }

    /**
     * Crea una struttura organizzativa di default
     * @param reason La ragione per cui viene creata la struttura di default
     * @return Una struttura organizzativa di default
     */
    private OrganizationalUnit createDefaultStructure(String reason) {
        System.out.println("Creazione struttura di default: " + reason);
        Board rootBoard = new Board("Root Board");
        rootBoard.addRole(new Role("Presidente", "Board President"));
        return rootBoard;
    }

    /**
     * Verifica se un file è un database SQLite valido
     * @param dbFile Il file da verificare
     * @return true se il file è un database SQLite valido, false altrimenti
     */
    private boolean isValidSqliteDatabase(File dbFile) {
        // Verifica esistenza e dimensione minima
        if (!dbFile.exists() || dbFile.length() < 100) {
            System.err.println("Il file " + dbFile.getAbsolutePath() + " non esiste o è troppo piccolo per essere un database SQLite valido.");
            return false;
        }

        // Prima verifica: check dell'header del file
        boolean validHeader = checkSqliteHeader(dbFile);
        if (!validHeader) {
            return false;
        }

        // Seconda verifica: prova a eseguire una query
        return testSqliteConnection(dbFile);
    }

    /**
     * Verifica se l'header del file corrisponde a quello di un database SQLite
     */
    private boolean checkSqliteHeader(File dbFile) {
        try (FileInputStream fis = new FileInputStream(dbFile)) {
            byte[] header = new byte[16];
            if (fis.read(header) != header.length) {
                System.err.println("Impossibile leggere l'header del file.");
                return false;
            }

            String headerStr = new String(header);
            if (!headerStr.startsWith("SQLite format 3")) {
                System.err.println("L'header del file non corrisponde a 'SQLite format 3'.");
                System.err.println("Header rilevato: " + headerStr.replaceAll("[\\x00-\\x1F]", "?"));
                return false;
            }

            return true;
        } catch (IOException e) {
            System.err.println("Errore nella lettura dell'header: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tenta di stabilire una connessione al database ed eseguire una query di test
     * @param dbFile Il file del database da testare
     * @return true se il test ha successo, false altrimenti
     */
    private boolean testSqliteConnection(File dbFile) {
        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        boolean connectionStatus = false;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // Tenta di aprire una connessione
            System.out.println("Tentativo di connessione al database SQLite: " + jdbcUrl);
            conn = DriverManager.getConnection(jdbcUrl);

            // Crea uno statement
            stmt = conn.createStatement();

            // Esegui una query semplice
            rs = stmt.executeQuery("SELECT 1");

            // Verifica se restituisce risultati
            connectionStatus = rs.next();

            System.out.println("Connessione e query di test al database SQLite riuscite.");
            return true;

        } catch (SQLException e) {
            // Verifica se si tratta dell'errore SQLITE_NOTADB
            if (e.getMessage().contains("not a database") ||
                    e.getMessage().contains("file is encrypted") ||
                    e.getMessage().contains("file is not a database") ||
                    e.getMessage().contains("malformed")) {
                System.err.println("Il file non è un database SQLite valido: " + e.getMessage());
            } else {
                System.err.println("Errore durante la connessione al database: " + e.getMessage());
            }
            return false;

        } finally {
            // Chiudi le risorse in ordine inverso (LIFO)
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Errore durante la chiusura delle risorse: " + e.getMessage());
            }
        }
    }

    @Override
    public OrganizationalUnit load(String filePath) {
        String jdbcUrl = "jdbc:sqlite:" + (filePath != null ? filePath : dbPath);

        try {
            // Carica il driver SQLite JDBC
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                System.err.println("Driver SQLite JDBC non trovato: " + e.getMessage());
                e.printStackTrace();
                return createDefaultStructure("Errore nel caricamento del driver JDBC");
            }

            // Verifica se il file esiste
            File dbFile = new File(filePath != null ? filePath : dbPath);
            if (!dbFile.exists()) {
                System.out.println("Il file del database non esiste: " + dbFile.getAbsolutePath());
                return createDefaultStructure("File database non trovato");
            }

            // Verifica se il file è un database SQLite valido
            if (dbFile.length() > 0) {
                try {
                    // Verifica che sia un database SQLite valido leggendo i primi byte
                    boolean isValid = isValidSqliteDatabase(dbFile);
                    if (!isValid) {
                        System.err.println("ATTENZIONE: " + dbFile.getAbsolutePath() + " non sembra essere un database SQLite valido.");
                        System.err.println("È necessario rigenerare il file database.");

                        // Rinomina il file corrotto per diagnostica futura
                        File corruptedFile = new File(dbFile.getAbsolutePath() + ".corrupted");
                        if (corruptedFile.exists()) {
                            corruptedFile.delete();
                        }
                        dbFile.renameTo(corruptedFile);
                        System.out.println("File database corrotto rinominato in: " + corruptedFile.getAbsolutePath());

                        return createDefaultStructure("Database corrotto");
                    }
                } catch (Exception e) {
                    System.err.println("Errore durante la verifica del file database: " + e.getMessage());
                    e.printStackTrace();
                    return createDefaultStructure("Errore verifica database");
                }
            }

            // Tentiamo di stabilire una connessione
            try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
                // Attiva le foreign key constraints
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                } catch (SQLException e) {
                    // Non critico se fallisce
                    System.err.println("Attenzione: impossibile attivare le foreign keys: " + e.getMessage());
                }

                // Verifica se le tabelle esistono
                if (!tablesExist(conn)) {
                    System.err.println("La struttura del database non è stata trovata. Creazione di una struttura di base.");
                    return createDefaultStructure("Struttura database non valida");
                }

                System.out.println("*** Caricamento dal database DBMS ***");

                // Carica le unità organizzative
                Map<Integer, OrganizationalUnit> unitsMap = new HashMap<>();
                Map<Integer, Integer> parentMap = new HashMap<>();
                loadUnits(conn, unitsMap, parentMap);
                System.out.println("Unità caricate: " + unitsMap.size());

                // Carica i ruoli
                Map<Integer, Role> rolesMap = new HashMap<>();
                Map<Integer, Integer> roleUnitMap = new HashMap<>();
                loadRoles(conn, rolesMap, roleUnitMap);
                System.out.println("Ruoli caricati: " + rolesMap.size());

                // Associa i ruoli alle unità
                for (Map.Entry<Integer, Integer> entry : roleUnitMap.entrySet()) {
                    int roleId = entry.getKey();
                    int unitId = entry.getValue();

                    Role role = rolesMap.get(roleId);
                    OrganizationalUnit unit = unitsMap.get(unitId);

                    if (role != null && unit != null) {
                        unit.addRole(role);
                        role.setUnit(unit);
                    }
                }

                // Carica gli impiegati e assegnali ai ruoli
                Map<String, Employee> employeesMap = new HashMap<>();
                loadEmployees(conn, rolesMap, employeesMap);
                System.out.println("Dipendenti caricati: " + employeesMap.size());

                // Costruisci la gerarchia delle unità
                System.out.println("\n*** Ricostruzione della gerarchia organizzativa ***");
                System.out.println("Relazioni parent-child da ricostruire: " + parentMap.size());

                for (Map.Entry<Integer, Integer> entry : parentMap.entrySet()) {
                    int unitId = entry.getKey();
                    int parentId = entry.getValue();

                    OrganizationalUnit unit = unitsMap.get(unitId);
                    OrganizationalUnit parent = unitsMap.get(parentId);

                    if (unit != null && parent != null) {
                        System.out.println("Collegando " + unit.getName() + " (ID: " + unitId +
                                ") come figlio di " + parent.getName() + " (ID: " + parentId + ")");
                        parent.addSubUnit(unit);
                    } else {
                        System.out.println("ERRORE: Impossibile collegare l'unità " + unitId +
                                " al parent " + parentId + " (almeno uno dei due non è stato trovato)");
                    }
                }

                // Stampa un sommario della gerarchia ricostruita
                System.out.println("\nSommario della gerarchia ricostruita:");
                for (OrganizationalUnit unit : unitsMap.values()) {
                    if (unit.getParent() == null) {
                        System.out.println("Unità root: " + unit.getName() + " con " +
                                unit.getSubUnits().size() + " unità figlie");
                    }
                }

                // Qui sta il problema principale: la ricerca della radice avviene DOPO aver già ricostruito tutte le relazioni
                // A questo punto, tutte le unità già hanno il loro parent impostato, ma c'è un problema...
                // Non stiamo ritornando l'intera struttura, ma solo la radice. Le altre unità potrebbero non essere accessibili
                // Quindi dobbiamo ritornare tutto l'albero delle unità, non solo la radice
                System.out.println("\n*** Identificazione e verifica dell'albero completo ***");

                // Stampa tutte le unità e le loro relazioni per debug
                System.out.println("Verifica struttura di tutte le unità caricate:");
                for (OrganizationalUnit unit : unitsMap.values()) {
                    System.out.println("- Unità: " + unit.getName() + " (" + unit.getType() + ")");
                    if (unit.getParent() != null) {
                        System.out.println("  → Parent: " + unit.getParent().getName());
                    } else {
                        System.out.println("  → È una unità root");
                    }

                    System.out.println("  → Sottounità: " + unit.getSubUnits().size());
                    for (OrganizationalUnit subUnit : unit.getSubUnits()) {
                        System.out.println("    - " + subUnit.getName());
                    }

                    System.out.println("  → Ruoli: " + unit.getRoles().size());
                }

                // Trova la radice dell'albero (unità senza parent)
                OrganizationalUnit rootUnit = null;
                List<OrganizationalUnit> rootUnits = new ArrayList<>();

                for (OrganizationalUnit unit : unitsMap.values()) {
                    if (unit.getParent() == null) {
                        rootUnits.add(unit);
                        System.out.println("Trovata una unità root: " + unit.getName() + " (" + unit.getType() + ")");
                    }
                }

                System.out.println("Trovate " + rootUnits.size() + " unità root");

                if (rootUnits.size() == 1) {
                    // Se c'è una sola unità root, la usiamo come radice
                    rootUnit = rootUnits.get(0);
                    System.out.println("Utilizzo " + rootUnit.getName() + " come unità root principale");
                } else if (rootUnits.size() > 1) {
                    // Se ci sono più unità root, dobbiamo scegliere quella giusta
                    // Strategia: prima cerchiamo se c'è un'unità con nome specifico (es. "Acme Corp")
                    boolean foundNamedRoot = false;
                    for (OrganizationalUnit unit : rootUnits) {
                        String name = unit.getName().toLowerCase();
                        if (name.contains("acme") || name.contains("root") ||
                                name.contains("azienda") || name.contains("company") ||
                                name.contains("corp")) {
                            rootUnit = unit;
                            foundNamedRoot = true;
                            System.out.println("Trovata unità root con nome speciale: " + unit.getName());
                            break;
                        }
                    }

                    // Se non troviamo una radice con nome specifico, prendiamo la prima
                    if (!foundNamedRoot) {
                        rootUnit = rootUnits.get(0);
                        System.out.println("Nessuna radice con nome specifico, uso la prima: " + rootUnit.getName());
                    }
                } else if (!unitsMap.isEmpty()) {
                    // Non ci sono unità root ma il database non è vuoto - problema di relazioni
                    // Prendiamo la prima unità disponibile come root
                    rootUnit = unitsMap.values().iterator().next();
                    System.out.println("ATTENZIONE: Nessuna unità root trovata! Uso " + rootUnit.getName() + " come radice");
                }

                if (rootUnit != null) {
                    System.out.println("Struttura caricata: radice = " + rootUnit.getName());

                    // Ricostruisci le mappature dipendente-unità
                    rebuildEmployeeMapping(rootUnit, employeesMap);

                    // Statistiche dopo la ricostruzione
                    int totalRoles = 0;
                    int totalEmployees = 0;
                    Map<String, Employee> allEmployees = new HashMap<>();
                    countUnitsAndEmployees(rootUnit, totalRoles, allEmployees);
                    totalEmployees = allEmployees.size();

                    System.out.println("Dopo la ricostruzione: " + totalEmployees + " dipendenti");

                    return rootUnit;
                }

                // Crea un'unità di base se il database è vuoto
                Department rootDept = new Department("Root Department");
                rootDept.addRole(new Role("Manager", "Department Manager"));
                return rootDept;
            }
        } catch (Exception e) {
            System.err.println("Unexpected error during load: " + e.getMessage());
            e.printStackTrace();
        }

        // In caso di errore, restituisci una struttura di base con Board
        Board rootBoard = new Board("Root Board");
        rootBoard.addRole(new Role("Presidente", "Board President"));
        return rootBoard;
    }

    /**
     * Ricostruisce le relazioni tra dipendenti, ruoli e unità
     * @param unit L'unità organizzativa di partenza
     * @param employeesMap Mappa dei dipendenti per ID
     */
    private void rebuildEmployeeMapping(OrganizationalUnit unit, Map<String, Employee> employeesMap) {
        System.out.println("*** Ricostruzione della mappa dei dipendenti ***");
        rebuildUnitEmployeeMapping(unit, new HashMap<>());
    }

    private void rebuildUnitEmployeeMapping(OrganizationalUnit unit, Map<String, Employee> processedEmployees) {
        System.out.println("Elaborazione unità: " + unit.getName());

        // Per ogni ruolo in questa unità
        for (Role role : unit.getRoles()) {
            System.out.println("- Ruolo: " + role.getName() + ", dipendenti: " + role.getEmployees().size());

            // Per ogni dipendente nel ruolo
            for (Employee employee : role.getEmployees()) {
                // Assicurati che il dipendente abbia il ruolo
                if (!employee.getRoles().contains(role)) {
                    employee.addRole(role);
                }

                // Assicurati che il dipendente sia associato all'unità
                if (!employee.getUnits().contains(unit)) {
                    employee.addUnit(unit);
                }

                processedEmployees.put(employee.getUniqueId(), employee);
            }
        }

        // Elabora ricorsivamente le sottounità
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            rebuildUnitEmployeeMapping(subUnit, processedEmployees);
        }

        // Stampa la mappa dei dipendenti ricostruita
        if (unit.getParent() == null) {
            System.out.println("Mappa dipendenti ricostruita:");
            for (Map.Entry<String, Employee> entry : processedEmployees.entrySet()) {
                Employee emp = entry.getValue();
                for (OrganizationalUnit empUnit : emp.getUnits()) {
                    System.out.println("- Unità: " + empUnit.getName() + ", Dipendenti: " + empUnit.getRoles().size());
                    System.out.println("  + " + emp.getName() + " [" + emp.getUniqueId() + "]");
                    System.out.println("    Ruoli in questa unità: " + emp.getRoles().size());
                }
            }
            System.out.println("Totale dipendenti trovati: " + processedEmployees.size());
        }
    }

    /**
     * Conta le unità e i dipendenti in una struttura organizzativa
     */
    private void countUnitsAndEmployees(OrganizationalUnit unit, int totalRoles, Map<String, Employee> allEmployees) {
        // Conta i ruoli
        totalRoles += unit.getRoles().size();

        // Conta i dipendenti
        for (Role role : unit.getRoles()) {
            for (Employee employee : role.getEmployees()) {
                allEmployees.put(employee.getUniqueId(), employee);
            }
        }

        // Processa ricorsivamente le sottounità
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            countUnitsAndEmployees(subUnit, totalRoles, allEmployees);
        }
    }

    private boolean tablesExist(Connection conn) throws SQLException {
        try {
            // Primo tentativo: usare il metodo standard per verificare l'esistenza delle tabelle
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet tables = metaData.getTables(null, null, "units", null)) {
                if (tables.next()) {
                    return true;
                }
            }

            // Secondo tentativo: verificare l'esistenza con una query diretta
            try (Statement stmt = conn.createStatement()) {
                // Questo eseguirà una query che restituisce 0 righe se la tabella esiste
                stmt.executeQuery("SELECT 1 FROM units WHERE 1=0");
                return true;
            } catch (SQLException e) {
                // La tabella non esiste
                return false;
            }
        } catch (Exception e) {
            System.err.println("Errore durante la verifica delle tabelle: " + e.getMessage());
            // In caso di errore grave, meglio ricreare la struttura
            return false;
        }
    }

    private void createTables(Connection conn) throws SQLException {
        // Crea tabella units
        String createUnitsTable = "CREATE TABLE IF NOT EXISTS units (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "type TEXT NOT NULL," +
                "parent_id INTEGER" +
                ")";

        // Crea tabella roles
        String createRolesTable = "CREATE TABLE IF NOT EXISTS roles (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "unit_id INTEGER NOT NULL," +
                "FOREIGN KEY (unit_id) REFERENCES units(id)" +
                ")";

        // Crea tabella employees
        String createEmployeesTable = "CREATE TABLE IF NOT EXISTS employees (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT NOT NULL" +
                ")";

        // Crea tabella employee_roles (relazione molti-a-molti)
        String createEmployeeRolesTable = "CREATE TABLE IF NOT EXISTS employee_roles (" +
                "employee_id TEXT NOT NULL," +
                "role_id INTEGER NOT NULL," +
                "PRIMARY KEY (employee_id, role_id)," +
                "FOREIGN KEY (employee_id) REFERENCES employees(id)," +
                "FOREIGN KEY (role_id) REFERENCES roles(id)" +
                ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createUnitsTable);
            stmt.execute(createRolesTable);
            stmt.execute(createEmployeesTable);
            stmt.execute(createEmployeeRolesTable);
        }
    }

    private void clearDatabase(Connection conn) throws SQLException {
        String[] tables = {"employee_roles", "employees", "roles", "units"};

        try (Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                stmt.execute("DELETE FROM " + table);
            }
        }
    }

    private void saveUnit(Connection conn, OrganizationalUnit unit, Integer parentId) throws SQLException {
        // Genera un ID univoco basato su hashCode (usiamo un valore assoluto per evitare ID negativi)
        int unitId = Math.abs(unit.hashCode());

        // Determina il tipo di unità organizzativa basandosi su getType() dell'unità
        // Ma convertiamo in maiuscolo per uniformità
        String unitType = unit.getType().toUpperCase();

        System.out.println("Salvando unità: " + unit.getName() + " (ID: " + unitId +
                ", Tipo Java: " + unit.getType() + ", Tipo DB: " + unitType +
                ", Istanza: " + (unit instanceof Group ? "Group" :
                unit instanceof Department ? "Department" :
                        unit instanceof Board ? "Board" : "Unknown"));
        if (parentId != null) {
            System.out.println(" - Con parent ID: " + parentId);
        } else {
            System.out.println(" - Unità root (nessun parent)");
        }

        // Controlla se l'unità esiste già nel database
        boolean unitExists = false;
        try (PreparedStatement checkStmt = conn.prepareStatement("SELECT 1 FROM units WHERE id = ?")) {
            checkStmt.setInt(1, unitId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                unitExists = rs.next();
            }
        }

        if (unitExists) {
            // Aggiorna l'unità esistente
            String updateUnit = "UPDATE units SET name = ?, description = ?, type = ?, parent_id = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateUnit)) {
                pstmt.setString(1, unit.getName());
                pstmt.setString(2, unit.getDescription());
                pstmt.setString(3, unitType);
                if (parentId != null) {
                    pstmt.setInt(4, parentId);
                } else {
                    pstmt.setNull(4, Types.INTEGER);
                }
                pstmt.setInt(5, unitId);
                int result = pstmt.executeUpdate();
                System.out.println(" - Unità aggiornata: " + result + " righe modificate");
            }
        } else {
            // Inserisci la nuova unità
            String insertUnit = "INSERT INTO units (id, name, description, type, parent_id) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertUnit)) {
                pstmt.setInt(1, unitId);
                pstmt.setString(2, unit.getName());
                pstmt.setString(3, unit.getDescription());
                pstmt.setString(4, unitType);
                if (parentId != null) {
                    pstmt.setInt(5, parentId);
                } else {
                    pstmt.setNull(5, Types.INTEGER);
                }
                int result = pstmt.executeUpdate();
                System.out.println(" - Nuova unità inserita: " + result + " righe inserite");
            }
        }

        // Salva i ruoli dell'unità
        System.out.println(" - Salvando " + unit.getRoles().size() + " ruoli per questa unità");
        for (Role role : unit.getRoles()) {
            saveRole(conn, role, unitId);
        }

        // Salva le sottounità ricorsivamente
        System.out.println(" - Salvando " + unit.getSubUnits().size() + " sottounità");
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            // Passa l'ID dell'unità corrente come parentId per la sottounità
            saveUnit(conn, subUnit, unitId);
        }
    }

    private void saveRole(Connection conn, Role role, int unitId) throws SQLException {
        int roleId = Math.abs(role.hashCode()); // Usa valore assoluto per evitare ID negativi

        System.out.println("Salvando ruolo: " + role.getName() + " (ID: " + roleId + ")");
        System.out.println(" - Per l'unità con ID: " + unitId);
        System.out.println(" - Dipendenti associati: " + role.getEmployees().size());

        // Controlla se il ruolo esiste già
        boolean roleExists = false;
        try (PreparedStatement checkStmt = conn.prepareStatement("SELECT 1 FROM roles WHERE id = ?")) {
            checkStmt.setInt(1, roleId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                roleExists = rs.next();
            }
        }

        if (roleExists) {
            // Aggiorna il ruolo esistente
            String updateRole = "UPDATE roles SET name = ?, description = ?, unit_id = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateRole)) {
                pstmt.setString(1, role.getName());
                pstmt.setString(2, role.getDescription());
                pstmt.setInt(3, unitId);
                pstmt.setInt(4, roleId);
                int result = pstmt.executeUpdate();
                System.out.println(" - Ruolo aggiornato: " + result + " righe modificate");
            }
        } else {
            // Inserisci il nuovo ruolo
            String insertRole = "INSERT INTO roles (id, name, description, unit_id) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertRole)) {
                pstmt.setInt(1, roleId);
                pstmt.setString(2, role.getName());
                pstmt.setString(3, role.getDescription());
                pstmt.setInt(4, unitId);
                int result = pstmt.executeUpdate();
                System.out.println(" - Nuovo ruolo inserito: " + result + " righe inserite");
            }
        }

        // Salva gli impiegati associati al ruolo
        for (Employee employee : role.getEmployees()) {
            saveEmployee(conn, employee, roleId);
        }
    }

    private void saveEmployee(Connection conn, Employee employee, int roleId) throws SQLException {
        String employeeId = employee.getUniqueId();

        System.out.println("Salvataggio dipendente: " + employee.getName() + " con ID: " + employeeId);

        // Inserisci l'impiegato (gestisci il caso in cui esista già)
        String insertEmployee = "INSERT OR IGNORE INTO employees (id, name) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertEmployee)) {
            pstmt.setString(1, employeeId);
            pstmt.setString(2, employee.getName());
            int result = pstmt.executeUpdate();
            System.out.println("Risultato insert dipendente: " + result + " righe inserite");
        }

        // Crea l'associazione impiegato-ruolo
        String insertEmployeeRole = "INSERT OR IGNORE INTO employee_roles (employee_id, role_id) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertEmployeeRole)) {
            pstmt.setString(1, employeeId);
            pstmt.setInt(2, roleId);
            int result = pstmt.executeUpdate();
            System.out.println("Risultato insert employee_role: " + result + " righe inserite");
        }

        // Verifica che l'inserimento sia avvenuto correttamente
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employees WHERE id = '" + employeeId + "'")) {
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("Verificato: " + count + " dipendenti con ID " + employeeId);
            }
        }
    }

    private void loadUnits(Connection conn, Map<Integer, OrganizationalUnit> unitsMap, Map<Integer, Integer> parentMap) throws SQLException {
        String query = "SELECT id, name, description, type, parent_id FROM units";

        System.out.println("Caricamento unità organizzative dal database...");
        System.out.println("Iniziando caricamento unità dal database");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                String type = rs.getString("type");

                OrganizationalUnit unit;
                String upperType = type.toUpperCase();

                System.out.println("Elaborando unità dal DB: " + name + " (ID: " + id + ", Tipo: " + type + ")");

                // Confrontiamo con i tipi convertiti in maiuscolo per coerenza
                if ("DEPARTMENT".equals(upperType)) {
                    unit = new Department(name);
                    System.out.println("Caricato dipartimento: " + name + " (ID: " + id + ")");
                } else if ("GROUP".equals(upperType)) {
                    unit = new Group(name);
                    System.out.println("Caricato gruppo: " + name + " (ID: " + id + ")");
                } else if ("BOARD".equals(upperType)) {
                    unit = new Board(name);
                    System.out.println("Caricato board: " + name + " (ID: " + id + ")");
                    System.out.println("*** BOARD RILEVATO *** - " + name + " (ID: " + id + ")");
                } else {
                    // Se il tipo non è riconosciuto, facciamo un secondo tentativo con il nome del tipo
                    System.out.println("Tentativo di riconoscimento del tipo: " + type);
                    if (type.toUpperCase().contains("GROUP")) {
                        unit = new Group(name);
                        System.out.println("Caricato gruppo (riconoscimento alternativo): " + name);
                    } else if (type.toUpperCase().contains("BOARD")) {
                        unit = new Board(name);
                        System.out.println("Caricato board (riconoscimento alternativo): " + name);
                        System.out.println("*** BOARD RILEVATO (alt) *** - " + name + " (ID: " + id + ")");
                    } else {
                        unit = new Department(name); // Default fallback
                        System.out.println("Caricata unità di tipo sconosciuto (default=Department): " + name + " (ID: " + id + ")");
                    }
                }

                unit.setDescription(description);
                unitsMap.put(id, unit);

                // Salva il riferimento al parent per costruire la gerarchia dopo
                int parentId = rs.getInt("parent_id");
                if (!rs.wasNull()) {
                    parentMap.put(id, parentId);
                    System.out.println(" - Questa unità ha un parent con ID: " + parentId);
                } else {
                    System.out.println(" - Questa è un'unità root (nessun parent)");
                }
            }

            System.out.println("Totale unità caricate: " + unitsMap.size());
            System.out.println("Relazioni parent-child: " + parentMap.size());
        }
    }

    private void loadRoles(Connection conn, Map<Integer, Role> rolesMap, Map<Integer, Integer> roleUnitMap) throws SQLException {
        String query = "SELECT id, name, description, unit_id FROM roles";

        System.out.println("Caricamento ruoli dal database...");

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                int unitId = rs.getInt("unit_id");

                Role role = new Role(name, description);
                rolesMap.put(id, role);
                roleUnitMap.put(id, unitId);

                System.out.println("Caricato ruolo: " + name + " (ID: " + id + ") per unità " + unitId);
            }

            System.out.println("Totale ruoli caricati: " + rolesMap.size());
        }

        // Conta il numero totale di ruoli nel database per verifica
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM roles")) {
            if (rs.next()) {
                int count = rs.getInt(1);
                if (count != rolesMap.size()) {
                    System.out.println("ATTENZIONE: Discrepanza nel numero di ruoli - DB: " + count +
                            ", Caricati: " + rolesMap.size());
                } else {
                    System.out.println("Conteggio ruoli coerente: " + count);
                }
            }
        }
    }

    private void loadEmployees(Connection conn, Map<Integer, Role> rolesMap, Map<String, Employee> employeesMap) throws SQLException {
        System.out.println("Caricamento dipendenti dal database...");

        // Carica tutti gli impiegati in una mappa
        String employeeQuery = "SELECT id, name FROM employees";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(employeeQuery)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");

                Employee employee = new Employee(name);
                // Usiamo setUniqueId in modo che corrisponda all'ID del database
                employee.setUniqueId(id);
                employeesMap.put(id, employee);
                System.out.println("Trovato dipendente: " + name + " [" + id + "]");
            }
        }

        // Conta il numero totale di impiegati
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employees")) {
            if (rs.next()) {
                System.out.println("Numero totale di dipendenti nel database: " + rs.getInt(1));
            }
        }

        // Conta le associazioni dipendente-ruolo
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM employee_roles")) {
            if (rs.next()) {
                System.out.println("Numero totale di associazioni dipendente-ruolo nel database: " + rs.getInt(1));
            }
        }

        // Carica le associazioni impiegato-ruolo
        String relationQuery = "SELECT employee_id, role_id FROM employee_roles";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(relationQuery)) {

            int assignmentsCount = 0;
            while (rs.next()) {
                String employeeId = rs.getString("employee_id");
                int roleId = rs.getInt("role_id");

                System.out.println("Trovata associazione: dipendente [" + employeeId + "] - ruolo [" + roleId + "]");

                Employee employee = employeesMap.get(employeeId);
                Role role = rolesMap.get(roleId);

                if (employee != null && role != null) {
                    // Crea l'associazione bidirezionale
                    if (!role.getEmployees().contains(employee)) {
                        role.addEmployee(employee);
                    }

                    if (!employee.getRoles().contains(role)) {
                        employee.addRole(role);
                    }

                    // Aggiungi l'unità al dipendente
                    if (role.getUnit() != null) {
                        employee.addUnit(role.getUnit());
                    }

                    assignmentsCount++;
                    System.out.println("Associato dipendente " + employee.getName() +
                            " con ID " + employee.getUniqueId() +
                            " al ruolo " + role.getName());
                } else {
                    System.out.println("ERRORE: Impossibile trovare dipendente [" + employeeId +
                            "] o ruolo [" + roleId + "]");
                }
            }

            System.out.println("Associazioni create con successo: " + assignmentsCount);
        }
    }
}