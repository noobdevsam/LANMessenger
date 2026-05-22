package utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

public class NotificationUtils {
    private static TrayIcon trayIcon = null;

    public static void init() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image img = createDefaultImage();
            trayIcon = new TrayIcon(img, "LANMessenger");
            trayIcon.setImageAutoSize(true);
            PopupMenu menu = new PopupMenu();
            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(e -> {
                try {
                    tray.remove(trayIcon);
                } catch (Exception ex) {
                    // ignore
                }
                System.exit(0);
            });
            menu.add(exit);
            trayIcon.setPopupMenu(menu);
            tray.add(trayIcon);
        } catch (Exception e) {
            trayIcon = null;
        }
    }

    private static Image createDefaultImage() {
        int w = 16, h = 16;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(new Color(0, 120, 215));
            g.fillOval(0, 0, w, h);
            g.setColor(Color.WHITE);
            g.drawString("L", 4, 12);
        } finally {
            g.dispose();
        }
        return img;
    }

    public static void displayNotification(String title, String message) {
        if (trayIcon != null) {
            try {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                return;
            } catch (Exception e) {
                // fallthrough to beep
            }
        }
        Toolkit.getDefaultToolkit().beep();
    }
}
