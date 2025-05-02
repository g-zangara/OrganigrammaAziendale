package command;

import model.OrganizationalUnit;
import model.Employee;
import controller.OrgChartManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Comando per rimuovere un dipendente da un'unità
 */
public class RemoveEmployeeCommand implements Command {
    private OrganizationalUnit unit;
    private Employee employee;
    private OrgChartManager manager;
    private boolean executed = false;
    private String originalRole; // Memorizza il ruolo originale del dipendente

    /**
     * Costruttore
     * @param unit L'unità da cui rimuovere il dipendente
     * @param employee Il dipendente da rimuovere
     */
    public RemoveEmployeeCommand(OrganizationalUnit unit, Employee employee) {
        this.unit = unit;
        this.employee = employee;
        this.manager = OrgChartManager.getInstance();

        // Salva il ruolo originale per ripristinarlo durante l'undo
        //this.originalRole = unit.getEmployeeRole(employee);
    }

    @Override
    public boolean execute() {
        if (executed) {
            return false;
        }

        manager.removeEmployeeDirectly(unit, employee);
        executed = true;
        return true;
    }

    @Override
    public boolean undo() {
        if (!executed) {
            return false;
        }

        boolean result = manager.addEmployeeDirectly(unit, employee);
        if (result && originalRole != null && !originalRole.isEmpty()) {
            // Ripristina il ruolo originale
            manager.assignRoleDirectly(unit, employee, originalRole);
        }

        executed = false;
        return result;
    }

    @Override
    public String getDescription() {
        return "Rimozione dipendente '" + employee.getName() + "' da '" + unit.getName() + "'";
    }
}