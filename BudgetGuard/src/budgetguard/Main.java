package budgetguard;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BudgetStorage.LoadedData data = BudgetStorage.load();
        double unallocatedBudget = data.unallocatedBudget;
        List<BudgetCategory> categories = data.categories;

        if (!categories.isEmpty() || unallocatedBudget > 0) {
            System.out.print("Found saved budget data. Use saved data? (y/n): ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("y")) {
                categories = new ArrayList<>();
                unallocatedBudget = 0;
            }
        }

        if (categories.isEmpty()) {
            System.out.println("=== MONTHLY BUDGET SETUP ===");
            System.out.print("Enter your total budget for the month: ");
            double totalBudget = Double.parseDouble(scanner.nextLine().trim().replace(",", ""));
            unallocatedBudget = totalBudget;

            System.out.print("How many categories do you want to create? ");
            int count = Integer.parseInt(scanner.nextLine().trim());

            for (int i = 0; i < count; i++) {
                System.out.println("\nCategory #" + (i + 1) + " (Remaining unallocated budget: " + unallocatedBudget + ")");
                System.out.print("Name: ");
                String name = scanner.nextLine().trim();

                System.out.print("Type (1 = Fixed, 2 = Flexible): ");
                int type = Integer.parseInt(scanner.nextLine().trim());

                double allocation = 0;
                while (true) {
                    System.out.print("Allocate budget to '" + name + "' (Max " + unallocatedBudget + "): ");
                    allocation = Double.parseDouble(scanner.nextLine().trim().replace(",", ""));
                    if (allocation >= 0 && allocation <= unallocatedBudget) {
                        unallocatedBudget -= allocation;
                        break;
                    }
                    System.out.println("Invalid amount. Must be between 0 and " + unallocatedBudget + ".");
                }

                if (type == 2) {
                    System.out.print("Activate daily limit? (y/n): ");
                    boolean active = scanner.nextLine().trim().equalsIgnoreCase("y");
                    double limit = 0;
                    if (active) {
                        System.out.print("Daily limit amount: ");
                        limit = Double.parseDouble(scanner.nextLine().trim().replace(",", ""));
                    }
                    categories.add(new FlexibleCategory(name, allocation, active, limit));
                } else {
                    categories.add(new FixedCategory(name, allocation));
                }
            }
            BudgetStorage.save(unallocatedBudget, categories);
        }

        boolean keepGoing = true;

        while (keepGoing) {
            System.out.println("\n=== CURRENT BUDGET OVERVIEW ===");
            System.out.println("Unallocated Budget Pool: " + unallocatedBudget);
            for (int i = 0; i < categories.size(); i++) {
                BudgetCategory c = categories.get(i);
                String extra = "";
                if (c instanceof FlexibleCategory) {
                    FlexibleCategory flex = (FlexibleCategory) c;
                    if (flex.isDailyLimitActive()) {
                        extra = " | Spent today: " + flex.getSpentToday() + "/" + flex.getDailyLimit();
                    }
                }
                System.out.println((i + 1) + ". " + c.getName() + " | Balance: " + c.getBalance() + extra);
            }

            System.out.println("\nActions: [1] Log expense  [2] Reallocate budget  [3] Exit");
            System.out.print("Choose action: ");
            String action = scanner.nextLine().trim();

            if (action.equals("3")) {
                keepGoing = false;
                continue;
            }

            if (action.equals("2")) {
                System.out.println("Source options: [0] Unallocated Pool (" + unallocatedBudget + ")");
                for (int i = 0; i < categories.size(); i++) {
                    System.out.println("[" + (i + 1) + "] " + categories.get(i).getName() + " (" + categories.get(i).getBalance() + ")");
                }
                System.out.print("Move funds FROM: ");
                int fromIdx = Integer.parseInt(scanner.nextLine().trim());

                System.out.print("Move funds TO (category number 1-" + categories.size() + "): ");
                int toIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;

                if (toIdx < 0 || toIdx >= categories.size()) {
                    System.out.println("Invalid destination category.");
                    continue;
                }

                BudgetCategory target = categories.get(toIdx);

                if (fromIdx == 0) {
                    System.out.print("Amount to move from Unallocated Pool (Available: " + unallocatedBudget + "): ");
                    double moveAmt = Double.parseDouble(scanner.nextLine().trim().replace(",", ""));
                    if (moveAmt > 0 && moveAmt <= unallocatedBudget) {
                        unallocatedBudget -= moveAmt;
                        target.addFunds(moveAmt);
                        System.out.println("Moved " + moveAmt + " from Unallocated Pool to '" + target.getName() + "'.");
                        BudgetStorage.save(unallocatedBudget, categories);
                    } else {
                        System.out.println("Invalid or insufficient unallocated budget.");
                    }
                } else {
                    int donorIdx = fromIdx - 1;
                    if (donorIdx < 0 || donorIdx >= categories.size() || donorIdx == toIdx) {
                        System.out.println("Invalid source category.");
                        continue;
                    }
                    BudgetCategory donor = categories.get(donorIdx);
                    if (!(donor instanceof FlexibleCategory)) {
                        System.out.println("Cannot reallocate budget from fixed category '" + donor.getName() + "'. Only flexible categories can be used as a source.");
                        continue;
                    }
                    System.out.print("Amount to reallocate from '" + donor.getName() + "' (Available: " + donor.getBalance() + "): ");
                    double moveAmt = Double.parseDouble(scanner.nextLine().trim().replace(",", ""));
                    if (moveAmt > 0 && donor.hasEnoughBalance(moveAmt)) {
                        donor.deductBalance(moveAmt);
                        target.addFunds(moveAmt);
                        System.out.println("Reallocated " + moveAmt + " from '" + donor.getName() + "' to '" + target.getName() + "'.");
                        BudgetStorage.save(unallocatedBudget, categories);
                    } else {
                        System.out.println("Invalid or insufficient balance in source category.");
                    }
                }
                continue;
            }

            System.out.print("\nPick category number to log expense: ");
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx < 0 || idx >= categories.size()) {
                System.out.println("Invalid category selection.");
                continue;
            }
            BudgetCategory selected = categories.get(idx);

            System.out.print("Expense amount: ");
            double amount = Double.parseDouble(scanner.nextLine().trim().replace(",", ""));

            if (!selected.hasEnoughBalance(amount)) {
                System.out.println("ALERT: Not enough balance in '" + selected.getName() + "'!");
                System.out.print("Shift funds from another category? (y/n): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    System.out.print("Pick source category number: ");
                    int donorIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    if (donorIdx < 0 || donorIdx >= categories.size() || donorIdx == idx) {
                        System.out.println("Invalid source category. Skipping expense.");
                        continue;
                    }
                    BudgetCategory donor = categories.get(donorIdx);

                    System.out.print("Amount to transfer (Available: " + donor.getBalance() + "): ");
                    double transferAmt = Double.parseDouble(scanner.nextLine().trim().replace(",", ""));

                    if (donor.hasEnoughBalance(transferAmt)) {
                        donor.processExpense(transferAmt);
                        selected.addFunds(transferAmt);
                        System.out.println("Transferred " + transferAmt + " from '" + donor.getName() + "' to '" + selected.getName() + "'.");
                    } else {
                        System.out.println("Source category doesn't have enough funds. Skipping expense.");
                        continue;
                    }
                } else {
                    System.out.println("Expense cancelled.");
                    continue;
                }
            }

            if (selected.hasEnoughBalance(amount)) {
                String message = selected.processExpense(amount);
                System.out.println(message);
                System.out.println("New balance for '" + selected.getName() + "': " + selected.getBalance());
                BudgetStorage.save(unallocatedBudget, categories);
            } else {
                System.out.println("Still not enough balance after transfer. Expense not logged.");
            }
        }

        BudgetStorage.save(unallocatedBudget, categories);
        System.out.println("\n=== SESSION FINISHED ===");
        scanner.close();
    }
}