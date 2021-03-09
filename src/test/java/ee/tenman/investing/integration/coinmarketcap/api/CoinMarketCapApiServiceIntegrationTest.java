package ee.tenman.investing.integration.coinmarketcap.api;

import ee.tenman.investing.integration.coingecko.CoinGeckoService;
import ee.tenman.investing.integration.coinmarketcap.CoinMarketCapService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.service.PriceService;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoinMarketCapApiServiceIntegrationTest {

    @Resource
    CoinMarketCapApiService coinMarketCapApiService;

    @Resource
    CoinMarketCapService coinMarketCapService;

    @Resource
    CoinGeckoService coinGeckoService;

    @Resource
    PriceService priceService;

    @ParameterizedTest
    @EnumSource(Symbol.class)
    void eur(Symbol symbol) {
        BigDecimal symbolToEurPrice = coinMarketCapApiService.eurPrice(symbol.name());

        assertThat(symbolToEurPrice).isGreaterThan(ZERO);
    }

    @ParameterizedTest
    @EnumSource(Symbol.class)
    @DisplayName("Compare CoinGecko and CoinMarketCap prices. Should not differ more than 3%")
    void eur2(Symbol symbol) {
        BigDecimal priceToEur = priceService.toEur(symbol);
        BigDecimal symbolToEurPrice = coinMarketCapService.eurPrice(symbol);
        BigDecimal symbolToEurPriceApi = coinMarketCapApiService.eurPrice(symbol.name());
        BigDecimal symbolToEurPriceCoinGecko = coinGeckoService.eurPrice(symbol);

        List<BigDecimal> prices = asList(priceToEur, symbolToEurPrice, symbolToEurPriceApi, symbolToEurPriceCoinGecko);

        for (BigDecimal priceA : prices) {
            for (BigDecimal priceB : prices) {
                if (ComparableUtils.is(priceB).equalTo(ZERO)) {
                    continue;
                }

                BigDecimal difference = priceA.divide(priceB, HALF_UP);

                assertThat(difference).isBetween(BigDecimal.valueOf(0.97), BigDecimal.valueOf(1.03));
            }
        }
    }
}