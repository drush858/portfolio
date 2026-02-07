package dr.portfolio.market;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import dr.portfolio.dto.market.CachedPrice;

@Component
public class PriceCache {

    private final Map<String, CachedPrice> cache = new ConcurrentHashMap<>();

    public void update(String symbol, double price) {
    	cache.put(symbol, new CachedPrice(price, Instant.now()));
    }

    public Map<String, CachedPrice> snapshot() {
        return Map.copyOf(cache);
    }

    public void restore(String symbol, CachedPrice price) {
        cache.put(symbol, price);
    }
    
    public Double get(String symbol) {
        return cache.get(symbol).price();
    }

    public Map<String, CachedPrice> getAll() {
        return cache;
    }
}

