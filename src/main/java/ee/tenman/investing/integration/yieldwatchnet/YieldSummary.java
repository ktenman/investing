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
    @Builder.Default
    private BigDecimal bdoAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal wbnbAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal busdAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal yieldEarnedPercentage = BigDecimal.ZERO;
}
