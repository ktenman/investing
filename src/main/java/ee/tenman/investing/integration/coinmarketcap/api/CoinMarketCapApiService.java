package ee.tenman.investing.integration.coinmarketcap.api;

import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.integration.binance.BinanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class CoinMarketCapApiService {

    private static final ImmutableMap<String, Integer> SYMBOL_TO_ID_MAP = ImmutableMap.of(
            "BDO", 8219,
            "bdollar", 8219,
            "wbnb", 7192,
            "WBNB", 7192
    );

    @Resource
    private CoinMarketCapApiClient coinMarketCapApiClient;

    @Resource
    private BinanceService binanceService;

    public BigDecimal eurPrice(String currency) {

        CoinInformation coinInformation = coinMarketCapApiClient.fetchCoinData(
                id(currency), "Mozilla/5.0", "BTC", "ETH"
        );

        log.info("{}", coinInformation);

        ImmutableMap<String, BigDecimal> prices = ImmutableMap.of(
                "BTC", btcPrice(coinInformation),
                "ETH", ethPrice(coinInformation)
        );

        Map<String, BigDecimal> binanceEurPrices = prices.keySet()
                .parallelStream()
                .collect(toMap(identity(), symbol -> binanceService.getPriceToEur(symbol)));

        BigDecimal sum = prices.keySet().stream()
                .map(symbol -> prices.get(symbol).multiply(binanceEurPrices.get(symbol)))
                .map(Objects::requireNonNull)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal average = sum.divide(BigDecimal.valueOf(prices.size()), HALF_UP);

        log.info("{}/EUR: {}", currency, average);

        return average;
    }

    private BigDecimal btcPrice(CoinInformation coinInformation) {
        return coinInformation.getData()
                .lastEntry()
                .getValue()
                .getBtcPrices()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No BTC price found"));
    }

    private BigDecimal ethPrice(CoinInformation coinInformation) {
        return coinInformation.getData()
                .lastEntry()
                .getValue()
                .getEthPrices()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No ETH price found"));
    }

    private Integer id(String symbol) {
        return Optional.ofNullable(SYMBOL_TO_ID_MAP.get(symbol))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Symbol %s not supported", symbol)));
    }

}
