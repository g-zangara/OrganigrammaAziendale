package controller;

import factory.StorageFactory;
import model.*;
import persistence.*;
import util.Logger;
import util.ErrorManager;
import java.util.*;

/**
 * Singleton controller class that manages the organization chart.
 * Implements Observer pattern to notify views of data changes.
 */
public class OrgChartManager {
    private static OrgChartManager instance;
    private OrganizationalUnit rootUnit;
    private List<Observer> observers = new ArrayList<>();
    private StorageStrategy storageStrategy;

    // Interface for Observer pattern
    public interface Observer {
        void update();
    }

    // Private constructor for Singleton pattern
    private OrgChartManager() {
        // Default to Java serialization storage strategy
        this.storageStrategy = new SerializationStorage();
    }

    /**
     * Get the singleton instance of OrgChartManager
     */
    public static synchronized OrgChartManager getInstance() {
        if (instance == null) {
            instance = new OrgChartManager();
        }
        return instance;
    }

    /**
     * Sets the storage strategy to use (Strategy pattern)
     */
    public void setStorageStrategy(StorageStrategy strategy) {
        this.storageStrategy = strategy;
    }

    /**
     * Register an observer to be notified of changes
     */
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    /**
     * Remove an observer
     */
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Notify all observers that data has changed
     */
    private void notifyObservers() {
        for (Observer observer : observers) {
            observer.update();
        }
    }

    /**
     * Create a new org chart with a root Board
     * Le unità di tipo Board possono esistere solo al livello radice
     */
    public void createNewOrgChart(String rootName) {
        rootUnit = new Board(rootName);
        notifyObservers();
    }

    /**
     * Get the root organizational unit
     */
    public OrganizationalUnit getRootUnit() {
        return rootUnit;
    }

    /**
     * Add a new organizational unit to a parent unit
     * @return true if the unit was added successfully, false if validation failed
     */
    /**
     * Aggiunge una nuova unità organizzativa a un'unità padre.
     * Effettua la validazione e lancia un'eccezione se l'unità non può essere aggiunta
     * (ad esempio, se il padre è un gruppo o se il nome esiste già tra i fratelli).
     *
     * @param parent L'unità organizzativa padre a cui aggiungere la nuova unità
     * @param newUnit La nuova unità da aggiungere
     * @return true se l'unità è stata aggiunta con successo
     * @throws ValidationException se la validazione fallisce
     */
    public boolean addUnit(OrganizationalUnit parent, OrganizationalUnit newUnit) throws ValidationException {
        if (parent == null || newUnit == null) {
            String errorMsg = "Unità padre o nuova unità non possono essere null";
            util.Logger.logError(errorMsg, "Errore di Validazione");
            throw new ValidationException(errorMsg);
        }

        try {
            // Effettua la validazione prima di aggiungere
            OrgChartValidator.validateAddUnit(parent, newUnit);

            // La validazione è passata, aggiungi l'unità
            parent.addSubUnit(newUnit);

            // Registra l'operazione per debugging
            util.Logger.logDebug("Unità '" + newUnit.getName() + "' aggiunta all'unità '" + parent.getName() + "'", "Operazione Completata");

            // Notifica gli osservatori del cambiamento
            notifyObservers();
            return true;
        } catch (ValidationException ex) {
            // Rilanciamo l'eccezione per gestirla nella UI
            util.Logger.logError("Errore nell'aggiunta di unità: " + ex.getMessage(), "Errore di Validazione");
            throw ex; // Importante: rilanciamo l'eccezione per essere gestita dal chiamante
        }
    }

    /**
     * Remove an organizational unit
     */
    public void removeUnit(OrganizationalUnit parent, OrganizationalUnit unit) {
        if (parent != null) {
            parent.removeSubUnit(unit);
            notifyObservers();
        }
    }

