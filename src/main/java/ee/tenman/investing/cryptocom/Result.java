
package ee.tenman.investing.cryptocom;


import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@lombok.Data
@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Result {

    private Object data;
    private String instrumentName;

}
