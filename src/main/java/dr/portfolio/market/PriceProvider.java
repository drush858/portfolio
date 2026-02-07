package dr.portfolio.market;

public interface PriceProvider {

    String name();

    Double fetchPrice(String symbol);
}
