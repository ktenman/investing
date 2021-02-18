package ee.tenman.investing.integration.cryptocom;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldData;
import ee.tenman.investing.service.SecretsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = "yieldwatch.url=https://yieldwatch.net/api/")
class YieldApiServiceIntegrationTest {

    @MockBean
    SecretsService secretsService;

    @Resource
    YieldApiService yieldApiService;

    @Test
    @DisplayName("Fetches know instrument's price")
    void getYieldData() {
        when(secretsService.getWalletAddress()).thenReturn("0x37413c688A3F03C919C5Ae83f4fb373fbABC5515");

        YieldData yieldData = yieldApiService.getYieldData();

        assertThat(yieldData).isNotNull();
    }

}