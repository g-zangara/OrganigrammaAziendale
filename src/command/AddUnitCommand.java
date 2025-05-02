package command;

import model.OrganizationalUnit;
import controller.OrgChartManager;
import util.Logger;

/**
 * Comando per aggiungere una nuova unità organizzativa
 */
public class AddUnitCommand implements Command {
    private OrganizationalUnit parent;
    private OrganizationalUnit unit;
    private OrgChartManager manager;
    private boolean executed = false;

    /**
     * Costruttore
     * @param parent L'unità genitore a cui aggiungere la sottounità
     * @param unit La nuova unità da aggiungere
     */
    public AddUnitCommand(OrganizationalUnit parent, OrganizationalUnit unit) {
        this.parent = parent;
        this.unit = unit;
        this.manager = OrgChartManager.getInstance();
    }

    @Override
    public boolean execute() {
        if (executed) {
            return false; // Impedisce l'esecuzione multipla dello stesso comando
        }

        boolean result = manager.addUnitDirectly(parent, unit);
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

        manager.removeUnitDirectly(parent, unit);
        executed = false;
        return true;
    }

    @Override
    public String getDescription() {
        return "Aggiunta unità '" + unit.getName() + "' a '" + parent.getName() + "'";
    }
}