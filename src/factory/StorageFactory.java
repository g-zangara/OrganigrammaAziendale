package factory;

import persistence.CsvStorage;
import persistence.DbmsStorage;
import persistence.JsonStorage;
import persistence.StorageStrategy;

/**
 * Factory class to create appropriate StorageStrategy instances.
 * This implements the Factory pattern for storage strategies.
 */
public class StorageFactory {

    /**
     * Storage format types
     */
    public enum StorageType {
        JSON,
        CSV,
        DBMS
    }

    /**
     * Create a StorageStrategy based on the specified type
     * @param type The type of storage strategy to create
     * @return A new StorageStrategy instance of the appropriate type
     */
    public static StorageStrategy createStorageStrategy(StorageType type) {
        switch (type) {
            case JSON:
                return new JsonStorage();
            case CSV:
                return new CsvStorage();
            case DBMS:
                return new DbmsStorage("orgchart.db");
            default:
                throw new IllegalArgumentException("Unknown storage type: " + type);
        }
    }

    /**
     * Determine the appropriate file extension based on storage type
     * @param type The storage type
     * @return The appropriate file extension including the dot
     */
    public static String getFileExtension(StorageType type) {
        switch (type) {
            case JSON:
                return ".json";
            case CSV:
                return ".csv";
            case DBMS:
                return ".db";
            default:
                return "";
        }
    }
}