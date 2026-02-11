package dr.portfolio.dto;

import java.time.LocalDate;
import java.util.UUID;

import dr.portfolio.domain.TradeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TradeCreate {

	private UUID accountId;
	
	@NotBlank(message = "Symbol is required")
	private String symbol;
	
	@Min(value = 1, message = "Quantity must be at least 1")
	private int quantity;
	
	@NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price cannot be negative")
	private double price;
	
	@NotNull(message = "Date is required")
	private LocalDate date;
	
	private TradeType type;
	
	
	public TradeType getTradeType() {
		return type;
	}
	public void setTradeType(TradeType type) {
		this.type = type;
	}
	public LocalDate getDate() {
		return date;
	}
	public void setDate(LocalDate date) {
		this.date = date;
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
	public void setPrice(double price) {
		this.price = price;
	}
	public UUID getAccountId() {
		return accountId;
	}
	public void setAccountId(UUID accountId) {
		this.accountId = accountId;
	}

}
