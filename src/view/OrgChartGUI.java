package view;

import controller.OrgChartManager;
import factory.StorageFactory;
import model.*;
import factory.UnitFactory;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.util.*;

/**
 * Main application window for the Organization Chart Manager.
 * Implements the Observer pattern to receive updates from the controller.
 */
public class OrgChartGUI extends JFrame implements OrgChartManager.Observer {
    private OrgChartManager manager;
    private JTree orgTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;

    private JPanel detailPanel;
    private UnitPanel unitPanel;
    private RolePanel rolePanel;
    private EmployeePanel employeePanel;

    private Map<OrganizationalUnit, DefaultMutableTreeNode> unitToNodeMap;

    /**
     * Constructor for the main GUI window
     */
    public OrgChartGUI() {
        manager = OrgChartManager.getInstance();
        manager.addObserver(this);
        unitToNodeMap = new HashMap<>();

        initializeUI();

        // Create a default org chart if none exists
        if (manager.getRootUnit() == null) {
            manager.createNewOrgChart("Company");
        }
    }

    /**
     * Initialize the UI components
     */
    private void initializeUI() {
        setTitle("Organization Chart Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Create the menu bar
        createMenuBar();

        // Create the main layout with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);

        // Create the tree view for the org chart
        createTreeView();
        JScrollPane treeScrollPane = new JScrollPane(orgTree);
        splitPane.setLeftComponent(treeScrollPane);

        // Create the detail panel for showing unit/role/employee details
        createDetailPanel();
        splitPane.setRightComponent(detailPanel);

        getContentPane().add(splitPane, BorderLayout.CENTER);

        // Create status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        JLabel statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        // Set default expanding of the tree after initialization
        SwingUtilities.invokeLater(() -> {
            // Ensure tree is always visible and can be interacted with
            orgTree.setSelectionRow(0);
            // Force tree to expand root
            orgTree.expandPath(new TreePath(rootNode.getPath()));
        });
    }

    /**
     * Create the application menu bar
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> showNewChartDialog());
        fileMenu.add(newItem);

        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();

            // Add file filters
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "JSON Files (*.json)", "json"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "CSV Files (*.csv)", "csv"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Database Files (*.db)", "db"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Java Serialization Files (*.ser)", "ser"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Text Files (*.txt)", "txt"));



            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String filePath = selectedFile.getAbsolutePath();

                // Determine appropriate storage strategy based on file extension
                if (filePath.toLowerCase().endsWith(".json")) {
                    manager.setStorageStrategy(StorageFactory.createStorageStrategy(
                            StorageFactory.StorageType.JSON));
                } else if (filePath.toLowerCase().endsWith(".csv")) {
                    manager.setStorageStrategy(StorageFactory.createStorageStrategy(
                            StorageFactory.StorageType.CSV));
                } else if (filePath.toLowerCase().endsWith(".db")) {
                    manager.setStorageStrategy(StorageFactory.createStorageStrategy(
                            StorageFactory.StorageType.DBMS));
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Unsupported file format. Please use .json, .csv, or .db extension.",
                            "Invalid File Format", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                boolean success = manager.loadOrgChart(filePath);
                if (success) {
                    // Aggiorna l'albero e seleziona l'unità radice
                    updateTree();

                    // Seleziona automaticamente l'unità radice caricata
                    OrganizationalUnit rootUnit = manager.getRootUnit();
                    if (rootUnit != null) {
                        selectAndShowUnit(rootUnit);
                    }

                    JOptionPane.showMessageDialog(this,
                            "Organization chart loaded successfully.",
                            "Load Successful", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to load organization chart.",
                            "Load Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> {
            if (manager.getRootUnit() == null) {
                JOptionPane.showMessageDialog(this,
                        "No organization chart to save.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Ask for the storage format
            Object[] options = {
                    "JSON (.json)",
                    "CSV (.csv)",
                    "Database (.db)"
            };
            int formatChoice = JOptionPane.showOptionDialog(this,
                    "Select the file format for saving:",
                    "Save Format",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            // Set the appropriate storage strategy
            StorageFactory.StorageType storageType;
            switch (formatChoice) {
                case 0:
                    storageType = StorageFactory.StorageType.JSON;
                    break;
                case 1:
                    storageType = StorageFactory.StorageType.CSV;
                    break;
                case 2:
                    storageType = StorageFactory.StorageType.DBMS;
                    break;
                default:
                    storageType = StorageFactory.StorageType.JSON; // Default to JSON
                    break;
            }
            manager.setStorageStrategy(StorageFactory.createStorageStrategy(storageType));

            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                // Add appropriate extension if missing
                String path = selectedFile.getAbsolutePath();
                String extension = StorageFactory.getFileExtension(storageType);
                if (!path.toLowerCase().endsWith(extension)) {
                    path += extension;
                }

                boolean success = manager.saveOrgChart(path);
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Organization chart saved successfully.",
                            "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to save organization chart.",
                            "Save Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");

        JMenuItem addUnitItem = new JMenuItem("Add Unit");
        addUnitItem.addActionListener(e -> showAddUnitDialog());
        editMenu.add(addUnitItem);

        JMenuItem removeUnitItem = new JMenuItem("Remove Unit");
        removeUnitItem.addActionListener(e -> removeSelectedUnit());
        editMenu.add(removeUnitItem);

        editMenu.addSeparator();

        JMenuItem addRoleItem = new JMenuItem("Add Role");
        addRoleItem.addActionListener(e -> showAddRoleDialog());
        editMenu.add(addRoleItem);

        JMenuItem addEmployeeItem = new JMenuItem("Add Employee");
        addEmployeeItem.addActionListener(e -> showAddEmployeeDialog());
        editMenu.add(addEmployeeItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e ->
                JOptionPane.showMessageDialog(this,
                        "Organization Chart Manager\nVersion 1.0",
                        "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);

        // Aggiunta dei menu alla barra
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * Create the tree view component for displaying the org chart
     */
    private void createTreeView() {
        rootNode = new DefaultMutableTreeNode("No Organization");
        treeModel = new DefaultTreeModel(rootNode);
        orgTree = new JTree(treeModel);
        orgTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        orgTree.setShowsRootHandles(true);

        // Customize the tree appearance
        orgTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                          boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                // Customize the node label
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof OrganizationalUnit) {
                    OrganizationalUnit unit = (OrganizationalUnit) userObject;
                    String type = unit.getType();

                    // Show both type and name for clarity
                    setText(type + ": " + unit.getName());

                    // Icons could be customized based on unit type
                    // For now, we'll use the default
                }

                return this;
            }
        });

        // Set row height for better visibility
        orgTree.setRowHeight(24);

        // Add listener to show details when a node is selected
        orgTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        orgTree.getLastSelectedPathComponent();

                if (node == null) return;

                Object nodeInfo = node.getUserObject();
                if (nodeInfo instanceof OrganizationalUnit) {
                    OrganizationalUnit unit = (OrganizationalUnit) nodeInfo;
                    showUnitDetails(unit);
                }
            }
        });

        // Add context menu for tree
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem addUnitMenuItem = new JMenuItem("Add Unit");
        addUnitMenuItem.addActionListener(e -> showAddUnitDialog());
        popupMenu.add(addUnitMenuItem);

        JMenuItem removeUnitMenuItem = new JMenuItem("Remove Unit");
        removeUnitMenuItem.addActionListener(e -> removeSelectedUnit());
        popupMenu.add(removeUnitMenuItem);

        popupMenu.addSeparator();

        JMenuItem addRoleMenuItem = new JMenuItem("Add Role");
        addRoleMenuItem.addActionListener(e -> showAddRoleDialog());
        popupMenu.add(addRoleMenuItem);

        JMenuItem addEmployeeMenuItem = new JMenuItem("Add Employee");
        addEmployeeMenuItem.addActionListener(e -> showAddEmployeeDialog());
        popupMenu.add(addEmployeeMenuItem);

        orgTree.setComponentPopupMenu(popupMenu);
    }

    /**
     * Create the detail panel for displaying selected items
     */
    private void createDetailPanel() {
        detailPanel = new JPanel(new CardLayout());

        // Create panels for different types of details
        unitPanel = new UnitPanel(manager);
        rolePanel = new RolePanel(manager);
        employeePanel = new EmployeePanel(manager);

        detailPanel.add(unitPanel, "UNIT");
        detailPanel.add(rolePanel, "ROLE");
        detailPanel.add(employeePanel, "EMPLOYEE");

        // Default to unit view
        CardLayout cardLayout = (CardLayout) detailPanel.getLayout();
        cardLayout.show(detailPanel, "UNIT");
    }

    /**
     * Show details for a selected organizational unit
     */
    private void showUnitDetails(OrganizationalUnit unit) {
        unitPanel.displayUnit(unit);
        CardLayout cardLayout = (CardLayout) detailPanel.getLayout();
        cardLayout.show(detailPanel, "UNIT");
    }

    /**
     * Display a dialog to add a new organizational unit
     */
    private void showAddUnitDialog() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                orgTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a parent unit first.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Object nodeInfo = selectedNode.getUserObject();
        if (!(nodeInfo instanceof OrganizationalUnit)) {
            return;
        }

        OrganizationalUnit parentUnit = (OrganizationalUnit) nodeInfo;

        // Create dialog
        JDialog dialog = new JDialog(this, "Add New Unit", true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        formPanel.add(new JLabel("Unit Type:"));
        String[] unitTypes = {"Department", "Group"};
        JComboBox<String> typeComboBox = new JComboBox<>(unitTypes);
        formPanel.add(typeComboBox);

        formPanel.add(new JLabel("Name:"));
        JTextField nameField = new JTextField(20);
        formPanel.add(nameField);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter a name for the unit.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String type = (String) typeComboBox.getSelectedItem();

            // Use Factory to create appropriate unit type
            OrganizationalUnit newUnit = UnitFactory.createUnit(type, name);
            manager.addUnit(parentUnit, newUnit);

            // Show confirmation
            JOptionPane.showMessageDialog(this,
                    "Unit '" + name + "' has been added to '" + parentUnit.getName() + "'.",
                    "Unit Added",
                    JOptionPane.INFORMATION_MESSAGE);

            dialog.dispose();

            // Make sure the tree selects the new unit node and updates the details panel
            SwingUtilities.invokeLater(() -> {
                // Find the newly added node in the tree
                DefaultMutableTreeNode parentNode = unitToNodeMap.get(parentUnit);
                if (parentNode != null) {
                    // Look for the new unit's node among children
                    for (int i = 0; i < parentNode.getChildCount(); i++) {
                        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
                        Object userObject = childNode.getUserObject();
                        if (userObject instanceof OrganizationalUnit) {
                            OrganizationalUnit unit = (OrganizationalUnit) userObject;
                            if (unit.equals(newUnit)) {
                                // Select this node
                                TreeNode[] pathToNode = treeModel.getPathToRoot(childNode);
                                TreePath path = new TreePath(pathToNode);
                                orgTree.setSelectionPath(path);
                                orgTree.scrollPathToVisible(path);
                                showUnitDetails(unit);
                                break;
                            }
                        }
                    }
                }
            });
        });
        buttonPanel.add(createButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Remove the currently selected organizational unit
     */
    private void removeSelectedUnit() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                orgTree.getLastSelectedPathComponent();

        if (selectedNode == null || selectedNode == rootNode) {
            JOptionPane.showMessageDialog(this,
                    "Please select a unit to remove.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Object selectedObject = selectedNode.getUserObject();
        if (!(selectedObject instanceof OrganizationalUnit)) {
            return;
        }

        OrganizationalUnit selectedUnit = (OrganizationalUnit) selectedObject;

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete " + selectedUnit.getName() + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
            if (parentNode != null) {
                Object parentObject = parentNode.getUserObject();
                if (parentObject instanceof OrganizationalUnit) {
                    OrganizationalUnit parentUnit = (OrganizationalUnit) parentObject;
                    manager.removeUnit(parentUnit, selectedUnit);

                    // Show confirmation
                    JOptionPane.showMessageDialog(this,
                            "Unit '" + selectedUnit.getName() + "' has been removed.",
                            "Unit Removed",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Select the parent node and show its details
                    TreeNode[] pathToParent = treeModel.getPathToRoot(parentNode);
                    TreePath path = new TreePath(pathToParent);
                    orgTree.setSelectionPath(path);
                    orgTree.scrollPathToVisible(path);
                    showUnitDetails(parentUnit);
                }
            }
        }
    }

    /**
     * Display a dialog to add a new role to the selected unit
     */
    private void showAddRoleDialog() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                orgTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a unit first.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Object nodeInfo = selectedNode.getUserObject();
        if (!(nodeInfo instanceof OrganizationalUnit)) {
            return;
        }

        OrganizationalUnit unit = (OrganizationalUnit) nodeInfo;

        // Create dialog
        JDialog dialog = new JDialog(this, "Add New Role", true);
        dialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        formPanel.add(new JLabel("Role Name:"));
        JTextField nameField = new JTextField(20);
        formPanel.add(nameField);

        formPanel.add(new JLabel("Description:"));
        JTextField descField = new JTextField(20);
        formPanel.add(descField);

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String description = descField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter a name for the role.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            Role newRole = new Role(name, description);
            manager.addRole(unit, newRole);

            // Show confirmation
            JOptionPane.showMessageDialog(this,
                    "Role '" + name + "' has been added to unit '" + unit.getName() + "'.",
                    "Role Added",
                    JOptionPane.INFORMATION_MESSAGE);

            dialog.dispose();

            // Force refresh the entire view
            update();

            // Select the unit node to display details
            selectAndShowUnit(unit);
        });
        buttonPanel.add(createButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Display a dialog to add a new employee to the selected unit
     */
    private void showAddEmployeeDialog() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                orgTree.getLastSelectedPathComponent();

        if (selectedNode == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a unit first.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Object nodeInfo = selectedNode.getUserObject();
        if (!(nodeInfo instanceof OrganizationalUnit)) {
            return;
        }

        // Get the selected unit
        OrganizationalUnit unit = (OrganizationalUnit) nodeInfo;

        // First show the unit panel to make it visible and active
        unitPanel.displayUnit(unit);
        CardLayout cardLayout = (CardLayout) detailPanel.getLayout();
        cardLayout.show(detailPanel, "UNIT");

        // Check if the unit has roles
        java.util.List<Role> availableRoles = unit.getRoles();

        if (availableRoles.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "This unit has no roles defined. Please add roles first.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create dialog for adding an employee
        JDialog dialog = new JDialog(this, "Add New Employee", true);
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
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> {
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter a name for the employee.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            Role selectedRole = (Role) roleComboBox.getSelectedItem();

            Employee newEmployee = new Employee(name);
            manager.assignEmployeeToRole(newEmployee, selectedRole, unit);

            // Show confirmation
            JOptionPane.showMessageDialog(this,
                    "Employee '" + name + "' has been added and assigned to role '" +
                            selectedRole.getName() + "' in unit '" + unit.getName() + "'.",
                    "Employee Added",
                    JOptionPane.INFORMATION_MESSAGE);

            dialog.dispose();

            // Force refresh the entire view
            update();

            // Select the unit node to display details
            selectAndShowUnit(unit);
        });
        buttonPanel.add(createButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Display a dialog to create a new organization chart or open an existing one
     * @return true if a chart was created or opened, false if the operation was cancelled
     */
    public boolean showNewChartDialog() {
        Object[] options = {"Create New Chart", "Open Existing Chart"};
        int choice = JOptionPane.showOptionDialog(this,
                "Would you like to create a new organization chart or open an existing one?",
                "Organization Chart Manager",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,     // no custom icon
                options,  // button labels
                options[0]); // default button

        if (choice == 0) {
            // Create new chart
            String name = JOptionPane.showInputDialog(this,
                    "Enter name for the root department:",
                    "New Organization Chart",
                    JOptionPane.QUESTION_MESSAGE);

            if (name != null && !name.trim().isEmpty()) {
                // Crea un nuovo organigramma
                manager.createNewOrgChart(name.trim());

                // Seleziona automaticamente il dipartimento appena creato (root)
                OrganizationalUnit rootUnit = manager.getRootUnit();
                if (rootUnit != null) {
                    selectAndShowUnit(rootUnit);
                }

                return true;
            }
        } else if (choice == 1) {
            // Open existing chart
            JFileChooser fileChooser = new JFileChooser();

            // Add file filters
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Java Serialization Files (*.ser)", "ser"));
            fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Text Files (*.txt)", "txt"));

            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();

                // Determine appropriate storage strategy based on file extension
                if (filePath.toLowerCase().endsWith(".json")) {
                    manager.setStorageStrategy(StorageFactory.createStorageStrategy(
                            StorageFactory.StorageType.JSON));
                } else if (filePath.toLowerCase().endsWith(".csv")) {
                    manager.setStorageStrategy(StorageFactory.createStorageStrategy(
                            StorageFactory.StorageType.CSV));
                } else if (filePath.toLowerCase().endsWith(".db")) {
                    manager.setStorageStrategy(StorageFactory.createStorageStrategy(
                            StorageFactory.StorageType.DBMS));
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Unsupported file format. Please use .json, .csv, or .db extension.",
                            "Invalid File Format", JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                boolean success = manager.loadOrgChart(filePath);
                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Organization chart loaded successfully.",
                            "Load Successful", JOptionPane.INFORMATION_MESSAGE);
                    updateTree();
                    return true;
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to load organization chart.",
                            "Load Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // If the user cancelled, create a default chart
        if (manager.getRootUnit() == null) {
            manager.createNewOrgChart("Default Company");

            // Seleziona automaticamente il dipartimento di default creato
            OrganizationalUnit rootUnit = manager.getRootUnit();
            if (rootUnit != null) {
                selectAndShowUnit(rootUnit);
            }

            return true;
        }

        return false;
    }

    /**
     * Update the tree view with the current organizational structure
     */
    private void updateTree() {
        OrganizationalUnit root = manager.getRootUnit();

        // Clear the old mapping
        unitToNodeMap.clear();

        if (root == null) {
            rootNode.setUserObject("No Organization");
            rootNode.removeAllChildren();
        } else {
            rootNode.setUserObject(root);
            rootNode.removeAllChildren();
            unitToNodeMap.put(root, rootNode);

            // Recursively build the tree
            buildTreeNodes(root, rootNode);
        }

        // Update the tree
        treeModel.reload();

        // Expand the root node
        orgTree.expandPath(new TreePath(rootNode.getPath()));
    }

    /**
     * Recursively build tree nodes for the organizational structure
     */
    private void buildTreeNodes(OrganizationalUnit unit, DefaultMutableTreeNode parentNode) {
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(subUnit);
            parentNode.add(childNode);
            unitToNodeMap.put(subUnit, childNode);

            // Recursively add sub-units
            buildTreeNodes(subUnit, childNode);
        }
    }

    /**
     * Observer pattern update method called when data changes
     */
    @Override
    public void update() {
        updateTree();
    }

    /**
     * Selects a unit in the tree and displays its details
     * @param unit The organizational unit to select and display
     */
    private void selectAndShowUnit(OrganizationalUnit unit) {
        // First update the tree to ensure all nodes are current
        updateTree();

        // Get the node for this unit from our map
        DefaultMutableTreeNode node = unitToNodeMap.get(unit);

        if (node != null) {
            // Create a TreePath for this node
            TreePath path = new TreePath(node.getPath());

            // Select the node in the tree
            orgTree.setSelectionPath(path);

            // Make sure it's visible
            orgTree.scrollPathToVisible(path);

            // Show the unit details
            showUnitDetails(unit);
        }
    }

    /**
     * Carica un file di test per il debug
     */
    private void loadTestFile(String filePath) {
        System.out.println("Caricamento file di test: " + filePath);

        try {
            // Seleziona lo storage strategy appropriato in base all'estensione
            if (filePath.toLowerCase().endsWith(".json")) {
                manager.setStorageStrategy(StorageFactory.createStorageStrategy(
                        StorageFactory.StorageType.JSON));
            } else if (filePath.toLowerCase().endsWith(".csv")) {
                manager.setStorageStrategy(StorageFactory.createStorageStrategy(
                        StorageFactory.StorageType.CSV));
            } else if (filePath.toLowerCase().endsWith(".db")) {
                manager.setStorageStrategy(StorageFactory.createStorageStrategy(
                        StorageFactory.StorageType.DBMS));
            }

            // Carica il file
            boolean success = manager.loadOrgChart(filePath);

            if (success) {
                // Aggiorna l'albero
                updateTree();

                // Seleziona l'unità radice
                OrganizationalUnit rootUnit = manager.getRootUnit();
                if (rootUnit != null) {
                    selectAndShowUnit(rootUnit);
                }

                // Conta i dipendenti per verifica
                int employeeCount = countEmployees(rootUnit);

                JOptionPane.showMessageDialog(this,
                        "File di test caricato con successo: " + filePath + "\n\n" +
                                "Unità radice: " + rootUnit.getName() + "\n" +
                                "Numero totale di dipendenti: " + employeeCount,
                        "Caricamento completato", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Errore durante il caricamento del file di test: " + filePath,
                        "Errore di caricamento", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Eccezione durante il caricamento: " + ex.getMessage(),
                    "Errore", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Conta il numero totale di dipendenti in una struttura organizzativa
     */
    private int countEmployees(OrganizationalUnit unit) {
        Set<Employee> uniqueEmployees = new HashSet<>();

        // Conta dipendenti nei ruoli di questa unità
        for (Role role : unit.getRoles()) {
            uniqueEmployees.addAll(role.getEmployees());
        }

        // Conta dipendenti nelle sottounità
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            Set<Employee> subUnitEmployees = new HashSet<>();
            for (Role role : subUnit.getRoles()) {
                subUnitEmployees.addAll(role.getEmployees());
            }
            uniqueEmployees.addAll(subUnitEmployees);

            // Ricorsivamente conta nelle unità più profonde
            uniqueEmployees.addAll(countEmployeesRecursive(subUnit));
        }

        return uniqueEmployees.size();
    }

    /**
     * Funzione di supporto per contare i dipendenti ricorsivamente
     */
    private Set<Employee> countEmployeesRecursive(OrganizationalUnit unit) {
        Set<Employee> uniqueEmployees = new HashSet<>();

        // Raccogli dipendenti dai ruoli di questa unità
        for (Role role : unit.getRoles()) {
            uniqueEmployees.addAll(role.getEmployees());
        }

        // Esplora ricorsivamente le sottounità
        for (OrganizationalUnit subUnit : unit.getSubUnits()) {
            uniqueEmployees.addAll(countEmployeesRecursive(subUnit));
        }

        return uniqueEmployees;
    }
}
