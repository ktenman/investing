package ee.tenman.investing.service;

import ee.tenman.investing.domain.Currency;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static com.codeborne.selenide.Configuration.headless;
import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CurrencyConversionServiceTest {

    @Resource
    CurrencyConversionService currencyConversionService;

    @ParameterizedTest
    @CsvSource({
            "GBP, EUR",
            "USD, EUR"
    })
    void convert(Currency from, Currency to) {
        BigDecimal currencyConversionRate = currencyConversionService.convert(from, to);

        assertThat(currencyConversionRate).isGreaterThan(ZERO);
    }
}
