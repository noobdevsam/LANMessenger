package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import client.NetworkManager;
import database.DBConnection;
import utils.NotificationUtils;

public class ChatFrame extends JFrame {
    private final String currentUsername;
    private String targetedReceiver = null;

    private final DefaultListModel<String> rosterModel = new DefaultListModel<>();
    private final JList<String> rosterList = new JList<>(rosterModel);
    private final JButton btnRefresh = new JButton("Refresh");
    private final JTextPane chatWindow = new JTextPane();
    private final JTextField txtPayloadInput = new JTextField();
    private final JButton btnEmit = new JButton("Send Message");
    private final JButton btnEmoji = new JButton("😀");
    private final JButton btnFile = new JButton("📎");
    private final JTextField txtSearch = new JTextField(12);
    private final JButton btnSearch = new JButton("Search");
    private final JButton btnDeleteChat = new JButton("Delete Chat");
    private final JLabel lblTargetStatus = new JLabel("Target: Global Sandbox");

    private NetworkManager networkManager;

    public ChatFrame(String username) {
        this.currentUsername = username;
        setTitle("Messenger Workspace [" + username + "]");
        setSize(750, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Live Discovery Node"));
        rosterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rosterList.setBackground(new Color(50, 50, 50));
        rosterList.setForeground(Color.GREEN);
        rosterList.setCellRenderer(new RosterCellRenderer());
        leftPanel.add(new JScrollPane(rosterList), BorderLayout.CENTER);
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        refreshPanel.add(btnRefresh);
        leftPanel.add(refreshPanel, BorderLayout.SOUTH);
        leftPanel.setPreferredSize(new Dimension(200, 0));
        add(leftPanel, BorderLayout.WEST);

        JPanel mainChatPanel = new JPanel(new BorderLayout());
        chatWindow.setEditable(false);
        chatWindow.setBackground(new Color(30, 30, 30));
        chatWindow.setForeground(Color.WHITE);
        chatWindow.setFont(new Font("Consolas", Font.PLAIN, 13));
        
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(lblTargetStatus);
        mainChatPanel.add(topBar, BorderLayout.NORTH);
        mainChatPanel.add(new JScrollPane(chatWindow), BorderLayout.CENTER);

        JPanel controlDeck = new JPanel(new BorderLayout());
        JPanel inputRow = new JPanel(new BorderLayout());
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftControls.add(btnEmoji);
        leftControls.add(btnFile);
        inputRow.add(leftControls, BorderLayout.WEST);
        inputRow.add(txtPayloadInput, BorderLayout.CENTER);
        inputRow.add(btnEmit, BorderLayout.EAST);
        controlDeck.add(inputRow, BorderLayout.CENTER);
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchRow.add(txtSearch);
        searchRow.add(btnSearch);
        searchRow.add(btnDeleteChat);
        mainChatPanel.add(searchRow, BorderLayout.SOUTH);
        mainChatPanel.add(controlDeck, BorderLayout.SOUTH);

        add(mainChatPanel, BorderLayout.CENTER);

        btnEmit.addActionListener(e -> dispatchOutgoingMessage());
        txtPayloadInput.addActionListener(e -> dispatchOutgoingMessage());
        btnEmoji.addActionListener(e -> showEmojiPicker());
        btnFile.addActionListener(e -> sendFileToTarget());
        btnSearch.addActionListener(e -> performSearch());
        btnDeleteChat.addActionListener(e -> deleteCurrentChat());
        btnRefresh.addActionListener(e -> requestUserList());
        
        rosterList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = rosterList.getSelectedValue();
                if (selected != null && !selected.equals(currentUsername)) {
                    this.targetedReceiver = selected;
                    lblTargetStatus.setText("Target peer link verified: " + targetedReceiver);
                    loadHistoricalChatMatrix();
                }
            }
        });

        establishNetworkPipe();
    }

    private void establishNetworkPipe() {
        String host = System.getProperty("lanm.server.host",
                System.getenv().getOrDefault("LANM_SERVER_HOST", "127.0.0.1"));
        int port = Integer.parseInt(System.getProperty("lanm.server.port",
                System.getenv().getOrDefault("LANM_SERVER_PORT", "8085")));
        this.networkManager = new NetworkManager(host, port, this);
        new Thread(networkManager).start();
        networkManager.sendRawPayload("CMD_LOGIN:" + currentUsername);
    }

    private void requestUserList() {
        if (networkManager != null) {
            networkManager.sendRawPayload("CMD_USER_LIST_REQUEST");
        } else {
            JOptionPane.showMessageDialog(this, "Not connected to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void dispatchOutgoingMessage() {
        String msg = txtPayloadInput.getText().trim();
        if (!msg.isEmpty() && targetedReceiver != null) {
            String structuralPayload = "CMD_MSG:" + currentUsername + ":" + targetedReceiver + ":" + msg;
            networkManager.sendRawPayload(structuralPayload);
            commitMessageToStore(currentUsername, targetedReceiver, msg);
            txtPayloadInput.setText("");
        }
    }

    public void updateRosterView(String[] dynamicUsernames) {
        SwingUtilities.invokeLater(() -> {
            rosterModel.clear();
            for (String user : dynamicUsernames) {
                rosterModel.addElement(user);
            }
        });
    }

    public void appendStreamMessage(String sender, String msg) {
        SwingUtilities.invokeLater(() -> {
            String formatLine = String.format("[%s]: %s\n", sender, msg);
            try {
                int length = chatWindow.getDocument().getLength();
                chatWindow.getDocument().insertString(length, formatLine, null);
                chatWindow.setCaretPosition(chatWindow.getDocument().getLength());
                Toolkit.getDefaultToolkit().beep();
                if (!ChatFrame.this.isActive()) {
                    NotificationUtils.displayNotification("Message from " + sender, msg.length() > 120 ? msg.substring(0, 120) + "..." : msg);
                }
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        });
    }

    public void receiveFile(String sender, String filename, String base64) {
        SwingUtilities.invokeLater(() -> {
            String display = String.format("[%s]: Sent a file -> %s\n", sender, filename);
            try {
                int length = chatWindow.getDocument().getLength();
                chatWindow.getDocument().insertString(length, display, null);
                chatWindow.setCaretPosition(chatWindow.getDocument().getLength());
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
            Toolkit.getDefaultToolkit().beep();
            if (!ChatFrame.this.isActive()) {
                NotificationUtils.displayNotification("File from " + sender, filename);
            }
            int rc = JOptionPane.showConfirmDialog(this, "Save received file '" + filename + "'?", "File Received", JOptionPane.YES_NO_OPTION);
            if (rc == JOptionPane.YES_OPTION) {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(filename));
                int sel = chooser.showSaveDialog(this);
                if (sel == JFileChooser.APPROVE_OPTION) {
                    File out = chooser.getSelectedFile();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] data = Base64.getDecoder().decode(base64);
                        fos.write(data);
                        JOptionPane.showMessageDialog(this, "File saved.", "OK", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }

    private void showEmojiPicker() {
        JPopupMenu menu = new JPopupMenu();
        String[] emojis = {"😀","😂","😅","😍","🤔","👍","🙏","🎉"};
        for (String e : emojis) {
            JMenuItem item = new JMenuItem(e);
            item.addActionListener(ev -> txtPayloadInput.setText(txtPayloadInput.getText() + e));
            menu.add(item);
        }
        menu.show(this, btnEmoji.getX(), btnEmoji.getY() + btnEmoji.getHeight());
    }

    private void sendFileToTarget() {
        if (targetedReceiver == null) {
            JOptionPane.showMessageDialog(this, "Select a target user first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int rc = chooser.showOpenDialog(this);
        if (rc == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(f);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = fis.read(buf)) != -1) baos.write(buf,0,r);
                String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                String payload = "CMD_FILE:" + currentUsername + ":" + targetedReceiver + ":" + f.getName() + ":" + b64;
                networkManager.sendRawPayload(payload);
                commitFileToStore(currentUsername, targetedReceiver, f.getName(), b64);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to send file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void commitFileToStore(String sender, String receiver, String filename, String b64) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO messages (sender, receiver, message, file_name, file_blob) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sender);
                ps.setString(2, receiver);
                ps.setString(3, "[FILE] " + filename);
                ps.setString(4, filename);
                ps.setString(5, b64);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            System.err.println("Failed SQL File commit: " + ex.getMessage());
        }
    }

    private void performSearch() {
        if (targetedReceiver == null) {
            JOptionPane.showMessageDialog(this, "Select a target user first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String term = txtSearch.getText().trim();
        if (term.isEmpty()) {
            loadHistoricalChatMatrix();
            return;
        }
        chatWindow.setText("");
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT sender, message FROM messages WHERE ((sender=? AND receiver=?) OR (sender=? AND receiver=?)) AND (message LIKE ?) ORDER BY created_at ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setString(2, targetedReceiver);
                ps.setString(3, targetedReceiver);
                ps.setString(4, currentUsername);
                ps.setString(5, "%" + term + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        appendStreamMessage(rs.getString("sender"), rs.getString("message"));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void deleteCurrentChat() {
        if (targetedReceiver == null) {
            JOptionPane.showMessageDialog(this, "Select a target user first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int rc = JOptionPane.showConfirmDialog(this, "Permanently delete chat history with " + targetedReceiver + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (rc == JOptionPane.YES_OPTION) {
            try (Connection conn = DBConnection.getConnection()) {
                String sql = "DELETE FROM messages WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, currentUsername);
                    ps.setString(2, targetedReceiver);
                    ps.setString(3, targetedReceiver);
                    ps.setString(4, currentUsername);
                    ps.executeUpdate();
                }
                loadHistoricalChatMatrix();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private ImageIcon loadUserProfileIcon(String username) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT profile_pic FROM users WHERE username=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String b64 = rs.getString("profile_pic");
                        if (b64 != null) {
                            byte[] data = Base64.getDecoder().decode(b64);
                            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                            Image scaled = img.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                            return new ImageIcon(scaled);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    private class RosterCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(isSelected ? Color.DARK_GRAY : Color.GRAY);
            String username = value.toString();
            JLabel lbl = new JLabel(username);
            lbl.setForeground(Color.GREEN);
            ImageIcon icon = loadUserProfileIcon(username);
            if (icon != null) lbl.setIcon(icon);
            p.add(lbl, BorderLayout.CENTER);
            return p;
        }
    }

    private void commitMessageToStore(String sender, String receiver, String body) {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO messages (sender, receiver, message) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sender);
                ps.setString(2, receiver);
                ps.setString(3, body);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            System.err.println("Failed SQL Message commit log: " + ex.getMessage());
        }
    }

    private void loadHistoricalChatMatrix() {
        chatWindow.setText(""); 
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT sender, message FROM messages WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?) ORDER BY created_at ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, currentUsername);
                ps.setString(2, targetedReceiver);
                ps.setString(3, targetedReceiver);
                ps.setString(4, currentUsername);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        appendStreamMessage(rs.getString("sender"), rs.getString("message"));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}