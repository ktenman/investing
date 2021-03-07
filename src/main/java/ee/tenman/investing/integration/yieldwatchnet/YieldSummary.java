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
    private Set<Balance> poolBalances = new HashSet<>();
    private Set<Balance> walletBalances = new HashSet<>();

    public void addToPoolAmounts(String symbol, BigDecimal amount) {
        Optional<Balance> optionalBalance = poolBalances.stream()
                .filter(balance -> StringUtils.equalsIgnoreCase(symbol, balance.getSymbol()))
                .findFirst();

        BigDecimal newBalance = optionalBalance
                .map(Balance::getBalance)
                .orElse(BigDecimal.ZERO)
                .add(amount);

        Balance balance = optionalBalance.orElseGet(() -> Balance.builder().symbol(symbol).build());
        balance.setBalance(newBalance);

        poolBalances.add(balance);
    }

    public BigDecimal amountInPool(Symbol symbol) {
        return poolBalances.stream()
                .filter(balance -> StringUtils.equalsIgnoreCase(symbol.name(), balance.getSymbol()))
                .findFirst()
                .map(Balance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal amountInWallet(Symbol symbol) {
        return walletBalances.stream()
                .filter(balance -> StringUtils.equalsIgnoreCase(symbol.name(), balance.getSymbol()))
                .findFirst()
                .map(Balance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

}
