package ee.tenman.investing.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static ee.tenman.investing.service.InformingService.NUMBER_FORMAT;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Portfolio {
    private String walletAddress;
    private BigDecimal totalValue;
    private Wallet pools;
    private Wallet wallet;

    @Override
    public String toString() {
        if (pools == null || pools.getTotalValue() == null) {
            return "Portfolio{" +
                    "walletAddress='" + walletAddress + '\'' +
                    ", totalValue=" + totalValue +
                    ", pools=" + pools +
                    ", wallet=" + wallet +
                    '}';
        }
        return String.format("%s - %s", walletAddress, NUMBER_FORMAT.format(pools.getTotalValue()));
    }
}
