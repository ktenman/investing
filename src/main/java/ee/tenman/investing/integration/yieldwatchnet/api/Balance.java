
package ee.tenman.investing.integration.yieldwatchnet.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Data
@SuppressWarnings("unused")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Balance {

    private BigDecimal balance;
    private String contractAddress;
    private String name;
    private BigDecimal priceInUSD;
    private String symbol;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Balance balance = (Balance) o;
        return Objects.equals(symbol, balance.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }
}
