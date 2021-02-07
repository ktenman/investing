package ee.tenman.investing.integration.coinmarketcap;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static ee.tenman.investing.configuration.FetchingConfiguration.TICKER_SYMBOL_MAP;

@Slf4j
@Service
public class CoinMarketCapService {

    private Map<String, BigDecimal> getPricesInUsd(List<String> tickers) {
        Map<String, BigDecimal> prices = new HashMap<>();

        Configuration.startMaximized = true;
        Configuration.headless = true;
        Configuration.proxyEnabled = false;
        Configuration.screenshots = false;
        Configuration.browser = "firefox";
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
}
