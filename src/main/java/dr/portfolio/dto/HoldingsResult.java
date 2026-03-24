package dr.portfolio.dto;

import org.springframework.data.domain.Page;

public class HoldingsResult {

    private Page<HoldingView> holdingsPage;

    private double totalMarketValue;
    private double totalCostBasis;
    private double totalGain;
    private double totalPercentGain;

    public Page<HoldingView> getHoldingsPage() {
        return holdingsPage;
    }

    public void setHoldingsPage(Page<HoldingView> holdingsPage) {
        this.holdingsPage = holdingsPage;
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