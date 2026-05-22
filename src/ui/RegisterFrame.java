package ui;

import database.DBConnection;
import utils.SecurityUtils;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class RegisterFrame extends JFrame {
    private final JTextField userField = new JTextField(20);
    private final JPasswordField passField = new JPasswordField(20);
    private final JButton registerBtn = new JButton("Commit Record");
    private final JButton navLoginBtn = new JButton("Back to Login");

    public RegisterFrame() {
        setTitle("Core LAN Messenger - Provision Provisioning Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(new Color(43, 43, 43));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Create New Account", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Username:") {{ setForeground(Color.LIGHT_GRAY); }}, gbc);
        gbc.gridx = 1; add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("Password:") {{ setForeground(Color.LIGHT_GRAY); }}, gbc);
        gbc.gridx = 1; add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        registerBtn.setBackground(new Color(50, 110, 180));
        registerBtn.setForeground(Color.WHITE);
        add(registerBtn, gbc);

        gbc.gridx = 1;
        navLoginBtn.setBackground(new Color(100, 100, 100));
        navLoginBtn.setForeground(Color.WHITE);
        add(navLoginBtn, gbc);

        registerBtn.addActionListener(e -> processRegistration());
        navLoginBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            this.dispose();
        });
    }

    private void processRegistration() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All input parameters are mandatory.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO users (username, password, status) VALUES (?, ?, 'OFFLINE')";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, SecurityUtils.hashPassword(password));
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Account provisioned successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                new LoginFrame().setVisible(true);
                this.dispose();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Username collision or DB failure: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}