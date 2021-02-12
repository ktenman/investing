package ee.tenman.investing.integration.yieldwatchnet;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.FileUtils;
import ee.tenman.investing.integration.binance.BinanceService;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.not;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Comparator.reverseOrder;
import static org.openqa.selenium.By.tagName;

@Service
public class YieldWatchService {

    static {
        Configuration.startMaximized = true;
        Configuration.headless = true;
        Configuration.proxyEnabled = false;
        Configuration.screenshots = false;
        Configuration.browser = "firefox";
    }

    @Value("wallet_address.txt")
    ClassPathResource walletAddressResource;
    private static final String YIELD_WATCH_NET = "http://yieldwatch.net/";
    private String walletAddress;

    @PostConstruct
    void setWalletAddress() {
        walletAddress = FileUtils.getSecret(walletAddressResource);
    }

    @Resource
    private BinanceService binanceService;

    private BigDecimal bnbAmount;

    public BigDecimal getBnbAmount() {
        if (bnbAmount == null) {
            setBnbAmount();
        }
        return bnbAmount;
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 300000)
    void setBnbAmount() {
        this.bnbAmount = fetchBnbAmount();
    }

    private BigDecimal fetchBnbAmount() {
        closeWebDriver();
        open(YIELD_WATCH_NET);

        SelenideElement addressInputField = $(By.id("addressInputField"));
        for (char c : walletAddress.toCharArray()) {
            addressInputField.append(String.valueOf(c));
        }

        $(tagName("button")).click();

        String h3 = $(tagName("h3")).text().split("\\$")[1];
        String yield = StringUtils.replace(h3, ",", "");

        BigDecimal busdToEur = binanceService.getPriceToEur("BUSD");
        BigDecimal bnbToEur = binanceService.getPriceToEur("BNB");


        closeWebDriver();
        return new BigDecimal(yield).multiply(busdToEur).divide(bnbToEur, HALF_UP);
    }

    public ImmutableMap<String, BigDecimal> fetchEarnedYield() {
        closeWebDriver();
        open(YIELD_WATCH_NET);

        SelenideElement addressInputField = $(By.id("addressInputField"));
        for (char c : walletAddress.toCharArray()) {
            addressInputField.append(String.valueOf(c));
        }

        $(tagName("button")).click();

        $(tagName("span")).waitUntil(Condition.text("$"), 5000, 200);

        BigDecimal busdToEur = binanceService.getPriceToEur("BUSD");
        List<BigDecimal> amounts = $$(tagName("span"))
                .filter(text("$"))
                .filter(not(text("k")))
                .texts()
                .stream()
                .distinct()
                .map(a -> a.replace("$", "").replace(",", ""))
                .map(BigDecimal::new)
                .map(a -> a.multiply(busdToEur))
                .sorted(reverseOrder())
                .collect(Collectors.toList());

        BigDecimal total = amounts.get(0);
        BigDecimal deposit = amounts.get(1);
        BigDecimal yieldEarned = amounts.get(2);

        closeWebDriver();
        return ImmutableMap.of(
                "total", total,
                "deposit", deposit,
                "yieldEarned", yieldEarned
        );
    }

}
