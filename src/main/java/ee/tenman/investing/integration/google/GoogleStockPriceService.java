package ee.tenman.investing.integration.google;

import ee.tenman.investing.domain.StockPrice;
import ee.tenman.investing.domain.StockSymbol;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.open;
import static ee.tenman.investing.domain.Currency.GBP;
import static ee.tenman.investing.domain.Currency.GBX;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.openqa.selenium.By.name;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class GoogleStockPriceService {

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 500))
    public StockPrice fetchPriceFromGoogle(StockSymbol stockSymbol) {
        open("https://www.google.com/");

        $(name("q"))
                .setValue(stockSymbol.ticker())
                .pressEnter();

        String currency = stockSymbol.currency().name();
        BigDecimal price = Optional.of($$(tagName("span"))
                .find(text(currency)).text().split(currency)[0])
                .map(StringUtils::deleteWhitespace)
                .map(this::removeComma)
                .map(BigDecimal::new)
                .orElseThrow(() -> new IllegalStateException(format("Couldn't fetch %s", stockSymbol)))
                .movePointLeft(stockSymbol.currency() == GBX ? 2 : 0);

        log.info("{}: {}", stockSymbol, price);

        closeWindow();

        return StockPrice.builder()
                .currency(stockSymbol.currency() == GBX ? GBP : stockSymbol.currency())
                .price(price)
                .stockSymbol(stockSymbol)
                .build();
    }

    String removeComma(String element) {
        log.info("Removing commas: `{}`", element);
        if (StringUtils.indexOf(element, ".") > StringUtils.indexOf(element, ",")) {
            return replace(replace(element, ",", ""), ".", ".");
        } else {
            return replace(replace(element, ".", ""), ",", ".");
        }
    }
}
