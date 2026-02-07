package dr.portfolio.market;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dr.portfolio.repositories.HoldingRepository;

@Service
public class AlphaVantagePollingService {

	private final CompositePriceProvider priceProvider;
    private final PriceCache priceCache;
    private int currentIndex = 0;
    private final List<String> symbols = new CopyOnWriteArrayList<>();

    private final HoldingRepository holdingRepository;
    
    private static final Logger log =
            LoggerFactory.getLogger(AlphaVantagePollingService.class);
    
    public AlphaVantagePollingService(
    		CompositePriceProvider priceProvider,
            PriceCache priceCache,
            HoldingRepository holdingRepository) {
        this.priceProvider = priceProvider;
        this.priceCache = priceCache;
        this.holdingRepository = holdingRepository;
    }
    
    public void trackSymbol(String symbol) {
        if (!symbols.contains(symbol)) {
            symbols.add(symbol);
            log.info("Tracking new symbol {}", symbol);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerSymbols() {
    	symbols.clear();
    	symbols.addAll(holdingRepository.findAllDistinctSymbols());
    	log.info("Loaded {} symbols for price polling", symbols.size());

        if (!symbols.isEmpty()) {
            String symbol = symbols.get(0);
            try {
                Double price = priceProvider.fetchPrice(symbol);
                if (price != null) {
                    priceCache.update(symbol, price);
                    log.info("Initial price loaded for {}", symbol);
                } else {
                    log.warn("Price unavailable for {}", symbol);
                }
                
            } catch (Exception e) {
                log.warn("Failed initial price load for {}", symbol, e);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void refreshOneSymbol() {
    	if (symbols == null || symbols.isEmpty()) {
    		return;
    	}
	    String symbol = symbols.get(currentIndex);
	    Double price = priceProvider.fetchPrice(symbol);
	    if (price != null) {
	    	priceCache.update(symbol, price);
	    	log.info("Updated price cache: {} = {}", symbol, price);
	    } else {
	    	System.out.println("Fetched price is null for " + symbol);	
	    }

	    currentIndex = (currentIndex + 1) % symbols.size();
    }

    //@Scheduled(fixedRate = 15_000) // every 15 seconds (safe)
    public void refreshPrices() {

        for (String symbol : symbols) {
            try {
                Double price = priceProvider.fetchPrice(symbol);
                if (price != null) {
                   priceCache.update(symbol, price);
                   log.info("Updated price cache: {} = {}", symbol, price);
                }
            } catch (Exception e) {
                // log + continue (never break scheduler)
            	System.out.println(symbol + " " + e.getMessage());
            }
        }
    }
}
