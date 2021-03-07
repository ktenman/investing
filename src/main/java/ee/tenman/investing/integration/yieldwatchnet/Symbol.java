package ee.tenman.investing.integration.yieldwatchnet;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

public enum Symbol {
    ADA,
    AUTO,
    BDO,
    BNB,
    BTC,
    BUSD,
    CAKE,
    CRO,
    DOT,
    EGG,
    ETH,
    SBDO,
    SUSHI,
    UNI,
    USDT,
    WATCH,
    WBNB;

    private static final Map<Symbol, Integer> COIN_MARKET_CAP_IDS = ImmutableMap.<Symbol, Integer>builder()
            .put(BDO, 8219)
            .put(SBDO, 8172)
            .put(WBNB, 7192)
            .put(BUSD, 4687)
            .put(EGG, 8449)
            .put(CAKE, 7186)
            .put(WATCH, 8621)
            .put(ADA, 2010)
            .put(AUTO, 8387)
            .put(BNB, 1839)
            .put(BTC, 1)
            .put(CRO, 3635)
            .put(DOT, 6636)
            .put(ETH, 1027)
            .put(SUSHI, 6758)
            .put(UNI, 7083)
            .put(USDT, 825)
            .build();

    public int getCoinMarketCapId() {
        return Optional.ofNullable(COIN_MARKET_CAP_IDS.get(this))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Symbol %s not supported", this)));
    }

}
