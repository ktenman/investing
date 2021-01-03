package ee.tenman.investing.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

import static ee.tenman.investing.service.CoinMarketCapService.BINANCE_COIN_ID;
import static ee.tenman.investing.service.CoinMarketCapService.CRO_ID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoinMarketCapServiceTest {

    @Resource
    CoinMarketCapService coinMarketCapService;

    @Test
    void getPrice() {
        Map<String, BigDecimal> prices = coinMarketCapService.getPrices(BINANCE_COIN_ID, CRO_ID);

        assertThat(prices.keySet()).contains(BINANCE_COIN_ID, CRO_ID);
        assertThat(prices.values()).hasSize(2);
        prices.values().forEach(p -> {
            assertThat(p).isNotZero();
            assertThat(p).isNotNull();
        });
    }
}