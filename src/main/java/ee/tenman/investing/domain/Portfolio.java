package ee.tenman.investing.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.TreeMap;

import static ee.tenman.investing.service.InformingService.NUMBER_FORMAT;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Portfolio {
    private String walletAddress;
    private BigDecimal totalValue;
    private BigDecimal totalValueInPools;
    private BigDecimal totalValueInWallet;
    private TreeMap<Symbol, Token> tokenBalances;

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
