package ee.tenman.investing.integration.bscscan;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.BigInteger;

import static ee.tenman.investing.integration.coinmarketcap.api.CoinMarketCapApiClient.USER_AGENT;

@FeignClient(value = "bscScanApiClient", url = "https://api.bscscan.com")
public interface BscScanApiClient {

    @GetMapping(value = "/api")
    BnbBalanceResponse fetchBnbBalance(
            @RequestParam("module") String module,
            @RequestParam("action") String action,
            @RequestParam("address") String address,
            @RequestParam("apikey") String apikey,
            @RequestHeader("User-Agent") String userAgent
    );

    @GetMapping(value = "/api")
    default BigDecimal fetchBnbBalance(@RequestParam("address") String address, @RequestParam("apikey") String apikey) {
        BnbBalanceResponse bnbBalanceResponse = fetchBnbBalance(
                "account", "balance", address, apikey, USER_AGENT
        );
        return new BigDecimal(new BigInteger(bnbBalanceResponse.getResult()), 18);
    }

    @GetMapping(value = "/api")
    TokenTransferEvents fetchTokenTransferEvents(
            @RequestParam("module") String module,
            @RequestParam("action") String action,
            @RequestParam("address") String address,
            @RequestParam("apikey") String apikey,
            @RequestHeader("User-Agent") String userAgent
    );

    default TokenTransferEvents fetchTokenTransferEvents(
            @RequestParam("address") String address,
            @RequestParam("apikey") String apikey
    ) {
        return fetchTokenTransferEvents("account", "tokentx", address, apikey, USER_AGENT);
    }

    @GetMapping(value = "/api")
    BnbBalanceResponse fetchTokenAccountBalance(
            @RequestParam("module") String module,
            @RequestParam("action") String action,
            @RequestParam("address") String address,
            @RequestParam("contractaddress") String contractaddress,
            @RequestParam("apikey") String apikey,
            @RequestHeader("User-Agent") String userAgent
    );

    default BigDecimal fetchTokenAccountBalance(
            @RequestParam("address") String address,
            @RequestParam("contractaddress") String contractaddress,
            @RequestParam("apikey") String apikey
    ) {
        BnbBalanceResponse bnbBalanceResponse = fetchTokenAccountBalance(
                "account", "tokenbalance", address, contractaddress, apikey, USER_AGENT
        );
        return new BigDecimal(new BigInteger(bnbBalanceResponse.getResult()), 18);
    }
}