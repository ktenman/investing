package ee.tenman.investing.integration.coingecko;

import com.google.common.collect.ImmutableMap;
import ee.tenman.investing.integration.binance.BinanceService;
import ee.tenman.investing.integration.yieldwatchnet.Symbol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.tagName;

@Slf4j
@Service
public class CoinGeckoService {

    private static final Map<Symbol, String> SYMBOL_TO_CURRENCY = ImmutableMap.<Symbol, String>builder()
            .put(Symbol.WBNB, "wbnb")
            .put(Symbol.BDO, "bdollar")
            .put(Symbol.SBDO, "bdollar-share")
            .put(Symbol.BUSD, "binance-usd")
            .put(Symbol.EGG, "goose-finance")
            .put(Symbol.CAKE, "pancakeswap")
            .put(Symbol.WATCH, "yieldwatch")
            .put(Symbol.ADA, "cardano")
            .put(Symbol.AUTO, "auto")
            .put(Symbol.BNB, "binance-coin")
            .put(Symbol.BTC, "bitcoin")
            .put(Symbol.CRO, "crypto-com-coin")
            .put(Symbol.DOT, "polkadot")
            .put(Symbol.ETH, "ethereum")
            .put(Symbol.SUSHI, "sushi")
            .put(Symbol.UNI, "uniswap")
            .put(Symbol.USDT, "tether")
            .put(Symbol.BTD, "bolt-true-dollar")
            .put(Symbol.BTS, "bolt-true-share")
            .put(Symbol.KEBAB, "kebab-token")
            .build();

    @Resource
    private BinanceService binanceService;

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public BigDecimal eurPrice(Symbol symbol) {
        open("https://www.coingecko.com/en/coins/" + SYMBOL_TO_CURRENCY.get(symbol));

        $(tagName("h1")).waitUntil(text(symbol.name()), 3000, 50);

        BigDecimal btcPrice = Optional.of($$(className("text-muted"))
                .filter(text("BTC"))
                .first()
                .text()
                .split(" BTC")[0])
                .map(BigDecimal::new)
                .orElseThrow(() -> new RuntimeException("Couldn't fetch BTC price"));

        BigDecimal btcToEur = binanceService.getPriceToEur(Symbol.BTC.name());


        BigDecimal price = btcPrice.multiply(btcToEur);

        log.info("{}/EUR: {}", symbol, price);

        return price;
    }

}
