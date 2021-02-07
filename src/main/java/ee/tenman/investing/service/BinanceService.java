package ee.tenman.investing.service;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolInfo;
import com.binance.api.client.exception.BinanceApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static java.math.BigDecimal.ROUND_UP;
import static java.math.RoundingMode.DOWN;

@Service
@Slf4j
public class BinanceService {

    private static final BigDecimal TEN_EUROS = new BigDecimal("10.000000000");
    private static final BigDecimal THIRTY_EUROS = new BigDecimal("30.000000000");

    @Resource
    BinanceApiRestClient client;

    private List<SymbolInfo> symbolInfos;

    @Scheduled(cron = "0 0 * * * *")
    @PostConstruct
    public void fetchSupportedSymbols() {
        log.info("Fetching supported symbols");
        symbolInfos = client.getExchangeInfo().getSymbols();
    }

    public boolean isSupportedTicker(String symbol) {
        return symbolInfos.stream().anyMatch(s -> s.getSymbol().equals(symbol));
    }

    private boolean isSupportedSymbol(String symbol) {
        return symbolInfos.stream().anyMatch(s -> s.getSymbol().contains(symbol));
    }

    public BigDecimal getPriceToEur(String symbol) {

        String symbolToEur = symbol + "EUR";
        if (!isSupportedTicker(symbolToEur)) {
            if (isSupportedTicker(symbol + "BTC")) {
                BigDecimal symbolInBtc = getPriceToEur(symbol, "BTC");
                BigDecimal btcInEuro = getPriceToEur("BTC", "EUR");
                BigDecimal price = symbolInBtc.multiply(btcInEuro);
                log.info("{} price {}", symbolToEur, price);
                return price;
            }
            if (isSupportedTicker("BTC" + symbol)) {
                BigDecimal btcInSymbol = getPriceToEur("BTC", symbol);
                BigDecimal btcInEuro = getPriceToEur("BTC", "EUR");
                BigDecimal price = btcInEuro.divide(btcInSymbol, RoundingMode.HALF_UP);
                log.info("{} price {}", symbolToEur, price);
                return price;
            }
            throw new NotSupportedSymbolException(String.format("%s not supported", symbolToEur));
        }

        BigDecimal price = getPriceToEur(symbol, "EUR");
        log.info("{} price {}", symbolToEur, price);
        return price;
    }

    public BigDecimal getPriceToEur(String from, String to) {
        if (!isSupportedSymbol(from)) {
            throw new NotSupportedSymbolException(String.format("From symbol %s not supported", from));
        }

        if (!isSupportedSymbol(to)) {
            throw new NotSupportedSymbolException(String.format("To symbol %s not supported", to));
        }

        return new BigDecimal(client.getPrice(from + to).getPrice());
    }

    @Scheduled(cron = "0 0 12 1-7 * MON")
    public void buyCrypto() {

        buy("BTCEUR", TEN_EUROS);
        buy("DOTEUR", TEN_EUROS);
        buy("ADAEUR", TEN_EUROS);
        buy("ETHEUR", TEN_EUROS);

        BigDecimal totalEuros = client.getAccount().getBalances().stream()
                .filter(assetBalance -> assetBalance.getAsset().equals("EUR"))
                .findFirst()
                .map(AssetBalance::getFree)
                .map(BigDecimal::new)
                .orElseThrow(() -> new RuntimeException("Not found EUR"));

        BigDecimal baseAmount = totalEuros.compareTo(THIRTY_EUROS) >= 0 ? THIRTY_EUROS : totalEuros;
        BigDecimal boughtBnbAmount = buy("BNBEUR", baseAmount);

        BigDecimal thirtyThreePercent = BigDecimal.valueOf(0.3333333333333333);
        buy("UNIBNB", boughtBnbAmount.multiply(thirtyThreePercent));
        buy("SUSHIBNB", boughtBnbAmount.multiply(thirtyThreePercent));
    }

    private BigDecimal buy(String ticker, BigDecimal baseAmount) {
        String stepSize = client.getExchangeInfo().getSymbolInfo(ticker).getSymbolFilter(FilterType.LOT_SIZE).getStepSize();
        int scale = scale(stepSize);
        BigDecimal quantity = quantity(client, ticker, baseAmount).setScale(scale, ROUND_UP);
        boolean success = false;
        while (!success) {
            try {
                client.newOrder(marketBuy(ticker, quantity.toString()));
                log.info("Success {} with amount {}", ticker, quantity);
                success = true;
            } catch (BinanceApiException e) {
                log.error("", e);
                log.info("Failed {} with amount {}", ticker, quantity);
                if ("Account has insufficient balance for requested action.".equals(e.getMessage())) {
                    quantity = quantity.subtract(new BigDecimal(stepSize)).setScale(scale, ROUND_UP);
                } else {
                    quantity = quantity.add(new BigDecimal(stepSize)).setScale(scale, ROUND_UP);
                }
            }
        }
        return quantity;
    }

    int scale(String stepSize) {
        double amount = new BigDecimal(stepSize).doubleValue();
        int scale = 0;
        while (amount != 1) {
            amount *= 10;
            scale++;
        }
        return scale;
    }

    BigDecimal quantity(BinanceApiRestClient client, String ticker, BigDecimal baseAmount) {
        String stepSize = client.getExchangeInfo().getSymbolInfo(ticker).getSymbolFilter(FilterType.LOT_SIZE).getStepSize();
        String price = client.getPrice(ticker).getPrice();
        BigDecimal precision = new BigDecimal(stepSize);
        BigDecimal amount = baseAmount
                .divide(new BigDecimal(price), DOWN);
        return BigDecimal.valueOf(mRound(amount.doubleValue(), precision.doubleValue()));
    }

    double mRound(double value, double factor) {
        return Math.round(value / factor) * factor;
    }

}
