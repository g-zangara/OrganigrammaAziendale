package command;

import model.OrganizationalUnit;
import model.Role;
import controller.OrgChartManager;

/**
 * Comando per rimuovere un ruolo da un'unità organizzativa
 */
public class RemoveRoleCommand implements Command {
    private OrganizationalUnit unit;
    private Role role;
    private OrgChartManager manager;
    private boolean executed = false;

    /**
     * Costruttore
     * @param unit L'unità da cui rimuovere il ruolo
     * @param role Il ruolo da rimuovere
     */
    public RemoveRoleCommand(OrganizationalUnit unit, Role role) {
        this.unit = unit;
        this.role = role;
        this.manager = OrgChartManager.getInstance();
    }

    @Override
    public boolean execute() {
        if (executed) {
            return false;
        }

        boolean result = manager.removeRoleDirectly(unit, role);
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

        boolean result = manager.addRoleDirectly(unit, role);
        if (result) {
            executed = false;
        }
        return result;
    }

    @Override
    public String getDescription() {
        return "Rimozione ruolo '" + role.getName() + "' dall'unità '" + unit.getName() + "'";
    }
}