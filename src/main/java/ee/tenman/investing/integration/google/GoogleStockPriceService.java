package ee.tenman.investing.integration.google;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.investing.domain.StockPrice;
import ee.tenman.investing.domain.StockSymbol;
import ee.tenman.investing.service.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static ee.tenman.investing.domain.Currency.GBP;
import static ee.tenman.investing.domain.Currency.GBX;
import static java.lang.String.format;
import static org.openqa.selenium.By.name;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class GoogleStockPriceService {

    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 500))
    public StockPrice fetchPriceFromGoogle(StockSymbol stockSymbol) {
        open("https://www.google.com/");

        ElementsCollection buttons = $$(tagName("button"));
        SelenideElement noustun = buttons.find(text("nõustun"));
        if (noustun.exists()) {
            noustun.click();
        } else {
            SelenideElement iAgree = buttons.find(text("I agree"));
            if (iAgree.exists()) {
                iAgree.click();
            }
        }

        $(name("q"))
                .setValue(stockSymbol.name() + " stock")
                .pressEnter();

        String currency = stockSymbol.currency().name();
        BigDecimal price = $$(tagName("span"))
                .filter(text(currency)).texts().stream()
                .map(s -> Stream.of(s.split(currency)).findFirst())
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .map(StringUtils::deleteWhitespace)
                .map(TextUtils::removeCommas)
                .map(this::toDecimal)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(format("Couldn't fetch %s", stockSymbol)))
                .movePointLeft(stockSymbol.currency() == GBX ? 2 : 0);

        log.info("{}: {}", stockSymbol, price);

        return StockPrice.builder()
                .currency(stockSymbol.currency() == GBX ? GBP : stockSymbol.currency())
                .price(price)
                .stockSymbol(stockSymbol)
                .build();
    }

    private Optional<BigDecimal> toDecimal(String s) {
        try {
            return Optional.of(new BigDecimal(s));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

}