    /**
     * Add a new role to an organizational unit
     * @return true if the role was added successfully, false if validation failed
     */
    /**
     * Aggiunge un ruolo a un'unità organizzativa, lanciando eventuali eccezioni di validazione.
     * Questo metodo verifica prima che il ruolo sia valido per il tipo di unità
     * e lancia un'eccezione se non lo è.
     *
     * @param unit l'unità a cui aggiungere il ruolo
     * @param role il ruolo da aggiungere
     * @return true se il ruolo è stato aggiunto con successo
     * @throws ValidationException se il ruolo non è valido per l'unità
     */
    public boolean addRole(OrganizationalUnit unit, Role role) throws ValidationException {
        if (unit == null || role == null) {
            String errorMsg = "Unità o ruolo non possono essere null";
            util.Logger.logError(errorMsg, "Errore di Validazione");
            throw new ValidationException(errorMsg);
        }

        try {
            // Effettua la validazione prima di aggiungere
            OrgChartValidator.validateAddRole(unit, role);

            // La validazione è passata, aggiungi il ruolo
            unit.addRole(role);
            role.setUnit(unit); // Imposta la relazione bidirezionale

            // Registra l'operazione per debugging
            util.Logger.logDebug("Ruolo '" + role.getName() + "' aggiunto all'unità '" + unit.getName() + "'", "Operazione Completata");

            // Notifica gli osservatori del cambiamento
            notifyObservers();
            return true;
        } catch (ValidationException ex) {
            // Rilanciamo l'eccezione per gestirla nella UI
            util.Logger.logError("Errore nell'aggiunta di ruolo: " + ex.getMessage(), "Errore di Validazione");
            throw ex; // Importante: rilanciamo l'eccezione per essere gestita dal chiamante
        }
    }

