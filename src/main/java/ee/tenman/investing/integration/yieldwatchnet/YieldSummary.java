package ee.tenman.investing.integration.yieldwatchnet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YieldSummary {
    @Builder.Default
    private BigDecimal bdoAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal sbdoAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal wbnbAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal busdAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal yieldEarnedPercentage = BigDecimal.ZERO;

    private Set<Pool> pools = new HashSet<>();

    public void add(String symbol, BigDecimal amount) {
        switch (symbol) {
            case "BDO":
                bdoAmount = bdoAmount.add(amount);
                break;
            case "SBDO":
                sbdoAmount = sbdoAmount.add(amount);
                break;
            case "BUSD":
                busdAmount = busdAmount.add(amount);
                break;
            case "WBNB":
                wbnbAmount = wbnbAmount.add(amount);
                break;
            default:
                throw new IllegalArgumentException(String.format("Symbol %s not supported", symbol));
        }

    }
}
