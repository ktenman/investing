package ee.tenman.investing.service;

import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

import static com.binance.api.client.domain.market.CandlestickInterval.DAILY;
import static com.binance.api.client.domain.market.CandlestickInterval.MONTHLY;
import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PriceServiceIntegrationTest {

    @Resource
    PriceService priceService;

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
    @Disabled
    void prices2() {
        Map<String, BigDecimal> prices = priceService.getPrices("BTC", "ETH", DAILY);

        assertThat(prices).isNotEmpty();
    }

    @ParameterizedTest
    @EnumSource(Symbol.class)
    void toEur(Symbol symbol) {
        BigDecimal symbolToEurPrice = priceService.toEur(symbol);

        assertThat(symbolToEurPrice).isGreaterThan(ZERO);
    }

}