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

import static com.codeborne.selenide.Configuration.browser;
import static com.codeborne.selenide.Configuration.headless;
import static ee.tenman.investing.configuration.ObjectMapperConfiguration.createCamelCaseMapper;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.AUTO;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BDO;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BNB;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BUSD;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.SBDO;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.WBNB;
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

    ObjectMapper objectMapper = createCamelCaseMapper();

    static {
        headless = true;
        browser = "chrome";
    }

    @Test
    void getYieldSummary2() throws IOException {
        JsonNode jsonNode = TestFileUtils.getJson("yieldwatch-response-2.json");
        YieldData yieldData = objectMapper.treeToValue(jsonNode, YieldData.class);
        when(yieldApiService.getYieldData()).thenReturn(yieldData);

        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.amountInPool(BUSD)).isGreaterThan(ZERO);
        assertThat(yieldSummary.amountInPool(BDO)).isGreaterThan(ZERO);
        assertThat(yieldSummary.getYieldEarnedPercentage()).isLessThan(ZERO);
        assertThat(yieldSummary.amountInPool(WBNB)).isGreaterThan(ZERO);
    }

    @Test
    void getYieldSummary3() throws IOException {
        JsonNode jsonNode = TestFileUtils.getJson("yieldwatch-response-3.json");
        YieldData yieldData = objectMapper.treeToValue(jsonNode, YieldData.class);
        when(yieldApiService.getYieldData()).thenReturn(yieldData);

        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.amountInPool(WBNB)).isEqualByComparingTo(BigDecimal.valueOf(2.5348626098479263));
        assertThat(yieldSummary.amountInPool(BUSD)).isEqualByComparingTo(new BigDecimal("721.7863408457527900011628724605745615"));
        assertThat(yieldSummary.amountInPool(BDO)).isEqualByComparingTo(new BigDecimal("571.7390739026846400009295052334499612"));
        assertThat(yieldSummary.amountInPool(SBDO)).isEqualByComparingTo(new BigDecimal("0.058602391657103355"));
        assertThat(yieldSummary.getYieldEarnedPercentage()).isEqualByComparingTo(new BigDecimal("0.00599010"));
    }

    @Test
    void getYieldSummary4() throws IOException {
        JsonNode jsonNode = TestFileUtils.getJson("yieldwatch-response-4.json");
        YieldData yieldData = objectMapper.treeToValue(jsonNode, YieldData.class);
        when(yieldApiService.getYieldData()).thenReturn(yieldData);

        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.amountInPool(WBNB)).isEqualByComparingTo(BigDecimal.valueOf(2.8809212883482200));
        assertThat(yieldSummary.amountInPool(BUSD)).isEqualByComparingTo(new BigDecimal("689.4687459803066400010837083573536946"));
        assertThat(yieldSummary.amountInWallet(BUSD)).isEqualByComparingTo(new BigDecimal("0.04603954012851944"));
        assertThat(yieldSummary.amountInPool(BDO)).isEqualByComparingTo(new BigDecimal("593.5699236463710800010015274691542726"));
        assertThat(yieldSummary.amountInWallet(BDO)).isEqualByComparingTo(new BigDecimal("0.11699094681649985"));
        assertThat(yieldSummary.amountInPool(SBDO)).isEqualByComparingTo(new BigDecimal("0.07738050381761301"));
        assertThat(yieldSummary.getYieldEarnedPercentage()).isEqualByComparingTo(new BigDecimal("0.07001209"));
        assertThat(yieldSummary.getPools())
                .hasSize(4)
                .contains(
                        entry("WBNB-BUSD", new BigDecimal("32.45194077553508")),
                        entry("BDO-BUSD", new BigDecimal("2.1681050115018623")),
                        entry("BDO-WBNB", new BigDecimal("32.14490088855059")),
                        entry("SBDO-BUSD", new BigDecimal("118.81148758159739"))
                );
        assertThat(yieldSummary.amountInPool(BNB)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInWallet(BNB)).isEqualByComparingTo(new BigDecimal("0.05921706753816801"));
        assertThat(yieldSummary.amountInPool(AUTO)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInWallet(AUTO)).isEqualByComparingTo(new BigDecimal("0.000794148871859627"));
    }

    @Test
    void getYieldSummary5() throws IOException {
        JsonNode jsonNode = TestFileUtils.getJson("yieldwatch-response-5.json");
        YieldData yieldData = objectMapper.treeToValue(jsonNode, YieldData.class);

        when(yieldApiService.getYieldData()).thenReturn(yieldData);

        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.amountInPool(WBNB)).isEqualByComparingTo(new BigDecimal("3.18356202494978100021038031952350194"));
        assertThat(yieldSummary.amountInPool(BUSD)).isEqualByComparingTo(new BigDecimal("1783.516077333940000020667150622545380"));
        assertThat(yieldSummary.amountInWallet(BUSD)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInPool(BDO)).isEqualByComparingTo(new BigDecimal("704.0784148452558000332278195937701087"));
        assertThat(yieldSummary.amountInWallet(BDO)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInPool(SBDO)).isEqualByComparingTo(new BigDecimal("0.21284092747825334"));
        assertThat(yieldSummary.getYieldEarnedPercentage()).isEqualByComparingTo(new BigDecimal("0.02223393"));
        assertThat(yieldSummary.getPools())
                .hasSize(4)
                .contains(
                        entry("WBNB-BUSD", new BigDecimal("36.75312894725852")),
                        entry("BDO-BUSD", new BigDecimal("2.2977627499894555")),
                        entry("BDO-WBNB", new BigDecimal("37.87966493363689")),
                        entry("SBDO-BUSD", new BigDecimal("197.6056859028279"))
                );
        assertThat(yieldSummary.amountInPool(BNB)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInWallet(BNB)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInPool(AUTO)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInWallet(AUTO)).isEqualByComparingTo(ZERO);
    }

    @Test
    void getYieldSummary6() throws IOException {
        JsonNode jsonNode = TestFileUtils.getJson("yieldwatch-response-6.json");
        YieldData yieldData = objectMapper.treeToValue(jsonNode, YieldData.class);

        when(yieldApiService.getYieldData()).thenReturn(yieldData);

        YieldSummary yieldSummary = yieldWatchService.getYieldSummary();

        assertThat(yieldSummary.amountInPool(WBNB)).isEqualByComparingTo(new BigDecimal("3.96050537437280034"));
        assertThat(yieldSummary.amountInPool(BUSD)).isEqualByComparingTo(new BigDecimal("1966.42584590076596"));
        assertThat(yieldSummary.amountInWallet(BUSD)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInPool(BDO)).isEqualByComparingTo(new BigDecimal("1398.5464948938497"));
        assertThat(yieldSummary.amountInWallet(BDO)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInPool(SBDO)).isEqualByComparingTo(new BigDecimal("0.0792111145894811"));
        assertThat(yieldSummary.getYieldEarnedPercentage()).isEqualByComparingTo(new BigDecimal("0.03858604"));
        assertThat(yieldSummary.getPools())
                .hasSize(5)
                .contains(
                        entry("WBNB-BUSD", new BigDecimal("36.887810510515685")),
                        entry("WATCH-WBNB", new BigDecimal("34.07683824018572")),
                        entry("BDO-BUSD", new BigDecimal("2.240698780732272")),
                        entry("BDO-WBNB", new BigDecimal("36.92256455792629")),
                        entry("SBDO-BUSD", new BigDecimal("193.627507811701"))
                );
        assertThat(yieldSummary.amountInPool(BNB)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInWallet(BNB)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInPool(AUTO)).isEqualByComparingTo(ZERO);
        assertThat(yieldSummary.amountInWallet(AUTO)).isEqualByComparingTo(ZERO);
    }
}