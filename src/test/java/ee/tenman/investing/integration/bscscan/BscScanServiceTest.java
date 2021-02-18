package ee.tenman.investing.integration.bscscan;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BscScanServiceTest {

    @Resource
    BscScanService bscScanService;

    @Test
    @Disabled
    void getBnbBalance() {
        BigDecimal bnbBalance = bscScanService.getBnbBalance();

        assertThat(bnbBalance).isGreaterThan(ZERO);
    }
}