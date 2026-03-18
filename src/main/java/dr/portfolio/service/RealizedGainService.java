package dr.portfolio.service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dr.portfolio.domain.Trade;
import dr.portfolio.domain.TradeType;
import dr.portfolio.dto.AccountSoldTotals;
import dr.portfolio.dto.AccountSymbolTotals;
import dr.portfolio.dto.BuyLot;
import dr.portfolio.dto.SoldHoldingView;
import dr.portfolio.dto.SoldHoldingsResult;
import dr.portfolio.dto.SoldLotDetail;
import dr.portfolio.repositories.CashTransactionRepository;

@Service
public class RealizedGainService {

    private final CashTransactionRepository cashTransactionRepository;

    public RealizedGainService(CashTransactionRepository cashTransactionRepository) {
        this.cashTransactionRepository = cashTransactionRepository;
    }

    private boolean isOptionSymbol(String symbol) {
        return symbol != null && symbol.matches(".*\\d{6}[CP].*");
    }

    public SoldHoldingsResult getSoldHoldingsForYear(List<Trade> trades, int year) {

        Map<UUID, SoldHoldingView> byHolding = new LinkedHashMap<>();
        Map<String, AccountSoldTotals> totalsByAccount = new LinkedHashMap<>();
        Map<String, AccountSymbolTotals> byAccountSymbol = new LinkedHashMap<>();
        List<SoldLotDetail> soldLotDetails = new ArrayList<>();

        Map<UUID, Deque<BuyLot>> fifo = new HashMap<>();

        for (Trade trade : trades) {

            if (trade.getTradeDate().getYear() > year) {
                break;
            }

            UUID holdingId = trade.getHolding().getId();
            String accountName = trade.getHolding().getAccount().getName();
            String symbol = trade.getSymbol();
            double multiplier = isOptionSymbol(symbol) ? 100.0 : 1.0;

            fifo.putIfAbsent(holdingId, new ArrayDeque<>());
            Deque<BuyLot> lots = fifo.get(holdingId);

            if (trade.getTradeType() == TradeType.BUY) {
                lots.addLast(new BuyLot(
                    trade.getQuantity(),
                    trade.getPrice(),
                    trade.getTradeDate()
                ));
                continue;
            }

            if (trade.getTradeType() == TradeType.SELL
                    && trade.getTradeDate().getYear() == year) {

                double proceeds = trade.getQuantity() * trade.getPrice() * multiplier;

                String key = accountName + "|" + symbol;
                AccountSymbolTotals symbolTotals =
                    byAccountSymbol.computeIfAbsent(
                        key,
                        k -> new AccountSymbolTotals(accountName, symbol)
                    );

                double costBasis = consumeLotsForSale(
                    lots,
                    trade,
                    accountName,
                    symbol,
                    multiplier,
                    soldLotDetails,
                    symbolTotals
                );

                SoldHoldingView holdingView =
                    byHolding.computeIfAbsent(holdingId, id -> {
                        SoldHoldingView v = new SoldHoldingView();
                        v.setHoldingId(id);
                        v.setSymbol(symbol);
                        v.setAccountName(accountName);
                        return v;
                    });

                holdingView.addSale(trade.getQuantity(), proceeds, costBasis);

                totalsByAccount
                    .computeIfAbsent(accountName, AccountSoldTotals::new)
                    .add(proceeds, costBasis);

                symbolTotals.add(proceeds, costBasis);
                symbolTotals.updateLastSoldDate(trade.getTradeDate());
            }
        }

        for (SoldHoldingView view : byHolding.values()) {
            Trade matchingTrade = trades.stream()
                .filter(t -> t.getHolding().getId().equals(view.getHoldingId()))
                .findFirst()
                .orElse(null);

            if (matchingTrade == null) {
                continue;
            }

            UUID accountId = matchingTrade.getHolding().getAccount().getId();
            String accountName = matchingTrade.getHolding().getAccount().getName();
            String symbol = view.getSymbol();

            double dividends = cashTransactionRepository
                .sumDividendsByAccountAndSymbolAndYear(accountId, symbol, year)
                .doubleValue();

            view.addDividends(dividends);

            totalsByAccount
                .computeIfAbsent(accountName, AccountSoldTotals::new)
                .addDividends(dividends);

            String key = accountName + "|" + symbol;
            byAccountSymbol
                .computeIfAbsent(key, k -> new AccountSymbolTotals(accountName, symbol))
                .addDividends(dividends);
        }

        List<AccountSymbolTotals> sortedAccountSymbolTotals = byAccountSymbol.values()
            .stream()
            .sorted(
                Comparator.comparing(
                    AccountSymbolTotals::getLastSoldDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
            )
            .toList();

        soldLotDetails.sort(
            Comparator.comparing(SoldLotDetail::buyDate).reversed()
                .thenComparing(SoldLotDetail::symbol, String.CASE_INSENSITIVE_ORDER)
        );

        return new SoldHoldingsResult(
            new ArrayList<>(byHolding.values()),
            new ArrayList<>(totalsByAccount.values()),
            sortedAccountSymbolTotals,
            soldLotDetails
        );
    }

    private double consumeLotsForSale(
            Deque<BuyLot> lots,
            Trade trade,
            String accountName,
            String symbol,
            double multiplier,
            List<SoldLotDetail> soldLotDetails,
            AccountSymbolTotals symbolTotals) {

        int sellQty = trade.getQuantity();
        double costBasis = 0.0;

        while (sellQty > 0) {
            BuyLot lot = lots.pollFirst();

            if (lot == null) {
                throw new IllegalStateException("Sell exceeds available buy quantity");
            }

            int matched = Math.min(sellQty, lot.qty());
            long daysHeld = ChronoUnit.DAYS.between(lot.buyDate(), trade.getTradeDate());

            double matchedCostBasis = matched * lot.price() * multiplier;
            double matchedProceeds = matched * trade.getPrice() * multiplier;
            double matchedGain = matchedProceeds - matchedCostBasis;

            costBasis += matchedCostBasis;
            sellQty -= matched;

            soldLotDetails.add(new SoldLotDetail(
                accountName,
                lot.buyDate(),
                trade.getTradeDate(),
                symbol,
                matched,
                lot.price(),
                trade.getPrice(),
                matchedGain,
                daysHeld
            ));

            symbolTotals.updateMaxDaysHeld(daysHeld);

            if (lot.qty() > matched) {
                lots.addFirst(new BuyLot(
                    lot.qty() - matched,
                    lot.price(),
                    lot.buyDate()
                ));
            }
        }

        return costBasis;
    }
}