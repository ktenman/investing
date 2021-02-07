package ee.tenman.investing.service.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @InjectMocks
    StatsService statsService;

    @Test
    void amount() {
        BigDecimal price = new BigDecimal("0.24260000");
        BigDecimal usdt = new BigDecimal("25.0000000");

        BigDecimal amount = statsService.amount(price, usdt);

        assertThat(amount).isEqualByComparingTo(BigDecimal.valueOf(102.97300083));
    }
}