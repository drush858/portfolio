package dr.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
	    String optionSummary,  	// "AAPL 150C 06/21"
	    double dividends,		// NEW: sum of dividend cash txns for symbol
	    double totalReturnPct, 	// NEW: (marketValue + realizedProceeds? + dividends - costBasis)/costBasis
	    double allocationPercent,

	    String underlyingSymbol,
	    String optionType,
	    LocalDate expiration,
	    BigDecimal strikePrice
	) {}
