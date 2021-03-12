package ee.tenman.investing.integration.google;

import com.codeborne.selenide.Configuration;
import ee.tenman.investing.domain.StockPrice;
import ee.tenman.investing.domain.StockSymbol;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GoogleStockPriceServiceTest {

    static {
        Configuration.startMaximized = true;
        Configuration.headless = false;
        Configuration.proxyEnabled = false;
        Configuration.screenshots = false;
        Configuration.browser = "firefox";
    }

    @InjectMocks
    GoogleStockPriceService googleStockPriceService;

    @ParameterizedTest
    @EnumSource(StockSymbol.class)
    void fetchPriceFromGoogle(StockSymbol stockSymbol) {
        StockPrice stockPrice = googleStockPriceService.fetchPriceFromGoogle(stockSymbol);

        assertThat(stockPrice.getPrice()).isGreaterThan(BigDecimal.ZERO);

    }
}