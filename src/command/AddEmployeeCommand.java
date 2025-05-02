package command;

import model.OrganizationalUnit;
import model.Employee;
import controller.OrgChartManager;

/**
 * Comando per aggiungere un nuovo dipendente a un'unità
 */
public class AddEmployeeCommand implements Command {
    private OrganizationalUnit unit;
    private Employee employee;
    private OrgChartManager manager;
    private boolean executed = false;

    /**
     * Costruttore
     * @param unit L'unità a cui aggiungere il dipendente
     * @param employee Il dipendente da aggiungere
     */
    public AddEmployeeCommand(OrganizationalUnit unit, Employee employee) {
        this.unit = unit;
        this.employee = employee;
        this.manager = OrgChartManager.getInstance();
    }

    @Override
    public boolean execute() {
        if (executed) {
            return false;
        }

        boolean result = manager.addEmployeeDirectly(unit, employee);
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

        manager.removeEmployeeDirectly(unit, employee);
        executed = false;
        return true;
    }

    @Override
    public String getDescription() {
        return "Aggiunta dipendente '" + employee.getName() + " " + "' a '" + unit.getName() + "'";
    }
}