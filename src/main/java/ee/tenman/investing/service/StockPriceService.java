package ee.tenman.investing.service;

import ee.tenman.investing.domain.StockPrice;
import ee.tenman.investing.integration.borsefrankfurt.Currency;
import ee.tenman.investing.integration.borsefrankfurt.StockSymbol;
import ee.tenman.investing.integration.google.GoogleStockPriceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ee.tenman.investing.integration.borsefrankfurt.Currency.EUR;
import static ee.tenman.investing.integration.borsefrankfurt.Currency.GBX;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
public class StockPriceService {

    @Resource
    GoogleStockPriceService googleStockPriceService;

    @Resource
    CurrencyConversionService currencyConversionService;

    public Map<StockSymbol, BigDecimal> priceInEur(List<StockSymbol> stockSymbols) {
        Set<Currency> possibleCurrencies = stockSymbols.stream()
                .map(StockSymbol::currency)
                .filter(c -> c != GBX)
                .collect(toSet());

        Map<Currency, BigDecimal> currenciesInEur = possibleCurrencies.stream()
                .collect(toMap(identity(), c -> currencyConversionService.convert(c, EUR)));

        List<StockPrice> collect = stockSymbols.stream()
                .parallel()
                .map(googleStockPriceService::fetchPriceFromGoogle)
                .collect(toList());
        return collect.stream()
                .collect(toMap(StockPrice::getStockSymbol, s ->
                        currenciesInEur.get(s.getCurrency()).multiply(s.getPrice()))
                );
    }

}
