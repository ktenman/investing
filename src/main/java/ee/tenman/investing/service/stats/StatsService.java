package ee.tenman.investing.service.stats;

import com.binance.api.client.domain.market.CandlestickInterval;
import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.integration.binance.BinanceService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.binance.api.client.domain.market.CandlestickInterval.DAILY;
import static java.math.BigDecimal.ROUND_UP;
import static java.math.RoundingMode.HALF_UP;
import static java.time.LocalTime.MIDNIGHT;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MONTHS;

@Slf4j
@Service
@AllArgsConstructor
public class StatsService {

    private final BinanceService binanceService;

    private static final Map<Integer, List<Integer>> WEEK_DAYS = ImmutableMap.of(
            1, Arrays.asList(1, 2, 3, 4, 5, 6, 7),
            2, Arrays.asList(8, 9, 10, 11, 12, 13, 14),
            3, Arrays.asList(15, 16, 17, 18, 19, 20, 21),
            4, Arrays.asList(22, 23, 24, 25, 26, 27, 28)
    );

    public Map<String, Coin> coins(List<String> symbols, CandlestickInterval candlestickInterval, int limit) {
        Map<String, Coin> coins = new TreeMap<>();
        symbols.forEach(symbol -> {
            Map<LocalDateTime, BigDecimal> prices = binanceService.getPrices(symbol + "USDT", candlestickInterval, limit);
            Coin coin = Coin.builder()
                    .name(symbol)
                    .prices(prices)
                    .build();
            coins.put(symbol, coin);
        });
        return coins;
    }

    public BigDecimal calculate(
            LocalDateTime startingDay,
            Map<String, Coin> coins,
            int frequency,
            int week,
            int dayOfWeek,
            int hour,
            int rebalanceHour,
            BigDecimal threshold,
            boolean rebalance,
            int minute,
            int rebalanceMinute
    ) {

        LocalDateTime temp = startingDay;
        LocalDateTime now = LocalDateTime.now();
        long duration = MONTHS.between(startingDay, temp);
        long prev = -1;

        List<BigDecimal> paid = new ArrayList<>();
        int unit = 0;
        int rebalanceCount = 0;

        Set<String> rebalancedDays = new HashSet<>();

        while (startingDay.isBefore(now)) {

            if (prev != duration &&
                    startingDay.getDayOfWeek().getValue() == dayOfWeek &&
                    startingDay.getHour() == hour &&
                    startingDay.getMinute() == minute &&
                    WEEK_DAYS.get(week).contains(startingDay.getDayOfMonth())
            ) {
                prev = duration;
                for (Coin coin : coins.values()) {
                    BigDecimal price = coin.getPrice(startingDay);
                    BigDecimal usdtAmount = BigDecimal.valueOf(25);
                    paid.add(usdtAmount);
                    BigDecimal amountBought = amount(price, usdtAmount);
                    coin.setAmount(coin.getAmount().add(amountBought));
                }
                log.info("Day of week : {}, Day of month: {}, {}", startingDay.getDayOfWeek(), startingDay.getDayOfMonth(), startingDay);
            }

            String rebalanceDay = startingDay.getYear() + "" + startingDay.getDayOfYear();
            if (startingDay.getDayOfYear() % frequency == 0 && !rebalancedDays.contains(rebalanceDay) &&
                    startingDay.getHour() == rebalanceHour &&
                    rebalance &&
                    startingDay.getMinute() == rebalanceMinute) {
                rebalancedDays.add(rebalanceDay);
                rebalanceCount += rebalance(coins, startingDay, threshold);
                log.info("REBALANCE Day of week : {}, Day of month: {}, {}", startingDay.getDayOfWeek(), startingDay.getDayOfMonth(), startingDay);
            }

            startingDay = startingDay.plusMinutes(1);
            duration = MONTHS.between(startingDay, temp);
            unit++;
        }

        BigDecimal total = totalValueInUsdt(coins.values(), now);
        log.info("Total value : {} and paid {} in {} unit. Frequency: {}. Rebalance count: {}. week: {}",
                total, paid.stream().reduce(BigDecimal.ZERO, BigDecimal::add), unit, frequency, rebalanceCount, week);

        for (Coin coin : coins.values()) {
            BigDecimal valueInUsdt = coin.valueInUsdt(now);
            log.info("{} : {} : {}", coin.getName(), valueInUsdt, valueInUsdt.divide(total, ROUND_UP));
        }

        return total;
    }

