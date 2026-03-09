package dr.portfolio.dto;

import java.util.List;

public record IncomeDashboardResult(
    IncomeSummary summary,
    List<MonthlyIncomeRow> monthlyRows,
    List<SymbolIncomeRow> symbolRows
) {}
