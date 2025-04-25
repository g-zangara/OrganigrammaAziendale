package model;

/**
 * Represents a Board, which is a specific type of organizational unit.
 * Part of the Composite pattern implementation.
 * A Board can only be added at the root level of the organization chart.
 */
public class Board extends OrganizationalUnit {
    /**
     * Constructor for a Board
     * @param name The name of the board
     */
    public Board(String name) {
        super(name, "Board");
    }
}