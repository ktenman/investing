package ee.tenman.investing.integration.coingecko;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    @ValueSource(strings = {"bdollar", "wbnb"})
    void eur(String symbol) {
        BigDecimal symbolToEurPrice = coinGeckoService.eur(symbol);

        assertThat(symbolToEurPrice).isGreaterThan(ZERO);
    }
}