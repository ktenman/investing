package ee.tenman.investing.integration.cryptocom;

import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "yieldwatch.url=https://yieldwatch.net/api/")
class YieldApiServiceIntegrationTest {

    @Resource
    YieldApiService yieldApiService;

    @Test
    @Disabled
    @DisplayName("Fetches know instrument's price")
    void getYieldData() {
        YieldData yieldData = yieldApiService.getYieldData();

        assertThat(yieldData).isNotNull();
    }

}