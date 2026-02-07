package dr.portfolio.dto;

import java.util.UUID;

public class TradeRequest {

    private UUID accountId;
    private String symbol;
    private Integer quantity;
    private double price;
    
	public UUID getAccountId() {
		return accountId;
	}
	public void setAccountId(UUID accountId) {
		this.accountId = accountId;
	}
	
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
}
