package ee.tenman.investing.integration.bscscan;

import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BscScanServiceIntegrationTest {

    @Resource
    BscScanService bscScanService;

    @Test
    @Disabled
    void getBnbBalance() {
        BigDecimal bnbBalance = bscScanService.getBnbBalance();

        assertThat(bnbBalance).isGreaterThan(ZERO);
    }

    @Test
    @Disabled
    void fetchTokenTransferEvents() {
        Map<Symbol, BigDecimal> balances = bscScanService.fetchSymbolBalances();
        ;

        balances.values().forEach(balance -> assertThat(balance).isNotNull());
    }
}