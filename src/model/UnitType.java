package model;

/**
 * Enum representing the different types of organizational units.
 */
public enum UnitType {
    DEPARTMENT,
    GROUP;

    /**
     * Check if a unit is compatible with this type
     * @param unit The unit to check
     * @return true if the unit is of this type
     */
    public boolean matches(OrganizationalUnit unit) {
        if (this == DEPARTMENT) {
            return unit instanceof Department;
        } else if (this == GROUP) {
            return unit instanceof Group;
        }
        return false;
    }
}