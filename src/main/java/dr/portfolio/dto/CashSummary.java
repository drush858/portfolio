package dr.portfolio.dto;

public record CashSummary(
	    double startingBalance,
	    double totalIn,
	    double totalOut,
	    double currentBalance
	) {}
