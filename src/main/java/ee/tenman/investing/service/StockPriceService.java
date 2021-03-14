package ee.tenman.investing.service;

import ee.tenman.investing.domain.Currency;
import ee.tenman.investing.domain.StockPrice;
import ee.tenman.investing.domain.StockSymbol;
import ee.tenman.investing.integration.google.GoogleStockPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
public class StockPriceService {

    @Resource
    private GoogleStockPriceService googleStockPriceService;

    @Resource
    private CurrencyConversionService currencyConversionService;

    public Map<StockSymbol, BigDecimal> priceInEur(List<StockSymbol> stockSymbols) {
        Map<Currency, BigDecimal> conversionRates = currencyConversionService.getConversionRatesToEur(stockSymbols);
        return stockSymbols
                .parallelStream()
                .map(googleStockPriceService::fetchPriceFromGoogle)
                .collect(toMap(StockPrice::getStockSymbol, s ->
                        conversionRates.get(s.getCurrency()).multiply(s.getPrice()))
                );
    }

}
