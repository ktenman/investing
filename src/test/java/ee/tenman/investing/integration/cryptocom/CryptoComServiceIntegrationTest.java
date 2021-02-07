package ee.tenman.investing.integration.cryptocom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class CryptoComServiceIntegrationTest {

    @Resource
    CryptoComService cryptoComService;

    @Test
    @DisplayName("Fetches know instrument's price")
    void getInstrument() {
        BigDecimal croBtcPrice = cryptoComService.getInstrumentPrice("CRO", "BTC");

        assertThat(croBtcPrice).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Doesn't fetch unknown instrument")
    void getInstrument2() {
        Throwable thrown = catchThrowable(() -> cryptoComService.getInstrumentPrice("BTC", "CRO"));

        assertThat(thrown).hasMessage("BTC_CRO not supported");
    }

}