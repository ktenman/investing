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
import java.util.Map;
import java.util.stream.Stream;

import static com.binance.api.client.domain.market.CandlestickInterval.DAILY;
import static com.binance.api.client.domain.market.CandlestickInterval.MONTHLY;
import static com.binance.api.client.domain.market.CandlestickInterval.WEEKLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class BinanceServiceIntegrationTest {

    @Resource
    BinanceService binanceService;

    @Disabled
    @Test
    void buyCrypto() {
        binanceService.buyCrypto();
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
        return Stream.of(
//                Arguments.of(HOURLY, 31210),
//                Arguments.of(ONE_MINUTE, 1872600),
                Arguments.of(MONTHLY, 44),
                Arguments.of(WEEKLY, 187),
                Arguments.of(DAILY, 1305)
        );
    }

    @ParameterizedTest
    @MethodSource("frequencyProvider")
    @DisplayName("Get prices with limit")
    void getPrice2(CandlestickInterval candlestickInterval, int limit) {
        Map<String, BigDecimal> prices = binanceService.getPrices("ETHBTC", candlestickInterval, limit);

        assertThat(prices).hasSize(limit);
    }
}