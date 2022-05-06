package ee.tenman.investing.domain;

import static ee.tenman.investing.domain.Currency.EUR;
import static ee.tenman.investing.domain.Currency.GBP;
import static ee.tenman.investing.domain.Currency.GBX;
import static ee.tenman.investing.domain.Currency.USD;
import static ee.tenman.investing.domain.Exchange.FRA;
import static ee.tenman.investing.domain.Exchange.LON;

public enum StockSymbol {
    LON_IITU(GBX),
    LON_VUSA(GBP),
    LON_HEAL(USD),
    LON_IGSG(GBX),
    FRA_SPYD(EUR),
    FRA_SPYJ(EUR),
    LON_XD9U(USD),
    LON_RBOT(USD),
    FRA_EXXT(EUR),
    LON_ISPY(GBX),
    FRA_SXR8(EUR),
    FRA_XDWD(EUR),
    FRA_QDVE(EUR),
    LON_EQQQ(GBX),
    LON_IWFM(GBX),
    LON_IUMF(GBX),
    LON_RBTX(GBX);

    private Currency currency;

    StockSymbol(Currency currency) {
        this.currency = currency;
    }

    public Currency currency() {
        return currency;
    }
}
