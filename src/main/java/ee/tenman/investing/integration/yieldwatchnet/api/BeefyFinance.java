
package ee.tenman.investing.integration.yieldwatchnet.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@SuppressWarnings("unused")
public class BeefyFinance {

    private BarnOfTrust barnOfTrust;
    @JsonProperty("LPVaults")
    private LPVaults lPVaults;
    private Staking staking;
    private Vaults vaults;

}
