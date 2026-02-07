package dr.portfolio.domain;

public enum CashTransactionType {
    DIVIDEND,      // stock or ETF dividend
    INTEREST,      // money market interest
    BUY,           // cash used to buy securities
    SELL,          // cash received from selling
    FEE,           // commissions, account fees
    TRANSFER_IN,   // external deposit
    TRANSFER_OUT,  // external withdrawal
    OPENING_BALANCE
}

