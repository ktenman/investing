package ee.tenman.investing.domain;

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
public class Portfolio {
    private String wallet;
    private BigDecimal totalValue;

    @Override
    public String toString() {
        return String.format("%s - %s", wallet, NUMBER_FORMAT.format(totalValue));
    }
}
