package ee.tenman.investing.service;

import com.binance.api.client.domain.account.AssetBalance;
import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.integration.binance.BinanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.ROUND_UP;
import static org.apache.commons.lang3.compare.ComparableUtils.is;

@Service
@Slf4j
public class RebalancingService {

    private static final BigDecimal TEN_EUROS = new BigDecimal("10.000000000");
    @Resource
    private BinanceService binanceService;

    @Scheduled(cron = "0 0 21 1 4 ?")
    @Scheduled(cron = "0 0 21 1 7 ?")
    @Scheduled(cron = "0 0 21 30 9 ?")
    @Scheduled(cron = "0 0 21 30 12 ?")
    public void rebalance() {
        log.info("Starting rebalancing...");

        List<String> symbolsToRebalance = Arrays.asList(
                "BNB",
                "DOT",
                "UNI",
                "BTC",
                "SUSHI",
                "ADA",
                "ETH",
                "SNX"
        );
        List<String> eurTickers = Arrays.asList("BNB", "BTC", "ETH", "ADA", "DOT");

        List<AssetBalance> balances = binanceService.getBinanceApiRestClient().getAccount(
                60000L, binanceService.getBinanceApiRestClientCorrectTimestamp())
                .getBalances();

        Map<String, BigDecimal> assets = new HashMap<>();
        for (String symbol : symbolsToRebalance) {
            BigDecimal availableBalance = balances.stream()
                    .filter(assetBalance -> assetBalance.getAsset().equals(symbol))
                    .findFirst()
                    .map(assetBalance -> new BigDecimal(assetBalance.getFree()).add(locked(assetBalance.getAsset())))
                    .orElseThrow(() -> new RuntimeException("Not found EUR"));
            BigDecimal priceToEur = binanceService.getPriceToEur(symbol);
            BigDecimal total = availableBalance.multiply(priceToEur);
            assets.put(symbol, total);
        }
        BigDecimal totalAvailableBalance = assets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percentage = BigDecimal.ONE.setScale(8, ROUND_UP)
                .divide(new BigDecimal(symbolsToRebalance.size()), ROUND_UP);
        BigDecimal shouldHaveBalanceForSymbol = totalAvailableBalance.multiply(percentage);

        for (String symbol : symbolsToRebalance) {
            BigDecimal availableBalance = assets.get(symbol);
            BigDecimal subtrahend = shouldHaveBalanceForSymbol.multiply(multiplier(symbol));
            BigDecimal difference = availableBalance.subtract(subtrahend);

            if (!is(difference).greaterThan(BigDecimal.ZERO)) {
                continue;
            }

            if (eurTickers.contains(symbol)) {
                String ticker = symbol + "EUR";
                if (is(TEN_EUROS).greaterThan(difference.abs()) && is(difference.abs()).greaterThan(BigDecimal.valueOf(5))) {
                    binanceService.sell(ticker, TEN_EUROS);
                } else if (is(TEN_EUROS).greaterThan(difference.abs()) && is(difference.abs()).lessThanOrEqualTo(BigDecimal.valueOf(5))) {
                    log.info("Skipping selling {} with amount {}", ticker, difference.abs());
                } else {
                    binanceService.sell(ticker, difference.abs());
                }
            } else {
                BigDecimal bnb = binanceService.getPriceToEur("BNB");
                binanceService.sell(symbol + "BNB", difference.divide(bnb, ROUND_UP).abs());
            }
            BigDecimal subtract = assets.get(symbol).subtract(difference.abs());
            assets.put(symbol, subtract);
        }

        for (String symbol : symbolsToRebalance) {
            BigDecimal availableBalance = assets.get(symbol);
            BigDecimal subtrahend = shouldHaveBalanceForSymbol.multiply(multiplier(symbol));
            BigDecimal difference = availableBalance.subtract(subtrahend);

            if (!is(difference).lessThan(BigDecimal.ZERO)) {
                continue;
            }

            if (eurTickers.contains(symbol)) {
                String ticker = symbol + "EUR";
                if (is(TEN_EUROS).greaterThan(difference.abs()) && is(difference.abs()).greaterThan(BigDecimal.valueOf(5))) {
                    binanceService.buy(ticker, TEN_EUROS);
                } else if (is(TEN_EUROS).greaterThan(difference.abs()) && is(difference.abs()).lessThanOrEqualTo(BigDecimal.valueOf(5))) {
                    log.info("Skipping buying {} with amount {}", ticker, difference.abs());
                } else {
                    binanceService.buy(ticker, difference.abs());
                }
            } else {
                BigDecimal bnb = binanceService.getPriceToEur("BNB");
                binanceService.buy(symbol + "BNB", difference.divide(bnb, ROUND_UP).abs());
            }
            BigDecimal subtract = assets.get(symbol).add(difference.abs());
            assets.put(symbol, subtract);
        }

        log.info("Finished rebalancing...");
    }

    private BigDecimal multiplier(String symbol) {
        BigDecimal two = new BigDecimal("2.00000000");
        ImmutableMap<String, BigDecimal> map = ImmutableMap.of(
                "BNB", two,
                "BTC", two
        );
        return map.getOrDefault(symbol, new BigDecimal("1.00000000"));
    }

    private BigDecimal locked(String symbol) {
        ImmutableMap<String, BigDecimal> map = ImmutableMap.of(
                "DOT", new BigDecimal("10.21"),
                "SUSHI", new BigDecimal("10"),
                "1INCH", new BigDecimal("20.81")
        );
        return map.getOrDefault(symbol, BigDecimal.ZERO);
    }

}
