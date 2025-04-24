package persistence;

import model.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Implementation of StorageStrategy using CSV format.
 * This class provides methods to save and load organizational structures using CSV files.
 */
public class CsvStorage implements StorageStrategy {

    // Constants for CSV column headers and separators
    private static final String CSV_SEPARATOR = ",";
    private static final String UNIT_HEADER = "TYPE,ID,NAME,DESCRIPTION,PARENT_ID";
    private static final String ROLE_HEADER = "UNIT_ID,NAME,DESCRIPTION";
    private static final String EMPLOYEE_HEADER = "ID,NAME";
    private static final String ASSIGNMENT_HEADER = "EMPLOYEE_ID,ROLE_NAME,UNIT_ID";
    private static final String SECTION_MARKER = "#SECTION:";

    /**
     * Save the organization chart to a single CSV file with sections
     * @param rootUnit The root organizational unit to save
     * @param filePath The file path to save to
     * @return True if the save was successful, false otherwise
     */
    @Override
    public boolean save(OrganizationalUnit rootUnit, String filePath) {
        try {
            // Ensure the file path is valid - don't try to create directories if path has no parent
            Path path = Paths.get(filePath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Elimina il file se esiste per evitare problemi di formato
            File csvFile = new File(filePath);
            if (csvFile.exists()) {
                csvFile.delete();
                System.out.println("File CSV esistente eliminato: " + filePath);
            }

            // Create data structures to hold all entities
            Map<String, OrganizationalUnit> units = new HashMap<>();
            List<Role> allRoles = new ArrayList<>();
            List<Employee> allEmployees = new ArrayList<>();
            List<String[]> assignments = new ArrayList<>();

            // Collect all data through traversal
            collectData(rootUnit, null, units, allRoles, allEmployees, assignments);

            // Write all data to a single CSV file with sections
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(filePath), StandardCharsets.UTF_8);
                 BufferedWriter buffWriter = new BufferedWriter(writer);
                 PrintWriter printWriter = new PrintWriter(buffWriter)) {

                System.out.println("Salvando CSV in: " + filePath);

                // Write units section
                printWriter.println(SECTION_MARKER + " UNITS");
                printWriter.println(UNIT_HEADER);
                for (Map.Entry<String, OrganizationalUnit> entry : units.entrySet()) {
                    OrganizationalUnit unit = entry.getValue();
                    // Rigeneriamo sempre l'ID univoco usando il metodo getUnitId
                    String unitId = getUnitId(unit);
                    String unitType = unit instanceof Department ? "Department" :
                            (unit instanceof Group ? "Group" : "Unit");
                    String parentId = unit.getParent() != null ? getUnitId(unit.getParent()) : "";

                    String line = String.join(CSV_SEPARATOR,
                            escapeCsv(unitType),
                            escapeCsv(unitId),
                            escapeCsv(unit.getName()),
                            escapeCsv(unit.getDescription()),
                            escapeCsv(parentId)
                    );

                    printWriter.println(line);
                }

                // Write roles section
                printWriter.println(SECTION_MARKER + " ROLES");
                printWriter.println(ROLE_HEADER);
                for (Role role : allRoles) {
                    OrganizationalUnit unit = role.getUnit();
                    if (unit != null) {
                        String line = String.join(CSV_SEPARATOR,
                                escapeCsv(getUnitId(unit)),
                                escapeCsv(role.getName()),
                                escapeCsv(role.getDescription())
                        );

                        printWriter.println(line);
                    }
                }

                // Write employees section
                printWriter.println(SECTION_MARKER + " EMPLOYEES");
                printWriter.println(EMPLOYEE_HEADER);
                for (Employee employee : allEmployees) {
                    String line = String.join(CSV_SEPARATOR,
                            escapeCsv(employee.getUniqueId()),
                            escapeCsv(employee.getName())
                    );

                    printWriter.println(line);
                }

                // Write assignments section
                printWriter.println(SECTION_MARKER + " ASSIGNMENTS");
                printWriter.println(ASSIGNMENT_HEADER);
                for (String[] assignment : assignments) {
                    String line = String.join(CSV_SEPARATOR,
                            escapeCsv(assignment[0]),
                            escapeCsv(assignment[1]),
                            escapeCsv(assignment[2])
                    );

                    printWriter.println(line);
                }

                printWriter.flush();
                System.out.println("File CSV salvato con successo in formato testuale.");
            }

            // Verifica che il file sia in formato testo leggendo le prime righe
            try {
                List<String> firstLines = Files.readAllLines(path, StandardCharsets.UTF_8).subList(0,
                        Math.min(5, (int)Files.lines(path, StandardCharsets.UTF_8).count()));
                boolean isTextFormat = firstLines.stream()
                        .anyMatch(line -> line.contains(SECTION_MARKER) ||
                                line.equals(UNIT_HEADER) ||
                                line.equals(ROLE_HEADER));

                if (!isTextFormat) {
                    System.err.println("ATTENZIONE: Il file CSV potrebbe non essere stato salvato in formato testuale!");
                } else {
                    System.out.println("Verifica formato: Il file è in formato CSV testuale.");
                }
            } catch (Exception e) {
                System.err.println("Errore durante la verifica del formato del file: " + e.getMessage());
            }

            return true;
        } catch (IOException e) {
            System.err.println("Error saving to CSV file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load an organization chart from a single CSV file with sections
     * @param filePath The file path to load from
     * @return The root organizational unit, or null if loading failed
     */
    @Override
    public OrganizationalUnit load(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                System.err.println("File CSV non trovato: " + filePath);
                return null;
            }

            // Check if file is a proper CSV
            try {
                // Prima verifica: controlla se il file contiene caratteri binari
                byte[] fileBytes = Files.readAllBytes(path);
                // Verifica se contiene byte non stampabili o che potrebbero essere binari
                // (escludi i caratteri di controllo comuni come CR, LF, TAB)
                boolean isBinary = false;

                // Controllo aggiornato: invece di scartare tutti i caratteri non-ASCII,
                // veritica solo che non sia un file binario (serializzato Java)
                // Consideriamo validi i caratteri estesi come lettere accentate italiane

                // Contatore dei byte sospetti
                int suspiciousBytes = 0;
                int checkLength = Math.min(400, fileBytes.length);

                for (int i = 0; i < checkLength; i++) {
                    byte b = fileBytes[i];
                    // Caratteri di controllo eccetto whitespace
                    boolean isControlChar = (b < 32 && b != 9 && b != 10 && b != 13);

                    // Nel caso di caratteri non stampabili che non sono caratteri di controllo,
                    // li consideriamo sospetti solo se non sono parte di un carattere UTF-8 valido
                    if (isControlChar) {
                        suspiciousBytes++;
                        if (suspiciousBytes > 5) { // Permettiamo fino a 5 byte sospetti (margine di tolleranza)
                            System.err.println("ATTENZIONE: Il file " + filePath + " contiene dati binari e non è un CSV valido.");
                            System.err.println("Troppi byte sospetti trovati.");
                            return null;
                        }
                    }
                }

                // Verifica se è un Java serialized object
                String fileStartBytes = new String(fileBytes, 0, Math.min(30, fileBytes.length), StandardCharsets.UTF_8);
                if (fileStartBytes.contains("sr") || fileStartBytes.contains("model.") ||
                        fileStartBytes.contains("java.util") || fileStartBytes.contains("xp")) {
                    System.err.println("ATTENZIONE: Il file " + filePath + " sembra essere in formato binario serializzato Java, non CSV.");
                    System.err.println("È necessario generare un nuovo file CSV valido.");
                    return null;
                }

                // Leggi le prime 10 righe per verificare che sia un file CSV
                String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
                String[] lines = fileContent.split("\\r?\\n");

                if (lines.length < 2) {
                    System.err.println("ATTENZIONE: Il file CSV contiene meno di 2 righe, non è un formato valido.");
                    return null;
                }

                List<String> sampleLines = Arrays.asList(lines).subList(0, Math.min(10, lines.length));

                boolean foundSection = false;
                boolean foundHeader = false;
                boolean foundCsvSeparator = false;

                // Verifica delle caratteristiche CSV
                for (String line : sampleLines) {
                    if (line.startsWith(SECTION_MARKER)) {
                        foundSection = true;
                    }
                    if (line.equals(UNIT_HEADER) || line.equals(ROLE_HEADER) ||
                            line.equals(EMPLOYEE_HEADER) || line.equals(ASSIGNMENT_HEADER)) {
                        foundHeader = true;
                    }
                    if (line.contains(CSV_SEPARATOR)) {
                        foundCsvSeparator = true;
                    }
                }

                boolean isCsvFormat = foundSection && (foundHeader || foundCsvSeparator);
                if (!isCsvFormat) {
                    // Non sembra un file CSV valido
                    System.err.println("ATTENZIONE: Il file " + filePath + " non sembra essere un file CSV valido.");
                    System.err.println("Caratteristiche cercate: Sezioni (" + foundSection +
                            "), Intestazioni (" + foundHeader +
                            "), Separatori CSV (" + foundCsvSeparator + ")");

                    // Mostra l'inizio del file per debug
                    if (sampleLines.size() > 0) {
                        System.err.println("Prime righe del file:");
                        for (int i = 0; i < Math.min(5, sampleLines.size()); i++) {
                            System.err.println("  " + (i+1) + ": " + sampleLines.get(i));
                        }
                    }

                    System.err.println("È necessario generare un nuovo file CSV nel formato corretto.");
                    return null;
                }

                System.out.println("File CSV valido rilevato.");
            } catch (Exception e) {
                System.err.println("Errore durante l'analisi del file CSV: " + e.getMessage());
                e.printStackTrace();
                return null;
            }

            // Read the file with UTF-8 encoding
            List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (allLines.isEmpty()) {
                System.err.println("Il file CSV è vuoto: " + filePath);
                return null;
            }

            System.out.println("Caricamento file CSV: " + filePath);
            System.out.println("Numero di righe nel file: " + allLines.size());

            // Parse the CSV file by sections
            Map<String, List<String>> sections = new HashMap<>();
            String currentSection = "";
            List<String> currentSectionLines = new ArrayList<>();

            for (String line : allLines) {
                if (line.startsWith(SECTION_MARKER)) {
                    // Save current section if we have one
                    if (!currentSection.isEmpty() && !currentSectionLines.isEmpty()) {
                        sections.put(currentSection, new ArrayList<>(currentSectionLines));
                    }

                    // Start a new section
                    currentSection = line.substring(SECTION_MARKER.length()).trim();
                    currentSectionLines.clear();
                    System.out.println("Trovata sezione: " + currentSection);
                } else {
                    currentSectionLines.add(line);
                }
            }

            // Save the last section
            if (!currentSection.isEmpty() && !currentSectionLines.isEmpty()) {
                sections.put(currentSection, new ArrayList<>(currentSectionLines));
            }

            System.out.println("Numero di sezioni trovate: " + sections.size());
            for (String section : sections.keySet()) {
                System.out.println(" - Sezione " + section + ": " + sections.get(section).size() + " righe");
            }

            // Process all sections
            Map<String, OrganizationalUnit> units = new HashMap<>();
            Map<String, Employee> employees = new HashMap<>();

            // Process UNITS section
            if (sections.containsKey("UNITS")) {
                units = readUnitsFromSection(sections.get("UNITS"));
                System.out.println("Unità organizzative caricate: " + units.size());
            } else {
                System.err.println("Sezione UNITS non trovata nel file CSV");
            }

            if (units.isEmpty()) {
                System.err.println("Nessuna unità organizzativa trovata nel file CSV");
                return null;
            }

            // Process ROLES section
            if (sections.containsKey("ROLES")) {
                readRolesFromSection(sections.get("ROLES"), units);
            } else {
                System.err.println("Sezione ROLES non trovata nel file CSV");
            }

            // Process EMPLOYEES section
            if (sections.containsKey("EMPLOYEES")) {
                employees = readEmployeesFromSection(sections.get("EMPLOYEES"));
                System.out.println("Dipendenti caricati: " + employees.size());
            } else {
                System.err.println("Sezione EMPLOYEES non trovata nel file CSV");
            }

            // Process ASSIGNMENTS section
            if (sections.containsKey("ASSIGNMENTS")) {
                readAssignmentsFromSection(sections.get("ASSIGNMENTS"), units, employees);
            } else {
                System.err.println("Sezione ASSIGNMENTS non trovata nel file CSV");
            }

            // Find the root unit (has no parent)
            OrganizationalUnit rootUnit = findRootUnit(units);
            if (rootUnit != null) {
                System.out.println("Unità radice trovata: " + rootUnit.getName());

                // Rebuild employee-unit mappings
                rebuildEmployeeUnitMappings(rootUnit);
            } else {
                System.err.println("Nessuna unità radice trovata nella struttura organizzativa");
            }

            return rootUnit;
        } catch (IOException e) {
            System.err.println("Error loading from CSV file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Ricostruisce le mappature tra dipendenti e unità
     */
    private void rebuildEmployeeUnitMappings(OrganizationalUnit unit) {
        System.out.println("*** Ricostruzione della mappa dei dipendenti ***");
        System.out.println("Elaborazione unità: " + unit.getName());

        Map<String, List<String>> employeeRolesMap = new HashMap<>();
        int totalEmployeesAdded = 0;

        // Verifica le relazioni tra ruoli e dipendenti in questa unità
        for (Role role : unit.getRoles()) {
            System.out.println("- Ruolo: " + role.getName() + ", dipendenti: " + role.getEmployees().size());

            for (Employee employee : role.getEmployees()) {
                // Log dei dipendenti trovati
                System.out.println("  + Dipendente: " + employee.getName() + " [" + employee.getUniqueId() + "]");

                // Assicura che il dipendente abbia questo ruolo
                if (!employee.getRoles().contains(role)) {
                    employee.addRole(role);
                    System.out.println("    * Aggiunto ruolo " + role.getName() + " al dipendente " + employee.getName());
                }

                // Assicura che il dipendente sia associato a questa unità
                if (!employee.getUnits().contains(unit)) {
                    employee.addUnit(unit);
                    totalEmployeesAdded++;
                    System.out.println("    * Aggiunta unità " + unit.getName() + " al dipendente " + employee.getName());
                }

                // Registra i ruoli di questo dipendente per il rapporto finale
                employeeRolesMap.computeIfAbsent(employee.getUniqueId(), k -> new ArrayList<>())
                        .add(role.getName());
            }
        }

        if (totalEmployeesAdded > 0) {
            System.out.println("Aggiunti " + totalEmployeesAdded + " dipendenti all'unità " + unit.getName());
        }

        // Processa ricorsivamente le sottounità
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            rebuildEmployeeUnitMappings(subUnit);
        }

        // Stampa rapporto finale per questa unità e i suoi dipendenti
        if (unit.getParent() == null) { // Solo per l'unità radice
            System.out.println("Mappa dipendenti ricostruita:");

            // Elenco delle unità e dei dipendenti
            Map<OrganizationalUnit, List<Employee>> unitEmployeesMap = new HashMap<>();

            // Costruisci la mappa unità -> dipendenti
            for (OrganizationalUnit u : getAllUnits(unit)) {
                List<Employee> unitEmployees = new ArrayList<>();

                for (Role r : u.getRoles()) {
                    for (Employee e : r.getEmployees()) {
                        if (!unitEmployees.contains(e)) {
                            unitEmployees.add(e);
                        }
                    }
                }

                if (!unitEmployees.isEmpty()) {
                    unitEmployeesMap.put(u, unitEmployees);
                }
            }

            // Stampa il rapporto
            for (Map.Entry<OrganizationalUnit, List<Employee>> entry : unitEmployeesMap.entrySet()) {
                OrganizationalUnit u = entry.getKey();
                List<Employee> employees = entry.getValue();

                System.out.println("- Unità: " + u.getName() + ", Dipendenti: " + employees.size());
                for (Employee e : employees) {
                    System.out.println("  + " + e.getName() + " [" + e.getUniqueId() + "]");
                    System.out.println("    Ruoli in questa unità: " + countRolesInUnit(e, u));
                }
            }

            // Conteggio totale dipendenti unici
            Set<Employee> allEmployees = new HashSet<>();
            for (List<Employee> employees : unitEmployeesMap.values()) {
                allEmployees.addAll(employees);
            }

            System.out.println("Totale dipendenti trovati: " + allEmployees.size());
        }
    }

    /**
     * Conta quanti ruoli ha un dipendente in una specifica unità
     */
    private int countRolesInUnit(Employee employee, OrganizationalUnit unit) {
        int count = 0;
        for (Role role : employee.getRoles()) {
            if (role.getUnit() == unit) {
                count++;
            }
        }
        return count;
    }

    /**
     * Ottiene tutte le unità in una gerarchia
     */
    private List<OrganizationalUnit> getAllUnits(OrganizationalUnit root) {
        List<OrganizationalUnit> result = new ArrayList<>();
        result.add(root);

        for (OrganizationalUnit subUnit : root.getSubUnits()) {
            result.addAll(getAllUnits(subUnit));
        }

        return result;
    }

    /**
     * Read units from a section of lines
     */
    private Map<String, OrganizationalUnit> readUnitsFromSection(List<String> lines) {
        Map<String, OrganizationalUnit> units = new HashMap<>();
        Map<String, String> parentRelations = new HashMap<>();

        if (lines.size() <= 1) {
            return units;
        }

        System.out.println("Elaborazione unità dalla sezione UNITS...");

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = splitCsvLine(lines.get(i));
            if (parts.length < 5) {
                System.out.println("ATTENZIONE: Riga " + i + " della sezione UNITS non ha abbastanza campi: " + Arrays.toString(parts));
                continue;
            }

            String type = parts[0];
            String id = parts[1];
            String name = parts[2];
            String description = parts[3];
            String parentId = parts[4];

            // Create the unit based on type
            OrganizationalUnit unit;
            if ("Department".equals(type)) {
                unit = new Department(name);
                System.out.println("Creato Department: " + name + " [ID: " + id + "]");
            } else if ("Group".equals(type)) {
                unit = new Group(name);
                System.out.println("Creato Group: " + name + " [ID: " + id + "]");
            } else {
                unit = new Department(name); // Default to Department
                System.out.println("Creato Department (default): " + name + " [ID: " + id + "]");
            }
            unit.setDescription(description);

            units.put(id, unit);
            if (!parentId.isEmpty()) {
                parentRelations.put(id, parentId);
                System.out.println("Relazione parent-child: " + name + " -> Parent ID: " + parentId);
            } else {
                System.out.println("Unità radice identificata: " + name);
            }
        }

        System.out.println("Totale unità create: " + units.size());
        System.out.println("Relazioni parent-child da stabilire: " + parentRelations.size());

        // Set up parent-child relationships
        for (Map.Entry<String, String> relation : parentRelations.entrySet()) {
            String childId = relation.getKey();
            String parentId = relation.getValue();

            OrganizationalUnit child = units.get(childId);
            OrganizationalUnit parent = units.get(parentId);

            if (child != null && parent != null) {
                System.out.println("Stabilita relazione parent-child: " + parent.getName() + " -> " + child.getName());
                parent.addSubUnit(child);
            } else {
                System.out.println("ERRORE: Impossibile stabilire relazione parent-child - Child ID: " + childId + ", Parent ID: " + parentId);
            }
        }

        // Verifica delle relazioni create
        int totalSubUnits = 0;
        for (OrganizationalUnit unit : units.values()) {
            int numSubUnits = unit.getSubUnits().size();
            totalSubUnits += numSubUnits;
            System.out.println("Unità: " + unit.getName() + " ha " + numSubUnits + " sottounità");

            for (OrganizationalUnit subUnit : unit.getSubUnits()) {
                System.out.println("  - Sottounità: " + subUnit.getName());
            }
        }
        System.out.println("Totale relazioni parent-child stabilite: " + totalSubUnits);

        return units;
    }

    /**
     * Read roles from a section of lines
     */
    private void readRolesFromSection(List<String> lines, Map<String, OrganizationalUnit> units) {
        if (lines.size() <= 1) {
            System.out.println("Nessun ruolo trovato nella sezione ROLES.");
            return;
        }

        System.out.println("Elaborazione ruoli dalla sezione ROLES...");
        System.out.println("Numero di righe di ruoli: " + (lines.size() - 1));

        int rolesCount = 0;

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = splitCsvLine(lines.get(i));
            if (parts.length < 3) {
                System.out.println("ATTENZIONE: Riga " + i + " della sezione ROLES non ha abbastanza campi.");
                continue;
            }

            String unitId = parts[0];
            String name = parts[1];
            String description = parts[2];

            OrganizationalUnit unit = units.get(unitId);
            if (unit != null) {
                Role role = new Role(name, description);
                unit.addRole(role);
                rolesCount++;
                System.out.println("Aggiunto ruolo: " + name + " all'unità: " + unit.getName());
            } else {
                System.out.println("ERRORE: Unità con ID [" + unitId + "] non trovata per il ruolo: " + name);
            }
        }

        System.out.println("Totale ruoli aggiunti: " + rolesCount);

        // Verifica ruoli creati in ogni unità
        for (OrganizationalUnit unit : units.values()) {
            System.out.println("Unità: " + unit.getName() + " - Ruoli: " + unit.getRoles().size());
            for (Role role : unit.getRoles()) {
                System.out.println("  - Ruolo: " + role.getName() + ", Descrizione: " + role.getDescription());
            }
        }
    }

