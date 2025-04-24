package model;

import java.io.Serial;
import java.util.*;
import java.io.Serializable;

/**
 * Represents an employee who can be assigned to roles within organizational units.
 * Implements Serializable to allow Java standard serialization.
 */
public class Employee implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String name;
    private List<Role> roles;
    private List<OrganizationalUnit> units;
    private String uniqueId; // ID univoco per ogni dipendente

    /**
     * Constructor for an employee
     * @param name The name of the employee
     */
    public Employee(String name) {
        this.name = name;
        this.roles = new ArrayList<>();
        this.units = new ArrayList<>();
        this.uniqueId = UUID.randomUUID().toString(); // Genera un ID unico per ogni dipendente
    }

    /**
     * Get the name of the employee
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the employee
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get all roles assigned to this employee
     */
    public List<Role> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    /**
     * Add a role to this employee
     */
    public void addRole(Role role) {
        if (!roles.contains(role)) {
            roles.add(role);

            // Add this employee to the role (avoiding infinite recursion)
            if (!role.getEmployees().contains(this)) {
                role.addEmployee(this);
            }

            // Also add the unit this role belongs to
            if (role.getUnit() != null && !units.contains(role.getUnit())) {
                units.add(role.getUnit());
            }
        }
    }

    /**
     * Remove a role from this employee
     */
    public boolean removeRole(Role role) {
        boolean removed = roles.remove(role);

        // Maintain bidirectional relationship
        if (removed && role.getEmployees().contains(this)) {
            role.removeEmployee(this);
        }

        // Check if we need to remove the unit as well
        if (removed) {
            boolean hasRoleInUnit = false;
            for (Role r : roles) {
                if (r.getUnit() != null && r.getUnit().equals(role.getUnit())) {
                    hasRoleInUnit = true;
                    break;
                }
            }

            // If no more roles in that unit, remove the unit
            if (!hasRoleInUnit && role.getUnit() != null) {
                units.remove(role.getUnit());
            }
        }

        return removed;
    }

    /**
     * Get all organizational units this employee belongs to
     */
    public List<OrganizationalUnit> getUnits() {
        return Collections.unmodifiableList(units);
    }

    /**
     * Add an organizational unit this employee belongs to
     */
    public void addUnit(OrganizationalUnit unit) {
        if (!units.contains(unit)) {
            units.add(unit);
        }
    }

    /**
     * Remove an organizational unit from this employee
     */
    public boolean removeUnit(OrganizationalUnit unit) {
        return units.remove(unit);
    }

    @Override
    public String toString() {
        return name;
    }

    // Usiamo l'ID univoco per equals e hashCode per permettere nomi duplicati
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Employee other = (Employee) obj;
        return uniqueId.equals(other.uniqueId);
    }

    @Override
    public int hashCode() {
        return uniqueId.hashCode();
    }

    /**
     * Get the unique ID of this employee
     * @return Unique ID
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Set the unique ID of this employee
     * @param uniqueId The new unique ID
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
