package ee.tenman.investing.integration.coinmarketcap.api;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoinInformation {

    private TreeMap<String, Map<String, List<BigDecimal>>> data;

    BigDecimal getLastPriceOf(String symbol) {
        return this.getData()
                .lastEntry()
                .getValue()
                .getOrDefault(symbol, ImmutableList.of())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("No %s price found", symbol)));
    }

    BigDecimal getFirsPriceOf(String symbol) {
        return this.getData()
                .firstEntry()
                .getValue()
                .getOrDefault(symbol, ImmutableList.of())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("No %s price found", symbol)));
    }

    BigDecimal getDifferenceIn24Hours() {
        String eur = "EUR";
        BigDecimal lastEntry = getLastPriceOf(eur);
        BigDecimal firstEntry = getFirsPriceOf(eur);
        return lastEntry.divide(firstEntry, RoundingMode.HALF_UP);
    }

}
