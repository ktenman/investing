package ee.tenman.investing;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.util.stream.Collectors.joining;

public interface FileUtils {
    static String getSecret(ClassPathResource classPathResource) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(classPathResource.getInputStream()))) {
            return buffer.lines().collect(joining(""));
        } catch (IOException ignored) {
            return null;
        }
    }
}
