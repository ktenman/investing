package ee.tenman.investing.integration.ecb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
class ECBUnmarshaller implements Function<String, List<ConversionRate>> {

    @Resource
    private DocumentBuilderFactory documentBuilderFactory;

    @Override
    public List<ConversionRate> apply(String xml) {
        InputSource inputSource = new InputSource(new StringReader(xml));
        List<ConversionRate> conversionRates = new ArrayList<>();
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(inputSource);

            document.getDocumentElement()
                    .normalize();

            Node rootCube = document.getElementsByTagName("Cube")
                    .item(0);
            NodeList dates = rootCube.getChildNodes();

            for (int i = 0; i < dates.getLength(); i++) {
                Node dailyNode = dates.item(i);
                if (dailyNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                NamedNodeMap dailyNodeAttributes = dailyNode.getAttributes();
                String timeAttributeValue = dailyNodeAttributes.getNamedItem("time")
                        .getNodeValue();
                LocalDate localDate = LocalDate.parse(timeAttributeValue, DateTimeFormatter.ISO_LOCAL_DATE);
                NodeList exchangeNodeList = dailyNode.getChildNodes();
                for (int j = 0; j < exchangeNodeList.getLength(); j++) {
                    Node exchangeNode = exchangeNodeList.item(j);
                    if (exchangeNode.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    NamedNodeMap exchangeNodeAttributes = exchangeNode.getAttributes();
                    String currency = exchangeNodeAttributes.getNamedItem("currency")
                            .getNodeValue();
                    String value = exchangeNodeAttributes.getNamedItem("rate")
                            .getNodeValue();
                    ConversionRate conversionRate = ConversionRate.builder()
                            .date(localDate)
                            .currency(currency)
                            .rate(new BigDecimal(value))
                            .build();
                    conversionRates.add(conversionRate);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse ECB xml rates: {}", e.getMessage(), e);
        }
        return conversionRates;
    }

}