package ee.tenman.investing.integration.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;

import static java.util.stream.Collectors.joining;

@Slf4j
@Configuration
public class GoogleSheetsApiClientConfiguration {

    @Value("private_key.txt")
    ClassPathResource privateKey;
    @Value("private_key_id.txt")
    ClassPathResource privateKeyId;

    @Bean
    public Sheets sheets() throws IOException, GeneralSecurityException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        Credential httpRequestInitializer = authorizeWithServiceAccount();

        return new Sheets.Builder(httpTransport, jsonFactory, httpRequestInitializer)
                .setApplicationName("Google-SheetsSample/0.1")
                .build();
    }

    private Credential authorizeWithServiceAccount() throws GeneralSecurityException, IOException {

        return new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(JacksonFactory.getDefaultInstance())
                .setServiceAccountId("splendid-myth-268820@appspot.gserviceaccount.com")
                .setServiceAccountScopes(Collections.singletonList(SheetsScopes.SPREADSHEETS))
                .setServiceAccountPrivateKeyId(getPrivateKeyId())
                .setServiceAccountPrivateKey(buildPrivateKey())
                .build();
    }

    private String getPrivateKeyId() {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(privateKeyId.getInputStream()))) {
            return buffer.lines().collect(joining(""));
        } catch (IOException e) {
            log.error("getPrivateKeyId ", e);
            return null;
        }
    }

    public PrivateKey buildPrivateKey() {
        try {
            // Read in the key into a String
            StringBuilder pkcs8Lines = new StringBuilder();
            InputStream resource = privateKey.getInputStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource));
            String line;
            while ((line = reader.readLine()) != null) {
                pkcs8Lines.append(line);
            }

            // Remove the "BEGIN" and "END" lines, as well as any whitespace
            String pkcs8Pem = pkcs8Lines.toString();
            pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
            pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
            pkcs8Pem = pkcs8Pem.replaceAll("\\s+", "");

            // Base64 decode the result
            byte[] pkcs8EncodedBytes = Base64.decodeBase64(pkcs8Pem.getBytes());

            // extract the private key
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(keySpec);
            return privateKey;
        } catch (Exception e) {
            return null;
        }
    }
}
