package ee.tenman.investing.integration.autofarm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class AutofarmServiceTest {

    @Resource
    AutofarmService autoFarmService;

    @Test
    void fetchBnbAmount() {
        autoFarmService.fetchDailyYieldReturn();
    }
}