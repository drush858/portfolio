package dr.portfolio.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dr.portfolio.domain.Account;
import dr.portfolio.domain.CashTransaction;
import dr.portfolio.domain.CashTransactionType;
import dr.portfolio.dto.IncomeDashboardResult;
import dr.portfolio.dto.IncomeSummary;
import dr.portfolio.dto.MonthlyIncomeRow;
import dr.portfolio.dto.SymbolIncomeRow;
import dr.portfolio.repositories.CashTransactionRepository;

@Service
public class IncomeDashboardService {

    private final AccountService accountService;
    private final CashTransactionRepository cashTransactionRepository;

    public IncomeDashboardService(
            AccountService accountService,
            CashTransactionRepository cashTransactionRepository) {
        this.accountService = accountService;
        this.cashTransactionRepository = cashTransactionRepository;
    }

    public IncomeDashboardResult buildForUser(String username, int year) {

        List<Account> accounts = accountService.findByUserUsername(username);
        List<UUID> accountIds = accounts.stream()
                .map(Account::getId)
                .toList();

        LocalDateTime ytdStart = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime ytdEnd = LocalDate.of(year, 12, 31).atTime(23, 59, 59);

        LocalDateTime trailing12Start = LocalDate.now().minusMonths(12).withDayOfMonth(1).atStartOfDay();
        LocalDateTime trailing12End = LocalDateTime.now();

        BigDecimal dividendsYtd = cashTransactionRepository.sumByAccountsTypeAndDateRange(
                accountIds, CashTransactionType.DIVIDEND, ytdStart, ytdEnd);

        BigDecimal interestYtd = cashTransactionRepository.sumByAccountsTypeAndDateRange(
                accountIds, CashTransactionType.INTEREST, ytdStart, ytdEnd);

        BigDecimal trailing12Dividends = cashTransactionRepository.sumByAccountsTypeAndDateRange(
                accountIds, CashTransactionType.DIVIDEND, trailing12Start, trailing12End);

        BigDecimal trailing12Interest = cashTransactionRepository.sumByAccountsTypeAndDateRange(
                accountIds, CashTransactionType.INTEREST, trailing12Start, trailing12End);

        IncomeSummary summary = new IncomeSummary(
                dividendsYtd,
                interestYtd,
                dividendsYtd.add(interestYtd),
                trailing12Dividends.add(trailing12Interest)
        );

        List<CashTransaction> incomeTxns = cashTransactionRepository.findIncomeTransactionsByAccountsAndDateRange(
                accountIds,
                List.of(CashTransactionType.DIVIDEND, CashTransactionType.INTEREST),
                ytdStart,
                ytdEnd
        );

        Map<YearMonth, BigDecimal> monthlyDividends = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> monthlyInterest = new LinkedHashMap<>();

        Map<String, BigDecimal> symbolDividends = new LinkedHashMap<>();
        Map<String, BigDecimal> symbolInterest = new LinkedHashMap<>();

        for (CashTransaction tx : incomeTxns) {
            YearMonth ym = YearMonth.from(tx.getTransactionDate());
            String symbol = (tx.getSymbol() == null || tx.getSymbol().isBlank()) ? "-" : tx.getSymbol();

            if (tx.getTransactionType() == CashTransactionType.DIVIDEND) {
                monthlyDividends.merge(ym, tx.getAmount(), BigDecimal::add);
                symbolDividends.merge(symbol, tx.getAmount(), BigDecimal::add);
            } else if (tx.getTransactionType() == CashTransactionType.INTEREST) {
                monthlyInterest.merge(ym, tx.getAmount(), BigDecimal::add);
                symbolInterest.merge(symbol, tx.getAmount(), BigDecimal::add);
            }
        }

        List<MonthlyIncomeRow> monthlyRows = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            BigDecimal div = monthlyDividends.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal interest = monthlyInterest.getOrDefault(ym, BigDecimal.ZERO);

            monthlyRows.add(new MonthlyIncomeRow(
                    ym,
                    div,
                    interest,
                    div.add(interest)
            ));
        }

        Map<String, BigDecimal> allSymbols = new LinkedHashMap<>();
        symbolDividends.forEach((k, v) -> allSymbols.put(k, BigDecimal.ZERO));
        symbolInterest.forEach((k, v) -> allSymbols.put(k, BigDecimal.ZERO));

        List<SymbolIncomeRow> symbolRows = allSymbols.keySet().stream()
                .map(symbol -> {
                    BigDecimal div = symbolDividends.getOrDefault(symbol, BigDecimal.ZERO);
                    BigDecimal interest = symbolInterest.getOrDefault(symbol, BigDecimal.ZERO);
                    return new SymbolIncomeRow(symbol, div, interest, div.add(interest));
                })
                .sorted(Comparator.comparing(SymbolIncomeRow::total).reversed())
                .toList();

        return new IncomeDashboardResult(summary, monthlyRows, symbolRows);
    }
}