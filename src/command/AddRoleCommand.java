package command;

import model.OrganizationalUnit;
import model.Role;
import controller.OrgChartManager;

/**
 * Comando per aggiungere un ruolo a un'unità organizzativa
 */
public class AddRoleCommand implements Command {
    private OrganizationalUnit unit;
    private Role role;
    private OrgChartManager manager;
    private boolean executed = false;

    /**
     * Costruttore
     * @param unit L'unità a cui aggiungere il ruolo
     * @param role Il ruolo da aggiungere
     */
    public AddRoleCommand(OrganizationalUnit unit, Role role) {
        this.unit = unit;
        this.role = role;
        this.manager = OrgChartManager.getInstance();
    }

    @Override
    public boolean execute() {
        if (executed) {
            return false;
        }

        boolean result = manager.addRoleDirectly(unit, role);
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

        boolean result = manager.removeRoleDirectly(unit, role);
        if (result) {
            executed = false;
        }
        return result;
    }

    @Override
    public String getDescription() {
        return "Aggiunta ruolo '" + role.getName() + "' all'unità '" + unit.getName() + "'";
    }
}