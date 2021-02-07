package ee.tenman.investing.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

import static com.binance.api.client.domain.market.CandlestickInterval.MONTHLY;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StatsServiceIntegrationTest {

    @Resource
    StatsService statsService;

    @ParameterizedTest
    @ValueSource(strings = {
            "BNB",
            "ADA",
            "ETH",
            "1INCH",
            "DOT",
            "SUSHI",
            "SNX",
            "UNI",
    })
    void prices(String from) {
        Map<String, BigDecimal> prices = statsService.getPrices(from, "EUR", MONTHLY);

        assertThat(prices).isNotEmpty();
    }

    @Test
    void prices() {
        Map<String, BigDecimal> prices = statsService.getPrices("BTC", "ETH", MONTHLY);

        assertThat(prices).isNotEmpty();
    }
}