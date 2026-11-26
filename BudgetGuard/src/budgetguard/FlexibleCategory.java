package budgetguard;

public class FlexibleCategory extends BudgetCategory {
    private boolean dailyLimitActive;
    private double dailyLimit;
    private double spentToday;
    private int consecutiveDailyWarnings;

    public FlexibleCategory(String name, double balance, boolean dailyLimitActive, double dailyLimit) {
        super(name, balance);
        this.dailyLimitActive = dailyLimitActive;
        this.dailyLimit = dailyLimit;
        this.spentToday = 0;
        this.consecutiveDailyWarnings = 0;
    }

    public FlexibleCategory(String name, double balance, boolean dailyLimitActive, double dailyLimit, double spentToday) {
        this(name, balance, dailyLimitActive, dailyLimit, spentToday, 0);
    }

    public FlexibleCategory(String name, double balance, boolean dailyLimitActive, double dailyLimit,
                            double spentToday, int consecutiveDailyWarnings) {
        super(name, balance);
        this.dailyLimitActive = dailyLimitActive;
        this.dailyLimit = dailyLimit;
        this.spentToday = spentToday;
        this.consecutiveDailyWarnings = consecutiveDailyWarnings;
    }

    @Override
    public String processExpense(double amount) {
        deductBalance(amount);
        spentToday += amount;

        if (dailyLimitActive) {
            if (spentToday > dailyLimit) {
                consecutiveDailyWarnings++;
                if (consecutiveDailyWarnings >= 3) {
                    return "WARNING : Daily limit (" + dailyLimit + ") exceeded for '" + getName()
                            + "'. Spent today: " + spentToday;
                }
            } else {
                consecutiveDailyWarnings = 0;
            }
        }
        return "Expense logged under flexible category '" + getName() + "'.";
    }

    public boolean isDailyLimitActive() {
        return dailyLimitActive;
    }

    public double getDailyLimit() {
        return dailyLimit;
    }

    public double getSpentToday() {
        return spentToday;
    }

    public int getConsecutiveDailyWarnings() {
        return consecutiveDailyWarnings;
    }
}