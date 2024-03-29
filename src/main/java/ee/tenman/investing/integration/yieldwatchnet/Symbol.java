package ee.tenman.investing.integration.yieldwatchnet;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

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
    KEBAB,
    MDX,
    SBDO,
    SUSHI,
    UNCX,
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
            .put(BTD, 8204)
            .put(BTS, 8205)
            .put(KEBAB, 8334)
            .put(UNCX, 7664)
            .put(MDX, 8335)
            .build();

    public int coinMarketCapId() {
        return Optional.ofNullable(COIN_MARKET_CAP_IDS.get(this))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Symbol %s not supported", this)));
    }

    public static boolean areSupported(String... values) {
        return Stream.of(values).allMatch(Symbol::isSupported);
    }

    public static boolean isSupported(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return SYMBOL_NAMES.contains(value.toUpperCase());
    }

}
