package ee.tenman.investing.integration.yieldwatchnet;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class YieldWatchServiceTest {

    @Resource
    YieldWatchService yieldWatchService;

    @Test
    void fetchBnbAmount() {
        yieldWatchService.setBnbAmount();

        assertThat(yieldWatchService.getBnbAmount()).isGreaterThan(ZERO);
    }


    @Test
    void fetchEarnedYield() throws InterruptedException {
        ImmutableMap<String, BigDecimal> earnedYield = yieldWatchService.fetchEarnedYield();

        for (Map.Entry<String, BigDecimal> entry : earnedYield.entrySet()) {
            assertThat(entry.getValue()).isGreaterThan(ZERO);
        }
    }

}