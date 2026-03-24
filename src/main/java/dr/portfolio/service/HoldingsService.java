package dr.portfolio.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dr.portfolio.domain.Trade;
import dr.portfolio.dto.HoldingView;
import dr.portfolio.dto.HoldingsResult;
import dr.portfolio.dto.market.CachedPrice;
import dr.portfolio.market.MarketPriceService;
import dr.portfolio.repositories.CashTransactionRepository;
import dr.portfolio.util.OptionFormatter;

@Service
public class HoldingsService {

	private final CashTransactionRepository cashTransactionRepository;
	
	public HoldingsService(
			
            CashTransactionRepository cashTransactionRepository) 
    {
        this.cashTransactionRepository = cashTransactionRepository;
    }

	public HoldingsResult calculateHoldings(
			
			List<Trade> trades, 
			Map<String, CachedPrice> cachedPrices,
			MarketPriceService marketPriceService,
			UUID accountId,
		    int page,
		    int size) 
	{
		record BuyLot(int qty, double price) {}

		record RawHolding(
			UUID holdingId,
			String symbol,
			int quantity,
			double averageCost,
			double marketPrice,
			double marketValue,
			double percentGain,
			double totalCost,
			boolean option,
			String optionSummary,
			double dividends,
			double totalReturnPct,
		    String underlyingSymbol,
		    String optionType,
		    LocalDate expiration,
		    BigDecimal strikePrice
		) {}
		 
		Map<UUID, Deque<BuyLot>> buyQueues = new HashMap<>();
		Map<UUID, String> symbols = new HashMap<>();
		Map<UUID, Double> lastTradePrice = new HashMap<>();
		Map<UUID, java.time.LocalDateTime> lastTradeDate = new HashMap<>();
		Map<UUID, String> underlyingSymbols = new HashMap<>();
		Map<UUID, String> optionTypes = new HashMap<>();
		Map<UUID, LocalDate> expirations = new HashMap<>();
		Map<UUID, BigDecimal> strikePrices = new HashMap<>();
		
		for (Trade trade : trades) {

			UUID holdingId = trade.getHolding().getId();
			var holding = trade.getHolding();

			if (holding.getType() != null && holding.getType().name().equals("OPTION")) {
			    underlyingSymbols.putIfAbsent(holdingId, extractUnderlyingFromOptionSymbol(trade.getSymbol()));
			    optionTypes.putIfAbsent(holdingId,
			        holding.getOptionType() != null ? holding.getOptionType().name() : null);
			    expirations.putIfAbsent(holdingId, holding.getExpiration());
			    strikePrices.putIfAbsent(holdingId, holding.getStrikePrice());
			}
			
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
			
				case BUY -> buyQueues.get(holdingId).addLast(new BuyLot(trade.getQuantity(), trade.getPrice()));
	
				case SELL, OPTION_EXPIRE -> {
					int remaining = trade.getQuantity();
	
					while (remaining > 0) {
						BuyLot lot = buyQueues.get(holdingId).peekFirst();
	
						if (lot == null) {
							throw new IllegalStateException("Invalid " + trade.getTradeType()
									+ ": reduces position below zero for holding " + trade.getSymbol());
						}
	
						int matched = Math.min(remaining, lot.qty());
						remaining -= matched;
	
						buyQueues.get(holdingId).pollFirst();
						if (lot.qty() > matched) {
							buyQueues.get(holdingId).addFirst(new BuyLot(lot.qty() - matched, lot.price()));
						}
					}
				}
			}
		}

		List<RawHolding> rawResults = new ArrayList<>();

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
			double percentGain = totalCost == 0.0 ? 0.0 : (marketValue - totalCost) / totalCost;
			
			double dividends = 0.0;
			if (!isOption) {
			    dividends = cashTransactionRepository
			        .sumDividendsByAccountAndSymbol(accountId, symbol)
			        .doubleValue();
			}

			double totalReturnPct =
			    (totalCost == 0) ? 0.0 : ((marketValue + dividends - totalCost) / totalCost);

			String optionSummary = isOption ? OptionFormatter.format(symbol) : null;

			rawResults.add(new RawHolding(
					holdingId, 
					symbol, 
					qty, 
					avgCost, 
					marketPrice, 
					marketValue, 
					percentGain,
					totalCost, 
					isOption, 
					optionSummary,
					dividends,
					totalReturnPct,
				    underlyingSymbols.get(holdingId),
				    optionTypes.get(holdingId),
				    expirations.get(holdingId),
				    strikePrices.get(holdingId)
			));
		});

		rawResults.sort(Comparator.comparing(RawHolding::symbol, String.CASE_INSENSITIVE_ORDER));

		
		double totalMarketValue = rawResults.stream().mapToDouble(RawHolding::marketValue).sum();
		
		List<HoldingView> results = rawResults.stream()
			.map(r -> {
                double allocationPercent =
                        totalMarketValue == 0.0 ? 0.0 : (r.marketValue() / totalMarketValue) * 100.0;

                return new HoldingView(
                    r.holdingId(),
                    r.symbol(),
                    r.quantity(),
                    r.averageCost(),
                    r.marketPrice(),
                    r.marketValue(),
                    r.percentGain(),
                    r.totalCost(),
                    r.option(),
                    r.optionSummary(),
                    r.dividends(),
                    r.totalReturnPct(),
                    allocationPercent,
                    r.underlyingSymbol(),
                    r.optionType(),
                    r.expiration(),
                    r.strikePrice()
                );
            })
            .toList();
		
		// ---- Totals ----
		double totalCostBasis = results.stream().mapToDouble(HoldingView::totalCost).sum();
		double totalGain = totalMarketValue - totalCostBasis;
		double totalGainPercent = totalCostBasis == 0.0 ? 0.0 : (totalGain / totalCostBasis);

		Pageable pageable = PageRequest.of(page, size);

	    int start = (int) pageable.getOffset();
	    int end = Math.min(start + pageable.getPageSize(), results.size());

	    List<HoldingView> pageContent =
	            start >= results.size() ? List.of() : results.subList(start, end);

	    Page<HoldingView> holdingsPage =
	            new PageImpl<>(pageContent, pageable, results.size());

	    HoldingsResult holdingsResult = new HoldingsResult();
	    holdingsResult.setHoldingsPage(holdingsPage);
	    holdingsResult.setTotalMarketValue(totalMarketValue);
	    holdingsResult.setTotalCostBasis(totalCostBasis);
	    holdingsResult.setTotalGain(totalGain);
	    holdingsResult.setTotalPercentGain(totalGainPercent);

				
		return holdingsResult;
	}
	
	private String extractUnderlyingFromOptionSymbol(String symbol) {
	    if (symbol == null) return null;
	    return symbol.replaceFirst("\\d{6}[CP].*$", "");
	}
}
