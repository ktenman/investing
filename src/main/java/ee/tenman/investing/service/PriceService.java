package ee.tenman.investing.service;

import com.binance.api.client.domain.market.CandlestickInterval;
import ee.tenman.investing.exception.NotSupportedSymbolException;
import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.coingecko.CoinGeckoService;
import ee.tenman.investing.integration.coinmarketcap.CoinMarketCapService;
import ee.tenman.investing.integration.cryptocom.CryptoComService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static ee.tenman.investing.configuration.FetchingConfiguration.TICKER_SYMBOL_MAP;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ROUND_UP;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

@Service
@Slf4j
public class PriceService {

    @Resource
    private CoinMarketCapService coinMarketCapService;
    @Resource
    private BinanceService binanceService;
    @Resource
    private CryptoComService cryptoComService;
    @Resource
    private CoinGeckoService coinGeckoService;

    public Map<String, BigDecimal> getPrices(String from, String to, CandlestickInterval candlestickInterval) {
        String fromTo = from + to;
        String toFrom = to + from;

        if (binanceService.isSupportedTicker(fromTo)) {
            return binanceService.getPrices(fromTo, candlestickInterval);
        }

        Map<String, BigDecimal> prices = new TreeMap<>();
        if (binanceService.isSupportedTicker(toFrom)) {
            Map<String, BigDecimal> toFromPrices = binanceService.getPrices(toFrom, candlestickInterval);
            for (Map.Entry<String, BigDecimal> entry : toFromPrices.entrySet()) {
                BigDecimal price = ONE.setScale(8, ROUND_UP).divide(entry.getValue(), ROUND_UP);
                prices.put(entry.getKey(), price);
            }
            return prices;
        } else if (!binanceService.isSupportedTicker(fromTo)) {
            if (binanceService.isSupportedTicker(from + "BTC")) {
                Map<String, BigDecimal> fromPrices = binanceService.getPrices(from + "BTC", candlestickInterval);
                Map<String, BigDecimal> toPrices = binanceService.getPrices("BTC" + to, candlestickInterval);
                for (Map.Entry<String, BigDecimal> entry : fromPrices.entrySet()) {
                    BigDecimal price = entry.getValue()
                            .multiply(toPrices.get(entry.getKey()))
                            .setScale(8, ROUND_UP);
                    prices.put(entry.getKey(), price);
                }
                return prices;
            }
            if (binanceService.isSupportedTicker("BTC" + to)) {
                Map<String, BigDecimal> fromPrices = binanceService.getPrices("BTC" + from, candlestickInterval);
                Map<String, BigDecimal> toPrices = binanceService.getPrices("BTC" + to, candlestickInterval);
                for (Map.Entry<String, BigDecimal> entry : fromPrices.entrySet()) {
                    BigDecimal price = toPrices.get(entry.getKey())
                            .divide(entry.getValue(), HALF_UP)
                            .setScale(8, ROUND_UP);
                    prices.put(entry.getKey(), price);
                }
                return prices;
            }
        }

        throw new NotSupportedSymbolException(String.format("%s not supported", fromTo));
    }

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public Map<String, BigDecimal> getPrices(Collection<String> input) {
        List<String> tickers = new ArrayList<>(input);
        Map<String, BigDecimal> binancePrices = new HashMap<>();

        for (String ticker : input) {
            try {
                String symbol = TICKER_SYMBOL_MAP.get(ticker);
                BigDecimal priceToEur = binanceService.getPriceToEur(symbol);
                binancePrices.put(ticker, priceToEur);
                tickers.remove(ticker);
            } catch (Exception ignored) {

            }
        }

        if (tickers.isEmpty()) {
            return binancePrices;
        }

        input = new ArrayList<>(tickers);

        BigDecimal btcToEur = binanceService.getPriceToEur("BTC");
        for (String ticker : input) {
            try {
                String symbol = TICKER_SYMBOL_MAP.get(ticker);
                BigDecimal priceToEur = cryptoComService.getInstrumentPrice(symbol, "BTC");
                binancePrices.put(ticker, priceToEur.multiply(btcToEur));
                tickers.remove(ticker);
            } catch (Exception ignored) {

            }
        }

        if (tickers.isEmpty()) {
            return binancePrices;
        }

        BigDecimal busdToEur = binanceService.getPriceToEur("BUSD");
        Map<String, BigDecimal> coinMarketCapServicePrices = coinMarketCapService.getPricesInEur(tickers, busdToEur);
        binancePrices.putAll(coinMarketCapServicePrices);
        return binancePrices;
    }

    public BigDecimal toEur(String currency) throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newWorkStealingPool(2);

        CompletableFuture<BigDecimal> coinMarketCapServiceSupplier = CompletableFuture.supplyAsync(
                () -> coinMarketCapService.eur(currency)
        );
        CompletableFuture<BigDecimal> coinGeckoPriceSupplier = CompletableFuture.supplyAsync(
                () -> coinGeckoService.eur(currency)
        );

        List<CompletableFuture<BigDecimal>> futures = Arrays.asList(coinMarketCapServiceSupplier, coinGeckoPriceSupplier);

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        for (CompletableFuture<?> future : futures) {
            executorService.submit(() -> future);
        }

        combinedFuture.get();

        List<BigDecimal> prices = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .filter(bigDecimal -> ComparableUtils.is(bigDecimal).greaterThan(ZERO))
                .collect(Collectors.toList());

        BigDecimal averagePrice = average(prices);

        log.info("Average {}/EUR price", averagePrice);

        return averagePrice;
    }

    public BigDecimal average(List<BigDecimal> bigDecimals) {
        BigDecimal sum = bigDecimals.stream()
                .map(Objects::requireNonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(bigDecimals.size()), HALF_UP);
    }

}
