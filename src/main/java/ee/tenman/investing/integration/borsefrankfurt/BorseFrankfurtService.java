package ee.tenman.investing.integration.borsefrankfurt;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static ee.tenman.investing.integration.borsefrankfurt.Exchange.FRA;
import static java.lang.String.format;
import static org.openqa.selenium.By.name;
import static org.openqa.selenium.By.tagName;

@Service
public class BorseFrankfurtService {

    public BigDecimal fetchPriceFromGoogle(StockSymbol stockSymbol) {
        open("https://www.google.com/");

        $(name("q"))
                .setValue(stockSymbol.ticker())
                .pressEnter();

        String currency = stockSymbol.currency().name();
        return Optional.of($$(tagName("span"))
                .find(text(currency))
                .closest("div")
                .text()
                .split(currency)[0])
                .map(StringUtils::trim)
                .map(element -> StringUtils.replace(element, ",", ""))
                .map(BigDecimal::new)
                .orElseThrow(() -> new IllegalStateException(format("Couldn't fetch %s", stockSymbol)));
    }

    public BigDecimal fetchPrice(StockSymbol stockSymbol) {
        if (stockSymbol.exchange() != FRA) {
            throw new IllegalStateException("Only FRA stock symbols");
        }

        open("https://www.boerse-frankfurt.de/en");

        SelenideElement searchBox = $$(tagName("input"))
                .find(attribute("placeholder", "Name / WKN / ISIN / Symbol"));

        try {
            TimeUnit.MILLISECONDS.sleep(700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SelenideElement selenideElement = searchBox.setValue(stockSymbol.symbol());

        try {
            TimeUnit.MILLISECONDS.sleep(700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        selenideElement.pressEnter();

        SelenideElement frankfurtExchange = $$(tagName("button"))
                .find(text("Frankfurt"));

        frankfurtExchange.shouldHave(Condition.exist);

        frankfurtExchange.click();

        return Optional.of($$(tagName("tr"))
                .find(text("Last price"))
                .$$(tagName("td"))
                .last()
                .text()).map(BigDecimal::new)
                .orElseThrow(() -> new IllegalStateException(format("Couldn't fetch %s", stockSymbol)));
    }

    private void waitUntilElementComesVisible(SelenideElement selenideElement) {
        int counter = 0;
        while (!selenideElement.exists() && counter < 30) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            counter++;
        }
    }
}
