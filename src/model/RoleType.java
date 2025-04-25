package model;

import java.util.Arrays;
import java.util.List;

/**
 * Enum representing the different types of roles and their valid organizational unit types.
 */
public enum RoleType {
    DIRETTORE("Direttore", UnitType.DEPARTMENT),
    COORDINATORE("Coordinatore", UnitType.GROUP),
    CONSIGLIERE("Consigliere", UnitType.DEPARTMENT, UnitType.GROUP),

    PRESIDENTE("Presidente", UnitType.BOARD),
    VICE_PRESIDENTE("Vicepresidente", UnitType.BOARD),
    SEGRETARIO("Segretario", UnitType.BOARD),

    RESPONSABILE_AMMINISTRATIVO("Responsabile Amministrativo", UnitType.DEPARTMENT),
    REFERENTE_TECNICO("Referente Tecnico", UnitType.DEPARTMENT),
    RESPONSABILE_COMMERCIALE("Responsabile Commerciale", UnitType.DEPARTMENT),
    RESPONSABILE_RISORSE_UMANE("Responsabile Risorse Umane", UnitType.DEPARTMENT),
    RESPONSABILE_LOGISTICA("Responsabile Logistica", UnitType.DEPARTMENT),
    ANALISTA("Analista", UnitType.DEPARTMENT),
    CONSULENTE("Consulente", UnitType.DEPARTMENT),

    TEAM_LEADER("Team Leader", UnitType.GROUP),
    TUTOR("Tutor", UnitType.GROUP),
    COLLABORATORE("Collaboratore", UnitType.GROUP),
    MEMBRO("Membro", UnitType.GROUP),
    STAGISTA("Stagista", UnitType.GROUP),

    DPO("Data Protection Officer", UnitType.DEPARTMENT),
    CFO("Chief Financial Officer", UnitType.DEPARTMENT),
    CTO("Chief Technology Officer", UnitType.DEPARTMENT),
    HR_SPECIALIST("HR Specialist", UnitType.DEPARTMENT),
    QA_MANAGER("Quality Assurance Manager", UnitType.DEPARTMENT);


    private final String roleName;
    private final List<UnitType> validUnitTypes;

    /**
     * Constructor
     * @param roleName The name of the role
     * @param validUnitTypes The unit types this role can be assigned to
     */
    RoleType(String roleName, UnitType... validUnitTypes) {
        this.roleName = roleName;
        this.validUnitTypes = Arrays.asList(validUnitTypes);
    }

    /**
     * Get the name of this role type
     * @return The role name
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Check if this role type is valid for a given unit type
     * @param unitType The unit type to check
     * @return true if this role can be assigned to the given unit type
     */
    public boolean isValidFor(UnitType unitType) {
        return validUnitTypes.contains(unitType);
    }

    /**
     * Check if this role type is valid for a given organizational unit
     * @param unit The unit to check
     * @return true if this role can be assigned to the given unit
     */
    public boolean isValidFor(OrganizationalUnit unit) {
        for (UnitType unitType : validUnitTypes) {
            if (unitType.matches(unit)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find a role type by name
     * @param name The role name to search for
     * @return The matching RoleType or null if not found
     */
    public static RoleType findByName(String name) {
        for (RoleType roleType : values()) {
            if (roleType.getRoleName().equalsIgnoreCase(name)) {
                return roleType;
            }
        }
        return null;
    }
}