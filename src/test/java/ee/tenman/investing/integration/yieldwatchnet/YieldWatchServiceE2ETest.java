package ee.tenman.investing.integration.yieldwatchnet;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
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
    @Disabled
    void getYieldSummary() {
        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.amountOf(Symbol.BUSD)).isGreaterThan(ZERO);
        assertThat(yieldSummary.amountOf(Symbol.BDO)).isGreaterThan(ZERO);
        assertThat(yieldSummary.getYieldEarnedPercentage()).isGreaterThan(ZERO);
        assertThat(yieldSummary.amountOf(Symbol.WBNB)).isGreaterThan(ZERO);
    }
}