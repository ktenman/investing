
package ee.tenman.investing.integration.yieldwatchnet.api;

import lombok.Data;

import java.math.BigDecimal;

@Data
@SuppressWarnings("unused")
public class TotalUSDValues {

    private BigDecimal deposit;
    private BigDecimal total;
    private BigDecimal yield;
    private BigDecimal supply;
    private BigDecimal borrow;
    private BigDecimal netAPY;

}
