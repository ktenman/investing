package ee.tenman.investing.integration.coinmarketcap.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoinInformation {

    private TreeMap<String, Price> data;

    BigDecimal getLastEthPrice() {
        return this.getData()
                .lastEntry()
                .getValue()
                .getEthPrices()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No ETH price found"));
    }

    BigDecimal getLastBtcPrice() {
        return this.getData()
                .lastEntry()
                .getValue()
                .getBtcPrices()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No BTC price found"));
    }

    BigDecimal getFirstEurPriceEntry() {
        return this.getData()
                .firstEntry()
                .getValue()
                .getEurPrices()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No EUR price found"));
    }

    BigDecimal getLastEurPriceEntry() {
        return this.getData()
                .lastEntry()
                .getValue()
                .getEurPrices()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No EUR price found"));
    }

    BigDecimal getDifferenceIn24Hours() {
        BigDecimal lastEntry = getLastEurPriceEntry();
        BigDecimal firstEntry = getFirstEurPriceEntry();
        return lastEntry.divide(firstEntry, RoundingMode.HALF_UP);
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
        @JsonProperty("EUR")
        List<BigDecimal> eurPrices = new ArrayList<>();
    }

}
