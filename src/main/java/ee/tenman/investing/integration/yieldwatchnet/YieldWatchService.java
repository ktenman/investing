package ee.tenman.investing.integration.yieldwatchnet;

import com.codeborne.selenide.SelenideElement;
import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.integration.yieldwatchnet.api.Autofarm;
import ee.tenman.investing.integration.yieldwatchnet.api.BeefyFinance;
import ee.tenman.investing.integration.yieldwatchnet.api.LPInfo;
import ee.tenman.investing.integration.yieldwatchnet.api.LPVaults;
import ee.tenman.investing.integration.yieldwatchnet.api.PancakeSwap;
import ee.tenman.investing.integration.yieldwatchnet.api.Result;
import ee.tenman.investing.integration.yieldwatchnet.api.TotalUSDValues;
import ee.tenman.investing.integration.yieldwatchnet.api.Vault;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldData;
import ee.tenman.investing.service.SecretsService;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.not;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static java.math.BigDecimal.ROUND_UP;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Comparator.reverseOrder;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class YieldWatchService {

    @Resource
    private SecretsService secretsService;
    private static final String YIELD_WATCH_NET = "http://yieldwatch.net/";

    @Resource
    private YieldApiService yieldApiService;

    YieldSummary fetchYieldSummary() {
        closeWebDriver();
        open(YIELD_WATCH_NET);

        SelenideElement addressInputField = $(By.id("addressInputField"));
        for (char c : secretsService.getWalletAddress().toCharArray()) {
            addressInputField.append(String.valueOf(c));
        }

        $$(tagName("i")).filter(cssClass("binoculars")).first().click();

        $(tagName("span")).waitUntil(text("$"), 5000, 200);

        List<BigDecimal> amounts = $$(tagName("span"))
                .filter(text("$"))
                .filter(not(text("k")))
                .texts()
                .stream()
                .distinct()
                .map(a -> a.replace("$", "").replace(",", ""))
                .map(BigDecimal::new)
                .sorted(reverseOrder())
                .collect(Collectors.toList());

        BigDecimal total = amounts.get(1).add(amounts.get(2));
        BigDecimal deposit = amounts.get(0);
        BigDecimal yieldEarned = total.subtract(deposit);

        List<BigDecimal> coinAmounts = $$(By.className("sub"))
                .filter(text("/"))
                .texts()
                .stream()
                .map(s -> s.replace(",", "").split(" / "))
                .flatMap(Stream::of)
                .map(BigDecimal::new)
                .sorted(reverseOrder())
                .collect(Collectors.toList());

        BigDecimal bdoAmount = coinAmounts.get(0).add(coinAmounts.get(1));
        BigDecimal wbnbAmount = coinAmounts.get(2).add(coinAmounts.get(3));
        BigDecimal yieldEarnedPercentage = yieldEarned.setScale(8, ROUND_UP).divide(total, HALF_UP);

        closeWebDriver();
        return YieldSummary.builder()
                .bdoAmount(bdoAmount)
                .wbnbAmount(wbnbAmount)
                .yieldEarnedPercentage(yieldEarnedPercentage)
                .build();
    }

    public YieldSummary getYieldSummary() {
        YieldData yieldData = yieldApiService.getYieldData();

        log.info("{}", yieldData);

        List<Vault> vaults = Stream.of(
                Optional.ofNullable(yieldData.getResult()
                        .getAutofarm()).map(Autofarm::getLPVaults)
                        .orElse(null),
                Optional.ofNullable(yieldData.getResult()
                        .getBeefyFinance()).map(BeefyFinance::getLPVaults)
                        .orElse(null),
                Optional.ofNullable(yieldData.getResult()
                        .getPancakeSwap()).map(PancakeSwap::getLPVaults)
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(LPVaults::getVaults)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        YieldSummary yieldSummary = new YieldSummary();

        vaults.forEach(vault -> addPoolData(vault, yieldSummary));

        List<TotalUSDValues> totalUsdValues = Stream.of(
                Optional.ofNullable(yieldData.getResult())
                        .map(Result::getAutofarm)
                        .map(Autofarm::getLPVaults)
                        .map(LPVaults::getTotalUSDValues)
                        .orElse(null),
                Optional.ofNullable(yieldData.getResult()).
                        map(Result::getBeefyFinance)
                        .map(BeefyFinance::getLPVaults)
                        .map(LPVaults::getTotalUSDValues)
                        .orElse(null),
                Optional.ofNullable(yieldData.getResult())
                        .map(Result::getPancakeSwap)
                        .map(PancakeSwap::getLPVaults)
                        .map(LPVaults::getTotalUSDValues)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        BigDecimal yield = totalUsdValues.stream()
                .map(TotalUSDValues::getYield)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal total = totalUsdValues.stream()
                .map(TotalUSDValues::getTotal)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal yieldEarnedPercentage = yield.divide(total, 8, ROUND_UP);

        yieldSummary.setYieldEarnedPercentage(yieldEarnedPercentage);

        return yieldSummary;
    }

    private void addPoolData(Vault vault, YieldSummary yieldSummary) {
        LPInfo lpInfo = vault.getLPInfo();
        String poolName = vault.getName();

        Map<String, BigDecimal> symbolAmounts = ImmutableMap.of(
                lpInfo.getSymbolToken0().toUpperCase(), lpInfo.getCurrentToken0(),
                lpInfo.getSymbolToken1().toUpperCase(), lpInfo.getCurrentToken1()
        );

        String first = poolName.split("-")[0];
        String second = poolName.split("-")[1].split(" ")[0];

        yieldSummary.add(first, symbolAmounts.get(first));
        yieldSummary.add(second, symbolAmounts.get(second));

        yieldSummary.getPools().put(poolName, yieldSummary.getPools().getOrDefault(poolName, lpInfo.getPriceInUSDLPToken()));
    }

}
