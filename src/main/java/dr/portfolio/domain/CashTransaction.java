package dr.portfolio.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cash_transaction",
       indexes = {
           @Index(name = "idx_cash_account", columnList = "account_id"),
           @Index(name = "idx_cash_date", columnList = "transaction_date"),
           @Index(name = "idx_cash_symbol", columnList = "symbol")
       })
public class CashTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashTransactionType transactionType;

    /**
     * Positive = cash inflow
     * Negative = cash outflow
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * Optional: symbol that caused this transaction
     * e.g. AAPL dividend, SPAXX interest, MSFT buy
     */
    @Column(length = 16)
    private String symbol;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    /**
     * Optional free-form note (import source, memo, etc.)
     */
    @Column(length = 255)
    private String description;

    public CashTransaction() {
        // JPA
    }

    public CashTransaction(
            Account account,
            CashTransactionType transactionType,
            BigDecimal amount,
            String symbol,
            LocalDateTime transactionDate,
            String description) {

        this.account = account;
        this.transactionType = transactionType;
        this.amount = amount;
        this.symbol = symbol;
        this.transactionDate = transactionDate;
        this.description = description;
    }

    // ---------- Getters ----------

    public UUID getId() { return id; }

    public Account getAccount() { return account; }

    public CashTransactionType getTransactionType() { return transactionType; }

    public BigDecimal getAmount() { return amount; }

    public String getSymbol() { return symbol; }

    public LocalDateTime getTransactionDate() { return transactionDate; }

    public String getDescription() { return description; }

	public void setId(UUID id) {
		this.id = id;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public void setTransactionType(CashTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public void setTransactionDate(LocalDateTime transactionDate) {
		this.transactionDate = transactionDate;
	}

	public void setDescription(String description) {
		this.description = description;
	}
    
    
}
