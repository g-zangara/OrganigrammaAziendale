package view;

import controller.OrgChartManager;
import model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * Panel for displaying and managing organizational unit details.
 * Implements Observer pattern to refresh when data changes.
 */
public class UnitPanel extends JPanel implements OrgChartManager.Observer {
    private OrgChartManager manager;
    private OrganizationalUnit currentUnit;

    private JLabel nameLabel;
    private JLabel typeLabel;
    private JPanel rolesPanel;
    private JTable employeesTable;
    private DefaultTableModel employeesTableModel;

    /**
     * Constructor for UnitPanel
     * @param manager The OrgChartManager controller instance
     */
    public UnitPanel(OrgChartManager manager) {
        this.manager = manager;
        manager.addObserver(this); // Register as observer
        initializeUI();
    }

    /**
     * Observer pattern update method - called when data changes
     */
    @Override
    public void update() {
        if (currentUnit != null) {
            displayUnit(currentUnit);
        }
    }

    /**
     * Initialize the UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout());

        // Header panel with unit info
        JPanel headerPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        headerPanel.setBorder(BorderFactory.createTitledBorder("Unit Details"));

        headerPanel.add(new JLabel("Name:"));
        nameLabel = new JLabel();
        headerPanel.add(nameLabel);

        headerPanel.add(new JLabel("Type:"));
        typeLabel = new JLabel();
        headerPanel.add(typeLabel);

        add(headerPanel, BorderLayout.NORTH);

        // Tabbed pane for roles and employees
        JTabbedPane tabbedPane = new JTabbedPane();

        // Roles panel
        rolesPanel = new JPanel(new BorderLayout());
        // Nota: qui mettiamo solo il rolesPanel senza ScrollPane, poiché lo aggiungeremo in updateRoles()
        tabbedPane.addTab("Roles", rolesPanel);

        // Employees panel
        JPanel employeesPanel = new JPanel(new BorderLayout());

        // Add a header panel with "Add Employee" button
        JPanel employeeHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        employeeHeaderPanel.add(new JLabel("Employees:"));

        JButton addEmployeeButton = new JButton("Add Employee");
        addEmployeeButton.addActionListener(e -> {
            // Show dialog to add a new employee if we have a current unit
            if (currentUnit == null) {
                JOptionPane.showMessageDialog(this,
                        "No unit selected.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<Role> availableRoles = currentUnit.getRoles();
            if (availableRoles.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "This unit has no roles defined. Please add roles first.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create dialog
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    "Add New Employee", true);
            dialog.setLayout(new BorderLayout());

            JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            formPanel.add(new JLabel("Employee Name:"));
            JTextField nameField = new JTextField(20);
            formPanel.add(nameField);

            formPanel.add(new JLabel("Assign to Role:"));
            JComboBox<Role> roleComboBox = new JComboBox<>(availableRoles.toArray(new Role[0]));
            formPanel.add(roleComboBox);

            dialog.add(formPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(ev -> dialog.dispose());
            buttonPanel.add(cancelButton);

            JButton createButton = new JButton("Create");
            createButton.addActionListener(ev -> {
                String name = nameField.getText().trim();

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Please enter a name for the employee.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Role selectedRole = (Role) roleComboBox.getSelectedItem();

                // Creiamo sempre un nuovo employee con ID unico, indipendentemente dal nome
                Employee newEmployee = new Employee(name);
                // Il nuovo ID unico garantisce che possano esistere più dipendenti con lo stesso nome
                manager.assignEmployeeToRole(newEmployee, selectedRole, currentUnit);

                // Show confirmation
                JOptionPane.showMessageDialog(this,
                        "Employee '" + name + "' has been added and assigned to role '" +
                                selectedRole.getName() + "' in unit '" + currentUnit.getName() + "'.",
                        "Employee Added",
                        JOptionPane.INFORMATION_MESSAGE);

                dialog.dispose();

                // Update the employee list
                updateEmployees();
            });
            buttonPanel.add(createButton);

            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });
        employeeHeaderPanel.add(addEmployeeButton);
        employeesPanel.add(employeeHeaderPanel, BorderLayout.NORTH);

        // Table for employees
        String[] columnNames = {"Name", "Role", "Change Role", "Remove"};
        employeesTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 3; // Actions columns are editable
            }
        };

        employeesTable = new JTable(employeesTableModel);

        // Configuriamo i pulsanti di rimozione con il nuovo approccio
        setupRemoveButtons();

        // Miglioramento dello scrollPane per supportare la rotellina del mouse
        JScrollPane employeesScrollPane = new JScrollPane(employeesTable);
        employeesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        employeesScrollPane.getVerticalScrollBar().setUnitIncrement(16); // Incremento dello scrolling
        employeesPanel.add(employeesScrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Employees", employeesPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Display the details of an organizational unit
     * @param unit The unit to display
     */
    public void displayUnit(OrganizationalUnit unit) {
        this.currentUnit = unit;

        if (unit == null) {
            nameLabel.setText("N/A");
            typeLabel.setText("N/A");
            clearRoles();
            clearEmployees();
            return;
        }

        nameLabel.setText(unit.getName());
        typeLabel.setText(unit.getType());

        updateRoles();
        updateEmployees();
    }

