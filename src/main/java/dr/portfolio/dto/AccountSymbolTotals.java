package dr.portfolio.dto;

import java.time.LocalDateTime;

public class AccountSymbolTotals {

    private String accountName;
    private String symbol;
    private LocalDateTime lastSoldDate;
    private long maxDaysHeld;

    private double proceeds;
    private double costBasis;
    private double gain;
    private double percentGain;
    
    private double dividends;
    private double totalReturn;
    private double totalReturnPercent;

    public AccountSymbolTotals(String accountName, String symbol) {
        this.accountName = accountName;
        this.symbol = symbol;
    }

    public void add(double proceeds, double costBasis) {
        this.proceeds += proceeds;
        this.costBasis += costBasis;
        recalc();
    }

    public void addDividends(double dividends) {
        this.dividends += dividends;
        recalc();
    }

    private void recalc() {
        this.gain = this.proceeds - this.costBasis;
        this.totalReturn = this.gain + this.dividends;
        this.percentGain = this.costBasis == 0 ? 0 : this.gain / this.costBasis;
        this.totalReturnPercent = this.costBasis == 0 ? 0 : this.totalReturn / this.costBasis;
    }

    public long getMaxDaysHeld() {
        return maxDaysHeld;
    }

    public void updateMaxDaysHeld(long daysHeld) {
        if (daysHeld > this.maxDaysHeld) {
            this.maxDaysHeld = daysHeld;
        }
    }
    
    public void updateLastSoldDate(LocalDateTime date) {
        if (lastSoldDate == null || date.isAfter(lastSoldDate)) {
            lastSoldDate = date;
        }
    }    

	public LocalDateTime getLastSoldDate() {
		return lastSoldDate;
	}

	public void setLastSoldDate(LocalDateTime lastSoldDate) {
		this.lastSoldDate = lastSoldDate;
	}

	public double getDividends() {
		return dividends;
	}

	public double getTotalReturn() {
		return totalReturn;
	}

	public double getTotalReturnPercent() {
		return totalReturnPercent;
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
