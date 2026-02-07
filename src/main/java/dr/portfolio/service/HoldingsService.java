package dr.portfolio.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dr.portfolio.domain.Trade;
import dr.portfolio.dto.HoldingView;
import dr.portfolio.dto.HoldingsResult;
import dr.portfolio.dto.market.CachedPrice;
import dr.portfolio.market.MarketPriceService;
import dr.portfolio.util.OptionFormatter;

@Service
public class HoldingsService {

    public HoldingsResult calculateHoldings(
            List<Trade> trades,
            Map<String, CachedPrice> cachedPrices,
            MarketPriceService marketPriceService
    ) {

        record BuyLot(int qty, double price) {}

        Map<UUID, Deque<BuyLot>> buyQueues = new HashMap<>();
        Map<UUID, String> symbols = new HashMap<>();
        Map<UUID, Double> lastTradePrice = new HashMap<>();
        Map<UUID, java.time.LocalDateTime> lastTradeDate = new HashMap<>();

        for (Trade trade : trades) {

            UUID holdingId = trade.getHolding().getId();

            symbols.putIfAbsent(holdingId, trade.getSymbol());
            buyQueues.putIfAbsent(holdingId, new ArrayDeque<>());

            // Track last trade price (for option market price fallback)
            var td = trade.getTradeDate();
            if (td != null) {
                var prev = lastTradeDate.get(holdingId);
                if (prev == null || td.isAfter(prev)) {
                    lastTradeDate.put(holdingId, td);
                    lastTradePrice.put(holdingId, trade.getPrice());
                }
            }

            switch (trade.getTradeType()) {
                case BUY -> buyQueues.get(holdingId)
                        .addLast(new BuyLot(trade.getQuantity(), trade.getPrice()));

                case SELL, OPTION_EXPIRE -> {
                    int remaining = trade.getQuantity();

                    while (remaining > 0) {
                        BuyLot lot = buyQueues.get(holdingId).peekFirst();

                        if (lot == null) {
                            throw new IllegalStateException(
                                    "Invalid " + trade.getTradeType()
                                            + ": reduces position below zero for holding "
                                            + trade.getSymbol()
                            );
                        }

                        int matched = Math.min(remaining, lot.qty());
                        remaining -= matched;

                        buyQueues.get(holdingId).pollFirst();
                        if (lot.qty() > matched) {
                            buyQueues.get(holdingId).addFirst(
                                    new BuyLot(lot.qty() - matched, lot.price())
                            );
                        }
                    }
                }
            }
        }

        List<HoldingView> results = new ArrayList<>();

        buyQueues.forEach((holdingId, lots) -> {

            int qty = 0;
            double cost = 0.0;

            for (BuyLot lot : lots) {
                qty += lot.qty();
                cost += lot.qty() * lot.price();
            }

            if (qty == 0) return;

            double avgCost = cost / qty;

            String symbol = symbols.get(holdingId);
            boolean isOption = symbol != null && symbol.matches(".*\\d{6}[CP].*");

            // ---- Market price ----
            double marketPrice;
            if (!isOption) {
                CachedPrice cachedPrice = cachedPrices.get(symbol);
                marketPrice = (cachedPrice != null) ? cachedPrice.price() : 0.0;
            } else {
                // You probably don't have real-time option quotes.
                // Fallback: last traded price for that option.
                marketPrice = lastTradePrice.getOrDefault(holdingId, 0.0);
            }

            // ---- Multiplier ----
            double multiplier = isOption ? 100.0 : 1.0;

            double marketValue = qty * marketPrice * multiplier;
            double totalCost = qty * avgCost * multiplier;

            double percentGain =
                    totalCost == 0.0 ? 0.0 : (marketValue - totalCost) / totalCost;

            String optionSummary = isOption ? OptionFormatter.format(symbol) : null;

            results.add(new HoldingView(
                    holdingId,
                    symbol,
                    qty,
                    avgCost,
                    marketPrice,
                    marketValue,
                    percentGain,
                    totalCost,
                    isOption,
                    optionSummary
            ));
        });

        results.sort(Comparator.comparing(HoldingView::symbol, String.CASE_INSENSITIVE_ORDER));

        // ---- Totals ----
        HoldingsResult holdingsResult = new HoldingsResult();

        double totalMarketValue = results.stream().mapToDouble(HoldingView::marketValue).sum();
        double totalCostBasis = results.stream().mapToDouble(HoldingView::totalCost).sum();
        double totalGain = totalMarketValue - totalCostBasis;

        double totalGainPercent =
                totalCostBasis == 0.0 ? 0.0 : (totalGain / totalCostBasis);

        holdingsResult.setTotalMarketValue(totalMarketValue);
        holdingsResult.setTotalCostBasis(totalCostBasis);
        holdingsResult.setTotalGain(totalGain);                 // dollars
        holdingsResult.setTotalPercentGain(totalGainPercent);   // fraction (0.12 = 12%)

        holdingsResult.setHoldings(results);
        return holdingsResult;
    }
}
