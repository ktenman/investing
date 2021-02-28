package ee.tenman.investing.integration.yieldwatchnet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Pool {
    private String poolName;
    private BigDecimal value;
}
