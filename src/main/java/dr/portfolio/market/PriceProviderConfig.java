package dr.portfolio.market;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class PriceProviderConfig {

    @Bean
    @Order(1)
    PriceProvider alphaVantage(AlphaVantageClient client) {
        return new AlphaVantagePriceProvider(client);
    }

    @Bean
    @Order(2)
    PriceProvider finnhub() {
        return new FinnhubPriceProvider();
    }
}

