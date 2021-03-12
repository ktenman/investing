package ee.tenman.investing.service;

import ee.tenman.investing.domain.Portfolio;
import ee.tenman.investing.integration.slack.SlackMessage;
import ee.tenman.investing.integration.slack.SlackService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import ee.tenman.investing.integration.yieldwatchnet.YieldSummary;
import ee.tenman.investing.integration.yieldwatchnet.YieldWatchService;
import ee.tenman.investing.integration.yieldwatchnet.api.Balance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BDO;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BNB;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.BTC;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.EGG;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.SBDO;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.WATCH;
import static ee.tenman.investing.integration.yieldwatchnet.Symbol.WBNB;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
public class InformingService {

    public static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.00 '€'");

    @Resource
    private SlackService slackService;
    @Resource
    private YieldWatchService yieldWatchService;
    @Resource
    private PriceService priceService;
    @Value("#{'${wallets:}'.split(',')}")
    private List<String> wallets;

    @Scheduled(cron = "0 0 8/12 * * *")
    public void informAboutPortfolios() {
        if (wallets.stream().noneMatch(StringUtils::isNotBlank)) {
            log.info("Skipping. No wallets were provided");
            return;
        }

        List<Portfolio> portfolios = getPortfolioTotalValues();

        String message = portfolios.stream()
                .map(Portfolio::toString)
                .collect(joining("\n"));

        postToSlack(message);
    }

    public List<Portfolio> getPortfolioTotalValues() {
        Map<String, YieldSummary> yieldSummaries = yieldWatchService.getYieldSummary(wallets);

        Set<Balance> allUniqueBalances = yieldSummaries.values().stream()
                .map(YieldSummary::getPoolBalances)
                .flatMap(Collection::stream)
                .collect(toSet());

        Map<Symbol, BigDecimal> prices = priceService.getPricesOfBalances(allUniqueBalances);

        return yieldSummaries.entrySet().stream()
                .map(entry -> Portfolio.builder()
                        .wallet(entry.getKey())
                        .totalValue(entry.getValue().getTotal(prices))
                        .build())
                .collect(toList());
    }

    @Scheduled(cron = "0 0 8/12 * * *")
    public void informAboutPerformance() {

        Map<Symbol, String> differences = getDifferencesIn24Hours();

        String messagePayload = differences.entrySet().stream()
                .map(entry -> String.format("%-5s %-5s", entry.getKey(), entry.getValue()))
                .collect(joining("\n"));

        postToSlack(messagePayload);
    }

    public Map<Symbol, String> getDifferencesIn24Hours() {
        DecimalFormat decimalFormat = new DecimalFormat("#0.00'%'");
        decimalFormat.setPositivePrefix("+");

        List<Symbol> symbols = asList(WBNB, EGG, BDO, SBDO, WATCH, BTC, BNB);

        Map<Symbol, BigDecimal> differences = priceService.to24HDifference(symbols);

        return symbols.stream().collect(toMap(
                identity(),
                s -> decimalFormat.format(differences.get(s)),
                (a, b) -> a,
                TreeMap::new
        ));
    }

    private void postToSlack(String message) {
        log.info("{}", message);

        slackService.post(new SlackMessage("```" + message + "```"));
    }

}
