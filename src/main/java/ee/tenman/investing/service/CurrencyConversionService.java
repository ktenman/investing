package ee.tenman.investing.service;

import ee.tenman.investing.domain.Currency;
import ee.tenman.investing.integration.binance.BinanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Optional;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static ee.tenman.investing.domain.Currency.EUR;
import static ee.tenman.investing.domain.Currency.GBP;
import static ee.tenman.investing.domain.Currency.GBX;
import static ee.tenman.investing.domain.Currency.USD;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BUSD;
import static java.lang.String.format;
import static org.openqa.selenium.By.name;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class CurrencyConversionService {
    @Resource
    private BinanceService binanceService;

    public BigDecimal convert(Currency from, Currency to) {

        if (from == to) {
            return BigDecimal.ONE;
        }

        if (from == GBX || to == GBX) {
            throw new IllegalArgumentException("GBX not supported");
        }

        if ((from == GBP || from == USD) && to == EUR) {
            try {
                BigDecimal conversionRate = binanceService.getPriceToEur(from == USD ? BUSD.name() : from.name());
                log.info("From {} to {}: {}", from, to, conversionRate);
                return conversionRate;
            } catch (Exception ignored) {

            }
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
