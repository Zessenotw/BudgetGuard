package budgetguard;

public class FixedCategory extends BudgetCategory {

    public FixedCategory(String name, double balance) {
        super(name, balance);
    }

    @Override
    public String processExpense(double amount) {
        deductBalance(amount);
        return "Expense logged under fixed category '" + getName() + "'.";
    }
}