package command;

import model.Employee;
import model.OrganizationalUnit;
import model.Role;
import model.ValidationException;
import controller.OrgChartManager;
import util.Logger;

/**
 * Command for changing an employee's role within an organizational unit.
 * Part of the Command pattern.
 */
public class ChangeEmployeeRoleCommand implements Command {
    private final OrgChartManager manager;
    private final Employee employee;
    private final Role oldRole;
    private final Role newRole;
    private final OrganizationalUnit unit;
    private boolean executed;

    /**
     * Constructor
     * @param manager The OrgChartManager
     * @param employee The employee to change role for
     * @param oldRole The current role
     * @param newRole The new role
     * @param unit The organizational unit
     */
    public ChangeEmployeeRoleCommand(OrgChartManager manager, Employee employee, Role oldRole, Role newRole, OrganizationalUnit unit) {
        this.manager = manager;
        this.employee = employee;
        this.oldRole = oldRole;
        this.newRole = newRole;
        this.unit = unit;
        this.executed = false;
    }

    @Override
    public boolean execute() {
        try {
            if (!executed) {
                boolean result = manager.changeEmployeeRoleDirectly(employee, oldRole, newRole, unit);
                executed = result;
                return result;
            }
            return false;
        } catch (Exception e) {
            Logger.logError("Execution failed during ChangeEmployeeRoleCommand: " + e.getMessage(), "ChangeEmployeeRoleCommand");
            return false;
        }
    }

    @Override
    public boolean undo() {
        if (executed) {
            try {
                // To undo, we swap the roles back
                boolean result = manager.changeEmployeeRoleDirectly(employee, newRole, oldRole, unit);
                if (result) {
                    executed = false;
                    return true;
                }
                return false;
            } catch (Exception e) {
                Logger.logError("Undo failed during ChangeEmployeeRoleCommand: " + e.getMessage(), "ChangeEmployeeRoleCommand");
                return false;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Change '" + employee.getName() + "' from role '" + oldRole.getName() +
                "' to '" + newRole.getName() + "' in '" + unit.getName() + "'";
    }
}