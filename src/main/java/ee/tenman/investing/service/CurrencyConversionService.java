package ee.tenman.investing.service;

import ee.tenman.investing.domain.Currency;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static ee.tenman.investing.domain.Currency.GBX;
import static java.lang.String.format;
import static org.openqa.selenium.By.name;
import static org.openqa.selenium.By.tagName;

@Service
public class CurrencyConversionService {

    public BigDecimal convert(Currency from, Currency to) {

        if (from == GBX || to == GBX) {
            throw new IllegalArgumentException("GBX not supported");
        }

        open("https://www.google.com/");

        String query = String.format("10000000 %s = %s", from.name(), to.name());

        $(name("q"))
                .setValue(query)
                .pressEnter();

        String currency = to.value();

        return Optional.of($$(tagName("span"))
                .find(text(currency))
                .closest("div")
                .text()
                .split(currency)[0])
                .map(StringUtils::trim)
                .map(element -> StringUtils.replace(element, ",", ""))
                .map(BigDecimal::new)
                .orElseThrow(() -> new IllegalStateException(format("Couldn't fetch %s -> %s", from, to)))
                .movePointLeft(7);
    }


}
