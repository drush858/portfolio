package dr.portfolio.dto;

import java.util.List;

public class HoldingsResult {
	
	private List<HoldingView> holdings;
	private double totalMarketValue;
	private double totalCostBasis;
	private double totalGain;
	private double totalPercentGain;
	
	public List<HoldingView> getHoldings() {
		return holdings;
	}
	public void setHoldings(List<HoldingView> holdings) {
		this.holdings = holdings;
	}
	public double getTotalMarketValue() {
		return totalMarketValue;
	}
	public void setTotalMarketValue(double totalMarketValue) {
		this.totalMarketValue = totalMarketValue;
	}
	public double getTotalCostBasis() {
		return totalCostBasis;
	}
	public void setTotalCostBasis(double totalCostBasis) {
		this.totalCostBasis = totalCostBasis;
	}
	public double getTotalGain() {
		return totalGain;
	}
	public void setTotalGain(double totalGain) {
		this.totalGain = totalGain;
	}
	public double getTotalPercentGain() {
		return totalPercentGain;
	}
	public void setTotalPercentGain(double totalPercentGain) {
		this.totalPercentGain = totalPercentGain;
	}
	
}
