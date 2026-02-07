package dr.portfolio.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
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

@Service
public class RealizedGainService {

	private boolean isOptionSymbol(String symbol) {
	    return symbol != null && symbol.matches(".*\\d{6}[CP].*");
	}
	
	public SoldHoldingsResult getSoldHoldingsForYear(
	        List<Trade> trades,
	        int year
	) {

	    Map<UUID, SoldHoldingView> byHolding = new LinkedHashMap<>();
	    Map<String, AccountSoldTotals> totalsByAccount = new LinkedHashMap<>();
	    Map<String, AccountSymbolTotals> byAccountSymbol = new LinkedHashMap<>();

	    // FIFO tracking per holding
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
	            lots.addLast(new BuyLot(trade.getQuantity(), trade.getPrice()));
	            continue;
	        }

	        if (trade.getTradeType() == TradeType.SELL &&
	            trade.getTradeDate().getYear() == year) {

	            int sellQty = trade.getQuantity();
	            double proceeds = sellQty * trade.getPrice() * multiplier;
	            double costBasis = 0;

	            while (sellQty > 0) {
	                BuyLot lot = lots.pollFirst();
	                if (lot == null) {
	                    throw new IllegalStateException("Sell exceeds available buy quantity");
	                }
	                int matched = Math.min(sellQty, lot.qty());
	                
	                costBasis += matched * lot.price() * multiplier;
	                sellQty -= matched;

	                if (lot.qty() > matched) {
	                    lots.addFirst(new BuyLot(lot.qty() - matched, lot.price()));
	                }
	            }
	            //String symbol = trade.getSymbol();

	            SoldHoldingView view =
	                byHolding.computeIfAbsent(holdingId, id -> {
	                    SoldHoldingView v = new SoldHoldingView();
	                    v.setHoldingId(id);
	                    v.setSymbol(trade.getSymbol());
	                    v.setAccountName(accountName);
	                    return v;
	                });

	            view.addSale(trade.getQuantity(), proceeds, costBasis);

	            totalsByAccount
	                .computeIfAbsent(accountName, AccountSoldTotals::new)
	                .add(proceeds, costBasis);
	            
	            // NEW: per-account + per-symbol totals
	            String key = accountName + "|" + symbol;

	            byAccountSymbol
	                .computeIfAbsent(
	                    key,
	                    k -> new AccountSymbolTotals(accountName, symbol)
	                )
	                .add(proceeds, costBasis);
	        }
	    }

	    return new SoldHoldingsResult(
	        new ArrayList<>(byHolding.values()),
	        new ArrayList<>(totalsByAccount.values()),
	        new ArrayList<>(byAccountSymbol.values())
	    );
	}
}
