package ee.tenman.investing.domain;

import ee.tenman.investing.integration.borsefrankfurt.Currency;
import ee.tenman.investing.integration.borsefrankfurt.StockSymbol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockPrice {
    private StockSymbol stockSymbol;
    private Currency currency;
    private BigDecimal price;
}
