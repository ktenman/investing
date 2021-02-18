package ee.tenman.investing.integration.yieldwatchnet.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import javax.annotation.Resource;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static ee.tenman.investing.TestFileUtils.getJson;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest
@AutoConfigureWireMock(port = 0)
class YieldApiServiceWireMockTest {

    @Resource
    YieldApiService yieldApiService;

    @Resource
    ObjectMapper objectMapper;

    @Test
    void getYieldData() throws IOException, JSONException {
        JsonNode json = getJson("yieldwatch-response.json");
        String expectedJson = json.toPrettyString();
        WireMock.stubFor(get(anyUrl())
                .willReturn(aResponse()
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(expectedJson)));

        YieldData yieldData = yieldApiService.getYieldData();

        String actual = objectMapper.writeValueAsString(yieldData);
        JSONAssert.assertEquals(expectedJson, actual, false);
    }
}