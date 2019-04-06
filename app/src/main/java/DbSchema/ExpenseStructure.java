package DbSchema;

public class ExpenseStructure {
    String category, paidBy, members;
    float amount;

    public ExpenseStructure() {
    }

    public ExpenseStructure(String category, String paidBy, String members, float amount) {
        this.category = category;
        this.paidBy = paidBy;
        this.members = members;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public String getPaidBy() {
        return paidBy;
    }

    public String getMembers() {
        return members;
    }

    public float getAmount() {
        return amount;
    }
}

