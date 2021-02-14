
package ee.tenman.investing.integration.yieldwatchnet.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@SuppressWarnings("unused")
public class Autofarm {

    @JsonProperty("LPVaults")
    private LPVaults lPVaults;
    private Vaults vaults;

}
