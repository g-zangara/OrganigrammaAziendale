package view;

import controller.OrgChartManager;
import model.*;
import command.Command;
import command.RemoveRoleCommand;
import command.CommandManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Field;

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

                System.out.println("Debug: Tentativo di aggiungere dipendente '" + name +
                        "' al ruolo '" + selectedRole.getName() +
                        "' nell'unità '" + currentUnit.getName() + "'");

                try {
                    boolean success = manager.assignEmployeeToRole(newEmployee, selectedRole, currentUnit);

                    System.out.println("Debug: Risultato aggiunta dipendente: " + (success ? "SUCCESS" : "FAILURE"));
                    System.out.println("Debug: Numero di dipendenti nell'unità dopo aggiunta: " +
                            manager.getEmployeesInUnit(currentUnit).size());

                    // Stampa ruoli per debug
                    for (Role r : currentUnit.getRoles()) {
                        System.out.println("Debug: Ruolo '" + r.getName() + "' ha " +
                                r.getEmployees().size() + " dipendenti");
                        for (Employee empDebug : r.getEmployees()) {
                            System.out.println("Debug: - Dipendente: " + empDebug.getName() + " (ID: " + empDebug.getUniqueId() + ")");
                        }
                    }

                    // Show confirmation
                    JOptionPane.showMessageDialog(this,
                            "Employee '" + name + "' has been added and assigned to role '" +
                                    selectedRole.getName() + "' in unit '" + currentUnit.getName() + "'.",
                            "Employee Added",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (ValidationException ex) {
                    System.out.println("Debug: Errore nell'aggiunta del dipendente: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this,
                            "Error assigning employee: " + ex.getMessage(),
                            "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                }

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

        System.out.println("Debug displayUnit: Mostrando unità " + unit.getName() + " con " +
                (unit.getRoles() != null ? unit.getRoles().size() : 0) + " ruoli");

        // Stampa info su ruoli
        for (Role r : unit.getRoles()) {
            System.out.println("Debug displayUnit: Ruolo " + r.getName() + " ha " +
                    (r.getEmployees() != null ? r.getEmployees().size() : 0) + " dipendenti");
            for (Employee emp : r.getEmployees()) {
                System.out.println("Debug displayUnit: - Dipendente " + emp.getName() + " (ID: " + emp.getUniqueId() + ")");
            }
        }

        // Stampa info sul mapping di dipendenti
        List<Employee> employees = manager.getEmployeesInUnit(unit);
        System.out.println("Debug displayUnit: Trovati " + employees.size() + " dipendenti nel mapping per l'unità");
        for (Employee emp : employees) {
            System.out.println("Debug displayUnit: - Dipendente mappato: " + emp.getName() + " (ID: " + emp.getUniqueId() + ")");
        }

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
                // Per i dipartimenti: ruoli di base e specializzati - DEVONO corrispondere ESATTAMENTE ai nomi in RoleType.java
                validRoleNames = new String[]{
                        "Direttore", "Consigliere", "Responsabile Amministrativo",
                        "Referente Tecnico", "Responsabile Commerciale",
                        "Responsabile Risorse Umane", "Responsabile Logistica",
                        "Analista", "Consulente", "Data Protection Officer",
                        "Chief Financial Officer", "Chief Technology Officer",
                        "HR Specialist", "Quality Assurance Manager"
                };
                unitType = "Dipartimento";
            } else if (currentUnit instanceof model.Group) {
                // Per i gruppi: ruoli di base e specializzati - DEVONO corrispondere ESATTAMENTE ai nomi in RoleType.java
                validRoleNames = new String[]{
                        "Coordinatore", "Consigliere", "Team Leader",
                        "Tutor", "Collaboratore", "Membro", "Stagista"
                };
                unitType = "Gruppo";
            } else if (currentUnit instanceof model.Board) {
                // Per i board: ruoli specifici del board - DEVONO corrispondere ESATTAMENTE ai nomi in RoleType.java
                validRoleNames = new String[]{
                        "Presidente", "Vicepresidente", "Segretario"
                };
                unitType = "Board";
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

                // Creiamo un comando per l'operazione
                Command addRoleCommand = new command.AddRoleCommand(currentUnit, newRole);

                // Otteniamo il CommandManager dalla finestra principale
                CommandManager commandManager = ((OrgChartGUI) SwingUtilities.getWindowAncestor(this)).getCommandManager();

                // Eseguiamo il comando tramite il CommandManager per supportare undo/redo
                boolean added = commandManager.executeCommand(addRoleCommand);

                if (added) {
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
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "Non è stato possibile aggiungere il ruolo.",
                            "Errore", JOptionPane.ERROR_MESSAGE);
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
                            // Creiamo un comando per la rimozione del ruolo
                            Command removeRoleCommand = new RemoveRoleCommand(currentUnit, role);

                            // Otteniamo il CommandManager dalla finestra principale
                            CommandManager commandManager = ((OrgChartGUI) SwingUtilities.getWindowAncestor(this)).getCommandManager();

                            // Eseguiamo il comando tramite il CommandManager per supportare undo/redo
                            boolean removed = commandManager.executeCommand(removeRoleCommand);

                            if (removed) {
                                String successMessage = "Ruolo '" + role.getName() + "' rimosso dall'unità '" + currentUnit.getName() + "'.";
                                util.Logger.logInfo(successMessage, "Operazione Completata");
                            } else {
                                JOptionPane.showMessageDialog(this,
                                        "Non è stato possibile rimuovere il ruolo.",
                                        "Errore", JOptionPane.ERROR_MESSAGE);
                            }
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

        // Debug: stampiamo informazioni sui dipendenti trovati
        System.out.println("*** Aggiornamento tabella employees ***");

        // Otteniamo la lista dei dipendenti in questa unità tramite la mappa in OrgChartManager
        List<Employee> employeesFromMap = manager.getEmployeesInUnit(currentUnit);
        System.out.println("Numero di dipendenti trovati nella mappa: " + employeesFromMap.size());

        // Verifica lo stato della mappa di dipendenti
        Map<OrganizationalUnit, List<Employee>> empMap = getEmployeesMapForDebug();
        System.out.println("Numero di mappature: " + (empMap != null ? empMap.size() : 0));

        // Stampa i ruoli disponibili nell'unità corrente
        System.out.println("Ruoli nell'unità: " + currentUnit.getRoles().size());

        // Raccogliamo tutti i dipendenti dai ruoli dell'unità corrente
        Set<Employee> allEmployeesInUnit = new HashSet<>();

        // Iteriamo per ogni ruolo nell'unità
        for (Role r : currentUnit.getRoles()) {
            System.out.println(" - Ruolo: " + r.getName() + ", Dipendenti: " + r.getEmployees().size());

            // Per ogni dipendente nel ruolo
            for (Employee employee : r.getEmployees()) {
                System.out.println("   * Dipendente: " + employee.getName() + " (ID: " + employee.getUniqueId() + ")");

                // Aggiungiamo il dipendente al set
                allEmployeesInUnit.add(employee);

                // Debug: stampiamo informazioni sulla riga che stiamo aggiungendo
                System.out.println("Aggiunta riga: Dipendente=" + employee.getName() + ", Ruolo=" + r.getName());

                // Aggiungiamo la riga alla tabella con il pulsante Change Role e Remove
                Object[] rowData = {
                        employee.getName(),
                        r.getName(),
                        "Change Role",
                        "Remove"
                };
                employeesTableModel.addRow(rowData);

                // Salviamo la mappatura tra riga e oggetti Employee/Role
                employeeRoleMappings.add(new EmployeeRoleMapping(employee, r));

                // Assicuriamoci che il dipendente sia nella mappa in OrgChartManager
                if (!employeesFromMap.contains(employee)) {
                    System.out.println("   # Attenzione: Dipendente " + employee.getName() +
                            " trovato nel ruolo ma non nella mappa di OrgChartManager!");
                }
            }
        }

        // Verifichiamo se ci sono dipendenti nella mappa ma non nei ruoli
        for (Employee employee : employeesFromMap) {
            if (!allEmployeesInUnit.contains(employee)) {
                System.out.println("   # Attenzione: Dipendente " + employee.getName() +
                        " trovato nella mappa ma non in nessun ruolo dell'unità!");
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

    /**
     * Ottiene la mappa degli impiegati solo per debug
     * @return Mappa degli impiegati per unità
     */
    private Map<OrganizationalUnit, List<Employee>> getEmployeesMapForDebug() {
        try {
            // Utilizziamo reflection per accedere alla mappa privata in OrgChartManager
            Field employeesByUnitField = OrgChartManager.class.getDeclaredField("employeesByUnit");
            employeesByUnitField.setAccessible(true);
            return (Map<OrganizationalUnit, List<Employee>>) employeesByUnitField.get(manager);
        } catch (Exception e) {
            System.out.println("Errore nell'accesso a employeesByUnit: " + e.getMessage());
            return null;
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
                            Role role = mapping.role;

                            // Chiediamo conferma all'utente
                            int choice = JOptionPane.showConfirmDialog(UnitPanel.this,
                                    "Sei sicuro di voler rimuovere il dipendente '" + targetEmployee.getName() +
                                            "' dal ruolo '" + role.getName() + "'?",
                                    "Conferma Rimozione", JOptionPane.YES_NO_OPTION);

                            if (choice == JOptionPane.YES_OPTION) {
                                // Rimuovi il dipendente dal ruolo
                                manager.removeEmployeeFromRole(targetEmployee, role, currentUnit);

                                // Mostra conferma
                                JOptionPane.showMessageDialog(UnitPanel.this,
                                        "Il dipendente '" + targetEmployee.getName() +
                                                "' è stato rimosso dal ruolo '" + role.getName() + "'.",
                                        "Dipendente Rimosso",
                                        JOptionPane.INFORMATION_MESSAGE);

                                // Aggiorna la vista
                                displayUnit(currentUnit);
                            }
                        }
                    }
                })
        );
    }
}