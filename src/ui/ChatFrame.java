package ui;

import client.NetworkManager;
import database.DBConnection;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.text.BadLocationException;

public class ChatFrame extends JFrame {
    private final String currentUsername;
    private String targetedReceiver = null;

    private final DefaultListModel<String> rosterModel = new DefaultListModel<>();
    private final JList<String> rosterList = new JList<>(rosterModel);
    private final JTextPane chatWindow = new JTextPane();
    private final JTextField txtPayloadInput = new JTextField();
    private final JButton btnEmit = new JButton("Send Message");
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
        leftPanel.add(new JScrollPane(rosterList), BorderLayout.CENTER);
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
        controlDeck.add(txtPayloadInput, BorderLayout.CENTER);
        controlDeck.add(btnEmit, BorderLayout.EAST);
        mainChatPanel.add(controlDeck, BorderLayout.SOUTH);

        add(mainChatPanel, BorderLayout.CENTER);

        btnEmit.addActionListener(e -> dispatchOutgoingMessage());
        txtPayloadInput.addActionListener(e -> dispatchOutgoingMessage());
        
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
        this.networkManager = new NetworkManager("127.0.0.1", 8085, this);
        new Thread(networkManager).start();
        networkManager.sendRawPayload("CMD_LOGIN:" + currentUsername);
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
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        });
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