package model;

import java.util.HashSet;
import java.util.Set;

/**
 * Validator class that enforces business rules for the organization chart.
 */
public class OrgChartValidator {

    /**
     * Validate adding a new organizational unit to a parent unit
     * Rules:
     * 1. Unit names must be unique among siblings
     * 2. Departments can contain Groups and other Departments
     * 3. Groups can't contain any subunits
     *
     * @param parent The parent unit
     * @param newUnit The new unit to add
     * @throws ValidationException if any validation rule is violated
     */
    public static void validateAddUnit(OrganizationalUnit parent, OrganizationalUnit newUnit) throws ValidationException {
        // Check for null
        if (parent == null || newUnit == null) {
            throw new ValidationException("Parent and new unit cannot be null");
        }

        // Check for duplicate names among siblings
        Set<String> siblingNames = new HashSet<>();
        for (OrganizationalUnit sibling : parent.getSubUnits()) {
            siblingNames.add(sibling.getName());
        }

        if (siblingNames.contains(newUnit.getName())) {
            throw new ValidationException("Unit name '" + newUnit.getName() +
                    "' already exists among siblings. Names must be unique within the same parent.");
        }

        // Check unit type compatibility
        if (parent instanceof Group) {
            throw new ValidationException("Groups cannot contain sub-units.");
        }

        // If parent is Department and new unit is not Group or Department, throw exception
        if (parent instanceof Department) {
            if (!(newUnit instanceof Group) && !(newUnit instanceof Department)) {
                throw new ValidationException("Departments can only contain Groups or other Departments.");
            }
        }
    }

    /**
     * Validate adding a role to an organizational unit
     * Rules:
     * 1. Role names must be one of the predefined types (Direttore, Coordinatore, Consigliere)
     * 2. Direttore roles can only be added to Departments
     * 3. Coordinatore roles can only be added to Groups
     * 4. Consigliere roles can be added to either
     *
     * @param unit The unit to add the role to
     * @param role The role to add
     * @throws ValidationException if any validation rule is violated
     */
    public static void validateAddRole(OrganizationalUnit unit, Role role) throws ValidationException {
        // Check for null
        if (unit == null || role == null) {
            throw new ValidationException("Unit and role cannot be null");
        }

        // Find the role type by name
        RoleType roleType = RoleType.findByName(role.getName());

        // Check if this is a recognized role type
        if (roleType == null) {
            throw new ValidationException("Role name '" + role.getName() +
                    "' is not a valid role type. Valid types are: Direttore, Coordinatore, Consigliere.");
        }

        // Check if the role type is valid for this unit type
        if (!roleType.isValidFor(unit)) {
            UnitType unitType = unit instanceof Department ? UnitType.DEPARTMENT : UnitType.GROUP;
            throw new ValidationException("Role '" + roleType.getRoleName() +
                    "' cannot be assigned to a " + unitType + ".");
        }

        // Check if the role already exists in this unit
        for (Role existingRole : unit.getRoles()) {
            if (existingRole.getName().equals(role.getName())) {
                throw new ValidationException("Role '" + role.getName() +
                        "' already exists in this unit.");
            }
        }
    }

    /**
     * Validate assigning an employee to a role in an organizational unit
     * Rules:
     * 1. The role must exist in the unit
     * 2. The employee can have multiple roles in different units
     *
     * @param employee The employee to assign
     * @param role The role to assign
     * @param unit The unit the role belongs to
     * @throws ValidationException if any validation rule is violated
     */
    public static void validateAssignEmployeeToRole(Employee employee, Role role, OrganizationalUnit unit) throws ValidationException {
        // Check for null
        if (employee == null || role == null || unit == null) {
            throw new ValidationException("Employee, role, and unit cannot be null");
        }

        // Check if the role belongs to the unit
        boolean roleExistsInUnit = false;
        for (Role unitRole : unit.getRoles()) {
            if (unitRole.equals(role)) {
                roleExistsInUnit = true;
                break;
            }
        }

        if (!roleExistsInUnit) {
            throw new ValidationException("Role '" + role.getName() +
                    "' does not exist in unit '" + unit.getName() + "'.");
        }

        // Check if the employee already has this role
        for (Role employeeRole : employee.getRoles()) {
            if (employeeRole.equals(role)) {
                throw new ValidationException("Employee '" + employee.getName() +
                        "' already has the role '" + role.getName() +
                        "' in unit '" + unit.getName() + "'.");
            }
        }
    }
}