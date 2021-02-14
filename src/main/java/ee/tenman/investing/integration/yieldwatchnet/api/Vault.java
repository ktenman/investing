
package ee.tenman.investing.integration.yieldwatchnet.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@SuppressWarnings("unused")
public class Vault {

    private BigDecimal apy;
    private String contractAddress;
    private BigDecimal currentTokens;
    private String depositToken;
    private BigDecimal depositedTokens;
    private BigDecimal harvestedRewards;
    private long id;
    private String inputToken;
    @JsonProperty("LPInfo")
    private LPInfo lPInfo;
    private String name;
    private BigDecimal pendingRewards;
    private String platform;
    private BigDecimal priceInUSD;
    private BigDecimal priceInUSDDepositToken;
    private BigDecimal priceInUSDRewardToken;
    private String rewardToken;
    private BigDecimal totalRewards;
    private String totalTokensStaked;
    private String type;

}
