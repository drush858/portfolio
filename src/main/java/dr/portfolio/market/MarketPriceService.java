package dr.portfolio.market;

import java.util.Optional;

public interface MarketPriceService {

    Optional<Double> getDelayedQuote(String symbol);

}

