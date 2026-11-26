package budgetguard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BudgetStorage {
    private static final String FILE_NAME = "budget_data.txt";

    public static class LoadedData {
        public final double unallocatedBudget;
        public final List<BudgetCategory> categories;

        public LoadedData(double unallocatedBudget, List<BudgetCategory> categories) {
            this.unallocatedBudget = unallocatedBudget;
            this.categories = categories;
        }
    }

    public static void save(double unallocatedBudget, List<BudgetCategory> categories) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            writer.write("DATE|" + LocalDate.now());
            writer.newLine();
            writer.write("POOL|" + unallocatedBudget);
            writer.newLine();

            for (BudgetCategory c : categories) {
                if (c instanceof FlexibleCategory) {
                    FlexibleCategory flex = (FlexibleCategory) c;
                    writer.write("FLEX|" + flex.getName() + "|" + flex.getBalance() + "|"
                            + flex.isDailyLimitActive() + "|" + flex.getDailyLimit() + "|"
                            + flex.getSpentToday() + "|" + flex.getConsecutiveDailyWarnings());
                } else {
                    writer.write("FIXED|" + c.getName() + "|" + c.getBalance());
                }
                writer.newLine();
            }
            System.out.println("Saved data to " + FILE_NAME);
        } catch (IOException e) {
            System.out.println("Failed to save data: " + e.getMessage());
        }
    }

    public static LoadedData load() {
        List<BudgetCategory> loaded = new ArrayList<>();
        double unallocated = 0;
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return new LoadedData(0, loaded);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|");
                String type = parts[0];

                if (type.equals("POOL") && parts.length >= 2) {
                    unallocated = Double.parseDouble(parts[1]);
                } else if (type.equals("FLEX") && parts.length >= 6) {
                    String name = parts[1];
                    double balance = Double.parseDouble(parts[2]);
                    boolean active = Boolean.parseBoolean(parts[3]);
                    double limit = Double.parseDouble(parts[4]);
                    int warnings = parts.length >= 7 ? Integer.parseInt(parts[6]) : 0;

                    loaded.add(new FlexibleCategory(name, balance, active, limit, 0, warnings));
                } else if (type.equals("FIXED") && parts.length >= 3) {
                    String name = parts[1];
                    double balance = Double.parseDouble(parts[2]);
                    loaded.add(new FixedCategory(name, balance));
                }
            }
            System.out.println("Loaded " + loaded.size() + " categories from " + FILE_NAME);
        } catch (IOException | NumberFormatException e) {
            System.out.println("Failed to load saved data: " + e.getMessage());
        }
        return new LoadedData(unallocated, loaded);
    }
}