package ee.tenman.investing.web.rest;

import ee.tenman.investing.domain.Portfolio;
import ee.tenman.investing.integration.bscscan.BscScanService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.service.InformingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class TestController {

    @Resource
    private InformingService informingService;

    @Resource
    private BscScanService bscScanService;

    @Value("#{'${wallets:}'.split(',')}")
    private List<String> wallets;

    @GetMapping("/portfolios")
    public ResponseEntity<List<Portfolio>> portfolios() {
        CompletableFuture<List<Portfolio>> portfolioTotalValuesFuture =
                CompletableFuture.supplyAsync(() -> informingService.getPortfolioTotalValues());
        CompletableFuture<Map<String, Map<Symbol, BigDecimal>>> walletBalancesFuture =
                CompletableFuture.supplyAsync(() -> bscScanService.fetchSymbolBalances(wallets));

        List<Portfolio> portfolioTotalValues = portfolioTotalValuesFuture.join();
        Map<String, Map<Symbol, BigDecimal>> walletBalances = walletBalancesFuture.join();

        portfolioTotalValues.forEach(portfolioTotalValue -> portfolioTotalValue.setTokenBalances(
                walletBalances.get(portfolioTotalValue.getWalletAddress())
        ));

        return ResponseEntity.ok(portfolioTotalValues);
    }

    @GetMapping("/performance")
    public ResponseEntity<Map<Symbol, String>> performance() {
        Map<Symbol, String> differencesIn24Hours = informingService.getDifferencesIn24Hours();
        return ResponseEntity.ok(differencesIn24Hours);
    }

}
