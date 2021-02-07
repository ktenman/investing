package ee.tenman.investing.integration.binance;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.exception.BinanceApiException;
import ee.tenman.investing.exception.NotSupportedSymbolException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.list.TreeList;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static java.math.BigDecimal.ROUND_UP;
import static java.math.RoundingMode.DOWN;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
public class BinanceService {

    private static final BigDecimal TEN_EUROS = new BigDecimal("10.000000000");
    private static final BigDecimal THIRTY_EUROS = new BigDecimal("30.000000000");

    @Resource
    BinanceApiRestClient binanceApiRestClient;

    private List<SymbolInfo> symbolInfos;

    @Scheduled(cron = "0 0 * * * *")
    @PostConstruct
    public void fetchSupportedSymbols() {
        log.info("Fetching supported symbols");
        symbolInfos = binanceApiRestClient.getExchangeInfo().getSymbols();
    }

    public boolean isSupportedTicker(String symbol) {
        return symbolInfos.stream().anyMatch(s -> s.getSymbol().equals(symbol));
    }

    private boolean isSupportedSymbol(String symbol) {
        return symbolInfos.stream().anyMatch(s -> s.getSymbol().contains(symbol));
    }

    public BigDecimal getPriceToEur(String symbol) {

        String symbolToEur = symbol + "EUR";
        if (!isSupportedTicker(symbolToEur)) {
            if (isSupportedTicker(symbol + "BTC")) {
                BigDecimal symbolInBtc = getPriceToEur(symbol, "BTC");
                BigDecimal btcInEuro = getPriceToEur("BTC", "EUR");
                BigDecimal price = symbolInBtc.multiply(btcInEuro);
                log.info("{} price {}", symbolToEur, price);
                return price;
            }
            if (isSupportedTicker("BTC" + symbol)) {
                BigDecimal btcInSymbol = getPriceToEur("BTC", symbol);
                BigDecimal btcInEuro = getPriceToEur("BTC", "EUR");
                BigDecimal price = btcInEuro.divide(btcInSymbol, RoundingMode.HALF_UP);
                log.info("{} price {}", symbolToEur, price);
                return price;
            }
            throw new NotSupportedSymbolException(String.format("%s not supported", symbolToEur));
        }

        BigDecimal price = getPriceToEur(symbol, "EUR");
        log.info("{} price {}", symbolToEur, price);
        return price;
    }

    public BigDecimal getPriceToEur(String from, String to) {
        if (!isSupportedSymbol(from)) {
            throw new NotSupportedSymbolException(String.format("From symbol %s not supported", from));
        }

        if (!isSupportedSymbol(to)) {
            throw new NotSupportedSymbolException(String.format("To symbol %s not supported", to));
        }

        return new BigDecimal(binanceApiRestClient.getPrice(from + to).getPrice());
    }

    @Scheduled(cron = "0 0 12 1-7 * MON")
    public void buyCrypto() {

        buy("BTCEUR", TEN_EUROS);
        buy("DOTEUR", TEN_EUROS);
        buy("ADAEUR", TEN_EUROS);
        buy("ETHEUR", TEN_EUROS);

        BigDecimal totalEuros = binanceApiRestClient.getAccount().getBalances().stream()
                .filter(assetBalance -> assetBalance.getAsset().equals("EUR"))
                .findFirst()
                .map(AssetBalance::getFree)
                .map(BigDecimal::new)
                .orElseThrow(() -> new RuntimeException("Not found EUR"));

        BigDecimal baseAmount = totalEuros.compareTo(THIRTY_EUROS) >= 0 ? THIRTY_EUROS : totalEuros;
        BigDecimal boughtBnbAmount = buy("BNBEUR", baseAmount);

        BigDecimal thirtyThreePercent = BigDecimal.valueOf(0.3333333333333333);
        buy("UNIBNB", boughtBnbAmount.multiply(thirtyThreePercent));
        buy("SUSHIBNB", boughtBnbAmount.multiply(thirtyThreePercent));
    }

