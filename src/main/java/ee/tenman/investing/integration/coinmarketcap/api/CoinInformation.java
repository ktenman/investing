package ee.tenman.investing.integration.coinmarketcap.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoinInformation {

    private TreeMap<String, Price> data;

    BigDecimal getBtcPrice() {
        return this.getData()
                .lastEntry()
                .getValue()
                .getBtcPrices()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No BTC price found"));
    }

    BigDecimal getEthPrice() {
        return this.getData()
                .lastEntry()
                .getValue()
                .getEthPrices()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No ETH price found"));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Price {
        @JsonProperty("BTC")
        List<BigDecimal> btcPrices = new ArrayList<>();
        @JsonProperty("ETH")
        List<BigDecimal> ethPrices = new ArrayList<>();
    }

}
