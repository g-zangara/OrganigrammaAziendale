package model;

/**
 * Represents a Department, which is a specific type of organizational unit.
 * Part of the Composite pattern implementation.
 */
public class Department extends OrganizationalUnit {
    /**
     * Constructor for a Department
     * @param name The name of the department
     */
    public Department(String name) {
        super(name, "Department");
    }
}