    private BigDecimal buy(String ticker, BigDecimal baseAmount) {
        String stepSize = binanceApiRestClient.getExchangeInfo().getSymbolInfo(ticker).getSymbolFilter(FilterType.LOT_SIZE).getStepSize();
        int scale = scale(stepSize);
        BigDecimal quantity = quantity(binanceApiRestClient, ticker, baseAmount).setScale(scale, ROUND_UP);
        boolean success = false;
        while (!success) {
            try {
                binanceApiRestClient.newOrder(marketBuy(ticker, quantity.toString()));
                log.info("Success {} with amount {}", ticker, quantity);
                success = true;
            } catch (BinanceApiException e) {
                log.error("", e);
                log.info("Failed {} with amount {}", ticker, quantity);
                if ("Account has insufficient balance for requested action.".equals(e.getMessage())) {
                    quantity = quantity.subtract(new BigDecimal(stepSize)).setScale(scale, ROUND_UP);
                } else {
                    quantity = quantity.add(new BigDecimal(stepSize)).setScale(scale, ROUND_UP);
                }
            }
        }
        return quantity;
    }

    int scale(String stepSize) {
        double amount = new BigDecimal(stepSize).doubleValue();
        int scale = 0;
        while (amount != 1) {
            amount *= 10;
            scale++;
        }
        return scale;
    }

    BigDecimal quantity(BinanceApiRestClient client, String ticker, BigDecimal baseAmount) {
        String stepSize = client.getExchangeInfo().getSymbolInfo(ticker).getSymbolFilter(FilterType.LOT_SIZE).getStepSize();
        String price = client.getPrice(ticker).getPrice();
        BigDecimal precision = new BigDecimal(stepSize);
        BigDecimal amount = baseAmount
                .divide(new BigDecimal(price), DOWN);
        return BigDecimal.valueOf(mRound(amount.doubleValue(), precision.doubleValue()));
    }

    double mRound(double value, double factor) {
        return Math.round(value / factor) * factor;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 200))
    public Map<String, BigDecimal> getPrices(String fromTo, CandlestickInterval candlestickInterval) {

        return getCandlestickBars(fromTo, candlestickInterval).stream()
                .collect(toMap(
                        candlestick -> Instant.ofEpochMilli(candlestick.getCloseTime()).toString(),
                        candlestick -> new BigDecimal(candlestick.getClose()),
                        (a, b) -> b,
                        TreeMap::new
                ));
    }

    private List<Candlestick> getCandlestickBars(String fromTo, CandlestickInterval candlestickInterval) {
        return binanceApiRestClient.getCandlestickBars(fromTo, candlestickInterval);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 200))
    public Map<LocalDateTime, BigDecimal> getPrices(String fromTo, CandlestickInterval candlestickInterval, int limit) {
        List<Candlestick> candlestickBars = getCandlestickBars(fromTo, candlestickInterval, limit);

        return getCandlestickBars(fromTo, candlestickInterval, limit).stream()
                .collect(toMap(
                        candlestick -> LocalDateTime.ofInstant(Instant.ofEpochMilli(candlestick.getCloseTime()), UTC),
                        candlestick -> new BigDecimal(candlestick.getClose()),
                        (a, b) -> b,
                        TreeMap::new
                ));
    }

    private List<Candlestick> getCandlestickBars(String fromTo, CandlestickInterval candlestickInterval, final int limit) {
        LocalDateTime now = LocalDateTime.now();
        ChronoUnit chronoUnit = chronoUnit(candlestickInterval);
        List<Candlestick> candlesticks = new TreeList<>();
        int step = Math.min(1000, limit);
        int startLimit = step;
        int endLimit = 0;
        while (endLimit <= limit * 1.1 || startLimit == step) {
            long start = now.minus(startLimit, chronoUnit).toInstant(UTC).toEpochMilli();
            long end = now.minus(endLimit, chronoUnit).toInstant(UTC).toEpochMilli();
            List<Candlestick> candlestickBars = binanceApiRestClient.getCandlestickBars(
                    fromTo,
                    candlestickInterval,
                    step,
                    start,
                    end
            );
            candlesticks.addAll(candlestickBars);
            startLimit += step;
            endLimit += step;
        }
        return candlesticks.stream()
                .sorted((a, b) -> b.getCloseTime().compareTo(a.getCloseTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private ChronoUnit chronoUnit(CandlestickInterval candlestickInterval) {
        switch (candlestickInterval) {
            case ONE_MINUTE:
                return ChronoUnit.MINUTES;
            case HOURLY:
                return ChronoUnit.HOURS;
            case DAILY:
                return ChronoUnit.DAYS;
            case TWELVE_HOURLY:
                return ChronoUnit.HALF_DAYS;
            case WEEKLY:
                return ChronoUnit.WEEKS;
            case MONTHLY:
                return ChronoUnit.MONTHS;
            default:
                throw new IllegalArgumentException(String.format("%s not supported", candlestickInterval));
        }

    }

}
