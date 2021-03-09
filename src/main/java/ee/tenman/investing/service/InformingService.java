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
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InformingService {

    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    @Resource
    private SlackService slackService;
    @Resource
    private YieldWatchService yieldWatchService;
    @Resource
    private PriceService priceService;
    @Value("#{'${wallets:}'.split(',')}")
    private List<String> wallets;

    @Scheduled(cron = "0 0 0/8 * * *")
    @Scheduled(cron = "40 52 0 9 3 *")
    public void informAboutPortfolios() {
        if (wallets.stream().noneMatch(StringUtils::isNotBlank)) {
            log.info("Skipping. No wallets were provided");
            return;
        }

        Map<String, YieldSummary> yieldSummaries = yieldWatchService.getYieldSummary(wallets);

        Set<Balance> allUniqueBalances = yieldSummaries.values().stream()
                .map(YieldSummary::getPoolBalances)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Map<Symbol, BigDecimal> prices = priceService.getPricesOfBalances(allUniqueBalances);

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("```");

        yieldSummaries.forEach((key, value) -> {
            BigDecimal total = value.getTotal(prices);
            stringBuilder.append(String.format("%s - %s", key, NUMBER_FORMAT.format(total)));
            stringBuilder.append("\n");
        });

        stringBuilder.append("```");

        slackService.post(SlackMessage.builder().text(stringBuilder.toString()).build());
    }

}
