package ee.tenman.investing.integration.yieldwatchnet;

import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.annotation.Resource;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class YieldWatchServiceTest {

    @Resource
    YieldWatchService yieldWatchService;

    @MockBean
    YieldApiService yieldApiService;

    @Test
    void fetchEarnedYield() {
        YieldSummary yieldSummary = yieldWatchService.fetchYieldSummary();

        assertThat(yieldSummary.getBdoAmount()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getYieldEarnedPercentage()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getWbnbAmount()).isGreaterThan(ZERO);
    }

    @Test
    void getYieldSummary() {
        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.getBdoAmount()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getYieldEarnedPercentage()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getWbnbAmount()).isGreaterThan(ZERO);

        System.out.println();
    }
}