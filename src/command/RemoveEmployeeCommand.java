package command;

import model.Employee;
import model.OrganizationalUnit;
import model.Role;
import model.ValidationException;
import controller.OrgChartManager;
import util.Logger;

/**
 * Command for removing an employee from a role in an organizational unit.
 * Part of the Command pattern.
 */
public class RemoveEmployeeCommand implements Command {
    private final OrgChartManager manager;
    private final Employee employee;
    private final Role role;
    private final OrganizationalUnit unit;
    private boolean executed;

    /**
     * Constructor
     * @param manager The OrgChartManager
     * @param employee The employee to remove
     * @param role The role to remove the employee from
     * @param unit The organizational unit
     */
    public RemoveEmployeeCommand(OrgChartManager manager, Employee employee, Role role, OrganizationalUnit unit) {
        this.manager = manager;
        this.employee = employee;
        this.role = role;
        this.unit = unit;
        this.executed = false;
    }

    @Override
    public boolean execute() {
        if (!executed) {
            boolean result = manager.removeEmployeeFromRoleDirectly(employee, role, unit);
            executed = result;
            return result;
        }
        return false;
    }

    @Override
    public boolean undo() {
        if (executed) {
            try {
                boolean result = manager.assignEmployeeToRoleDirectly(employee, role, unit);
                if (result) {
                    executed = false;
                    return true;
                }
                return false;
            } catch (Exception e) {
                Logger.logError("Execution failed during RemoveEmployeeCommand undo: " + e.getMessage(), "RemoveEmployeeCommand");
                return false;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Remove '" + employee.getName() + "' from role '" + role.getName() + "' in '" + unit.getName() + "'";
    }
}