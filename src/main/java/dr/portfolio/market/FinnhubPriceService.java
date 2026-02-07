package dr.portfolio.market;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dr.portfolio.dto.market.CachedPrice;
import dr.portfolio.dto.market.FinnhubQuote;

@Service
public class FinnhubPriceService implements MarketPriceService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;

    private final Map<String, CachedPrice> cache = new ConcurrentHashMap<>();
    
    private final Set<String> trackedSymbols = ConcurrentHashMap.newKeySet();

    public FinnhubPriceService(@Value("${finnhub.api.key}") String apiKey) 
    {
        this.apiKey = apiKey;
    }

    @Override
    public Optional<Double> getDelayedQuote(String symbol) {

        String normalized = symbol.toUpperCase();

        CachedPrice cached = cache.get(normalized);
        if (cached != null && !isExpired(cached)) {
            return Optional.of(cached.price());
        }

        FinnhubQuote quote = fetchQuote(normalized);
        if (quote == null || quote.getC() <= 0) {
            return Optional.empty();
        }

        double price = quote.getC();
        cache.put(normalized, new CachedPrice(price, Instant.now()));

        System.out.println(normalized + " " + price);
        
        return Optional.of(price);
    }

    private boolean isExpired(CachedPrice cached) {
        return cached.fetchedAt()
                     .plus(CACHE_TTL)
                     .isBefore(Instant.now());
    }

    private FinnhubQuote fetchQuote(String symbol) {
        try {
            String url = """
                https://finnhub.io/api/v1/quote?symbol=%s&token=%s
                """.formatted(symbol, apiKey);

            return restTemplate.getForObject(url, FinnhubQuote.class);

        } catch (Exception e) {
            return null; // fail soft
        }
    }
    
    public Set<String> getTrackedSymbols() {
        return trackedSymbols;
    }

    public void refresh(String symbol) {
        FinnhubQuote quote = fetchQuote(symbol);
        if (quote == null || quote.getC() <= 0) {
            return;
        }

        System.out.println(symbol + " in refresh");
        cache.put(
            symbol,
            new CachedPrice(
                quote.getC(),
                Instant.now()
            )
        );
    }

}
