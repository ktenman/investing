
package ee.tenman.investing.integration.yieldwatchnet.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@SuppressWarnings("unused")
public class LPInfo {

    private BigDecimal actInToken1;
    private BigDecimal actPrice;
    private BigDecimal changeToken0;
    private BigDecimal changeToken1;
    private String contractAddress;
    private BigDecimal currentBaseToken0;
    private BigDecimal currentBaseToken1;
    private BigDecimal currentToken0;
    private BigDecimal currentToken1;
    private BigDecimal depositPrice;
    private BigDecimal depositToken0;
    private BigDecimal depositToken1;
    private BigDecimal faktorIL;
    private BigDecimal feesEarnedInToken0;
    private BigDecimal feesEarnedInToken1;
    private BigDecimal hodlInToken1;
    @JsonProperty("ILInToken1")
    private BigDecimal iLInToken1;
    @JsonProperty("LPEarningsInToken1")
    private BigDecimal lPEarningsInToken1;
    private BigDecimal noOfLPTokens;
    private BigDecimal poolToken0;
    private BigDecimal poolToken1;
    private BigDecimal priceInUSDLPToken;
    private BigDecimal priceInUSDToken0;
    private BigDecimal priceInUSDToken1;
    private String symbolToken0;
    private String symbolToken1;
    private BigDecimal totalSupplyLPToken;
    private BigDecimal winningsToken0;
    private BigDecimal winningsToken1;
    private BigDecimal feesEarnedInPerc;
    @JsonProperty("ILInPerc")
    private BigDecimal iLInPerc;
    @JsonProperty("LPEarningsInPerc")
    private BigDecimal lPEarningsInPerc;

}
