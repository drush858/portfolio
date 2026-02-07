package dr.portfolio.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(
    indexes = @Index(
        name = "idx_trade_holding_date",
        columnList = "holding_id, trade_date, id"
    )
)
public class Trade {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "holding_id", nullable = false)
	private Holding holding;
	
	@Column(nullable = false, updatable = false)
	private String symbol;
	
	@Column(nullable = false, updatable = false)
	private int quantity;
	
	@Column(nullable = false)
	private double price;
	
	@Column(nullable = false, updatable = false)
	private LocalDateTime tradeDate;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TradeType tradeType;
	
	public Trade() {}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Holding getHolding() {
		return holding;
	}

	public void setHolding(Holding holding) {
		this.holding = holding;
	}
	
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double d) {
		this.price = d;
	}

	public LocalDateTime getTradeDate() {
		return tradeDate;
	}

	public void setTradeDate(LocalDateTime tradeDate) {
		this.tradeDate = tradeDate;
	}

	public TradeType getTradeType() {
		return tradeType;
	}

	public void setTradeType(TradeType tradeType) {
		this.tradeType = tradeType;
	}

	@Override
	public boolean equals(Object o) {
	    if (this == o) return true;
	    if (!(o instanceof Trade)) return false;
	    Trade trade = (Trade) o;
	    return id != null && id.equals(trade.id);
	}

	@Override
	public int hashCode() {
	    return getClass().hashCode();
	}

}
