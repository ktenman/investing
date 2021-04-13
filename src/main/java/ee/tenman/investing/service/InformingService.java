package ee.tenman.investing.service;

import ee.tenman.investing.domain.Portfolio;
import ee.tenman.investing.domain.PortfoliosResponse;
import ee.tenman.investing.domain.Stats;
import ee.tenman.investing.domain.Token;
import ee.tenman.investing.domain.Wallet;
import ee.tenman.investing.integration.bscscan.BalanceService;
import ee.tenman.investing.integration.slack.SlackMessage;
import ee.tenman.investing.integration.slack.SlackService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.integration.yieldwatchnet.YieldSummary;
import ee.tenman.investing.integration.yieldwatchnet.YieldWatchService;
import ee.tenman.investing.integration.yieldwatchnet.api.Balance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import static java.math.BigDecimal.ZERO;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
public class InformingService {

    public static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.00 '€'");
    private static final Set<Symbol> ALL_POSSIBLE_SYMBOLS = new HashSet<>(Arrays.asList(Symbol.values()));

    @Resource
    private SlackService slackService;
    @Resource
    private YieldWatchService yieldWatchService;
    @Resource
    private PriceService priceService;
    @Value("#{'${wallets:}'.split(',')}")
    private List<String> wallets;
    @Resource
    private BalanceService balanceService;

    private PortfoliosResponse portfoliosResponse;
    private Map<Symbol, Stats> differencesIn24Hours;

    @Scheduled(cron = "0 0 8/12 * * *")
    public void informAboutPortfolios() {
        if (wallets.stream().noneMatch(StringUtils::isNotBlank)) {
            log.info("Skipping. No wallets were provided");
            return;
        }

        List<Portfolio> portfolios = getSimplePortfolioTotalValues();

        String message = portfolios.stream()
                .map(Portfolio::toString)
                .collect(joining("\n"));

        postToSlack(message);
    }

    public List<Portfolio> getSimplePortfolioTotalValues() {

        Map<String, YieldSummary> yieldSummaries = yieldWatchService.getYieldSummary(wallets.toArray(new String[0]));

        Set<Balance> allUniqueBalances = yieldSummaries.values()
                .stream()
                .map(YieldSummary::getPoolBalances)
                .flatMap(Collection::stream)
                .collect(toSet());

        Map<Symbol, BigDecimal> prices = priceService.getPricesOfBalances(allUniqueBalances);

        List<Portfolio> portfolios = yieldSummaries.entrySet().stream()
                .map(entry -> {
                    Wallet pools = Wallet.builder().totalValue(entry.getValue().getTotal(prices)).build();
                    return Portfolio.builder()
                            .walletAddress(entry.getKey())
                            .pools(pools)
                            .build();
                })
                .collect(toList());

        log.info("{}", portfolios);

        return portfolios;
    }

    @Scheduled(fixedDelay = 1_200_000, initialDelay = 120_000)
    public void setPortfoliosResponse() {
        this.portfoliosResponse = buildPortfoliosResponse(wallets.toArray(new String[0]));
    }

    public PortfoliosResponse getPortfolioTotalValues() {
        if (portfoliosResponse == null) {
            setPortfoliosResponse();
        }
        return portfoliosResponse;
    }

    private Wallet buildPoolWallet(YieldSummary yieldSummary, Map<Symbol, BigDecimal> prices) {
        TreeMap<Symbol, Token> tokenBalances = yieldSummary.getPoolBalances()
                .stream()
                .filter(entry -> {
                    BigDecimal value = prices.get(Symbol.valueOf(entry.getSymbol().toUpperCase())).multiply(entry.getBalance());
                    return ComparableUtils.is(value).greaterThan(BigDecimal.valueOf(0.001));
                })
                .collect(toMap(
                        balance -> Symbol.valueOf(balance.getSymbol().toUpperCase()),
                        balance -> Token.builder()
                                .amount(balance.getBalance())
                                .valueInEur(prices.get(Symbol.valueOf(balance.getSymbol().toUpperCase())).multiply(balance.getBalance()))
                                .build(),
                        (a, b) -> a,
                        TreeMap::new
                ));
        return Wallet.builder()
                .tokenBalances(tokenBalances)
                .totalValue(totalValueInPools(tokenBalances))
                .build();
    }

