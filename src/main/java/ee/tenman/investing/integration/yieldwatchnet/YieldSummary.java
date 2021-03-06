package ee.tenman.investing.integration.yieldwatchnet;

import ee.tenman.investing.integration.yieldwatchnet.api.Balance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YieldSummary {
    @Builder.Default
    private BigDecimal bdoAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal sbdoAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal wbnbAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal busdAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal yieldEarnedPercentage = BigDecimal.ZERO;

    private Map<String, BigDecimal> pools = new TreeMap<>();
    private List<Balance> balances = new ArrayList<>();

    public void add(String symbol, BigDecimal amount) {
        switch (symbol) {
            case "BDO":
                bdoAmount = bdoAmount.add(amount);
                break;
            case "SBDO":
                sbdoAmount = sbdoAmount.add(amount);
                break;
            case "BUSD":
                busdAmount = busdAmount.add(amount);
                break;
            case "WBNB":
            case "BNB":
                wbnbAmount = wbnbAmount.add(amount);
                break;
            default:
                throw new IllegalArgumentException(String.format("Symbol %s not supported", symbol));
        }
    }

    public BigDecimal balanceOf(String symbol) {
        return balances.stream()
                .filter(balance -> StringUtils.equalsIgnoreCase(symbol, balance.getSymbol()))
                .findFirst()
                .map(Balance::getBalance)
                .orElse(BigDecimal.ZERO);
    }
}
