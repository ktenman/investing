package ee.tenman.investing.integration.yieldwatchnet;

import ee.tenman.investing.integration.yieldwatchnet.api.Balance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YieldSummary {
    @Builder.Default
    private BigDecimal yieldEarnedPercentage = BigDecimal.ZERO;

    private Map<String, BigDecimal> pools = new TreeMap<>();
    private Set<Balance> amounts = new HashSet<>();

    public void add(String symbol, BigDecimal amount) {
        Optional<Balance> optionalBalance = amounts.stream()
                .filter(balance -> StringUtils.equalsIgnoreCase(symbol, balance.getSymbol()))
                .findFirst();

        BigDecimal newBalance = optionalBalance
                .map(Balance::getBalance)
                .orElse(BigDecimal.ZERO)
                .add(amount);

        Balance balance = optionalBalance.orElseGet(() -> Balance.builder().symbol(symbol).build());
        balance.setBalance(newBalance);

        amounts.add(balance);
    }

    public BigDecimal amountOf(Symbol symbol) {
        return amounts.stream()
                .filter(balance -> StringUtils.equalsIgnoreCase(symbol.name(), balance.getSymbol()))
                .findFirst()
                .map(Balance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

}
