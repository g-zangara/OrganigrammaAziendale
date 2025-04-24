package view;

import controller.OrgChartManager;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel for displaying and managing employee details.
 */
public class EmployeePanel extends JPanel {
    private OrgChartManager manager;
    private Employee currentEmployee;

    private JLabel nameLabel;
    private JPanel rolesPanel;

    /**
     * Constructor for EmployeePanel
     * @param manager The OrgChartManager controller instance
     */
    public EmployeePanel(OrgChartManager manager) {
        this.manager = manager;
        initializeUI();
    }

    /**
     * Initialize the UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout());

        // Header panel with employee info
        JPanel headerPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Employee Details"));

        headerPanel.add(new JLabel("Name:"));
        nameLabel = new JLabel();
        headerPanel.add(nameLabel);

        add(headerPanel, BorderLayout.NORTH);

        // Roles panel
        rolesPanel = new JPanel(new BorderLayout());
        rolesPanel.setBorder(BorderFactory.createTitledBorder("Roles"));

        JPanel emptyPanel = new JPanel();
        emptyPanel.add(new JLabel("No employee selected"));
        rolesPanel.add(emptyPanel, BorderLayout.CENTER);

        add(rolesPanel, BorderLayout.CENTER);
    }

    /**
     * Display the details of an employee
     * @param employee The employee to display
     */
    public void displayEmployee(Employee employee) {
        this.currentEmployee = employee;

        if (employee == null) {
            nameLabel.setText("N/A");
            clearRoles();
            return;
        }

        nameLabel.setText(employee.getName());
        updateRoles();
    }

    /**
     * Update the roles panel with employee's roles
     */
    private void updateRoles() {
        rolesPanel.removeAll();

        if (currentEmployee == null) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.add(new JLabel("No employee selected"));
            rolesPanel.add(emptyPanel, BorderLayout.CENTER);
        } else {
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            headerPanel.add(new JLabel("Roles for " + currentEmployee.getName() + ":"));

            JButton addButton = new JButton("Add Role");
            addButton.addActionListener(e -> {
                // Show dialog to assign a new role to the employee
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                        "Assign Role", true);
                dialog.setLayout(new BorderLayout());

                JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
                formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                formPanel.add(new JLabel("Select Unit:"));
                // In a real app, get all units from the manager
                JComboBox<OrganizationalUnit> unitComboBox = new JComboBox<>();
                formPanel.add(unitComboBox);

                formPanel.add(new JLabel("Select Role:"));
                JComboBox<Role> roleComboBox = new JComboBox<>();
                formPanel.add(roleComboBox);

                // Update available roles when unit changes
                unitComboBox.addActionListener(ev -> {
                    OrganizationalUnit selectedUnit =
                            (OrganizationalUnit) unitComboBox.getSelectedItem();
                    roleComboBox.removeAllItems();

                    if (selectedUnit != null) {
                        for (Role role : selectedUnit.getRoles()) {
                            roleComboBox.addItem(role);
                        }
                    }
                });

                dialog.add(formPanel, BorderLayout.CENTER);

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(ev -> dialog.dispose());
                buttonPanel.add(cancelButton);

                JButton assignButton = new JButton("Assign");
                assignButton.addActionListener(ev -> {
                    OrganizationalUnit selectedUnit =
                            (OrganizationalUnit) unitComboBox.getSelectedItem();
                    Role selectedRole =
                            (Role) roleComboBox.getSelectedItem();

                    if (selectedUnit == null || selectedRole == null) {
                        JOptionPane.showMessageDialog(dialog,
                                "Please select a unit and role.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    manager.assignEmployeeToRole(currentEmployee, selectedRole, selectedUnit);

                    dialog.dispose();
                });
                buttonPanel.add(assignButton);

                dialog.add(buttonPanel, BorderLayout.SOUTH);
                dialog.pack();
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);
            });
            headerPanel.add(addButton);

            rolesPanel.add(headerPanel, BorderLayout.NORTH);

            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

            List<Role> roles = currentEmployee.getRoles();
            if (roles.isEmpty()) {
                listPanel.add(new JLabel("This employee has no assigned roles."));
            } else {
                for (Role role : roles) {
                    OrganizationalUnit unit = role.getUnit();

                    JPanel rolePanel = new JPanel(new BorderLayout());
                    rolePanel.setBorder(BorderFactory.createEtchedBorder());

                    JPanel infoPanel = new JPanel(new GridLayout(2, 1));
                    infoPanel.add(new JLabel("Role: " + role.getName()));
                    infoPanel.add(new JLabel("Unit: " +
                            (unit != null ? unit.getName() : "None")));
                    rolePanel.add(infoPanel, BorderLayout.CENTER);

                    JButton removeButton = new JButton("Remove");
                    removeButton.addActionListener(e -> {
                        int choice = JOptionPane.showConfirmDialog(this,
                                "Are you sure you want to remove this role assignment?",
                                "Confirm Removal", JOptionPane.YES_NO_OPTION);

                        if (choice == JOptionPane.YES_OPTION) {
                            manager.removeEmployeeFromRole(currentEmployee, role, unit);
                        }
                    });

                    JPanel buttonPanel = new JPanel();
                    buttonPanel.add(removeButton);
                    rolePanel.add(buttonPanel, BorderLayout.EAST);

                    listPanel.add(rolePanel);
                    listPanel.add(Box.createVerticalStrut(5)); // spacing
                }
            }

            JScrollPane scrollPane = new JScrollPane(listPanel);
            rolesPanel.add(scrollPane, BorderLayout.CENTER);
        }

        rolesPanel.revalidate();
        rolesPanel.repaint();
    }

    /**
     * Clear the roles panel
     */
    private void clearRoles() {
        rolesPanel.removeAll();

        JPanel emptyPanel = new JPanel();
        emptyPanel.add(new JLabel("No employee selected"));
        rolesPanel.add(emptyPanel, BorderLayout.CENTER);

        rolesPanel.revalidate();
        rolesPanel.repaint();
    }
}
