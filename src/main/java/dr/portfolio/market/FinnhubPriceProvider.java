package dr.portfolio.market;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import dr.portfolio.dto.market.FinnhubQuote;

@Service
public class FinnhubPriceProvider implements PriceProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${finnhub.api.key}")
    private String apiKey;

    @Override
    public String name() {
        return "Finnhub";
    }

    @Override
    public Double fetchPrice(String symbol) {

        String url = UriComponentsBuilder
            .fromUriString("https://finnhub.io/api/v1/quote")
            .queryParam("symbol", symbol)
            .queryParam("token", apiKey)
            .toUriString();

        FinnhubQuote q = restTemplate.getForObject(url, FinnhubQuote.class);

        return q != null && q.getC() > 0 ? q.getC() : null;
    }
}
