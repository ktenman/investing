package ee.tenman.investing;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;

import java.io.IOException;
import java.net.URL;

public class TestFileUtils {
    public static JsonNode getJson(String fileName) throws IOException {
        URL resource = TestFileUtils.class.getClassLoader().getResource(fileName);
        return JsonLoader.fromURL(resource);
    }
}