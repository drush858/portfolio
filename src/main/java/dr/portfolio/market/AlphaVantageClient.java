package dr.portfolio.market;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import dr.portfolio.dto.market.AlphaVantageResponse;

@Service
public class AlphaVantageClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Value("${alphavantage.api.url}")
    private String apiUrl;

    public Double fetchPrice(String symbol) {
    	String url = UriComponentsBuilder
           	.fromUriString(apiUrl)
            .queryParam("function", "GLOBAL_QUOTE")
            .queryParam("symbol", symbol)
            .queryParam("apikey", apiKey)
            .toUriString();

        AlphaVantageResponse response =
                restTemplate.getForObject(url, AlphaVantageResponse.class);

        if (response == null) {
        	return null;
            //throw new IllegalStateException("No price for " + symbol);
        }
        
        if (response.isRateLimited()) {
            // CRITICAL: do not throw
            return null;
        }

        double price = response.getPrice();
        return Double.isNaN(price) ? null : price;
    }
}

