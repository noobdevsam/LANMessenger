package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import database.DBConnection;
import utils.SecurityUtils;

public class LoginFrame extends JFrame {
    private final JTextField userField = new JTextField(20);
    private final JPasswordField passField = new JPasswordField(20);
    private final JButton loginBtn = new JButton("Authenticate");
    private final JButton navRegisterBtn = new JButton("Go to Register");

    public LoginFrame() {
        setTitle("Core LAN Messenger - Authentication Gate");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        
        getContentPane().setBackground(new Color(43, 43, 43));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("System Authentication", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(title, gbc);

        gbc.gridwidth = 1;
        setupRow(new JLabel("Username:"), userField, gbc, 1);
        setupRow(new JLabel("Password:"), passField, gbc, 2);

        gbc.gridx = 0; gbc.gridy = 3;
        styleButton(loginBtn, new Color(70, 140, 70));
        add(loginBtn, gbc);

        gbc.gridx = 1;
        styleButton(navRegisterBtn, new Color(100, 100, 100));
        add(navRegisterBtn, gbc);

        loginBtn.addActionListener(e -> processLogin());
        navRegisterBtn.addActionListener(e -> {
            new RegisterFrame().setVisible(true);
            this.dispose();
        });
    }

    private void setupRow(JLabel label, JComponent comp, GridBagConstraints gbc, int row) {
        label.setForeground(Color.LIGHT_GRAY);
        gbc.gridx = 0; gbc.gridy = row; add(label, gbc);
        gbc.gridx = 1; add(comp, gbc);
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
    }

    private void processLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fields cannot be blank.", "Validation Fail", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT password FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String hashedDbPass = rs.getString("password");
                        if (SecurityUtils.verifyPassword(password, hashedDbPass)) {
                            // mark user ONLINE
                            try (PreparedStatement ups = conn.prepareStatement("UPDATE users SET status='ONLINE' WHERE username=?")) {
                                ups.setString(1, username);
                                ups.executeUpdate();
                            }
                            new ChatFrame(username).setVisible(true);
                            this.dispose();
                        } else {
                            JOptionPane.showMessageDialog(this, "Invalid credentials.", "Auth Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "User profile non-existent.", "Auth Failed", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}