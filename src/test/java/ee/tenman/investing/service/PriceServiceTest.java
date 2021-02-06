package ee.tenman.investing.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static ee.tenman.investing.service.CoinMarketCapService.BINANCE_COIN_ID;
import static ee.tenman.investing.service.CoinMarketCapService.CRO_ID;
import static ee.tenman.investing.service.PriceService.TICKERS_TO_FETCH;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    CoinMarketCapService coinMarketCapService;

    @Mock
    BinanceService binanceService;

    @InjectMocks
    PriceService priceService;

    @Test
    @DisplayName("Don't call coinmarket cap service if all prices fetched from Binance")
    void getPrices() {
        when(binanceService.getPriceToEur(anyString())).thenReturn(BigDecimal.TEN);

        priceService.getPrices(TICKERS_TO_FETCH);

        verify(coinMarketCapService, never()).getPrices(anyList());
    }

    @Test
    @DisplayName("Call coinmarket cap service if some prices missing in Binance")
    void getPrices2() {
        when(binanceService.getPriceToEur(eq(BINANCE_COIN_ID))).thenReturn(BigDecimal.TEN);
        when(binanceService.getPriceToEur(eq(CRO_ID))).thenThrow(NotSupportedSymbolException.class);
        List<String> tickers = Arrays.asList(BINANCE_COIN_ID, CRO_ID);

        priceService.getPrices(tickers);

        verify(coinMarketCapService, times(1)).getPrices(anyList());
    }
}