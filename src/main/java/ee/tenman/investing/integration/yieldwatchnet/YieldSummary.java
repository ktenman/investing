package ee.tenman.investing.integration.yieldwatchnet;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

import static java.math.BigDecimal.ZERO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YieldSummary {
    @Builder.Default
    @JsonIgnore
    private BigDecimal yieldEarnedPercentage = ZERO;
    @JsonIgnore
    private Map<String, BigDecimal> pools = new TreeMap<>();
    @JsonIgnore
    private Set<Balance> poolBalances = new HashSet<>();
    @JsonIgnore
    private Set<Balance> walletBalances = new HashSet<>();

    private BigDecimal total = ZERO;

    public void addToPoolAmounts(String symbol, BigDecimal amount) {
        Optional<Balance> optionalBalance = poolBalances.stream()
                .filter(balance -> StringUtils.equalsIgnoreCase(symbol, balance.getSymbol()))
                .findFirst();

        BigDecimal newBalance = optionalBalance
                .map(Balance::getBalance)
                .orElse(ZERO)
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
                .orElse(ZERO);
    }

    public BigDecimal amountInWallet(Symbol symbol) {
        return walletBalances.stream()
                .filter(balance -> StringUtils.equalsIgnoreCase(symbol.name(), balance.getSymbol()))
                .findFirst()
                .map(Balance::getBalance)
                .orElse(ZERO);
    }

    public BigDecimal getTotal(Map<Symbol, BigDecimal> prices) {
        return prices.entrySet().stream()
                .map(e -> e.getValue().multiply(this.amountInPool(e.getKey())))
                .reduce(ZERO, BigDecimal::add);
    }
}
