package dr.portfolio.dto;

import dr.portfolio.domain.CashTransaction;
import dr.portfolio.domain.Trade;

public class TradeDetailView {

    private final Trade trade;
    private final CashTransaction cashTransaction;

    public TradeDetailView(Trade trade, CashTransaction cashTransaction) {
        this.trade = trade;
        this.cashTransaction = cashTransaction;
    }

    public Trade getTrade() {
        return trade;
    }

    public CashTransaction getCashTransaction() {
        return cashTransaction;
    }

    public boolean hasCashTransaction() {
        return cashTransaction != null;
    }
}