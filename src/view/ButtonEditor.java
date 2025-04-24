package view;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Un editor di celle specializzato per i pulsanti nelle tabelle
 */
public class ButtonEditor extends DefaultCellEditor {
    private JButton button;
    private ButtonClickListener clickListener;
    private int row, column;
    private String label;
    private boolean clicked;

    /**
     * Interfaccia per gestire gli eventi di click sul pulsante
     */
    public interface ButtonClickListener {
        void buttonClicked(int row);
    }

    /**
     * Costruttore
     * @param clickListener L'ascoltatore da notificare quando viene cliccato il pulsante
     */
    public ButtonEditor(ButtonClickListener clickListener) {
        super(new JCheckBox());
        this.clickListener = clickListener;
        this.button = new JButton();
        button.setOpaque(true);

        // Listener per il pulsante che notifica il cellEditor quando viene cliccato
        button.addActionListener(e -> {
            clicked = true;
            fireEditingStopped();
        });
    }

    /**
     * Preparazione del componente di editing
     */
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        // Memorizza riga e colonna correnti
        this.row = row;
        this.column = column;

        // Imposta il testo del pulsante
        this.label = (value == null) ? "" : value.toString();
        button.setText(label);

        // Imposta i colori in base alla selezione
        if (isSelected) {
            button.setForeground(table.getSelectionForeground());
            button.setBackground(table.getSelectionBackground());
        } else {
            button.setForeground(table.getForeground());
            button.setBackground(UIManager.getColor("Button.background"));
        }

        // Inizializza lo stato per questo ciclo di editing
        clicked = false;

        return button;
    }

    /**
     * Determina se l'editing può essere interrotto
     */
    @Override
    public boolean stopCellEditing() {
        clicked = false;
        return super.stopCellEditing();
    }

    /**
     * Restituisce il valore corrente della cella
     */
    @Override
    public Object getCellEditorValue() {
        return label;
    }

    /**
     * Chiamato quando l'editing viene interrotto
     */
    @Override
    protected void fireEditingStopped() {
        super.fireEditingStopped();

        // Se il pulsante è stato effettivamente cliccato, notifica il listener
        if (clicked && clickListener != null) {
            System.out.println("Pulsante cliccato nella riga: " + row);
            clickListener.buttonClicked(row);
        }
    }
}