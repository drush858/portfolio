package dr.portfolio.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(
	uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "symbol"})
)
public class Holding {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;
	
	@Column(nullable = false)
	private String symbol;
	
	@OneToMany(mappedBy = "holding")
	private List<Trade> trades = new ArrayList<>();
	
	// Cached / derived from trades
	@Column(nullable = false)
	private int quantity = 0;
	
	// Cached / derived from trades
	@Column(nullable = false)
	private double avgCost = 0;
	
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InstrumentType type;   // STOCK or OPTION

	// OPTION-only fields (nullable)
	private LocalDate expiration;

	private BigDecimal strikePrice;

	@Enumerated(EnumType.STRING)
	private OptionType optionType;
	
	public Holding() {}

	
	public InstrumentType getType() {
		return type;
	}


	public void setType(InstrumentType type) {
		this.type = type;
	}


	public LocalDate getExpiration() {
		return expiration;
	}


	public void setExpiration(LocalDate expiration) {
		this.expiration = expiration;
	}


	public BigDecimal getStrikePrice() {
		return strikePrice;
	}


	public void setStrikePrice(BigDecimal strikePrice) {
		this.strikePrice = strikePrice;
	}


	public OptionType getOptionType() {
		return optionType;
	}


	public void setOptionType(OptionType optionType) {
		this.optionType = optionType;
	}


	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public List<Trade> getTrades() {
        return trades;
    }

    public void setTrades(List<Trade> trades) {
        this.trades = trades;
    }

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public double getAvgCost() {
		return avgCost;
	}

	public void setAvgCost(double d) {
		this.avgCost = d;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public boolean equals(Object o) {
	    if (this == o) return true;
	    if (!(o instanceof Holding)) return false;
	    Holding that = (Holding) o;
	    return id != null && id.equals(that.id);
	}

	@Override
	public int hashCode() {
	    return getClass().hashCode();
	}

}
