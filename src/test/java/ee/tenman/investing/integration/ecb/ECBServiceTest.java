package ee.tenman.investing.integration.ecb;

import com.google.common.collect.ImmutableList;
import ee.tenman.investing.domain.Currency;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static ee.tenman.investing.domain.Currency.EUR;
import static ee.tenman.investing.domain.Currency.GBP;
import static ee.tenman.investing.domain.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ECBServiceTest {

    @Mock
    ECBClient ecbClient;

    @InjectMocks
    ECBService ecbService;

    private static Stream<Arguments> dateProvider() {
        return Stream.of(
                Arguments.of(EUR, EUR, BigDecimal.ONE),
                Arguments.of(GBP, EUR, "1.1521666494"),
                Arguments.of(EUR, GBP, "0.8679300000"),
                Arguments.of(GBP, USD, "1.3809869460"),
                Arguments.of(USD, USD, BigDecimal.ONE)
        );
    }

    @ParameterizedTest
    @MethodSource("dateProvider")
    void convert(Currency from, Currency to, BigDecimal expected) {
        lenient().when(ecbClient.getRates()).thenReturn(ImmutableList.of(
                ConversionRate.builder()
                        .currency("USD")
                        .rate(BigDecimal.valueOf(1.1986))
                        .build(),
                ConversionRate.builder()
                        .currency("GBP")
                        .rate(BigDecimal.valueOf(0.86793))
                        .build()
        ));
        BigDecimal result = ecbService.convert(from, to);

        assertThat(result).isEqualByComparingTo(expected);
    }
}