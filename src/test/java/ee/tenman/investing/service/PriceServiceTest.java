package ee.tenman.investing.service;

import ee.tenman.investing.exception.NotSupportedSymbolException;
import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.coinmarketcap.CoinMarketCapService;
import ee.tenman.investing.integration.cryptocom.CryptoComService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static ee.tenman.investing.configuration.FetchingConfiguration.BINANCE_COIN_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.BITCOIN_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.CRO_ID;
import static ee.tenman.investing.configuration.FetchingConfiguration.TICKER_SYMBOL_MAP;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    CryptoComService cryptoComService;

    @InjectMocks
    PriceService priceService;

    @Test
    @DisplayName("Don't call coinmarket cap service if all prices fetched from Binance")
    void getPrices() {
        when(binanceService.getPriceToEur(anyString())).thenReturn(BigDecimal.TEN);

        priceService.getPrices(TICKER_SYMBOL_MAP.keySet());

        verify(coinMarketCapService, never()).getPricesInEur(anyList(), any());
    }

    @Test
    @DisplayName("Call crypto com service if some prices missing in Binance")
    void getPrices2() {
        when(binanceService.getPriceToEur(eq("BNB"))).thenReturn(BigDecimal.TEN);
        when(binanceService.getPriceToEur(eq("BTC"))).thenReturn(BigDecimal.ONE);
        when(binanceService.getPriceToEur(eq("CRO"))).thenThrow(NotSupportedSymbolException.class);
        when(binanceService.getPriceToEur(eq("BUSD"))).thenReturn(BigDecimal.ONE);
        List<String> tickers = Arrays.asList(BINANCE_COIN_ID, CRO_ID, BITCOIN_ID);

        priceService.getPrices(tickers);

        verify(cryptoComService, times(1)).getInstrumentPrice(eq("CRO"), eq("BTC"));
    }
}