    private BigDecimal totalValueInPools(Map<Symbol, Token> tokenBalances) {
        return tokenBalances.values().stream()
                .map(Token::getValueInEur)
                .reduce(ZERO, BigDecimal::add);
    }

    private TreeMap<Symbol, Token> tokenBalances(Map<Symbol, BigDecimal> balances, Map<Symbol, BigDecimal> prices) {
        return balances.entrySet()
                .stream()
                .filter(entry -> {
                    BigDecimal value = prices.get(entry.getKey()).multiply(entry.getValue());
                    return ComparableUtils.is(value).greaterThan(BigDecimal.valueOf(0.001));
                })
                .collect(toMap(
                        Map.Entry::getKey,
                        entry -> Token.builder()
                                .amount(entry.getValue())
                                .valueInEur(prices.get(entry.getKey()).multiply(entry.getValue()))
                                .build(),
                        (a, b) -> a,
                        TreeMap::new
                ));
    }

    @Scheduled(cron = "0 0 8/12 * * *")
    public void informAboutPerformance() {
        Map<Symbol, Stats> differences = getDifferencesIn24Hours();

        String messagePayload = differences.entrySet().stream()
                .map(entry -> String.format("%-5s %-5s", entry.getKey(), entry.getValue()))
                .collect(joining("\n"));

        postToSlack(messagePayload);
    }

    public Map<Symbol, Stats> getDifferencesIn24Hours() {
        if (differencesIn24Hours == null) {
            setDifferencesIn24Hours();
        }
        return differencesIn24Hours;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 0)
    public void setDifferencesIn24Hours() {
        this.differencesIn24Hours = priceService.to24HDifference(asList(Symbol.values()));
    }

    private void postToSlack(String message) {
        log.info("{}", message);

        slackService.post(new SlackMessage("```" + message + "```"));
    }

    public PortfoliosResponse getPortfolioTotalValues(String... walletAddresses) {
        if (walletAddresses == null || walletAddresses.length == 0) {
            return getPortfolioTotalValues();
        }
        return buildPortfoliosResponse(walletAddresses);
    }

    private PortfoliosResponse buildPortfoliosResponse(String... walletAddresses) {
        CompletableFuture<Map<String, YieldSummary>> yieldSummariesFuture = CompletableFuture.supplyAsync(
                () -> yieldWatchService.getYieldSummary(walletAddresses));
        CompletableFuture<Map<String, Map<Symbol, BigDecimal>>> walletBalancesFuture = CompletableFuture.supplyAsync(
                () -> balanceService.fetchSymbolBalances(walletAddresses));
        CompletableFuture<Map<Symbol, BigDecimal>> pricesFuture = CompletableFuture.supplyAsync(
                () -> priceService.toEur(ALL_POSSIBLE_SYMBOLS));

        Map<String, Map<Symbol, BigDecimal>> walletBalances = walletBalancesFuture.join();
        Map<Symbol, BigDecimal> prices = pricesFuture.join();
        Map<String, YieldSummary> yieldSummaries = yieldSummariesFuture.join();

        List<Portfolio> portfolios = yieldSummaries.entrySet()
                .stream()
                .map(entry -> Portfolio.builder()
                        .walletAddress(entry.getKey())
                        .pools(buildPoolWallet(entry.getValue(), prices))
                        .build())
                .collect(toList());

        portfolios.forEach(portfolio -> {
            TreeMap<Symbol, Token> tokenBalances = tokenBalances(walletBalances.get(portfolio.getWalletAddress()), prices);
            BigDecimal totalValueInPools = totalValueInPools(tokenBalances);
            Wallet wallet = Wallet.builder()
                    .tokenBalances(tokenBalances)
                    .totalValue(totalValueInPools)
                    .build();
            portfolio.setWallet(wallet);
            portfolio.setTotalValue(portfolio.getWallet().getTotalValue().add(portfolio.getPools().getTotalValue()));
        });

        return PortfoliosResponse.builder()
                .portfolios(portfolios)
                .build();
    }
}
