package ee.tenman.investing.integration.coinmarketcap.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.TreeMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoinInformation {

    private TreeMap<String, Price> data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Price {
        @JsonProperty("BTC")
        List<BigDecimal> btcPrices;
        @JsonProperty("ETH")
        List<BigDecimal> ethPrices;
    }

}
