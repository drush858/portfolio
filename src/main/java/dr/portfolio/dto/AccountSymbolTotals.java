package dr.portfolio.dto;

public class AccountSymbolTotals {

    private String accountName;
    private String symbol;

    private double proceeds;
    private double costBasis;
    private double gain;
    private double percentGain;

    public AccountSymbolTotals(String accountName, String symbol) {
        this.accountName = accountName;
        this.symbol = symbol;
    }

    public void add(double proceeds, double costBasis) {
        this.proceeds += proceeds;
        this.costBasis += costBasis;
        this.gain = this.proceeds - this.costBasis;
        this.percentGain =
                (this.costBasis == 0.0) ? 0.0 : (this.gain / this.costBasis);
    }

    
    public double getPercentGain() {
		return percentGain;
	}

	public String getAccountName() {
        return accountName;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getProceeds() {
        return proceeds;
    }

    public double getCostBasis() {
        return costBasis;
    }

    public double getGain() {
        return gain;
    }
}
