package ee.tenman.investing.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Asset {
    private String symbol;
    private BigDecimal eurPrice;
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal lockedAmount = BigDecimal.ZERO;

    public BigDecimal getAvailableAmount() {
        return totalAmount.subtract(lockedAmount);
    }
}
