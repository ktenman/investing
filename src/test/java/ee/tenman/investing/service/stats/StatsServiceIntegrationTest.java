package ee.tenman.investing.service.stats;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class StatsServiceIntegrationTest {

    @Resource
    StatsService statsService;

    @Test
    void calculate() {
        statsService.calculate();
    }
}