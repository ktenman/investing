package ee.tenman.investing.service;

import ee.tenman.investing.cryptocom.CryptoComService;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ee.tenman.investing.configuration.FetchingConfiguration.TICKER_SYMBOL_MAP;

@Service
public class PriceService {

    @Resource
    CoinMarketCapService coinMarketCapService;

    @Resource
    BinanceService binanceService;

    @Resource
    CryptoComService cryptoComService;

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public Map<String, BigDecimal> getPrices(List<String> input) {
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

}
