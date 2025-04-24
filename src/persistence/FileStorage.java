package persistence;

import java.io.File;

/**
 * Abstract base class for storage strategies that use files.
 * Provides common utility methods for file operations.
 */
public abstract class FileStorage implements StorageStrategy {

    /**
     * Check if a file exists at the given path
     * @param filePath The path to check
     * @return True if the file exists, false otherwise
     */
    protected boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        File file = new File(filePath);
        return file.exists() && file.isFile();
    }

    /**
     * Ensure that directories exist for a given file path
     * @param filePath The path to the file
     * @return True if directories exist or were created successfully, false otherwise
     */
    protected boolean createDirectories(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        File file = new File(filePath);
        File parentDir = file.getParentFile();

        // If parent directory doesn't exist, create it
        if (parentDir != null && !parentDir.exists()) {
            return parentDir.mkdirs();
        }
        return true;
    }
}