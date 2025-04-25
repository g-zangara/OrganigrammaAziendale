package factory;

import model.*;

/**
 * Factory class for creating different types of organizational units.
 * Implements the Factory Method pattern.
 */
public class UnitFactory {
    /**
     * Create a new organizational unit based on the specified type
     * @param type The type of unit to create ("Department", "Group", etc.)
     * @param name The name for the new unit
     * @return A new OrganizationalUnit instance of the appropriate type
     */
    public static OrganizationalUnit createUnit(String type, String name) {
        if (type == null || name == null) {
            throw new IllegalArgumentException("Type and name cannot be null");
        }

        switch (type) {
            case "Department":
                return new Department(name);
            case "Group":
                return new Group(name);
            case "Board":
                return new Board(name);
            default:
                throw new IllegalArgumentException("Unknown unit type: " + type);
        }
    }
}