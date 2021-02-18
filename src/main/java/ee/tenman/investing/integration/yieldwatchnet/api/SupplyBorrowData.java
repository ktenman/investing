
package ee.tenman.investing.integration.yieldwatchnet.api;

import lombok.Data;

import java.util.List;

@Data
@SuppressWarnings("unused")
public class SupplyBorrowData {

    private TotalUSDValues totalUSDValues;
    private List<Vault> vaults;

}
