package dr.portfolio.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Service;

import dr.portfolio.domain.Holding;
import dr.portfolio.domain.Trade;
import dr.portfolio.repositories.HoldingRepository;
import dr.portfolio.repositories.TradeRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class HoldingRebuildService {

    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;

    public HoldingRebuildService(
            TradeRepository tradeRepository,
            HoldingRepository holdingRepository
    ) {
        this.tradeRepository = tradeRepository;
        this.holdingRepository = holdingRepository;
    }

    public void rebuildHolding(Holding holding) {
        List<Trade> trades =
                tradeRepository.findByHoldingOrderByTradeDateAscIdAsc(holding);

        record BuyLot(int qty, double price) {}

        Deque<BuyLot> lots = new ArrayDeque<>();

        for (Trade trade : trades) {
        	
        	if (trade.getQuantity() <= 0) {
        		throw new IllegalArgumentException(
        			"Trade quantity must be positive for holding " + holding.getSymbol()
    			);
        	}
        	 
            switch (trade.getTradeType()) {
                case BUY -> lots.addLast(new BuyLot(
                        trade.getQuantity(),
                        trade.getPrice()
                ));

                case SELL, OPTION_EXPIRE -> {
                    int remaining = trade.getQuantity();

                    while (remaining > 0) {
                    	
                        BuyLot lot = lots.peekFirst();

                        if (lot == null) {
                            throw new IllegalStateException(
                                    "Trade reduces position below zero for holding "
                                            + holding.getSymbol()
                            );
                        }

                        int matched = Math.min(remaining, lot.qty());
                        remaining -= matched;

                        lots.pollFirst();
                        if (lot.qty() > matched) {
                            lots.addFirst(new BuyLot(
                                    lot.qty() - matched,
                                    lot.price()
                            ));
                        }
                    }
                }

                default -> throw new IllegalArgumentException(
                        "Unsupported TradeType in rebuildHolding: " + trade.getTradeType()
                );
            }
        }

        int totalQty = 0;
        double totalCost = 0.0;

        for (BuyLot lot : lots) {
            totalQty += lot.qty();
            totalCost += lot.qty() * lot.price();
        }

        holding.setQuantity(totalQty);
        holding.setAvgCost(totalQty == 0 ? 0.0 : totalCost / totalQty);
        holdingRepository.save(holding);
    }
}
