package ee.tenman.investing.integration.yieldwatchnet;

import com.codeborne.selenide.SelenideElement;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldData;
import ee.tenman.investing.service.SecretsService;
import org.apache.commons.lang3.compare.ComparableUtils;
import org.openqa.selenium.By;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
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
import static java.math.RoundingMode.HALF_UP;
import static java.util.Comparator.reverseOrder;
import static org.openqa.selenium.By.tagName;

@Service
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

        if (yieldData == null) {
            return fetchYieldSummary();
        }

        BigDecimal bdo1 = yieldData.getResult().getAutofarm().getLPVaults().getVaults().get(0).getLPInfo().getCurrentToken0();
        BigDecimal bdo2 = yieldData.getResult().getBeefyFinance().getLPVaults().getVaults().get(0).getLPInfo().getCurrentToken0();

        BigDecimal wbnb1 = yieldData.getResult().getAutofarm().getLPVaults().getVaults().get(0).getLPInfo().getCurrentToken1();
        BigDecimal wbnb2 = yieldData.getResult().getBeefyFinance().getLPVaults().getVaults().get(0).getLPInfo().getCurrentToken1();

        BigDecimal bdoAmount = bdo1.add(bdo2);
        BigDecimal wbnbAmount = wbnb1.add(wbnb2);

        BigDecimal yield = yieldData.getResult().getAutofarm().getLPVaults().getTotalUSDValues().getYield()
                .add(yieldData.getResult().getBeefyFinance().getLPVaults().getTotalUSDValues().getYield());

        BigDecimal total = yieldData.getResult().getAutofarm().getLPVaults().getTotalUSDValues().getTotal()
                .add(yieldData.getResult().getBeefyFinance().getLPVaults().getTotalUSDValues().getTotal());

        BigDecimal yieldEarnedPercentage = yield.divide(total, 8, ROUND_UP);

        YieldSummary yieldSummary = YieldSummary.builder()
                .bdoAmount(bdoAmount)
                .wbnbAmount(wbnbAmount)
                .yieldEarnedPercentage(yieldEarnedPercentage)
                .build();

        boolean hasMissingAmounts = Stream.of(yieldSummary.getBdoAmount(), yieldSummary.getWbnbAmount(), yieldEarnedPercentage)
                .filter(Objects::nonNull)
                .anyMatch(bigDecimal -> ComparableUtils.is(bigDecimal).lessThanOrEqualTo(BigDecimal.ZERO));

        if (hasMissingAmounts) {
            return fetchYieldSummary();
        }

        return yieldSummary;
    }

}
