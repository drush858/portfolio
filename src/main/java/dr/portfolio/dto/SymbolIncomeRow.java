package dr.portfolio.dto;

import java.math.BigDecimal;

public record SymbolIncomeRow(
    String symbol,
    BigDecimal dividends,
    BigDecimal interest,
    BigDecimal total
) {}