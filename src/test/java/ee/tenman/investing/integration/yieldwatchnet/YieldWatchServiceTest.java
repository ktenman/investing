package ee.tenman.investing.integration.yieldwatchnet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.tenman.investing.TestFileUtils;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldData;
import ee.tenman.investing.service.SecretsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YieldWatchServiceTest {

    @InjectMocks
    YieldWatchService yieldWatchService;
    @Mock
    SecretsService secretsService;
    @Mock
    YieldApiService yieldApiService;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getYieldSummary() throws IOException {
        JsonNode jsonNode = TestFileUtils.getJson("yieldwatch-response-2.json");
        YieldData yieldData = objectMapper.treeToValue(jsonNode, YieldData.class);
        when(yieldApiService.getYieldData()).thenReturn(yieldData);

        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.getBusdAmount()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getBdoAmount()).isEqualTo(ZERO);
        assertThat(yieldSummary.getYieldEarnedPercentage()).isLessThan(ZERO);
        assertThat(yieldSummary.getWbnbAmount()).isGreaterThan(ZERO);
    }
}