package controller;

import factory.StorageFactory;
import model.*;
import persistence.*;
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
     * Create a new org chart with a root department
     */
    public void createNewOrgChart(String rootName) {
        rootUnit = new Department(rootName);
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
     */
    public void addUnit(OrganizationalUnit parent, OrganizationalUnit newUnit) {
        if (parent != null) {
            parent.addSubUnit(newUnit);
            notifyObservers();
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
     */
    public void addRole(OrganizationalUnit unit, Role role) {
        if (unit != null) {
            unit.addRole(role);
            notifyObservers();
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
                return false; // Non possiamo rimuovere un ruolo con dipendenti associati
            }

            unit.removeRole(role);
            notifyObservers();
            return true;
        }
        return false;
    }

    /**
     * Assign an employee to a role in an organizational unit
     */
    public void assignEmployeeToRole(Employee employee, Role role, OrganizationalUnit unit) {
        if (employee != null && role != null && unit != null) {
            // Set the unit for the role if it's not already set
            if (role.getUnit() == null) {
                role.setUnit(unit);
            }

            // Add role to employee
            employee.addRole(role);
            employee.addUnit(unit);

            // Add employee to the unit's employee list
            List<Employee> employees = employeesByUnit.computeIfAbsent(unit, k -> new ArrayList<>());
            if (!employees.contains(employee)) {
                employees.add(employee);
            }

            notifyObservers();
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
                rootUnit = loadedRoot;

                // Importante: ricostruire la mappa employeesByUnit dopo il caricamento
                rebuildEmployeeMapping();

                notifyObservers();
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Ricostruisce la mappa employeesByUnit analizzando l'intera struttura
     * organizzativa per trovare tutti i dipendenti.
     */
    private void rebuildEmployeeMapping() {
        // Svuota la mappa corrente
        employeesByUnit.clear();

        System.out.println("*** Ricostruzione della mappa dei dipendenti ***");

        // Funzione ricorsiva per analizzare l'albero organizzativo
        if (rootUnit != null) {
            processUnitForEmployees(rootUnit);
        }

        // Stampa per debug
        System.out.println("Mappa dipendenti ricostruita:");
        int totalEmployees = 0;
        for (Map.Entry<OrganizationalUnit, List<Employee>> entry : employeesByUnit.entrySet()) {
            OrganizationalUnit unit = entry.getKey();
            List<Employee> employees = entry.getValue();
            totalEmployees += employees.size();
            System.out.println("- Unità: " + unit.getName() + ", Dipendenti: " + employees.size());
            for (Employee emp : employees) {
                System.out.println("  + " + emp.getName() + " [" + emp.getUniqueId() + "]");
                System.out.println("    Ruoli in questa unità: " + emp.getRoles().size());
            }
        }
        System.out.println("Totale dipendenti trovati: " + totalEmployees);
    }

    /**
     * Elabora ricorsivamente un'unità organizzativa per estrarre i dipendenti
     */
    private void processUnitForEmployees(OrganizationalUnit unit) {
        if (unit == null) return;

        System.out.println("Elaborazione unità: " + unit.getName());
        Set<Employee> uniqueEmployeesInUnit = new HashSet<>();

        // Controlla tutti i ruoli in questa unità
        for (Role role : unit.getRoles()) {
            System.out.println("- Ruolo: " + role.getName() + ", dipendenti: " + role.getEmployees().size());

            // Aggiungi tutti i dipendenti per questo ruolo
            for (Employee emp : role.getEmployees()) {
                System.out.println("  + Dipendente: " + emp.getName() + " [" + emp.getUniqueId() + "]");

                // Assicurati che il ruolo abbia un riferimento all'unità
                if (role.getUnit() == null) {
                    role.setUnit(unit);
                }

                // Assicurati che il dipendente abbia un riferimento al ruolo e all'unità
                if (!emp.getRoles().contains(role)) {
                    emp.addRole(role);
                }

                // Aggiungi l'unità al dipendente per la relazione bidirezionale
                emp.addUnit(unit);

                // Aggiungi il dipendente all'insieme per questa unità
                uniqueEmployeesInUnit.add(emp);
            }
        }

        // Aggiunge i dipendenti all'unità nella mappa
        if (!uniqueEmployeesInUnit.isEmpty()) {
            List<Employee> employeeList = employeesByUnit.computeIfAbsent(unit, k -> new ArrayList<>());
            employeeList.addAll(uniqueEmployeesInUnit);
            System.out.println("Aggiunti " + uniqueEmployeesInUnit.size() + " dipendenti all'unità " + unit.getName());
        }

        // Processa ricorsivamente le sottounità
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            processUnitForEmployees(subUnit);
        }
    }
}
