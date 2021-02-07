
package ee.tenman.investing.integration.cryptocom;


import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@lombok.Data
@SuppressWarnings("unused")
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Result<T> {

    private T data;
    private String instrumentName;

}
