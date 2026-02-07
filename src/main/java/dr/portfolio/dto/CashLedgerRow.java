package dr.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dr.portfolio.domain.CashTransactionType;

public record CashLedgerRow(
	    LocalDateTime date,
	    CashTransactionType type,
	    String symbol,
	    String description,
	    BigDecimal amount,
	    double balance
	) {}
