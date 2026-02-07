package dr.portfolio.dto;

import java.time.LocalDate;
import java.util.UUID;

import dr.portfolio.domain.TradeType;

public class TradeCreate {

	private UUID accountId;
	private String symbol;
	private int quantity;
	private double price;
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
