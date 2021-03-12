package ee.tenman.investing.integration.borsefrankfurt;

import static ee.tenman.investing.integration.borsefrankfurt.Currency.EUR;
import static ee.tenman.investing.integration.borsefrankfurt.Currency.GBP;
import static ee.tenman.investing.integration.borsefrankfurt.Currency.GBX;
import static ee.tenman.investing.integration.borsefrankfurt.Currency.USD;
import static ee.tenman.investing.integration.borsefrankfurt.Exchange.FRA;
import static ee.tenman.investing.integration.borsefrankfurt.Exchange.LON;

public enum StockSymbol {
    LON_IITU(LON, "IITU", GBX),
    LON_VUSA(LON, "VUSA", GBP),
    FRA_2B78(FRA, "2B78", EUR),
    FRA_IUSL(FRA, "IUSL", EUR),
    FRA_SPYD(FRA, "SPYD", EUR),
    FRA_SPYJ(FRA, "SPYJ", EUR),
    LON_XD9U(LON, "XD9U", USD),
    FRA_2B76(FRA, "2B76", EUR),
    FRA_EXXT(FRA, "EXXT", EUR),
    LON_ISPY(LON, "ISPY", GBX),
    FRA_SXR8(FRA, "SXR8", EUR),
    FRA_XDWD(FRA, "XDWD", EUR),
    FRA_XDWH(FRA, "XDWH", EUR),
    LON_EQQQ(LON, "EQQQ", GBX),
    LON_IWFM(LON, "IWFM", GBX),
    LON_IUMF(LON, "IUMF", GBX),
    LON_RBTX(LON, "RBTX", GBX);

    private String symbol;
    private Exchange exchange;
    private Currency currency;

    StockSymbol(Exchange exchange, String symbol, Currency currency) {
        this.symbol = symbol;
        this.exchange = exchange;
        this.currency = currency;
    }

    public String symbol() {
        return symbol;
    }

    public Exchange exchange() {
        return exchange;
    }

    public Currency currency() {
        return currency;
    }

    public String ticker() {
        return exchange + ":" + symbol;
    }
}
