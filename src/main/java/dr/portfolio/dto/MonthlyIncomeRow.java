package dr.portfolio.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyIncomeRow(
    YearMonth month,
    BigDecimal dividends,
    BigDecimal interest,
    BigDecimal total
) {}