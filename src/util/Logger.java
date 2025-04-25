package util;

import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for logging messages and showing error dialogs
 * in both graphical and headless environments.
 * This class is intended for internal logging only, while user-facing messages
 * should be handled by the UI components directly.
 */
public class Logger {

    private static final boolean IS_HEADLESS = GraphicsEnvironment.isHeadless();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Flag to control whether to show dialog boxes automatically
    private static boolean showDialogsAutomatically = false;

    // Flag to control debug level logging
    private static boolean debugMode = false;

    /**
     * Set whether to show dialog boxes automatically for error and info logs
     * @param show True to show dialogs, false to only log to console
     */
    public static void setShowDialogsAutomatically(boolean show) {
        showDialogsAutomatically = show;
    }

    /**
     * Get whether dialogs are shown automatically
     * @return True if dialogs are shown automatically
     */
    public static boolean getShowDialogsAutomatically() {
        return showDialogsAutomatically;
    }

    /**
     * Enable or disable debug mode for more verbose logging
     * @param enable True to enable debug logging, false to disable
     */
    public static void setDebugMode(boolean enable) {
        debugMode = enable;
    }

    /**
     * Check if debug mode is enabled
     * @return True if debug mode is enabled
     */
    public static boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Log an error message to the console and optionally register with ErrorManager
     *
     * @param message The error message to log
     * @param title The error category or title
     * @param registerWithErrorManager Whether to register this error with ErrorManager
     */
    public static void logError(String message, String title, boolean registerWithErrorManager) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);

        // Always log to console
        System.err.println(timestamp + " [ERROR] " + title + ": " + message);

        // Register with ErrorManager if requested
        if (registerWithErrorManager) {
            ErrorManager.registerError(message);
        }

        // Show a dialog only if configured to do so
        if (showDialogsAutomatically && !IS_HEADLESS) {
            try {
                JOptionPane.showMessageDialog(null,
                        message,
                        title,
                        JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                // Fallback if something goes wrong with the dialog
                System.err.println(timestamp + " [ERROR] Failed to show dialog: " + e.getMessage());
            }
        }
    }

    /**
     * Log an error message to the console without registering with ErrorManager
     *
     * @param message The error message to log
     * @param title The error category or title
     */
    public static void logError(String message, String title) {
        logError(message, title, false);
    }

    /**
     * Logs an error from the ErrorManager and optionally shows a dialog.
     * This method is intended for UI components to display errors from ErrorManager.
     *
     * @param showDialog Whether to show a dialog regardless of automatic dialog setting
     * @return The error message that was displayed, or null if no error was available
     */
    public static String showLastError(boolean showDialog) {
        String errorMessage = ErrorManager.getLastErrorMessage();
        if (errorMessage != null) {
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            System.err.println(timestamp + " [UI-ERROR] " + errorMessage);

            if (showDialog && !IS_HEADLESS) {
                try {
                    JOptionPane.showMessageDialog(null,
                            errorMessage,
                            "Errore",
                            JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    // Fallback if something goes wrong with the dialog
                    System.err.println(timestamp + " [ERROR] Failed to show dialog: " + e.getMessage());
                }
            }

            // Clear the error after showing it
            ErrorManager.clearLastError();
        }
        return errorMessage;
    }

    /**
     * Log an info message to the console
     *
     * @param message The information message to log
     * @param title The information category or title
     */
    public static void logInfo(String message, String title) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);

        // Always log to console
        System.out.println(timestamp + " [INFO] " + title + ": " + message);

        // Show a dialog only if configured to do so
        if (showDialogsAutomatically && !IS_HEADLESS) {
            try {
                JOptionPane.showMessageDialog(null,
                        message,
                        title,
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                // Fallback if something goes wrong with the dialog
                System.err.println(timestamp + " [ERROR] Failed to show dialog: " + e.getMessage());
            }
        }
    }

    /**
     * Log a debug message to the console (only if debug mode is enabled)
     *
     * @param message The debug message to log
     * @param title The debug category or title
     */
    public static void logDebug(String message, String title) {
        if (debugMode) {
            String timestamp = LocalDateTime.now().format(DATE_FORMAT);
            System.out.println(timestamp + " [DEBUG] " + title + ": " + message);
        }
    }

    /**
     * Log a warning message to the console
     *
     * @param message The warning message to log
     * @param title The warning category or title
     */
    public static void logWarning(String message, String title) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);

        // Always log to console
        System.out.println(timestamp + " [WARNING] " + title + ": " + message);

        // Show a dialog only if configured to do so
        if (showDialogsAutomatically && !IS_HEADLESS) {
            try {
                JOptionPane.showMessageDialog(null,
                        message,
                        title,
                        JOptionPane.WARNING_MESSAGE);
            } catch (Exception e) {
                // Fallback if something goes wrong with the dialog
                System.err.println(timestamp + " [ERROR] Failed to show dialog: " + e.getMessage());
            }
        }
    }

    /**
     * Show a confirmation dialog in graphical environments or default to true in headless environments
     *
     * @param message The confirmation message to display
     * @param title The dialog title (used only in graphical environments)
     * @return true if confirmed, false otherwise (always true in headless environments)
     */
    public static boolean confirm(String message, String title) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);

        // Log to console
        System.out.println(timestamp + " [CONFIRM] " + title + ": " + message);

        // Show dialog in graphical environments
        if (!IS_HEADLESS) {
            try {
                int result = JOptionPane.showConfirmDialog(null,
                        message,
                        title,
                        JOptionPane.YES_NO_OPTION);
                return result == JOptionPane.YES_OPTION;
            } catch (Exception e) {
                // Fallback if something goes wrong with the dialog
                System.err.println(timestamp + " [ERROR] Failed to show dialog: " + e.getMessage());
                return false;
            }
        }

        // Default to true in headless environments
        return true;
    }
}