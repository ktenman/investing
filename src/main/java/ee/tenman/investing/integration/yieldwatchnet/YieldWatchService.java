package ee.tenman.investing.integration.yieldwatchnet;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import ee.tenman.investing.integration.binance.BinanceService;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static java.math.RoundingMode.HALF_UP;

@Service
public class YieldWatchService {

    private static final String WALLET_ADDRESS = "0x37413c688A3F03C919C5Ae83f4fb373fbABC5515";
    private static final String YIELD_WATCH_NET = "http://yieldwatch.net/";

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
    public void setBnbAmount() {
        this.bnbAmount = fetchBnbAmount();
    }

    public BigDecimal fetchBnbAmount() {
        Configuration.startMaximized = true;
        Configuration.headless = true;
        Configuration.proxyEnabled = false;
        Configuration.screenshots = false;
        Configuration.browser = "firefox";
        closeWebDriver();
        open(YIELD_WATCH_NET);

        SelenideElement addressInputField = $(By.id("addressInputField"));
        for (char c : WALLET_ADDRESS.toCharArray()) {
            addressInputField.append(String.valueOf(c));
        }

        $(By.tagName("button")).click();

        String h3 = $(By.tagName("h3")).text().split("\\$")[1];
        String yield = StringUtils.replace(h3, ",", "");

        BigDecimal busdToEur = binanceService.getPriceToEur("BUSD");
        BigDecimal bnbToEur = binanceService.getPriceToEur("BNB");


        closeWebDriver();
        return new BigDecimal(yield).multiply(busdToEur).divide(bnbToEur, HALF_UP);
    }

}
