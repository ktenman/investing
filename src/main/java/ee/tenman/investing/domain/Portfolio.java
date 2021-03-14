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
    private Map<Symbol, BigDecimal> tokenBalances;

    @Override
    public String toString() {
        return String.format("%s - %s", walletAddress, NUMBER_FORMAT.format(totalValue));
    }
}