    public BigDecimal calculateDay(Map<String, Coin> coins, int frequency, int dayOfMonth, int dayOfWeek, int hour, int rebalanceHour, BigDecimal threshold, boolean rebalance) {

//        LocalDateTime startingDay = LocalDateTime.of(LocalDate.of(2018, 4, 21), MIDNIGHT);
        LocalDateTime startingDay = LocalDateTime.parse("2018-04-20T05:00:00.00");
        ;
        LocalDateTime temp = startingDay;
        LocalDateTime now = LocalDateTime.now();
        long duration = MONTHS.between(startingDay, temp);
        long prev = -1;

        List<BigDecimal> paid = new ArrayList<>();
        int unit = 0;
        int rebalanceCount = 0;

        Set<String> rebalancedDays = new HashSet<>();

        while (startingDay.isBefore(now)) {

            if (prev != duration &&
//                    startingDay.getDayOfWeek().getValue() == dayOfWeek &&
                    startingDay.getHour() == hour &&
                    startingDay.getDayOfMonth() == dayOfMonth
//                    WEEK_DAYS.get(week).contains(startingDay.getDayOfMonth())
            ) {
                prev = duration;
                for (Coin coin : coins.values()) {
                    BigDecimal price = coin.getPrice(startingDay);
                    BigDecimal usdtAmount = BigDecimal.valueOf(25);
                    paid.add(usdtAmount);
                    BigDecimal amountBought = amount(price, usdtAmount);
                    coin.setAmount(coin.getAmount().add(amountBought));
                }
                log.info("Day of week : {}, Day of month: {}, {}", startingDay.getDayOfWeek(), startingDay.getDayOfMonth(), startingDay);
            }

            String rebalanceDay = startingDay.getYear() + "" + startingDay.getDayOfYear();
            if (startingDay.getDayOfYear() % frequency == 0 && !rebalancedDays.contains(rebalanceDay) &&
                    startingDay.getHour() == rebalanceHour && rebalance) {
                rebalancedDays.add(rebalanceDay);
                rebalanceCount += rebalance(coins, startingDay, threshold);
                log.info("REBALANCE Day of week : {}, Day of month: {}, {}", startingDay.getDayOfWeek(), startingDay.getDayOfMonth(), startingDay);
            }

            startingDay = startingDay.plus(1, HOURS);
            duration = MONTHS.between(startingDay, temp);
            unit++;
        }

        BigDecimal total = totalValueInUsdt(coins.values(), now);
        log.info("Total value : {} and paid {} in {} unit. Frequency: {}. Rebalance count: {}. week: {}",
                total, paid.stream().reduce(BigDecimal.ZERO, BigDecimal::add), unit, frequency, rebalanceCount, dayOfMonth);

        for (Coin coin : coins.values()) {
            BigDecimal valueInUsdt = coin.valueInUsdt(now);
            log.info("{} : {} : {}", coin.getName(), valueInUsdt, valueInUsdt.divide(total, ROUND_UP));
        }

        return total;
    }


    public BigDecimal calculate(int frequency, int diff) {

        List<String> symbols = Arrays.asList("BTC", "BNB", "ETH", "ADA");
        Map<String, Coin> coins = new TreeMap<>();
        symbols.forEach(symbol -> {
            Map<LocalDateTime, BigDecimal> prices = binanceService.getPrices(symbol + "USDT", DAILY, 1028);
            Coin coin = Coin.builder()
                    .name(symbol)
                    .prices(prices)
                    .build();
            coins.put(symbol, coin);
        });

        LocalDateTime startingDay = LocalDateTime.of(LocalDate.of(2018, 4, 18 + diff), MIDNIGHT);
        LocalDateTime now = LocalDateTime.now();

        List<BigDecimal> paid = new ArrayList<>();
        int days = 0;
        int rebalanceCount = 0;

        while (startingDay.isBefore(now)) {

            if (days % 30 == 0) {
                for (Coin coin : coins.values()) {
                    BigDecimal price = coin.getPrice(startingDay.toLocalDate());
                    BigDecimal usdtAmount = BigDecimal.valueOf(25);
                    paid.add(usdtAmount);
                    BigDecimal amountBought = amount(price, usdtAmount);
                    coin.setAmount(coin.getAmount().add(amountBought));
                }
            }

            if (days % frequency == 0 && days != 0) {
                rebalanceCount++;
                rebalance(coins, startingDay.toLocalDate());
                log.info("Day of week : {}, Day of month: {}, {}", startingDay.getDayOfWeek(), startingDay.getDayOfMonth(), startingDay);
            }
            startingDay = startingDay.plus(1, DAYS);
            days++;
        }

//        rebalance(coins, now);

        BigDecimal total = totalValueInUsdt(coins.values(), now);
        log.info("Total value : {} and paid {} in {} days. Frequency: {}. Rebalance count: {}. Diff: {}",
                total, paid.stream().reduce(BigDecimal.ZERO, BigDecimal::add), days, frequency, rebalanceCount, diff);

        for (Coin coin : coins.values()) {
            BigDecimal valueInUsdt = coin.valueInUsdt(now.toLocalDate());
            log.info("{} : {} : {}", coin.getName(), valueInUsdt, valueInUsdt.divide(total, ROUND_UP));
        }

        return total;
    }

