package dr.portfolio.dto;

import java.math.BigDecimal;

public record IncomeSummary(
    BigDecimal dividendsYtd,
    BigDecimal interestYtd,
    BigDecimal totalIncomeYtd,
    BigDecimal trailing12MonthIncome
) {}