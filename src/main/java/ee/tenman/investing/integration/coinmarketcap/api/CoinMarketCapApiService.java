package ee.tenman.investing.integration.coinmarketcap.api;

import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class CoinMarketCapApiService {

    @Resource
    private CoinMarketCapApiClient coinMarketCapApiClient;

    @Resource
    private BinanceService binanceService;

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public BigDecimal eurPrice(String currency) {

        CoinInformation coinInformation = coinMarketCapApiClient.fetchCoinData(
                Symbol.valueOf(currency).getCoinMarketCapId(), "Mozilla/5.0", Symbol.BTC.name(), Symbol.ETH.name()
        );

        log.info("{}", coinInformation);

        ImmutableMap<String, BigDecimal> prices = ImmutableMap.of(
                Symbol.BTC.name(), coinInformation.getBtcPrice(),
                Symbol.ETH.name(), coinInformation.getEthPrice()
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

}
