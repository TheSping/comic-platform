package comic.platform.backend.parser.Impl;

import comic.platform.backend.parser.RuleParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class JsoupRuleParser implements RuleParser {
    // div#content@text

    @Override
    public boolean match(String rule) {
        // 因为它是最常用的，所以只要别人都不管，就默认归Jsoup管
        return rule != null && !rule.startsWith("$.") && !rule.startsWith("@XPath:");
    }

    @Override
    public String parse(String sourceData, String rule) {
        // 切割规则
        String[] parts = rule.split("@");
        String cssQuery = parts[0];
        String attr = parts.length > 1 ? parts[1] : "text";

        // 解析 HTML
        Document doc = Jsoup.parse(sourceData);
        Element target = doc.selectFirst(cssQuery);

        if (target == null) return "";

        // 提取
        if ("text".equalsIgnoreCase(attr)) {
            return target.text().trim();
        } else {
            return target.attr(attr).trim();
        }
    }
}
