
package ee.tenman.investing.integration.cryptocom;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Instrument {

    private long code;
    private String method;
    private Result<?> result;

}
