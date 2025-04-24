package view;

import controller.OrgChartManager;
import model.*;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for displaying and managing role details.
 */
public class RolePanel extends JPanel {
    private OrgChartManager manager;
    private Role currentRole;

    private JLabel nameLabel;
    private JLabel descriptionLabel;
    private JLabel unitLabel;
    private JPanel employeesPanel;

    /**
     * Constructor for RolePanel
     * @param manager The OrgChartManager controller instance
     */
    public RolePanel(OrgChartManager manager) {
        this.manager = manager;
        initializeUI();
    }

    /**
     * Initialize the UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout());

        // Header panel with role info
        JPanel headerPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Role Details"));

        headerPanel.add(new JLabel("Name:"));
        nameLabel = new JLabel();
        headerPanel.add(nameLabel);

        headerPanel.add(new JLabel("Description:"));
        descriptionLabel = new JLabel();
        headerPanel.add(descriptionLabel);

        headerPanel.add(new JLabel("Unit:"));
        unitLabel = new JLabel();
        headerPanel.add(unitLabel);

        add(headerPanel, BorderLayout.NORTH);

        // Employees panel
        employeesPanel = new JPanel(new BorderLayout());
        employeesPanel.setBorder(BorderFactory.createTitledBorder("Employees with this Role"));

        JPanel emptyPanel = new JPanel();
        emptyPanel.add(new JLabel("No role selected"));
        employeesPanel.add(emptyPanel, BorderLayout.CENTER);

        add(employeesPanel, BorderLayout.CENTER);
    }

    /**
     * Display the details of a role
     * @param role The role to display
     */
    public void displayRole(Role role) {
        this.currentRole = role;

        if (role == null) {
            nameLabel.setText("N/A");
            descriptionLabel.setText("N/A");
            unitLabel.setText("N/A");
            clearEmployees();
            return;
        }

        nameLabel.setText(role.getName());
        descriptionLabel.setText(role.getDescription());

        OrganizationalUnit unit = role.getUnit();
        unitLabel.setText(unit != null ? unit.getName() : "None");

        updateEmployees();
    }

    /**
     * Update the employees panel with employees having this role
     */
    private void updateEmployees() {
        employeesPanel.removeAll();

        if (currentRole == null) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.add(new JLabel("No role selected"));
            employeesPanel.add(emptyPanel, BorderLayout.CENTER);
        } else {
            // In a real application, you would have a more efficient way
            // to track which employees have which roles
            // This is simplified for the demo

            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

            // Aggiungiamo un'intestazione
            JLabel titleLabel = new JLabel("Dipendenti con il ruolo: " + currentRole.getName());
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            contentPanel.add(titleLabel);

            // Aggiungiamo elementi di esempio per testare lo scrolling
            for (int i = 1; i <= 20; i++) {
                JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                itemPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                itemPanel.add(new JLabel("Dipendente di esempio " + i));
                contentPanel.add(itemPanel);
                contentPanel.add(Box.createVerticalStrut(3)); // spaziatura
            }

            // Configuriamo lo scrollPane per supportare correttamente la rotellina del mouse
            JScrollPane scrollPane = new JScrollPane(contentPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Incremento dello scrolling per la rotellina
            scrollPane.setWheelScrollingEnabled(true); // Assicuriamo che lo scrolling con la rotellina sia abilitato

            // Aggiungiamo lo scrollPane al pannello principale
            employeesPanel.add(scrollPane, BorderLayout.CENTER);
        }

        employeesPanel.revalidate();
        employeesPanel.repaint();
    }

    /**
     * Clear the employees panel
     */
    private void clearEmployees() {
        employeesPanel.removeAll();

        JPanel emptyPanel = new JPanel();
        emptyPanel.add(new JLabel("No role selected"));
        employeesPanel.add(emptyPanel, BorderLayout.CENTER);

        employeesPanel.revalidate();
        employeesPanel.repaint();
    }
}
