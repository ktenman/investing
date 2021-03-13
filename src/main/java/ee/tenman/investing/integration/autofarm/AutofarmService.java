package ee.tenman.investing.integration.autofarm;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.By.tagName;

@Service
public class AutofarmService {
    private static final String AUTO_FARM_NETWORK_URL = "https://beta.autofarm.network/";

    private BigDecimal dailyYieldReturn;

    public BigDecimal getDailyYieldReturn() {
        if (dailyYieldReturn == null) {
            setDailyYieldReturn();
        }
        return dailyYieldReturn;
    }

    @Scheduled(fixedDelay = 900000, initialDelay = 900000)
    public void setDailyYieldReturn() {
        this.dailyYieldReturn = fetchDailyYieldReturn();
    }

    public BigDecimal fetchDailyYieldReturn() {
        closeWebDriver();
        open(AUTO_FARM_NETWORK_URL);
        String yieldPercentage = $$(tagName("a"))
                .find(text("WBNB-BDO LP"))
                .$$(tagName("div"))
                .filter(text("%"))
                .last()
                .text()
                .replace("%", "");

        closeWindow();
        return new BigDecimal(yieldPercentage).setScale(6, BigDecimal.ROUND_UP)
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
    }

}
