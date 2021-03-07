package ee.tenman.investing.integration.coinmarketcap.api;

import ee.tenman.investing.integration.coingecko.CoinGeckoService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoinMarketCapApiServiceTest {

    @Resource
    CoinMarketCapApiService coinMarketCapApiService;

    @Resource
    CoinGeckoService coinGeckoService;

    @ParameterizedTest
    @EnumSource(Symbol.class)
    void eur(Symbol symbol) {
        BigDecimal symbolToEurPrice = coinMarketCapApiService.eurPrice(symbol.name());

        assertThat(symbolToEurPrice).isGreaterThan(ZERO);
    }

    @ParameterizedTest
    @EnumSource(Symbol.class)
    @DisplayName("Compare CoinGecko and CoinMarketCap prices. Should not differ more than 2%")
    void eur2(Symbol symbol) {
        BigDecimal symbolToEurPrice = coinMarketCapApiService.eurPrice(symbol.name());
        BigDecimal symbolToEurPriceCoinGecko = coinGeckoService.eurPrice(symbol);

        BigDecimal dividedResult = symbolToEurPrice.divide(symbolToEurPriceCoinGecko, HALF_UP);

        assertThat(dividedResult)
                .isGreaterThan(BigDecimal.valueOf(0.98))
                .isLessThan(BigDecimal.valueOf(1.02));
    }
}