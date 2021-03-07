package ee.tenman.investing.integration.coinmarketcap;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static ee.tenman.investing.configuration.FetchingConfiguration.TICKER_SYMBOL_MAP;
import static java.math.RoundingMode.HALF_UP;

@Slf4j
@Service
public class CoinMarketCapService {

    private static final Map<Symbol, String> SYMBOL_TO_CURRENCY = ImmutableMap.<Symbol, String>builder()
            .put(Symbol.WBNB, "wbnb")
            .put(Symbol.BDO, "bdollar")
            .put(Symbol.SBDO, "bdollar-share")
            .put(Symbol.BUSD, "binance-usd")
            .put(Symbol.EGG, "goose-finance")
            .put(Symbol.CAKE, "pancakeswap")
            .put(Symbol.WATCH, "yieldwatch")
            .put(Symbol.ADA, "cardano")
            .put(Symbol.AUTO, "auto")
            .put(Symbol.BNB, "binance-coin")
            .put(Symbol.BTC, "bitcoin")
            .put(Symbol.CRO, "crypto-com-coin")
            .put(Symbol.DOT, "polkadot-new")
            .put(Symbol.SUSHI, "sushiswap")
            .put(Symbol.USDT, "tether")
            .put(Symbol.UNI, "uniswap")
            .put(Symbol.ETH, "ethereum")
            .build();

    @Resource
    private BinanceService binanceService;

    private Map<String, BigDecimal> getPricesInUsd(List<String> tickers) {
        Map<String, BigDecimal> prices = new HashMap<>();

        closeWebDriver();
        open("https://coinmarketcap.com/");
        ElementsCollection selenideElements = $(By.tagName("table"))
                .$$(By.tagName("tr"));

        for (String ticker : tickers) {
            String priceAsString = selenideElements.find(text(ticker))
                    .text()
                    .replace("\n", " ")
                    .replace(",", "")
                    .split("\\$")[1]
                    .split(" ")[0];
            BigDecimal price = new BigDecimal(priceAsString);
            prices.put(ticker, price);
            log.info("{} price {}", TICKER_SYMBOL_MAP.get(ticker), price);
        }
        closeWebDriver();
        return prices;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public Map<String, BigDecimal> getPricesInEur(List<String> tickers, BigDecimal busdToEur) {
        Map<String, BigDecimal> pricesInUsd = getPricesInUsd(tickers);
        Map<String, BigDecimal> pricesInEUr = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : pricesInUsd.entrySet()) {
            pricesInEUr.put(entry.getKey(), entry.getValue().multiply(busdToEur));
        }
        return pricesInEUr;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public BigDecimal eurPrice(Symbol symbol) {

        open("https://coinmarketcap.com/currencies/" + SYMBOL_TO_CURRENCY.get(symbol));

        ElementsCollection selenideElements = Selenide.$$(By.tagName("p"));

        List<BigDecimal> prices = new ArrayList<>();

        Stream.of(Symbol.BTC.name(), Symbol.ETH.name())
                .map(s -> Optional.of(selenideElements
                        .filter(text(s))
                        .first()
                        .text()
                        .split(" ")[0])
                        .filter(this::isANumber)
                        .map(amount -> {
                            log.info("{}: {}", s, amount);
                            return new BigDecimal(amount);
                        })
                        .map(a -> a.multiply(binanceService.getPriceToEur(s)))
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .forEach(prices::add);

        closeWebDriver();

        BigDecimal average = prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(prices.size()), HALF_UP);

        log.info("{}/EUR: {}", symbol.name(), average);
        return average;
    }

    private boolean isANumber(String string) {
        try {
            new BigDecimal(string);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

}
