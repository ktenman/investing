
package ee.tenman.investing.integration.yieldwatchnet.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@SuppressWarnings("unused")
public class Result {

    @JsonProperty("Autofarm")
    private Autofarm autofarm;
    @JsonProperty("BeefyFinance")
    private BeefyFinance beefyFinance;
    @JsonProperty("Venus")
    private Venus venus;
    private Currencies currencies;

}