    private void rebalance(Map<String, Coin> coins, LocalDate startingDay) {
        BigDecimal total = totalValueInUsdt(coins.values(), startingDay);
        BigDecimal shouldHave = BigDecimal.valueOf(0.25).multiply(total);
        for (Map.Entry<String, Coin> e : coins.entrySet()) {
            Coin coin = e.getValue();
            BigDecimal valueInUsdt = coin.valueInUsdt(startingDay);
            if (ComparableUtils.is(valueInUsdt).greaterThan(shouldHave)) {
                BigDecimal amount = amount(coin.getPrice(startingDay), valueInUsdt.subtract(shouldHave));
                coin.setAmount(coin.getAmount().subtract(amount));
            } else {
                BigDecimal amount = amount(coin.getPrice(startingDay), shouldHave.subtract(valueInUsdt));
                coin.setAmount(coin.getAmount().add(amount));
            }
        }
    }

    private int rebalance(Map<String, Coin> coins, LocalDateTime startingDay, BigDecimal threshold) {
        // 4 ==== treshold
        BigDecimal multiplier = BigDecimal.valueOf(4).divide(BigDecimal.valueOf(coins.size()), HALF_UP);
        threshold = multiplier.multiply(threshold);

        BigDecimal total = totalValueInUsdt(coins.values(), startingDay);
        BigDecimal expectedPercentage = new BigDecimal("1.00000000").divide(BigDecimal.valueOf(coins.size()), HALF_UP);
        BigDecimal shouldHave = expectedPercentage.multiply(total);
        List<BigDecimal> percentages = new ArrayList<>();

        if (ComparableUtils.is(total).equalTo(BigDecimal.ZERO)) {
            log.info("Skipping rebalance... Total is ZERO");
            return 0;
        }

        for (Map.Entry<String, Coin> e : coins.entrySet()) {
            Coin coin = e.getValue();
            BigDecimal valueInUsdt = coin.valueInUsdt(startingDay);
            percentages.add(valueInUsdt.divide(total, HALF_UP));
        }

        boolean skipRebalncingBecauseOfThreshold = true;
        for (BigDecimal percentage : percentages) {
            BigDecimal difference = percentage.subtract(expectedPercentage).abs();
            if (ComparableUtils.is(difference).greaterThan(threshold)) {
                skipRebalncingBecauseOfThreshold = false;
            }
        }

        if (skipRebalncingBecauseOfThreshold) {
            log.info("Skipping rebalance...");
            return 0;
        }

        for (Map.Entry<String, Coin> e : coins.entrySet()) {
            Coin coin = e.getValue();
            BigDecimal valueInUsdt = coin.valueInUsdt(startingDay);
            if (ComparableUtils.is(valueInUsdt).greaterThan(shouldHave)) {
                BigDecimal amount = amount(coin.getPrice(startingDay), valueInUsdt.subtract(shouldHave));
                coin.setAmount(coin.getAmount().subtract(amount));
            } else {
                BigDecimal amount = amount(coin.getPrice(startingDay), shouldHave.subtract(valueInUsdt));
                coin.setAmount(coin.getAmount().add(amount));
            }
        }

        return 1;
    }

    BigDecimal totalValueInUsdt(Collection<Coin> coinSet, LocalDate localDate) {
        return coinSet.stream()
                .map(c -> c.valueInUsdt(localDate))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    BigDecimal totalValueInUsdt(Collection<Coin> coinSet, LocalDateTime localDateTime) {
        return coinSet.stream()
                .map(c -> c.valueInUsdt(localDateTime))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    BigDecimal amount(BigDecimal coinPrice, BigDecimal usdtAmount) {
        BigDecimal feeTaken = usdtAmount.multiply(new BigDecimal("0.99925"));
        return feeTaken.divide(coinPrice, ROUND_UP)
                .setScale(8, ROUND_UP);
    }


}
