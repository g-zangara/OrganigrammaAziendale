package model;

/**
 * Represents a Group, which is a specific type of organizational unit.
 * Part of the Composite pattern implementation.
 */
public class Group extends OrganizationalUnit {
    /**
     * Constructor for a Group
     * @param name The name of the group
     */
    public Group(String name) {
        super(name, "Group");
    }
}
