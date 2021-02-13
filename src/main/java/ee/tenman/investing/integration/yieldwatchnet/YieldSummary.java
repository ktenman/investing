package ee.tenman.investing.integration.yieldwatchnet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YieldSummary {
    private BigDecimal total;
    private BigDecimal deposit;
    private BigDecimal yieldEarned;
    private BigDecimal bdoAmount;
    private BigDecimal wbnbAmount;
}
