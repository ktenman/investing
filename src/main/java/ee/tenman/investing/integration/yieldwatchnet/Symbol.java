package ee.tenman.investing.integration.yieldwatchnet;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Symbol {
    ADA,
    AUTO,
    BDO,
    BNB,
    BTC,
    BTD,
    BTS,
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

    public static final Set<String> SYMBOL_NAMES = Stream.of(Symbol.values()).
            map(Symbol::name)
            .collect(Collectors.toSet());

    private static final Map<Symbol, Integer> COIN_MARKET_CAP_IDS = ImmutableMap.<Symbol, Integer>builder()
            .put(ADA, 2010)
            .put(AUTO, 8387)
            .put(BDO, 8219)
            .put(BNB, 1839)
            .put(BTC, 1)
            .put(BUSD, 4687)
            .put(CAKE, 7186)
            .put(CRO, 3635)
            .put(DOT, 6636)
            .put(EGG, 8449)
            .put(ETH, 1027)
            .put(SBDO, 8172)
            .put(SUSHI, 6758)
            .put(UNI, 7083)
            .put(USDT, 825)
            .put(WATCH, 8621)
            .put(WBNB, 7192)
            .build();

    public int coinMarketCapId() {
        return Optional.ofNullable(COIN_MARKET_CAP_IDS.get(this))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Symbol %s not supported", this)));
    }

}
