package util;

/**
 * Utility class for handling and storing error messages
 * with the ability to retrieve the last error.
 */
public class ErrorManager {

    private static String lastErrorMessage = null;

    /**
     * Register an error message
     *
     * @param errorMessage The error message to register
     */
    public static void registerError(String errorMessage) {
        lastErrorMessage = errorMessage;
    }

    /**
     * Get the last registered error message
     *
     * @return The last error message, or null if no error was registered
     */
    public static String getLastErrorMessage() {
        return lastErrorMessage;
    }

    /**
     * Clear the last error message
     */
    public static void clearLastError() {
        lastErrorMessage = null;
    }
}