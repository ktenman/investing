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

    private static final String EUR_CURRENCY = "EUR";
    private TreeMap<String, Map<String, List<BigDecimal>>> data;

    BigDecimal getLastPrice() {
        return getLastPriceOf(EUR_CURRENCY);
    }

    BigDecimal getFirstPrice() {
        return getFirsPriceOf(EUR_CURRENCY);
    }

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
        BigDecimal lastEntry = getLastPriceOf(EUR_CURRENCY);
        BigDecimal firstEntry = getFirsPriceOf(EUR_CURRENCY);
        return lastEntry.divide(firstEntry, RoundingMode.HALF_UP);
    }

}
