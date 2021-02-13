package ee.tenman.investing.integration.binance;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.exception.BinanceApiException;
import ee.tenman.investing.exception.NotSupportedSymbolException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.list.TreeList;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ROUND_UP;
import static java.math.RoundingMode.DOWN;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
@AllArgsConstructor
public class BinanceService {

    private static final BigDecimal TEN_EUROS = new BigDecimal("10.000000000");
    private static final BigDecimal THIRTY_EUROS = new BigDecimal("30.000000000");

    private final BinanceApiRestClient binanceApiRestClient;

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

    public BinanceApiRestClient getBinanceApiRestClient() {
        return binanceApiRestClient;
    }

    public long getBinanceApiRestClientCorrectTimestamp() {
        Instant now = Instant.now();
        return now.toEpochMilli() - (now.toEpochMilli() - getBinanceApiRestClient().getServerTime());
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

    public void buyCrypto2() {

        buy("BTCEUR", new BigDecimal("49.840000000000000"));
//        buy("DOTEUR", TEN_EUROS);
        buy("ADAEUR", new BigDecimal("21.590000000000"));
        buy("ETHEUR", new BigDecimal("23.2200000000000"));

//        BigDecimal totalEuros = binanceApiRestClient.getAccount().getBalances().stream()
//                .filter(assetBalance -> assetBalance.getAsset().equals("EUR"))
//                .findFirst()
//                .map(AssetBalance::getFree)
//                .map(BigDecimal::new)
//                .orElseThrow(() -> new RuntimeException("Not found EUR"));
//
//        BigDecimal baseAmount = totalEuros.compareTo(THIRTY_EUROS) >= 0 ? THIRTY_EUROS : totalEuros;
//        BigDecimal boughtBnbAmount = buy("BNBEUR", baseAmount);
//
//        BigDecimal thirtyThreePercent = BigDecimal.valueOf(0.3333333333333333);
//        buy("UNIBNB", boughtBnbAmount.multiply(thirtyThreePercent));
//        buy("SUSHIBNB", boughtBnbAmount.multiply(thirtyThreePercent));
    }

    //    @Scheduled(cron = "0 0 12 1-7 * MON")
    @Scheduled(cron = "0 0 20 23-29 * THU")
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

    public BigDecimal buy(String ticker, BigDecimal baseAmount) {
        return trade(ticker, baseAmount, true);
    }

    public BigDecimal sell(String ticker, BigDecimal baseAmount) {
        return trade(ticker, baseAmount, false);
    }

    private BigDecimal trade(String ticker, BigDecimal baseAmount, boolean isBuy) {
        String stepSize = binanceApiRestClient.getExchangeInfo().getSymbolInfo(ticker).getSymbolFilter(FilterType.LOT_SIZE).getStepSize();
        int scale = scale(stepSize);
        BigDecimal quantity = quantity(binanceApiRestClient, ticker, baseAmount).setScale(scale, ROUND_UP);
        boolean success = false;
        int tryCount = 0;
        while (!success && tryCount < 3) {
            try {
                long correctTimestamp = getBinanceApiRestClientCorrectTimestamp();
                binanceApiRestClient.newOrderTest(isBuy ?
                        NewOrderWithTimestamp.marketBuy(ticker, quantity.toString(), correctTimestamp) :
                        NewOrderWithTimestamp.marketSell(ticker, quantity.toString(), correctTimestamp)
                );
                log.info("{} Success {} with amount {}", isBuy ? "BUY" : "SELL", ticker, quantity);
                success = true;
            } catch (BinanceApiException e) {
                log.error("", e);
                log.info("{} Failed {} with amount {}", isBuy ? "BUY" : "SELL", ticker, quantity);
                if ("Account has insufficient balance for requested action.".equals(e.getMessage())) {
                    quantity = quantity.subtract(new BigDecimal(stepSize)).setScale(scale, ROUND_UP);
                } else {
                    quantity = quantity.add(new BigDecimal(stepSize)).setScale(scale, ROUND_UP);
                }
            }
            tryCount++;
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
        int minuteMultiplier = minuteMultiplier(candlestickInterval);
        List<Candlestick> candlesticks = new TreeList<>();

        int step = Math.min(1000, limit);
        int startLimit = step;
        int endLimit = 0;

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        List<CompletableFuture> futures = new ArrayList<>();

        while (endLimit <= limit || startLimit == step) {
            long start = now.minus(startLimit * minuteMultiplier, chronoUnit).toInstant(UTC).toEpochMilli();
            long end = now.minus(endLimit * minuteMultiplier, chronoUnit).toInstant(UTC).toEpochMilli();

            try {
                TimeUnit.MILLISECONDS.sleep(51);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
                List<Candlestick> candlestickBars = binanceApiRestClient.getCandlestickBars(fromTo, candlestickInterval, step, start, end);
                candlesticks.addAll(candlestickBars);
            });
            futures.add(completableFuture);

            startLimit += step;
            endLimit += step;
        }

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        for (CompletableFuture<?> future : futures) {
            executorService.submit(() -> future);
        }
        combinedFuture.join();

        return candlesticks.stream()
                .sorted((a, b) -> b.getCloseTime().compareTo(a.getCloseTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private int minuteMultiplier(CandlestickInterval candlestickInterval) {
        switch (candlestickInterval) {
            case ONE_MINUTE:
            case HOURLY:
            case DAILY:
            case TWELVE_HOURLY:
            case WEEKLY:
            case MONTHLY:
                return 1;
            case THREE_MINUTES:
                return 3;
            case FIVE_MINUTES:
                return 5;
            case FIFTEEN_MINUTES:
                return 15;
            case HALF_HOURLY:
                return 30;
            default:
                throw new IllegalArgumentException(String.format("%s not supported", candlestickInterval));
        }
    }

    private ChronoUnit chronoUnit(CandlestickInterval candlestickInterval) {
        switch (candlestickInterval) {
            case ONE_MINUTE:
            case THREE_MINUTES:
            case FIVE_MINUTES:
            case FIFTEEN_MINUTES:
            case HALF_HOURLY:
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

    public Map<String, BigDecimal> fetchAvailableBalances(Collection<String> assetNames) {
        List<AssetBalance> balances = getBinanceApiRestClient()
                .getAccount(60000L, getBinanceApiRestClientCorrectTimestamp())
                .getBalances();
        return assetNames.stream()
                .map(assetName -> balances.stream()
                        .filter(b -> b.getAsset().equals(assetName))
                        .findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toMap(AssetBalance::getAsset, assetBalance -> new BigDecimal(assetBalance.getFree())));
    }


}
