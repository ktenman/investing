package ee.tenman.investing.integration.yieldwatchnet;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
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
    void fetchEarnedYield() {
        YieldSummary yieldSummary = yieldWatchService.fetchYieldSummary();

        assertThat(yieldSummary.getBdoAmount()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getDeposit()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getTotal()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getWbnbAmount()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getYieldEarned()).isGreaterThan(ZERO);
    }

}