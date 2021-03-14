package ee.tenman.investing.web.rest;

import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.domain.Portfolio;
import ee.tenman.investing.integration.bscscan.BscScanService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.service.InformingService;
import ee.tenman.investing.service.PriceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
public class TestController {

    @Resource
    private InformingService informingService;

    @Resource
    private BscScanService bscScanService;

    @Resource
    private PriceService priceService;

    @Value("#{'${wallets:}'.split(',')}")
    private List<String> wallets;

    @GetMapping("/portfolios")
    public ResponseEntity<List<Portfolio>> portfolios() {
        Set<Symbol> symbols = new HashSet<>(Arrays.asList(Symbol.values()));

        CompletableFuture<List<Portfolio>> portfolioTotalValuesFuture =
                CompletableFuture.supplyAsync(() -> informingService.getPortfolioTotalValues());
        CompletableFuture<Map<String, Map<Symbol, BigDecimal>>> walletBalancesFuture =
                CompletableFuture.supplyAsync(() -> bscScanService.fetchSymbolBalances(wallets));
        CompletableFuture<Map<Symbol, BigDecimal>> pricesFuture =
                CompletableFuture.supplyAsync(() -> priceService.toEur(symbols));

        List<Portfolio> portfolioTotalValues = portfolioTotalValuesFuture.join();
        Map<String, Map<Symbol, BigDecimal>> walletBalances = walletBalancesFuture.join();
        Map<Symbol, BigDecimal> prices = pricesFuture.join();

        portfolioTotalValues.forEach(portfolioTotalValue ->
                portfolioTotalValue.setTokenBalances(tokenBalances(walletBalances.get(portfolioTotalValue.getWalletAddress()), prices)));

        return ResponseEntity.ok(portfolioTotalValues);
    }

    private Map<Symbol, Map<String, BigDecimal>> tokenBalances(Map<Symbol, BigDecimal> balances, Map<Symbol, BigDecimal> prices) {
        return balances.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> ImmutableMap.of(
                        "amount", e.getValue(),
                        "valueInEur", prices.get(e.getKey()).multiply(e.getValue())
                )));
    }


    @GetMapping("/performance")
    public ResponseEntity<Map<Symbol, String>> performance() {
        Map<Symbol, String> differencesIn24Hours = informingService.getDifferencesIn24Hours();
        return ResponseEntity.ok(differencesIn24Hours);
    }

}
