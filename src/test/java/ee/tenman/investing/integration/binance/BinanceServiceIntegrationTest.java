package ee.tenman.investing.integration.binance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class BinanceServiceIntegrationTest {

    @Resource
    BinanceService binanceService;

    @Disabled
    @Test
    void buyCrypto() {
        binanceService.buyCrypto();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "BNB",
            "DOT",
            "UNI",
            "BTC",
            "SUSHI",
            "SNX",
            "1INCH",
            "ADA",
            "ETH",
            "BUSD",
            "USDT",
            "USDC",
    })
    void getPrice(String ticker) {
        BigDecimal priceInEuros = binanceService.getPriceToEur(ticker);

        assertThat(priceInEuros).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("CROEUR is not supported in Binance")
    void getPrice() {
        Throwable thrown = catchThrowable(() -> binanceService.getPriceToEur("CRO"));

        assertThat(thrown.getMessage()).isEqualTo("CROEUR not supported");
    }

}