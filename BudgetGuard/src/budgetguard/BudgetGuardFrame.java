package budgetguard;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class BudgetGuardFrame extends JFrame {
    private double unallocatedBudget;
    private List<BudgetCategory> categories;

    private JLabel poolLabel;
    private JLabel statusLabel;
    private DefaultTableModel tableModel;
    private JTable categoryTable;

    public BudgetGuardFrame() {
        setTitle("BudgetGuard");
        setSize(680, 440);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadOrSetupData();
        initUI();
        refreshView();
    }

    private void loadOrSetupData() {
        BudgetStorage.LoadedData data = BudgetStorage.load();
        this.unallocatedBudget = data.unallocatedBudget;
        this.categories = data.categories;

        if (!categories.isEmpty() || unallocatedBudget > 0) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Found saved budget data. Use saved data?",
                    "Load Budget",
                    JOptionPane.YES_NO_OPTION
            );
            if (choice != JOptionPane.YES_OPTION) {
                categories = new ArrayList<>();
                unallocatedBudget = 0;
            }
        }

        if (categories.isEmpty()) {
            runInitialSetup();
        }
    }

    private void runInitialSetup() {
        String totalStr = JOptionPane.showInputDialog(this, "Enter your total budget for the month:", "Monthly Setup", JOptionPane.QUESTION_MESSAGE);
        if (totalStr == null) System.exit(0);
        unallocatedBudget = Double.parseDouble(totalStr.trim().replace(",", ""));

        String countStr = JOptionPane.showInputDialog(this, "How many categories do you want to create?", "Monthly Setup", JOptionPane.QUESTION_MESSAGE);
        if (countStr == null) System.exit(0);
        int count = Integer.parseInt(countStr.trim());

        for (int i = 0; i < count; i++) {
            String name = JOptionPane.showInputDialog(this, "Category #" + (i + 1) + " Name\n(Remaining pool: " + unallocatedBudget + "):");
            if (name == null || name.trim().isEmpty()) name = "Category " + (i + 1);

            String[] types = {"Fixed ", "Flexible "};
            int typeChoice = JOptionPane.showOptionDialog(
                    this,
                    "Select type for '" + name + "':",
                    "Category Type",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    types,
                    types[1]
            );

            double allocation = 0;
            while (true) {
                String allocStr = JOptionPane.showInputDialog(this, "Allocate budget to '" + name + "' (Max " + unallocatedBudget + "):");
                if (allocStr == null) System.exit(0);
                allocation = Double.parseDouble(allocStr.trim().replace(",", ""));
                if (allocation >= 0 && allocation <= unallocatedBudget) {
                    unallocatedBudget -= allocation;
                    break;
                }
                JOptionPane.showMessageDialog(this, "Must be between 0 and " + unallocatedBudget + ".");
            }

            if (typeChoice == 1) {
                int limitConfirm = JOptionPane.showConfirmDialog(this, "Activate daily limit for '" + name + "'?", "Daily Limit", JOptionPane.YES_NO_OPTION);
                boolean active = (limitConfirm == JOptionPane.YES_OPTION);
                double limit = 0;
                if (active) {
                    String limitStr = JOptionPane.showInputDialog(this, "Daily limit amount:");
                    limit = Double.parseDouble(limitStr.trim().replace(",", ""));
                }
                categories.add(new FlexibleCategory(name.trim(), allocation, active, limit));
            } else {
                categories.add(new FixedCategory(name.trim(), allocation));
            }
        }
        BudgetStorage.save(unallocatedBudget, categories);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Top bar: Unallocated pool
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 14, 4, 14));
        poolLabel = new JLabel();
        poolLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        topPanel.add(poolLabel, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);

        // Center: Category table
        String[] columns = {"Category", "Type", "Balance", "Daily Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        categoryTable = new JTable(tableModel);
        categoryTable.setRowHeight(26);
        categoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(categoryTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom: Buttons + status message
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 14, 12, 14));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton logExpenseBtn = new JButton("Log Expense");
        JButton reallocateBtn = new JButton("Reallocate Funds");
        JButton exitBtn = new JButton("Save & Exit");

        logExpenseBtn.addActionListener(e -> handleLogExpense());
        reallocateBtn.addActionListener(e -> handleReallocate());
        exitBtn.addActionListener(e -> {
            BudgetStorage.save(unallocatedBudget, categories);
            dispose();
            System.exit(0);
        });

        buttonPanel.add(logExpenseBtn);
        buttonPanel.add(reallocateBtn);
        buttonPanel.add(exitBtn);

        statusLabel = new JLabel("Ready.");
        statusLabel.setForeground(new Color(60, 60, 60));

        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void refreshView() {
        poolLabel.setText("Unallocated Budget Pool: " + unallocatedBudget);
        tableModel.setRowCount(0);

        for (BudgetCategory c : categories) {
            String type = (c instanceof FlexibleCategory) ? "Flexible" : "Fixed";
            String dailyStatus = "-";
            if (c instanceof FlexibleCategory) {
                FlexibleCategory flex = (FlexibleCategory) c;
                if (flex.isDailyLimitActive()) {
                    dailyStatus = "Spent today: " + flex.getSpentToday() + " / " + flex.getDailyLimit();
                }
            }
            tableModel.addRow(new Object[]{c.getName(), type, c.getBalance(), dailyStatus});
        }
    }

    private void handleLogExpense() {
        int selectedRow = categoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Click a category in the table first.");
            return;
        }

        BudgetCategory selected = categories.get(selectedRow);
        String amountStr = JOptionPane.showInputDialog(this, "Expense amount for '" + selected.getName() + "':");
        if (amountStr == null || amountStr.trim().isEmpty()) return;

        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
            return;
        }

        if (!selected.hasEnoughBalance(amount)) {
            int shift = JOptionPane.showConfirmDialog(
                    this,
                    "Not enough balance in '" + selected.getName() + "'!\nShift funds from another category?",
                    "Insufficient Balance",
                    JOptionPane.YES_NO_OPTION
            );
            if (shift != JOptionPane.YES_OPTION) return;

            String[] names = new String[categories.size()];
            for (int i = 0; i < categories.size(); i++) {
                names[i] = categories.get(i).getName() + " (" + categories.get(i).getBalance() + ")";
            }
            String chosen = (String) JOptionPane.showInputDialog(
                    this, "Pick source category:", "Shift Funds",
                    JOptionPane.QUESTION_MESSAGE, null, names, names[0]
            );
            if (chosen == null) return;

            int donorIdx = java.util.Arrays.asList(names).indexOf(chosen);
            BudgetCategory donor = categories.get(donorIdx);
            if (donor == selected) {
                JOptionPane.showMessageDialog(this, "Can't transfer from the same category.");
                return;
            }
            if (!(donor instanceof FlexibleCategory)) {
                JOptionPane.showMessageDialog(this, "Cannot shift funds from fixed category '" + donor.getName() + "'.");
                return;
            }

            String transferStr = JOptionPane.showInputDialog(this, "Amount to transfer (Available: " + donor.getBalance() + "):");
            if (transferStr == null) return;
            double transferAmt = Double.parseDouble(transferStr.trim().replace(",", ""));

            if (donor.hasEnoughBalance(transferAmt)) {
                donor.deductBalance(transferAmt);
                selected.addFunds(transferAmt);
            } else {
                JOptionPane.showMessageDialog(this, "Source category doesn't have enough funds.");
                return;
            }
        }

        if (selected.hasEnoughBalance(amount)) {
            String message = selected.processExpense(amount);
            BudgetStorage.save(unallocatedBudget, categories);
            refreshView();
            statusLabel.setText(message);

            if (message.startsWith("WARNING")) {
                statusLabel.setForeground(Color.RED);
                JOptionPane.showMessageDialog(this, message, "Daily Limit Alert", JOptionPane.WARNING_MESSAGE);
            } else {
                statusLabel.setForeground(new Color(0, 128, 0));
            }
        }
    }

    private void handleReallocate() {
        List<String> sources = new ArrayList<>();
        sources.add("Unallocated Pool (" + unallocatedBudget + ")");
        for (BudgetCategory c : categories) {
            sources.add(c.getName() + " (" + c.getBalance() + ")");
        }

        String fromChoice = (String) JOptionPane.showInputDialog(
                this, "Move funds FROM:", "Reallocate Budget",
                JOptionPane.QUESTION_MESSAGE, null, sources.toArray(), sources.get(0)
        );
        if (fromChoice == null) return;
        int fromIdx = sources.indexOf(fromChoice);

        String[] targets = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            targets[i] = categories.get(i).getName() + " (" + categories.get(i).getBalance() + ")";
        }
        String toChoice = (String) JOptionPane.showInputDialog(
                this, "Move funds TO:", "Reallocate Budget",
                JOptionPane.QUESTION_MESSAGE, null, targets, targets[0]
        );
        if (toChoice == null) return;
        int toIdx = java.util.Arrays.asList(targets).indexOf(toChoice);
        BudgetCategory target = categories.get(toIdx);

        if (fromIdx == 0) {
            String amtStr = JOptionPane.showInputDialog(this, "Amount from Unallocated Pool (Available: " + unallocatedBudget + "):");
            if (amtStr == null) return;
            double moveAmt = Double.parseDouble(amtStr.trim().replace(",", ""));
            if (moveAmt > 0 && moveAmt <= unallocatedBudget) {
                unallocatedBudget -= moveAmt;
                target.addFunds(moveAmt);
                statusLabel.setForeground(new Color(0, 128, 0));
                statusLabel.setText("Moved " + moveAmt + " from Unallocated Pool to '" + target.getName() + "'.");
                BudgetStorage.save(unallocatedBudget, categories);
                refreshView();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid or insufficient unallocated budget.");
            }
        } else {
            int donorIdx = fromIdx - 1;
            if (donorIdx == toIdx) {
                JOptionPane.showMessageDialog(this, "Source and destination must be different.");
                return;
            }
            BudgetCategory donor = categories.get(donorIdx);
            if (!(donor instanceof FlexibleCategory)) {
                JOptionPane.showMessageDialog(this, "Cannot reallocate from fixed category '" + donor.getName() + "'. Only flexible categories can be used as a source.");
                return;
            }
            String amtStr = JOptionPane.showInputDialog(this, "Amount from '" + donor.getName() + "' (Available: " + donor.getBalance() + "):");
            if (amtStr == null) return;
            double moveAmt = Double.parseDouble(amtStr.trim().replace(",", ""));
            if (moveAmt > 0 && donor.hasEnoughBalance(moveAmt)) {
                donor.deductBalance(moveAmt);
                target.addFunds(moveAmt);
                statusLabel.setForeground(new Color(0, 128, 0));
                statusLabel.setText("Reallocated " + moveAmt + " from '" + donor.getName() + "' to '" + target.getName() + "'.");
                BudgetStorage.save(unallocatedBudget, categories);
                refreshView();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid or insufficient balance.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new BudgetGuardFrame().setVisible(true);
        });
    }
}