package ee.tenman.investing.service;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
public class InformingService {

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#.00 '€'");

    @Resource
    private SlackService slackService;
    @Resource
    private YieldWatchService yieldWatchService;
    @Resource
    private PriceService priceService;
    @Value("#{'${wallets:}'.split(',')}")
    private List<String> wallets;

    @Scheduled(cron = "0 0 0/8 * * *")
    public void informAboutPortfolios() {
        if (wallets.stream().noneMatch(StringUtils::isNotBlank)) {
            log.info("Skipping. No wallets were provided");
            return;
        }

        Map<String, YieldSummary> yieldSummaries = yieldWatchService.getYieldSummary(wallets);

        Set<Balance> allUniqueBalances = yieldSummaries.values().stream()
                .map(YieldSummary::getPoolBalances)
                .flatMap(Collection::stream)
                .collect(toSet());

        Map<Symbol, BigDecimal> prices = priceService.getPricesOfBalances(allUniqueBalances);

        StringBuilder stringBuilder = new StringBuilder();

        yieldSummaries.forEach((key, value) -> {
            BigDecimal total = value.getTotal(prices);
            stringBuilder.append(String.format("%s - %s", key, NUMBER_FORMAT.format(total)));
            stringBuilder.append("\n");
        });

        postToSlack(stringBuilder.toString());
    }

    @Scheduled(cron = "0 0 2/8 * * *")
    public void informAboutPerformance() {

        DecimalFormat decimalFormat = new DecimalFormat("#0.00'%'");
        decimalFormat.setPositivePrefix("+");

        Map<Symbol, BigDecimal> differences = priceService.to24HDifference(
                Arrays.asList(Symbol.WBNB, Symbol.EGG, Symbol.BDO, Symbol.SBDO, Symbol.WATCH)
        );

        StringBuilder stringBuilder = new StringBuilder();

        differences.forEach((key, value) -> {
            stringBuilder.append(String.format("%-5s %-5s", key, decimalFormat.format(value)));
            stringBuilder.append("\n");
        });

        postToSlack(stringBuilder.toString());
    }

    private void postToSlack(String message) {
        slackService.post(SlackMessage.builder().text("```" + message + "```").build());
    }

}
