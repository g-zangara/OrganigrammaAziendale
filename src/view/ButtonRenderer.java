package view;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Custom renderer for buttons in tables.
 * Used to display buttons in the Actions column of tables.
 */
public class ButtonRenderer extends JButton implements TableCellRenderer {

    /**
     * Constructor for ButtonRenderer
     */
    public ButtonRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(UIManager.getColor("Button.background"));
        }

        // Use value (expected to be a string) as the button text
        setText((value == null) ? "" : value.toString());

        return this;
    }
}