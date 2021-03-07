package ee.tenman.investing.integration.coingecko;

import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoinGeckoServiceTest {

    @Resource
    CoinGeckoService coinGeckoService;

    @ParameterizedTest
    @EnumSource(Symbol.class)
    void eur(Symbol symbol) {
        BigDecimal symbolToEurPrice = coinGeckoService.eurPrice(symbol);

        assertThat(symbolToEurPrice).isGreaterThan(ZERO);
    }
}