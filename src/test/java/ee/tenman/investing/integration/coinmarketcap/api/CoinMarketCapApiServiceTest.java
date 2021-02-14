package ee.tenman.investing.integration.coinmarketcap.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CoinMarketCapApiServiceTest {

    @Resource
    CoinMarketCapApiService coinMarketCapApiService;

    @ValueSource(strings = {"BDO", "bdollar", "wbnb", "WBNB"})
    @ParameterizedTest
    void getPrice(String currency) {
        BigDecimal eurPrice = coinMarketCapApiService.eurPrice(currency);

        assertThat(eurPrice).isGreaterThan(ZERO);
    }
}