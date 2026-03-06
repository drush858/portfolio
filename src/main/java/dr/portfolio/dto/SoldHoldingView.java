package dr.portfolio.dto;

import java.util.UUID;

public class SoldHoldingView {

    private UUID holdingId;
    private String symbol;
    private int quantitySold;
    
    private double proceeds;
    private double costBasis;
    private double gain;
    private double percentGain;
    
    private double dividends;
    private double totalReturn;
    private double totalReturnPercent;
    
    private String accountName;
    
    public void addSale(int qty, double proceeds, double costBasis) {
        this.quantitySold += qty;
        this.proceeds += proceeds;
        this.costBasis += costBasis;
        
        this.gain = this.proceeds - this.costBasis;
        
        recalcTotals();
    }
    
    public void addDividends(double dividends) {
        this.dividends += dividends;
        recalcTotals();
    }

    private void recalcTotals() {
        this.gain = this.proceeds - this.costBasis;
        this.totalReturn = this.gain + this.dividends;
        this.percentGain = this.costBasis == 0 ? 0 : this.gain / this.costBasis;
        this.totalReturnPercent = this.costBasis == 0 ? 0 : this.totalReturn / this.costBasis;
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

	public void setPercentGain(double percentGain) {
		this.percentGain = percentGain;
	}

	public UUID getHoldingId() {
		return holdingId;
	}
	
	public void setHoldingId(UUID holdingId) {
		this.holdingId = holdingId;
	}
	
	public String getSymbol() {
		return symbol;
	}
	
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	
	public int getQuantitySold() {
		return quantitySold;
	}
	
	public void setQuantitySold(int quantitySold) {
		this.quantitySold = quantitySold;
	}
	
	public double getProceeds() {
		return proceeds;
	}
	
	public void setProceeds(double proceeds) {
		this.proceeds = proceeds;
	}
	
	public double getCostBasis() {
		return costBasis;
	}
	
	public void setCostBasis(double costBasis) {
		this.costBasis = costBasis;
	}
	
	public double getGain() {
		return gain;
	}
	
	public void setGain(double gain) {
		this.gain = gain;
	}
	
	public String getAccountName() {
		return accountName;
	}
	
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}
	
}
