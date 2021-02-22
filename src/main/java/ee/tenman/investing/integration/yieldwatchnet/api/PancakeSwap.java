
package ee.tenman.investing.integration.yieldwatchnet.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@SuppressWarnings("unused")
public class PancakeSwap {

    @JsonProperty("LPStaking")
    private LPVaults lPVaults;
    private Staking staking;

}
