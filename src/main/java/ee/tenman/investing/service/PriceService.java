package ee.tenman.investing.service;

import com.google.common.collect.ImmutableMap;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ee.tenman.investing.service.CoinMarketCapService.BINANCE_COIN_ID;
import static ee.tenman.investing.service.CoinMarketCapService.BITCOIN_ID;
import static ee.tenman.investing.service.CoinMarketCapService.CARDANO_ID;
import static ee.tenman.investing.service.CoinMarketCapService.CRO_ID;
import static ee.tenman.investing.service.CoinMarketCapService.ETHEREUM_ID;
import static ee.tenman.investing.service.CoinMarketCapService.ONE_INCH_ID;
import static ee.tenman.investing.service.CoinMarketCapService.POLKADOT_ID;
import static ee.tenman.investing.service.CoinMarketCapService.SUSHI_SWAP_ID;
import static ee.tenman.investing.service.CoinMarketCapService.SYNTHETIX_ID;
import static ee.tenman.investing.service.CoinMarketCapService.UNISWAP_ID;

@Service
public class PriceService {

    protected static final List<String> TICKERS_TO_FETCH = Arrays.asList(
            BINANCE_COIN_ID,
            BITCOIN_ID,
            CARDANO_ID,
            CRO_ID,
            ETHEREUM_ID,
            ONE_INCH_ID,
            POLKADOT_ID,
            SUSHI_SWAP_ID,
            SYNTHETIX_ID,
            UNISWAP_ID
    );
    public static ImmutableMap<String, String> TICKER_SYMBOL_MAP = ImmutableMap.<String, String>builder()
            .put(BINANCE_COIN_ID, "BNB")
            .put(BITCOIN_ID, "BTC")
            .put(CARDANO_ID, "ADA")
            .put(CRO_ID, "CRO")
            .put(ETHEREUM_ID, "ETH")
            .put(ONE_INCH_ID, "1INCH")
            .put(POLKADOT_ID, "DOT")
            .put(SUSHI_SWAP_ID, "SUSHI")
            .put(SYNTHETIX_ID, "SNX")
            .put(UNISWAP_ID, "UNI")
            .build();

    @Resource
    CoinMarketCapService coinMarketCapService;

    @Resource
    BinanceService binanceService;

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public Map<String, BigDecimal> getPrices(List<String> input) {
        List<String> tickers = new ArrayList<>(input);
        Map<String, BigDecimal> binancePrices = new HashMap<>();

        for (String ticker : input) {
            try {
                BigDecimal priceToEur = binanceService.getPriceToEur(TICKER_SYMBOL_MAP.get(ticker));
                binancePrices.put(ticker, priceToEur);
                tickers.remove(ticker);
            } catch (Exception ignored) {

            }
        }

        if (tickers.isEmpty()) {
            return binancePrices;
        }

        Map<String, BigDecimal> coinMarketCapServicePrices = coinMarketCapService.getPrices(tickers);
        binancePrices.putAll(coinMarketCapServicePrices);
        return binancePrices;
    }

}
