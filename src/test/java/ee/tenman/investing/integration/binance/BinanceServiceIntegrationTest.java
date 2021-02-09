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

import static com.binance.api.client.domain.market.CandlestickInterval.DAILY;
import static com.binance.api.client.domain.market.CandlestickInterval.FIFTEEN_MINUTES;
import static com.binance.api.client.domain.market.CandlestickInterval.FIVE_MINUTES;
import static com.binance.api.client.domain.market.CandlestickInterval.HALF_HOURLY;
import static com.binance.api.client.domain.market.CandlestickInterval.HOURLY;
import static com.binance.api.client.domain.market.CandlestickInterval.ONE_MINUTE;
import static com.binance.api.client.domain.market.CandlestickInterval.THREE_MINUTES;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class BinanceServiceIntegrationTest {

    @Resource
    BinanceService binanceService;

    @Disabled
    @Test
    void buyCrypto() {
//        binanceService.buyCrypto();
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
        int minutes = (int) MINUTES.between(LocalDateTime.parse("2018-04-17T04:59:59.99"), now());
        return Stream.of(
//                Arguments.of("ETHBTC", HOURLY, 31210),
                Arguments.of("ADAUSDT", HOURLY, 24618),
                Arguments.of("ADAUSDT", HALF_HOURLY, 49236),
                Arguments.of("ADAUSDT", FIFTEEN_MINUTES, 97465),
                Arguments.of("ADAUSDT", FIVE_MINUTES, 295041),
                Arguments.of("ADAUSDT", THREE_MINUTES, 487399),
                Arguments.of("ADAUSDT", ONE_MINUTE, 22222),
//                Arguments.of("BNBUSDT", HOURLY, minutes),
//                Arguments.of("ETHUSDT", HOURLY, minutes),
//                Arguments.of("BTCUSDT", HOURLY, minutes),
//                Arguments.of("ETHBTC", MONTHLY, 44),
//                Arguments.of("ETHBTC", WEEKLY, 187),
//                Arguments.of("ETHBTC", DAILY, 1305),
//                Arguments.of("BNBUSDT", DAILY, 1028),
//                Arguments.of("ETHBTC", ONE_MINUTE, 1872600),
//                Arguments.of("BTCUSDT", DAILY, 1028),
//                Arguments.of("ETHUSDT", DAILY, 1028),
                Arguments.of("ADABTC", DAILY, 1166)
        );
    }

    @ParameterizedTest
    @MethodSource("frequencyProvider")
    @DisplayName("Get prices with limit")
    void getPrice2(String fromTo, CandlestickInterval candlestickInterval, int limit) {
        Map<LocalDateTime, BigDecimal> prices = binanceService.getPrices(fromTo, candlestickInterval, limit);

        assertThat(prices).hasSize(limit);
    }

    @Test
    @Disabled
    void rebalance() {
        binanceService.rebalance();
    }

}