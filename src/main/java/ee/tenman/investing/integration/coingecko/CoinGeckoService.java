package ee.tenman.investing.integration.coingecko;

import ee.tenman.investing.integration.binance.BinanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Optional;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.tagName;

@Slf4j
@Service
public class CoinGeckoService {

    @Resource
    BinanceService binanceService;

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 500))
    public BigDecimal eurPrice(String currency) {
        open("https://www.coingecko.com/en/coins/" + currency);

        $(tagName("h1")).waitUntil(text(currency), 2000, 200);

        BigDecimal btcPrice = Optional.of($$(className("text-muted"))
                .filter(text("BTC"))
                .first()
                .text()
                .split(" BTC")[0])
                .map(BigDecimal::new)
                .orElseThrow(() -> new RuntimeException("Couldn't fetch BTC price"));

        BigDecimal btcToEur = binanceService.getPriceToEur("BTC");

        closeWebDriver();
        BigDecimal price = btcPrice.multiply(btcToEur);

        log.info("{}/EUR: {}", currency, price);

        return price;
    }

}
