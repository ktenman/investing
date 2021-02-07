package ee.tenman.investing.service.stats;

import ee.tenman.investing.integration.binance.BinanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.binance.api.client.domain.market.CandlestickInterval.DAILY;
import static java.time.LocalTime.MIDNIGHT;

@Slf4j
@Service
public class StatsService {

    @Resource
    private BinanceService binanceService;

    public void calculate() {

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

        LocalDateTime startingDay = LocalDateTime.of(LocalDate.of(2020, 1, 1), MIDNIGHT);
        LocalDateTime now = LocalDateTime.now();

        while (startingDay.isBefore(now)) {
            for (Coin coin : coins.values()) {
                BigDecimal price = coin.getPrice(startingDay.toLocalDate());
                BigDecimal amountBought = amount(price, BigDecimal.valueOf(25));
                coin.setAmount(coin.getAmount().add(amountBought));
            }
            startingDay = startingDay.plusMonths(1);
        }

        BigDecimal total = totalValueInUsdt(coins.values(), now);
        log.info("Total : {}", total);

        for (Coin coin : coins.values()) {
            BigDecimal valueInUsdt = coin.valueInUsdt(now.toLocalDate());
            log.info("{} : {} : {}", coin.getName(), valueInUsdt, valueInUsdt.divide(total, BigDecimal.ROUND_UP));
        }

        System.out.println();

    }

    BigDecimal totalValueInUsdt(Collection<Coin> coinSet, LocalDateTime localDateTime) {
        return coinSet.stream()
                .map(c -> c.valueInUsdt(localDateTime.toLocalDate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    BigDecimal amount(BigDecimal coinPrice, BigDecimal usdtAmount) {
        BigDecimal feeTaken = usdtAmount.multiply(new BigDecimal("0.99925"));
        return feeTaken.divide(coinPrice, BigDecimal.ROUND_UP)
                .setScale(8, BigDecimal.ROUND_UP);
    }


}
