package dr.portfolio.domain;

public enum CashTransactionType {
    
	
    DEPOSIT("Deposit", 10, true),         // cash deposit
    WITHDRAWAL("Withdrawal", 20, true),   // cash withdrawal
    DIVIDEND("Dividend", 30, true),       // stock or ETF dividend
    
    SWEEP_TO_FUTURES("Sweep to Futures", 40, true),      // cash moved to futures account
    SWEEP_FROM_FUTURES("Sweep from Futures", 50, true),  // cash moved from futures account
    
    INTEREST("Interest", 60, true),                        // money market interest
    INCOME("Income", 70, true),                            // other income
    FEDERAL_WITHHOLDING("Federal Withholding", 80, true),  // tax withholding
    
    FEE("Fee", 90, true),                        // commissions, account fees
    ADJUSTMENT("Adjustment", 100, true),         // manual adjustment
    OPTION_EXPIRE("Option Expire", 110, true),   // cash from option expiration;
    
    TRANSFER_IN("Transfer In", 120, true),    // external deposit
    TRANSFER_OUT("Transfer Out", 130, true),  // external withdrawal
    
    BUY("Buy", 1000, false),                          // cash used to buy securities
    SELL("Sell", 1010, false),                        // cash received from selling
    OPENING_BALANCE("Opening Balance", 1020, true);  // initial account balance
    
    
	private final String displayName;
	private final int sortOrder;
	private final boolean manualEntry;

	CashTransactionType(String displayName, int sortOrder, boolean manualEntry) {
        this.displayName = displayName;
        this.sortOrder = sortOrder;
        this.manualEntry = manualEntry;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public int getSortOrder() {
        return sortOrder;
    }
    
    public boolean isManualEntry() {
		return manualEntry;
	}
    
}

