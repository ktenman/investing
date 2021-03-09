package ee.tenman.investing.integration.yieldwatchnet;

import com.codeborne.selenide.SelenideElement;
import ee.tenman.investing.integration.yieldwatchnet.api.Autofarm;
import ee.tenman.investing.integration.yieldwatchnet.api.Balance;
import ee.tenman.investing.integration.yieldwatchnet.api.BeefyFinance;
import ee.tenman.investing.integration.yieldwatchnet.api.LPInfo;
import ee.tenman.investing.integration.yieldwatchnet.api.LPVaults;
import ee.tenman.investing.integration.yieldwatchnet.api.PancakeSwap;
import ee.tenman.investing.integration.yieldwatchnet.api.Result;
import ee.tenman.investing.integration.yieldwatchnet.api.Staking;
import ee.tenman.investing.integration.yieldwatchnet.api.TotalUSDValues;
import ee.tenman.investing.integration.yieldwatchnet.api.Vault;
import ee.tenman.investing.integration.yieldwatchnet.api.WalletBalance;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldApiService;
import ee.tenman.investing.integration.yieldwatchnet.api.YieldData;
import ee.tenman.investing.service.SecretsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class YieldWatchService {

    @Resource
    private SecretsService secretsService;
    private static final String YIELD_WATCH_NET = "http://yieldwatch.net/";

    @Resource
    private YieldApiService yieldApiService;

    @Deprecated
    YieldSummary fetchYieldSummary() {
        closeWebDriver();
        open(YIELD_WATCH_NET);

        SelenideElement addressInputField = $(By.id("addressInputField"));
        for (char c : secretsService.getWalletAddress().toCharArray()) {
            addressInputField.append(String.valueOf(c));
        }

        addressInputField.pressEnter();

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
                .collect(toList());

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
                .collect(toList());

        BigDecimal bdoAmount = coinAmounts.get(0).add(coinAmounts.get(1));
        BigDecimal wbnbAmount = coinAmounts.get(2).add(coinAmounts.get(3));
        BigDecimal yieldEarnedPercentage = yieldEarned.setScale(8, ROUND_UP).divide(total, HALF_UP);

        closeWebDriver();
        return YieldSummary.builder()
                // TODO fix implementation
//                .bdoAmount(bdoAmount)
//                .wbnbAmount(wbnbAmount)
//                .yieldEarnedPercentage(yieldEarnedPercentage)
                .build();
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<String, YieldSummary> getYieldSummary(List<String> walletAddresses) {
        return walletAddresses.stream()
                .collect(toMap(Function.identity(), walletAddress -> {
                    YieldData yieldData = yieldApiService.getYieldData(walletAddress);
                    return buildYieldSummary(yieldData);
                }));
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public YieldSummary getYieldSummary() {
        YieldData yieldData = yieldApiService.getYieldData();
        return buildYieldSummary(yieldData);
    }

    private YieldSummary buildYieldSummary(YieldData yieldData) {
        log.info("{}", yieldData);

        List<Vault> vaults = Stream.of(
                Optional.ofNullable(yieldData.getResult()
                        .getAutofarm())
                        .map(Autofarm::getLPVaults)
                        .map(LPVaults::getVaults)
                        .orElse(null),
                Optional.ofNullable(yieldData.getResult()
                        .getBeefyFinance())
                        .map(BeefyFinance::getLPVaults)
                        .map(LPVaults::getVaults)
                        .orElse(null),
                Optional.ofNullable(yieldData.getResult()
                        .getPancakeSwap())
                        .map(PancakeSwap::getLPVaults)
                        .map(LPVaults::getVaults)
                        .orElse(null),
                Optional.ofNullable(yieldData.getResult()
                        .getPancakeSwap())
                        .map(PancakeSwap::getStaking)
                        .map(Staking::getVaults)
                        .orElse(null))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(toList());

        YieldSummary yieldSummary = new YieldSummary();

        Set<Balance> walletBalances = new HashSet<>(Optional.of(yieldData)
                .map(YieldData::getResult)
                .map(Result::getWalletBalance)
                .map(WalletBalance::getBalances)
                .orElse(Collections.emptyList()));

        yieldSummary.setWalletBalances(walletBalances);

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
                        .orElse(null),
                Optional.ofNullable(yieldData.getResult())
                        .map(Result::getPancakeSwap)
                        .map(PancakeSwap::getStaking)
                        .map(Staking::getTotalUSDValues)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(toList());

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

        log.info("{}", vault);

        if (lpInfo != null) {
            yieldSummary.addToPoolAmounts(lpInfo.getSymbolToken0().toUpperCase(), lpInfo.getCurrentToken0());
            yieldSummary.addToPoolAmounts(lpInfo.getSymbolToken1().toUpperCase(), lpInfo.getCurrentToken1());
            String newPoolName = String.format("%s-%s Pool", lpInfo.getSymbolToken0().toUpperCase(), lpInfo.getSymbolToken1().toUpperCase());
            yieldSummary.getPools().put(newPoolName, yieldSummary.getPools().getOrDefault(newPoolName, lpInfo.getPriceInUSDLPToken()));
        } else if (StringUtils.isNotEmpty(vault.getDepositToken())) {
            yieldSummary.addToPoolAmounts(vault.getDepositToken().toUpperCase(), vault.getDepositedTokens());
        } else {
            throw new IllegalArgumentException(String.format("Not supported. %s", vault.toString()));
        }
    }

}
