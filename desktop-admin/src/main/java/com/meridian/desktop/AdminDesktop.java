package com.meridian.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/** Meridian Admin Console (Java Swing + FlatLaf). Uses the same REST API as the web app; sign in with an ADMIN account. */
public class AdminDesktop {
    private static final ObjectMapper M = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"));
    private static String base = "http://localhost:8080/api";
    private static String token;

    private final DefaultTableModel users = model("ID", "Name", "Email", "Mobile", "Role", "Status", "KYC", "Locked");
    private final DefaultTableModel accounts = model("ID", "Holder", "Bank", "Account no.", "IFSC", "Type", "Balance", "Status");
    private final DefaultTableModel txs = model("UTR", "Time", "Type", "Channel", "From", "To", "Amount");
    private final JLabel stats = new JLabel(" ");
    private final JTable accountTable = new JTable(accounts);

    private static JsonNode call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() >= 400) {
            String msg = "HTTP " + r.statusCode();
            try { msg = M.readTree(r.body()).path("message").asText(msg); } catch (Exception ignored) { }
            throw new RuntimeException(msg);
        }
        return r.body().isBlank() ? M.createObjectNode() : M.readTree(r.body());
    }

    private static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(AdminDesktop::login);
    }

    private static void login() {
        JTextField email = new JTextField("admin@meridian.bank");
        JPasswordField pass = new JPasswordField("Meridian@123");
        JTextField url = new JTextField(base);
        JPanel p = new JPanel(new GridLayout(0, 1, 6, 6));
        p.add(new JLabel("API URL")); p.add(url); p.add(new JLabel("Admin email")); p.add(email); p.add(new JLabel("Password")); p.add(pass);
        while (true) {
            if (JOptionPane.showConfirmDialog(null, p, "Meridian Admin Console", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) System.exit(0);
            try {
                base = url.getText().trim();
                JsonNode res = call("POST", "/auth/login", M.writeValueAsString(Map.of("identifier", email.getText(), "password", new String(pass.getPassword()))));
                if (!"ADMIN".equals(res.path("user").path("role").asText())) throw new RuntimeException("This user is not an administrator");
                token = res.path("token").asText();
                break;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Sign-in failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        new AdminDesktop().window();
    }

    private void window() {
        JFrame f = new JFrame("Meridian - Admin Console");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1200, 720);
        f.setLocationRelativeTo(null);
        stats.setFont(stats.getFont().deriveFont(Font.BOLD, 14f));
        stats.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JTable userTable = new JTable(users), txTable = new JTable(txs);
        for (JTable t : new JTable[]{userTable, accountTable, txTable}) t.setRowHeight(26);

        JButton refresh = new JButton("Refresh");
        JButton freeze = new JButton("Freeze / Unfreeze selected account");
        refresh.addActionListener(e -> reload());
        freeze.addActionListener(e -> {
            int row = accountTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(null, "Select an account first."); return; }
            long id = (Long) accounts.getValueAt(row, 0);
            String next = "ACTIVE".equals(accounts.getValueAt(row, 7)) ? "FROZEN" : "ACTIVE";
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception { call("POST", "/manager/accounts/" + id + "/status?value=" + next, null); return null; }
                @Override protected void done() { try { get(); reload(); } catch (Exception ex) { error(ex); } }
            }.execute();
        });
        JPanel accPanel = new JPanel(new BorderLayout());
        accPanel.add(new JScrollPane(accountTable), BorderLayout.CENTER);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(refresh); bar.add(freeze);
        accPanel.add(bar, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Users", new JScrollPane(userTable));
        tabs.addTab("Bank accounts", accPanel);
        tabs.addTab("Latest transactions", new JScrollPane(txTable));
        f.add(stats, BorderLayout.NORTH);
        f.add(tabs, BorderLayout.CENTER);
        f.setVisible(true);
        reload();
    }

    private void reload() {
        new SwingWorker<Void, Void>() {
            JsonNode s, u, a, t;
            @Override protected Void doInBackground() throws Exception {
                s = call("GET", "/admin/stats", null); u = call("GET", "/admin/users", null);
                a = call("GET", "/manager/accounts", null); t = call("GET", "/audit/transactions?size=50", null).path("items");
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    stats.setText(String.format("Users: %d  |  Customers: %d  |  Accounts: %d  |  Banks: %d  |  Transactions: %d  |  Total deposits: %s",
                            s.path("users").asLong(), s.path("customers").asLong(), s.path("accounts").asLong(), s.path("banks").asLong(),
                            s.path("transactions").asLong(), INR.format(s.path("totalDeposits").asDouble())));
                    users.setRowCount(0);
                    for (JsonNode n : u) users.addRow(new Object[]{n.path("id").asLong(), n.path("fullName").asText(), n.path("email").asText(), n.path("phone").asText(),
                            n.path("role").asText(), n.path("status").asText(), n.path("kycStatus").asText("-"), n.path("locked").asBoolean() ? "YES" : "no"});
                    accounts.setRowCount(0);
                    for (JsonNode n : a) accounts.addRow(new Object[]{n.path("id").asLong(), n.path("holderName").asText(), n.path("bankName").asText(), n.path("accountNumber").asText(),
                            n.path("ifsc").asText(), n.path("type").asText(), INR.format(n.path("balance").asDouble()), n.path("status").asText()});
                    txs.setRowCount(0);
                    for (JsonNode n : t) txs.addRow(new Object[]{n.path("reference").asText(), n.path("createdAt").asText().replace('T', ' '), n.path("type").asText(), n.path("channel").asText(),
                            n.path("fromName").asText("-"), n.path("toName").asText("-"), INR.format(n.path("amount").asDouble())});
                } catch (Exception e) { error(e); }
            }
        }.execute();
    }

    private static void error(Exception e) {
        Throwable t = e.getCause() != null ? e.getCause() : e;
        JOptionPane.showMessageDialog(null, t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
