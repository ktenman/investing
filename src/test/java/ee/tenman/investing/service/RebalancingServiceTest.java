package ee.tenman.investing.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class RebalancingServiceTest {

    @Resource
    RebalancingService rebalancingService;

    @Test
    @Disabled
    void rebalance() {
        rebalancingService.rebalance();
    }
}