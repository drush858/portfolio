package dr.portfolio.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import dr.portfolio.domain.CashTransactionType;

public class CashTransactionEdit {

    @NotNull
    private UUID id;

    @NotNull(message = "Date is required")
    private LocalDateTime transactionDate;

    @NotNull(message = "Type is required")
    private CashTransactionType transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be > 0")
    private BigDecimal amount;

    private String symbol;

    @Size(max = 255, message = "Description too long")
    private String description;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public LocalDateTime getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(LocalDateTime transactionDate) {
		this.transactionDate = transactionDate;
	}

	public CashTransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(CashTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}

