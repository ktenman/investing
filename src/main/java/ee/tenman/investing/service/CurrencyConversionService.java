package ee.tenman.investing.service;

import ee.tenman.investing.domain.Currency;
import ee.tenman.investing.domain.StockSymbol;
import ee.tenman.investing.integration.binance.BinanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
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

        String foundedText = $$(tagName("span"))
                .find(text(currency))
                .closest("div")
                .text();

        log.info("Found text {}", foundedText);

        String splittedText = foundedText.toUpperCase().split(currency.toUpperCase())[0];

        log.info("Splitted text {}", splittedText);

        BigDecimal conversionRate = Optional.of(splittedText)
                .map(StringUtils::deleteWhitespace)
                .map(TextUtils::removeCommas)
                .map(BigDecimal::new)
                .orElseThrow(() -> new IllegalStateException(format("Couldn't fetch %s -> %s", from, to)))
                .movePointLeft(7);

        log.info("{} -> {}: {}", from, to, conversionRate);

        return conversionRate;
    }

    public Map<Currency, BigDecimal> getConversionRatesToEur(List<StockSymbol> stockSymbols) {
        return getConversionRates(stockSymbols, EUR);
    }

    public Map<Currency, BigDecimal> getConversionRates(List<StockSymbol> stockSymbols, Currency currencyTo) {
        Set<Currency> possibleCurrencies = stockSymbols.stream()
                .map(StockSymbol::currency)
                .filter(c -> c != GBX)
                .collect(toSet());
        Map<Currency, BigDecimal> conversionRates = possibleCurrencies.stream()
                .collect(toMap(identity(), c -> convert(c, currencyTo)));
        log.info("Currencies to EUR {}", conversionRates);
        return conversionRates;
    }
}
