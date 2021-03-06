
package ee.tenman.investing.integration.yieldwatchnet.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@SuppressWarnings("unused")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Balance {

    private BigDecimal balance;
    private String contractAddress;
    private String name;
    private BigDecimal priceInUSD;
    private String symbol;

}
