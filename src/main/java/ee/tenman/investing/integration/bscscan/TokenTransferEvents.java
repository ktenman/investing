
package ee.tenman.investing.integration.bscscan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@SuppressWarnings("unused")
public class TokenTransferEvents {

    private String message;
    @JsonProperty("result")
    private List<Event> events;
    private String status;

    @Data
    @Getter
    @SuppressWarnings("unused")
    public static class Event {

        private String blockHash;
        private String blockNumber;
        private String confirmations;
        private String contractAddress;
        private String cumulativeGasUsed;
        private String from;
        private String gas;
        private String gasPrice;
        private String gasUsed;
        private String hash;
        private String input;
        private String nonce;
        private String timeStamp;
        private String to;
        private String tokenDecimal;
        private String tokenName;
        private String tokenSymbol;
        private String transactionIndex;
        private String value;

    }

}
