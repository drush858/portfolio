package dr.portfolio.dto;

import java.time.LocalDateTime;

public record BuyLot(
	int qty,
	double price,
	LocalDateTime buyDate
) {}
