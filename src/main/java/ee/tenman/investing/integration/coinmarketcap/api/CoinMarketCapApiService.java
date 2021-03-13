package ee.tenman.investing.integration.coinmarketcap.api;

import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static ee.tenman.investing.integration.coinmarketcap.api.CoinMarketCapApiClient.USER_AGENT;
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
    public BigDecimal differenceIn24Hours(Symbol symbol) {
        Instant toDateTime = Instant.now();
        Instant fromDateTime = toDateTime.minus(1, ChronoUnit.DAYS);

        CoinInformation coinInformation = coinMarketCapApiClient.fetchCoinData(
                symbol.coinMarketCapId(), USER_AGENT,
                fromDateTime.getEpochSecond(),
                toDateTime.getEpochSecond(),
                "EUR"
        );

        return coinInformation.getDifferenceIn24Hours()
                .subtract(BigDecimal.ONE)
                .multiply(new BigDecimal("100.00"));
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public BigDecimal eurPrice(String currency) {

        String[] baseSymbols = {Symbol.BTC.name(), Symbol.ETH.name()};

        CoinInformation coinInformation = coinMarketCapApiClient.fetchCoinData(
                Symbol.valueOf(currency).coinMarketCapId(),
                USER_AGENT,
                baseSymbols
        );

        log.info("{}", coinInformation);

        Map<String, BigDecimal> prices = Stream.of(baseSymbols)
                .collect(toMap(identity(), coinInformation::getLastPriceOf));

        Map<String, BigDecimal> binanceEurPrices = prices.keySet()
                .stream()
                .parallel()
                .collect(toMap(identity(), symbol -> binanceService.getPriceToEur(symbol)));

        BigDecimal sum = prices.keySet()
                .stream()
                .parallel()
                .map(symbol -> prices.get(symbol).multiply(binanceEurPrices.get(symbol)))
                .map(Objects::requireNonNull)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal average = sum.divide(BigDecimal.valueOf(prices.size()), HALF_UP);

        log.info("{}/EUR: {}", currency, average);

        return average;
    }

}
