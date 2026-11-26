package budgetguard;

public abstract class BudgetCategory {
    private String name;
    private double balance;

    public BudgetCategory(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public void addFunds(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    public boolean hasEnoughBalance(double amount) {
        return balance >= amount;
    }

    protected void deductBalance(double amount) {
        balance -= amount;
    }

    public abstract String processExpense(double amount);
}