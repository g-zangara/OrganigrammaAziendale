package command;

import model.Employee;
import model.OrganizationalUnit;
import model.Role;
import model.ValidationException;
import controller.OrgChartManager;
import util.Logger;

/**
 * Command for assigning an employee to a role in an organizational unit.
 * Part of the Command pattern.
 */
public class AssignEmployeeCommand implements Command {
    private final OrgChartManager manager;
    private final Employee employee;
    private final Role role;
    private final OrganizationalUnit unit;
    private boolean executed;

    /**
     * Constructor
     * @param manager The OrgChartManager
     * @param employee The employee to assign
     * @param role The role to assign the employee to
     * @param unit The organizational unit
     */
    public AssignEmployeeCommand(OrgChartManager manager, Employee employee, Role role, OrganizationalUnit unit) {
        this.manager = manager;
        this.employee = employee;
        this.role = role;
        this.unit = unit;
        this.executed = false;
    }

    @Override
    public boolean execute() {
        try {
            System.out.println("Debug AssignEmployeeCommand: Tentativo di aggiungere: " + employee.getName() + " a " + role.getName() + " in " + unit.getName());

            if (!executed) {
                boolean result = manager.assignEmployeeToRoleDirectly(employee, role, unit);
                executed = result;

                System.out.println("Debug AssignEmployeeCommand: Risultato dell'aggiunta: " + (result ? "SUCCESS" : "FAILURE"));

                // Se l'operazione ha avuto successo, verifichiamo lo stato della mappa
                if (result) {
                    System.out.println("Debug AssignEmployeeCommand: Verifica dipendenti nell'unità dopo l'aggiunta:");
                    int count = manager.getEmployeesInUnit(unit).size();
                    System.out.println("Debug AssignEmployeeCommand: Numero di dipendenti nell'unità: " + count);
                }

                return result;
            }
            System.out.println("Debug AssignEmployeeCommand: Comando già eseguito, non rieseguito");
            return false;
        } catch (Exception e) {
            System.out.println("Debug AssignEmployeeCommand: Eccezione: " + e.getMessage());
            e.printStackTrace();
            Logger.logError("Execution failed during AssignEmployeeCommand: " + e.getMessage(), "AssignEmployeeCommand");
            return false;
        }
    }

    @Override
    public boolean undo() {
        if (executed) {
            manager.removeEmployeeFromRole(employee, role, unit);
            executed = false;
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Assign '" + employee.getName() + "' to role '" + role.getName() + "' in '" + unit.getName() + "'";
    }
}