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
import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
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
    void getYieldSummary2() throws IOException {
        JsonNode jsonNode = TestFileUtils.getJson("yieldwatch-response-2.json");
        YieldData yieldData = objectMapper.treeToValue(jsonNode, YieldData.class);
        when(yieldApiService.getYieldData()).thenReturn(yieldData);

        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.getBusdAmount()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getBdoAmount()).isGreaterThan(ZERO);
        assertThat(yieldSummary.getYieldEarnedPercentage()).isLessThan(ZERO);
        assertThat(yieldSummary.getWbnbAmount()).isGreaterThan(ZERO);
    }

    @Test
    void getYieldSummary3() throws IOException {
        JsonNode jsonNode = TestFileUtils.getJson("yieldwatch-response-3.json");
        YieldData yieldData = objectMapper.treeToValue(jsonNode, YieldData.class);
        when(yieldApiService.getYieldData()).thenReturn(yieldData);

        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.getWbnbAmount()).isEqualByComparingTo(BigDecimal.valueOf(2.5348626098479263));
        assertThat(yieldSummary.getBusdAmount()).isEqualByComparingTo(new BigDecimal("721.7863408457527900011628724605745615"));
        assertThat(yieldSummary.getBdoAmount()).isEqualByComparingTo(new BigDecimal("571.7390739026846400009295052334499612"));
        assertThat(yieldSummary.getSbdoAmount()).isEqualByComparingTo(new BigDecimal("0.058602391657103355"));
        assertThat(yieldSummary.getYieldEarnedPercentage()).isEqualByComparingTo(new BigDecimal("0.00599010"));
    }

    @Test
    void getYieldSummary4() throws IOException {
        JsonNode jsonNode = TestFileUtils.getJson("yieldwatch-response-4.json");
        YieldData yieldData = objectMapper.treeToValue(jsonNode, YieldData.class);
        when(yieldApiService.getYieldData()).thenReturn(yieldData);

        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.getWbnbAmount()).isEqualByComparingTo(BigDecimal.valueOf(2.8809212883482200));
        assertThat(yieldSummary.getBusdAmount()).isEqualByComparingTo(new BigDecimal("689.4687459803066400010837083573536946"));
        assertThat(yieldSummary.getBdoAmount()).isEqualByComparingTo(new BigDecimal("593.5699236463710800010015274691542726"));
        assertThat(yieldSummary.getSbdoAmount()).isEqualByComparingTo(new BigDecimal("0.07738050381761301"));
        assertThat(yieldSummary.getYieldEarnedPercentage()).isEqualByComparingTo(new BigDecimal("0.07001209"));
        assertThat(yieldSummary.getPools())
                .hasSize(4)
                .contains(
                        entry("WBNB-BUSD Pool", new BigDecimal("32.45194077553508")),
                        entry("BDO-BUSD Pool", new BigDecimal("2.1681050115018623")),
                        entry("BDO-WBNB Pool", new BigDecimal("32.14490088855059")),
                        entry("SBDO-BUSD Pool", new BigDecimal("118.81148758159739"))
                );
    }

}