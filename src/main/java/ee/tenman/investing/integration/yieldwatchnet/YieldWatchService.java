package ee.tenman.investing.integration.yieldwatchnet;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.investing.FileUtils;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public YieldSummary fetchYieldSummary() {
        closeWebDriver();
        open(YIELD_WATCH_NET);

        SelenideElement addressInputField = $(By.id("addressInputField"));
        for (char c : walletAddress.toCharArray()) {
            addressInputField.append(String.valueOf(c));
        }

        $(tagName("button")).click();

        $(tagName("span")).waitUntil(Condition.text("$"), 5000, 200);

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
        BigDecimal yieldEarnedPercentage = yieldEarned.setScale(8, BigDecimal.ROUND_UP).divide(total, HALF_UP);

        closeWebDriver();
        return YieldSummary.builder()
                .bdoAmount(bdoAmount)
                .wbnbAmount(wbnbAmount)
                .yieldEarnedPercentage(yieldEarnedPercentage)
                .build();
    }

}
