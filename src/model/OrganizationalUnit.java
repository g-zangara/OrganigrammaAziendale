package model;

import java.io.Serial;
import java.util.*;
import java.io.Serializable;

/**
 * Abstract base class for organizational units in the company hierarchy.
 * Uses the Composite pattern to create a tree structure of units.
 * Implements Serializable to allow Java standard serialization.
 */
public abstract class OrganizationalUnit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private final String type;
    private final List<OrganizationalUnit> subUnits;
    private final List<Role> roles;
    private OrganizationalUnit parent;

    /**
     * Constructor for organizational unit
     * @param name The name of the unit
     * @param type The type of the unit (e.g., "Department", "Group")
     */
    public OrganizationalUnit(String name, String type) {
        this.name = name;
        this.type = type;
        this.subUnits = new ArrayList<>();
        this.roles = new ArrayList<>();
        this.parent = null;
    }

    /**
     * Get the name of the organizational unit
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the organizational unit
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the type of the organizational unit
     */
    public String getType() {
        return type;
    }

    /**
     * Get all sub-units (Composite pattern)
     */
    public List<OrganizationalUnit> getSubUnits() {
        return Collections.unmodifiableList(subUnits);
    }

    /**
     * Add a sub-unit to this unit (Composite pattern)
     */
    public void addSubUnit(OrganizationalUnit unit) {
        subUnits.add(unit);
        unit.setParent(this);
    }

    /**
     * Remove a sub-unit from this unit (Composite pattern)
     */
    public boolean removeSubUnit(OrganizationalUnit unit) {
        boolean result = subUnits.remove(unit);
        if (result) {
            unit.setParent(null);
        }
        return result;
    }

    /**
     * Get all roles defined for this unit
     */
    public List<Role> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    /**
     * Add a role to this unit
     */
    public void addRole(Role role) {
        roles.add(role);
        role.setUnit(this);
    }

    /**
     * Remove a role from this unit
     */
    public boolean removeRole(Role role) {
        boolean result = roles.remove(role);
        if (result) {
            role.setUnit(null);
        }
        return result;
    }

    /**
     * Get the description of the unit
     */
    public String getDescription() {
        return description != null ? description : "";
    }

    /**
     * Set the description of the unit
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the parent unit
     * @return The parent unit or null if this is a root unit
     */
    public OrganizationalUnit getParent() {
        return parent;
    }

    /**
     * Set the parent unit (used internally by addSubUnit)
     * @param parent The parent unit
     */
    protected void setParent(OrganizationalUnit parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return type + ": " + name;
    }
}
