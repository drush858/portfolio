package dr.portfolio.dto;

public class AccountSoldTotals {

    private String accountName;
    private double totalProceeds;
    private double totalCostBasis;
    private double totalGain;
    private double totalPercentGain;
    private double dividends;
    private double totalReturn;
    private double totalReturnPercent;

    public AccountSoldTotals(String accountName) {
        this.accountName = accountName;
    }

    public void add(double proceeds, double costBasis) {
        this.totalProceeds += proceeds;
        this.totalCostBasis += costBasis;
        recalc();
    }

    public void addDividends(double dividends) {
        this.dividends += dividends;
        recalc();
    }

    private void recalc() {
        this.totalGain = this.totalProceeds - this.totalCostBasis;
        this.totalReturn = this.totalGain + this.dividends;
        this.totalPercentGain = this.totalCostBasis == 0 ? 0 : this.totalGain / this.totalCostBasis;
        this.totalReturnPercent = this.totalCostBasis == 0 ? 0 : this.totalReturn / this.totalCostBasis;
    }
    
    // getters
    
    public String getAccountName() { return accountName; }
    public double getDividends() {
		return dividends;
	}

	public double getTotalReturn() {
		return totalReturn;
	}

	public double getTotalReturnPercent() {
		return totalReturnPercent;
	}

	public double getTotalPercentGain() { return totalPercentGain; }

	public double getTotalProceeds() { return totalProceeds; }
    public double getTotalCostBasis() { return totalCostBasis; }
    public double getTotalGain() { return totalGain; }
}
