package dr.portfolio.dto.market;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlphaVantageResponse {

    @JsonProperty("Global Quote")
    private GlobalQuote globalQuote;

    @JsonProperty("Note")
    private String note;

    @JsonProperty("Information")
    private String information;

    public boolean isRateLimited() {
        return note != null || information != null;
    }

    public double getPrice() {
        if (globalQuote == null) {
            return Double.NaN;
        }
        return globalQuote.getPrice();
    }

    public String getThrottleMessage() {
        return note != null ? note : information;
    }

    public static class GlobalQuote {

        @JsonProperty("05. price")
        private String price;

        public double getPrice() {
            return price == null ? Double.NaN : Double.parseDouble(price);
        }
    }
}
