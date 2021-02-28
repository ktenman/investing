
package ee.tenman.investing.integration.yieldwatchnet.api;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@SuppressWarnings("unused")
public class WalletBalance {

    private List<Balance> balances;
    private BigDecimal totalUSDValue;

}
