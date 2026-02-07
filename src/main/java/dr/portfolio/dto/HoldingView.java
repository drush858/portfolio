package dr.portfolio.dto;

import java.util.UUID;

public record HoldingView(
	    UUID holdingId,
	    String symbol,
	    int quantity,
	    double averageCost,
	    double marketPrice,
	    double marketValue,
	    double percentGain,
	    double totalCost,
	    boolean option,
	    String optionSummary  // "AAPL 150C 06/21"
	) {}
