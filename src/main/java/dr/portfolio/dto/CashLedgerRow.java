package dr.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import dr.portfolio.domain.CashTransactionType;

public record CashLedgerRow(
		UUID id,
	    LocalDateTime date,
	    CashTransactionType type,
	    String symbol,
	    String description,
	    BigDecimal amount,
	    double balance,
	    boolean editable
	) {}
