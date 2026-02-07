package dr.portfolio.domain;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = "symbol")
)
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PriceSnapshot() {}

    public PriceSnapshot(String symbol, double price, Instant updatedAt) {
        this.symbol = symbol;
        this.price = price;
        this.updatedAt = updatedAt;
    }

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getSymbol() {
		// TODO Auto-generated method stub
		return symbol;
	}

}
