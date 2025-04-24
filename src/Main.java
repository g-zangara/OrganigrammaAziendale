/**
 * Main entry point for the Organization Chart Management application.
 * This class initializes the application and launches the GUI.
 */
public class Main {
    public static void main(String[] args) {
        try {
            // Set the look and feel to the system default
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Launch the application using SwingUtilities to ensure thread safety
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Initialize the main GUI
                view.OrgChartGUI gui = new view.OrgChartGUI();

                // Show the new chart dialog at startup
                // If the user cancels or closes the dialog, exit the application
                if (!gui.showNewChartDialog()) {
                    System.exit(0);
                }

                // Display the main GUI only if a chart was created/opened
                gui.setVisible(true);
            }
        });
    }
}
