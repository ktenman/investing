
package ee.tenman.investing.cryptocom;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;

@lombok.Data
@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Data {

    @JsonProperty("a")
    private BigDecimal latestTradePrice;
    @JsonProperty("b")
    private BigDecimal bestBidPrice;
    @JsonProperty("c")
    private BigDecimal twentyFourHourPriceChange;
    @JsonProperty("h")
    private BigDecimal highestTradePrice;
    @JsonProperty("i")
    private String instrumentName;
    @JsonProperty("k")
    private BigDecimal bestAskPrice;
    @JsonProperty("l")
    private BigDecimal lowestTradePrice;
    private Instant timestampOfData;
    @JsonProperty("v")
    private BigDecimal totalTwentyFourHourTradeVolume;

    @JsonProperty("t")
    public void setTimestampOfData(long millis) {
        this.timestampOfData = Instant.ofEpochMilli(millis);
    }
}
