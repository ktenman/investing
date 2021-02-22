package ee.tenman.investing.integration.binance;

import com.binance.api.client.domain.market.CandlestickInterval;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Stream;

import static com.binance.api.client.domain.market.CandlestickInterval.HOURLY;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class BinanceServiceIntegrationTest {

    @Resource
    BinanceService binanceService;

    @Disabled
    @Test
    void buyCrypto() {
        binanceService.buyCrypto2();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "BNB",
            "DOT",
            "UNI",
            "BTC",
            "SUSHI",
            "SNX",
            "1INCH",
            "ADA",
            "ETH",
            "BUSD",
            "USDT",
            "USDC",
    })
    void getPrice(String ticker) {
        BigDecimal priceInEuros = binanceService.getPriceToEur(ticker);

        assertThat(priceInEuros).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("CROEUR is not supported in Binance")
    void getPrice() {
        Throwable thrown = catchThrowable(() -> binanceService.getPriceToEur("CRO"));

        assertThat(thrown.getMessage()).isEqualTo("CROEUR not supported");
    }

    private static Stream<Arguments> frequencyProvider() {
        int hours = (int) HOURS.between(LocalDateTime.parse("2021-02-10T04:57:36.099"), now()) + 23587;
        return Stream.of(
                Arguments.of("BNBUSDT", HOURLY, hours),
                Arguments.of("BTCUSDT", HOURLY, hours)
        );
    }

    @ParameterizedTest
    @MethodSource("frequencyProvider")
    @DisplayName("Get prices with limit")
    @Disabled
    void getPrice2(String fromTo, CandlestickInterval candlestickInterval, int limit) {
        Map<LocalDateTime, BigDecimal> prices = binanceService.getPrices(fromTo, candlestickInterval, limit);

        assertThat(prices).hasSize(limit);
    }

}