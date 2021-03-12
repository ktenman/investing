package ee.tenman.investing.service;

import ee.tenman.investing.domain.StockSymbol;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StockPriceServiceTest {

    @Resource
    StockPriceService stockPriceService;

    @Test
    void priceInEur() {
        List<StockSymbol> stockSymbols = Arrays.asList(StockSymbol.values());

        Map<StockSymbol, BigDecimal> stockSymbolBigDecimalMap = stockPriceService.priceInEur(stockSymbols);

        assertThat(stockSymbolBigDecimalMap)
                .containsKeys(StockSymbol.values());
        for (BigDecimal value : stockSymbolBigDecimalMap.values()) {
            assertThat(value).isGreaterThan(BigDecimal.ZERO);
        }
    }
}