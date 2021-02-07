package ee.tenman.investing.service;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ROUND_UP;
import static java.math.RoundingMode.HALF_UP;

@Service
public class StatsService {

    @Resource
    BinanceApiRestClient binanceApiRestClient;

    @Resource
    BinanceService binanceService;

    public Map<String, BigDecimal> getPrices(String from, String to, CandlestickInterval candlestickInterval) {
        String fromTo = from + to;
        String toFrom = to + from;

        if (binanceService.isSupportedTicker(fromTo)) {
            return getPrices(fromTo, candlestickInterval);
        }

        Map<String, BigDecimal> prices = new TreeMap<>();
        if (binanceService.isSupportedTicker(toFrom)) {
            Map<String, BigDecimal> toFromPrices = getPrices(toFrom, candlestickInterval);
            for (Map.Entry<String, BigDecimal> entry : toFromPrices.entrySet()) {
                BigDecimal price = ONE.setScale(8, ROUND_UP).divide(entry.getValue(), ROUND_UP);
                prices.put(entry.getKey(), price);
            }
            return prices;
        } else if (!binanceService.isSupportedTicker(fromTo)) {
            if (binanceService.isSupportedTicker(from + "BTC")) {
                Map<String, BigDecimal> fromPrices = getPrices(from + "BTC", candlestickInterval);
                Map<String, BigDecimal> toPrices = getPrices("BTC" + to, candlestickInterval);
                for (Map.Entry<String, BigDecimal> entry : fromPrices.entrySet()) {
                    BigDecimal price = entry.getValue()
                            .multiply(toPrices.get(entry.getKey()))
                            .setScale(8, ROUND_UP);
                    prices.put(entry.getKey(), price);
                }
                return prices;
            }
            if (binanceService.isSupportedTicker("BTC" + to)) {
                Map<String, BigDecimal> fromPrices = getPrices("BTC" + from, candlestickInterval);
                Map<String, BigDecimal> toPrices = getPrices("BTC" + to, candlestickInterval);
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

    public Map<String, BigDecimal> getPrices(String fromTo, CandlestickInterval candlestickInterval) {
        Map<String, BigDecimal> prices = new TreeMap<>();

        List<Candlestick> candlestickBars = binanceApiRestClient.getCandlestickBars(fromTo, candlestickInterval);
        for (Candlestick candlestick : candlestickBars) {
            prices.put(Instant.ofEpochMilli(candlestick.getCloseTime()).toString(), new BigDecimal(candlestick.getClose()));
        }

        return prices;
    }


}
