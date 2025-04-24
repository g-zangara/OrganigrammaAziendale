package model;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents a role that can be assigned to employees within an organizational unit.
 * Implements Serializable to allow Java standard serialization.
 */
public class Role implements Serializable {
    // Serialization version UID
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private OrganizationalUnit unit;
    private List<Employee> employees = new ArrayList<>();

    /**
     * Constructor for a role
     * @param name The name of the role
     * @param description The description of the role
     */
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Get the name of the role
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the role
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the description of the role
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description of the role
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the organizational unit this role belongs to
     */
    public OrganizationalUnit getUnit() {
        return unit;
    }

    /**
     * Set the organizational unit this role belongs to
     */
    public void setUnit(OrganizationalUnit unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Role other = (Role) obj;
        return name.equals(other.name) &&
                (unit == other.unit || (unit != null && unit.equals(other.unit)));
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (unit != null ? unit.hashCode() : 0);
        return result;
    }

    /**
     * Get employees assigned to this role
     * @return List of employees
     */
    public List<Employee> getEmployees() {
        return Collections.unmodifiableList(employees);
    }

    /**
     * Add an employee to this role
     * @param employee The employee to add
     */
    public void addEmployee(Employee employee) {
        if (!employees.contains(employee)) {
            employees.add(employee);

            // Bidirectional relationship with Employee
            if (!employee.getRoles().contains(this)) {
                employee.addRole(this);
            }
        }
    }

    /**
     * Remove an employee from this role
     * @param employee The employee to remove
     * @return True if the employee was removed, false otherwise
     */
    public boolean removeEmployee(Employee employee) {
        boolean removed = employees.remove(employee);

        // Maintain bidirectional relationship
        if (removed && employee.getRoles().contains(this)) {
            employee.removeRole(this);
        }

        return removed;
    }
}
