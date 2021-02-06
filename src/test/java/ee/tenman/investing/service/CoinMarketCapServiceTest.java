package ee.tenman.investing.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
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
        List<String> tickers = Arrays.asList(BINANCE_COIN_ID, CRO_ID);
        Map<String, BigDecimal> prices = coinMarketCapService.getPrices(tickers);

        tickers.forEach(t -> assertThat(prices.keySet()).contains(t));
        assertThat(prices.values()).hasSize(2);
        prices.values().forEach(p -> {
            assertThat(p).isNotZero();
            assertThat(p).isNotNull();
        });
    }

}