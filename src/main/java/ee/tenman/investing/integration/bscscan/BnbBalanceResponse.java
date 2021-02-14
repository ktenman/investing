
package ee.tenman.investing.integration.bscscan;

import lombok.Data;

@Data
@SuppressWarnings("unused")
public class BnbBalanceResponse {

    private String message;
    private String result;
    private String status;

}
