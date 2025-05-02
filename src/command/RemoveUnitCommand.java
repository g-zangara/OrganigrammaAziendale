package command;

import model.OrganizationalUnit;
import controller.OrgChartManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Comando per rimuovere un'unità organizzativa
 */
public class RemoveUnitCommand implements Command {
    private OrganizationalUnit parent;
    private OrganizationalUnit unit;
    private OrgChartManager manager;
    private boolean executed = false;

    // Memorizza lo stato dell'unità prima della rimozione per poterla ripristinare
    private int originalIndex;
    private List<OrganizationalUnit> originalSubUnits = new ArrayList<>();

    /**
     * Costruttore
     * @param parent L'unità genitore da cui rimuovere la sottounità
     * @param unit L'unità da rimuovere
     */
    public RemoveUnitCommand(OrganizationalUnit parent, OrganizationalUnit unit) {
        this.parent = parent;
        this.unit = unit;
        this.manager = OrgChartManager.getInstance();

        // Salva l'indice della sottounità
        List<OrganizationalUnit> subUnits = parent.getSubUnits();
        originalIndex = subUnits.indexOf(unit);

        // Salva le sottounità ricorsivamente
        originalSubUnits.addAll(unit.getSubUnits());
    }

    @Override
    public boolean execute() {
        if (executed) {
            return false;
        }

        manager.removeUnitDirectly(parent, unit);
        executed = true;
        return true;
    }

    @Override
    public boolean undo() {
        if (!executed) {
            return false;
        }

        boolean result = manager.addUnitDirectly(parent, unit);

        // Aggiungi tutte le sottounità originali
        for (OrganizationalUnit subUnit : originalSubUnits) {
            unit.addSubUnit(subUnit);
        }

        executed = false;
        return result;
    }

    @Override
    public String getDescription() {
        return "Rimozione unità '" + unit.getName() + "' da '" + parent.getName() + "'";
    }
}