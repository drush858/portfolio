package dr.portfolio.market;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PriceRefreshJob {

    private final FinnhubPriceService priceService;

    public PriceRefreshJob(FinnhubPriceService priceService) {
        this.priceService = priceService;
    }

    /**
     * Refresh prices every 10 minutes
     * Runs off-request thread
     */
    @Scheduled(fixedDelayString = "${prices.refresh.delay}")
    public void refreshTrackedSymbols() {

        for (String symbol : priceService.getTrackedSymbols()) {
        	
        	System.out.println(symbol + "refreshTrackedSymbols");
            priceService.refresh(symbol);
        }
    }
}
