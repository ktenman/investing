package ee.tenman.investing.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

import static com.binance.api.client.domain.market.CandlestickInterval.DAILY;
import static com.binance.api.client.domain.market.CandlestickInterval.MONTHLY;
import static ee.tenman.investing.configuration.FetchingConfiguration.TICKER_SYMBOL_MAP;
import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PriceServiceIntegrationTest {

    @Resource
    PriceService priceService;

    @Test
    void getPrices() {
        Map<String, BigDecimal> prices = priceService.getPrices(TICKER_SYMBOL_MAP.keySet());

        assertThat(prices).hasSize(TICKER_SYMBOL_MAP.size());
        TICKER_SYMBOL_MAP.keySet().forEach(ticker -> assertThat(prices.keySet()).contains(ticker));
        prices.values().forEach(price -> assertThat(price).isGreaterThan(ZERO));
    }

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
        Map<String, BigDecimal> prices = priceService.getPrices(from, "EUR", MONTHLY);

        assertThat(prices).isNotEmpty();
    }

    @Test
    void prices() {
        Map<String, BigDecimal> prices = priceService.getPrices("BTC", "ETH", MONTHLY);

        assertThat(prices).isNotEmpty();
    }

    @Test
    @DisplayName("Get last 1000 days")
    void prices2() {
        Map<String, BigDecimal> prices = priceService.getPrices("BTC", "ETH", DAILY);

        assertThat(prices).isNotEmpty();
    }
}