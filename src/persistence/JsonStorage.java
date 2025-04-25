package persistence;

import model.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;

/**
 * Implementation of StorageStrategy using JSON format with a simplified approach.
 * This class provides methods to save and load organizational structures using JSON.
 */
public class JsonStorage implements StorageStrategy {

    @Override
    public boolean save(OrganizationalUnit rootUnit, String filePath) {
        try {
            // Verifica la presenza di dipendenti
            List<Employee> allEmployees = new ArrayList<>();
            countEmployees(rootUnit, allEmployees);
            System.out.println("*** Verifica dipendenti per JSON ***");
            System.out.println("Numero dipendenti trovati: " + allEmployees.size());

            for (Employee emp : allEmployees) {
                System.out.println("Dipendente da salvare in JSON: " + emp.getName() + " [" + emp.getUniqueId() + "]");
                for (Role role : emp.getRoles()) {
                    System.out.println(" - Ruolo: " + role.getName());
                }
            }

            // Serializza l'unità in JSON
            String jsonOutput = serializeToJson(rootUnit);

            // Crea un writer per il file usando UTF-8
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"))) {
                writer.write(jsonOutput);
                writer.flush();

                // Log parziale del contenuto JSON per verifica
                System.out.println("Primi 200 caratteri del JSON: " +
                        (jsonOutput.length() > 200 ? jsonOutput.substring(0, 200) + "..." : jsonOutput));

                System.out.println("File JSON salvato correttamente: " + filePath);
                System.out.println("Dimensione del file JSON: " + jsonOutput.length() + " bytes");
                return true;
            }
        } catch (IOException e) {
            System.err.println("Errore durante il salvataggio del file JSON: " + e.getMessage());
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

    @Override
    public OrganizationalUnit load(String filePath) {
        try {
            System.out.println("*** Caricamento dal file JSON: " + filePath + " ***");

            // Verifica se il file esiste
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("File JSON non trovato: " + filePath);
                return null;
            }

            // Controllo preventivo del formato del file
            Path path = Paths.get(filePath);
            byte[] fileBytes = Files.readAllBytes(path);

            // Verifica se il file è vuoto
            if (fileBytes.length == 0) {
                System.err.println("Il file è vuoto: " + filePath);
                return null;
            }

            // Verifica se il file è in formato binario o JSON testuale
            boolean isBinaryFormat = false;

            // Controlla se inizia con i magic bytes di Java Serialization (0xACED)
            if (fileBytes.length >= 2 && fileBytes[0] == (byte)0xAC && fileBytes[1] == (byte)0xED) {
                System.out.println("ATTENZIONE: Rilevato file in formato serializzato Java (binario)");
                isBinaryFormat = true;
            }
            // Controlla se inizia con un carattere JSON valido ({ o [)
            else if (fileBytes.length > 0) {
                // Salta eventuali whitespace all'inizio
                int i = 0;
                while (i < fileBytes.length && (fileBytes[i] == ' ' || fileBytes[i] == '\t' ||
                        fileBytes[i] == '\n' || fileBytes[i] == '\r')) {
                    i++;
                }

                if (i < fileBytes.length) {
                    char firstChar = (char)fileBytes[i];
                    if (firstChar != '{' && firstChar != '[') {
                        // Probabilmente non è un JSON valido
                        String preview = new String(fileBytes, 0, Math.min(100, fileBytes.length), "UTF-8");
                        System.out.println("ATTENZIONE: Il file non sembra iniziare con un JSON valido.");
                        System.out.println("Anteprima: " + preview.replaceAll("[\\r\\n]+", " "));
                    }
                }
            }

            if (isBinaryFormat) {
                System.out.println("Tentativo di caricamento da file serializzato...");
                try (FileInputStream fileIn = new FileInputStream(filePath);
                     ObjectInputStream in = new ObjectInputStream(fileIn)) {

                    Object obj = in.readObject();

                    if (obj instanceof OrganizationalUnit) {
                        OrganizationalUnit unit = (OrganizationalUnit) obj;
                        System.out.println("Struttura caricata con successo dal file serializzato. Root: " + unit.getName());
                        return unit;
                    } else {
                        System.out.println("Il file contiene un oggetto di tipo errato: " + obj.getClass().getName());
                    }
                } catch (Exception e) {
                    System.out.println("Errore nel caricamento del file serializzato: " + e.getMessage());
                }
            }

            // Leggi il contenuto del file come testo JSON con encoding specifico
            System.out.println("Caricamento del file come JSON testuale...");
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            String jsonContent = sb.toString();
            System.out.println("Dimensione contenuto JSON: " + jsonContent.length() + " caratteri");

            if (jsonContent.length() < 200) {
                System.out.println("Contenuto completo: " + jsonContent);
            } else {
                System.out.println("Prime 200 caratteri: " + jsonContent.substring(0, 200) + "...");
            }

            // Verifica la struttura JSON prima di deserializzare
            if (!jsonContent.contains("\"roles\"")) {
                System.out.println("AVVISO: Il file JSON non contiene sezione 'roles'");
            }

            if (!jsonContent.contains("\"employees\"")) {
                System.out.println("AVVISO: Il file JSON non contiene sezione 'employees'");
            }

            // Deserializza il contenuto JSON
            OrganizationalUnit rootUnit = deserializeFromJson(jsonContent);

            // Verifica il risultato del caricamento
            if (rootUnit != null) {
                int totalEmployees = countTotalEmployees(rootUnit);
                System.out.println("Struttura caricata con successo. Radice: " + rootUnit.getName());
                System.out.println("Numero totale di dipendenti caricati: " + totalEmployees);
            } else {
                System.out.println("Errore: Impossibile deserializzare il file JSON");
            }

            return rootUnit;
        } catch (IOException e) {
            System.err.println("Errore durante il caricamento del file JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Metodo per contare tutti i dipendenti nell'albero organizzativo
    private int countTotalEmployees(OrganizationalUnit unit) {
        Set<Employee> uniqueEmployees = new HashSet<>();

        // Conta dipendenti in questa unità
        for (Role role : unit.getRoles()) {
            uniqueEmployees.addAll(role.getEmployees());
        }

        // Conta dipendenti nelle sottounità
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            Set<Employee> subUnitEmployees = new HashSet<>();
            for (Role role : subUnit.getRoles()) {
                subUnitEmployees.addAll(role.getEmployees());
            }
            uniqueEmployees.addAll(subUnitEmployees);

            // Ricorsivamente conta nelle unità più profonde
            uniqueEmployees.addAll(new HashSet<>(subUnit.getRoles().stream()
                    .flatMap(r -> r.getEmployees().stream())
                    .collect(java.util.stream.Collectors.toList())));
        }

        return uniqueEmployees.size();
    }

    /**
     * Serializza un'unità organizzativa in JSON
     */
    private String serializeToJson(OrganizationalUnit unit) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // Tipo dell'unità
        String unitType = unit instanceof Department ? "Department" :
                unit instanceof Group ? "Group" :
                        unit instanceof Board ? "Board" : "OrganizationalUnit";
        sb.append("  \"type\": \"").append(unitType).append("\",\n");

        // Nome e descrizione
        sb.append("  \"name\": \"").append(escapeJson(unit.getName())).append("\",\n");
        sb.append("  \"description\": \"").append(escapeJson(unit.getDescription())).append("\",\n");

        // Ruoli
        sb.append("  \"roles\": [\n");
        List<Role> roles = unit.getRoles();
        for (int i = 0; i < roles.size(); i++) {
            Role role = roles.get(i);
            sb.append("    {\n");
            sb.append("      \"name\": \"").append(escapeJson(role.getName())).append("\",\n");
            sb.append("      \"description\": \"").append(escapeJson(role.getDescription())).append("\",\n");

            // Dipendenti del ruolo
            sb.append("      \"employees\": [\n");
            List<Employee> employees = role.getEmployees();
            for (int j = 0; j < employees.size(); j++) {
                Employee emp = employees.get(j);
                sb.append("        {\n");
                sb.append("          \"id\": \"").append(emp.getUniqueId()).append("\",\n");
                sb.append("          \"name\": \"").append(escapeJson(emp.getName())).append("\"\n");
                sb.append("        }").append(j < employees.size() - 1 ? "," : "").append("\n");
            }
            sb.append("      ]\n");
            sb.append("    }").append(i < roles.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        // Sottounità
        sb.append("  \"subUnits\": [\n");
        List<OrganizationalUnit> subUnits = unit.getSubUnits();
        for (int i = 0; i < subUnits.size(); i++) {
            sb.append(indent(serializeToJson(subUnits.get(i)), 4));
            if (i < subUnits.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Deserializza un'unità organizzativa da JSON
     */
    private OrganizationalUnit deserializeFromJson(String json) {
        try {
            System.out.println("Deserializzazione dell'oggetto JSON...");

            // Estrai informazioni di base sull'unità
            String unitType = extractStringValue(json, "type");
            String unitName = extractStringValue(json, "name");
            String unitDescription = extractStringValue(json, "description");

            System.out.println("Tipo unità: " + unitType + ", Nome: " + unitName);

            // Crea l'unità appropriata
            OrganizationalUnit unit;
            if ("Department".equals(unitType)) {
                unit = new Department(unitName);
            } else if ("Group".equals(unitType)) {
                unit = new Group(unitName);
            } else if ("Board".equals(unitType)) {
                unit = new Board(unitName);
            } else {
                // Se non riconosciamo il tipo, tentiamo di determinarlo dal nome
                if (unitName.toLowerCase().contains("board") || unitName.toLowerCase().contains("comitato")) {
                    unit = new Board(unitName);
                } else {
                    unit = new Department(unitName); // Default fallback
                }
            }
            unit.setDescription(unitDescription);

            // Estrai e crea i ruoli
            String rolesJson = extractArrayContent(json, "roles");
            if (rolesJson != null && !rolesJson.isEmpty()) {
                List<String> roleObjects = extractJsonObjects(rolesJson);
                System.out.println("Trovati " + roleObjects.size() + " ruoli in " + unitName);

                for (String roleJson : roleObjects) {
                    String roleName = extractStringValue(roleJson, "name");
                    String roleDescription = extractStringValue(roleJson, "description");

                    System.out.println("Creazione ruolo: " + roleName + " in " + unitName);

                    Role role = new Role(roleName, roleDescription);
                    role.setUnit(unit);
                    unit.addRole(role);

                    // Estrai e crea i dipendenti per questo ruolo
                    String employeesJson = extractArrayContent(roleJson, "employees");
                    if (employeesJson != null && !employeesJson.isEmpty()) {
                        List<String> employeeObjects = extractJsonObjects(employeesJson);
                        System.out.println("Trovati " + employeeObjects.size() + " dipendenti nel ruolo " + roleName);

                        for (String empJson : employeeObjects) {
                            String empId = extractStringValue(empJson, "id");
                            String empName = extractStringValue(empJson, "name");

                            System.out.println("Creazione dipendente: " + empName + " [" + empId + "] per il ruolo " + roleName);

                            Employee emp = new Employee(empName);
                            if (empId != null && !empId.isEmpty()) {
                                emp.setUniqueId(empId);
                            }

                            // Importante: crea la relazione bidirezionale
                            emp.addRole(role);
                            emp.addUnit(unit);
                            role.addEmployee(emp);

                            System.out.println("Dipendente " + empName + " associato al ruolo " + roleName + " nell'unità " + unitName);
                        }
                    } else {
                        System.out.println("Nessun dipendente trovato per il ruolo " + roleName);
                    }
                }
            } else {
                System.out.println("Nessun ruolo trovato per l'unità " + unitName);
            }

            // Estrai e crea le sottounità
            String subUnitsJson = extractArrayContent(json, "subUnits");
            if (subUnitsJson != null && !subUnitsJson.isEmpty()) {
                List<String> subUnitObjects = extractJsonObjects(subUnitsJson);
                System.out.println("Trovate " + subUnitObjects.size() + " sottounità in " + unitName);

                for (String subUnitJson : subUnitObjects) {
                    OrganizationalUnit subUnit = deserializeFromJson(subUnitJson);
                    if (subUnit != null) {
                        unit.addSubUnit(subUnit);
                        System.out.println("Sottounità " + subUnit.getName() + " aggiunta a " + unitName);
                    }
                }
            }

            // Verifica finale
            int totalRoles = unit.getRoles().size();
            int totalEmployees = 0;
            for (Role r : unit.getRoles()) {
                totalEmployees += r.getEmployees().size();
            }

            System.out.println("Unità " + unitName + " completata: " + totalRoles + " ruoli, " +
                    totalEmployees + " dipendenti, " + unit.getSubUnits().size() + " sottounità");

            return unit;
        } catch (Exception e) {
            System.err.println("Errore durante la deserializzazione JSON: " + e.getMessage());
            e.printStackTrace();

            // In caso di errore, restituisci un'unità di base
            Department dept = new Department("Imported Unit");
            dept.addRole(new Role("Manager", "Department Manager"));
            return dept;
        }
    }

    /**
     * Estrae il valore di una proprietà di stringa da un oggetto JSON
     */
    private String extractStringValue(String json, String property) {
        String pattern = "\"" + property + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    /**
     * Estrae il contenuto di un array JSON
     */
    private String extractArrayContent(String json, String arrayName) {
        int start = json.indexOf("\"" + arrayName + "\"");
        if (start == -1) return "";

        start = json.indexOf("[", start);
        if (start == -1) return "";

        int end = start + 1;
        int level = 1;

        while (level > 0 && end < json.length()) {
            char c = json.charAt(end);
            if (c == '[') level++;
            else if (c == ']') level--;
            end++;
        }

        if (level == 0) {
            return json.substring(start, end);
        } else {
            return "";
        }
    }

    /**
     * Estrae oggetti JSON da un array JSON
     */
    private List<String> extractJsonObjects(String jsonArray) {
        List<String> objects = new ArrayList<>();
        int start = jsonArray.indexOf("{");

        while (start != -1) {
            int end = start + 1;
            int level = 1;

            while (level > 0 && end < jsonArray.length()) {
                char c = jsonArray.charAt(end);
                if (c == '{') level++;
                else if (c == '}') level--;
                end++;
            }

            if (level == 0) {
                objects.add(jsonArray.substring(start, end));
                start = jsonArray.indexOf("{", end);
            } else {
                break;
            }
        }

        return objects;
    }

    /**
     * Aggiunge indentazione a tutte le righe di una stringa
     */
    private String indent(String str, int spaces) {
        String indentation = " ".repeat(spaces);
        StringBuilder sb = new StringBuilder();

        String[] lines = str.split("\n");
        for (int i = 0; i < lines.length; i++) {
            sb.append(indentation).append(lines[i]);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Escape dei caratteri speciali JSON
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}