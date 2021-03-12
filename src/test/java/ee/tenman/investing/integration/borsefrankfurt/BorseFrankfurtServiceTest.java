package ee.tenman.investing.integration.borsefrankfurt;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BorseFrankfurtServiceTest {

    @InjectMocks
    BorseFrankfurtService borseFrankfurtService;

    @ParameterizedTest
    @EnumSource(StockSymbol.class)
    void fetchPrice(StockSymbol stockSymbol) {
        if (stockSymbol.exchange() != Exchange.FRA) {
            return;
        }

        BigDecimal price = borseFrankfurtService.fetchPrice(stockSymbol);

        assertThat(price).isGreaterThan(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @EnumSource(StockSymbol.class)
    void fetchPrice2(StockSymbol stockSymbol) {
        BigDecimal price = borseFrankfurtService.fetchPriceFromGoogle(stockSymbol);

        assertThat(price).isGreaterThan(BigDecimal.ZERO);
    }
}