package ee.tenman.investing.integration.cryptocom;

import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class YieldApiServiceIntegrationTest {

    @Resource
    YieldApiService yieldApiService;

    @Test
    @DisplayName("Fetches know instrument's price")
    void getYieldData() {
        YieldData yieldData = yieldApiService.getYieldData();

        System.out.println();
    }

}