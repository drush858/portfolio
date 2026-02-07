package dr.portfolio.dto;

public class AccountSymbolTotals {

    private String accountName;
    private String symbol;

    private double proceeds;
    private double costBasis;
    private double gain;

    public AccountSymbolTotals(String accountName, String symbol) {
        this.accountName = accountName;
        this.symbol = symbol;
    }

    public void add(double proceeds, double costBasis) {
        this.proceeds += proceeds;
        this.costBasis += costBasis;
        this.gain = this.proceeds - this.costBasis;
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