    /**
     * Controlla se un ruolo ha dipendenti associati
     * @param role Il ruolo da controllare
     * @return true se ci sono dipendenti con questo ruolo, false altrimenti
     */
    public boolean roleHasEmployees(Role role) {
        if (role == null) return false;

        // Scorriamo tutte le unità e tutti i dipendenti per trovare quelli con questo ruolo
        for (Map.Entry<OrganizationalUnit, List<Employee>> entry : employeesByUnit.entrySet()) {
            for (Employee employee : entry.getValue()) {
                if (employee.getRoles().contains(role)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Remove a role from an organizational unit
     * @return true se il ruolo è stato rimosso con successo, false se c'erano dipendenti associati
     */
    public boolean removeRole(OrganizationalUnit unit, Role role) {
        if (unit != null && role != null) {
            // Prima controlliamo se ci sono dipendenti con questo ruolo
            if (roleHasEmployees(role)) {
                ErrorManager.registerError("Non puoi rimuovere un ruolo con dipendenti associati.");
                return false; // Non possiamo rimuovere un ruolo con dipendenti associati
            }

            unit.removeRole(role);
            notifyObservers();
            return true;
        }
        return false;
    }

    /**
     * Assegna un dipendente a un ruolo in un'unità organizzativa, con validazione.
     * Questo metodo verifica che l'assegnazione sia valida rispetto alle regole
     * di business dell'organigramma e lancia un'eccezione se ci sono problemi.
     *
     * @param employee il dipendente da assegnare
     * @param role il ruolo a cui assegnare il dipendente
     * @param unit l'unità in cui si trova il ruolo
     * @return true se il dipendente è stato assegnato con successo
     * @throws ValidationException se la validazione fallisce
     */
    public boolean assignEmployeeToRole(Employee employee, Role role, OrganizationalUnit unit) throws ValidationException {
        if (employee == null || role == null || unit == null) {
            String errorMsg = "Dipendente, ruolo o unità non possono essere null";
            util.Logger.logError(errorMsg, "Errore di Validazione");
            throw new ValidationException(errorMsg);
        }

        try {
            // Effettua la validazione prima di procedere
            OrgChartValidator.validateAssignEmployeeToRole(employee, role, unit);

            // Imposta l'unità per il ruolo se non è già impostata
            if (role.getUnit() == null) {
                role.setUnit(unit);
            }

            // Aggiungi il ruolo al dipendente
            employee.addRole(role);
            employee.addUnit(unit);

            // Aggiungi il dipendente alla lista dei dipendenti dell'unità
            List<Employee> employees = employeesByUnit.computeIfAbsent(unit, k -> new ArrayList<>());
            if (!employees.contains(employee)) {
                employees.add(employee);
            }

            // Registra l'operazione per debugging
            util.Logger.logDebug("Dipendente '" + employee.getName() + "' assegnato al ruolo '" +
                    role.getName() + "' nell'unità '" + unit.getName() + "'", "Operazione Completata");

            // Notifica gli osservatori del cambiamento
            notifyObservers();
            return true;
        } catch (ValidationException ex) {
            // Rilanciamo l'eccezione per gestirla nella UI
            util.Logger.logError("Errore nell'assegnazione del dipendente: " + ex.getMessage(), "Errore di Validazione");
            throw ex; // Importante: rilanciamo l'eccezione per essere gestita dal chiamante
        }
    }

    /**
     * Remove an employee from a role in an organizational unit
     */
    public void removeEmployeeFromRole(Employee employee, Role role, OrganizationalUnit unit) {
        if (employee != null) {
            // Remove the role from the employee
            employee.removeRole(role);

            // Check if the employee still has other roles in this unit
            boolean hasOtherRolesInUnit = false;
            for (Role r : employee.getRoles()) {
                if (r.getUnit() == unit) {
                    hasOtherRolesInUnit = true;
                    break;
                }
            }

            // If no other roles in this unit, remove the unit from employee
            if (!hasOtherRolesInUnit) {
                employee.removeUnit(unit);

                // Also remove from our employee tracking map
                List<Employee> employees = employeesByUnit.get(unit);
                if (employees != null) {
                    employees.remove(employee);
                }
            }

            notifyObservers();
        }
    }

    /**
     * Cambia il ruolo di un dipendente nella stessa unità organizzativa
     * @param employee Il dipendente a cui cambiare ruolo
     * @param oldRole Il ruolo attuale
     * @param newRole Il nuovo ruolo
     * @param unit L'unità organizzativa
     */
    public void changeEmployeeRole(Employee employee, Role oldRole, Role newRole, OrganizationalUnit unit) {
        if (employee != null && oldRole != null && newRole != null && unit != null) {
            // Rimuovi il vecchio ruolo
            employee.removeRole(oldRole);

            // Aggiungi il nuovo ruolo
            employee.addRole(newRole);

            // Se il nuovo ruolo non è ancora associato all'unità, associalo
            if (newRole.getUnit() == null) {
                newRole.setUnit(unit);
            }

            notifyObservers();
        }
    }

    // Store employees globally in the manager
    private Map<OrganizationalUnit, List<Employee>> employeesByUnit = new HashMap<>();

    /**
     * Get all employees assigned to a specific organizational unit
     */
    public List<Employee> getEmployeesInUnit(OrganizationalUnit unit) {
        if (unit == null) {
            return Collections.emptyList();
        }

        // Get employees for this unit or create a new list if none exists
        List<Employee> employees = employeesByUnit.computeIfAbsent(unit, k -> new ArrayList<>());
        return new ArrayList<>(employees); // Return a copy to prevent external modification
    }

    /**
     * Save the organization chart to a file
     */
    public boolean saveOrgChart(String filePath) {
        try {
            return storageStrategy.save(rootUnit, filePath);
        } catch (Exception e) {
            e.printStackTrace();
            ErrorManager.registerError("Errore durante il salvataggio del file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load an organization chart from a file
     */
    public boolean loadOrgChart(String filePath) {
        try {
            // Select the appropriate storage strategy based on file extension
            String lowerPath = filePath.toLowerCase();
            if (lowerPath.endsWith(".json")) {
                setStorageStrategy(StorageFactory.createStorageStrategy(
                        StorageFactory.StorageType.JSON));
            } else if (lowerPath.endsWith(".csv")) {
                setStorageStrategy(StorageFactory.createStorageStrategy(
                        StorageFactory.StorageType.CSV));
            } else if (lowerPath.endsWith(".db")) {
                setStorageStrategy(StorageFactory.createStorageStrategy(
                        StorageFactory.StorageType.DBMS));
            }

            OrganizationalUnit loadedRoot = storageStrategy.load(filePath);
            if (loadedRoot != null) {
                // Log del tipo di radice caricata
                System.out.println("Caricata unità radice di tipo: " + loadedRoot.getClass().getSimpleName());

                // NUOVA VERIFICA: La radice DEVE essere di tipo Board
                if (!(loadedRoot instanceof Board)) {
                    String errorMsg = "La radice dell'organigramma deve essere di tipo Board. Tipo trovato: "
                            + loadedRoot.getClass().getSimpleName();
                    System.err.println(errorMsg);
                    ErrorManager.registerError(errorMsg);
                    Logger.logError(errorMsg, "Errore di Validazione");
                    return false;
                }

                // Per Board, assicuriamo che abbia almeno un ruolo Presidente se non ne ha
                if (loadedRoot.getRoles().isEmpty()) {
                    System.out.println("Aggiunto ruolo Presidente alla Board radice");
                    loadedRoot.addRole(new Role("Presidente", "Board President"));
                }

                // Validazione della struttura caricata
                if (!validateLoadedStructure(loadedRoot)) {
                    ErrorManager.registerError("Il file contiene dati non validi secondo le regole di validazione. Caricamento interrotto.");
                    return false;
                }

                rootUnit = loadedRoot;

                // Importante: ricostruire la mappa employeesByUnit dopo il caricamento
                rebuildEmployeeMapping();

                notifyObservers();
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            ErrorManager.registerError("Errore durante il caricamento del file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Valida la struttura caricata da file secondo le regole di business
     * @param root L'unità radice da validare
     * @return true se la struttura è valida, false altrimenti
     */
    private boolean validateLoadedStructure(OrganizationalUnit root) {
        try {
            // Aggiungiamo log dettagliati sulla struttura caricata
            System.out.println("Validazione struttura caricata...");
            System.out.println("Tipo di unità radice: " + root.getClass().getSimpleName());
            System.out.println("Nome unità radice: " + root.getName());
            System.out.println("Numero di ruoli nella radice: " + root.getRoles().size());
            for (Role role : root.getRoles()) {
                System.out.println(" - Ruolo: " + role.getName());
            }
            System.out.println("Numero di sottounità nella radice: " + root.getSubUnits().size());

            // Verifica che i nomi delle sottounità siano unici tra fratelli
            System.out.println("Verificando nomi unici tra fratelli...");
            validateUniqueNames(root);
            System.out.println("Verifica nomi completata con successo.");

            // Verifica che le relazioni gerarchiche siano valide (gruppi non possono contenere sottounità)
            System.out.println("Verificando relazioni gerarchiche...");
            validateHierarchy(root);
            System.out.println("Verifica gerarchie completata con successo.");

            // Per Board senza ruoli, aggiungiamo ruoli essenziali se necessario
            if (root instanceof Board && root.getRoles().isEmpty()) {
                System.out.println("Board senza ruoli rilevata, aggiunta automatica del ruolo Presidente");
                root.addRole(new Role("Presidente", "Board President"));
            }

            // Verifica che i ruoli siano validi per i tipi di unità (saltiamo questo passaggio per la Board radice)
            if (root instanceof Board) {
                System.out.println("Radice di tipo Board: la validazione dei ruoli sarà meno restrittiva");
                // Saltare la validazione completa per la radice Board e verificare solo le sottounità
                for (OrganizationalUnit child : root.getSubUnits()) {
                    validateRoles(child);
                }
            } else {
                System.out.println("Verificando validità dei ruoli per ogni unità...");
                validateRoles(root);
                System.out.println("Verifica ruoli completata con successo.");
            }

            return true;
        } catch (ValidationException e) {
            System.out.println("ERRORE DI VALIDAZIONE: " + e.getMessage());
            e.printStackTrace();
            ErrorManager.registerError("Errore di validazione: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica che i nomi delle sottounità siano unici tra fratelli
     */
    private void validateUniqueNames(OrganizationalUnit unit) throws ValidationException {
        if (unit == null) return;

        // Controllo nomi unici tra i figli diretti
        Set<String> childNames = new HashSet<>();
        for (OrganizationalUnit child : unit.getSubUnits()) {
            if (!childNames.add(child.getName())) {
                throw new ValidationException("Nome duplicato trovato: '" + child.getName() +
                        "' in '" + unit.getName() + "'");
            }
        }

        // Verifica ricorsivamente per ogni figlio
        for (OrganizationalUnit child : unit.getSubUnits()) {
            validateUniqueNames(child);
        }
    }

    /**
     * Verifica che le relazioni gerarchiche siano valide:
     * 1. I gruppi non possono contenere sottounità
     * 2. Le unità di tipo Board possono esistere solo come radice
     */
    private void validateHierarchy(OrganizationalUnit unit) throws ValidationException {
        if (unit == null) return;

        // Se l'unità è un gruppo, non può avere sottounità
        if (unit instanceof Group && !unit.getSubUnits().isEmpty()) {
            throw new ValidationException("Il gruppo '" + unit.getName() +
                    "' non può contenere sottounità");
        }

        // Verifica che non ci siano Board come sottounità
        for (OrganizationalUnit child : unit.getSubUnits()) {
            if (child instanceof Board) {
                throw new ValidationException("L'unità di tipo Board '" + child.getName() +
                        "' può esistere solo come radice dell'organigramma");
            }
        }

        // Verifica ricorsivamente per ogni figlio
        for (OrganizationalUnit child : unit.getSubUnits()) {
            validateHierarchy(child);
        }
    }

    /**
     * Verifica che i ruoli siano validi per i tipi di unità durante il caricamento
     * Questa è una versione modificata per il caricamento da file che è più permissiva
     * poiché non verifica l'unicità dei ruoli tra unità diverse
     */
    private void validateRoles(OrganizationalUnit unit) throws ValidationException {
        if (unit == null) return;

        // Verifica che i ruoli siano validi per il tipo di unità
        for (Role role : unit.getRoles()) {
            // Invece di usare validateAddRole, che controlla anche l'unicità,
            // controlliamo solo se il tipo di ruolo è valido per questo tipo di unità
            RoleType roleType = RoleType.findByName(role.getName());

            if (roleType == null) {
                // Se il roleType non esiste, registriamo un warning ma non blocchiamo il caricamento
                System.out.println("AVVISO: Il ruolo '" + role.getName() +
                        "' non è riconosciuto, ma verrà accettato durante il caricamento.");
            } else if (!roleType.isValidFor(unit)) {
                // Questo è un errore più grave - il ruolo esiste ma non è valido per questo tipo di unità
                UnitType unitType;
                if (unit instanceof Department) {
                    unitType = UnitType.DEPARTMENT;
                } else if (unit instanceof Group) {
                    unitType = UnitType.GROUP;
                } else if (unit instanceof Board) {
                    unitType = UnitType.BOARD;
                } else {
                    unitType = null; // Should never happen
                }

                throw new ValidationException("Role '" + roleType.getRoleName() +
                        "' cannot be assigned to a " + unitType + ".");
            }
        }

        // Verifica ricorsivamente per ogni figlio
        for (OrganizationalUnit child : unit.getSubUnits()) {
            validateRoles(child);
        }
    }

    /**
     * Ricostruisce la mappa employeesByUnit dopo il caricamento da file
     */
    private void rebuildEmployeeMapping() {
        // Pulisci la mappa attuale
        employeesByUnit.clear();

        // Visita ricorsivamente l'albero per ricostruire le associazioni
        if (rootUnit != null) {
            rebuildEmployeeMappingRecursive(rootUnit);
        }
    }

    /**
     * Helper per la ricostruzione ricorsiva della mappa employeesByUnit
     */
    private void rebuildEmployeeMappingRecursive(OrganizationalUnit unit) {
        if (unit == null) return;

        // Inizializza la lista degli employee per questa unità
        List<Employee> employees = employeesByUnit.computeIfAbsent(unit, k -> new ArrayList<>());

        // Per ogni ruolo in questa unità, aggiungi gli employee associati
        for (Role role : unit.getRoles()) {
            for (Employee employee : role.getEmployees()) {
                if (!employees.contains(employee)) {
                    employees.add(employee);
                }

                // Assicuriamo che l'employee abbia anche questa unità e questo ruolo
                employee.addUnit(unit);
                employee.addRole(role);
            }
        }

        // Procedi ricorsivamente con le sottounità
        for (OrganizationalUnit child : unit.getSubUnits()) {
            rebuildEmployeeMappingRecursive(child);
        }
    }

    // ----------- METODI PER IMPLEMENTAZIONE PATTERN COMMAND ----------- //

    /**
     * Aggiunge un'unità direttamente senza effettuare validazioni.
     * Questo metodo viene utilizzato dal pattern Command per le operazioni di undo/redo.
     * @param parent L'unità padre a cui aggiungere la sottounità
     * @param unit L'unità da aggiungere
     * @return true se l'unità è stata aggiunta con successo
     */
    public boolean addUnitDirectly(OrganizationalUnit parent, OrganizationalUnit unit) {
        if (parent == null || unit == null) {
            return false;
        }

        try {
            // Applica validazioni di base anche nell'operazione diretta
            // ma cattura l'eccezione e ritorna false invece di propagarla
            OrgChartValidator.validateAddUnit(parent, unit);

            parent.addSubUnit(unit);
            notifyObservers();
            return true;
        } catch (ValidationException ex) {
            util.Logger.logError("Validazione fallita in addUnitDirectly: " + ex.getMessage(), "Operazione Command");
            return false;
        }
    }

    /**
     * Rimuove un'unità direttamente senza effettuare validazioni.
     * Questo metodo viene utilizzato dal pattern Command per le operazioni di undo/redo.
     * @param parent L'unità padre da cui rimuovere la sottounità
     * @param unit L'unità da rimuovere
     */
    public void removeUnitDirectly(OrganizationalUnit parent, OrganizationalUnit unit) {
        if (parent != null && unit != null) {
            parent.removeSubUnit(unit);
            notifyObservers();
        }
    }

    /**
     * Aggiunge un dipendente a un'unità direttamente senza effettuare validazioni.
     * Questo metodo viene utilizzato dal pattern Command per le operazioni di undo/redo.
     * @param unit L'unità a cui aggiungere il dipendente
     * @param employee Il dipendente da aggiungere
     * @return true se il dipendente è stato aggiunto con successo
     */
    public boolean addEmployeeDirectly(OrganizationalUnit unit, Employee employee) {
        if (unit == null || employee == null) {
            return false;
        }

        List<Employee> employees = employeesByUnit.computeIfAbsent(unit, k -> new ArrayList<>());
        if (!employees.contains(employee)) {
            employees.add(employee);
            employee.addUnit(unit);
            notifyObservers();
            return true;
        }
        return false;
    }

    /**
     * Rimuove un dipendente da un'unità direttamente senza effettuare validazioni.
     * Questo metodo viene utilizzato dal pattern Command per le operazioni di undo/redo.
     * @param unit L'unità da cui rimuovere il dipendente
     * @param employee Il dipendente da rimuovere
     */
    public void removeEmployeeDirectly(OrganizationalUnit unit, Employee employee) {
        if (unit != null && employee != null) {
            List<Employee> employees = employeesByUnit.get(unit);
            if (employees != null) {
                employees.remove(employee);
                employee.removeUnit(unit);

                // Rimuovi anche tutti i ruoli del dipendente in questa unità
                for (Role role : new ArrayList<>(employee.getRoles())) {
                    if (role.getUnit() == unit) {
                        employee.removeRole(role);
                    }
                }

                notifyObservers();
            }
        }
    }

    /**
     * Assegna un ruolo a un dipendente direttamente senza effettuare validazioni.
     * Questo metodo viene utilizzato dal pattern Command per le operazioni di undo/redo.
     * @param unit L'unità in cui assegnare il ruolo
     * @param employee Il dipendente a cui assegnare il ruolo
     * @param roleName Il nome del ruolo da assegnare
     * @return true se il ruolo è stato assegnato con successo
     */
    public boolean assignRoleDirectly(OrganizationalUnit unit, Employee employee, String roleName) {
        if (unit == null || employee == null || roleName == null || roleName.isEmpty()) {
            return false;
        }

        // Cerca se il ruolo esiste già nell'unità
        Role role = null;
        for (Role r : unit.getRoles()) {
            if (r.getName().equals(roleName)) {
                role = r;
                break;
            }
        }

        // Se il ruolo non esiste, crealo
        if (role == null) {
            role = new Role(roleName, "");
            role.setUnit(unit);
            unit.addRole(role);
        }

        // Assicurati che il dipendente sia associato all'unità
        List<Employee> employees = employeesByUnit.computeIfAbsent(unit, k -> new ArrayList<>());
        if (!employees.contains(employee)) {
            employees.add(employee);
        }

        // Prima rimuovi qualsiasi ruolo precedente nella stessa unità
        for (Role r : new ArrayList<>(employee.getRoles())) {
            if (r.getUnit() == unit) {
                employee.removeRole(r);
            }
        }

        // Assegna il nuovo ruolo
        employee.addRole(role);
        employee.addUnit(unit);

        notifyObservers();
        return true;
    }

    /**
     * Rimuove un ruolo da un dipendente direttamente senza effettuare validazioni.
     * Questo metodo viene utilizzato dal pattern Command per le operazioni di undo/redo.
     * @param unit L'unità in cui rimuovere il ruolo
     * @param employee Il dipendente a cui rimuovere il ruolo
     */
    public void removeRoleDirectly(OrganizationalUnit unit, Employee employee) {
        if (unit != null && employee != null) {
            // Rimuovi tutti i ruoli del dipendente in questa unità
            for (Role role : new ArrayList<>(employee.getRoles())) {
                if (role.getUnit() == unit) {
                    employee.removeRole(role);
                }
            }

            // Controlla se il dipendente ha ancora altri ruoli nell'unità
            boolean hasOtherRolesInUnit = false;
            for (Role r : employee.getRoles()) {
                if (r.getUnit() == unit) {
                    hasOtherRolesInUnit = true;
                    break;
                }
            }

            // Se non ha più ruoli nell'unità, rimuovilo dall'unità
            if (!hasOtherRolesInUnit) {
                List<Employee> employees = employeesByUnit.get(unit);
                if (employees != null) {
                    employees.remove(employee);
                }
                employee.removeUnit(unit);
            }

            notifyObservers();
        }
    }

    /**
     * Adds a role to a unit directly with basic validation
     * Used by AddRoleCommand for execute and by RemoveRoleCommand for undo
     * @param unit The unit to add the role to
     * @param role The role to add
     * @return true if successful, false otherwise
     */
    public boolean addRoleDirectly(OrganizationalUnit unit, Role role) {
        try {
            // Applica validazioni di base anche nell'operazione diretta
            // ma cattura l'eccezione e ritorna false invece di propagarla
            OrgChartValidator.validateAddRole(unit, role);

            unit.addRole(role);
            notifyObservers();
            return true;
        } catch (ValidationException ex) {
            util.Logger.logError("Validazione fallita in addRoleDirectly: " + ex.getMessage(), "Operazione Command");
            return false;
        } catch (Exception e) {
            util.Logger.logError("Errore in addRoleDirectly: " + e.getMessage(), "Errore");
            return false;
        }
    }

    /**
     * Removes a role from a unit directly without validation
     * Used by RemoveRoleCommand for execute and by AddRoleCommand for undo
     * @param unit The unit to remove the role from
     * @param role The role to remove
     * @return true if successful, false otherwise
     */
    public boolean removeRoleDirectly(OrganizationalUnit unit, Role role) {
        try {
            unit.removeRole(role);
            notifyObservers();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Assigns an employee to a role in a unit directly without validation
     * Used by AssignRoleCommand for execute
     * @param employee The employee to assign
     * @param role The role to assign the employee to
     * @param unit The unit containing the role
     * @return true if successful, false otherwise
     */
    public boolean assignEmployeeToRoleDirectly(Employee employee, Role role, OrganizationalUnit unit) {
        try {
            role.addEmployee(employee);
            employee.addUnit(unit);
            notifyObservers();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Removes an employee from a role in a unit directly without validation
     * Used by AssignRoleCommand for undo
     * @param employee The employee to remove
     * @param role The role to remove the employee from
     * @param unit The unit containing the role
     * @return true if successful, false otherwise
     */
    public boolean removeEmployeeFromRoleDirectly(Employee employee, Role role, OrganizationalUnit unit) {
        try {
            role.removeEmployee(employee);
            // Check if employee still has roles in this unit
            boolean hasRolesInUnit = false;
            for (Role r : employee.getRoles()) {
                if (r.getUnit() == unit) {
                    hasRolesInUnit = true;
                    break;
                }
            }
            if (!hasRolesInUnit) {
                employee.removeUnit(unit);
            }
            notifyObservers();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}