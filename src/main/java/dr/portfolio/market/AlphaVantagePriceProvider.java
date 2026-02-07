package dr.portfolio.market;

import org.springframework.stereotype.Service;

@Service
public class AlphaVantagePriceProvider implements PriceProvider {

    private final AlphaVantageClient client;

    public AlphaVantagePriceProvider(AlphaVantageClient client) {
        this.client = client;
    }

    @Override
    public String name() {
        return "AlphaVantage";
    }

    @Override
    public Double fetchPrice(String symbol) {
        return client.fetchPrice(symbol); // returns null on throttle
    }
}

