package dr.portfolio.dto;

import java.util.List;

public record SoldHoldingsResult(
        List<SoldHoldingView> soldHoldings,
        List<AccountSoldTotals> accountTotals,
        List<AccountSymbolTotals> accountSymbolTotals
) {}
