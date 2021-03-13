package ee.tenman.investing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.codeborne.selenide.Configuration.headless;
import static ee.tenman.investing.domain.Currency.EUR;
import static ee.tenman.investing.domain.Currency.GBP;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceTest {

    static {
        headless = true;
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