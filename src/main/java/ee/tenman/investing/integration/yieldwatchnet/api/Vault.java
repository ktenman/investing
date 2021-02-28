
package ee.tenman.investing.integration.yieldwatchnet.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@SuppressWarnings("unused")
public class Vault {
    private BigDecimal apy;
    private BigDecimal borrowAmount;
    private BigDecimal borrowAPY;
    private BigDecimal currentTokens;
    private BigDecimal depositAmount;
    private BigDecimal depositedTokens;
    private BigDecimal harvestedRewards;
    private BigDecimal pendingRewards;
    private BigDecimal priceInUSD;
    private BigDecimal priceInUSDDepositToken;
    private BigDecimal priceInUSDRewardToken;
    private BigDecimal supplyAPY;
    private BigDecimal totalRewards;
    private long id;
    @JsonProperty("LPInfo")
    private LPInfo lPInfo;
    private String collateralFactor;
    private String contractAddress;
    private String depositToken;
    private String inputToken;
    private String name;
    private String platform;
    private String rewardToken;
    private String type;
    private String totalTokensStaked;
    private String depositTokenAddress;

}