    /**
     * Read employees from a section of lines
     */
    private Map<String, Employee> readEmployeesFromSection(List<String> lines) {
        Map<String, Employee> employees = new HashMap<>();

        if (lines.size() <= 1) {
            System.out.println("Nessun dipendente trovato nella sezione EMPLOYEES.");
            return employees;
        }

        System.out.println("Elaborazione dipendenti dalla sezione EMPLOYEES...");
        System.out.println("Numero di righe di dipendenti: " + (lines.size() - 1));

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = splitCsvLine(lines.get(i));
            if (parts.length < 2) {
                System.out.println("ATTENZIONE: Riga " + i + " della sezione EMPLOYEES non ha abbastanza campi.");
                continue;
            }

            String id = parts[0];
            String name = parts[1];

            Employee employee = new Employee(name);
            // Imposta esplicitamente l'ID univoco dell'impiegato
            employee.setUniqueId(id);
            employees.put(id, employee);

            System.out.println("Creato dipendente: " + name + " [ID: " + id + "]");
        }

        System.out.println("Totale dipendenti creati: " + employees.size());
        return employees;
    }

    /**
     * Read assignments from a section of lines
     */
    private void readAssignmentsFromSection(List<String> lines, Map<String, OrganizationalUnit> units,
                                            Map<String, Employee> employees) {
        if (lines.size() <= 1) {
            return;
        }

        System.out.println("Elaborazione delle assegnazioni dei dipendenti...");
        System.out.println("Numero di linee di assegnazione: " + (lines.size() - 1));

        int assignmentsCount = 0;

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = splitCsvLine(lines.get(i));
            if (parts.length < 3) {
                System.out.println("ERRORE: Formato assegnazione non valido alla riga " + i);
                continue;
            }

            String employeeId = parts[0];
            String roleName = parts[1];
            String unitId = parts[2];

            System.out.println("Assegnazione: Dipendente [" + employeeId + "] - Ruolo [" + roleName + "] - Unità [" + unitId + "]");

            Employee employee = employees.get(employeeId);
            OrganizationalUnit unit = units.get(unitId);

            if (employee == null) {
                System.out.println("ERRORE: Dipendente con ID [" + employeeId + "] non trovato");
            }

            if (unit == null) {
                System.out.println("ERRORE: Unità con ID [" + unitId + "] non trovata");
            }

            if (employee != null && unit != null) {
                Role role = findRoleByName(unit, roleName);
                if (role != null) {
                    // Assign the employee to this role (bidirectional)
                    role.addEmployee(employee);
                    employee.addRole(role);
                    employee.addUnit(unit);

                    assignmentsCount++;
                    System.out.println("Assegnazione completata con successo: " + employee.getName() +
                            " [" + employee.getUniqueId() + "] assegnato a " + role.getName() +
                            " in " + unit.getName());
                } else {
                    System.out.println("ERRORE: Ruolo [" + roleName + "] non trovato nell'unità [" + unit.getName() + "]");
                }
            }
        }

        System.out.println("Assegnazioni completate con successo: " + assignmentsCount);
    }

    /**
     * Collects all data from the organizational structure
     */
    private void collectData(OrganizationalUnit unit, OrganizationalUnit parent,
                             Map<String, OrganizationalUnit> units,
                             List<Role> allRoles,
                             List<Employee> allEmployees,
                             List<String[]> assignments) {
        System.out.println("Raccolta dati dall'unità: " + unit.getName());

        // Generate unique ID for this unit
        String unitId = getUnitId(unit);

        // Add unit to collection with parent reference
        // Assicuriamoci che venga sempre usato l'ID univoco generato da getUnitId
        // invece di qualsiasi altro identificatore
        units.put(unitId, unit);

        // Stampa di debug per tracciare gli ID
        System.out.println("  * Salvato " + unit.getType() + " '" + unit.getName() +
                "' con ID: " + unitId +
                (unit.getParent() != null ? " - Parent: " + unit.getParent().getName() +
                        " [ID: " + getUnitId(unit.getParent()) + "]" : " (Root)"));

        // Add all roles from this unit
        System.out.println("- Ruoli in " + unit.getName() + ": " + unit.getRoles().size());
        for (Role role : unit.getRoles()) {
            allRoles.add(role);
            System.out.println("  - Ruolo: " + role.getName() + ", dipendenti: " + role.getEmployees().size());
        }

        // Process all employees in the unit
        int employeeCount = 0;
        for (Role role : unit.getRoles()) {
            for (Employee employee : role.getEmployees()) {
                if (!allEmployees.contains(employee)) {
                    allEmployees.add(employee);
                    employeeCount++;
                    System.out.println("    + Dipendente aggiunto: " + employee.getName() + " [" + employee.getUniqueId() + "]");
                }

                // Create assignment record
                assignments.add(new String[] {
                        employee.getUniqueId(),
                        role.getName(),
                        unitId
                });
            }
        }
        System.out.println("- Dipendenti trovati in " + unit.getName() + ": " + employeeCount);

        // Process subunits recursively
        System.out.println("- Sottounità in " + unit.getName() + ": " + unit.getSubUnits().size());
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            // The parent relationship is already established via the addSubUnit method
            collectData(subUnit, unit, units, allRoles, allEmployees, assignments);
        }

        System.out.println("Completata raccolta dati per: " + unit.getName());
        System.out.println("Totale dipendenti raccolti: " + allEmployees.size());
    }

    /**
     * Writes units to CSV file
     */
    private void writeUnitsCsv(Map<String, OrganizationalUnit> units, String filepath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            // Write header
            writer.println(UNIT_HEADER);

            // Write each unit
            for (Map.Entry<String, OrganizationalUnit> entry : units.entrySet()) {
                OrganizationalUnit unit = entry.getValue();
                String unitId = entry.getKey();
                String unitType = unit instanceof Department ? "Department" :
                        (unit instanceof Group ? "Group" : "Unit");
                String parentId = unit.getParent() != null ? getUnitId(unit.getParent()) : "";

                String line = String.join(CSV_SEPARATOR,
                        escapeCsv(unitType),
                        escapeCsv(unitId),
                        escapeCsv(unit.getName()),
                        escapeCsv(unit.getDescription()),
                        escapeCsv(parentId)
                );

                writer.println(line);
            }
        }
    }

    /**
     * Writes roles to CSV file
     */
    private void writeRolesCsv(List<Role> roles, String filepath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            // Write header
            writer.println(ROLE_HEADER);

            // Write each role
            for (Role role : roles) {
                OrganizationalUnit unit = role.getUnit();
                if (unit != null) {
                    String line = String.join(CSV_SEPARATOR,
                            escapeCsv(getUnitId(unit)),
                            escapeCsv(role.getName()),
                            escapeCsv(role.getDescription())
                    );

                    writer.println(line);
                }
            }
        }
    }

    /**
     * Writes employees to CSV file
     */
    private void writeEmployeesCsv(List<Employee> employees, String filepath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            // Write header
            writer.println(EMPLOYEE_HEADER);

            // Write each employee
            for (Employee employee : employees) {
                String line = String.join(CSV_SEPARATOR,
                        escapeCsv(employee.getUniqueId()),
                        escapeCsv(employee.getName())
                );

                writer.println(line);
            }
        }
    }

    /**
     * Writes role assignments to CSV file
     */
    private void writeAssignmentsCsv(List<String[]> assignments, String filepath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            // Write header
            writer.println(ASSIGNMENT_HEADER);

            // Write each assignment
            for (String[] assignment : assignments) {
                String line = String.join(CSV_SEPARATOR,
                        escapeCsv(assignment[0]),
                        escapeCsv(assignment[1]),
                        escapeCsv(assignment[2])
                );

                writer.println(line);
            }
        }
    }

    /**
     * Reads units from CSV file
     */
    private Map<String, OrganizationalUnit> readUnitsCsv(String filepath) throws IOException {
        Map<String, OrganizationalUnit> units = new HashMap<>();
        Map<String, String> parentRelations = new HashMap<>();

        List<String> lines = Files.readAllLines(Paths.get(filepath));
        if (lines.size() <= 1) {
            return units;
        }

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = splitCsvLine(lines.get(i));
            if (parts.length < 5) continue;

            String type = parts[0];
            String id = parts[1];
            String name = parts[2];
            String description = parts[3];
            String parentId = parts[4];

            // Create the unit based on type
            OrganizationalUnit unit;
            if ("Department".equals(type)) {
                unit = new Department(name);
            } else if ("Group".equals(type)) {
                unit = new Group(name);
            } else {
                unit = new Department(name); // Default to Department
            }

            units.put(id, unit);
            if (!parentId.isEmpty()) {
                parentRelations.put(id, parentId);
            }
        }

        // Set up parent-child relationships
        for (Map.Entry<String, String> relation : parentRelations.entrySet()) {
            OrganizationalUnit child = units.get(relation.getKey());
            OrganizationalUnit parent = units.get(relation.getValue());

            if (child != null && parent != null) {
                parent.addSubUnit(child);
            }
        }

        return units;
    }

    /**
     * Reads roles from CSV file and attaches them to units
     */
    private void readRolesCsv(String filepath, Map<String, OrganizationalUnit> units) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filepath));
        if (lines.size() <= 1) {
            return;
        }

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = splitCsvLine(lines.get(i));
            if (parts.length < 3) continue;

            String unitId = parts[0];
            String name = parts[1];
            String description = parts[2];

            OrganizationalUnit unit = units.get(unitId);
            if (unit != null) {
                Role role = new Role(name, description);
                unit.addRole(role);
            }
        }
    }

    /**
     * Reads employees from CSV file
     */
    private Map<String, Employee> readEmployeesCsv(String filepath) throws IOException {
        Map<String, Employee> employees = new HashMap<>();

        List<String> lines = Files.readAllLines(Paths.get(filepath));
        if (lines.size() <= 1) {
            return employees;
        }

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = splitCsvLine(lines.get(i));
            if (parts.length < 2) continue;

            String id = parts[0];
            String name = parts[1];

            Employee employee = new Employee(name);
            // Imposta esplicitamente l'ID univoco dell'impiegato
            employee.setUniqueId(id);
            employees.put(id, employee);
        }

        return employees;
    }

    /**
     * Reads and processes role assignments
     */
    private void readAssignmentsCsv(String filepath, Map<String, OrganizationalUnit> units,
                                    Map<String, Employee> employees) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filepath));
        if (lines.size() <= 1) {
            return;
        }

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = splitCsvLine(lines.get(i));
            if (parts.length < 3) continue;

            String employeeId = parts[0];
            String roleName = parts[1];
            String unitId = parts[2];

            Employee employee = employees.get(employeeId);
            OrganizationalUnit unit = units.get(unitId);

            if (employee != null && unit != null) {
                Role role = findRoleByName(unit, roleName);
                if (role != null) {
                    // Assign the employee to this role
                    employee.addRole(role);
                    employee.addUnit(unit);
                }
            }
        }
    }

    /**
     * Find a role by name within a unit
     */
    private Role findRoleByName(OrganizationalUnit unit, String name) {
        for (Role role : unit.getRoles()) {
            if (role.getName().equals(name)) {
                return role;
            }
        }
        return null;
    }

    /**
     * Find the root unit in the collection
     */
    private OrganizationalUnit findRootUnit(Map<String, OrganizationalUnit> units) {
        System.out.println("Cerco unità radice tra " + units.size() + " unità...");

        if (units.isEmpty()) {
            System.err.println("Nessuna unità trovata nel CSV");
            return null;
        }

        // Metodo 1: Verifica se abbiamo un'unità di nome "Acme Corp"
        for (OrganizationalUnit unit : units.values()) {
            if ("Acme Corp".equals(unit.getName())) {
                System.out.println("Trovata unità radice 'Acme Corp': " + unit.getName() + " (" + unit.getType() + ")");
                return unit;
            }
        }

        // Metodo 2: Identifica unità che non hanno parent esaminando le sottounità
        Map<String, Boolean> isSubUnit = new HashMap<>();

        // Inizializza la mappa
        for (String unitId : units.keySet()) {
            isSubUnit.put(unitId, false);
        }

        // Marca tutte le unità che sono sottounità di qualcuno
        for (OrganizationalUnit unit : units.values()) {
            for (OrganizationalUnit subUnit : unit.getSubUnits()) {
                for (Map.Entry<String, OrganizationalUnit> entry : units.entrySet()) {
                    if (entry.getValue() == subUnit) {
                        isSubUnit.put(entry.getKey(), true);
                        System.out.println("Unità '" + subUnit.getName() + "' è una sottounità di '" + unit.getName() + "'");
                    }
                }
            }
        }

        // Trova tutte le unità che non sono sottounità di nessuno
        List<OrganizationalUnit> rootCandidates = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : isSubUnit.entrySet()) {
            if (!entry.getValue()) {
                OrganizationalUnit unit = units.get(entry.getKey());
                rootCandidates.add(unit);
                System.out.println("Candidato unità radice: " + unit.getName() + " (" + unit.getType() + ")");
            }
        }

        if (rootCandidates.size() == 1) {
            System.out.println("Trovato un solo candidato radice: " + rootCandidates.get(0).getName());
            return rootCandidates.get(0);
        }

        // Metodo 3: Se abbiamo più candidati, cerca quelli con nomi speciali
        for (OrganizationalUnit unit : rootCandidates) {
            String name = unit.getName().toLowerCase();
            if (name.contains("acme") || name.contains("root") ||
                    name.contains("azienda") || name.contains("company") ||
                    name.contains("corp")) {
                System.out.println("Scelta unità radice per nome: " + unit.getName());
                return unit;
            }
        }

        // Metodo 4: Se ancora non abbiamo una radice, guarda quale unità ha più sottounità
        if (!rootCandidates.isEmpty()) {
            OrganizationalUnit bestCandidate = rootCandidates.get(0);
            int maxSubUnits = bestCandidate.getSubUnits().size();

            for (int i = 1; i < rootCandidates.size(); i++) {
                OrganizationalUnit candidate = rootCandidates.get(i);
                if (candidate.getSubUnits().size() > maxSubUnits) {
                    bestCandidate = candidate;
                    maxSubUnits = candidate.getSubUnits().size();
                }
            }

            System.out.println("Scelta unità radice con più sottounità (" + maxSubUnits + "): " + bestCandidate.getName());
            return bestCandidate;
        }

        // Fallback: restituisci la prima unità
        OrganizationalUnit first = units.values().iterator().next();
        System.out.println("Fallback: usata prima unità come radice: " + first.getName());
        return first;
    }

    /**
     * Generate a consistent and unique ID for a unit
     */
    private String getUnitId(OrganizationalUnit unit) {
        // Per garantire unicità anche in caso di nomi duplicati, utilizziamo il
        // nome dell'unità combinato con il suo hashCode per creare un ID univoco
        String baseName = unit.getName().replaceAll("\\s+", "_").toLowerCase();
        String uniquePart = String.valueOf(Math.abs(unit.hashCode() % 10000));
        String type = unit.getType().toLowerCase().substring(0, 3); // prime 3 lettere del tipo

        // Aggiungiamo anche l'indice di sottounità se presente
        String parentInfo = "";
        if (unit.getParent() != null) {
            List<OrganizationalUnit> siblings = unit.getParent().getSubUnits();
            int index = siblings.indexOf(unit) + 1; // +1 per evitare indice 0
            if (index > 0) {
                parentInfo = "_" + index;
            }
        }

        return type + "_" + baseName + "_" + uniquePart + parentInfo;
    }

    /**
     * Escape special characters for CSV
     */
    private String escapeCsv(String str) {
        if (str == null) return "";
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    /**
     * Split a CSV line respecting quoted values
     */
    private String[] splitCsvLine(String line) {
        boolean inQuotes = false;
        List<String> result = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        result.add(currentField.toString());
        return result.toArray(new String[0]);
    }
}