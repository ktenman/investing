package ee.tenman.investing.integration.yieldwatchnet;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@TestPropertySource(properties = "yieldwatch.url=https://yieldwatch.net/api/")
class YieldWatchServiceE2ETest {

    @Resource
    YieldWatchService yieldWatchService;

    @Test
    void getYieldSummary() {
        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.getBusdAmount()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getBdoAmount()).isEqualTo(ZERO);
        assertThat(yieldSummary.getYieldEarnedPercentage()).isLessThan(ZERO);
        assertThat(yieldSummary.getWbnbAmount()).isGreaterThan(ZERO);
    }
}