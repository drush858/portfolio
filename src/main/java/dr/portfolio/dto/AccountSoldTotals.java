package dr.portfolio.dto;

public class AccountSoldTotals {

    private String accountName;
    private double totalProceeds;
    private double totalCostBasis;
    private double totalGain;

    public AccountSoldTotals(String accountName) {
        this.accountName = accountName;
    }

    public void add(double proceeds, double costBasis) {
        this.totalProceeds += proceeds;
        this.totalCostBasis += costBasis;
        this.totalGain += (proceeds - costBasis);
    }

    // getters
    public String getAccountName() { return accountName; }
    public double getTotalProceeds() { return totalProceeds; }
    public double getTotalCostBasis() { return totalCostBasis; }
    public double getTotalGain() { return totalGain; }
}
