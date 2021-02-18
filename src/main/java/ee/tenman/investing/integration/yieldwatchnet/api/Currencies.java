
package ee.tenman.investing.integration.yieldwatchnet.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@SuppressWarnings("unused")
public class Currencies {
    @JsonProperty("BTCB")
    private BigDecimal btcb;
    @JsonProperty("EUR")
    private BigDecimal eur;
    @JsonProperty("GBP")
    private BigDecimal gbp;
    @JsonProperty("JPY")
    private BigDecimal jpy;
    @JsonProperty("RMB")
    private BigDecimal rmb;
    @JsonProperty("WBNB")
    private BigDecimal wbnb;
}
