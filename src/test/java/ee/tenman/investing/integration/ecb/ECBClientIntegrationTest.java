package ee.tenman.investing.integration.ecb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
class ECBClientIntegrationTest {

    @Resource
    private ECBClient ecbClient;

    @Test
    void getDailyRates() {
        List<ConversionRate> dailyRates = ecbClient.getDailyRates();

        System.out.println();
    }
}