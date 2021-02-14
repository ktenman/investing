package ee.tenman.investing.integration.coinmarketcap;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.coinmarketcap.api.CoinMarketCapApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.openqa.selenium.By;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static ee.tenman.investing.configuration.FetchingConfiguration.TICKER_SYMBOL_MAP;
import static java.math.RoundingMode.HALF_UP;

@Slf4j
@Service
public class CoinMarketCapService {

    @Resource
    private BinanceService binanceService;

    @Resource
    private CoinMarketCapApiService coinMarketCapApiService;

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

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public Map<String, BigDecimal> getPricesInEur(List<String> tickers, BigDecimal busdToEur) {
        Map<String, BigDecimal> pricesInUsd = getPricesInUsd(tickers);
        Map<String, BigDecimal> pricesInEUr = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : pricesInUsd.entrySet()) {
            pricesInEUr.put(entry.getKey(), entry.getValue().multiply(busdToEur));
        }
        return pricesInEUr;
    }

    public BigDecimal eurPrice(String currency) {

        BigDecimal eurPrice = coinMarketCapApiService.eurPrice(currency);
        if (eurPrice != null && ComparableUtils.is(eurPrice).greaterThan(BigDecimal.ZERO)) {
            return eurPrice;
        }

        open("https://coinmarketcap.com/currencies/" + currency);

        $(By.tagName("div")).waitUntil(text("$"), 1000, 100);

        ElementsCollection selenideElements = Selenide.$$(By.tagName("p"));

        List<String> strings = Arrays.asList("BTC", "ETH");
        BigDecimal sum = strings.stream()
                .map(symbol -> Optional.of(selenideElements
                        .filter(text(symbol))
                        .first()
                        .text()
                        .replace(String.format(" %s", symbol), ""))
                        .map(amount -> {
                            log.info("{}: {}", symbol, amount);
                            return new BigDecimal(amount);
                        })
                        .map(a -> a.multiply(binanceService.getPriceToEur(symbol)))
                        .orElseThrow(() -> new RuntimeException(String.format("Price for %s not found", symbol)))
                )
                .map(Objects::requireNonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        closeWebDriver();
        BigDecimal average = sum.divide(BigDecimal.valueOf(strings.size()), HALF_UP);
        log.info("{}/EUR: {}", currency, average);
        return average;
    }

}
