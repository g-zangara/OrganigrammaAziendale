package persistence;

import model.OrganizationalUnit;

/**
 * Interface for the Strategy pattern to implement different storage mechanisms.
 */
public interface StorageStrategy {
    /**
     * Save the organization chart to a file
     * @param rootUnit The root organizational unit to save
     * @param filePath The file path to save to
     * @return True if the save was successful, false otherwise
     */
    boolean save(OrganizationalUnit rootUnit, String filePath);

    /**
     * Load an organization chart from a file
     * @param filePath The file path to load from
     * @return The root organizational unit, or null if loading failed
     */
    OrganizationalUnit load(String filePath);
}
