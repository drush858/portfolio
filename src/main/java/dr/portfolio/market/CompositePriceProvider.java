package dr.portfolio.market;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class CompositePriceProvider {

    private final List<PriceProvider> providers;

    public CompositePriceProvider(List<PriceProvider> providers) {
        this.providers = providers;
    }

    public Double fetchPrice(String symbol) {

        for (PriceProvider provider : providers) {
            try {
                Double price = provider.fetchPrice(symbol);
                if (price != null) {
                    return price;
                }
            } catch (Exception e) {
               System.err.println("PriceProvider.fetchPrice error for symbol: " + symbol + " Error: " + e.getMessage()); // never throw, just continue
            }
        }

        return null;
    }
}
