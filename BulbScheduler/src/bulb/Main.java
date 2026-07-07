package bulb;

import bulb.gui.BulbSchedulerGUI;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BulbSchedulerGUI gui = new BulbSchedulerGUI();
            gui.setVisible(true);
        });
    }
}
