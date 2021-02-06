package ee.tenman.investing.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static ee.tenman.investing.configuration.FetchingConfiguration.BINANCE_COIN_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.CRO_ID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoinMarketCapServiceTest {

    @Resource
    CoinMarketCapService coinMarketCapService;

    @Resource
    BinanceService binanceService;

    @Test
    void getPrice() {
        List<String> tickers = Arrays.asList(BINANCE_COIN_ID, CRO_ID);
        BigDecimal busd = binanceService.getPriceToEur("BUSD");
        Map<String, BigDecimal> prices = coinMarketCapService.getPricesInEur(tickers, busd);

        tickers.forEach(t -> assertThat(prices.keySet()).contains(t));
        assertThat(prices.values()).hasSize(2);
        prices.values().forEach(p -> {
            assertThat(p).isNotZero();
            assertThat(p).isNotNull();
        });
    }

}