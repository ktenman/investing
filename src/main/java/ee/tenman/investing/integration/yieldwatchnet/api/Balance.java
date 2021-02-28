
package ee.tenman.investing.integration.yieldwatchnet.api;

import lombok.Data;

import java.math.BigDecimal;

@Data
@SuppressWarnings("unused")
public class Balance {

    private BigDecimal balance;
    private String contractAddress;
    private String name;
    private BigDecimal priceInUSD;
    private String symbol;

}
