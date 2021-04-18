package ee.tenman.investing.integration.ecb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.parsers.DocumentBuilderFactory;

@Configuration
class ECBConfiguration {

    @Bean
    public DocumentBuilderFactory documentBuilderFactory() {
        return DocumentBuilderFactory.newInstance();
    }

}
