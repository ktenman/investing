package ee.tenman.investing.service;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import org.openqa.selenium.By;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;

@Service
public class CoinMarketCapService {

    public static final String BINANCE_COIN_ID = "Binance Coin";
    public static final String POLKADOT_ID = "Polkadot";
    public static final String CRO_ID = "Crypto.com Coin";

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public Map<String, BigDecimal> getPrices(String... tickers) {
        Configuration.startMaximized = true;
        Configuration.headless = true;
        Configuration.proxyEnabled = false;
        Configuration.screenshots = false;
        Configuration.browser = "firefox";
        closeWebDriver();
        open("https://coinmarketcap.com/");
        ElementsCollection selenideElements = $(By.tagName("table"))
                .$$(By.tagName("tr"));

        Map<String, BigDecimal> prices = new HashMap<>();
        for (String ticker : tickers) {
            String priceAsString = selenideElements.find(text(ticker))
                    .text()
                    .replace("\n", " ")
                    .split("\\$")[1]
                    .split(" ")[0];
            prices.put(ticker, new BigDecimal(priceAsString));
        }
        closeWebDriver();
        return prices;
    }
}
