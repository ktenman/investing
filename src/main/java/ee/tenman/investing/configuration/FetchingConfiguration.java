package ee.tenman.investing.configuration;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.List;

public interface FetchingConfiguration {

    String BINANCE_COIN_ID = "Binance Coin";
    String POLKADOT_ID = "Polkadot";
    String CRO_ID = "Crypto.com Coin";
    String UNISWAP_ID = "Uniswap";
    String BITCOIN_ID = "Bitcoin";
    String SUSHI_SWAP_ID = "SushiSwap";
    String SYNTHETIX_ID = "Synthetix";
    String ONE_INCH_ID = "1inch";
    String CARDANO_ID = "Cardano";
    String ETHEREUM_ID = "Ethereum";

    List<String> TICKERS_TO_FETCH = Arrays.asList(
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

    ImmutableMap<String, String> TICKER_SYMBOL_MAP = ImmutableMap.<String, String>builder()
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
}