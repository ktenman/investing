package ee.tenman.investing.service.stats;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

@SpringBootTest
class StatsServiceIntegrationTest {

    @Resource
    StatsService statsService;

    @Test
    void calculate() {
        Map<BigDecimal, Integer> results = new TreeMap<>();
        for (int i = 1; i < 180; i++) {
            BigDecimal calculate = statsService.calculate(i);
            results.put(calculate, i);
        }
        System.out.println(results);
    }
}