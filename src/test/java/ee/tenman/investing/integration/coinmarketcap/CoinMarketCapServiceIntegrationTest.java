package ee.tenman.investing.integration.coinmarketcap;

import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static ee.tenman.investing.configuration.FetchingConfiguration.BINANCE_COIN_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.CRO_ID;
import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoinMarketCapServiceIntegrationTest {

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

    @Test
    @Disabled
    void yieldwatchNet() {
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

    @ParameterizedTest
    @EnumSource(value = Symbol.class, mode = EnumSource.Mode.INCLUDE, names = {"ADA", "BNB"})
    void eurPrice(Symbol symbol) {
        BigDecimal eurPrice = coinMarketCapService.eurPrice(symbol);

        assertThat(eurPrice).isGreaterThan(ZERO);
    }

    @ParameterizedTest
    @EnumSource(Symbol.class)
    @Disabled
    void eurPrice2(Symbol symbol) {
        BigDecimal eurPrice = coinMarketCapService.eurPrice(symbol);

        assertThat(eurPrice).isGreaterThan(ZERO);
    }

}