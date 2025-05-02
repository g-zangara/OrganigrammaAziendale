package command;

import model.OrganizationalUnit;
import model.Employee;
import model.Role;
import controller.OrgChartManager;

/**
 * Comando per assegnare un ruolo a un dipendente
 */
public class AssignRoleCommand implements Command {
    private OrganizationalUnit unit;
    private Employee employee;
    private Role role;
    private Role previousRole;
    private OrgChartManager manager;
    private boolean executed = false;

    /**
     * Costruttore
     * @param unit L'unità in cui assegnare il ruolo
     * @param employee Il dipendente a cui assegnare il ruolo
     * @param role Il ruolo da assegnare
     */
    public AssignRoleCommand(OrganizationalUnit unit, Role role, Employee employee) {
        this.unit = unit;
        this.employee = employee;
        this.role = role;
        this.manager = OrgChartManager.getInstance();

        // Nella prima implementazione, non teniamo traccia del ruolo precedente
        // poiché non c'è un metodo diretto per ottenere il ruolo di un dipendente
        this.previousRole = null;
    }

    @Override
    public boolean execute() {
        if (executed) {
            return false;
        }

        boolean result = manager.assignEmployeeToRoleDirectly(employee, role, unit);
        if (result) {
            executed = true;
        }
        return result;
    }

    @Override
    public boolean undo() {
        if (!executed) {
            return false;
        }

        // Rimuove l'assegnazione dell'employee dal ruolo
        boolean result = manager.removeEmployeeFromRoleDirectly(employee, role, unit);
        if (result) {
            executed = false;
        }
        return result;
    }

    @Override
    public String getDescription() {
        return "Assegnazione ruolo '" + role.getName() + "' a '" + employee.getName() + "' in '" + unit.getName() + "'";
    }
}