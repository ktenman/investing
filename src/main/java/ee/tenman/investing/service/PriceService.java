package ee.tenman.investing.service;

import com.binance.api.client.domain.market.CandlestickInterval;
import ee.tenman.investing.exception.NotSupportedSymbolException;
import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.coingecko.CoinGeckoService;
import ee.tenman.investing.integration.coinmarketcap.CoinMarketCapService;
import ee.tenman.investing.integration.coinmarketcap.api.CoinMarketCapApiService;
import ee.tenman.investing.integration.cryptocom.CryptoComService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.integration.yieldwatchnet.api.Balance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BTC;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BTS;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.CRO;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ROUND_UP;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

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

    public Map<Symbol, BigDecimal> toEur(Set<Symbol> symbols) {
        return symbols.parallelStream()
                .collect(toMap(identity(), this::toEur));
    }

    public BigDecimal toEur(Symbol symbol) {

        try {
            if (symbol == BTS) {
                throw new NotSupportedSymbolException("BTS not supported");
            }
            BigDecimal priceToEur = binanceService.getPriceToEur(symbol.name());
            if (priceToEur != null && ComparableUtils.is(priceToEur).greaterThan(ZERO)) {
                return priceToEur;
            }
        } catch (NotSupportedSymbolException ignored) {
        }

        if (symbol == CRO) {
            try {
                BigDecimal btcToEur = binanceService.getPriceToEur(BTC.name());
                BigDecimal priceToBtc = cryptoComService.getInstrumentPrice(CRO.name(), BTC.name());
                BigDecimal priceToEur = priceToBtc.multiply(btcToEur);
                if (priceToEur != null && ComparableUtils.is(priceToEur).greaterThan(ZERO)) {
                    return priceToEur;
                }
            } catch (Exception ignored) {

            }
        }

        try {
            BigDecimal coinMarketCapApiPrice = coinMarketCapApiService.eurPrice(symbol.name());
            if (coinMarketCapApiPrice != null && ComparableUtils.is(coinMarketCapApiPrice).greaterThan(ZERO)) {
                return coinMarketCapApiPrice;
            }
        } catch (Exception ignored) {
        }

        try {
            BigDecimal coinMarketCapPrice = coinMarketCapService.eurPrice(symbol);
            if (coinMarketCapPrice != null && ComparableUtils.is(coinMarketCapPrice).greaterThan(ZERO)) {
                return coinMarketCapPrice;
            }
        } catch (Exception ignored) {
        }

        return coinGeckoService.eurPrice(symbol);
    }

    public Map<Symbol, BigDecimal> to24HDifference(List<Symbol> symbols) {
        return symbols.parallelStream()
                .collect(toMap(
                        identity(),
                        symbol -> coinMarketCapApiService.differenceIn24Hours(symbol),
                        (a, b) -> b,
                        TreeMap::new
                ));
    }

    public Map<Symbol, BigDecimal> getPricesOfBalances(Set<Balance> poolBalances) {
        return poolBalances.stream()
                .map(Balance::getSymbol)
                .filter(StringUtils::isNotBlank)
                .map(String::toUpperCase)
                .map(Symbol::valueOf)
                .collect(toMap(
                        identity(),
                        this::toEur,
                        (v1, v2) -> {
                            throw new IllegalStateException(String.format("Duplicate key for values %s and %s", v1, v2));
                        },
                        TreeMap::new
                ));
    }
}
