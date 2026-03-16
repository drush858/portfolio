package dr.portfolio.dto;

import java.time.LocalDateTime;

public record SoldLotDetail(
	    String accountName,
	    LocalDateTime buyDate,
	    LocalDateTime saleDate,
	    String symbol,
	    int quantity,
	    double avgCost,
	    double soldPrice,
	    double gain,
	    long daysHeld
	) {}