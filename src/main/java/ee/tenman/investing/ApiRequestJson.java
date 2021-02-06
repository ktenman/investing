package ee.tenman.investing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiRequestJson {
    private Long id;
    private String method;
    private Map<String, Object> params;
    private String sig;

    @JsonProperty("api_key")
    private String apiKey;

    private Long nonce;
}
