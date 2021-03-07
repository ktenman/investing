package ee.tenman.investing.service;

import com.binance.api.client.domain.market.CandlestickInterval;
import ee.tenman.investing.exception.NotSupportedSymbolException;
import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.coingecko.CoinGeckoService;
import ee.tenman.investing.integration.coinmarketcap.CoinMarketCapService;
import ee.tenman.investing.integration.coinmarketcap.api.CoinMarketCapApiService;
import ee.tenman.investing.integration.cryptocom.CryptoComService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    private CoinMarketCapApiService coinMarketCapApiService;
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

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
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

    public BigDecimal toEur(Symbol symbol) {

        try {
            BigDecimal priceToEur = binanceService.getPriceToEur(symbol.name());
            if (priceToEur != null && ComparableUtils.is(priceToEur).greaterThan(ZERO)) {
                return priceToEur;
            }
        } catch (NotSupportedSymbolException ignored) {

        }

        BigDecimal coinMarketCapApiPrice = coinMarketCapApiService.eurPrice(symbol.name());
        if (coinMarketCapApiPrice != null && ComparableUtils.is(coinMarketCapApiPrice).greaterThan(ZERO)) {
            return coinMarketCapApiPrice;
        }

        BigDecimal coinMarketCapPrice = coinMarketCapService.eurPrice(symbol);
        if (coinMarketCapPrice != null && ComparableUtils.is(coinMarketCapPrice).greaterThan(ZERO)) {
            return coinMarketCapPrice;
        }

        return coinGeckoService.eurPrice(symbol);
    }

}
