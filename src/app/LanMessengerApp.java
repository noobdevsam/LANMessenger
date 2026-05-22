package app;

import javax.swing.SwingUtilities;
import server.ServerMain;
import ui.LoginFrame;

public class LanMessengerApp {
    public static void main(String[] args) {
        Thread serverThread = new Thread(() -> ServerMain.main(new String[0]));
        serverThread.setDaemon(true);
        serverThread.start();

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}