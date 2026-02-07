package dr.portfolio.util;

public class OptionFormatter {

    public static String format(String symbol) {
        // AAPL240621C150 → AAPL 150C 06/21
        String underlying = symbol.replaceAll("\\d.*", "");
        String date = symbol.replaceAll(".*?(\\d{6}).*", "$1");
        String type = symbol.replaceAll(".*([CP]).*", "$1");
        String strike = symbol.replaceAll(".*[CP]", "");

        return String.format(
            "%s %s%s %s/%s",
            underlying,
            strike,
            type,
            date.substring(2,4),
            date.substring(4,6)
        );
    }
}