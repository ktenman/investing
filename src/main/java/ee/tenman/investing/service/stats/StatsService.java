package ee.tenman.investing.service.stats;

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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.binance.api.client.domain.market.CandlestickInterval.DAILY;
import static java.math.BigDecimal.ROUND_UP;
import static java.time.LocalTime.MIDNIGHT;
import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@Service
@AllArgsConstructor
public class StatsService {

    private final BinanceService binanceService;

    public Map<String, Coin> coins() {
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
        return coins;
    }

    public BigDecimal calculate(Map<String, Coin> coins, int frequency, int diff) {

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
                rebalance(coins, startingDay);
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
                rebalance(coins, startingDay);
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

    private void rebalance(Map<String, Coin> coins, LocalDateTime startingDay) {
        BigDecimal total = totalValueInUsdt(coins.values(), startingDay);
        BigDecimal shouldHave = BigDecimal.valueOf(0.25).multiply(total);
        for (Map.Entry<String, Coin> e : coins.entrySet()) {
            Coin coin = e.getValue();
            BigDecimal valueInUsdt = coin.valueInUsdt(startingDay.toLocalDate());
            if (ComparableUtils.is(valueInUsdt).greaterThan(shouldHave)) {
                BigDecimal amount = amount(coin.getPrice(startingDay.toLocalDate()), valueInUsdt.subtract(shouldHave));
                coin.setAmount(coin.getAmount().subtract(amount));
            } else {
                BigDecimal amount = amount(coin.getPrice(startingDay.toLocalDate()), shouldHave.subtract(valueInUsdt));
                coin.setAmount(coin.getAmount().add(amount));
            }
        }

    }

    BigDecimal totalValueInUsdt(Collection<Coin> coinSet, LocalDateTime localDateTime) {
        return coinSet.stream()
                .map(c -> c.valueInUsdt(localDateTime.toLocalDate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    BigDecimal amount(BigDecimal coinPrice, BigDecimal usdtAmount) {
        BigDecimal feeTaken = usdtAmount.multiply(new BigDecimal("0.99925"));
        return feeTaken.divide(coinPrice, ROUND_UP)
                .setScale(8, ROUND_UP);
    }


}
