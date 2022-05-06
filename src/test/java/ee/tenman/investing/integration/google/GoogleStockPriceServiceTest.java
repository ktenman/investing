package ee.tenman.investing.integration.google;

import ee.tenman.investing.domain.StockPrice;
import ee.tenman.investing.domain.StockSymbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static com.codeborne.selenide.Configuration.browser;
import static com.codeborne.selenide.Configuration.headless;
import static ee.tenman.investing.domain.StockSymbol.LON_HEAL;
import static ee.tenman.investing.domain.StockSymbol.LON_IGSG;
import static ee.tenman.investing.domain.StockSymbol.LON_IITU;
import static ee.tenman.investing.domain.StockSymbol.LON_RBOT;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GoogleStockPriceServiceTest {

    static {
        headless = true;
        browser = "chrome";
    }

    @InjectMocks
    GoogleStockPriceService googleStockPriceService;

    @ParameterizedTest
    @EnumSource(StockSymbol.class)
    void fetchPriceFromGoogle(StockSymbol stockSymbol) {
        StockPrice stockPrice = googleStockPriceService.fetchPriceFromGoogle(stockSymbol);

        assertThat(stockPrice.getPrice()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void fetchPriceFromGoogle() {
        StockPrice stockPrice = googleStockPriceService.fetchPriceFromGoogle(LON_IGSG);

        assertThat(stockPrice.getPrice()).isGreaterThan(BigDecimal.ZERO);
    }

}
