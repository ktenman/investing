package ee.tenman.investing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.codeborne.selenide.Configuration.browser;
import static com.codeborne.selenide.Configuration.headless;
import static com.codeborne.selenide.Configuration.proxyEnabled;
import static com.codeborne.selenide.Configuration.screenshots;
import static com.codeborne.selenide.Configuration.startMaximized;
import static ee.tenman.investing.integration.borsefrankfurt.Currency.EUR;
import static ee.tenman.investing.integration.borsefrankfurt.Currency.GBP;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceTest {

    static {
        startMaximized = true;
        headless = false;
        proxyEnabled = false;
        screenshots = false;
        browser = "firefox";
    }

    @InjectMocks
    CurrencyConversionService currencyConversionService;

    @Test
    void convert() {
        BigDecimal gbpToEur = currencyConversionService.convert(GBP, EUR);
        BigDecimal eurToGbp = currencyConversionService.convert(EUR, GBP);

        System.out.println();
    }
}