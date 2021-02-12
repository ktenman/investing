package ee.tenman.investing.integration.yieldwatchnet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class YieldWatchServiceTest {

    @Resource
    YieldWatchService yieldWatchService;

    @Test
    void fetchBnbAmount() {
        BigDecimal yield = yieldWatchService.fetchBnbAmount();

        assertThat(yield).isGreaterThan(ZERO);
    }
}