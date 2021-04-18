package ee.tenman.investing.integration.ecb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ECBClientTest {

    @Resource
    ECBClient ecbClient;

    @Test
    void getRates() {
        List<ConversionRate> rates = ecbClient.getRates();

        assertThat(rates).hasSize(32);
    }
}