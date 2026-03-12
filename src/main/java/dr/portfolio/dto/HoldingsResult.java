package dr.portfolio.dto;

import java.util.List;

public class HoldingsResult {
	
	private List<HoldingView> holdings;
	private double totalMarketValue;
	private double totalCostBasis;
	private double totalGain;
	private double totalPercentGain;
	
	// paging
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;

    public boolean isHasNext() {
        return page + 1 < totalPages;
    }

    public boolean isHasPrevious() {
        return page > 0;
    }
    
	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	public long getTotalElements() {
		return totalElements;
	}

	public void setTotalElements(long totalElements) {
		this.totalElements = totalElements;
	}

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
