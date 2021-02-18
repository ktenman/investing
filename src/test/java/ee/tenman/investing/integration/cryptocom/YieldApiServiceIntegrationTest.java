package ee.tenman.investing.integration.cryptocom;

import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldData;
import ee.tenman.investing.service.SecretsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.annotation.Resource;

import static org.mockito.Mockito.when;

@SpringBootTest
class YieldApiServiceIntegrationTest {

    @Resource
    YieldApiService yieldApiService;

    @MockBean
    SecretsService secretsService;

    @Test
    @DisplayName("Fetches know instrument's price")
    void getYieldData() {
        when(secretsService.getWalletAddress()).thenReturn("0x085243DbCe451b0ceB020571B176b37819a90969");

        YieldData yieldData = yieldApiService.getYieldData();

        System.out.println();
    }

}