
package ee.tenman.investing.integration.yieldwatchnet.api;

import lombok.Data;

import java.math.BigDecimal;

@Data
@SuppressWarnings("unused")
public class CompactSession {

    private BigDecimal amountLPToken;
    private BigDecimal amountToken0;
    private long amountToken1;
    private String blockNumber;
    private String hash;
    private long timeIndex;
    private String timeStamp;
    private String token0InUSD;
    private BigDecimal token1InUSD;
    private String type;

}