    /**
     * Update the roles panel with current unit's roles
     */
    private void updateRoles() {
        rolesPanel.removeAll();

        if (currentUnit == null) {
            rolesPanel.revalidate();
            rolesPanel.repaint();
            return;
        }

        // Header panel at the top
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Roles in " + currentUnit.getName() + ":"));

        JButton addButton = new JButton("★ Aggiungi Ruolo ★");
        addButton.addActionListener(e -> {
            // Mostra una finestra di dialogo per aggiungere un nuovo ruolo
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    "Aggiungi Nuovo Ruolo", true);
            dialog.setLayout(new BorderLayout());

            JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Campo per il tipo di ruolo con menu a tendina
            formPanel.add(new JLabel("Tipo di Ruolo:"));

            // Determina i ruoli validi in base al tipo di unità
            String[] validRoleNames;
            String unitType;

            if (currentUnit instanceof model.Department) {
                // Per i dipartimenti: Direttore e Consigliere
                validRoleNames = new String[]{"Direttore", "Consigliere"};
                unitType = "Dipartimento";
            } else if (currentUnit instanceof model.Group) {
                // Per i gruppi: Coordinatore e Consigliere
                validRoleNames = new String[]{"Coordinatore", "Consigliere"};
                unitType = "Gruppo";
            } else {
                // Nel caso improbabile di un altro tipo di unità
                validRoleNames = new String[]{"Consigliere"};
                unitType = "Unità generica";
            }

            // Menu a tendina per selezionare il ruolo
            JComboBox<String> roleComboBox = new JComboBox<>(validRoleNames);
            formPanel.add(roleComboBox);

            // Campo per la descrizione
            formPanel.add(new JLabel("Descrizione:"));
            JTextField descField = new JTextField(20);
            formPanel.add(descField);

            dialog.add(formPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelButton = new JButton("Annulla");
            cancelButton.addActionListener(ev -> dialog.dispose());
            buttonPanel.add(cancelButton);

            JButton createButton = new JButton("Crea");
            createButton.addActionListener(ev -> {
                // Ottieni i valori inseriti dall'utente
                String roleName = (String) roleComboBox.getSelectedItem();
                String description = descField.getText().trim();

                // Verifica che sia stato selezionato un ruolo
                if (roleName == null || roleName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Seleziona un tipo di ruolo valido.", "Errore",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Verifica che non esista già un ruolo con lo stesso nome in questa unità
                for (Role existingRole : currentUnit.getRoles()) {
                    if (existingRole.getName().equalsIgnoreCase(roleName)) {
                        JOptionPane.showMessageDialog(dialog,
                                "Un ruolo con questo nome esiste già in questa unità.",
                                "Ruolo Duplicato",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // Crea il nuovo ruolo
                Role newRole = new Role(roleName, description);

                try {
                    // Tenta di aggiungere il ruolo all'unità
                    boolean added = manager.addRole(currentUnit, newRole);

                    // Se siamo arrivati qui, l'aggiunta è andata a buon fine
                    String successMessage = "Ruolo '" + roleName + "' aggiunto all'unità '" +
                            currentUnit.getName() + "' (" + unitType + ").";

                    // Registra il successo nel logger
                    util.Logger.logInfo(successMessage, "Operazione Completata");

                    // Mostra un messaggio di conferma all'utente
                    JOptionPane.showMessageDialog(dialog.getOwner(),
                            successMessage,
                            "Ruolo Aggiunto",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Chiudi la finestra di dialogo
                    dialog.dispose();
                } catch (model.ValidationException ex) {
                    // Gestione dell'errore di validazione

                    // Crea un messaggio di errore dettagliato
                    String errorMsg = "Errore nell'aggiunta del ruolo '" + roleName + "' all'unità '" +
                            currentUnit.getName() + "' (" + unitType + "): " + ex.getMessage();

                    // Registra l'errore nel logger e nell'ErrorManager
                    util.Logger.logError(errorMsg, "Errore di Validazione", true);

                    // Mostra un messaggio di errore all'utente
                    JOptionPane.showMessageDialog(dialog,
                            "Errore di validazione: " + ex.getMessage(),
                            "Errore Validazione",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            buttonPanel.add(createButton);

            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });
        headerPanel.add(addButton);

        // Content panel that will contain all the roles (inside a scroll pane)
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Add many roles for testing scrolling
        List<Role> roles = currentUnit.getRoles();
        if (roles.isEmpty()) {
            JLabel emptyLabel = new JLabel("No roles defined for this unit.");
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(emptyLabel);
        } else {
            // Add each role with its info and a remove button
            for (Role role : roles) {
                JPanel rolePanel = new JPanel(new BorderLayout());
                rolePanel.setBorder(BorderFactory.createEtchedBorder());
                rolePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
                rolePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel infoPanel = new JPanel(new GridLayout(2, 1));
                infoPanel.add(new JLabel("Name: " + role.getName()));
                infoPanel.add(new JLabel("Description: " + role.getDescription()));
                rolePanel.add(infoPanel, BorderLayout.CENTER);

                JButton removeButton = new JButton("Remove");
                removeButton.addActionListener(e -> {
                    // Prima verifichiamo se ci sono dipendenti associati a questo ruolo
                    if (manager.roleHasEmployees(role)) {
                        // Ruolo con dipendenti - Non può essere eliminato
                        JOptionPane.showMessageDialog(this,
                                "Impossibile rimuovere il ruolo '" + role.getName() +
                                        "' perché ci sono dipendenti associati ad esso.\n" +
                                        "Prima di eliminare il ruolo, è necessario riassegnare o rimuovere tutti i dipendenti associati.",
                                "Impossibile Rimuovere Ruolo",
                                JOptionPane.WARNING_MESSAGE);
                    } else {
                        // Ruolo senza dipendenti - Può essere eliminato
                        int choice = JOptionPane.showConfirmDialog(this,
                                "Sei sicuro di voler rimuovere il ruolo '" + role.getName() + "'?",
                                "Conferma Rimozione", JOptionPane.YES_NO_OPTION);

                        if (choice == JOptionPane.YES_OPTION) {
                            manager.removeRole(currentUnit, role);
                        }
                    }
                });

                JPanel buttonPanel = new JPanel();
                buttonPanel.add(removeButton);
                rolePanel.add(buttonPanel, BorderLayout.EAST);

                contentPanel.add(rolePanel);
                contentPanel.add(Box.createRigidArea(new Dimension(0, 5))); // spacing
            }

            // Add extra vertical space at the end for better scrolling
            contentPanel.add(Box.createVerticalGlue());
        }

        // Create a scroll pane with ALWAYS visible scrollbar for testing
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Faster scrolling
        scrollPane.setWheelScrollingEnabled(true); // Ensure mouse wheel works

        // Add to the rolesPanel with BorderLayout
        rolesPanel.setLayout(new BorderLayout());
        rolesPanel.add(headerPanel, BorderLayout.NORTH);
        rolesPanel.add(scrollPane, BorderLayout.CENTER);

        rolesPanel.revalidate();
        rolesPanel.repaint();
    }

    /**
     * Lista che memorizza gli Employee e Role per ogni riga della tabella
     * Necessario per trovare il dipendente e ruolo esatto quando si preme il pulsante Remove
     */
    private List<EmployeeRoleMapping> employeeRoleMappings = new ArrayList<>();

    /**
     * Classe interna per mantenere la mappatura tra riga della tabella e oggetti Employee/Role
     */
    private static class EmployeeRoleMapping {
        Employee employee;
        Role role;

        EmployeeRoleMapping(Employee employee, Role role) {
            this.employee = employee;
            this.role = role;
        }
    }

    /**
     * Update the employees table with current unit's employees
     */
    private void updateEmployees() {
        clearEmployees();

        if (currentUnit == null) {
            return;
        }

        // Puliamo la lista di mappature
        employeeRoleMappings.clear();

        // Otteniamo la lista dei dipendenti in questa unità
        List<Employee> employees = manager.getEmployeesInUnit(currentUnit);

        // Debug: stampiamo informazioni sui dipendenti trovati
        System.out.println("*** Aggiornamento tabella employees ***");
        System.out.println("Numero di dipendenti trovati nell'unità: " + employees.size());

        for (Employee employee : employees) {
            for (Role role : employee.getRoles()) {
                if (role.getUnit() == currentUnit) {
                    // Debug: stampiamo informazioni sulla riga che stiamo aggiungendo
                    System.out.println("Aggiunta riga: Dipendente=" + employee.getName() + ", Ruolo=" + role.getName());

                    // Aggiungiamo la riga alla tabella con il pulsante Change Role e Remove
                    Object[] rowData = {
                            employee.getName(),
                            role.getName(),
                            "Change Role",
                            "Remove"
                    };
                    employeesTableModel.addRow(rowData);

                    // Salviamo la mappatura tra riga e oggetti Employee/Role
                    employeeRoleMappings.add(new EmployeeRoleMapping(employee, role));
                }
            }
        }

        // Debug: stampiamo il numero totale di righe nella tabella
        System.out.println("Numero totale di righe nella tabella: " + employeesTableModel.getRowCount());
        System.out.println("Numero di mappature: " + employeeRoleMappings.size());
    }

    /**
     * Clear the roles panel
     */
    private void clearRoles() {
        rolesPanel.removeAll();
        rolesPanel.revalidate();
        rolesPanel.repaint();
    }

    /**
     * Clear the employees table
     */
    private void clearEmployees() {
        while (employeesTableModel.getRowCount() > 0) {
            employeesTableModel.removeRow(0);
        }
    }

    // Utilizziamo la ButtonRenderer esterna dal package view invece di questa interna

    /**
     * Setup dei pulsanti di rimozione e cambio ruolo usando ButtonRenderer e ButtonEditor
     * Questo approccio risolve il problema dell'editing del testo dei pulsanti
     */
    private void setupRemoveButtons() {
        // Configuriamo il renderer e l'editor per la colonna "Change Role"
        employeesTable.getColumnModel().getColumn(2).setCellRenderer(new ButtonRenderer());

        // Configuriamo il renderer e l'editor per la colonna "Remove"
        employeesTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());

        // Configura il pulsante "Change Role"
        employeesTable.getColumnModel().getColumn(2).setCellEditor(
                new ButtonEditor(new ButtonEditor.ButtonClickListener() {
                    @Override
                    public void buttonClicked(int row) {
                        if (row >= 0 && row < employeesTableModel.getRowCount() && row < employeeRoleMappings.size()) {
                            // Otteniamo direttamente i dati mappati per questa riga
                            EmployeeRoleMapping mapping = employeeRoleMappings.get(row);
                            Employee targetEmployee = mapping.employee;
                            Role currentRole = mapping.role;

                            // Ottenere la lista dei ruoli disponibili nell'unità
                            List<Role> availableRoles = currentUnit.getRoles();

                            // Se c'è solo un ruolo, non è possibile cambiarlo
                            if (availableRoles.size() <= 1) {
                                JOptionPane.showMessageDialog(UnitPanel.this,
                                        "Non ci sono altri ruoli disponibili nell'unità '" +
                                                currentUnit.getName() + "'. Prima crea nuovi ruoli.",
                                        "Cambio Ruolo Non Possibile",
                                        JOptionPane.WARNING_MESSAGE);
                                return;
                            }

                            // Creiamo la finestra di dialogo per cambiare ruolo
                            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(UnitPanel.this),
                                    "Cambia Ruolo", true);
                            dialog.setLayout(new BorderLayout());

                            JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
                            formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                            formPanel.add(new JLabel("Dipendente:"));
                            formPanel.add(new JLabel(targetEmployee.getName()));

                            formPanel.add(new JLabel("Nuovo Ruolo:"));

                            // Creiamo un elenco di ruoli che esclude quello attuale
                            List<Role> otherRoles = new ArrayList<>();
                            for (Role role : availableRoles) {
                                if (!role.equals(currentRole)) {
                                    otherRoles.add(role);
                                }
                            }

                            // Se non ci sono altri ruoli dopo aver escluso quello attuale
                            if (otherRoles.isEmpty()) {
                                JOptionPane.showMessageDialog(UnitPanel.this,
                                        "Non ci sono altri ruoli disponibili nell'unità '" +
                                                currentUnit.getName() + "'. Prima crea nuovi ruoli.",
                                        "Cambio Ruolo Non Possibile",
                                        JOptionPane.WARNING_MESSAGE);
                                return;
                            }

                            JComboBox<Role> roleComboBox = new JComboBox<>(otherRoles.toArray(new Role[0]));
                            formPanel.add(roleComboBox);

                            dialog.add(formPanel, BorderLayout.CENTER);

                            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                            JButton cancelButton = new JButton("Annulla");
                            cancelButton.addActionListener(ev -> dialog.dispose());
                            buttonPanel.add(cancelButton);

                            JButton changeButton = new JButton("Cambia");
                            changeButton.addActionListener(ev -> {
                                Role newRole = (Role) roleComboBox.getSelectedItem();

                                if (newRole != null) {
                                    // Cambiamo il ruolo dell'employee
                                    manager.changeEmployeeRole(targetEmployee, currentRole, newRole, currentUnit);

                                    // Mostriamo la conferma
                                    JOptionPane.showMessageDialog(UnitPanel.this,
                                            "Il ruolo del dipendente '" + targetEmployee.getName() +
                                                    "' è stato cambiato da '" + currentRole.getName() +
                                                    "' a '" + newRole.getName() + "'.",
                                            "Ruolo Cambiato",
                                            JOptionPane.INFORMATION_MESSAGE);

                                    dialog.dispose();

                                    // Aggiorniamo la vista
                                    displayUnit(currentUnit);
                                }
                            });
                            buttonPanel.add(changeButton);

                            dialog.add(buttonPanel, BorderLayout.SOUTH);
                            dialog.pack();
                            dialog.setLocationRelativeTo(UnitPanel.this);
                            dialog.setVisible(true);
                        }
                    }
                })
        );

        // Configura il pulsante "Remove"
        employeesTable.getColumnModel().getColumn(3).setCellEditor(
                new ButtonEditor(new ButtonEditor.ButtonClickListener() {
                    @Override
                    public void buttonClicked(int row) {
                        if (row >= 0 && row < employeesTableModel.getRowCount() && row < employeeRoleMappings.size()) {
                            // Otteniamo direttamente i dati mappati per questa riga
                            EmployeeRoleMapping mapping = employeeRoleMappings.get(row);
                            Employee targetEmployee = mapping.employee;
                            Role targetRole = mapping.role;

                            // Conferma rimozione
                            int choice = JOptionPane.showConfirmDialog(UnitPanel.this,
                                    "Sei sicuro di voler rimuovere " + targetEmployee.getName() +
                                            " dal ruolo " + targetRole.getName() + "?",
                                    "Conferma Rimozione", JOptionPane.YES_NO_OPTION);

                            if (choice == JOptionPane.YES_OPTION) {
                                // Rimuoviamo l'employee dal ruolo - ora utilizziamo direttamente gli oggetti dalla mappatura
                                manager.removeEmployeeFromRole(targetEmployee, targetRole, currentUnit);

                                // Mostriamo la conferma
                                JOptionPane.showMessageDialog(UnitPanel.this,
                                        "Dipendente '" + targetEmployee.getName() + "' è stato rimosso dal ruolo '" +
                                                targetRole.getName() + "' nell'unità '" + currentUnit.getName() + "'.",
                                        "Dipendente Rimosso",
                                        JOptionPane.INFORMATION_MESSAGE);

                                // Aggiorniamo la vista
                                displayUnit(currentUnit);
                            }
                        }
                    }
                })
        );
    }
}
