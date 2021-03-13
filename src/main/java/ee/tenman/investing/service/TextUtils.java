package ee.tenman.investing.service;

import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.replace;

public interface TextUtils {
    static String removeCommas(String element) {
        if (StringUtils.indexOf(element, ".") > StringUtils.indexOf(element, ",")) {
            return replace(replace(element, ",", ""), ".", ".");
        } else {
            return replace(replace(element, ".", ""), ",", ".");
        }
    }
}
