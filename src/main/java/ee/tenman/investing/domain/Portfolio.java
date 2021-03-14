package ee.tenman.investing.domain;

import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

import static ee.tenman.investing.service.InformingService.NUMBER_FORMAT;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Portfolio {
    private String walletAddress;
    private BigDecimal totalValue;
    private BigDecimal totalValueInPools;
    private BigDecimal totalValueInWallet;
    private Map<Symbol, Map<String, BigDecimal>> tokenBalances;

    @Override
    public String toString() {
        if (totalValueInPools == null) {
            return "Portfolio{" +
                    "walletAddress='" + walletAddress + '\'' +
                    ", totalValue=" + totalValue +
                    ", totalValueInPools=" + totalValueInPools +
                    ", totalValueInWallet=" + totalValueInWallet +
                    ", tokenBalances=" + tokenBalances +
                    '}';
        }
        return String.format("%s - %s", walletAddress, NUMBER_FORMAT.format(totalValueInPools));
    }
}
