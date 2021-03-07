package ee.tenman.investing.integration.yieldwatchnet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.tenman.investing.TestFileUtils;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import javax.annotation.Resource;
import java.io.IOException;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@DirtiesContext
class YieldWatchServiceIntegrationTest {

    @Resource
    YieldWatchService yieldWatchService;

    @MockBean
    YieldApiService yieldApiService;

    @Resource
    ObjectMapper objectMapper;

    @Test
    @Disabled
    void fetchEarnedYield() {
        YieldSummary yieldSummary = yieldWatchService.fetchYieldSummary();

        assertThat(yieldSummary.amountInPool(Symbol.BDO)).isGreaterThan(ZERO);
        assertThat(yieldSummary.getYieldEarnedPercentage()).isGreaterThan(ZERO);
        assertThat(yieldSummary.amountInPool(Symbol.WBNB)).isGreaterThan(ZERO);
    }

    @Test
    void getYieldSummary() throws IOException {
        JsonNode jsonNode = TestFileUtils.getJson("yieldwatch-response-2.json");
        YieldData yieldData = objectMapper.treeToValue(jsonNode, YieldData.class);
        when(yieldApiService.getYieldData()).thenReturn(yieldData);

        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.amountInPool(Symbol.BUSD)).isGreaterThan(ZERO);
        assertThat(yieldSummary.amountInPool(Symbol.BDO)).isGreaterThan(ZERO);
        assertThat(yieldSummary.getYieldEarnedPercentage()).isLessThan(ZERO);
        assertThat(yieldSummary.amountInPool(Symbol.WBNB)).isGreaterThan(ZERO);
    }
}