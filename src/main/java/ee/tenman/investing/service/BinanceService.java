package ee.tenman.investing.service;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.exception.BinanceApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static java.math.BigDecimal.ROUND_UP;
import static java.math.RoundingMode.DOWN;
import static java.util.stream.Collectors.joining;

@Service
@Slf4j
public class BinanceService {

    private static final BigDecimal TEN_EUROS = new BigDecimal("10.000000000");
    private static final BigDecimal THIRTY_EUROS = new BigDecimal("30.000000000");
    @Value("binance_api_key.txt")
    ClassPathResource binanceApiKey;
    @Value("binance_secret_key.txt")
    ClassPathResource binanceSecretKey;

    @Scheduled(cron = "0 0 12 1-7 * MON")
    public void buyCrypto() {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(getSecret(binanceApiKey), getSecret(binanceSecretKey));
        BinanceApiRestClient client = factory.newRestClient();
        client.ping();

        buy(client, "BTCEUR", TEN_EUROS);
        buy(client, "DOTEUR", TEN_EUROS);

        BigDecimal totalEuros = client.getAccount().getBalances().stream()
                .filter(assetBalance -> assetBalance.getAsset().equals("EUR"))
                .findFirst()
                .map(AssetBalance::getFree)
                .map(BigDecimal::new)
                .orElseThrow(() -> new RuntimeException("Not found EUR"));

        BigDecimal baseAmount = totalEuros.compareTo(THIRTY_EUROS) >= 0 ? THIRTY_EUROS : totalEuros;
        BigDecimal boughtBnbAmount = buy(client, "BNBEUR", baseAmount);

        BigDecimal thirtyThreePercent = BigDecimal.valueOf(0.3333333333333333);
        buy(client, "UNIBNB", boughtBnbAmount.multiply(thirtyThreePercent));
        buy(client, "SUSHIBNB", boughtBnbAmount.multiply(thirtyThreePercent));
    }

    private BigDecimal buy(BinanceApiRestClient client, String ticker, BigDecimal baseAmount) {
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
                quantity = quantity.add(new BigDecimal(stepSize)).setScale(scale, ROUND_UP);
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

    private String getSecret(ClassPathResource classPathResource) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(classPathResource.getInputStream()))) {
            return buffer.lines().collect(joining(""));
        } catch (IOException e) {
            log.error("getPrivateKeyId ", e);
            return null;
        }
    }

}
