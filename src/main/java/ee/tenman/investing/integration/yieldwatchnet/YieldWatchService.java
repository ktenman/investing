package ee.tenman.investing.integration.yieldwatchnet;

import com.codeborne.selenide.SelenideElement;
import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.integration.yieldwatchnet.api.Autofarm;
import ee.tenman.investing.integration.yieldwatchnet.api.LPInfo;
import ee.tenman.investing.integration.yieldwatchnet.api.LPVaults;
import ee.tenman.investing.integration.yieldwatchnet.api.Result;
import ee.tenman.investing.integration.yieldwatchnet.api.Vault;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldData;
import ee.tenman.investing.service.SecretsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.compare.ComparableUtils;
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

        LPVaults lpVaults = yieldData
                .getResult()
                .getAutofarm()
                .getLPVaults();

        YieldSummary yieldSummary = new YieldSummary();

        addPoolData(lpVaults, yieldSummary, "BDO-BUSD Pool");
        addPoolData(lpVaults, yieldSummary, "WBNB-BUSD Pool");
        addPoolData(lpVaults, yieldSummary, "BDO-WBNB Pool");

        BigDecimal yield = lpVaults.getTotalUSDValues().getYield();
        BigDecimal total = lpVaults.getTotalUSDValues().getTotal();

        BigDecimal yieldEarnedPercentage = yield.divide(total, 8, ROUND_UP);

        yieldSummary.setYieldEarnedPercentage(yieldEarnedPercentage);

        return yieldSummary;
    }

    private void addPoolData(LPVaults lpVaults, YieldSummary yieldSummary, String poolName) {
        LPInfo lpInfo = lpVaults.getVaults()
                .stream()
                .filter(v -> v.getName().equals(poolName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("Couldn't fetch %s data", poolName)))
                .getLPInfo();

        Map<String, BigDecimal> symbolAmounts = ImmutableMap.of(
                lpInfo.getSymbolToken0(), lpInfo.getCurrentToken0(),
                lpInfo.getSymbolToken1(), lpInfo.getCurrentToken1()
        );

        String first = poolName.split("-")[0];
        String second = poolName.split("-")[1].split(" ")[0];

        yieldSummary.add(first, symbolAmounts.get(first));
        yieldSummary.add(second, symbolAmounts.get(second));
    }

    public YieldSummary getYieldSummaryIK() {
        YieldData yieldData = yieldApiService.getYieldData();

        if (yieldData == null) {
            return fetchYieldSummary();
        }

        Autofarm autofarm = Optional.ofNullable(yieldData.getResult())
                .map(Result::getAutofarm)
                .orElseThrow(() -> new RuntimeException("Couldn't fetch Autofarm"));

        LPInfo lpInfo = autofarm
                .getLPVaults()
                .getVaults()
                .stream()
                .filter(vault -> "BDO-WBNB Pool".equals(vault.getName()))
                .findFirst()
                .map(Vault::getLPInfo)
                .orElseThrow(() -> new RuntimeException("Failed to fetch BDO-WBNB Pool"));

        BigDecimal bdoAmount = lpInfo.getCurrentToken0();
        BigDecimal wbnAmount = lpInfo.getCurrentToken1();

        BigDecimal yield = yieldData.getResult().getAutofarm().getLPVaults().getTotalUSDValues().getYield()
                .add(yieldData.getResult().getBeefyFinance().getLPVaults().getTotalUSDValues().getYield());

        BigDecimal total = yieldData.getResult().getAutofarm().getLPVaults().getTotalUSDValues().getTotal()
                .add(yieldData.getResult().getBeefyFinance().getLPVaults().getTotalUSDValues().getTotal());

        BigDecimal yieldEarnedPercentage = yield.divide(total, 8, ROUND_UP);

        YieldSummary yieldSummary = YieldSummary.builder()
                .bdoAmount(bdoAmount)
                .wbnbAmount(wbnAmount)
                .yieldEarnedPercentage(yieldEarnedPercentage)
                .build();

        boolean hasMissingAmounts = Stream.of(yieldSummary.getBdoAmount(), yieldSummary.getWbnbAmount(), yieldEarnedPercentage)
                .filter(Objects::nonNull)
                .anyMatch(bigDecimal -> ComparableUtils.is(bigDecimal).lessThanOrEqualTo(ZERO));

        if (hasMissingAmounts) {
            return fetchYieldSummary();
        }

        return yieldSummary;
    }


}
