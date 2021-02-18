
package ee.tenman.investing.integration.yieldwatchnet.api;

import lombok.Data;
import org.apache.commons.collections4.list.TreeList;

@Data
@SuppressWarnings("unused")
public class LPVaults {

    private TotalUSDValues totalUSDValues;
    private TreeList<Vault> vaults;

}
